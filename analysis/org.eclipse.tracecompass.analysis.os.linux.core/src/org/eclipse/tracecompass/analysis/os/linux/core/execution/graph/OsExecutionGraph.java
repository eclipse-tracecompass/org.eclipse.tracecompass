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

package org.eclipse.tracecompass.analysis.os.linux.core.execution.graph;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.building.ITmfGraphProvider;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.AbstractCriticalPathModule;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.OSCriticalPathModule;
import org.eclipse.tracecompass.analysis.graph.core.building.AbstractTmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.WorkerSerializer;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.OsHistoryTreeGraph;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Graph building module for the lttng kernel execution graph
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 * @since 2.4
 */
public class OsExecutionGraph extends AbstractTmfGraphBuilderModule {

    /**
     * Analysis id of this module
     */
    public static final String ANALYSIS_ID = "org.eclipse.tracecompass.analysis.os.linux.execgraph"; //$NON-NLS-1$

    private @Nullable OSCriticalPathModule fCriticalPathModule;

    private static class OsWorkerSerializer implements WorkerSerializer {

        private static final String SERIALIZER_SEPARATOR = "///"; //$NON-NLS-1$

        @Override
        public String serialize(IGraphWorker worker) {
            if (!(worker instanceof OsWorker)) {
                throw new IllegalArgumentException("Unexpected type of worker: " + worker); //$NON-NLS-1$
            }
            OsWorker osWorker = (OsWorker) worker;
            return osWorker.getHostThread().getTid() + SERIALIZER_SEPARATOR + osWorker.getStart() + SERIALIZER_SEPARATOR + osWorker.getHostId() + SERIALIZER_SEPARATOR + osWorker.getName();
        }

        @Override
        public IGraphWorker deserialize(String serializedWorker) {
            @NonNull String[] workerParts = serializedWorker.split(SERIALIZER_SEPARATOR, 4);
            return new OsWorker(new HostThread(workerParts[2], Integer.decode(workerParts[0])), workerParts[3], Long.decode(workerParts[1]));
        }

    }

    private static WorkerSerializer WORKER_SERIALIZER = new OsWorkerSerializer();

    @Override
    public boolean canExecute(ITmfTrace trace) {
        /**
         * TODO: Trace must have at least sched_switches and sched_wakeups
         * enabled
         */
        return true;
    }

    @Override
    protected ITmfGraphProvider getGraphProvider() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new NullPointerException();
        }
        return new OsExecutionGraphProvider(trace);
    }

    @Override
    protected @Nullable ITmfGraph createGraphInstance(Path htFile, WorkerSerializer workerSerializer, long startTime, int version) {
        OsHistoryTreeGraph graph;
        try {
            graph = new OsHistoryTreeGraph(htFile, version, workerSerializer, startTime);
        } catch (IOException e) {
            return null;
        }

        return graph;
    }

    @Override
    protected AbstractCriticalPathModule getCriticalPathModule() {
        if (fCriticalPathModule == null) {
            fCriticalPathModule = new OSCriticalPathModule(this);
        }
        return Objects.requireNonNull(fCriticalPathModule);
    }

    @Override
    protected String getFullHelpText() {
        return super.getFullHelpText();
    }

    @Override
    protected String getShortHelpText(ITmfTrace trace) {
        return super.getShortHelpText(trace);
    }

    @Override
    protected String getTraceCannotExecuteHelpText(ITmfTrace trace) {
        return "The trace must have events 'sched_switch' and 'sched_wakeup' enabled"; //$NON-NLS-1$
    }

    /**
     * @since 6.0
     */
    @Override
    public WorkerSerializer getWorkerSerializer() {
        return WORKER_SERIALIZER;
    }

}
