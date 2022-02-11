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

package org.eclipse.tracecompass.analysis.graph.core.tests.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.tracecompass.analysis.graph.core.building.ITraceEventHandler;
import org.eclipse.tracecompass.analysis.graph.core.building.AbstractTmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.tests.Activator;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.TestGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.module.GraphBuilderModuleStub;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.module.GraphProviderStub;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test suite for the {@link AbstractTmfGraphBuilderModule} class
 *
 * @author Geneviève Bastien
 * @author Francis Giraldeau
 */
public class TmfGraphBuilderModuleTest {

    private static final String STUB_TRACE_FILE = "testfiles/stubtrace.xml";

    /**
     * With this trace, the resulting graph should look like this:
     *
     * <pre>
     *           0  1  2  3  4  5  6  7  8  9  10  11  12  13
     * Player 1     *--*        *-----*                *
     *                 |        |                      |
     * Player 2        *--------*               *------*
     * </pre>
     *
     * @return
     */
    private GraphBuilderModuleStub getModule(TmfTrace trace) {
        trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        GraphBuilderModuleStub module = null;
        for (GraphBuilderModuleStub mod : TmfTraceUtils.getAnalysisModulesOfClass(trace, GraphBuilderModuleStub.class)) {
            module = mod;
        }
        assertNotNull(module);

        return module;
    }

    /**
     * Test the graph builder execution
     */
    @Test
    public void testBuildGraph() {
        TmfXmlTraceStub trace = TmfXmlTraceStubNs.setupTrace(Activator.getAbsoluteFilePath(STUB_TRACE_FILE));

        AbstractTmfGraphBuilderModule module = getModule(trace);
        module.schedule();
        module.waitForCompletion();

        ITmfGraph graph = module.getTmfGraph();
        assertNotNull(graph);

        assertEquals(2, graph.getWorkers().size());
        // assertEquals(9, graph.size());

        Iterator<@NonNull ITmfVertex> it = graph.getNodesOf(new TestGraphWorker(1));
        ImmutableList<@NonNull ITmfVertex> vertices = ImmutableList.copyOf(it);
        assertEquals(5, vertices.size());

        long timestamps1[] = { 1, 2, 5, 7, 12 };
        boolean hasEdges1[][] = {
                { false, true, false, false },
                { true, false, false, true },
                { false, true, true, false },
                { true, false, false, false},
                { false, false, true, false} };
        for (int i = 0; i < vertices.size(); i++) {
            ITmfVertex v = vertices.get(i);
            assertEquals(timestamps1[i], v.getTimestamp());
            assertEquals(hasEdges1[i][0], graph.getEdgeFrom(v, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE) != null);
            assertEquals(hasEdges1[i][1], graph.getEdgeFrom(v, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE) != null);
            assertEquals(hasEdges1[i][2], graph.getEdgeFrom(v, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE) != null);
            assertEquals(hasEdges1[i][3], graph.getEdgeFrom(v, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE) != null);
        }

        it = graph.getNodesOf(new TestGraphWorker(2));
        vertices = ImmutableList.copyOf(it);
        assertEquals(4, vertices.size());

        long timestamps2[] = { 2, 5, 10, 12 };
        boolean hasEdges2[][] = {
                { false, true, true, false },
                { true, false, false, true },
                { false, true, false, false },
                { true, false, false, true} };
        for (int i = 0; i < vertices.size(); i++) {
            ITmfVertex v = vertices.get(i);
            assertEquals(timestamps2[i], v.getTimestamp());
            assertEquals(hasEdges2[i][0], graph.getEdgeFrom(v, ITmfGraph.EdgeDirection.INCOMING_HORIZONTAL_EDGE) != null);
            assertEquals(hasEdges2[i][1], graph.getEdgeFrom(v, ITmfGraph.EdgeDirection.OUTGOING_HORIZONTAL_EDGE) != null);
            assertEquals(hasEdges2[i][2], graph.getEdgeFrom(v, ITmfGraph.EdgeDirection.INCOMING_VERTICAL_EDGE) != null);
            assertEquals(hasEdges2[i][3], graph.getEdgeFrom(v, ITmfGraph.EdgeDirection.OUTGOING_VERTICAL_EDGE) != null);
        }

        trace.dispose();
    }

    private class TestEventHandler extends AbstractTraceEventHandler {
        public TestEventHandler(int priority) {
            super(priority);
        }

        @Override
        public void handleEvent(@NonNull ITmfEvent event) {
            // Nothing to do, just stubs
        }
    }

    private class TestEventHandler2 extends TestEventHandler {
        public TestEventHandler2(int priority) {
            super(priority);
        }
    }

    /**
     * Test adding handlers to the graph provider
     */
    @Test
    public void testHandlers() {
        TmfXmlTraceStub trace = TmfXmlTraceStubNs.setupTrace(Activator.getAbsoluteFilePath(STUB_TRACE_FILE));

        try {
            GraphBuilderModuleStub module = getModule(trace);
            GraphProviderStub graphProvider = module.getGraphProvider();
            int origSize = graphProvider.getHandlers().size();
            // Add an handler with priority 5
            graphProvider.registerHandler(new TestEventHandler(5));
            List<@NonNull ITraceEventHandler> handlers = graphProvider.getHandlers();
            int newSize = handlers.size();
            assertTrue(areHandlersSorted(handlers));
            assertEquals(origSize + 1, newSize);

            // Add a new instance of the same handler, with same priority, it should not be
            // added
            graphProvider.registerHandler(new TestEventHandler(5));
            handlers = graphProvider.getHandlers();
            newSize = handlers.size();
            assertTrue(areHandlersSorted(handlers));
            assertEquals(origSize + 1, newSize);

            // Add the same handler with another priority, it should be added
            graphProvider.registerHandler(new TestEventHandler(7));
            handlers = graphProvider.getHandlers();
            newSize = handlers.size();
            assertTrue(areHandlersSorted(handlers));
            assertEquals(origSize + 2, newSize);

            // Add another class of handler with same priority as another one
            graphProvider.registerHandler(new TestEventHandler2(5));
            handlers = graphProvider.getHandlers();
            newSize = handlers.size();
            assertTrue(areHandlersSorted(handlers));
            assertEquals(origSize + 3, newSize);
        } finally {
            trace.dispose();
        }
    }

    private static boolean areHandlersSorted(List<@NonNull ITraceEventHandler> handlers) {
        // Verify that handlers are sorted by priority
        if (handlers.isEmpty()) {
            return true;
        }
        int prevPrio = -1;
        for (int i = 0; i < handlers.size(); i++) {
            int prio = handlers.get(i).getPriority();
            if (prevPrio > prio) {
                return false;
            }
            prevPrio = prio;
        }
        return true;
    }

}
