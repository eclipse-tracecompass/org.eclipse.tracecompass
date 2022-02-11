/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.graph;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

/**
 * Interface for undirected, unweighed, timed graph data type for dependencies
 * between elements of a system.
 *
 * Vertices are timed: each vertex has a timestamp associated, and the vertex
 * belongs to an object at a given time.
 *
 * @author Geneviève Bastien
 * @since 4.0
 */
public interface ITmfGraph {

    /**
     * Describe the four edge directions coming in and out of a vertex
     */
    enum EdgeDirection {
        /**
         * Constant for the outgoing vertical edge (to other object)
         */
        OUTGOING_VERTICAL_EDGE,
        /**
         * Constant for the incoming vertical edge (from other object)
         */
        INCOMING_VERTICAL_EDGE,
        /**
         * Constant for the outgoing horizontal edge (to same object)
         */
        OUTGOING_HORIZONTAL_EDGE,
        /**
         * Constant for the incoming horizontal edge (from same object)
         */
        INCOMING_HORIZONTAL_EDGE
    }

    /**
     * Create a vertex for a worker on this graph
     *
     * @param worker
     *            The worker for which to create the vertex
     * @param timestamp
     *            The timestamp of the new vertex
     * @return The new vertex
     */
    ITmfVertex createVertex(IGraphWorker worker, long timestamp);

    /**
     * Add vertex to the provided object without creating an edge
     *
     * @param vertex
     *            The new vertex
     */
    void add(ITmfVertex vertex);

    /**
     * Add the vertex to the graph and make horizontal link with the latest
     * vertex of this worker with an unknown type
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @param vertex
     *            The new vertex
     * @param type
     *            The type of edge to create
     * @return The edge constructed
     */
    @Nullable ITmfEdge appendUnknown(ITmfVertex vertex);

    /**
     * Add the vertex to the graph and make horizontal link with the latest
     * vertex of this worker
     *
     * @param vertex
     *            The new vertex
     * @return The edge constructed
     */
    @Nullable ITmfEdge append(ITmfVertex vertex);

    /**
     * Add the vertex to the graph and make horizontal link with the latest
     * vertex of this worker
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @param vertex
     *            The new vertex
     * @param type
     *            The type of edge to create
     * @return The edge constructed
     */
    @Nullable ITmfEdge append(ITmfVertex vertex, ITmfEdgeContextState type);

    /**
     * Add the vertex to the graph and make horizontal link with the latest
     * vertex of this worker
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @param vertex
     *            The new vertex
     * @param type
     *            The type of edge to create
     * @param linkQualifier
     *            An optional qualifier to identify this link
     * @return The edge constructed
     */
    @Nullable ITmfEdge append(ITmfVertex vertex, ITmfEdgeContextState type, @Nullable String linkQualifier);

    /**
     * Add an edge between two vertices of the graph. The from vertex must be in
     * the graph. If the 'to' vertex is not in the graph, it will be appended to
     * the worker it belongs to. Otherwise a vertical or horizontal link will be
     * created between the vertices, depending if the vertices are from the same
     * worker or not.
     *
     * Caution: If a link already exist in the corresponding direction, the
     * behavior is implementation dependent. Ideally, this method should not be
     * called twice for a same vertex.
     *
     * @param from
     *            The source vertex
     * @param to
     *            The destination vertex
     * @return The newly created edge
     */
    @Nullable ITmfEdge edgeUnknown(ITmfVertex from, ITmfVertex to);

    /**
     * Add an edge between two vertices of the graph. The from vertex must be in
     * the graph. If the 'to' vertex is not in the graph, it will be appended to
     * the worker it belongs to. Otherwise a vertical or horizontal link will be
     * created between the vertices, depending if the vertices are from the same
     * worker or not.
     *
     * Caution: If a link already exist in the corresponding direction, the
     * behavior is implementation dependent. Ideally, this method should not be
     * called twice for a same vertex.
     *
     * @param from
     *            The source vertex
     * @param to
     *            The destination vertex
     * @return The newly created edge
     */
    @Nullable ITmfEdge edge(ITmfVertex from, ITmfVertex to);

    /**
     * Add an edge between two vertices of the graph. The from vertex must be in
     * the graph. If the 'to' vertex is not in the graph, it will be appended to
     * the worker it belongs to. Otherwise a vertical or horizontal link will be
     * created between the vertices, depending if the vertices are from the same
     * worker or not.
     *
     * Caution: If a link already exist in the corresponding direction, the
     * behavior is implementation dependent. Ideally, this method should not be
     * called twice for a same vertex.
     *
     * @param from
     *            The source vertex
     * @param to
     *            The destination vertex
     * @param type
     *            The type of edge to create
     * @return The newly created edge
     */
    @Nullable ITmfEdge edge(ITmfVertex from, ITmfVertex to, ITmfEdgeContextState type);

    /**
     * Add an edge between two vertices of the graph. The from vertex must be in
     * the graph. If the 'to' vertex is not in the graph, it will be appended to
     * the worker it belongs to. Otherwise a vertical or horizontal link will be
     * created between the vertices, depending if the vertices are from the same
     * worker or not.
     *
     * Caution: If a link already exist in the corresponding direction, the
     * behavior is implementation dependent. Ideally, this method should not be
     * called twice for a same vertex.
     *
     * @param from
     *            The source vertex
     * @param to
     *            The destination vertex
     * @param type
     *            The type of edge to create
     * @param linkQualifier
     *            An optional qualifier to identify this link
     * @return The newly created edge
     */
    @Nullable ITmfEdge edge(ITmfVertex from, ITmfVertex to, ITmfEdgeContextState type, String linkQualifier);

    /**
     * Add a vertical edge between two vertices of the graph. This method adds a
     * link in the vertical direction, even if the 2 vertices are from the same
     * worker. Both vertices should be in the graph already, otherwise an
     * IllegalArgumentException will be thrown
     *
     * @param from
     *            The source vertex
     * @param to
     *            The destination vertex
     * @param type
     *            The type of edge to create
     * @param linkQualifier
     *            An optional qualifier to identify this link
     * @return The newly created edge
     */
    @Nullable ITmfEdge edgeVertical(ITmfVertex from, ITmfVertex to, ITmfEdgeContextState type, @Nullable String linkQualifier);

    /**
     * Returns tail vertex of the provided worker
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @return The last vertex of obj
     */
    @Nullable ITmfVertex getTail(IGraphWorker worker);

    /**
     * Returns head vertex of the provided worker. This is the very first vertex
     * of a worker
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @return The head vertex
     */
    @Nullable ITmfVertex getHead(IGraphWorker worker);

    /**
     * Return the head vertex of the first worker in the graph.
     *
     * @return The head vertex of the first worker in the graph
     */
    @Nullable ITmfVertex getHead();

    /**
     * Returns head vertex from a given vertex. That is the first of the current
     * sequence of edges, the one with no incoming horizontal edge when going
     * back through the original vertex's left edge
     *
     * @param vertex
     *            The vertex for which to get the head
     * @return The head vertex from the requested vertex
     */
    ITmfVertex getHead(ITmfVertex vertex);

    /**
     * Returns an iterator of all vertices of the provided worker
     *
     * @param obj
     *            The key of the object the vertex belongs to
     * @return An iterator of vertices for the object
     */
    Iterator<ITmfVertex> getNodesOf(IGraphWorker obj);

    /**
     * Returns the object the vertex belongs to
     *
     * @param vertex
     *            The vertex to get the parent for
     * @return The object the vertex belongs to
     */
    @Nullable IGraphWorker getParentOf(ITmfVertex vertex);

    /**
     * Returns the graph workers
     *
     * @return The collection of workers
     */
    Collection<IGraphWorker> getWorkers();

    // ----------------------------------------------
    // Graph operations and visits
    // ----------------------------------------------

    /**
     * Visits a graph from the start vertex and every vertex of the graph having
     * a path to/from them that intersects the start vertex
     *
     * Each time the worker changes, it goes back to the beginning of the
     * current horizontal sequence and visits all nodes from there.
     *
     * Parts of the graph that are totally disjoints from paths to/from start
     * will not be visited by this method
     *
     * @param start
     *            The vertex to start the scan for
     * @param visitor
     *            The visitor
     */
    default void scanLineTraverse(final @Nullable ITmfVertex start, final ITmfGraphVisitor visitor) {
        if (start == null) {
            return;
        }
        Deque<ITmfVertex> stack = new ArrayDeque<>();
        HashSet<ITmfVertex> visited = new HashSet<>();
        stack.add(start);
        while (!stack.isEmpty()) {
            ITmfVertex curr = stack.removeFirst();
            if (visited.contains(curr)) {
                continue;
            }
            // process one line
            ITmfVertex n = getHead(curr);
            visitor.visitHead(n);
            while (true) {
                visitor.visit(n);
                visited.add(n);

                // Only visit links up-right, guarantee to visit once only
                ITmfEdge edge = getEdgeFrom(n, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE);
                if (edge != null) {
                    stack.addFirst(edge.getVertexTo());
                    visitor.visit(edge, false);
                }
                edge = getEdgeFrom(n, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE);
                if (edge != null) {
                    stack.addFirst(edge.getVertexFrom());
                }
                edge = getEdgeFrom(n, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                if (edge != null) {
                    visitor.visit(edge, true);
                    n = edge.getVertexTo();
                } else {
                    // end of the horizontal list
                    break;
                }
            }
        }
    }

    /**
     * @see #scanLineTraverse(ITmfVertex, ITmfGraphVisitor)
     *
     * @param start
     *            The worker from which to start the scan
     * @param visitor
     *            The visitor
     */
    default void scanLineTraverse(@Nullable IGraphWorker start, final ITmfGraphVisitor visitor) {
        if (start == null) {
            return;
        }
        scanLineTraverse(getHead(start), visitor);
    }

    /**
     * Return the vertex for an object at a given timestamp, or the first vertex
     * after the timestamp
     *
     * @param startTime
     *            The desired time
     * @param worker
     *            The object for which to get the vertex
     * @return Vertex at timestamp or null if no vertex at or after timestamp
     */
    @Nullable ITmfVertex getVertexAt(ITmfTimestamp startTime, IGraphWorker worker);

    /**
     * Returns whether the graph is completed or not
     *
     * @return whether the graph is done building
     */
    boolean isDoneBuilding();

    /**
     * Method called when the graph is done building
     *
     * @param endTime
     *            The time at which the graph should be closed
     */
    void closeGraph(long endTime);

    /**
     * Get the edge
     *
     * @param vertex
     *            The vertex to/from which the edge comes from
     * @param direction
     *            The direction of the requested edge
     * @return The edge from the vertex to the direction.
     */
    @Nullable ITmfEdge getEdgeFrom(ITmfVertex vertex, ITmfGraph.EdgeDirection direction);

    /**
     * Method to dispose of this graph, called when the backend should be
     * closed, to free the resources used by the graph
     */
    void dispose();

}
