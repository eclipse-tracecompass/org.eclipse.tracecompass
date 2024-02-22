/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Provide arrows for a flame chart. These arrows may come from any other flame
 * chart analysis.
 *
 * @author Geneviève Bastien
 */
public class FlameChartArrowProvider {
    private final ITmfTrace fTrace;

    /**
     * Constructor
     *
     * @param trace
     *            The trace for which this data provider applies
     */
    public FlameChartArrowProvider(ITmfTrace trace) {
        fTrace = trace;
    }

    /**
     * Fetch arrows with query
     *
     * @param fetchParameters
     *            the parameters
     * @param monitor
     *            the monitor
     * @return the corresponding state intervals
     */
    public List<ITmfStateInterval> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        if (filter == null) {
            return Collections.emptyList();
        }
        long start = filter.getStart();
        long end = filter.getEnd();

        InstrumentedCallStackAnalysis csModule = null;
        Iterable<InstrumentedCallStackAnalysis> modules = TmfTraceUtils.getAnalysisModulesOfClass(fTrace, InstrumentedCallStackAnalysis.class);
        Iterator<InstrumentedCallStackAnalysis> iterator = modules.iterator();

        List<ITmfStateInterval> allEdges = new ArrayList<>();
        while (iterator.hasNext()) {
            csModule = iterator.next();
            List<ITmfStateInterval> moduleEdges = csModule.getLinks(start, end, monitor == null ? new NullProgressMonitor() : monitor);
            allEdges.addAll(moduleEdges);
        }
        return allEdges;
    }
}
