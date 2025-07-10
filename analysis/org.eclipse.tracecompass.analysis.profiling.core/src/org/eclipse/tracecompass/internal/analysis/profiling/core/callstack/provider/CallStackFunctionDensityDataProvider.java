/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.internal.analysis.profiling.core.callstack.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackAnalysis;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackSeries;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.IAxisDomain;
import org.eclipse.tracecompass.tmf.core.model.ISampling;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.genericxy.AbstractTreeGenericXYCommonXDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * Bar chart provider to show function duration distributions in the call stack.
 *
 * @author Siwei Zhang
 * @since 2.6
 */
public class CallStackFunctionDensityDataProvider extends AbstractTreeGenericXYCommonXDataProvider<@NonNull CallStackAnalysis, @NonNull ITmfTreeDataModel> {

    /**
     * Provider id.
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.profiling.core.callstack.functiondensity.provider"; //$NON-NLS-1$

    private static final String EXECUTION_TIME = "Execution Time"; //$NON-NLS-1$
    private static final String NUMBER_OF_EXECUTIONS = "Number of Executions"; //$NON-NLS-1$
    private static final String UNIT_NS = "ns"; //$NON-NLS-1$
    private static final String UNIT_EMPTY = ""; //$NON-NLS-1$
    private final Map<Integer, Long> fPidsToEntryIds = new HashMap<>();
    private static final int UNKNOWN_PID = -1;
    private static final TmfXYAxisDescription Y_AXIS_DESCRIPTION = new TmfXYAxisDescription(
            NUMBER_OF_EXECUTIONS, UNIT_EMPTY, DataType.NUMBER);

    /**
     * Stores the state system end time when making the query and the maximum
     * execution time.
     */
    private Pair<Long, Long> fEndTimeToMaxDuration = null;

    /**
     * Constructor.
     *
     * @param trace
     *            The trace associated with this data provider
     * @param analysisModule
     *            The analysis module used to compute data
     */
    public CallStackFunctionDensityDataProvider(@NonNull ITmfTrace trace, @NonNull CallStackAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public TmfXYAxisDescription getXAxisDescription() {
        long maxDuration = getMaxExecutionTime();
        if (maxDuration == -1) {
            return new TmfXYAxisDescription(
                    EXECUTION_TIME, UNIT_NS, DataType.DURATION, new IAxisDomain.Range(-1, -1));
        }

        return new TmfXYAxisDescription(
                EXECUTION_TIME, UNIT_NS, DataType.DURATION, new IAxisDomain.Range(0, maxDuration));
    }

    /**
     * Find the maximum duration in the segment store.
     */
    private long getMaxExecutionTime() {
        ITmfStateSystem ss = getAnalysisModule().getStateSystem();
        if (ss == null) {
            return -1;
        }

        if (fEndTimeToMaxDuration != null && fEndTimeToMaxDuration.getFirst() == ss.getCurrentEndTime()) {
            return fEndTimeToMaxDuration.getSecond();
        }


        CallStackSeries callstackSeries = getAnalysisModule().getCallStackSeries();
        if (callstackSeries == null) {
            return -1;
        }

        long maxDuration = -1;
        for (ISegment segment : callstackSeries.getIntersectingElements(ss.getStartTime(), ss.getCurrentEndTime())) {
            maxDuration = Math.max(segment.getLength(), maxDuration);
        }

        fEndTimeToMaxDuration = new Pair<>(ss.getCurrentEndTime(), maxDuration);
        return maxDuration;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected boolean isCacheable() {
        return false;
    }

    @Override
    protected TmfTreeModel<@NonNull ITmfTreeDataModel> getTree(@NonNull ITmfStateSystem ss, @NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        CallStackSeries series = getAnalysisModule().getCallStackSeries();
        if (series == null) {
            return new TmfTreeModel<>(Collections.emptyList(), Collections.emptyList());
        }

        List<@NonNull ITmfTreeDataModel> entries = new ArrayList<>();
        long traceId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        entries.add(new TmfTreeDataModel(traceId, -1, NonNullUtils.nullToEmptyString(getTrace().getName())));

        List<@NonNull Integer> processQuarks = ss.getQuarks(getAnalysisModule().getProcessesPattern());
        long end = ss.getCurrentEndTime();
        List<@NonNull ITmfStateInterval> fullEnd = ss.queryFullState(end);
        for(@NonNull Integer processQuark : processQuarks) {
            int pid = UNKNOWN_PID;
            if (processQuark != ITmfStateSystem.ROOT_ATTRIBUTE) {
                String processName = ss.getAttributeName(processQuark);
                Object processValue = fullEnd.get(processQuark).getValue();
                pid = getThreadProcessId(processName, processValue);
            }
            long entryId = getId(processQuark);
            fPidsToEntryIds.put(pid, entryId);
            entries.add(new TmfTreeDataModel(entryId, traceId, getNameFromPID(pid)));
        }
        return new TmfTreeModel<>(Collections.emptyList(), entries);
    }

    private static @NonNull String getNameFromPID(int pid) {
        return pid == UNKNOWN_PID ? "UNKNOWN_PID" : String.valueOf(pid); //$NON-NLS-1$
    }

    private static int getThreadProcessId(String name, @Nullable Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            return UNKNOWN_PID;
        }
    }

    @Override
    protected @Nullable Pair<@NonNull ISampling,@NonNull Collection<@NonNull IYModel>> getXAxisAndYSeriesModels(
            @NonNull ITmfStateSystem ss,
            @NonNull Map<@NonNull String, @NonNull Object> fetchParameters,
            @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {

        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQueryWithSamples(fetchParameters);
        if (filter == null) {
            return null;
        }

        CallStackSeries series = getAnalysisModule().getCallStackSeries();
        if (series == null) {
            return null;
        }

        long sampleStart = 0;
        long sampleEnd = getMaxExecutionTime();
        int nbSamples = filter.getNumberOfSamples();
        ISampling.Ranges sampling = createEvenlyDistributedRanges(sampleStart, sampleEnd, nbSamples);
        if(sampling == null) {
            return null;
        }

        List<ISampling.@NonNull Range<@NonNull Long>> ranges = sampling.ranges();
        Map<Integer, double[]> pidsToBins = new HashMap<>();
        Map<@NonNull Long, @NonNull Integer> selectedEntries = getSelectedEntries(filter);
        // Initialize bins only for selected entries
        for (Entry<Integer, Long> pidToEntryId : fPidsToEntryIds.entrySet()) {
            int pid = pidToEntryId.getKey();
            long entryId = pidToEntryId.getValue();
            if (selectedEntries.containsKey(entryId)) {
                pidsToBins.put(pid, new double[ranges.size()]);
            }
        }

        // Count function durations falling in each bin
        long totalSpan = sampleEnd - sampleStart + 1;
        long step = totalSpan / nbSamples;
        for (ISegment segment : series.getIntersectingElements(filter.getStart(), filter.getEnd())) {
            if (segment instanceof ICalledFunction function) {
                int pid = function.getProcessId();
                double[] bins = pidsToBins.get(pid);
                if (bins == null) {
                    continue;
                }
                long duration = segment.getLength();

                if (step > 0 && duration >= sampleStart && duration <= sampleEnd) {
                    int index = (int) ((duration - sampleStart) / step);
                    if (index >= nbSamples) {
                        index = nbSamples - 1;
                    }
                    bins[index]++;
                }
            }
        }

        // Build final Y models
        List<@NonNull IYModel> yModels = new ArrayList<>();
        for (Entry<Integer, double[]> entry : pidsToBins.entrySet()) {
            int pid = entry.getKey();
            double[] bins = entry.getValue();
            long entryId = fPidsToEntryIds.getOrDefault(pid, -1L);
            yModels.add(new YModel(entryId, getNameFromPID(pid), bins, Y_AXIS_DESCRIPTION));
        }

        return new Pair<>(sampling, yModels);
    }

    private static ISampling.@Nullable Ranges createEvenlyDistributedRanges(long start, long end, int samples) {
        if (samples <= 0 || start >= end) {
            return null;
        }

        List<ISampling.@NonNull Range<@NonNull Long>> ranges = new ArrayList<>(samples);
        long totalSpan = end - start;
        long step = totalSpan / samples;
        long remainder = totalSpan % samples;

        long current = start;
        for (int i = 0; i < samples; i++) {
            long rangeStart = current;
            long rangeEnd = current + step - 1;
            if (remainder > 0) {
                rangeEnd += 1;
                remainder--;
            }
            if (rangeEnd >= end || i == samples - 1) {
                rangeEnd = end;
            }
            ranges.add(new ISampling.Range<>(rangeStart, rangeEnd));
            current = rangeEnd + 1;
        }

        return new ISampling.Ranges(ranges);
    }

    @Override
    protected String getTitle() {
        String title = Objects.requireNonNull(Messages.CallStackFunctionDensityDataProviderFactory_title);
        return title;
    }
}
