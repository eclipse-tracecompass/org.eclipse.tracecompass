/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.internal.analysis.graph.core.Activator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * This class abstracts inputs/outputs of the HistoryTree nodes.
 *
 * It contains all the methods and descriptors to handle reading/writing nodes
 * to the tree-file on disk and all the caching mechanisms.
 *
 * This abstraction is mainly for code isolation/clarification purposes. Every
 * HistoryTree must contain 1 and only 1 HT_IO element.
 *
 * This class is yet another copy of the classes from o.e.t.statesystem.core
 * (HT_IO, too custom) and o.e.t.datastore.core (use generics, which kill
 * triples run time with no other changes).
 *
 * @author Alexandre Montplaisir
 * @author Geneviève Bastien
 */
public class HtIo {

    private static final Logger LOGGER = TraceCompassLog.getLogger(HtIo.class);

    // ------------------------------------------------------------------------
    // Global cache of nodes
    // ------------------------------------------------------------------------

    private static final class CacheKey {

        public final HtIo fHistoryTreeIo;
        public final int fSeqNumber;

        public CacheKey(HtIo htio, int seqNumber) {
            fHistoryTreeIo = htio;
            fSeqNumber = seqNumber;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fHistoryTreeIo, fSeqNumber);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            return (fHistoryTreeIo.equals(other.fHistoryTreeIo) &&
                    fSeqNumber == other.fSeqNumber);
        }
    }

    private static final int CACHE_SIZE = 230;

    private static final LoadingCache<CacheKey, GraphTreeNode> NODE_CACHE = checkNotNull(CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .build(new CacheLoader<CacheKey, GraphTreeNode>() {
                @Override
                public GraphTreeNode load(CacheKey key) throws IOException {
                    HtIo io = key.fHistoryTreeIo;
                    int seqNb = key.fSeqNumber;

                    TraceCompassLogUtils.traceInstant(LOGGER, Level.FINEST, "HtIo:CacheMiss", "seqNum", seqNb); //$NON-NLS-1$ //$NON-NLS-2$

                    synchronized (io) {
                        io.seekFCToNodePos(io.fFileChannelIn, seqNb);
                        return HTNode.readNode(io.fBlockSize,
                                io.fNodeMaxChildren,
                                io.fFileChannelIn,
                                io.fEdgeIntervalReader,
                                io.fNodeFactory);
                    }
                }
            }));

    /**
     * This method invalidates all data in the cache so nodes will have to be
     * read again
     */
    @VisibleForTesting
    static void clearCache() {
        NODE_CACHE.invalidateAll();
    }

    /**
     * Get whether a node is present in the cache
     *
     * @param htio
     *            The htio object that contains the node
     * @param seqNum
     *            The sequence number of the node to check
     * @return <code>true</code> if the node is present in the cache,
     *         <code>false</code> otherwise
     */
    @VisibleForTesting
    static boolean isInCache(HtIo htio, int seqNum) {
        @Nullable
        HTNode present = NODE_CACHE.getIfPresent(new CacheKey(htio, seqNum));
        return (present != null);
    }

    // ------------------------------------------------------------------------
    // Instance fields
    // ------------------------------------------------------------------------

    /* Relevant configuration elements from the History Tree */
    private final File fStateHistoryFile;
    private final int fBlockSize;
    private final int fNodeMaxChildren;
    private final HTNode.IHTNodeFactory<GraphTreeNode> fNodeFactory;
    private final IHTIntervalReader<TmfEdgeInterval> fEdgeIntervalReader;

    /* Fields related to the file I/O */
    private final FileInputStream fFileInputStream;
    private final FileOutputStream fFileOutputStream;
    private final FileChannel fFileChannelIn;
    private final FileChannel fFileChannelOut;

    // ------------------------------------------------------------------------
    // Methods
    // ------------------------------------------------------------------------

    /**
     * Standard constructor
     *
     * @param stateHistoryFile
     *            The name of the history file
     * @param blockSize
     *            The size of each "block" on disk in bytes. One node will
     *            always fit in one block. It should be at least 4096.
     * @param nodeMaxChildren
     *            The maximum number of children allowed per core (non-leaf)
     *            node.
     * @param newFile
     *            Flag indicating that the file must be created from scratch
     * @param nodeFactory
     *            The factory to create new nodes for this tree
     * @param edgeIntervalReader
     *            The reader to deserialize the edge intervals written on disk
     * @throws IOException
     *             An exception can be thrown when file cannot be accessed
     */
    public HtIo(File stateHistoryFile,
            int blockSize,
            int nodeMaxChildren,
            boolean newFile,
            HTNode.IHTNodeFactory<GraphTreeNode> nodeFactory,
            IHTIntervalReader<TmfEdgeInterval> edgeIntervalReader) throws IOException {

        fBlockSize = blockSize;
        fNodeMaxChildren = nodeMaxChildren;
        fNodeFactory = nodeFactory;
        fEdgeIntervalReader = edgeIntervalReader;

        fStateHistoryFile = stateHistoryFile;
        if (newFile) {
            boolean success1 = true;
            /* Create a new empty History Tree file */
            if (fStateHistoryFile.exists()) {
                success1 = fStateHistoryFile.delete();
            }
            boolean success2 = fStateHistoryFile.createNewFile();
            if (!(success1 && success2)) {
                /* It seems we do not have permission to create the new file */
                throw new IOException("Cannot create new file at " + //$NON-NLS-1$
                        fStateHistoryFile.getName());
            }
            fFileInputStream = new FileInputStream(fStateHistoryFile);
            fFileOutputStream = new FileOutputStream(fStateHistoryFile, false);
        } else {
            /*
             * We want to open an existing file, make sure we don't squash the
             * existing content when opening the fos!
             */
            fFileInputStream = new FileInputStream(fStateHistoryFile);
            fFileOutputStream = new FileOutputStream(fStateHistoryFile, true);
        }
        fFileChannelIn = fFileInputStream.getChannel();
        fFileChannelOut = fFileOutputStream.getChannel();
    }

    /**
     * Read a node from the file on disk.
     *
     * @param seqNumber
     *            The sequence number of the node to read.
     * @return The object representing the node
     * @throws ClosedChannelException
     *             Usually happens because the file was closed while we were
     *             reading. Instead of using a big reader-writer lock, we'll
     *             just catch this exception.
     */
    public GraphTreeNode readNode(int seqNumber) throws ClosedChannelException {
        /* Do a cache lookup. If it's not present it will be loaded from disk */
        TraceCompassLogUtils.traceInstant(LOGGER, Level.FINEST, "HtIo:CacheLookup", "seqNum", seqNumber); //$NON-NLS-1$ //$NON-NLS-2$
        CacheKey key = new CacheKey(this, seqNumber);
        try {
            return checkNotNull(NODE_CACHE.get(key));

        } catch (ExecutionException e) {
            /* Get the inner exception that was generated */
            Throwable cause = e.getCause();
            if (cause instanceof ClosedChannelException) {
                throw (ClosedChannelException) cause;
            }
            /*
             * Other types of IOExceptions shouldn't happen at this point
             * though.
             */
            Activator.getInstance().logError(e.getMessage(), e);
            throw new IllegalStateException();
        }
    }

    /**
     * Write the given node to disk.
     *
     * @param node
     *            The node to write.
     */
    public void writeNode(GraphTreeNode node) {
        try {
            int seqNumber = node.getSequenceNumber();

            /* "Write-back" the node into the cache */
            CacheKey key = new CacheKey(this, seqNumber);
            NODE_CACHE.put(key, node);

            /* Position ourselves at the start of the node and write it */
            synchronized (this) {
                seekFCToNodePos(fFileChannelOut, seqNumber);
                node.writeSelf(fFileChannelOut);
            }
        } catch (IOException e) {
            /* If we were able to open the file, we should be fine now... */
            Activator.getInstance().logError(e.getMessage(), e);
        }
    }

    /**
     * Get the output file channel, used for writing, positioned after a certain
     * number of nodes, or at the beginning.
     *
     * FIXME: Do not expose the file output. Use rather a method to
     * writeAtEnd(int nodeOffset, ByteBuffer)
     *
     * @param nodeOffset
     *            The offset in the file, in number of nodes. If the value is
     *            lower than 0, the file will be positioned at the beginning.
     * @return The correctly-seeked input stream
     */
    public FileOutputStream getFileWriter(int nodeOffset) {
        try {
            if (nodeOffset < 0) {
                fFileChannelOut.position(0);
            } else {
                seekFCToNodePos(fFileChannelOut, nodeOffset);
            }
        } catch (IOException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
        return fFileOutputStream;
    }

    /**
     * Retrieve the input stream with which to write the attribute tree.
     *
     * FIXME: Do not expose the stream, have a method to write at the end
     * instead
     *
     * @param nodeOffset
     *            The offset in the file, in number of nodes. This should be
     *            after all the nodes.
     * @return The correctly-seeked input stream
     */
    public FileInputStream supplyATReader(int nodeOffset) {
        try {
            /*
             * Position ourselves at the start of the Mapping section in the
             * file (which is right after the Blocks)
             */
            seekFCToNodePos(fFileChannelIn, nodeOffset);
        } catch (IOException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
        return fFileInputStream;
    }

    /**
     * Close all file channels and streams.
     */
    public synchronized void closeFile() {
        try {
            fFileInputStream.close();
            fFileOutputStream.close();
        } catch (IOException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
    }

    /**
     * Delete the history tree file
     */
    public synchronized void deleteFile() {
        closeFile();

        if (!fStateHistoryFile.delete()) {
            /* We didn't succeed in deleting the file */
            Activator.getInstance().logError("Failed to delete" + fStateHistoryFile.getName()); //$NON-NLS-1$
        }
    }

    /**
     * Seek the given FileChannel to the position corresponding to the node that
     * has seqNumber
     *
     * @param fc
     *            the channel to seek
     * @param seqNumber
     *            the node sequence number to seek the channel to
     * @throws IOException
     *             If some other I/O error occurs
     */
    private void seekFCToNodePos(FileChannel fc, int seqNumber)
            throws IOException {
        /*
         * Cast to (long) is needed to make sure the result is a long too and
         * doesn't get truncated
         */
        fc.position(4096
                + ((long) seqNumber) * fBlockSize);
    }

}
