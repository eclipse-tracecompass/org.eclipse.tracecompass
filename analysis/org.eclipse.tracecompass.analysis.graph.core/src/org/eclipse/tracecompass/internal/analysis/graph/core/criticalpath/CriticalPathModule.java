/*******************************************************************************
 * Copyright (c) 2015, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath;

import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.building.AbstractTmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.ICriticalPathProvider;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.OSCriticalPathModule;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;

import com.google.common.annotations.VisibleForTesting;

/**
 * Class to implement the critical path analysis
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 */
@Deprecated
public class CriticalPathModule extends TmfAbstractAnalysisModule implements ICriticalPathProvider {
    /**
     * Analysis ID for this module
     */
    public static final String ANALYSIS_ID = "org.eclipse.tracecompass.analysis.graph.core.criticalpath"; //$NON-NLS-1$
    /** Worker_id parameter name */
    private @Nullable OSCriticalPathModule fCriticalPathModule;
    private AbstractTmfGraphBuilderModule fGraph;
    private @Nullable IGraphWorker fWorker;

    /**
     * Default constructor
     *
     * @param graph
     *            The graph module that will be used to calculate the critical
     *            path on
     * @since 4.0
     */
    public CriticalPathModule(AbstractTmfGraphBuilderModule graph) {
        fGraph = graph;
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
     * @since 4.0
     */
    @VisibleForTesting
    public CriticalPathModule(AbstractTmfGraphBuilderModule graph, IGraphWorker worker) {
        fGraph = graph;
        fWorker = worker;
    }

    private CriticalPathModule getCriticalPathModule() {
        if (fCriticalPathModule == null) {
            if (fWorker == null) {
                fCriticalPathModule = new OSCriticalPathModule(fGraph);
            } else {
                fCriticalPathModule = new OSCriticalPathModule(fGraph, Objects.requireNonNull(fWorker));
            }
        }
        return Objects.requireNonNull(fCriticalPathModule);
    }

    public @Nullable TmfGraph getCriticalPath() {
        return getCriticalPathModule().getCriticalPath();
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        return getCriticalPathModule().executeAnalysis(monitor);
    }

    @Override
    protected void canceling() {
        getCriticalPathModule().canceling();
    }
}