/*******************************************************************************
 * Copyright (c) 2015, 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.ICriticalPathAlgorithm;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;

/**
 * Abstract class for critical path algorithms
 *
 * @author Francis Giraldeau
 */
public abstract class AbstractCriticalPathAlgorithm implements ICriticalPathAlgorithm {

    private final ITmfGraph fGraph;

    /**
     * Constructor
     *
     * @param graph
     *            The graph on which to calculate critical path
     */
    public AbstractCriticalPathAlgorithm(ITmfGraph graph) {
        fGraph = graph;
    }

    /**
     * Get the graph
     *
     * @return the graph
     */
    public ITmfGraph getGraph() {
        return fGraph;
    }

    /**
     * Copy link of type TYPE between nodes FROM and TO in the graph PATH. The
     * return value is the tail node for the new path.
     *
     * @param criticalPath
     *            The graph on which to add the link
     * @param graph
     *            The original graph on which to calculate critical path
     * @param anchor
     *            The anchor vertex from the path graph
     * @param from
     *            The origin vertex in the main graph
     * @param to
     *            The destination vertex in the main graph
     * @param ts
     *            The timestamp of the edge
     * @param type
     *            The type of the edge to create
     * @param string
     *            An optional qualifier for the link
     * @return The destination vertex in the path graph
     */
    protected ITmfVertex copyLink(ITmfGraph criticalPath, ITmfGraph graph, ITmfVertex anchor, ITmfVertex from, ITmfVertex to, long ts, ITmfEdge.EdgeType type, @Nullable String string) {
        IGraphWorker parentTo = graph.getParentOf(to);
        if (parentTo == null) {
            throw new NullPointerException();
        }
        ITmfVertex tmp = criticalPath.createVertex(parentTo, ts);
        if (tmp.equals(anchor)) {
            return anchor;
        }
        if (string == null) {
            criticalPath.edge(anchor, tmp, type);
        } else {
            criticalPath.edge(anchor, tmp, type, string);
        }
        return tmp;
    }

    /**
     * Find the next incoming vertex from another object (in vertical) from a
     * node in a given direction
     *
     * @param vertex
     *            The starting vertex
     * @param dir
     *            The direction in which to search
     * @return The next incoming vertex
     */
    public @Nullable ITmfVertex findIncoming(ITmfVertex vertex, ITmfGraph.EdgeDirection dir) {
        ITmfVertex currentVertex = vertex;
        while (true) {
            ITmfEdge incoming = fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE);
            if (incoming != null) {
                return currentVertex;
            }
            ITmfEdge edge = fGraph.getEdgeFrom(vertex, dir);
            if (edge == null || edge.getEdgeType() != ITmfEdge.EdgeType.EPS) {
                break;
            }
            currentVertex = getNeighborFromEdge(edge, dir);
        }
        return null;
    }

    private static ITmfVertex getNeighborFromEdge(ITmfEdge edge, ITmfGraph.EdgeDirection dir) {
        switch (dir) {
        case OUTGOING_VERTICAL_EDGE:
        case OUTGOING_HORIZONTAL_EDGE:
            return edge.getVertexTo();
        case INCOMING_VERTICAL_EDGE:
        case INCOMING_HORIZONTAL_EDGE:
            return edge.getVertexFrom();
        default:
            throw new IllegalStateException("Unknown direction: " + dir); //$NON-NLS-1$
        }
    }

    @Override
    public String getID() {
        return getClass().getName();
    }

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

}
