/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.internal.analysis.graph.core.Activator;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.HTNode.NodeType;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.TmfEdgeInterval.EdgeIntervalType;
import org.eclipse.tracecompass.internal.provisional.datastore.core.condition.TimeRangeCondition;
import org.eclipse.tracecompass.internal.provisional.datastore.core.exceptions.RangeException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

/**
 * History tree to save execution graphs on disk. Based on the segment store's
 * SegmentHistoryTree class, but removes the generic for performance reason (no
 * generics is 3 to 5 times faster). Most of the class is a copy-paste of the
 * datastore's and segment store's history tree classes. The main specificities
 * are in the queries, optimized for the kind of queries done in a graph, ie
 * following a path from a vertex's start/end time vs queries for all objects
 * intersecting specific time ranges for segment stores.
 *
 * The graph history tree contains {@link TmfEdgeInterval} objects and the pair
 * end time/end worker ID of an edge should match the start time/start worker ID
 * of another edge.
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class GraphHistoryTree {

    private static final int HISTORY_MAGIC_NUMBER = 0x05FFC0DE;

    /** File format version. Increment when breaking compatibility. */
    private static final int FILE_VERSION = 1;

    /**
     * Size of the "tree header" in the tree-file The nodes will use this offset
     * to know where they should be in the file. This should always be a
     * multiple of 4K.
     */
    private static final int TREE_HEADER_SIZE = 4096;

    // ------------------------------------------------------------------------
    // Tree-specific configuration
    // ------------------------------------------------------------------------

    /* Tree configuration constants */
    private final File fHistoryFile;
    private final int fBlockSize;
    private final int fMaxChildren;
    private final int fProviderVersion;
    private final long fTreeStart;
    private final IHTIntervalReader<TmfEdgeInterval> fEdgeIntervalReader;

    /** Reader/writer object */
    private HtIo fTreeIO;

    // ------------------------------------------------------------------------
    // Variable Fields (will change throughout the existence of the SHT)
    // ------------------------------------------------------------------------

    /** Latest timestamp found in the tree (at any given moment) */
    private long fTreeEnd;

    /** The total number of nodes that exists in this tree */
    private int fNodeCount;

    /** "Cache" to keep the active nodes in memory */
    private final List<GraphTreeNode> fLatestBranch;

    /* Lock used to protect the accesses to the HT_IO object */
    private final ReentrantReadWriteLock fRwl = new ReentrantReadWriteLock(false);

    private transient @Nullable List<GraphTreeNode> fLatestBranchSnapshot = null;

    /**
     * Create a new History from scratch, specifying all configuration
     * parameters.
     *
     * @param stateHistoryFile
     *            The name of the history file
     * @param blockSize
     *            The size of each "block" on disk in bytes. One node will
     *            always fit in one block. It should be at least 4096.
     * @param maxChildren
     *            The maximum number of children allowed per core (non-leaf)
     *            node.
     * @param providerVersion
     *            The version of the state provider. If a file already exists,
     *            and their versions match, the history file will not be rebuilt
     *            uselessly.
     * @param treeStart
     *            The start time of the history
     * @param edgeIntervalReader
     *            The edge interval reader to deserialize edges written on disk
     * @throws IOException
     *             If an error happens trying to open/write to the file
     *             specified in the config
     */
    public GraphHistoryTree(File stateHistoryFile,
            int blockSize,
            int maxChildren,
            int providerVersion,
            long treeStart,
            IHTIntervalReader<TmfEdgeInterval> edgeIntervalReader) throws IOException {
        /*
         * Simple check to make sure we have enough place in the 0th block for
         * the tree configuration
         */
        if (blockSize < TREE_HEADER_SIZE) {
            throw new IllegalArgumentException();
        }

        fHistoryFile = stateHistoryFile;
        fBlockSize = blockSize;
        fMaxChildren = maxChildren;
        fProviderVersion = providerVersion;
        fTreeStart = treeStart;
        fEdgeIntervalReader = edgeIntervalReader;

        fTreeEnd = treeStart;
        fNodeCount = 0;
        fLatestBranch = NonNullUtils.checkNotNull(Collections.synchronizedList(new ArrayList<>()));

        /* Prepare the IO object */
        fTreeIO = new HtIo(stateHistoryFile,
                blockSize,
                maxChildren,
                true,
                getNodeFactory(),
                fEdgeIntervalReader);

        /* Add the first node to the tree */
        GraphTreeNode firstNode = initNewLeafNode(-1, treeStart);
        fLatestBranch.add(firstNode);
    }

    /**
     * "Reader" constructor : instantiate a SHTree from an existing tree file on
     * disk
     *
     * @param existingStateFile
     *            Path/filename of the history-file we are to open
     * @param expectedProviderVersion
     *            The expected version of the state provider
     * @param edgeIntervalReader
     *            Interval reader
     * @throws IOException
     *             If an error happens reading the file
     */
    public GraphHistoryTree(File existingStateFile,
            int expectedProviderVersion, IHTIntervalReader<TmfEdgeInterval> edgeIntervalReader) throws IOException {
        /*
         * Open the file ourselves, get the tree header information we need,
         * then pass on the descriptor to the TreeIO object.
         */
        int rootNodeSeqNb, res;
        int bs, maxc;
        long startTime;

        /* Java I/O mumbo jumbo... */
        if (!existingStateFile.exists()) {
            throw new IOException("Selected state file does not exist"); //$NON-NLS-1$
        }
        if (existingStateFile.length() <= 0) {
            throw new IOException("Empty target file"); //$NON-NLS-1$
        }

        try (FileInputStream fis = new FileInputStream(existingStateFile);
                FileChannel fc = fis.getChannel();) {

            ByteBuffer buffer = ByteBuffer.allocate(TREE_HEADER_SIZE);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.clear();

            res = fc.read(buffer);
            if (res != TREE_HEADER_SIZE) {
                throw new IOException("Invalid header size"); //$NON-NLS-1$
            }

            buffer.flip();

            /*
             * Check the magic number to make sure we're opening the right type
             * of file
             */
            res = buffer.getInt();
            if (res != HISTORY_MAGIC_NUMBER) {
                throw new IOException("Wrong magic number"); //$NON-NLS-1$
            }

            res = buffer.getInt(); /* File format version number */
            if (res != FILE_VERSION) {
                throw new IOException("Mismatching History Tree file format versions"); //$NON-NLS-1$
            }

            res = buffer.getInt(); /* Event handler's version number */
            if (res != expectedProviderVersion) {
                /*
                 * The existing history was built using an event handler that
                 * doesn't match the current one in the framework.
                 *
                 * Information could be all wrong. Instead of keeping an
                 * incorrect history file, a rebuild is done.
                 */
                throw new IOException("Mismatching event handler versions"); //$NON-NLS-1$
            }

            bs = buffer.getInt(); /* Block Size */
            maxc = buffer.getInt(); /* Max nb of children per node */

            fNodeCount = buffer.getInt();
            rootNodeSeqNb = buffer.getInt();
            startTime = buffer.getLong();

            /* Set all other permanent configuration options */
            fHistoryFile = existingStateFile;
            fBlockSize = bs;
            fMaxChildren = maxc;
            fProviderVersion = expectedProviderVersion;
            fTreeStart = startTime;
            fEdgeIntervalReader = edgeIntervalReader;
        }

        /*
         * FIXME We close this here and the TreeIO will then reopen the same
         * file, not extremely elegant. But how to pass the information here to
         * the SHT otherwise?
         */
        fTreeIO = new HtIo(fHistoryFile,
                fBlockSize,
                fMaxChildren,
                false,
                getNodeFactory(),
                fEdgeIntervalReader);

        fLatestBranch = buildLatestBranch(rootNodeSeqNb);
        fTreeEnd = getRootNode().getNodeEnd();

        /*
         * Make sure the history start time we read previously is consistent
         * with was is actually in the root node.
         */
        if (startTime != getRootNode().getNodeStart()) {
            throw new IOException("Inconsistent start times in the " + //$NON-NLS-1$
                    "history file, it might be corrupted."); //$NON-NLS-1$
        }
    }

    /**
     * Rebuild the latestBranch "cache" object by reading the nodes from disk
     * (When we are opening an existing file on disk and want to append to it,
     * for example).
     *
     * @param rootNodeSeqNb
     *            The sequence number of the root node, so we know where to
     *            start
     * @throws ClosedChannelException
     */
    private List<GraphTreeNode> buildLatestBranch(int rootNodeSeqNb) throws ClosedChannelException {
        List<GraphTreeNode> list = new ArrayList<>();

        GraphTreeNode nextChildNode = fTreeIO.readNode(rootNodeSeqNb);
        list.add(nextChildNode);

        // TODO: Do we need the full latest branch? The latest leaf may not be
        // the one we'll query first... Won't it build itself later?

        /* Follow the last branch up to the leaf */
        while (nextChildNode.getNodeType() == HTNode.NodeType.CORE) {
            nextChildNode = fTreeIO.readNode(nextChildNode.getLatestChild());
            list.add(nextChildNode);
        }
        return Collections.synchronizedList(list);
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Get the start time of this tree
     *
     * @return The start time of the tree, in nanoseconds
     */
    public long getTreeStart() {
        return fTreeStart;
    }

    /**
     * Get the current end time of the tree
     *
     * @return The time of the latest edge in the tree
     */
    public long getTreeEnd() {
        return fTreeEnd;
    }

    /**
     * Get the number of nodes in this tree.
     *
     * @return The number of nodes
     */
    public int getNodeCount() {
        return fNodeCount;
    }

    /**
     * Get the current root node of this tree
     *
     * @return The root node
     */
    public GraphTreeNode getRootNode() {
        return fLatestBranch.get(0);
    }

    /**
     * Get the size of the file, in bytes
     *
     * @return The size of the file in bytes
     */
    public long getFileSize() {
        return fHistoryFile.length();
    }

    /**
     * Return the latest branch of the tree. That branch is immutable.
     *
     * @return The immutable latest branch
     */
    @VisibleForTesting
    protected final List<GraphTreeNode> getLatestBranch() {
        List<GraphTreeNode> latestBranchSnapshot = fLatestBranchSnapshot;
        if (latestBranchSnapshot == null) {
            synchronized (fLatestBranch) {
                latestBranchSnapshot = ImmutableList.copyOf(fLatestBranch);
                fLatestBranchSnapshot = latestBranchSnapshot;
            }
        }
        return latestBranchSnapshot;
    }

    /**
     * Get the node in the latest branch at a depth. If the depth is too large,
     * it will throw an IndexOutOfBoundsException
     *
     * @param depth
     *            The depth at which to get the node
     * @return The node at depth
     */
    protected GraphTreeNode getLatestNode(int depth) {
        if (depth > fLatestBranch.size()) {
            throw new IndexOutOfBoundsException("Trying to get latest node too deep"); //$NON-NLS-1$
        }
        return fLatestBranch.get(depth);
    }

    /**
     * Get the node factory for this tree.
     *
     * @return The node factory
     */
    protected HTNode.IHTNodeFactory<GraphTreeNode> getNodeFactory() {
        // This cannot be defined statically because of the generic and because
        // this method is called from the constructor of the abstract class,
        // assigning it in a final field in the constructor generates a NPE. So
        // we return the method directly here.
        return (t, b, m, seq, p, start) -> new GraphTreeNode(t, b, m, seq, p, start);
    }

    /**
     * Read a node with a given sequence number
     *
     * @param seqNum
     *            The sequence number of the node to read
     * @return The HTNode object
     * @throws ClosedChannelException
     *             Exception thrown when reading the node, if the file was
     *             closed
     */
    @VisibleForTesting
    protected GraphTreeNode getNode(int seqNum) throws ClosedChannelException {
        // First, check in the latest branch if the node is there
        for (GraphTreeNode node : fLatestBranch) {
            if (node.getSequenceNumber() == seqNum) {
                return node;
            }
        }
        return fTreeIO.readNode(seqNum);
    }

    /**
     * Retrieve the TreeIO object. Should only be used for testing.
     *
     * @return The TreeIO
     */
    @VisibleForTesting
    HtIo getTreeIO() {
        return fTreeIO;
    }

    // ------------------------------------------------------------------------
    // HT_IO interface
    // ------------------------------------------------------------------------

    /**
     * Return the FileInputStream reader with which we will read the worker
     * section (it will be sought to the correct position).
     *
     * @return The FileInputStream indicating the file and position from which
     *         the worker section can be read.
     */
    public FileInputStream supplyATReader() {
        fRwl.readLock().lock();
        try {
            return fTreeIO.supplyATReader(getNodeCount());
        } finally {
            fRwl.readLock().unlock();
        }
    }

    /**
     * Return the file to which we will write the worker section.
     *
     * @return The file to which we will write the worker section
     */
    public File supplyATWriterFile() {
        return fHistoryFile;
    }

    /**
     * Return the position in the file (given by {@link #supplyATWriterFile})
     * where to start writing the worker section.
     *
     * @return The position in the file where to start writing
     */
    public long supplyATWriterFilePos() {
        return TREE_HEADER_SIZE
                + ((long) getNodeCount() * fBlockSize);
    }

    /**
     * Read a node from the tree.
     *
     * @param seqNumber
     *            The sequence number of the node to read
     * @return The node
     * @throws ClosedChannelException
     *             If the tree IO is unavailable
     */
    public GraphTreeNode readNode(int seqNumber) throws ClosedChannelException {
        /* Try to read the node from memory */
        synchronized (fLatestBranch) {
            for (GraphTreeNode node : fLatestBranch) {
                if (node.getSequenceNumber() == seqNumber) {
                    return node;
                }
            }
        }

        fRwl.readLock().lock();
        try {
            /* Read the node from disk */
            return fTreeIO.readNode(seqNumber);
        } finally {
            fRwl.readLock().unlock();
        }
    }

    /**
     * Write a node object to the history file.
     *
     * @param node
     *            The node to write to disk
     */
    public void writeNode(GraphTreeNode node) {
        fRwl.readLock().lock();
        try {
            fTreeIO.writeNode(node);
        } finally {
            fRwl.readLock().unlock();
        }
    }

    /**
     * Close the history file.
     */
    public void closeFile() {
        fRwl.writeLock().lock();
        try {
            fTreeIO.closeFile();
            clearContent();
        } finally {
            fRwl.writeLock().unlock();
        }
    }

    /**
     * Delete the history file.
     */
    public void deleteFile() {
        fRwl.writeLock().lock();
        try {
            fTreeIO.deleteFile();
            clearContent();
        } finally {
            fRwl.writeLock().unlock();
        }
    }

    /**
     * Reinitialize the tree, delete the file and remove all current branch
     * context.
     *
     * @throws IOException
     *             Exceptions thrown by the file deletion
     */
    public void cleanFile() throws IOException {
        fRwl.writeLock().lock();
        try {
            closeTree(fTreeEnd);
            fTreeIO.deleteFile();

            fTreeIO = new HtIo(fHistoryFile,
                    fBlockSize,
                    fMaxChildren,
                    true,
                    getNodeFactory(),
                    fEdgeIntervalReader);

            clearContent();
            /* Add the first node to the tree */
            GraphTreeNode firstNode = initNewLeafNode(-1, fTreeStart);
            fLatestBranch.add(firstNode);
        } finally {
            fRwl.writeLock().unlock();
        }
    }

    private void clearContent() {
        // Re-initialize the content of the tree after the file is deleted or
        // closed
        fNodeCount = 0;
        fLatestBranch.clear();
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Insert an interval in the tree.
     *
     * @param interval
     *            The interval to be inserted
     * @throws RangeException
     *             If the start of end time of the interval are invalid
     */
    public synchronized void insert(TmfEdgeInterval interval) throws RangeException {
        if (interval.getStart() < fTreeStart) {
            throw new RangeException("Interval Start:" + interval.getStart() + ", Config Start:" + fTreeStart); //$NON-NLS-1$ //$NON-NLS-2$
        }
        tryInsertAtNode(interval, fLatestBranch.size() - 1);
    }

    /**
     * Add a new empty core node to the tree.
     *
     * @param parentSeqNumber
     *            Sequence number of this node's parent
     * @param startTime
     *            Start time of the new node
     * @return The newly created node
     */
    protected final GraphTreeNode initNewCoreNode(int parentSeqNumber, long startTime) {
        GraphTreeNode newNode = getNodeFactory().createNode(NodeType.CORE, fBlockSize, fMaxChildren,
                fNodeCount, parentSeqNumber, startTime);
        fNodeCount++;
        return newNode;
    }

    /**
     * Add a new empty leaf node to the tree.
     *
     * @param parentSeqNumber
     *            Sequence number of this node's parent
     * @param startTime
     *            Start time of the new node
     * @return The newly created node
     */
    protected final GraphTreeNode initNewLeafNode(int parentSeqNumber, long startTime) {
        GraphTreeNode newNode = getNodeFactory().createNode(NodeType.LEAF, fBlockSize, fMaxChildren,
                fNodeCount, parentSeqNumber, startTime);
        fNodeCount++;
        return newNode;
    }

    /**
     * Inner method to find in which node we should add the interval.
     *
     * @param interval
     *            The interval to add to the tree
     * @param depth
     *            The index *in the latestBranch* where we are trying the
     *            insertion
     */
    protected final void tryInsertAtNode(TmfEdgeInterval interval, int depth) {
        GraphTreeNode targetNode = fLatestBranch.get(depth);
        informInsertingAtDepth(depth);

        /* Verify if there is enough room in this node to store this interval */
        if (interval.getSizeOnDisk() > targetNode.getNodeFreeSpace()) {
            /* Nope, not enough room. Insert in a new sibling instead. */
            addSiblingNode(depth, getNewBranchStart(depth, interval));
            tryInsertAtNode(interval, getLatestBranch().size() - 1);
            return;
        }

        /* Make sure the interval time range fits this node */
        if (interval.getStart() < targetNode.getNodeStart()) {
            /*
             * No, this interval starts before the startTime of this node. We
             * need to check recursively in parents if it can fit.
             */
            tryInsertAtNode(interval, depth - 1);
            return;
        }

        /*
         * Ok, there is room, and the interval fits in this time slot. Let's add
         * it.
         */
        targetNode.add(interval);

        updateEndTime(interval);
    }

    /**
     * Informs the tree that the insertion is requested at a given depth. When
     * this is called, the element is not yet inserted, but the last call to
     * this for an element will represent the depth at which is was really
     * inserted. By default, this method does nothing and should not be
     * necessary for concrete implementations, but it can be used by unit tests
     * to check to position of insertion of elements.
     *
     * @param depth
     *            The depth at which the last insertion was done
     */
    @VisibleForTesting
    protected void informInsertingAtDepth(int depth) {

    }

    private long getNewBranchStart(int depth, TmfEdgeInterval interval) {
        // The node starts at the time of the interval to add, but should not
        // start before the previous sibling
        // TODO: Do some benchmark to see if this Math.max is efficient enough
        // as opposed to just interval.getStart which would require to start the
        // new branch higher up in the tree
        return Math.max(interval.getStart(), getLatestNode(depth).getNodeStart());
    }

    /**
     * Method to add a sibling to any node in the latest branch. This will add
     * children back down to the leaf level, if needed.
     *
     * @param depth
     *            The depth in latestBranch where we start adding
     * @param newNodeStartTime
     *            The start time of the new node
     */
    private final void addSiblingNode(int depth, long newNodeStartTime) {
        synchronized (fLatestBranch) {
            final long splitTime = fTreeEnd;
            fLatestBranchSnapshot = null;

            if (depth >= fLatestBranch.size()) {
                /*
                 * We need to make sure (indexOfNode - 1) doesn't get the last
                 * node in the branch, because that one is a Leaf Node.
                 */
                throw new IllegalStateException();
            }

            /* Check if we need to add a new root node */
            if (depth == 0) {
                addNewRootNode(newNodeStartTime);
                return;
            }

            /*
             * Check if we can indeed add a child to the target parent and if
             * the new start time is not before the target parent.
             */
            if (fLatestBranch.get(depth - 1).getNbChildren() == fMaxChildren ||
                    newNodeStartTime < fLatestBranch.get(depth - 1).getNodeStart()) {
                /* If not, add a branch starting one level higher instead */
                addSiblingNode(depth - 1, newNodeStartTime);
                return;
            }

            /*
             * Close nodes from the leaf up because some parent nodes may need
             * to get updated when their children are closed
             */
            for (int i = fLatestBranch.size() - 1; i >= depth; i--) {
                fLatestBranch.get(i).closeThisNode(splitTime);
                fTreeIO.writeNode(fLatestBranch.get(i));
            }

            /* Split off the new branch from the old one */
            for (int i = depth; i < fLatestBranch.size(); i++) {
                GraphTreeNode prevNode = fLatestBranch.get(i - 1);
                GraphTreeNode newNode;

                switch (fLatestBranch.get(i).getNodeType()) {
                case CORE:
                    newNode = initNewCoreNode(prevNode.getSequenceNumber(), newNodeStartTime);
                    break;
                case LEAF:
                    newNode = initNewLeafNode(prevNode.getSequenceNumber(), newNodeStartTime);
                    break;
                default:
                    throw new IllegalStateException();
                }

                prevNode.linkNewChild(newNode);
                fLatestBranch.set(i, newNode);
            }
        }
    }

    /**
     * Similar to the previous method, except here we rebuild a completely new
     * latestBranch
     */
    private void addNewRootNode(long newNodeStartTime) {
        final long nodeEnd = fTreeEnd;

        GraphTreeNode oldRootNode = fLatestBranch.get(0);
        GraphTreeNode newRootNode = initNewCoreNode(-1, fTreeStart);

        /* Tell the old root node that it isn't root anymore */
        oldRootNode.setParentSequenceNumber(newRootNode.getSequenceNumber());

        /* Close off the whole current latestBranch */
        for (int i = fLatestBranch.size() - 1; i >= 0; i--) {
            fLatestBranch.get(i).closeThisNode(nodeEnd);
            fTreeIO.writeNode(fLatestBranch.get(i));
        }

        /* Link the new root to its first child (the previous root node) */
        newRootNode.linkNewChild(oldRootNode);

        /* Rebuild a new latestBranch */
        int depth = fLatestBranch.size();
        fLatestBranch.clear();
        fLatestBranch.add(newRootNode);

        // Create new coreNode
        for (int i = 1; i < depth; i++) {
            GraphTreeNode prevNode = fLatestBranch.get(i - 1);
            GraphTreeNode newNode = initNewCoreNode(prevNode.getSequenceNumber(), newNodeStartTime);
            prevNode.linkNewChild(newNode);
            fLatestBranch.add(newNode);
        }

        // Create the new leafNode
        GraphTreeNode prevNode = fLatestBranch.get(depth - 1);
        GraphTreeNode newNode = initNewLeafNode(prevNode.getSequenceNumber(), newNodeStartTime);
        prevNode.linkNewChild(newNode);
        fLatestBranch.add(newNode);
    }

    /**
     * Update the tree's end time with this interval data
     *
     * @param interval
     *            The interval that was just added to the tree
     */
    protected void updateEndTime(TmfEdgeInterval interval) {
        fTreeEnd = Math.max(fTreeEnd, interval.getEnd());
    }

    /**
     * "Save" the tree to disk. This method will cause the treeIO object to
     * commit all nodes to disk and the header of the tree should also be saved
     * on disk
     *
     * @param requestedEndTime
     *            The greatest timestamp present in the history tree
     */
    public void closeTree(long requestedEndTime) {
        /* This is an important operation, queries can wait */
        synchronized (fLatestBranch) {
            /*
             * Work-around the "empty branches" that get created when the root
             * node becomes full. Overwrite the tree's end time with the
             * original wanted end-time, to ensure no queries are sent into
             * those empty nodes.
             */
            fTreeEnd = requestedEndTime;

            /* Close off the latest branch of the tree */
            for (int i = fLatestBranch.size() - 1; i >= 0; i--) {
                fLatestBranch.get(i).closeThisNode(fTreeEnd);
                fTreeIO.writeNode(fLatestBranch.get(i));
            }

            // Write the header of the history tree, at the beginning of the
            // file
            try (FileOutputStream fc = fTreeIO.getFileWriter(-1);) {
                ByteBuffer buffer = ByteBuffer.allocate(TREE_HEADER_SIZE);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.clear();

                buffer.putInt(HISTORY_MAGIC_NUMBER);

                buffer.putInt(FILE_VERSION);
                buffer.putInt(fProviderVersion);

                buffer.putInt(fBlockSize);
                buffer.putInt(fMaxChildren);

                buffer.putInt(fNodeCount);

                /* root node seq. nb */
                buffer.putInt(fLatestBranch.get(0).getSequenceNumber());

                /* start time of this history */
                buffer.putLong(fLatestBranch.get(0).getNodeStart());

                buffer.flip();
                fc.write(buffer.array());
                /* done writing the file header */

            } catch (IOException e) {
                /*
                 * If we were able to write so far, there should not be any
                 * problem at this point...
                 */
                throw new RuntimeException("State system write error"); //$NON-NLS-1$
            }
        }
    }

    /**
     * Get the number of elements in this history tree
     *
     * @return The number of elements in this tree
     */
    public int size() {
        GraphTreeNode node;
        long total = 0;

        try {
            // Add the number of intervals of each node
            for (int seq = 0; seq < getNodeCount(); seq++) {
                node = readNode(seq);
                total += node.getNumIntervals();
            }
        } catch (ClosedChannelException e) {
            Activator.getInstance().logError(e.getMessage(), e);
            return 0;
        }
        return (int) total;
    }

    // ------------------------------------------
    // graph specific methods
    // ------------------------------------------

    /**
     * Return the first edge that validates the predicate
     *
     * @param ts
     *            The timestamp that the edge should intersect
     * @param predicate
     *            A predicate that the edge should validate
     * @param tsIsExactEnd
     *            Whether the timestamp represents the exact end time of an
     *            edge, in which case the query can be optimized
     * @return The first edge that validates the predicate or <code>null</code>
     *         if no edge is found
     * @throws ClosedChannelException
     */
    private @Nullable TmfEdgeInterval queryEdge(long ts, Predicate<TmfEdgeInterval> predicate, boolean tsIsExactEnd) throws ClosedChannelException {
        final TimeRangeCondition rc = TimeRangeCondition.singleton(ts);
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(getRootNode().getSequenceNumber());
        TmfEdgeInterval interval = null;
        while (interval == null && !queue.isEmpty()) {
            int sequenceNumber = queue.pop();
            GraphTreeNode currentNode = readNode(sequenceNumber);
            if (currentNode.getNodeType() == HTNode.NodeType.CORE) {
                /* Here we add the relevant children nodes for BFS */
                queue.addAll(currentNode.selectNextChildren(rc));
            }
            interval = currentNode.getMatchingInterval(ts, predicate, tsIsExactEnd);
        }
        // System.out.println("Number of nodes to check: " + count);
        return interval;
    }

    /**
     * Query an edge starting at the vertex in parameter
     *
     * @param vertex
     *            The origin vertex of the edge
     * @param horizontal
     *            Whether the returned edge is horizontal or vertical. If
     *            horizontal, any edge type that is not vertical will be
     *            returned, including null and filler edges.
     * @return The horizontal edge from the vertex or <code>null</code> if no
     *         horizontal edge starts at this vertex
     * @throws ClosedChannelException
     *             Exception thrown by the query if the graph is closed
     */
    public @Nullable TmfEdgeInterval queryEdgeFrom(TmfVertex vertex, boolean horizontal) throws ClosedChannelException {
        // Accept null or horizontal edge for horizontal
        long ts = vertex.getTimestamp();
        Predicate<TmfEdgeInterval> predicate = horizontal ? edge -> (edge.getIntervalType() != EdgeIntervalType.VERTICAL) &&
                edge.getStart() == ts &&
                edge.getFromWorkerId() == vertex.getWorkerId()
                : edge -> edge.getIntervalType() == EdgeIntervalType.VERTICAL &&
                        edge.getStart() == ts &&
                        edge.getFromWorkerId() == vertex.getWorkerId();
        return queryEdge(ts, predicate, false);
    }

    /**
     * Returns any interval that contains this vertex, to verify that a vertex
     * is in the graph
     *
     * @param vertex
     *            The vertex for which to validate the presence
     * @return Any edge that contains the vertex
     * @throws ClosedChannelException
     *             Exception thrown by the query if the graph is closed
     */
    public @Nullable TmfEdgeInterval queryVertex(TmfVertex vertex) throws ClosedChannelException {
        return queryEdge(vertex.getTimestamp(),
                edge -> (edge.getStart() == vertex.getTimestamp() && edge.getFromWorkerId() == vertex.getWorkerId()) || (edge.getEnd() == vertex.getTimestamp() && edge.getToWorkerId() == vertex.getWorkerId()), false);
    }

    /**
     * Query an edge ending at the vertex in parameter
     *
     * @param vertex
     *            The destination vertex of the edge
     * @param horizontal
     *            Whether the returned edge is horizontal or vertical. If
     *            horizontal, any edge type that is not vertical will be
     *            returned, including null and filler edges.
     * @return The horizontal edge to the vertex or <code>null</code> if no
     *         horizontal edge ends at this vertex
     * @throws ClosedChannelException
     *             Exception thrown by the query if the graph is closed
     */
    public @Nullable TmfEdgeInterval queryEdgeTo(TmfVertex vertex, boolean horizontal) throws ClosedChannelException {
        long ts = vertex.getTimestamp();
        Predicate<TmfEdgeInterval> predicate = horizontal ? edge -> edge.getIntervalType() != EdgeIntervalType.VERTICAL &&
                edge.getEnd() == ts &&
                edge.getToWorkerId() == vertex.getWorkerId()
                : edge -> edge.getIntervalType() == EdgeIntervalType.VERTICAL &&
                        edge.getEnd() == ts &&
                        edge.getToWorkerId() == vertex.getWorkerId();
        return queryEdge(ts, predicate, true);

    }

    @Override
    public String toString() {
        return "Information on the current tree:\n\n" + "Blocksize: " //$NON-NLS-1$ //$NON-NLS-2$
                + fBlockSize + "\n" + "Max nb. of children per node: " //$NON-NLS-1$//$NON-NLS-2$
                + fMaxChildren + "\n" + "Number of nodes: " + fNodeCount //$NON-NLS-1$//$NON-NLS-2$
                + "\n" + "Depth of the tree: " + fLatestBranch.size() + "\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + "Size of the treefile: " + getFileSize() + "\n" //$NON-NLS-1$//$NON-NLS-2$
                + "Root node has sequence number: " //$NON-NLS-1$
                + fLatestBranch.get(0).getSequenceNumber() + "\n" //$NON-NLS-1$
                + "'Latest leaf' has sequence number: " //$NON-NLS-1$
                + fLatestBranch.get(fLatestBranch.size() - 1).getSequenceNumber();
    }

}
