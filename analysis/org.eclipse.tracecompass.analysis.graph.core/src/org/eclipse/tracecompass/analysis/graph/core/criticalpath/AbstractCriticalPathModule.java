/*******************************************************************************
 * Copyright (c) 2022, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.criticalpath;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.building.AbstractTmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.graph.WorkerSerializer;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.graph.core.Activator;
import org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath.CriticalPathModule;
import org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath.Messages;
import org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath.OSCriticalPathAlgorithm;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.annotations.VisibleForTesting;

/**
 * Class to implement the critical path analysis
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 * @since 4.0
 */
public abstract class AbstractCriticalPathModule extends CriticalPathModule {

    /** Worker_id parameter name */
    public static final String PARAM_WORKER = "workerid"; //$NON-NLS-1$

    private static final int CRITICAL_PATH_GRAPH_VERSION = 1;

    private final AbstractTmfGraphBuilderModule fGraphModule;

    private volatile @Nullable ITmfGraph fCriticalPath;
    private volatile boolean fScheduleOnParameterChange = true;

    /**
     * Default constructor
     *
     * @param graph
     *            The graph module that will be used to calculate the critical
     *            path on
     * @since 1.1
     */
    public AbstractCriticalPathModule(AbstractTmfGraphBuilderModule graph) {
        super(graph);
        addParameter(PARAM_WORKER);
        setId(ANALYSIS_ID);
        fGraphModule = graph;
    }

    /**
     * Constructor with the parameter. Can be used by benchmarks, to avoid that
     * setting the parameter causes the module to be schedule and run in a job
     * which keeps it in memory forever (and thus can cause OOME)
     *
     * @param graph
     *            The graph module that will be used to calculate the critical
     *            path on
     * @param worker
     *            The worker parameter to set
     * @since 3.1
     */
    @VisibleForTesting
    public AbstractCriticalPathModule(AbstractTmfGraphBuilderModule graph, IGraphWorker worker) {
        this(graph);
        fScheduleOnParameterChange = false;
        setParameter(PARAM_WORKER, worker);
    }

    @Override
    protected boolean executeAnalysis(final IProgressMonitor monitor) throws TmfAnalysisException {
        /* Get the worker id */
        Object workerObj = getParameter(PARAM_WORKER);
        if (workerObj == null) {
            return false;
        }
        if (!(workerObj instanceof IGraphWorker)) {
            throw new IllegalStateException("Worker parameter must be an IGraphWorker"); //$NON-NLS-1$
        }
        IGraphWorker worker = (IGraphWorker) workerObj;

        /* Get the graph */
        AbstractTmfGraphBuilderModule graphModule = fGraphModule;
        graphModule.schedule();

        monitor.setTaskName(NLS.bind(Messages.CriticalPathModule_waitingForGraph, graphModule.getName()));
        if (!graphModule.waitForCompletion(monitor)) {
            Activator.getInstance().logInfo("Critical path execution: graph building was cancelled.  Results may not be accurate."); //$NON-NLS-1$
            return false;
        }
        ITmfGraph graph = graphModule.getTmfGraph();
        if (graph == null) {
            throw new TmfAnalysisException("Critical Path analysis: graph " + graphModule.getName() + " is null"); //$NON-NLS-1$//$NON-NLS-2$
        }

        ITmfVertex head = graph.getHead(worker);
        if (head == null) {
            /* Nothing happens with this worker, return an empty graph */
            fCriticalPath = createGraph();
            return true;
        }

        ICriticalPathAlgorithm cp = getAlgorithm(graph);
        try {
            ITmfGraph criticalPath = createGraph();
            if (criticalPath == null) {
                return false;
            }
            // If the critical path has already been computed previously
            if (criticalPath.isDoneBuilding()) {
                ITmfVertex previousGraphHead = criticalPath.getHead();
                IGraphWorker previousGraphWorker = previousGraphHead == null ? null : criticalPath.getParentOf(previousGraphHead);
                IGraphWorker currentWorker = graph.getParentOf(head);
                // If it is the correct critical path
                boolean isSameWorker = previousGraphWorker != null && compareWorker(previousGraphWorker, currentWorker);
                if (isSameWorker) {
                    fCriticalPath = criticalPath;
                    return true;
                }
                // Else delete it
                criticalPath.dispose();
                getHistoryTreeFilePath().toFile().delete();
                // Recreate a new graph with a brand new history tree file
                criticalPath = createGraph();
            }
            criticalPath = cp.computeCriticalPath(criticalPath, head, null);
            criticalPath.closeGraph(Long.MAX_VALUE);
            fCriticalPath = criticalPath;
            return true;
        } catch (CriticalPathAlgorithmException e) {
            Activator.getInstance().logError(NonNullUtils.nullToEmptyString(e.getMessage()), e);
        }
        return false;
    }

    private static boolean compareWorker(IGraphWorker worker1, IGraphWorker worker2) {
        if (!worker1.getHostId().equals(worker2.getHostId())) {
            return false;
        }
        for (String key: worker1.getWorkerAspects().keySet()) {
            Object value1 = worker1.getWorkerAspects().get(key);
            Object value2 = worker2.getWorkerAspects().get(key);
            if (value1 == null && value2 == null) {
                continue;
            }
            if (value1 == null || value2 == null || !value1.equals(value2)) {
                return false;
            }
        }
        return true;
    }

    private @Nullable ITmfGraph createGraph() {
        Path htFile = getHistoryTreeFilePath();
        ITmfTrace trace = fGraphModule.getTrace();
        if (trace == null) {
            throw new NullPointerException("The graph should not be created if there is no trace set"); //$NON-NLS-1$
        }
        return createGraphInstance(htFile, fGraphModule.getWorkerSerializer(), trace.getStartTime().toNanos(), CRITICAL_PATH_GRAPH_VERSION);
    }

    private Path getHistoryTreeFilePath() {
        ITmfTrace trace = fGraphModule.getTrace();
        if (trace == null) {
            throw new NullPointerException("The graph should not be created if there is no trace set"); //$NON-NLS-1$
        }
        String fileDirectory = TmfTraceManager.getSupplementaryFileDir(trace);
        // FIXME Move somewhere where both graph and crit path module can use
        String id = fGraphModule.getId() + ".critPath"; //$NON-NLS-1$
        Path historyTreeFile = Paths.get(fileDirectory + id + ".ht"); //$NON-NLS-1$
        return historyTreeFile;
    }


    /**
     * Create the graph instance.
     *
     * The method needs to be implemented by children as the graph has to implement factories
     * and other methods that are specific to each use of the critical path module.
     *
     * @param htFile The history tree file used to store the graph
     * @param workerSerializer Responsible to read and write htfiles from disk
     * @param startTime Start time of the trace
     * @param version Version of the critical path graph used
     * @return The critical path graph
     *
     * @since 3.2
     */
    protected abstract @Nullable ITmfGraph createGraphInstance(Path htFile, WorkerSerializer workerSerializer, long startTime, int version);

    @Override
    protected void canceling() {
        // Do nothing
    }

    @Override
    protected void parameterChanged(String name) {
        closeCriticalPath();
        cancel();
        resetAnalysis();
        if (fScheduleOnParameterChange) {
            schedule();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        closeCriticalPath();
    }

    private void closeCriticalPath() {
        if (fCriticalPath != null) {
            fCriticalPath.dispose();
        }
        fCriticalPath = null;
    }

    private static ICriticalPathAlgorithm getAlgorithm(ITmfGraph graph) {
        return new OSCriticalPathAlgorithm(graph);
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        /*
         * TODO: The critical path executes on a graph, so at least a graph must
         * be available for this trace
         */
        return true;
    }

    /**
     * Gets the graph for the critical path
     *
     * @return The critical path graph
     */
    @Override
    public @Nullable ITmfGraph getCriticalPathGraph() {
        return fCriticalPath;
    }

    @Override
    protected @NonNull String getFullHelpText() {
        return NonNullUtils.nullToEmptyString(Messages.CriticalPathModule_fullHelpText);
    }

    @Override
    protected @NonNull String getShortHelpText(ITmfTrace trace) {
        return getFullHelpText();
    }

    @Override
    protected @NonNull String getTraceCannotExecuteHelpText(@NonNull ITmfTrace trace) {
        return NonNullUtils.nullToEmptyString(Messages.CriticalPathModule_cantExecute);
    }

}
