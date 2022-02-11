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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.SafeByteBufferFactory;
import org.eclipse.tracecompass.internal.provisional.datastore.core.condition.TimeRangeCondition;
import org.eclipse.tracecompass.internal.provisional.datastore.core.exceptions.RangeException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

/**
 * The base class for all the types of nodes that go in the graph History Tree.
 *
 * This class is a copy of the HTNode class from o.e.t.datastore.core, removing
 * the generics, which triple execution time, and dropping the unnecessary
 * interface implementation.
 *
 * @author Alexandre Montplaisir
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class HTNode {

    // ------------------------------------------------------------------------
    // Class fields
    // ------------------------------------------------------------------------

    /**
     * Size of the basic node header. This is backward-compatible with previous
     * state sytem history trees
     *
     * <pre>
     *  1 - byte (the type of node)
     * 16 - 2x long (start time, end time)
     * 12 - 3x int (seq number, parent seq number, intervalcount)
     *  1 - byte (done or not)
     * </pre>
     */
    @VisibleForTesting
    public static final int COMMON_HEADER_SIZE = Byte.BYTES
            + 2 * Long.BYTES
            + 3 * Integer.BYTES
            + Byte.BYTES;

    private static final IntPredicate ALWAYS_TRUE = i -> true;

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * Default implementation of the interval comparator, which sorts first by
     * end times, then by start times
     */
    private final Comparator<TmfEdgeInterval> fDefaultIntervalComparator = Comparator
            .comparingLong(TmfEdgeInterval::getEnd)
            .thenComparingLong(TmfEdgeInterval::getStart);

    /* Node configuration, defined by the history tree */
    private final int fBlockSize;
    private final int fMaxChildren;

    /* Time range of this node */
    private final long fNodeStart;
    private long fNodeEnd;

    /* Sequence number = position in the node section of the file */
    private final int fSequenceNumber;
    private int fParentSequenceNumber; /* = -1 if this node is the root node */

    /* Sum of bytes of all objects in the node */
    private int fSizeOfContentSection;

    /*
     * True if this node was saved on disk (meaning its end time is now fixed)
     */
    private volatile boolean fIsOnDisk;

    /* List containing all the intervals contained in this node */
    private final List<TmfEdgeInterval> fIntervals;

    /* Lock used to protect the accesses to objects, nodeEnd and such */
    private final ReentrantReadWriteLock fRwl = new ReentrantReadWriteLock(false);

    /* Object containing extra data for core nodes */
    private final @Nullable CoreNodeData fExtraData;

    /**
     * The type of node
     */
    public enum NodeType {
        /**
         * Core node, which is a "front" node, at any level of the tree except
         * the bottom-most one. It has children, and may have extensions.
         */
        CORE((byte) 1),
        /**
         * Leaf node, which is a node at the last bottom level of the tree. It
         * cannot have any children or extensions.
         */
        LEAF((byte) 2);

        private final byte fByte;

        NodeType(byte rep) {
            fByte = rep;
        }

        /**
         * Determine a node type by reading a serialized byte.
         *
         * @param rep
         *            The byte representation of the node type
         * @return The corresponding NodeType
         */
        public static NodeType fromByte(byte rep) {
            switch (rep) {
            case 1:
                return CORE;
            case 2:
                return LEAF;
            default:
                throw new IllegalArgumentException("The NodeType byte " + rep + " is not a valid type"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        /**
         * Get the byte representation of this node type. It can then be read
         * with {@link #fromByte}.
         *
         * @return The byte matching this node type
         */
        public byte toByte() {
            return fByte;
        }
    }

    /**
     * Interface for history to create the various HTNodes
     *
     * @param <N>
     *            The base type of the nodes of this tree
     */
    @FunctionalInterface
    public interface IHTNodeFactory<N extends HTNode> {

        /**
         * Creates a new node for the specific history tree
         *
         * @param type
         *            The type of node to create. See {@link NodeType}.
         * @param blockSize
         *            The size (in bytes) of each node once serialized to disk
         * @param maxChildren
         *            The maximum number of amount a single core node can have
         * @param seqNumber
         *            The (unique) sequence number assigned to this particular
         *            node
         * @param parentSeqNumber
         *            The sequence number of this node's parent node
         * @param start
         *            The earliest timestamp stored in this node
         * @return The new core node
         */
        N createNode(NodeType type, int blockSize, int maxChildren,
                int seqNumber, int parentSeqNumber, long start);
    }

    /**
     * A class that encapsulates data about children of this node. This class
     * will be constructed by the core node and contains the extra header data,
     * methods to read/write the header data, etc.
     *
     * This base class for CORE nodes just saves the children, ie their sequence
     * number.
     *
     * @author Geneviève Bastien
     */
    protected static class CoreNodeData {

        /** Back-reference to the node class */
        private final HTNode fNode;

        /**
         * Seq. numbers of the children nodes (size = max number of nodes,
         * determined by configuration)
         */
        private final int[] fChildren;

        /** Nb. of children this node has */
        private int fNbChildren;

        /**
         * Constructor
         *
         * @param node
         *            The node containing this extra data.
         */
        public CoreNodeData(HTNode node) {
            fNode = node;
            fNbChildren = 0;
            /*
             * We instantiate the following array at full size right away, since
             * we want to reserve that space in the node's header. "fNbChildren"
             * will tell us how many relevant entries there are in those tables.
             */
            fChildren = new int[fNode.fMaxChildren];
        }

        /**
         * Return this core data's full node. To be used by subclasses.
         *
         * @return The node
         */
        protected HTNode getNode() {
            return fNode;
        }

        /**
         * Read the specific header for this extra node data
         *
         * @param buffer
         *            The byte buffer in which to read
         */
        protected void readSpecificHeader(ByteBuffer buffer) {
            fNode.takeWriteLock();
            try {
                int size = fNode.getMaxChildren();

                fNbChildren = buffer.getInt();

                for (int i = 0; i < fNbChildren; i++) {
                    fChildren[i] = buffer.getInt();
                }
                for (int i = fNbChildren; i < size; i++) {
                    buffer.getInt();
                }
            } finally {
                fNode.releaseWriteLock();
            }
        }

        /**
         * Write the specific header for this extra node data
         *
         * @param buffer
         *            The byte buffer in which to write
         */
        protected void writeSpecificHeader(ByteBuffer buffer) {
            fNode.takeReadLock();
            try {
                buffer.putInt(fNbChildren);

                /* Write the "children's seq number" array */
                for (int i = 0; i < fNbChildren; i++) {
                    buffer.putInt(fChildren[i]);
                }
                for (int i = fNbChildren; i < fNode.getMaxChildren(); i++) {
                    buffer.putInt(0);
                }
            } finally {
                fNode.releaseReadLock();
            }
        }

        /**
         * Get the number of children
         *
         * @return The number of children
         */
        public int getNbChildren() {
            fNode.takeReadLock();
            try {
                return fNbChildren;
            } finally {
                fNode.releaseReadLock();
            }
        }

        /**
         * Get the child sequence number at an index
         *
         * @param index
         *            The index of the child to get
         * @return The sequence number of the child node
         */
        public int getChild(int index) {
            fNode.takeReadLock();
            try {
                if (index >= fNbChildren) {
                    throw new IndexOutOfBoundsException("The child at index " + index + " does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                return fChildren[index];
            } finally {
                fNode.releaseReadLock();
            }
        }

        /**
         * Get the sequence number of the last child node of this one
         *
         * @return The sequence number of the last child
         */
        public int getLatestChild() {
            fNode.takeReadLock();
            try {
                return fChildren[fNbChildren - 1];
            } finally {
                fNode.releaseReadLock();
            }
        }

        /**
         * Add a new child to this node's data
         *
         * @param childNode
         *            The child node to add
         */
        public void linkNewChild(HTNode childNode) {
            fNode.takeWriteLock();
            try {
                if (fNbChildren >= fNode.getMaxChildren()) {
                    throw new IllegalStateException("Asked to link another child but parent already has maximum number of children"); //$NON-NLS-1$
                }

                fChildren[fNbChildren] = childNode.getSequenceNumber();
                fNbChildren++;

            } finally {
                fNode.releaseWriteLock();
            }
        }

        /**
         * Get the size of the extra header space necessary for this node's
         * extra data
         *
         * @return The extra header size
         */
        protected int getSpecificHeaderSize() {
            int maxChildren = fNode.getMaxChildren();
            int specificSize = Integer.BYTES /* 1x int (nbChildren) */

                    /* MAX_NB * int ('children' table) */
                    + Integer.BYTES * maxChildren;

            return specificSize;
        }

        @Override
        public String toString() {
            /* Only used for debugging, shouldn't be externalized */
            return String.format("Core Node, %d children %s", //$NON-NLS-1$
                    fNbChildren, Arrays.toString(Arrays.copyOf(fChildren, fNbChildren)));
        }

        /**
         * Inner method to select the sequence numbers for the children of the
         * current node that intersect the given timestamp. Useful for moving
         * down the tree.
         *
         * @param timeCondition
         *            The time-based RangeCondition to choose which children
         *            match.
         * @return Collection of sequence numbers of the child nodes that
         *         intersect t, non-null empty collection if this is a Leaf Node
         */
        public final Collection<Integer> selectNextChildren(TimeRangeCondition timeCondition) {
            return selectNextChildren(timeCondition, ALWAYS_TRUE);
        }

        /**
         * Inner method to select the sequence numbers for the children of the
         * current node that intersect the given timestamp. Useful for moving
         * down the tree.
         *
         * @param timeCondition
         *            The time-based RangeCondition to choose which children
         *            match.
         * @param extraPredicate
         *            Extra check to decide whether this child should be
         *            returned. This predicate receives the index of a child
         *            node and can do extra verification from the child's header
         *            data at this index.
         * @return Collection of sequence numbers of the child nodes that
         *         intersect t, non-null empty collection if this is a Leaf Node
         */
        public final Collection<Integer> selectNextChildren(TimeRangeCondition timeCondition, IntPredicate extraPredicate) {
            fNode.takeReadLock();
            try {
                List<Integer> list = new ArrayList<>();
                for (int index : selectNextIndices(timeCondition)) {
                    if (extraPredicate.test(index)) {
                        list.add(fChildren[index]);
                    }
                }
                return list;
            } finally {
                fNode.releaseReadLock();
            }
        }

        /**
         * Inner method to select the indices of the children of the current
         * node that intersect the given timestamp. Useful for moving down the
         * tree.
         *
         * This is the method that children implementations of this node should
         * override. They may call
         * <code>super.selectNextIndices(timeCondition)</code> to get access to
         * the subset of indices that match the parent's condition and add their
         * own filters. When this method is called a read-lock is already taken
         * on the node
         *
         * @param timeCondition
         *            The time-based RangeCondition to choose which children
         *            match.
         * @return Collection of the indices of the child nodes that intersect
         *         the time condition
         */
        protected Collection<Integer> selectNextIndices(TimeRangeCondition timeCondition) {
            /* By default, all children are returned */
            List<Integer> childList = new ArrayList<>();
            for (int i = 0; i < fNbChildren; i++) {
                childList.add(i);
            }

            return childList;
        }

        @Override
        public int hashCode() {
            /*
             * Do not consider "fNode", since the node's equals/hashCode already
             * consider us.
             */
            return Objects.hash(fNbChildren, fChildren);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            CoreNodeData other = (CoreNodeData) obj;
            return (fNbChildren == other.fNbChildren
                    && Arrays.equals(fChildren, other.fChildren));
        }

    }

    /**
     * Constructor
     *
     * @param type
     *            The type of this node
     * @param blockSize
     *            The size (in bytes) of a serialized node on disk
     * @param maxChildren
     *            The maximum allowed number of children per node
     * @param seqNumber
     *            The (unique) sequence number assigned to this particular node
     * @param parentSeqNumber
     *            The sequence number of this node's parent node
     * @param start
     *            The earliest timestamp stored in this node
     */
    public HTNode(NodeType type, int blockSize, int maxChildren, int seqNumber,
            int parentSeqNumber, long start) {
        fBlockSize = blockSize;
        fMaxChildren = maxChildren;

        fNodeStart = start;
        fSequenceNumber = seqNumber;
        fParentSequenceNumber = parentSeqNumber;

        fSizeOfContentSection = 0;
        fIsOnDisk = false;
        fIntervals = new ArrayList<>();

        fExtraData = createNodeExtraData(type);
    }

    /**
     * Reader factory method. Build a Node object (of the right type) by reading
     * a block in the file.
     *
     * @param blockSize
     *            The size (in bytes) of a serialized node on disk
     * @param maxChildren
     *            The maximum allowed number of children per node
     * @param fc
     *            FileChannel to the history file, ALREADY SEEKED at the start
     *            of the node.
     * @param objectReader
     *            The reader to read serialized node objects
     * @param nodeFactory
     *            The factory to create the nodes for this tree
     * @return The node object
     * @throws IOException
     *             If there was an error reading from the file channel
     */
    public static final <N extends HTNode> N readNode(
            int blockSize,
            int maxChildren,
            FileChannel fc,
            IHTIntervalReader<TmfEdgeInterval> objectReader,
            IHTNodeFactory<N> nodeFactory) throws IOException {

        N newNode;

        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.clear();
        int res = fc.read(buffer);
        if (res != blockSize) {
            throw new IOException("The block for the HTNode is not the right size: " + res); //$NON-NLS-1$
        }
        buffer.flip();

        /* Read the common header part */
        byte typeByte = buffer.get();
        NodeType type = NodeType.fromByte(typeByte);
        long start = buffer.getLong();
        long end = buffer.getLong();
        int seqNb = buffer.getInt();
        int parentSeqNb = buffer.getInt();
        int intervalCount = buffer.getInt();
        buffer.get(); // TODO Used to be "isDone", to be removed from the header

        /* Now the rest of the header depends on the node type */
        switch (type) {
        case CORE:
        case LEAF:
            newNode = nodeFactory.createNode(type, blockSize, maxChildren, seqNb, parentSeqNb, start);
            newNode.readSpecificHeader(buffer);
            break;

        default:
            /* Unrecognized node type */
            throw new IOException();
        }

        /*
         * At this point, we should be done reading the header and 'buffer'
         * should only have the intervals left
         */
        ISafeByteBufferReader readBuffer = SafeByteBufferFactory.wrapReader(buffer, res - buffer.position());
        for (int i = 0; i < intervalCount; i++) {
            TmfEdgeInterval interval = objectReader.readInterval(readBuffer);
            newNode.addNoCheck(interval);
        }

        /* Assign the node's other information we have read previously */
        newNode.setNodeEnd(end);
        newNode.setOnDisk();

        return newNode;
    }

    /**
     * Create a node's extra data object for the given node type
     *
     * @param type
     *            The type of node
     * @return The node's extra data object, or <code>null</code> if there is
     *         none
     */
    protected @Nullable CoreNodeData createNodeExtraData(NodeType type) {
        if (type == NodeType.CORE) {
            return new CoreNodeData(this);
        }
        return null;
    }

    /**
     * Write this node to the given file channel.
     *
     * @param fc
     *            The file channel to write to (should be sought to be correct
     *            position)
     * @throws IOException
     *             If there was an error writing
     */
    public final void writeSelf(FileChannel fc) throws IOException {
        /*
         * Yes, we are taking the *read* lock here, because we are reading the
         * information in the node to write it to disk.
         */
        fRwl.readLock().lock();
        try {
            final int blockSize = getBlockSize();

            ByteBuffer buffer = ByteBuffer.allocate(blockSize);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.clear();

            /* Write the common header part */
            buffer.put(getNodeType().toByte());
            buffer.putLong(fNodeStart);
            buffer.putLong(fNodeEnd);
            buffer.putInt(fSequenceNumber);
            buffer.putInt(fParentSequenceNumber);
            buffer.putInt(fIntervals.size());
            buffer.put((byte) 1); // TODO Used to be "isDone", to be removed
                                  // from header

            /* Now call the inner method to write the specific header part */
            writeSpecificHeader(buffer);

            /* Back to us, we write the intervals */
            fIntervals.forEach(i -> i.writeSegment(SafeByteBufferFactory.wrapWriter(buffer, i.getSizeOnDisk())));
            if (blockSize - buffer.position() != getNodeFreeSpace()) {
                throw new IllegalStateException("Wrong free space: Actual: " + (blockSize - buffer.position()) + ", Expected: " + getNodeFreeSpace()); //$NON-NLS-1$ //$NON-NLS-2$
            }
            /*
             * Fill the rest with zeros
             */
            while (buffer.position() < blockSize) {
                buffer.put((byte) 0);
            }

            /* Finally, write everything in the Buffer to disk */
            buffer.flip();
            int res = fc.write(buffer);
            if (res != blockSize) {
                throw new IllegalStateException("Wrong size of block written: Actual: " + res + ", Expected: " + blockSize); //$NON-NLS-1$ //$NON-NLS-2$
            }

        } finally {
            fRwl.readLock().unlock();
        }
        fIsOnDisk = true;
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Get this node's block size.
     *
     * @return The block size
     */
    protected final int getBlockSize() {
        return fBlockSize;
    }

    /**
     * Get this node's maximum amount of children.
     *
     * @return The maximum amount of children
     */
    protected final int getMaxChildren() {
        return fMaxChildren;
    }

    /**
     * Get the interval comparator. Intervals will always be stored sorted
     * according to this comparator. This can be used by insertion or retrieval
     * algorithms.
     *
     * Sub-classes may override this to change or specify the interval
     * comparator.
     *
     * NOTE: sub-classes who override this may also need to override the
     * {@link #getStartIndexFor(long, Predicate)}.
     *
     * @return The way intervals are to be sorted in this node
     */
    protected Comparator<TmfEdgeInterval> getIntervalComparator() {
        return fDefaultIntervalComparator;
    }

    /**
     * Get the start time of this node.
     *
     * @return The start time of this node
     */
    public long getNodeStart() {
        return fNodeStart;
    }

    /**
     * Get the end time of this node. Will return {@link Long#MAX_VALUE} if the
     * node is not yet written to disk, as the real end time is not yet known.
     *
     * @return The end time of this node.
     */
    public long getNodeEnd() {
        if (fIsOnDisk) {
            return fNodeEnd;
        }
        return Long.MAX_VALUE;
    }

    /**
     * Get the sequence number of this node.
     *
     * @return The sequence number of this node
     */
    public int getSequenceNumber() {
        return fSequenceNumber;
    }

    /**
     * Get the sequence number of this node's parent.
     *
     * @return The parent sequence number
     */
    public int getParentSequenceNumber() {
        return fParentSequenceNumber;
    }

    /**
     * Change this node's parent. Used when we create a new root node for
     * example.
     *
     * @param newParent
     *            The sequence number of the node that is the new parent
     */
    public void setParentSequenceNumber(int newParent) {
        fParentSequenceNumber = newParent;
    }

    /**
     * Return if this node is "done" (full and written to disk).
     *
     * @return If this node is done or not
     */
    public boolean isOnDisk() {
        return fIsOnDisk;
    }

    /**
     * Get the node's extra data.
     *
     * @return The node extra data
     */
    protected @Nullable CoreNodeData getCoreNodeData() {
        return fExtraData;
    }

    /**
     * Get the list of objects in this node. This list is immutable. All objects
     * must be inserted through the {@link #add(TmfEdgeInterval)} method
     *
     * @return The list of intervals in this node
     */
    protected List<TmfEdgeInterval> getIntervals() {
        return ImmutableList.copyOf(fIntervals);
    }

    /**
     * Set this node's end time. Called by the reader factory.
     *
     * @param end
     *            The end time of the node
     */
    protected void setNodeEnd(long end) {
        fNodeEnd = end;
    }

    /**
     * Set this node to be on disk. Called by the reader factory.
     */
    protected void setOnDisk() {
        fIsOnDisk = true;
    }

    /**
     * Add an interval to this node. The caller of this method must make sure that
     * there is enough space on this node to add this object. Also, it is the
     * responsibility of the caller to make sure that the element to add is
     * within the boundary of this node. No check on start and end is expected
     * to be done in this method.
     *
     * @param newInterval
     *            Interval to add to this node
     */
    public void add(TmfEdgeInterval newInterval) {
        fRwl.writeLock().lock();
        try {
            /*
             * Just in case, should be checked before even calling this function
             */
            int objSize = newInterval.getSizeOnDisk();
            if (objSize > getNodeFreeSpace()) {
                throw new IllegalArgumentException("The interval to insert (" + objSize + ") is larger than available space (" + getNodeFreeSpace() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            int insertPoint = Collections.binarySearch(fIntervals, newInterval, getIntervalComparator());
            insertPoint = (insertPoint >= 0 ? insertPoint : -insertPoint - 1);
            fIntervals.add(insertPoint, newInterval);

            fSizeOfContentSection += objSize;

        } finally {
            fRwl.writeLock().unlock();
        }
    }

    /**
     * Directly add the interval to the node, without check. This method is
     * package private because only the read method should make use of it.
     */
    void addNoCheck(TmfEdgeInterval newInterval) {
        fIntervals.add(newInterval);
    }

    /**
     * Retrieve an interval inside this node that matches the given conditions.
     *
     * @param ts
     *            The time-based RangeCondition
     * @param extraPredicate
     *            Extra predicate to run on the elements. Only intervals also
     *            matching this predicate will be returned.
     * @param tsIsExactEnd Whether the timestamp to search for represents the exact end time of an interval, in which case implementations can
     * @return An interval matching the conditions or <code>null</code> if no
     *         interval was found
     */
    public @Nullable TmfEdgeInterval getMatchingInterval(long ts, Predicate<TmfEdgeInterval> extraPredicate, boolean tsIsExactEnd) {
        if (isOnDisk()) {
            return doGetMatchingInterval(ts, extraPredicate, tsIsExactEnd);
        }

        takeReadLock();
        try {
            return doGetMatchingInterval(ts, extraPredicate, tsIsExactEnd);

        } finally {
            releaseReadLock();
        }
    }

    private @Nullable TmfEdgeInterval doGetMatchingInterval(long ts,
            Predicate<TmfEdgeInterval> extraPredicate, boolean tsIsExactEnd) {
        long lastIndex = tsIsExactEnd ? getEndIndexFor(ts) : fIntervals.size();
        for (int i = getStartIndexFor(ts, extraPredicate); i < lastIndex; i++) {
            TmfEdgeInterval curInterval = fIntervals.get(i);
            if (curInterval.getStart() <= ts && curInterval.getEnd() >= ts &&
                    extraPredicate.test(curInterval)) {
                return curInterval;
            }
        }
        return null;
    }

    private int getEndIndexFor(long target) {
        if (fIntervals.isEmpty()) {
            return 0;
        }
        /*
         * Implement our own binary search since the intervals are generic, we
         * cannot make a dummy interval and find its position
         */
        int low = 0;
        int high = fIntervals.size() - 1;

        while (low <= high) {
            // Divide the sum by 2
            int mid = (low + high) >>> 1;
            TmfEdgeInterval midVal = fIntervals.get(mid);
            long end = midVal.getEnd();

            if (end < target) {
                low = mid + 1;
            } else if (end > target) {
                high = mid - 1;
            } else {
                // key found, go forward to see where it ends
                while (end == target && mid < fIntervals.size() - 1) {
                    mid = mid + 1;
                    midVal = fIntervals.get(mid);
                    end = midVal.getEnd();
                }
                // if end == target, then mid is last index
                return (end == target ? mid + 1 : mid); // key found
            }
        }
        return high; // key not found
    }

    /**
     * Get the start index. It will skip all the intervals that end before the
     * beginning of the time range.
     *
     * NOTE: This method goes with the comparator. If a tree overrides the
     * default comparator (that sorts first by end time), then it will need to
     * override this method. Here, the binary search is re-implemented because
     * the interval type is generic but a tree for a concrete type could use a
     * binary search instead.
     *
     * @param target
     *            The time-based RangeCondition
     * @param extraPredicate
     *            Extra predicate to run on the elements.
     * @return The index of the first interval greater than or equal to the
     *         conditions in parameter
     */
    protected int getStartIndexFor(long target, Predicate<TmfEdgeInterval> extraPredicate) {
        if (fIntervals.isEmpty()) {
            return 0;
        }
        /*
         * Implement our own binary search since the intervals are generic, we
         * cannot make a dummy interval and find its position
         */
        int low = 0;
        int high = fIntervals.size() - 1;

        while (low <= high) {
            // Divide the sum by 2
            int mid = (low + high) >>> 1;
            TmfEdgeInterval midVal = fIntervals.get(mid);
            long end = midVal.getEnd();

            if (end < target) {
                low = mid + 1;
            } else if (end > target) {
                high = mid - 1;
            } else {
                // key found, rewind to see where it starts
                while (end == target && mid > 0) {
                    mid = mid - 1;
                    midVal = fIntervals.get(mid);
                    end = midVal.getEnd();
                }
                // if end == target, then mid is 0
                return (end == target ? mid : mid + 1); // key found
            }
        }
        return low; // key not found
    }

    /**
     * Transform a predicate on the intervals into a predicate to filter the
     * children nodes from their index in the {@link HTNode.CoreNodeData}.
     *
     * Typically, implementations of the tree who saves more information than
     * time in their CoreNodeData will have some Predicate classes for the
     * intervals that can be transformed in Predicate to add an additional
     * filter for the core node data.
     *
     * By default, this method should return <code>i -> true</code>.
     *
     * @param predicate
     *            The predicate on the intervals of a node
     * @return The predicate on the index in the core node data
     */
    public IntPredicate getCoreDataPredicate(Predicate<TmfEdgeInterval> predicate) {
        return ALWAYS_TRUE;
    }

    /**
     * We've received word from the containerTree that newest nodes now exist to
     * our right. (Puts isDone = true and sets the endtime)
     *
     * @param endtime
     *            The nodeEnd time that the node will have
     */
    public void closeThisNode(long endtime) {
        fRwl.writeLock().lock();
        try {
            /**
             * FIXME: was assert (endtime >= fNodeStart); but that exception is
             * reached with an empty node that has start time endtime + 1
             */
            // if (endtime < fNodeStart) {
            // throw new IllegalArgumentException("Endtime " + endtime + "
            // cannot be lower than start time " + fNodeStart);
            // }

            if (!fIntervals.isEmpty()) {
                /*
                 * Make sure there are no intervals in this node with their
                 * EndTime > the one requested. Only need to check the last one
                 * since they are sorted
                 */
                if (endtime < fIntervals.get(fIntervals.size() - 1).getEnd()) {
                    throw new IllegalArgumentException("Closing end time should be greater than or equal to the end time of the objects of this node"); //$NON-NLS-1$
                }
            }

            fNodeEnd = endtime;
        } finally {
            fRwl.writeLock().unlock();
        }
    }

    /**
     * Return the total header size of this node (will depend on the node type).
     *
     * @return The total header size
     */
    public final int getTotalHeaderSize() {
        return COMMON_HEADER_SIZE + getSpecificHeaderSize();
    }

    /**
     * @return The offset, within the node, where the Data section ends
     */
    private int getDataSectionEndOffset() {
        return getTotalHeaderSize() + fSizeOfContentSection;
    }

    /**
     * Returns the free space left in the node to write objects
     *
     * @return The amount of free space in the node (in bytes)
     */
    public int getNodeFreeSpace() {
        fRwl.readLock().lock();
        try {
            int ret = getBlockSize() - getDataSectionEndOffset();
            return ret;
        } finally {
            fRwl.readLock().unlock();
        }
    }

    /**
     * Returns the current space utilization of this node, as a percentage.
     * (used space / total usable space, which excludes the header)
     *
     * @return The percentage (value between 0 and 100) of space utilization in
     *         this node.
     */
    public long getNodeUsagePercent() {
        fRwl.readLock().lock();
        try {
            final int blockSize = getBlockSize();
            float freePercent = (float) getNodeFreeSpace()
                    / (float) (blockSize - getTotalHeaderSize())
                    * 100F;
            return (long) (100L - freePercent);

        } finally {
            fRwl.readLock().unlock();
        }
    }

    /**
     * Get the type of this node
     *
     * @return The node type
     */
    public NodeType getNodeType() {
        @Nullable
        CoreNodeData extraData = getCoreNodeData();
        if (extraData == null) {
            return NodeType.LEAF;
        }
        return NodeType.CORE;
    }

    /**
     * Return the specific header size of this node. This means the size
     * occupied by the type-specific section of the header (not counting the
     * common part).
     *
     * @return The specific header size
     */
    protected int getSpecificHeaderSize() {
        CoreNodeData extraData = fExtraData;
        if (extraData != null) {
            return extraData.getSpecificHeaderSize();
        }
        return 0;
    }

    /**
     * Read the specific header for this node
     *
     * @param buffer
     *            The buffer to read from
     */
    protected void readSpecificHeader(ByteBuffer buffer) {
        CoreNodeData extraData = fExtraData;
        if (extraData != null) {
            extraData.readSpecificHeader(buffer);
        }
    }

    /**
     * Write the type-specific part of the header in a byte buffer.
     *
     * @param buffer
     *            The buffer to write to. It should already be at the correct
     *            position.
     */
    protected void writeSpecificHeader(ByteBuffer buffer) {
        CoreNodeData extraData = fExtraData;
        if (extraData != null) {
            extraData.writeSpecificHeader(buffer);
        }
    }

    /**
     * Node-type-specific toString method. Used for debugging.
     *
     * @return A string representing the node
     */
    protected String toStringSpecific() {
        return ""; //$NON-NLS-1$
    }

    /**
     * Return whether this node has elements in it or is empty
     *
     * @return <code>true</code> if the node is empty
     */
    public boolean isEmpty() {
        return fIntervals.isEmpty();
    }

    // -------------------------------------------
    // Core node methods
    // -------------------------------------------

    /**
     * Return the number of child nodes this node has.
     *
     * @return The number of child nodes
     */
    public int getNbChildren() {
        CoreNodeData extraData = fExtraData;
        if (extraData != null) {
            return extraData.getNbChildren();
        }
        return 0;
    }

    /**
     * Get the child node corresponding to the specified index. It will throw an
     * {@link IndexOutOfBoundsException} if there is no children at this index.
     *
     * @param index
     *            The index of the child to lookup
     * @return The child node
     */
    public int getChild(int index) {
        CoreNodeData extraData = fExtraData;
        if (extraData != null) {
            return extraData.getChild(index);
        }
        throw new IndexOutOfBoundsException("This node does not have any children"); //$NON-NLS-1$
    }

    /**
     * Get the latest (right-most) child node of this node. This applies only if
     * the node is allowed to have children, ie is a {@link NodeType#CORE} node,
     * otherwise this method is not supported.
     *
     * @return The latest child node
     */
    public int getLatestChild() {
        CoreNodeData extraData = fExtraData;
        if (extraData != null) {
            return extraData.getLatestChild();
        }
        throw new UnsupportedOperationException("This node does not support children"); //$NON-NLS-1$
    }

    /**
     * Tell this node that it has a new child. This applies only if the node is
     * allowed to have children, ie is a {@link NodeType#CORE} node, otherwise
     * this method is not supported.
     *
     * @param childNode
     *            The new child node to add to this one
     */
    public void linkNewChild(HTNode childNode) {
        CoreNodeData extraData = fExtraData;
        if (extraData != null) {
            extraData.linkNewChild(childNode);
            return;
        }
        throw new UnsupportedOperationException("This node does not support children"); //$NON-NLS-1$
    }

    /**
     * Method to select the sequence numbers for the children of the current
     * node that intersect the given timestamp. Useful when navigating the tree.
     *
     * @param timeCondition
     *            The time-based range condition to choose which child is the
     *            next one
     * @return Collection of sequence numbers of the child nodes that intersect
     *         the time condition, non-null empty collection if this is a Leaf
     *         Node
     * @throws RangeException
     *             If t is out of the node's range
     */
    public Collection<Integer> selectNextChildren(TimeRangeCondition timeCondition)
            throws RangeException {
        CoreNodeData extraData = fExtraData;
        if (extraData != null) {
            return extraData.selectNextChildren(timeCondition);
        }
        return Collections.emptyList();
    }

    // -----------------------------------------
    // Locking
    // -----------------------------------------

    /**
     * Takes a read lock on the fields of this class. Each call to this method
     * should be followed by a {@link HTNode#releaseReadLock()}, in a
     * try-finally clause
     */
    protected void takeReadLock() {
        fRwl.readLock().lock();
    }

    /**
     * Releases a read lock on the fields of this class. A call to this method
     * should have been preceded by a call to {@link HTNode#takeReadLock()}
     */
    protected void releaseReadLock() {
        fRwl.readLock().unlock();
    }

    /**
     * Takes a write lock on the fields of this class. Each call to this method
     * should be followed by a {@link HTNode#releaseWriteLock()}, in a
     * try-finally clause
     */
    protected void takeWriteLock() {
        fRwl.writeLock().lock();
    }

    /**
     * Releases a write lock on the fields of this class. A call to this method
     * should have been preceded by a call to {@link HTNode#takeWriteLock()}
     */
    protected void releaseWriteLock() {
        fRwl.writeLock().unlock();
    }

    // -----------------------------------------
    // Object methods
    // -----------------------------------------

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        /* Only used for debugging, shouldn't be externalized */
        return String.format("Node #%d, %s, %s, %d intervals (%d%% used), [%d - %s]",
                fSequenceNumber,
                (fParentSequenceNumber == -1) ? "Root" : "Parent #" + fParentSequenceNumber,
                toStringSpecific(),
                fIntervals.size(),
                getNodeUsagePercent(),
                fNodeStart,
                (fIsOnDisk || fNodeEnd != 0) ? fNodeEnd : "...");
    }

    @Override
    public int hashCode() {
        return Objects.hash(fBlockSize, fMaxChildren, fNodeStart, fNodeEnd, fSequenceNumber, fParentSequenceNumber, fExtraData);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        HTNode other = (HTNode) obj;
        return (fBlockSize == other.fBlockSize &&
                fMaxChildren == other.fMaxChildren &&
                fNodeStart == other.fNodeStart &&
                fNodeEnd == other.fNodeEnd &&
                fSequenceNumber == other.fSequenceNumber &&
                fParentSequenceNumber == other.fParentSequenceNumber &&
                Objects.equals(fExtraData, other.fExtraData));
    }

}
