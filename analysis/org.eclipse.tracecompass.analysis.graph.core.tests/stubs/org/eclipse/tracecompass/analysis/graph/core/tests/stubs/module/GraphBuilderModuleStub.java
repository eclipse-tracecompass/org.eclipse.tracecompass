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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.building.AbstractTmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.AbstractCriticalPathModule;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.OSCriticalPathModule;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.WorkerSerializer;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.HistoryTreeTmfGraph;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.OsHistoryTreeGraph;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Graph builder module stub
 *
 * @author Geneviève Bastien
 * @author Francis Giraldeau
 */
public class GraphBuilderModuleStub extends AbstractTmfGraphBuilderModule {

    /** The analysis id */
    public static final String ANALYSIS_ID = "org.eclipse.linuxtools.tmf.analysis.graph.tests.stub";

    private @Nullable OSCriticalPathModule fCriticalPathModule;

    /* Make it public so unit tests can use the graph provider */
    @Override
    public GraphProviderStub getGraphProvider() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new NullPointerException();
        }
        return new GraphProviderStub(trace);
    }

    @Override
    protected @NonNull AbstractCriticalPathModule getCriticalPathModule() {
        if (fCriticalPathModule == null) {
            fCriticalPathModule = new OSCriticalPathModule(this);
        }
        return Objects.requireNonNull(fCriticalPathModule);
    }

    @Override
    protected @Nullable ITmfGraph createGraphInstance(@NonNull Path htFile, @NonNull WorkerSerializer workerSerializer, long startTime, int version) {
        HistoryTreeTmfGraph graph;
        try {
            graph = new OsHistoryTreeGraph(htFile, version, workerSerializer, startTime);
        } catch (IOException e) {
            return null;
        }
        return graph;
    }

}
