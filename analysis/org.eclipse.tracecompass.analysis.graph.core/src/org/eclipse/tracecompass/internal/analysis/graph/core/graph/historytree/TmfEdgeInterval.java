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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.graph.IEdgeContextStateFactory;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdgeContextState;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.datastore.core.interval.IHTInterval;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.datastore.core.serialization.SafeByteBufferFactory;

/**
 * The edge object to save on disk. The start and end time of the edges, along
 * with the from/to worker IDs represent the from/to vertices of the edge.
 *
 * @author Geneviève Bastien
 */
public abstract class TmfEdgeInterval implements IHTInterval {

    /**
     * The type of edge interval that can be used for various queries.
     * package-private to share with the tree nodes.
     */
    enum EdgeIntervalType {
        NULL, VERTICAL, HORIZONTAL, FILLER
    }

    /**
     * Edge interval reader to deserialize intervals on disk. package-private to
     * share with the tree nodes.
     */
    static final TmfEdgeInterval readBuffer(ISafeByteBufferReader buffer, IEdgeContextStateFactory contextStateFactory) {
        byte b = buffer.get();
        switch (b) {
        case 0: {
            return new TmfNullEdgeInterval(buffer.getLong(), buffer.getLong(), buffer.getInt());
        }
        case 1: {
            return new TmfHorizontalEdgeInterval(buffer.getLong(), buffer.getLong(), buffer.getInt(), contextStateFactory.createContextState(buffer.getInt()), buffer.getString());
        }
        case 2: {
            return new TmfVerticalEdgeInterval(buffer.getLong(), buffer.getLong(), buffer.getInt(), buffer.getInt(), contextStateFactory.createContextState(buffer.getInt()), buffer.getString());
        }
        case 3: {
            return new TmfFillerInterval(buffer.getLong(), buffer.getLong(), buffer.getInt());
        }
        default:
            throw new IllegalArgumentException("Type of data " + b + " cannot be read"); //$NON-NLS-1$ //$NON-NLS-2$
        }

    }

    /**
     * Create a new null edge between 2 vertices. A null edge means there is no
     * transition between the 2 vertices, but the object it belongs to was
     * active nevertheless. Both vertices should be for the same object.
     *
     * @param vertexFrom
     *            Vertex from which there is no horizontal transition
     * @param vertexTo
     *            New vertex for the object.
     * @return A {@link TmfEdgeInterval} object of the proper sub-type
     */
    public static TmfEdgeInterval nullEdge(TmfVertex vertexFrom, TmfVertex vertexTo) {
        return new TmfNullEdgeInterval(vertexFrom.getTimestamp(), vertexTo.getTimestamp(), vertexFrom.getWorkerId());
    }

    /**
     * Create a new horizontal edge between 2 vertices, with a transition type
     * and possible edgeQualifier. Horizontal edges must be between vertices
     * from the same worker.
     *
     * @param vertexFrom
     *            Vertex from which there is no horizontal transition
     * @param vertexTo
     *            New vertex for the object.
     * @param edgeType
     *            The type of this edge
     * @param edgeQualifier
     *            A qualifier for this link, can be <code>null</code>
     * @return A {@link TmfEdgeInterval} object of the proper sub-type
     */
    public static TmfEdgeInterval horizontalEdge(TmfVertex vertexFrom, TmfVertex vertexTo, ITmfEdgeContextState contextState, @Nullable String edgeQualifier) {
        return new TmfHorizontalEdgeInterval(vertexFrom.getTimestamp(), vertexTo.getTimestamp(), vertexFrom.getWorkerId(), contextState, edgeQualifier);
    }

    /**
     * Create a new vertical edge between 2 vertices, with a transition type and
     * possible edgeQualifier. A vertical edge can be from different worker
     * objects or the same
     *
     * @param vertexFrom
     *            Vertex from which there is no horizontal transition
     * @param vertexTo
     *            New vertex for the object.
     * @param edgeType
     *            The type of this edge
     * @param edgeQualifier
     *            A qualifier for this link, can be <code>null</code>
     * @return A {@link TmfEdgeInterval} object of the proper sub-type
     */
    public static TmfEdgeInterval verticalEdge(TmfVertex vertexFrom, TmfVertex vertexTo, ITmfEdgeContextState contextState, @Nullable String edgeQualifier) {
        return new TmfVerticalEdgeInterval(vertexFrom.getTimestamp(), vertexTo.getTimestamp(), vertexFrom.getWorkerId(), vertexTo.getWorkerId(), contextState, edgeQualifier);
    }

    /**
     * Create a new interval object between a vertex and a timestamp. The filler
     * interval means the object this interval belongs to had to life during
     * that time, typically at the beginning or end of a worker's life cycle.
     *
     * @param vertex
     *            Vertex to/from which the interval should go.
     * @param otherTime
     *            The time of the other end of this interval. If earlier than
     *            vertex, it will add a filler interval at the starting from the
     *            otherTime to the time of the vertex, otherwise, the filler
     *            edge will cover the vertex to the otherTime.
     * @return A {@link TmfEdgeInterval} object of the proper sub-type
     */
    public static TmfEdgeInterval fillerEdge(TmfVertex vertex, long otherTime) {
        return new TmfFillerInterval(Math.min(vertex.getTimestamp(), otherTime), Math.max(vertex.getTimestamp(), otherTime), vertex.getWorkerId());
    }

    private final long fStart;
    /**
     * End time of the interval
     */
    protected long fEnd;
    private int fWorkerId;

    /**
     * Constructor
     *
     * @param start
     *            Start time of this edge
     * @param end
     *            End time of this edge
     * @param workerId
     *            The attribute this edge is for, start attribute if the edge is
     *            vertical
     */
    protected TmfEdgeInterval(long start, long end, int workerId) {
        fStart = start;
        fEnd = end;
        fWorkerId = workerId;
    }

    @Override
    public long getStart() {
        return fStart;
    }

    @Override
    public long getEnd() {
        return fEnd;
    }

    /**
     * Get an edge object for this edge interval
     *
     * @return An edge object, or <code>null</code> if this interval represents
     *         the absence of an edge.
     */
    public abstract @Nullable TmfEdge getEdge();

    /**
     * Get the type of edge interval
     *
     * @return The type of edge interval
     */
    public abstract EdgeIntervalType getIntervalType();

    /**
     * Get the worker ID this edge starts from.
     *
     * @return The edge origin worker ID.
     */
    public int getFromWorkerId() {
        return fWorkerId;
    }

    /**
     * Get the worker ID this is goes to. By default, it is the same as the
     * origin worker ID.
     *
     * @return The edge destination worker ID
     */
    public int getToWorkerId() {
        return fWorkerId;
    }

    /**
     * Get the vertex this edge starts from
     *
     * @return The vertex from
     */
    public ITmfVertex getVertexFrom() {
        return new TmfVertex(getStart(), getFromWorkerId());
    }

    /**
     * Get the vertex this edge ends at
     *
     * @return The vertex to
     */
    public ITmfVertex getVertexTo() {
        return new TmfVertex(getEnd(), getToWorkerId());
    }

    /**
     * Update the edge's destination.
     *
     * @param vertexTo
     *            The new end vertex
     */
    protected abstract void updateVertexTo(TmfVertex vertexTo);

    private static class TmfNullEdgeInterval extends TmfEdgeInterval {

        public TmfNullEdgeInterval(long start, long end, int attribute) {
            super(start, end, attribute);
        }

        @Override
        public int getSizeOnDisk() {
            return Byte.BYTES + 2 * Long.BYTES + Integer.BYTES;
        }

        @Override
        public void writeSegment(ISafeByteBufferWriter buffer) {
            buffer.put((byte) 0);
            buffer.putLong(getStart());
            buffer.putLong(getEnd());
            buffer.putInt(getFromWorkerId());
        }

        @Override
        public @Nullable TmfEdge getEdge() {
            return null;
        }

        @Override
        public EdgeIntervalType getIntervalType() {
            return EdgeIntervalType.NULL;
        }

        @Override
        protected void updateVertexTo(TmfVertex vertexTo) {
            throw new IllegalArgumentException("Can't update a null edge"); //$NON-NLS-1$
        }

        @Override
        public String toString() {
            return "Null edge [" + getStart() + ',' + getEnd() + "] for " + getFromWorkerId(); //$NON-NLS-1$//$NON-NLS-2$
        }
    }

    private static class TmfHorizontalEdgeInterval extends TmfEdgeInterval {

        protected final ITmfEdgeContextState fContextState;
        protected final @Nullable String fQualifier;

        public TmfHorizontalEdgeInterval(long start, long end, int attributeFrom, ITmfEdgeContextState contextState, @Nullable String linkQualifier) {
            super(start, end, attributeFrom);
            fContextState = contextState;
            fQualifier = linkQualifier;
        }

        @Override
        public int getSizeOnDisk() {
            return Byte.BYTES + Long.BYTES * 2 + Integer.BYTES * 2 + SafeByteBufferFactory.getStringSizeInBuffer(getQualifier());
        }

        protected String getQualifier() {
            String linkQualifier = fQualifier;
            return linkQualifier == null ? StringUtils.EMPTY : linkQualifier;
        }

        @Override
        public void writeSegment(ISafeByteBufferWriter buffer) {
            buffer.put((byte) 1);
            buffer.putLong(getStart());
            buffer.putLong(getEnd());
            buffer.putInt(getFromWorkerId());
            buffer.putInt(fContextState.serialize());
            buffer.putString(getQualifier());
        }

        @Override
        public @Nullable TmfEdge getEdge() {
            String qualifier = fQualifier;
            TmfVertex fromVertex = new TmfVertex(getStart(), getFromWorkerId());
            TmfVertex toVertex = new TmfVertex(getEnd(), getToWorkerId());
            return (qualifier == null || qualifier.isEmpty()) ? new TmfEdge(fromVertex, toVertex, fContextState) : new TmfEdge(fromVertex, toVertex, fContextState, qualifier);
        }

        @Override
        public EdgeIntervalType getIntervalType() {
            return EdgeIntervalType.HORIZONTAL;
        }

        @Override
        protected void updateVertexTo(TmfVertex vertexTo) {
            if (vertexTo.getWorkerId() != getFromWorkerId()) {
                throw new IllegalArgumentException("Can't update end vertex to another attribute, the edge is not horizontal anymore"); //$NON-NLS-1$
            }
            fEnd = vertexTo.getTimestamp();
        }

        @Override
        public String toString() {
            return "Horizontal edge [" + getStart() + ',' + getEnd() //$NON-NLS-1$
                    + "] from " + getFromWorkerId() //$NON-NLS-1$
                    + ": " + fContextState + (fQualifier != null ? '(' + fQualifier + ')' : StringUtils.EMPTY); //$NON-NLS-1$
        }

    }

    private static class TmfVerticalEdgeInterval extends TmfHorizontalEdgeInterval {

        private int fAttributeTo;

        public TmfVerticalEdgeInterval(long start, long end, int attributeFrom, int attributeTo, ITmfEdgeContextState contextState, @Nullable String linkQualifier) {
            super(start, end, attributeFrom, contextState, linkQualifier);
            fAttributeTo = attributeTo;
        }

        @Override
        public int getSizeOnDisk() {
            return Byte.BYTES + Long.BYTES * 2 + Integer.BYTES * 3 + SafeByteBufferFactory.getStringSizeInBuffer(getQualifier());
        }

        @Override
        public void writeSegment(ISafeByteBufferWriter buffer) {
            buffer.put((byte) 2);
            buffer.putLong(getStart());
            buffer.putLong(getEnd());
            buffer.putInt(getFromWorkerId());
            buffer.putInt(fAttributeTo);
            buffer.putInt(fContextState.serialize());
            buffer.putString(getQualifier());
        }

        @Override
        public EdgeIntervalType getIntervalType() {
            return EdgeIntervalType.VERTICAL;
        }

        @Override
        public int getToWorkerId() {
            return fAttributeTo;
        }

        @Override
        protected void updateVertexTo(TmfVertex vertexTo) {
            fEnd = vertexTo.getTimestamp();
            fAttributeTo = vertexTo.getWorkerId();
        }

        @Override
        public String toString() {
            return "Vertical edge [" + getStart() + ',' + getEnd() //$NON-NLS-1$
                    + "] from " + getFromWorkerId() //$NON-NLS-1$
                    + " to " + getToWorkerId() //$NON-NLS-1$
                    + ": " + fContextState + (fQualifier != null ? '(' + fQualifier + ')' : StringUtils.EMPTY); //$NON-NLS-1$
        }

    }

    private static class TmfFillerInterval extends TmfEdgeInterval {

        public TmfFillerInterval(long start, long end, int attribute) {
            super(start, end, attribute);
        }

        @Override
        public int getSizeOnDisk() {
            return Byte.BYTES + 2 * Long.BYTES + Integer.BYTES;
        }

        @Override
        public void writeSegment(ISafeByteBufferWriter buffer) {
            buffer.put((byte) 3);
            buffer.putLong(getStart());
            buffer.putLong(getEnd());
            buffer.putInt(getFromWorkerId());
        }

        @Override
        public @Nullable TmfEdge getEdge() {
            return null;
        }

        @Override
        public EdgeIntervalType getIntervalType() {
            return EdgeIntervalType.FILLER;
        }

        @Override
        protected void updateVertexTo(TmfVertex vertexTo) {
            throw new IllegalArgumentException("Can't update a filler edge."); //$NON-NLS-1$
        }

        @Override
        public String toString() {
            return "Start or end filler edge [" + getStart() + ',' + getEnd() + "] for " + getFromWorkerId(); //$NON-NLS-1$//$NON-NLS-2$
        }

    }

}
