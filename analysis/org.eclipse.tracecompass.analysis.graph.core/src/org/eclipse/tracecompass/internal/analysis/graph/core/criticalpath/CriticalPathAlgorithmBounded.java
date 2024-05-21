/*******************************************************************************
 * Copyright (c) 2015, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.CriticalPathAlgorithmException;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.graph.TmfEdgeState;

/**
 * Critical path bounded algorithm: backward resolution of blocking limited to
 * the blocking window
 *
 * This algorithm is described in
 *
 * F. Giraldeau and M.Dagenais, Wait analysis of distributed systems using
 * kernel tracing, IEEE Transactions on Parallel and Distributed Systems
 *
 * @author Francis Giraldeau
 */
public class CriticalPathAlgorithmBounded extends AbstractCriticalPathAlgorithm {

    /**
     * Constructor
     *
     * @param graph
     *            The graph on which to calculate the critical path
     */
    public CriticalPathAlgorithmBounded(ITmfGraph graph) {
        super(graph);
    }

    /**
     * Add the links to the critical path, with currentVertex to glue to
     *
     * @param criticalPath the critical path graph
     * @param graph the execution graph
     * @param currentVertex the vertex to glue to
     * @param links the links that were resolved
     */
    protected void appendPathComponent(ITmfGraph criticalPath, ITmfGraph graph, ITmfVertex currentVertex, List<ITmfEdge> links) {
        IGraphWorker currentActor = checkNotNull(graph.getParentOf(currentVertex));
        if (links.isEmpty()) {
            /*
             * The next vertex should not be null, since we glue only after
             * resolve of the blocking of the edge to that vertex
             */
            ITmfEdge next = graph.getEdgeFrom(currentVertex, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
            if (next == null) {
                return;
            }
            criticalPath.append(criticalPath.createVertex(currentActor, next.getVertexTo().getTimestamp()), next.getEdgeContextState(), next.getLinkQualifier());
            return;
        }
        // FIXME: assert last link.to actor == currentActor

        // attach subpath to b1
        ITmfVertex b1 = checkNotNull(criticalPath.getTail(currentActor));

        // glue head
        ITmfEdge lnk = links.get(0);
        ITmfVertex anchor = null;
        IGraphWorker objSrc = checkNotNull(graph.getParentOf(lnk.getVertexFrom()));
        if (objSrc.equals(currentActor)) {
            anchor = b1;
        } else {
            anchor = criticalPath.createVertex(objSrc, currentVertex.getTimestamp());
            criticalPath.add(anchor);
            criticalPath.edge(b1, anchor);
            /* fill any gap with UNKNOWN */
            if (lnk.getVertexFrom().compareTo(anchor) > 0) {
                anchor = criticalPath.createVertex(objSrc, lnk.getVertexFrom().getTimestamp());
                checkNotNull(criticalPath.appendUnknown(anchor));
            }
        }

        // glue body
        ITmfEdge prev = null;
        for (ITmfEdge link : links) {
            // check connectivity
            if (prev != null && (!prev.getVertexTo().equals(link.getVertexFrom()))) {
                anchor = copyLink(criticalPath, graph, anchor, prev.getVertexTo(), link.getVertexFrom(),
                        Math.max(prev.getVertexTo().getTimestamp(), link.getVertexFrom().getTimestamp()),
                        null, link.getLinkQualifier());
            }
            anchor = copyLink(criticalPath, graph, anchor, link.getVertexFrom(), link.getVertexTo(),
                    link.getVertexTo().getTimestamp(), link.getEdgeContextState(), link.getLinkQualifier());
            prev = link;
        }
    }

    /**
     * Resolve a blocking by going through the graph vertically from the
     * blocking edge
     *
     * FIXME: build a tree with partial subpath in order to return the best
     * path, not the last one traversed
     *
     * @param blocking
     *            The blocking edge
     * @param bound
     *            The vertex that limits the boundary until which to resolve the
     *            blocking
     * @return The list of non-blocking edges
     */
    protected List<ITmfEdge> resolveBlockingBounded(ITmfEdge blocking, ITmfVertex bound) {

        ITmfGraph graph = getGraph();

        LinkedList<ITmfEdge> subPath = new LinkedList<>();
        ITmfVertex junction = findIncoming(blocking.getVertexTo(), ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        /* if wake-up source is not found, return empty list */
        if (junction == null) {
            return subPath;
        }

        ITmfEdge down = graph.getEdgeFrom(junction, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE);
        if (down == null) {
            return subPath;
        }
        subPath.add(down);
        ITmfVertex vertexFrom = down.getVertexFrom();

        ITmfVertex currentBound = bound.compareTo(blocking.getVertexFrom()) < 0 ? blocking.getVertexFrom() : bound;

        Deque<ITmfVertex> stack = new ArrayDeque<>();
        while (vertexFrom != null && vertexFrom.compareTo(currentBound) > 0) {
            /* shortcut for down link that goes beyond the blocking */
            ITmfEdge inVerticalEdge = graph.getEdgeFrom(vertexFrom, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE);
            if (inVerticalEdge != null && inVerticalEdge.getVertexFrom().compareTo(currentBound) <= 0) {
                subPath.add(inVerticalEdge);
                break;
            }

            /**
             * Add DOWN links to explore stack in case dead-end occurs. Here is
             * an example to illustrate the procedure.
             *
             * <pre>
             *           -------------------------
             *            BLOCKED    | PREEMPT
             *           -------------------------
             *                       ^
             *                       |WAKE-UP
             *                       |
             *         +--------------------+
             * +-------+      INTERRUPT     +--------+
             *         +--------------------+
             *           ^   ^   |       ^
             *           |   |   |       |
             *           +   +   v       +
             *           1   2   3       4
             * </pre>
             *
             * The event wake-up is followed backward. The edge 4 will never be
             * visited (it cannot be the cause of the wake-up, because it occurs
             * after it). The edge 3 will not be explored, because it is
             * outgoing. The edges 2 and 1 will be pushed on the stack. When the
             * beginning of the interrupt is reached, then the edges on the
             * stack will be explored.
             *
             * If a dead-end is reached, while the stack is not empty, the
             * accumulated path is rewinded, and a different incoming edge is
             * tried. The backward traversal ends if there is nothing left to
             * explore, or if the start of the blocking window start is reached.
             *
             * Do not add if left is BLOCKED, because this link would be visited
             * twice
             */
            ITmfEdge incomingEdge = graph.getEdgeFrom(vertexFrom, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE);
            if (inVerticalEdge != null && (incomingEdge == null || incomingEdge.getEdgeContextState().getEdgeState() != TmfEdgeState.BLOCK)) {
                stack.addFirst(vertexFrom);
            }
            if (incomingEdge != null) {
                if (incomingEdge.getEdgeContextState().getEdgeState() == TmfEdgeState.BLOCK) {
                    List<ITmfEdge> blockings = resolveBlockingBounded(incomingEdge, currentBound);
                    if (blockings.isEmpty() && incomingEdge.getEdgeContextState().isMatchable()) {
                        // There's no explanation for the blocking, keep this
                        // edge if it's network, let the algorithm stitch this
                        // otherwise
                        subPath.add(incomingEdge);
                    } else {
                        subPath.addAll(blockings);
                    }
                } else {
                    subPath.add(incomingEdge);
                }
                vertexFrom = incomingEdge.getVertexFrom();
            } else {
                if (!stack.isEmpty()) {
                    ITmfVertex v = stack.removeFirst();
                    /* rewind subpath */
                    while (!subPath.isEmpty() && subPath.getLast().getVertexFrom() != v) {
                        subPath.removeLast();
                    }
                    ITmfEdge edge = graph.getEdgeFrom(v, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE);
                    if (edge != null) {
                        subPath.add(edge);
                        vertexFrom = edge.getVertexFrom();
                        continue;
                    }
                }
                vertexFrom = null;
            }

        }
        return subPath;
    }

    @Override
    public ITmfGraph computeCriticalPath(ITmfGraph criticalPath, ITmfVertex start, @Nullable ITmfVertex end) throws CriticalPathAlgorithmException {

        /* Get the main graph from which to get critical path */
        ITmfGraph graph = getGraph();

        /*
         * Calculate path starting from the object the start vertex belongs to
         */
        IGraphWorker parent = checkNotNull(graph.getParentOf(start));
        criticalPath.add(criticalPath.createVertex(parent, start.getTimestamp()));
        ITmfVertex currentVertex = start;
        ITmfEdge nextEdge = graph.getEdgeFrom(currentVertex, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);

        long endTime = Long.MAX_VALUE;
        if (end != null) {
            endTime = end.getTimestamp();
        }

        /*
         * Run through all horizontal edges from this object and resolve each
         * blocking as they come
         */
        while (nextEdge != null) {
            ITmfVertex nextVertex = nextEdge.getVertexTo();
            if (nextVertex.getTimestamp() >= endTime) {
                break;
            }
            switch (nextEdge.getEdgeContextState().getEdgeState()) {
            case PASS:
                /**
                 * This edge is not blocked, so nothing to resolve, just add the
                 * edge to the critical path
                 */
                /**
                 * TODO: Normally, the parent of the link's vertex to should be
                 * the object itself, verify if that is true
                 */
                IGraphWorker parentTo = checkNotNull(graph.getParentOf(nextEdge.getVertexTo()));
                if (parentTo != parent) {
                    throw new CriticalPathAlgorithmException("no, the parents of horizontal edges are not always identical... shouldn't they be?"); //$NON-NLS-1$
                }
                ITmfVertex vertex = criticalPath.createVertex(parentTo, nextEdge.getVertexTo().getTimestamp());
                criticalPath.append(vertex, nextEdge.getEdgeContextState(), nextEdge.getLinkQualifier());
                break;
            case BLOCK:
                List<ITmfEdge> links = resolveBlockingBounded(nextEdge, nextEdge.getVertexFrom());
                Collections.reverse(links);
                appendPathComponent(criticalPath, graph, currentVertex, links);
                break;
            case UNKNOWN:
            default:
                throw new CriticalPathAlgorithmException("Illegal link type " + nextEdge.getEdgeContextState().getContextEnum()); //$NON-NLS-1$
            }
            currentVertex = nextVertex;
            nextEdge = graph.getEdgeFrom(currentVertex, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
            if (nextEdge != null) {
                nextEdge = null;
            }
        }
        return criticalPath;
    }

}
