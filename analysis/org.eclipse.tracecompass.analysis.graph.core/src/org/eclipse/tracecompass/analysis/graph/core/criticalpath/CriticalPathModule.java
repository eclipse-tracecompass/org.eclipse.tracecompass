/*******************************************************************************
 * Copyright (c) 2015, 2022 École Polytechnique de Montréal
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
import org.eclipse.tracecompass.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.building.TmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.graph.TmfGraphFactory;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.graph.core.Activator;
import org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath.CriticalPathAlgorithmBounded;
import org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath.Messages;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.annotations.VisibleForTesting;

/**
 * Class to implement the critical path analysis
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 */
public class CriticalPathModule extends TmfAbstractAnalysisModule implements ICriticalPathProvider {

    /**
     * Analysis ID for this module
     */
    public static final String ANALYSIS_ID = "org.eclipse.tracecompass.analysis.graph.core.criticalpath"; //$NON-NLS-1$

    /** Worker_id parameter name */
    public static final String PARAM_WORKER = "workerid"; //$NON-NLS-1$

    private static final int CRITICAL_PATH_GRAPH_VERSION = 1;

    private final TmfGraphBuilderModule fGraphModule;

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
    public CriticalPathModule(TmfGraphBuilderModule graph) {
        super();
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
    public CriticalPathModule(TmfGraphBuilderModule graph, IGraphWorker worker) {
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
        TmfGraphBuilderModule graphModule = fGraphModule;
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
            fCriticalPath = null;
            return true;
        }

        ICriticalPathAlgorithm cp = getAlgorithm(graph);
        try {
            ITmfGraph criticalPath = createGraph();
            cp.computeCriticalPath(criticalPath, head, null);
            fCriticalPath = criticalPath;
            return true;
        } catch (CriticalPathAlgorithmException e) {
            Activator.getInstance().logError(NonNullUtils.nullToEmptyString(e.getMessage()), e);
        }
        return false;
    }

    private @Nullable ITmfGraph createGraph() {
        TmfGraphBuilderModule graphModule = fGraphModule;
        ITmfTrace trace = graphModule.getTrace();
        if (trace == null) {
            throw new NullPointerException("The graph shouuld not be created if there is no trace set"); //$NON-NLS-1$
        }
        String fileDirectory = TmfTraceManager.getSupplementaryFileDir(trace);

        // FIXME Move somewhere where both graph and crit path module can use
        String id = graphModule.getId() + ".critPath"; //$NON-NLS-1$
        Path htFile = Paths.get(fileDirectory + id + ".ht"); //$NON-NLS-1$

        // Path segFile = Files.createFile(path);
        return TmfGraphFactory.createOnDiskGraph(htFile, graphModule.getWorkerSerializer(), trace.getStartTime().toNanos(), CRITICAL_PATH_GRAPH_VERSION);
    }

    @Override
    protected void canceling() {
        // Do nothing
    }

    @Override
    protected void parameterChanged(String name) {
        fCriticalPath = null;
        cancel();
        resetAnalysis();
        if (fScheduleOnParameterChange) {
            schedule();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        fCriticalPath = null;
    }

    private static ICriticalPathAlgorithm getAlgorithm(ITmfGraph graph) {
        return new CriticalPathAlgorithmBounded(graph);
    }

    @Override
    public boolean canExecute(@NonNull ITmfTrace trace) {
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
     * @deprecated Use the {@link #getCriticalPathGraph()} method instead.
     */
    @Deprecated
    @Override
    public @Nullable TmfGraph getCriticalPath() {
        return null;
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
