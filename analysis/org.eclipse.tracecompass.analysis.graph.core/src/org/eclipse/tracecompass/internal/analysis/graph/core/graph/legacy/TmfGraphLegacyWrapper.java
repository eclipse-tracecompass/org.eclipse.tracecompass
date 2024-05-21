/*******************************************************************************
 * Copyright (c) 2022, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdgeContextState;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.TmfEdge;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.TmfVertex;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

/**
 * A wrapper around the legacy graph classes. Still useful for in-memory graphs
 * that do not require writing to disk, for very small traces.
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("deprecation")
public class TmfGraphLegacyWrapper implements ITmfGraph {

    /* Latch tracking if the graph is done building or not */
    private final CountDownLatch fFinishedLatch = new CountDownLatch(1);
    private final TmfGraph fGraph;

    /**
     * Constructor
     *
     * @param graph
     *            The legacy graph object
     */
    public TmfGraphLegacyWrapper(TmfGraph graph) {
        fGraph = graph;
    }

    /**
     * Constructor
     */
    public TmfGraphLegacyWrapper() {
        fGraph = new TmfGraph();
    }

    @Override
    public ITmfVertex createVertex(IGraphWorker worker, long timestamp) {
        TmfVertex tmfVertex = new TmfVertex(timestamp);
        return new TmfVertexLegacyWrapper(worker, tmfVertex);
    }

    @Override
    public void add(ITmfVertex vertex) {
        if (!(vertex instanceof TmfVertexLegacyWrapper)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        TmfVertexLegacyWrapper legacyVertex = (TmfVertexLegacyWrapper) vertex;
        fGraph.add(legacyVertex.getWorker(), legacyVertex.getVertex());
    }

    @Override
    public @Nullable ITmfEdge appendUnknown(ITmfVertex vertex) {
        return append(vertex, new OSEdgeContextState(TmfEdge.EdgeType.UNKNOWN), null);
    }

    @Override
    public @Nullable ITmfEdge append(ITmfVertex vertex) {
        return append(vertex, new OSEdgeContextState(TmfEdge.EdgeType.DEFAULT), null);
    }

    @Override
    public @Nullable ITmfEdge append(ITmfVertex vertex, ITmfEdgeContextState contextState) {
        if (!(vertex instanceof TmfVertexLegacyWrapper)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        return append(vertex, contextState, null);
    }

    @Override
    public @Nullable ITmfEdge append(ITmfVertex vertex, ITmfEdgeContextState contextState, @Nullable String linkQualifier) {
        if (!(vertex instanceof TmfVertexLegacyWrapper)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        TmfVertexLegacyWrapper legacyVertex = (TmfVertexLegacyWrapper) vertex;
        TmfEdge edge = (linkQualifier == null) ? fGraph.append(legacyVertex.getWorker(),
                legacyVertex.getVertex(),
                TmfEdgeLegacyWrapper.newTypeToOldType((OSEdgeContextState) contextState))
                : fGraph.append(legacyVertex.getWorker(),
                        legacyVertex.getVertex(),
                        TmfEdgeLegacyWrapper.newTypeToOldType((OSEdgeContextState) contextState),
                        linkQualifier);
        return (edge == null) ? null : new TmfEdgeLegacyWrapper(edge, new TmfVertexLegacyWrapper(legacyVertex.getWorker(), edge.getVertexFrom()), legacyVertex);
    }

    @Override
    public @Nullable ITmfEdge edgeUnknown(ITmfVertex from, ITmfVertex to) {
        return edge(from, to, new OSEdgeContextState(TmfEdge.EdgeType.UNKNOWN));
    }

    @Override
    public @Nullable ITmfEdge edge(ITmfVertex from, ITmfVertex to) {
        return edge(from, to, new OSEdgeContextState(TmfEdge.EdgeType.DEFAULT));
    }

    @Override
    public @Nullable ITmfEdge edge(ITmfVertex from, ITmfVertex to, ITmfEdgeContextState contextState) {
        return edge(from, to, contextState, null);
    }

    @Override
    public @Nullable ITmfEdge edge(ITmfVertex from, ITmfVertex to, ITmfEdgeContextState contextState, @Nullable String linkQualifier) {
        if (!(from instanceof TmfVertexLegacyWrapper) || !(to instanceof TmfVertexLegacyWrapper)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        TmfVertexLegacyWrapper fromVertex = (TmfVertexLegacyWrapper) from;
        TmfVertexLegacyWrapper toVertex = (TmfVertexLegacyWrapper) to;

        if (from.equals(to)) {
            throw new IllegalArgumentException("A node cannot link to itself"); //$NON-NLS-1$
        }
        // Add the to vertex if it is not in the graph already
        IGraphWorker parentOf = fGraph.getParentOf(toVertex.getVertex());
        if (parentOf == null) {
            fGraph.add(toVertex.getWorker(), toVertex.getVertex());
        }
        TmfEdge link = (linkQualifier == null) ? fGraph.link(fromVertex.getVertex(), toVertex.getVertex(), TmfEdgeLegacyWrapper.newTypeToOldType((OSEdgeContextState) contextState))
                : fGraph.link(fromVertex.getVertex(), toVertex.getVertex(), TmfEdgeLegacyWrapper.newTypeToOldType((OSEdgeContextState) contextState), linkQualifier);
        return new TmfEdgeLegacyWrapper(link, fromVertex, toVertex);
    }

    @Override
    public @Nullable ITmfEdge edgeVertical(ITmfVertex from, ITmfVertex to, ITmfEdgeContextState contextState, @Nullable String linkQualifier) {
        if (!(from instanceof TmfVertexLegacyWrapper) || !(to instanceof TmfVertexLegacyWrapper)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        TmfVertexLegacyWrapper fromVertex = (TmfVertexLegacyWrapper) from;
        TmfVertexLegacyWrapper toVertex = (TmfVertexLegacyWrapper) to;

        if (from.equals(to)) {
            throw new IllegalArgumentException("A node cannot link to itself"); //$NON-NLS-1$
        }
        return new TmfEdgeLegacyWrapper(fromVertex.getVertex().linkVertical(toVertex.getVertex(), TmfEdgeLegacyWrapper.newTypeToOldType((OSEdgeContextState) contextState), linkQualifier), fromVertex, toVertex);
    }

    @Override
    public @Nullable ITmfVertex getTail(IGraphWorker worker) {
        TmfVertex tail = fGraph.getTail(worker);
        return tail == null ? null : new TmfVertexLegacyWrapper(worker, tail);
    }

    @Override
    public @Nullable ITmfVertex getHead(IGraphWorker worker) {
        TmfVertex head = fGraph.getHead(worker);
        return head == null ? null : new TmfVertexLegacyWrapper(worker, head);
    }

    @Override
    public @Nullable ITmfVertex getHead() {
        TmfVertex head = fGraph.getHead();
        return head == null ? null : new TmfVertexLegacyWrapper(Objects.requireNonNull(fGraph.getParentOf(head)), head);
    }

    @Override
    public ITmfVertex getHead(ITmfVertex vertex) {
        if (!(vertex instanceof TmfVertexLegacyWrapper)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        TmfVertex head = fGraph.getHead(((TmfVertexLegacyWrapper) vertex).getVertex());
        IGraphWorker parent = fGraph.getParentOf(head);
        if (parent == null) {
            throw new NullPointerException("Parent of vertex should not be null"); //$NON-NLS-1$
        }
        return new TmfVertexLegacyWrapper(parent, head);
    }

    @Override
    public Iterator<ITmfVertex> getNodesOf(IGraphWorker obj) {
        List<ITmfVertex> nodesOf = fGraph.getNodesOf(obj).stream()
                .map(lg -> new TmfVertexLegacyWrapper(obj, lg))
                .collect(Collectors.toList());

        return Objects.requireNonNull(nodesOf.iterator());
    }

    @Override
    public @Nullable IGraphWorker getParentOf(ITmfVertex vertex) {
        if (!(vertex instanceof TmfVertexLegacyWrapper)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        TmfVertexLegacyWrapper legacyVertex = (TmfVertexLegacyWrapper) vertex;
        return fGraph.getParentOf(legacyVertex.getVertex());
    }

    @Override
    public Set<IGraphWorker> getWorkers() {
        return fGraph.getWorkers();
    }

    @Override
    public @Nullable ITmfVertex getVertexAt(ITmfTimestamp startTime, IGraphWorker worker) {
        TmfVertex vertexAt = fGraph.getVertexAt(startTime, worker);
        return vertexAt == null ? null : new TmfVertexLegacyWrapper(worker, vertexAt);
    }

    @Override
    public boolean isDoneBuilding() {
        return fFinishedLatch.getCount() == 0;
    }

    @Override
    public void closeGraph(long endTime) {
        fFinishedLatch.countDown();
    }

    @Override
    public @Nullable ITmfEdge getEdgeFrom(ITmfVertex vertex, ITmfGraph.EdgeDirection direction) {
        if (!(vertex instanceof TmfVertexLegacyWrapper)) {
            throw new IllegalArgumentException("Wrong vertex class"); //$NON-NLS-1$
        }
        TmfVertexLegacyWrapper legacyVertex = (TmfVertexLegacyWrapper) vertex;
        TmfEdge edge = legacyVertex.getVertex().getEdge(TmfVertexLegacyWrapper.newDirectionToOldDirection(direction));
        if (edge == null) {
            return null;
        }
        switch (direction) {
        case INCOMING_HORIZONTAL_EDGE: // Fall-through
        case INCOMING_VERTICAL_EDGE:
            TmfVertex vertexFrom = edge.getVertexFrom();
            return new TmfEdgeLegacyWrapper(edge, new TmfVertexLegacyWrapper(Objects.requireNonNull(fGraph.getParentOf(vertexFrom)), vertexFrom), legacyVertex);
        case OUTGOING_HORIZONTAL_EDGE: // Fall-through
        case OUTGOING_VERTICAL_EDGE:
            TmfVertex vertexTo = edge.getVertexTo();
            return new TmfEdgeLegacyWrapper(edge, legacyVertex, new TmfVertexLegacyWrapper(Objects.requireNonNull(fGraph.getParentOf(vertexTo)), vertexTo));
        default:
            break;
        }
        return null;
    }

    @Override
    public void dispose() {
        // Nothing to do
    }

}
