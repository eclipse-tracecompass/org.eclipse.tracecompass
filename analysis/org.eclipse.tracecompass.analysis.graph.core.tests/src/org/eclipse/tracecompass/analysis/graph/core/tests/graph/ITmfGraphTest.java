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

package org.eclipse.tracecompass.analysis.graph.core.tests.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdgeContextState;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraphVisitor;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.TestGraphWorker;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfGraphStatistics;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState.OSEdgeContextEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test for the base {@link ITmfGraph} classes
 *
 * @author Geneviève Bastien
 */
public abstract class ITmfGraphTest {

    /** One worker, to use for the tests */
    protected static final @NonNull IGraphWorker WORKER1 = new TestGraphWorker(1);
    /** Second worker, to use for the tests */
    protected static final @NonNull IGraphWorker WORKER2 = new TestGraphWorker(2);
    /** Third worker, to use for the tests */
    protected static final @NonNull IGraphWorker WORKER3 = new TestGraphWorker(3);

    private ITmfGraph fGraph;

    /**
     * Create the graph to use for tests
     *
     * @throws IOException Exception throw by the deletion of the test
     */
    @Before
    public void setup() throws IOException {
        fGraph = createNewGraph();
    }

    /**
     * Create a new graph for the test
     *
     * @return A new graph
     * @throws IOException Exception throw by the deletion of the test
     */
    protected abstract ITmfGraph createNewGraph() throws IOException;

    /**
     * Delete the graph at the end of the test
     *
     * @throws IOException Exception throw by the deletion of the test
     */
    @After
    public void cleanup() throws IOException {
        if (fGraph != null) {
            deleteGraph(fGraph);
        }
    }

    /**
     * Ecologically dispose of the graph
     *
     * @param graph The graph to delete
     * @throws IOException Exception throw by the deletion of the test
     */
    protected abstract void deleteGraph(ITmfGraph graph) throws IOException;

    /**
     * Get the graph for the test
     *
     * @return The graph to test
     */
    protected @NonNull ITmfGraph getGraph() {
        return Objects.requireNonNull(fGraph);
    }

    /**
     * Test the {@link ITmfGraph#add(ITmfVertex)} method:
     * vertices are added, but no edge between them is created
     */
    @Test
    public void testAddVertex() {

        // Add vertices for a single worker covering the entire graph.
        ITmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        fGraph.add(v0);
        ITmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        fGraph.add(v1);
        Iterator<ITmfVertex> it = fGraph.getNodesOf(WORKER1);
        int count = 0;
        while (it.hasNext()) {
            ITmfVertex vertex = it.next();
            assertEquals("Vertext at count " + count, count, vertex.getTimestamp());
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE));
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE));
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE));
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE));
            count++;
        }
        assertEquals(2, count);

        // Add vertices for another worker later on the graph, to make sure
        // vertex count is still ok
        ITmfVertex v2 = fGraph.createVertex(WORKER2, 2);
        fGraph.add(v2);
        ITmfVertex v3 = fGraph.createVertex(WORKER2, 3);
        fGraph.add(v3);
        it = fGraph.getNodesOf(WORKER1);
        count = 0;
        while (it.hasNext()) {
            ITmfVertex vertex = it.next();
            assertEquals(count, vertex.getTimestamp());
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE));
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE));
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE));
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE));
            count++;
        }
        assertEquals(2, count);
        it = fGraph.getNodesOf(WORKER2);
        count = 0;
        while (it.hasNext()) {
            ITmfVertex vertex = it.next();
            assertEquals(count + 2, vertex.getTimestamp());
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE));
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE));
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE));
            assertNull(fGraph.getEdgeFrom(vertex, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE));
            count++;
        }
        assertEquals(2, count);
    }

    /**
     * Test the {@link ITmfGraph#append(ITmfVertex)} and
     * {@link ITmfGraph#append(ITmfVertex, ITmfEdgeContextState)} methods:
     * vertices are added and links are created between them.
     */
    @Test
    public void testAppendVertex() {

        // Add vertices for a single worker covering the entire graph.
        ITmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        ITmfEdge edge = fGraph.append(v0);
        assertNull("First edge of a worker", edge);
        ITmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        edge = fGraph.append(v1);
        assertNotNull(edge);
        assertEquals(OSEdgeContextEnum.DEFAULT, edge.getEdgeContextState().getContextEnum());
        assertEquals(v1, edge.getVertexTo());
        assertEquals(v0, edge.getVertexFrom());
        assertEquals(v1.getTimestamp() - v0.getTimestamp(), edge.getDuration());

        Iterator<@NonNull ITmfVertex> it = fGraph.getNodesOf(WORKER1);
        List<@NonNull ITmfVertex> list = ImmutableList.copyOf(it);
        assertEquals(2, list.size());
        checkLinkHorizontal(list, fGraph);

        /* Append with a type */
        ITmfVertex v2 = fGraph.createVertex(WORKER1, 2);
        edge = fGraph.append(v2, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
        assertNotNull(edge);
        assertEquals(OSEdgeContextEnum.BLOCKED, edge.getEdgeContextState().getContextEnum());
        assertEquals(v2, edge.getVertexTo());
        assertEquals(v1, edge.getVertexFrom());
        assertEquals(v2.getTimestamp() - v1.getTimestamp(), edge.getDuration());

        it = fGraph.getNodesOf(WORKER1);
        list = ImmutableList.copyOf(it);
        assertEquals(3, list.size());
        checkLinkHorizontal(list, fGraph);

    }

    /**
     * Test the {@link ITmfGraph#edge(ITmfVertex, ITmfVertex)} and
     * {@link ITmfGraph#edge(ITmfVertex, ITmfVertex, ITmfEdgeContextState)} methods
     */
    @Test
    public void testLink() {
        // Start with a first node
        ITmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        fGraph.add(v0);
        ITmfVertex v1 = fGraph.createVertex(WORKER1, 1);

        // Link with second node not in graph
        ITmfEdge edge = fGraph.edge(v0, v1);
        assertNotNull(edge);
        assertEquals(OSEdgeContextEnum.DEFAULT, edge.getEdgeContextState().getContextEnum());
        assertEquals(v1, edge.getVertexTo());
        assertEquals(v0, edge.getVertexFrom());
        assertEquals(v1.getTimestamp() - v0.getTimestamp(), edge.getDuration());

        Iterator<ITmfVertex> it = fGraph.getNodesOf(WORKER1);
        assertEquals(2, ImmutableList.copyOf(it).size());
        ITmfEdge edge1 = fGraph.getEdgeFrom(v1, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v0, edge1.getVertexFrom());
        edge1 = fGraph.getEdgeFrom(v1, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNull(edge1);
        edge1 = fGraph.getEdgeFrom(v0, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v1, edge1.getVertexTo());

        // Link with second node for the same object
        ITmfVertex v2 = fGraph.createVertex(WORKER1, 2);
        edge = fGraph.edge(v1, v2, new OSEdgeContextState(OSEdgeContextEnum.NETWORK));
        assertNotNull(edge);
        assertEquals(OSEdgeContextEnum.NETWORK, edge.getEdgeContextState().getContextEnum());
        assertEquals(v2, edge.getVertexTo());
        assertEquals(v1, edge.getVertexFrom());
        assertEquals(v2.getTimestamp() - v1.getTimestamp(), edge.getDuration());

        it = fGraph.getNodesOf(WORKER1);
        assertEquals(3, ImmutableList.copyOf(it).size());
        edge1 = fGraph.getEdgeFrom(v2, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v1, edge1.getVertexFrom());
        edge1 = fGraph.getEdgeFrom(v1, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v2, edge1.getVertexTo());

        // Link with second node for another object
        ITmfVertex v3 = fGraph.createVertex(WORKER2, 3);
        edge = fGraph.edge(v2, v3, new OSEdgeContextState(OSEdgeContextEnum.NETWORK));
        assertNotNull(edge);
        assertEquals(v3, edge.getVertexTo());
        assertEquals(v2, edge.getVertexFrom());
        assertEquals(OSEdgeContextEnum.NETWORK, edge.getEdgeContextState().getContextEnum());

        it = fGraph.getNodesOf(WORKER1);
        assertEquals(3, ImmutableList.copyOf(it).size());

        it = fGraph.getNodesOf(WORKER2);
        assertEquals(1, ImmutableList.copyOf(it).size());

        edge1 = fGraph.getEdgeFrom(v2, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v3, edge1.getVertexTo());
        edge1 = fGraph.getEdgeFrom(v3, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v2, edge1.getVertexFrom());

        // No duration vertical link with second node for another object
        ITmfVertex v4 = fGraph.createVertex(WORKER3, 3);
        edge = fGraph.edge(v3, v4, new OSEdgeContextState(OSEdgeContextEnum.NETWORK), "test");
        assertNotNull(edge);
        assertEquals(v4, edge.getVertexTo());
        assertEquals(v3, edge.getVertexFrom());
        assertEquals(OSEdgeContextEnum.NETWORK, edge.getEdgeContextState().getContextEnum());

        edge1 = fGraph.getEdgeFrom(v3, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v4, edge1.getVertexTo());
        edge1 = fGraph.getEdgeFrom(v4, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v3, edge1.getVertexFrom());

    }

    /**
     * Test the
     * {@link ITmfGraph#edgeVertical(ITmfVertex, ITmfVertex, ITmfEdgeContextState, String)}
     * method: vertices are added and links are created between them.
     */
    @Test
    public void testLinkVertical() {

        // Add vertices for a single worker covering the entire graph.
        ITmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        fGraph.add(v0);
        ITmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        fGraph.add(v1);
        ITmfEdge edge = fGraph.edgeVertical(v0, v1, new OSEdgeContextState(OSEdgeContextEnum.RUNNING), null);
        assertNotNull(edge);
        assertEquals(OSEdgeContextEnum.RUNNING, edge.getEdgeContextState().getContextEnum());
        assertEquals(v1, edge.getVertexTo());
        assertEquals(v0, edge.getVertexFrom());
        assertEquals(v1.getTimestamp() - v0.getTimestamp(), edge.getDuration());

        edge = fGraph.getEdgeFrom(v0, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE);
        assertNotNull(edge);
        edge = fGraph.getEdgeFrom(v0, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNull(edge);

        edge = fGraph.getEdgeFrom(v1, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE);
        assertNotNull(edge);
        edge = fGraph.getEdgeFrom(v1, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        assertNull(edge);

    }

    /**
     * Verify that vertices in the list form a chain linked by edges and have no
     * vertical edges
     *
     * @param graph
     */
    private static void checkLinkHorizontal(List<@NonNull ITmfVertex> list, ITmfGraph graph) {
        if (list.isEmpty()) {
            return;
        }
        for (int i = 0; i < list.size() - 1; i++) {
            ITmfVertex v0 = list.get(i);
            ITmfVertex v1 = list.get(i + 1);
            ITmfEdge edgeOut = graph.getEdgeFrom(v0, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
            assertNotNull(edgeOut);
            assertEquals(v0, edgeOut.getVertexFrom());
            assertEquals(v1, edgeOut.getVertexTo());
            ITmfEdge edgeIn = graph.getEdgeFrom(v1, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE);
            assertNotNull(edgeIn);
            assertEquals(v0, edgeIn.getVertexFrom());
            assertEquals(v1, edgeIn.getVertexTo());
            assertEquals(edgeOut.getEdgeContextState().getContextEnum(), edgeIn.getEdgeContextState().getContextEnum());
            assertNull(graph.getEdgeFrom(v1, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE));
            assertNull(graph.getEdgeFrom(v1, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE));
            assertNull(graph.getEdgeFrom(v0, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE));
            assertNull(graph.getEdgeFrom(v0, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE));
        }
    }

    /**
     * Test the {@link ITmfGraph#getTail(IGraphWorker)} methods
     */
    @Test
    public void testTail() {
        ITmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        ITmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        ITmfVertex v2 = fGraph.createVertex(WORKER2, 2);
        ITmfVertex v3 = fGraph.createVertex(WORKER2, 3);
        fGraph.append(v0);
        fGraph.append(v1);
        fGraph.append(v2);
        fGraph.append(v3);
        fGraph.closeGraph(3);
        assertEquals(v1, fGraph.getTail(WORKER1));
        assertEquals(v3, fGraph.getTail(WORKER2));
    }

    /**
     * Test the {@link ITmfGraph#getHead()} methods with 2 workers
     */
    @Test
    public void testHead() {
        ITmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        ITmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        ITmfVertex v2 = fGraph.createVertex(WORKER2, 2);
        ITmfVertex v3 = fGraph.createVertex(WORKER2, 3);
        fGraph.append(v0);
        fGraph.append(v1);
        fGraph.append(v2);
        fGraph.append(v3);
        fGraph.closeGraph(3);
        assertEquals(v0, fGraph.getHead(WORKER1));
        assertEquals(v0, fGraph.getHead(v1));
        assertEquals(v0, fGraph.getHead(v0));
        assertEquals(v2, fGraph.getHead(WORKER2));
        assertEquals(v2, fGraph.getHead(v2));
        assertEquals(v2, fGraph.getHead(v3));
    }

    /**
     * Test the {@link ITmfGraph#getHead(ITmfVertex)} methods with multiple
     * sequences of vertices
     */
    @Test
    public void testHeadSequence() {
        ITmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        ITmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        ITmfVertex v2 = fGraph.createVertex(WORKER1, 2);
        ITmfVertex v3 = fGraph.createVertex(WORKER1, 3);
        fGraph.append(v0);
        fGraph.append(v1);
        fGraph.add(v2);
        fGraph.append(v3);
        fGraph.closeGraph(3);
        assertEquals(v0, fGraph.getHead(v1));
        assertEquals(v0, fGraph.getHead(v0));
        assertEquals(v2, fGraph.getHead(v2));
        assertEquals(v2, fGraph.getHead(v3));
    }

    /**
     * The test {@link ITmfGraph#getParentOf(ITmfVertex)} method
     */
    @Test
    public void testParent() {
        ITmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        ITmfVertex v1 = fGraph.createVertex(WORKER2, 1);
        fGraph.append(v0);
        fGraph.append(v1);
        assertEquals(WORKER1, fGraph.getParentOf(v0));
        assertNotSame(WORKER1, fGraph.getParentOf(v1));
        assertEquals(WORKER2, fGraph.getParentOf(v1));
    }

    private class ScanCountVertex implements ITmfGraphVisitor {
        public int nbVertex = 0;
        public int nbVLink = 0;
        public int nbHLink = 0;
        public int nbStartVertex = 0;

        @Override
        public void visitHead(ITmfVertex node) {
            System.out.println("Start vertex " + node);
            nbStartVertex++;
        }

        @Override
        public void visit(ITmfVertex node) {
            System.out.println("Visit vertex " + node);
            nbVertex++;

        }

        @Override
        public void visit(ITmfEdge edge, boolean horizontal) {
            System.out.println("Visit edge " + edge);
            if (horizontal) {
                nbHLink++;
            } else {
                nbVLink++;
            }
        }
    }

    /**
     * The following graph will be used
     *
     * <pre>
     * ____0___1___2___3___4___5___6___7___8___9___10___11___12___13___14___15
     *
     * A   *-------*       *---*-------*---*---*    *---*----*----*---------*
     *             |           |           |            |    |
     * B       *---*---*-------*   *-------*------------*    *----------*
     * </pre>
     */
    @SuppressWarnings("null")
    private void buildFullGraph() {
        ITmfVertex[] vertexA;
        ITmfVertex[] vertexB;
        long[] timesA = { 0, 2, 4, 5, 7, 8, 9, 10, 11, 12, 13, 15 };
        long[] timesB = { 1, 2, 3, 5, 6, 8, 11, 12, 14 };
        vertexA = new ITmfVertex[timesA.length];
        vertexB = new ITmfVertex[timesB.length];
        for (int i = 0; i < timesA.length; i++) {
            vertexA[i] = fGraph.createVertex(WORKER1, timesA[i]);
        }
        for (int i = 0; i < timesB.length; i++) {
            vertexB[i] = fGraph.createVertex(WORKER2, timesB[i]);
        }
        fGraph.append(vertexA[0]);
        fGraph.append(vertexA[1]);
        fGraph.add(vertexA[2]);
        fGraph.append(vertexA[3]);
        fGraph.append(vertexA[4]);
        fGraph.append(vertexA[5]);
        fGraph.append(vertexA[6]);
        fGraph.add(vertexA[7]);
        fGraph.append(vertexA[8]);
        fGraph.append(vertexA[9]);
        fGraph.append(vertexA[10]);
        fGraph.append(vertexA[11]);
        fGraph.append(vertexB[0]);
        fGraph.append(vertexB[1]);
        fGraph.append(vertexB[2]);
        fGraph.append(vertexB[3]);
        fGraph.add(vertexB[4]);
        fGraph.append(vertexB[5]);
        fGraph.append(vertexB[6]);
        fGraph.add(vertexB[7]);
        fGraph.append(vertexB[8]);
        fGraph.edge(vertexA[1], vertexB[1]);
        fGraph.edge(vertexB[3], vertexA[3]);
        fGraph.edge(vertexA[5], vertexB[5]);
        fGraph.edge(vertexB[6], vertexA[8]);
        fGraph.edge(vertexA[9], vertexB[7]);
    }

    /**
     * Test the
     * {@link ITmfGraph#scanLineTraverse(IGraphWorker, ITmfGraphVisitor)}
     * method
     */
    @Test
    public void testScanCount() {
        buildFullGraph();
        ScanCountVertex visitor = new ScanCountVertex();
        fGraph.scanLineTraverse(fGraph.getHead(WORKER1), visitor);
        assertEquals(21, visitor.nbVertex);
        assertEquals(6, visitor.nbStartVertex);
        assertEquals(5, visitor.nbVLink);
        assertEquals(15, visitor.nbHLink);
    }

    /**
     * Test the {@link TmfGraphStatistics} class
     */
    @Test
    public void testGraphStatistics() {
        buildFullGraph();
        TmfGraphStatistics stats = new TmfGraphStatistics();
        stats.computeGraphStatistics(fGraph, WORKER1);
        assertEquals(12, stats.getSum(WORKER1).longValue());
        assertEquals(11, stats.getSum(WORKER2).longValue());
        assertEquals(23, stats.getSum().longValue());
    }

    /**
     * Test that exception is thrown if a node is linked horizontally to itself
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHorizontalSelfLink() {
        ITmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        fGraph.add(v1);
        fGraph.edge(v1, v1);
    }
}
