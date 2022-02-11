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

package org.eclipse.tracecompass.analysis.graph.core.tests.stubs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.List;

import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;

import com.google.common.collect.ImmutableList;

/**
 * Class that implements static operations on vertices and edges. The sets of
 * nodes and vertices can then be transformed to a graph.
 *
 * @author Geneviève Bastien
 * @author Francis Giraldeau
 */
public class GraphOps {

    /**
     * Check whether 2 graphs are identical
     *
     * @param g1
     *            The first graph to compare
     * @param g2
     *            The second graph
     */
    public static void checkEquality(ITmfGraph g1, ITmfGraph g2) {
        Collection<IGraphWorker> obj1 = g1.getWorkers();
        Collection<IGraphWorker> obj2 = g2.getWorkers();
        assertEquals("Graph objects", obj1, obj2);
        for (IGraphWorker graphObject : obj1) {
            assertNotNull(graphObject);
            List<ITmfVertex> nodesOf1 = ImmutableList.copyOf(g1.getNodesOf(graphObject));
            List<ITmfVertex> nodesOf2 = ImmutableList.copyOf(g2.getNodesOf(graphObject));
            for (int i = 0; i < nodesOf1.size(); i++) {
                ITmfVertex v1 = nodesOf1.get(i);
                ITmfVertex v2 = nodesOf2.get(i);
                assertEquals("Node timestamps for " + graphObject + ", node " + i, v1.getTimestamp(), v2.getTimestamp());
                /* Check each edge */
                for (ITmfGraph.EdgeDirection dir : ITmfGraph.EdgeDirection.values()) {
                    ITmfEdge edge1 = g1.getEdgeFrom(v1, dir);
                    ITmfEdge edge2 = g2.getEdgeFrom(v2, dir);
                    if (edge1 == null) {
                        assertNull("Expected null edge for " + graphObject + ", node " + v1 + " and dir " + dir, edge2);
                        continue;
                    }
                    assertNotNull("Expected non null edge for " + graphObject + ", node " + v1 + " and dir " + dir, edge2);
                    assertEquals("Edge type for " + graphObject + ", node " + v1 + " and dir " + dir, edge1.getEdgeContextState().getContextEnum(), edge2.getEdgeContextState().getContextEnum());
                    assertEquals("Edge duration for " + graphObject + ", node " + v1 + " edge direction " + dir, edge1.getDuration(), edge2.getDuration());
                    assertEquals("From objects for " + graphObject + ", node " + v1 + " and dir " + dir, g1.getParentOf(edge1.getVertexFrom()), g2.getParentOf(edge2.getVertexFrom()));
                    assertEquals("To objects for " + graphObject + ", node " + v1 + " and dir " + dir, g1.getParentOf(edge1.getVertexTo()), g2.getParentOf(edge2.getVertexTo()));
                    assertEquals("Edge qualifier for " + graphObject + ", node " + v1 + " and dir " + dir, edge1.getLinkQualifier(), edge2.getLinkQualifier());
                }
            }
        }
    }

}
