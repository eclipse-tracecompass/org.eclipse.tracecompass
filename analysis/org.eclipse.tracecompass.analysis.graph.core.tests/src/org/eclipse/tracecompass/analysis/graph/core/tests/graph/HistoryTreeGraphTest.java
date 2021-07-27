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

package org.eclipse.tracecompass.analysis.graph.core.tests.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge.EdgeType;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.graph.TmfGraphFactory;
import org.eclipse.tracecompass.analysis.graph.core.graph.WorkerSerializer;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.TestGraphWorker;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.HistoryTreeTmfGraph;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test the basic functionalities of the {@link HistoryTreeTmfGraph} class
 *
 * @author Geneviève Bastien
 * @author Francis Giraldeau
 */
public class HistoryTreeGraphTest extends ITmfGraphTest {

    private Path fGraphFile;
    private Path fGraphWorkerFiler;

    private class TestWorkerSerializer implements WorkerSerializer {

        @Override
        public @NonNull String serialize(@NonNull IGraphWorker worker) {
            return String.valueOf(((TestGraphWorker) worker).getValue());
        }

        @Override
        public @NonNull IGraphWorker deserialize(@NonNull String serializedWorker) {
            return new TestGraphWorker(Integer.decode(serializedWorker));
        }

    }

    @Before
    @Override
    public void setup() throws IOException {
        fGraphFile = Files.createTempFile("tmpGraph", ".ht");
        fGraphWorkerFiler = Files.createTempFile("tmpGraph", ".workers");
        super.setup();
    }

    @Override
    protected ITmfGraph createNewGraph() {
        return Objects.requireNonNull(TmfGraphFactory.createOnDiskGraph(fGraphFile, new TestWorkerSerializer(), 0, 1));
    }

    @Override
    protected void deleteGraph(ITmfGraph graph) throws IOException {
        Files.deleteIfExists(fGraphFile);
        Files.deleteIfExists(fGraphWorkerFiler);
    }

    /**
     * Test the graph constructor
     */
    @Test
    public void testDefaultConstructor() {
        ITmfGraph graph = TmfGraphFactory.createOnDiskGraph(fGraphFile, new TestWorkerSerializer(), 0, 1);
        assertNotNull(graph);
        Iterator<ITmfVertex> it = graph.getNodesOf(WORKER1);
        assertEquals(0, ImmutableList.copyOf(it).size());
    }

    /**
     * Test re-reading a graph from file
     */
    @Test
    public void testReReadGraph() {
        ITmfGraph graph = getGraph();

        // Start with a first node
        ITmfVertex v0 = graph.createVertex(WORKER1, 0);
        graph.add(v0);
        ITmfVertex v1 = graph.createVertex(WORKER1, 1);

        // Link with second node not in graph
        ITmfEdge edge = graph.edge(v0, v1);

        ITmfVertex v2 = graph.createVertex(WORKER1, 2);
        edge = graph.edge(v1, v2, EdgeType.NETWORK);

        ITmfVertex v3 = graph.createVertex(WORKER2, 3);
        edge = graph.edge(v2, v3, EdgeType.NETWORK);

        ITmfVertex v4 = graph.createVertex(WORKER3, 3);
        edge = graph.edge(v3, v4, EdgeType.NETWORK, "test");

        graph.closeGraph(3);
        graph = null;

        ITmfGraph reOpenedGraph = createNewGraph();
        // Assert the number of nodes per worker
        Iterator<ITmfVertex> it = reOpenedGraph.getNodesOf(WORKER1);
        assertEquals(3, ImmutableList.copyOf(it).size());
        it = reOpenedGraph.getNodesOf(WORKER2);
        assertEquals(1, ImmutableList.copyOf(it).size());
        it = reOpenedGraph.getNodesOf(WORKER3);
        assertEquals(1, ImmutableList.copyOf(it).size());

        edge = reOpenedGraph.getEdgeFrom(v0, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNotNull(edge);
        assertEquals(EdgeType.DEFAULT, edge.getEdgeType());
        assertEquals(v1, edge.getVertexTo());
        assertEquals(v0, edge.getVertexFrom());
        assertEquals(v1.getTimestamp() - v0.getTimestamp(), edge.getDuration());
        edge = reOpenedGraph.getEdgeFrom(v1, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        assertNotNull(edge);
        assertEquals(v0, edge.getVertexFrom());

        edge = reOpenedGraph.getEdgeFrom(v2, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        assertNotNull(edge);
        assertEquals(v1, edge.getVertexFrom());
        assertEquals(EdgeType.NETWORK, edge.getEdgeType());
        edge = reOpenedGraph.getEdgeFrom(v1, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNotNull(edge);
        assertEquals(v2, edge.getVertexTo());

        edge = reOpenedGraph.getEdgeFrom(v2, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE);
        assertNotNull(edge);
        assertEquals(v3, edge.getVertexTo());
        assertEquals(EdgeType.NETWORK, edge.getEdgeType());
        edge = reOpenedGraph.getEdgeFrom(v3, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE);
        assertNotNull(edge);
        assertEquals(v2, edge.getVertexFrom());

        edge = reOpenedGraph.getEdgeFrom(v3, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE);
        assertNotNull(edge);
        assertEquals(v4, edge.getVertexTo());
        edge = reOpenedGraph.getEdgeFrom(v4, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE);
        assertNotNull(edge);
        assertEquals(v3, edge.getVertexFrom());

    }

    /**
     * Test that appending vertices in non chronological order gives error
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalVertex() {
        ITmfGraph graph = getGraph();

        ITmfVertex v0 = graph.createVertex(WORKER1, 0);
        ITmfVertex v1 = graph.createVertex(WORKER1, 1);
        graph.add(v1);
        graph.add(v0);
    }

    /**
     * Test creating an edge where the from attribute is not in the graph
     */
    @Test(expected = IllegalArgumentException.class)
    public void testVertexNotInGraph() {
        ITmfGraph graph = getGraph();

        // Add vertices for worker 1
        ITmfVertex v0 = graph.createVertex(WORKER1, 0);
        ITmfVertex v1 = graph.createVertex(WORKER1, 1);
        graph.add(v0);
        graph.add(v1);

        // Add vertices for worker 1
        ITmfVertex v3 = graph.createVertex(WORKER2, 1);
        ITmfVertex v4 = graph.createVertex(WORKER2, 2);
        graph.add(v4);
        graph.edge(v3, v4, EdgeType.RUNNING);
    }

}
