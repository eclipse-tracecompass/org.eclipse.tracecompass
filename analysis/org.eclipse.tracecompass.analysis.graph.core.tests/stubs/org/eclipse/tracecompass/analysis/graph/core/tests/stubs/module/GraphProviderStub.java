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

package org.eclipse.tracecompass.analysis.graph.core.tests.stubs.module;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.building.AbstractTmfGraphProvider;
import org.eclipse.tracecompass.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.tracecompass.analysis.graph.core.building.ITraceEventHandler;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.TestGraphWorker;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState.OSEdgeContextEnum;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Simple graph provider stub
 *
 * @author Geneviève Bastien
 * @author Francis Giraldeau
 */
public class GraphProviderStub extends AbstractTmfGraphProvider {

    /**
     * Constructor
     *
     * @param trace
     *            The trace
     */
    public GraphProviderStub(@NonNull ITmfTrace trace) {
        super(trace, "Graph Provider Stub");
        registerHandler(new StubEventHandler());
    }

    private class StubEventHandler extends AbstractTraceEventHandler {

        public StubEventHandler() {
            super(5);
        }

        @Override
        public void handleEvent(ITmfEvent event) {
            String evname = event.getType().getName();

            ITmfGraph graph = getGraph();
            if (graph == null) {
                throw new IllegalStateException();
            }

            switch (evname) {
            case "take": {
                IGraphWorker player = new TestGraphWorker(NonNullUtils.checkNotNull((Integer) event.getContent().getField("player").getValue()));
                ITmfVertex v = graph.createVertex(player, event.getTimestamp().getValue());
                graph.add(v);
                break;
            }
            case "pass": {
                IGraphWorker playerFrom = new TestGraphWorker(NonNullUtils.checkNotNull((Integer) event.getContent().getField("from").getValue()));
                IGraphWorker playerTo = new TestGraphWorker(NonNullUtils.checkNotNull((Integer) event.getContent().getField("to").getValue()));
                ITmfVertex vFrom = graph.createVertex(playerFrom, event.getTimestamp().getValue());
                ITmfVertex vTo = graph.createVertex(playerTo, event.getTimestamp().getValue());
                graph.append(vFrom);
                graph.add(vTo);
                graph.edge(vFrom, vTo, new OSEdgeContextState(OSEdgeContextEnum.NETWORK));
                break;
            }
            case "kick": {
                IGraphWorker player = new TestGraphWorker(NonNullUtils.checkNotNull((Integer) event.getContent().getField("player").getValue()));
                ITmfVertex v = graph.createVertex(player, event.getTimestamp().getValue());
                graph.append(v);
                break;
            }
            default:
                break;
            }
        }

    }

    // Change the visibility of this method to be used in tests
    @Override
    public @NonNull List<@NonNull ITraceEventHandler> getHandlers() {
        return super.getHandlers();
    }



}
