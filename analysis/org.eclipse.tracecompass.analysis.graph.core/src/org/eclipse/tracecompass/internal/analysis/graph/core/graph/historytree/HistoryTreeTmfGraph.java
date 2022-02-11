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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdgeContextState;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.graph.WorkerSerializer;
import org.eclipse.tracecompass.internal.analysis.graph.core.Activator;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.Messages;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.TmfEdgeInterval.EdgeIntervalType;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * ITmfGraph implementation saving the graph in a history-tree like structure
 *
 * @author Geneviève Bastien
 */
public abstract class HistoryTreeTmfGraph implements ITmfGraph {

    /**
     * Benchmarks have been run with different values of max children and
     * block_size and those values seem a good compromise
     */
    public static final int MAX_CHILDREN = 50;
    /**
     * Good compromise for the benchmark that have been run
     */
    public static final int BLOCK_SIZE = 64 * 1024;
    /* "Magic number" for file sections */
    private static final int WORKER_SECTION_MAGIC_NUMBER = 0x02BEEF66;

    /**
     * The history tree that sits underneath.
     */
    private final GraphHistoryTree fSht;

    /** Indicates if the history tree construction is done */
    private volatile boolean fFinishedBuilding = false;
    private BiMap<IGraphWorker, Integer> fWorkerAttrib = HashBiMap.create();
    // Maps a state system attribute to the timestamp of the latest vertex
    private Map<Integer, TmfVertex> fCurrentWorkerLatestTime = new HashMap<>();
    private int fCount = 0;
    private final long fStartTime;
    private final WorkerSerializer fWorkerSerializer;

    /**
     * Constructor for new history files. Use this when creating a new history
     * from scratch.
     *
     * @param newStateFile
     *            The filename/location where to store the state history (Should
     *            end in .ht)
     * @param version
     *            The version number of the reader/writer
     * @param workerSerializer
     *            The worker serializer object for this graph
     * @param startTime
     *            The start time of this graph
     * @throws IOException
     *             Thrown if we can't create the file for some reason
     */
    public HistoryTreeTmfGraph(Path newStateFile, int version, WorkerSerializer workerSerializer, long startTime) throws IOException {
        fWorkerSerializer = workerSerializer;
        fSht = createHistoryTree(newStateFile, version, startTime);
        fStartTime = startTime;
    }

    /**
     * Create a history tree from an existing file
     *
     * @param treeFile
     *            Filename/location of the history we want to load
     * @param version
     *            The version number of the reader/writer
     * @param startTime
     *            The start time of the trace
     * @return The new history tree
     * @throws IOException
     *             If we can't read the file, if it doesn't exist, is not
     *             recognized, or if the version of the file does not match the
     *             expected providerVersion.
     */
    protected abstract GraphHistoryTree createHistoryTree(Path treeFile, int version, long startTime) throws IOException;

    /**
     * Get the History Tree built by this backend.
     *
     * @return The history tree
     */
    public GraphHistoryTree getSHT() {
        return Objects.requireNonNull(fSht);
    }

    /**
     * Tell the SHT that all segments have been inserted and to write latest
     * branch to disk.
     *
     * @param endTime
     *            the time at which to close latest branch and tree
     */
    public void finishedBuilding(long endTime) {
        getSHT().closeTree(endTime);
        fFinishedBuilding = true;
    }

    /**
     * @param finishedBuilding boolean indicating the building state
     */
    protected void setFinishedBuilding(boolean finishedBuilding) {
        fFinishedBuilding = finishedBuilding;
    }

    /**
     * delete the SHT files from disk
     */
    public void removeFiles() {
        getSHT().deleteFile();
    }

    /**
     * Return the size of the history tree file
     *
     * @return The current size of the history file in bytes
     */
    public long getFileSize() {
        return getSHT().getFileSize();
    }

    protected BiMap<IGraphWorker, Integer> getWorkerAttrib() {
        return fWorkerAttrib;
    }

    @Override
    public ITmfVertex createVertex(IGraphWorker worker, long timestamp) {
        Integer attribute = fWorkerAttrib.computeIfAbsent(worker, (graphWorker) -> fCount++);
        return new TmfVertex(timestamp, attribute);
    }

    private void checkCurrentWorkerTime(TmfVertex vertex) {
        ITmfVertex latest = fCurrentWorkerLatestTime.get(vertex.getWorkerId());
        if (latest != null && latest.getTimestamp() > vertex.getTimestamp()) {
            throw new IllegalArgumentException("Vertex is earlier than latest time for this worker"); //$NON-NLS-1$
        }
    }

    @Override
    public void add(ITmfVertex vertex) {
        if (!(vertex instanceof TmfVertex)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        checkCurrentWorkerTime((TmfVertex) vertex);

        int attribute = ((TmfVertex) vertex).getWorkerId();
        // Type of edge is NO_EDGE for this edge
        TmfVertex latestVertex = fCurrentWorkerLatestTime.get(attribute);
        if (latestVertex != null && vertex.getTimestamp() == latestVertex.getTimestamp()) {
            // Do not add a vertex for a same timestamp.
            return;
        }
        if (latestVertex == null && vertex.getTimestamp() != fStartTime) {
            getSHT().insert(TmfEdgeInterval.fillerEdge((TmfVertex) vertex, fStartTime));
        } else if (latestVertex != null) {
            getSHT().insert(TmfEdgeInterval.nullEdge(latestVertex, (TmfVertex) vertex));
        }
        fCurrentWorkerLatestTime.put(attribute, (TmfVertex) vertex);
    }

    @Override
    public @Nullable ITmfEdge append(ITmfVertex vertex, ITmfEdgeContextState contextState) {
        return append(vertex, contextState, StringUtils.EMPTY);
    }

    @Override
    public @Nullable ITmfEdge append(ITmfVertex vertex, ITmfEdgeContextState contextState, @Nullable String linkQualifier) {
        if (!(vertex instanceof TmfVertex)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        checkCurrentWorkerTime((TmfVertex) vertex);
        int attribute = ((TmfVertex) vertex).getWorkerId();
        TmfVertex latestVertex = fCurrentWorkerLatestTime.get(attribute);
        if (latestVertex != null && vertex.getTimestamp() == latestVertex.getTimestamp()) {
            // Do not append a vertex for a same timestamp.
            return null;
        }
        fCurrentWorkerLatestTime.put(attribute, (TmfVertex) vertex);
        TmfEdgeInterval edge = latestVertex != null ? TmfEdgeInterval.horizontalEdge(latestVertex, (TmfVertex) vertex, contextState, linkQualifier)
                : fStartTime < vertex.getTimestamp() ? TmfEdgeInterval.fillerEdge((TmfVertex) vertex, fStartTime) : null;
        if (edge != null) {
            getSHT().insert(edge);
        }
        return edge != null ? edge.getEdge() : null;
    }

    @Override
    public @Nullable ITmfEdge getEdgeFrom(ITmfVertex node, ITmfGraph.EdgeDirection direction) {
        if (!(node instanceof TmfVertex)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        TmfVertex vertex = (TmfVertex) node;
        TmfEdgeInterval edgeInterval = null;
        try {
            GraphHistoryTree backend = getSHT();
            switch (direction) {
            case INCOMING_HORIZONTAL_EDGE: {
                if (vertex.getTimestamp() == backend.getTreeStart()) {
                    return null;
                }
                edgeInterval = backend.queryEdgeTo(vertex, true);
                break;
            }
            case INCOMING_VERTICAL_EDGE: {
                edgeInterval = backend.queryEdgeTo(vertex, false);
                break;
            }
            case OUTGOING_HORIZONTAL_EDGE: {
                edgeInterval = backend.queryEdgeFrom(vertex, true);
                break;
            }
            case OUTGOING_VERTICAL_EDGE: {
                edgeInterval = backend.queryEdgeFrom(vertex, false);
                break;
            }
            default:
                break;

            }

        } catch (ClosedChannelException e) {
            // Nothing to do, graph is closed
        }
        return edgeInterval == null ? null : edgeInterval.getEdge();
    }

    @Override
    public @Nullable ITmfEdge edge(ITmfVertex from, ITmfVertex to, ITmfEdgeContextState contextState) {
        return edge(from, to, contextState, StringUtils.EMPTY);
    }

    @Override
    public @Nullable ITmfEdge edge(ITmfVertex from, ITmfVertex to, ITmfEdgeContextState contextState, String linkQualifier) {
        if (!(from instanceof TmfVertex) || !(to instanceof TmfVertex)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        TmfVertex fromVertex = (TmfVertex) from;
        TmfVertex toVertex = (TmfVertex) to;

        if (from.equals(to)) {
            throw new IllegalArgumentException("A node cannot link to itself"); //$NON-NLS-1$
        }

        // Are vertexes in the graph? From should be, to can be added
        if (!isVertexInGraph(from)) {
            throw new IllegalArgumentException(Messages.TmfGraph_FromNotInGraph);
        }
        boolean toInGraph = isVertexInGraph(to);
        // Are they from the same worker? Add horizontal link
        if (fromVertex.getWorkerId() == toVertex.getWorkerId()) {
            return append(to, contextState, linkQualifier);
        }
        // From different workers, add a vertical link
        if (!toInGraph) {
            add(to);
        }
        TmfEdgeInterval edge = TmfEdgeInterval.verticalEdge(fromVertex, toVertex, contextState, linkQualifier);
        getSHT().insert(edge);
        return edge.getEdge();
    }

    @Override
    public @Nullable ITmfEdge edgeVertical(ITmfVertex from, ITmfVertex to, ITmfEdgeContextState contextState, @Nullable String linkQualifier) {
        if (!(from instanceof TmfVertex) || !(to instanceof TmfVertex)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        TmfVertex fromVertex = (TmfVertex) from;
        TmfVertex toVertex = (TmfVertex) to;

        if (from.equals(to)) {
            throw new IllegalArgumentException("A node cannot link to itself"); //$NON-NLS-1$
        }

        // Are vertexes in the graph? From should be, to can be added
        if (!isVertexInGraph(from) || !(isVertexInGraph(to))) {
            throw new IllegalArgumentException("One of the nodes in the vertical link is not in the graph"); //$NON-NLS-1$
        }

        TmfEdgeInterval edge = TmfEdgeInterval.verticalEdge(fromVertex, toVertex, contextState, linkQualifier);
        getSHT().insert(edge);
        return edge.getEdge();
    }

    private boolean isVertexInGraph(ITmfVertex vertex) {
        if (!(vertex instanceof TmfVertex)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }

        TmfVertex ts = fCurrentWorkerLatestTime.get(((TmfVertex) vertex).getWorkerId());
        if (ts == null || vertex.getTimestamp() > ts.getTimestamp()) {
            return false;
        }
        if (vertex.getTimestamp() == ts.getTimestamp()) {
            return true;
        }
        TmfEdgeInterval edge = null;
        try {
            edge = getSHT().queryVertex((TmfVertex) vertex);
        } catch (ClosedChannelException e) {
            // Nothing to do, graph is closed
        }
        return edge != null;
    }

    @Override
    public @Nullable ITmfVertex getTail(IGraphWorker worker) {
        Integer attribute = fWorkerAttrib.get(worker);
        if (attribute == null) {
            return null;
        }
        GraphHistoryTree backend = getSHT();
        try {
            long currentEndTime = backend.getTreeEnd();
            TmfEdgeInterval interval = backend.queryEdgeFrom(new TmfVertex(currentEndTime, attribute), true);
            TmfEdge edge = interval == null ? null : interval.getEdge();
            if (edge == null && interval != null && interval.getIntervalType() == EdgeIntervalType.FILLER) {
                return new TmfVertex(interval.getStart(), interval.getFromWorkerId());
            }
            // NOT Ok, it's not necessarily the end of the vertex
            return edge == null ? fCurrentWorkerLatestTime.get(attribute) : edge.getVertexTo();
        } catch (ClosedChannelException e) {
            // Nothing to do, graph is closed
        }
        return null;
    }

    @Override
    public @Nullable ITmfVertex getHead(IGraphWorker worker) {
        Integer attribute = fWorkerAttrib.get(worker);
        if (attribute == null) {
            return null;
        }
        return getHeadForAttribute(attribute);
    }

    @Override
    public @Nullable ITmfVertex getHead() {
        return getHeadForAttribute(0);
    }

    @Override
    public ITmfVertex getHead(ITmfVertex vertex) {
        ITmfVertex headVertex = vertex;
        ITmfEdge edgeFrom = getEdgeFrom(vertex, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        while (edgeFrom != null) {
            headVertex = edgeFrom.getVertexFrom();
            edgeFrom = getEdgeFrom(headVertex, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        }
        return headVertex;
    }

    private @Nullable ITmfVertex getHeadForAttribute(int attribute) {
        try {
            TmfEdgeInterval edge = getSHT().queryEdgeFrom(new TmfVertex(fStartTime, attribute), true);
            if (edge != null && edge.getIntervalType() == EdgeIntervalType.FILLER) {
                return edge.getVertexTo();
            }
            return edge != null ? Objects.requireNonNull(edge.getEdge()).getVertexFrom() : null;
        } catch (ClosedChannelException e) {
            // Nothing to do, graph is closed
        }
        return null;
    }

    @Override
    public Iterator<ITmfVertex> getNodesOf(IGraphWorker worker) {
        GraphHistoryTree backend = getSHT();
        Integer attribute = fWorkerAttrib.get(worker);
        if (attribute == null) {
            return Objects.requireNonNull(Collections.emptyIterator());
        }

        long currentEndTime = fFinishedBuilding ? backend.getTreeEnd() : backend.getTreeEnd() + 1;
        return new Iterator<ITmfVertex>() {

            private boolean fGotFirst = false;
            private @Nullable TmfEdgeInterval fCurrentInterval = null;
            private long nextQueryTime = backend.getTreeStart();

            @Override
            public boolean hasNext() {
                if (fCurrentInterval != null) {
                    return true;
                }
                if (nextQueryTime > currentEndTime) {
                    return false;
                }
                try {
                    TmfEdgeInterval edge = backend.queryEdgeFrom(new TmfVertex(nextQueryTime, attribute), true);
                    fCurrentInterval = edge;
                    // false if edge is null or if not the first and the edge is
                    // the end filler
                    return edge != null && !(fGotFirst && edge.getIntervalType() == EdgeIntervalType.FILLER);
                } catch (TimeRangeException | ClosedChannelException e) {
                    // Nothing to do, graph is closed
                }
                return false;
            }

            @Override
            public ITmfVertex next() {
                hasNext();
                TmfEdgeInterval next = fCurrentInterval;
                if (next == null) {
                    throw new NoSuchElementException();
                }
                if (!fGotFirst) {
                    fGotFirst = true;
                    if (next.getIntervalType() != EdgeIntervalType.FILLER) {
                        // First interval is not null, so we return a vertex at
                        // the start time, and keep the interval for next query
                        return next.getVertexFrom();
                    }
                }
                fCurrentInterval = null;
                nextQueryTime = Math.min(currentEndTime, next.getVertexTo().getTimestamp());
                return next.getVertexTo();
            }

        };
    }

    @Override
    public @Nullable IGraphWorker getParentOf(ITmfVertex node) {
        if (!(node instanceof TmfVertex)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        return fWorkerAttrib.inverse().get(((TmfVertex) node).getWorkerId());
    }

    @Override
    public Set<IGraphWorker> getWorkers() {
        return fWorkerAttrib.keySet();
    }

    @Override
    public @Nullable ITmfVertex getVertexAt(ITmfTimestamp startTime, IGraphWorker worker) {
        try {
            TmfVertex vertex = (TmfVertex) createVertex(worker, startTime.toNanos());
            TmfEdgeInterval edge = getSHT().queryVertex(vertex);
            return edge != null ? ((edge.getVertexFrom().equals(vertex)) ? edge.getVertexFrom() : edge.getVertexTo()) : null;
        } catch (ClosedChannelException e) {
            // Nothing to do, graph is closed
        }
        return null;
    }

    @Override
    public boolean isDoneBuilding() {
        return fFinishedBuilding;
    }

    @Override
    public void closeGraph(long endTime) {
        // Close the graph and write the workers
        GraphHistoryTree backend = getSHT();
        for (Integer attribute : fWorkerAttrib.values()) {
            TmfVertex latest = fCurrentWorkerLatestTime.get(attribute);
            if (latest != null && latest.getTimestamp() != endTime) {
                backend.insert(TmfEdgeInterval.fillerEdge(latest, endTime));
            }
        }
        backend.closeTree(endTime);

        // Write the workers
        writeWorkers(backend);
        fFinishedBuilding = true;
    }

    private void writeWorkers(GraphHistoryTree backend) {
        File file = backend.supplyATWriterFile();
        long pos = backend.supplyATWriterFilePos();

        try (FileOutputStream fos = new FileOutputStream(file, true);
                FileChannel fc = fos.getChannel();) {
            fc.position(pos);
            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {

                /* Write the almost-magic number */
                oos.writeInt(WORKER_SECTION_MAGIC_NUMBER);
                oos.writeInt(fWorkerAttrib.size());

                /* Compute the serialized list of attributes and write it */
                for (Entry<IGraphWorker, Integer> entry : fWorkerAttrib.entrySet()) {
                    oos.writeInt(entry.getValue());
                    oos.writeObject(fWorkerSerializer.serialize(entry.getKey()));
                }
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Error writing the file " + file, e); //$NON-NLS-1$
        }
    }

    /**
     * Read the workers that have been stored on disk.
     *
     * @param sht the state history tree that is storing the graph.
     * @throws IOException If the the workers are not readable (invalid, corrupted or unrecognizable)
     */
    @SuppressWarnings("resource") /* ois must not be closed there, because the {@link GraphHistoryTree}
        exposes the file input stream, which is not supposed to be modified. */
    protected void readWorkers(GraphHistoryTree sht) throws IOException {
        FileInputStream wris = sht.supplyATReader();
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(wris));
        /* Read the header of the Attribute Tree file (or file section) */
        int res = ois.readInt(); /* Magic number */
        if (res != WORKER_SECTION_MAGIC_NUMBER) {
            throw new IOException("The graph worker file section is either invalid or corrupted."); //$NON-NLS-1$
        }
        int workerCount = ois.readInt();

        for (int i = 0; i < workerCount; i++) {
            try {
                int workerAttrib = ois.readInt();
                String serializedWorker = (String) ois.readObject();
                IGraphWorker worker = fWorkerSerializer.deserialize(serializedWorker);
                fWorkerAttrib.put(worker, workerAttrib);
            } catch (ClassNotFoundException e) {
                throw new IOException("Unrecognizable attribute list"); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void dispose() {
        if (fFinishedBuilding) {
            getSHT().closeFile();
        } else {
            /*
             * The build is being interrupted, delete the file we partially
             * built since it won't be complete, so shouldn't be re-used in the
             * future (.deleteFile() will close the file first)
             */
            getSHT().deleteFile();
        }
    }

    /**
     * Return the average node usage as a percentage (between 0 and 100)
     *
     * @param sampleSize
     *            The number of nodes to sample (useful for large HT files). Use
     *            a value of 0 to calculate the size of all nodes
     *
     * @return Average node usage %
     */
    public int getAverageNodeUsage(int sampleSize) {
        GraphTreeNode node;
        long total = 0;
        long ret;

        int nodeCount = getSHT().getNodeCount();
        int step = 1;
        if (sampleSize >= 1) {
            step = Math.max(1, nodeCount / sampleSize);
        }
        try {
            for (int seq = 0; seq < nodeCount; seq += step) {
                node = getSHT().readNode(seq);
                total += node.getNodeUsagePercent();
            }
        } catch (ClosedChannelException e) {
            // Nothing to do
            System.out.println("In the catch of Closed channel exception");
        }

        ret = total / nodeCount;
        /* The return value should be a percentage */
        if (ret < 0 || ret > 100) {
            throw new IllegalStateException("Average node usage is not a percentage: " + ret); //$NON-NLS-1$
        }
        return (int) ret;
    }

}
