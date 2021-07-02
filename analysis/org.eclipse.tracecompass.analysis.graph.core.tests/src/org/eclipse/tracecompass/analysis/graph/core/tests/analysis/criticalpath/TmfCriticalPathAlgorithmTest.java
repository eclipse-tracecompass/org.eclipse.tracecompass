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

package org.eclipse.tracecompass.analysis.graph.core.tests.analysis.criticalpath;

import static org.junit.Assert.assertNotNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.GraphBuilder;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.GraphFactory;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.GraphOps;
import org.junit.Test;

/**
 * Abstract class to test the critical path algorithms
 *
 * @author Geneviève Bastien
 * @author Francis Giraldeau
 */
public abstract class TmfCriticalPathAlgorithmTest {

    /**
     * Computes the critical path using a specific algorithm
     *
     * @param graph
     *            The execution graph on which to calculate the critical path
     * @param start
     *            The start vertex from which to calculate the path
     * @return The computed critical path with the tested algorithm
     */
    protected abstract ITmfGraph computeCriticalPath(ITmfGraph graph, @NonNull ITmfVertex start);

    /**
     * Get the expected critical path from the builder data
     *
     * @param builder
     *            The Graph builder object for this use case
     * @return The actual critical path
     */
    protected abstract ITmfGraph getExpectedCriticalPath(GraphBuilder builder);

    private void testCriticalPath(GraphBuilder builder, @NonNull IGraphWorker obj) {
        /* Get the base graph */
        ITmfGraph main = builder.build();
        assertNotNull(main);

        /* The expected critical path */
        ITmfGraph expected = getExpectedCriticalPath(builder);
        assertNotNull(expected);

        /* The actual critical path */
        ITmfVertex head = main.getHead(obj);
        ITmfGraph actual = computeCriticalPath(main, head);
        assertNotNull(actual);

        /* Check the 2 graphs are equivalent */
        GraphOps.checkEquality(expected, actual);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_BASIC} graph
     */
    @Test
    public void testCriticalPathBasic() {
        testCriticalPath(GraphFactory.GRAPH_BASIC, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_WAKEUP_SELF} graph
     */
    @Test
    public void testCriticalPathWakeupSelf() {
        testCriticalPath(GraphFactory.GRAPH_WAKEUP_SELF, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_WAKEUP_NEW} graph
     */
    @Test
    public void testCriticalPathWakeupNew() {
        testCriticalPath(GraphFactory.GRAPH_WAKEUP_NEW, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_WAKEUP_UNKNOWN} graph
     */
    @Test
    public void testCriticalPathWakeupUnknown() {
        testCriticalPath(GraphFactory.GRAPH_WAKEUP_UNKNOWN, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_WAKEUP_MUTUAL} graph
     */
    @Test
    public void testCriticalPathWakeupMutual() {
        testCriticalPath(GraphFactory.GRAPH_WAKEUP_MUTUAL, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_NESTED} graph
     */
    @Test
    public void testCriticalPathWakeupNested() {
        testCriticalPath(GraphFactory.GRAPH_NESTED, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_OPENED} graph
     */
    @Test
    public void testCriticalPathWakeupOpened() {
        testCriticalPath(GraphFactory.GRAPH_OPENED, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_OPENED_DELAY} graph
     */
    @Test
    public void testCriticalPathWakeupOpenedDelay() {
        testCriticalPath(GraphFactory.GRAPH_OPENED_DELAY, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_WAKEUP_MISSING} graph
     */
    @Test
    public void testCriticalPathWakeupMissing() {
        testCriticalPath(GraphFactory.GRAPH_WAKEUP_MISSING, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_WAKEUP_EMBEDDED}
     * graph
     */
    @Test
    public void testCriticalPathWakeupEmbedded() {
        testCriticalPath(GraphFactory.GRAPH_WAKEUP_EMBEDDED, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_WAKEUP_INTERLEAVE}
     * graph
     */
    @Test
    public void testCriticalPathWakeupInterleave() {
        testCriticalPath(GraphFactory.GRAPH_WAKEUP_INTERLEAVE, GraphFactory.Actor0);
    }

    /**
     * Test the algorithm on the {@link GraphFactory#GRAPH_NET1} graph
     */
    @Test
    public void testCriticalPathWakeupNet1() {
        testCriticalPath(GraphFactory.GRAPH_NET1, GraphFactory.Actor0);
    }

}
