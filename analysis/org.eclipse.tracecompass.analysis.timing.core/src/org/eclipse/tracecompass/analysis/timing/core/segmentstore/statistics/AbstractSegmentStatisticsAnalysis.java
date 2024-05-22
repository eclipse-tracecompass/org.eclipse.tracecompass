/*******************************************************************************
 * Copyright (c) 2016, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatisticsAnalysis;
import org.eclipse.tracecompass.analysis.timing.core.statistics.Statistics;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * Abstract analysis to build statistics data for a segment store
 *
 * @author Jean-Christian Kouame
 * @since 3.0
 */
public abstract class AbstractSegmentStatisticsAnalysis extends TmfAbstractAnalysisModule implements IStatisticsAnalysis<ISegment> {

    private @Nullable ISegmentStoreProvider fSegmentStoreProvider;

    private @Nullable IStatistics<ISegment> fTotalStats;

    private Map<String, IStatistics<ISegment>> fPerSegmentTypeStats = new HashMap<>();

    /**
     * Gets the segment mapper. This allows values to be resolved
     *
     * @return the resolver, the mapper that maps an {@link ISegment} to a
     *         {@link Number}
     *
     * @since 5.2
     */
    protected Function<ISegment, @Nullable Number> getMapper() {
        return ISegment::getLength;
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        ITmfTrace trace = getTrace();
        if (trace != null) {
            ISegmentStoreProvider provider = getSegmentStoreProvider(trace);
            fSegmentStoreProvider = provider;
            if (provider instanceof IAnalysisModule) {
                return ImmutableList.of((IAnalysisModule) provider);
            }
        }
        return super.getDependentAnalyses();
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        if (monitor.isCanceled()) {
            return false;
        }

        IStatistics<ISegment> totalStats = getTotalStats(TmfTimeRange.ETERNITY.getStartTime().toNanos(), TmfTimeRange.ETERNITY.getEndTime().toNanos(), monitor);
        if (totalStats == null) {
            return false;
        }

        Map<String, IStatistics<ISegment>> perTypeStats = getPerTypeStats(TmfTimeRange.ETERNITY.getStartTime().toNanos(), TmfTimeRange.ETERNITY.getEndTime().toNanos(), monitor);
        fTotalStats = totalStats;
        fPerSegmentTypeStats = perTypeStats;

        return true;
    }

    private @Nullable IStatistics<ISegment> getTotalStats(long start, long end, IProgressMonitor monitor) {
        Iterable<@NonNull ISegment> store = getSegmentStore(start, end);
        if (store == null) {
            return null;
        }
        if (monitor.isCanceled()) {
            return null;
        }
        return calculateTotalManual(store, monitor);
    }

    /**
     * Get the total statistics for a specific range. If the range start is
     * TmfTimeRange.ETERNITY.getStartTime().toNanos() and the range end is
     * TmfTimeRange.ETERNITY.getEndTime().toNanos(), it will return the
     * statistics for the whole trace.
     *
     * @param start
     *            The start time of the range
     * @param end
     *            The end time of the range
     * @param monitor
     *            The progress monitor
     * @return The total statistics, or null if segment store is not valid or if
     *         the request is canceled
     * @since 1.3
     */
    @Override
    public @Nullable IStatistics<ISegment> getStatsForRange(long start, long end, IProgressMonitor monitor) {
        ITmfTrace trace = getTrace();
        if (trace != null && isEternity(start, end)) {
            waitForCompletion();
            return getStatsTotal();
        }
        return getTotalStats(start, end, monitor);
    }

    private Map<@NonNull String, IStatistics<@NonNull ISegment>> getPerTypeStats(long start, long end, IProgressMonitor monitor) {
        Iterable<@NonNull ISegment> store = getSegmentStore(start, end);
        if (monitor.isCanceled() || store == null) {
            return Collections.emptyMap();
        }
        return calculateTotalPerType(store, monitor);
    }

    /**
     * Get the per segment type statistics for a specific range. If the range
     * start is TmfTimeRange.ETERNITY.getStartTime().toNanos() and the range end
     * is TmfTimeRange.ETERNITY.getEndTime().toNanos(), it will return the
     * statistics for the whole trace.
     *
     * @param start
     *            The start time of the range
     * @param end
     *            The end time of the range
     * @param monitor
     *            The progress monitor
     * @return The per segment type statistics, or null if segment store is not
     *         valid or if the request is canceled
     * @since 1.3
     */
    @Override
    public Map<@NonNull String, IStatistics<@NonNull ISegment>> getStatsPerTypeForRange(long start, long end, IProgressMonitor monitor) {
        ITmfTrace trace = getTrace();
        if (trace != null && isEternity(start, end)) {
            waitForCompletion();
            return getStatsPerType();
        }
        return getPerTypeStats(start, end, monitor);
    }

    private static boolean isEternity(long start, long end) {
        return start == TmfTimeRange.ETERNITY.getStartTime().toNanos() && end == TmfTimeRange.ETERNITY.getEndTime().toNanos();
    }

    /**
     * Get the segment store from which we want the statistics
     *
     * @return The segment store
     */
    private @Nullable Iterable<@NonNull ISegment> getSegmentStore(long start, long end) {
        ISegmentStoreProvider segmentStoreProvider = fSegmentStoreProvider;
        if (segmentStoreProvider == null) {
            return null;
        }
        if (segmentStoreProvider instanceof IAnalysisModule) {
            ((IAnalysisModule) segmentStoreProvider).waitForCompletion();
        }
        long t0 = Long.min(start, end);
        long t1 = Long.max(start, end);
        ISegmentStore<@NonNull ISegment> segmentStore = segmentStoreProvider.getSegmentStore();
        return segmentStore != null ?
                isEternity(t0, t1) ?
                        segmentStore :
                        segmentStore.getIntersectingElements(t0, t1) :
                Collections.emptyList();
    }

    private @Nullable IStatistics<ISegment> calculateTotalManual(Iterable<@NonNull ISegment> segments, IProgressMonitor monitor) {
        IStatistics<ISegment> total = new Statistics<>(getMapper());
        for (ISegment segment : segments) {
            if (monitor.isCanceled()) {
                return null;
            }
            total.update(segment);
        }
        return total;
    }

    private Map<@NonNull String, IStatistics<@NonNull ISegment>> calculateTotalPerType(Iterable<ISegment> segments, IProgressMonitor monitor) {
        Map<String, IStatistics<ISegment>> perSegmentTypeStats = new HashMap<>();

        for (ISegment segment : segments) {
            if (monitor.isCanceled()) {
                return Collections.emptyMap();
            }
            String segmentType = getSegmentType(segment);
            if (segmentType != null) {
                IStatistics<ISegment> values = perSegmentTypeStats.getOrDefault(segmentType, new Statistics<>(getMapper()));
                values.update(segment);
                perSegmentTypeStats.put(segmentType, values);
            }
        }
        return perSegmentTypeStats;
    }

    /**
     * Get the type of a segment. Statistics per type will use this type as a
     * key
     *
     * @param segment
     *            the segment for which to get the type
     * @return The type of the segment
     */
    protected abstract @Nullable String getSegmentType(ISegment segment);

    /**
     * Find the segment store provider used for this analysis
     *
     * @param trace
     *            The active trace
     *
     * @return The segment store provider
     * @since 5.3
     */
    protected abstract @Nullable ISegmentStoreProvider getSegmentStoreProvider(ITmfTrace trace);

    @Override
    protected void canceling() {
    }

    /**
     * Get the statistics for the full segment store
     *
     * @return The complete statistics
     * @since 1.3
     */
    @Override
    public @Nullable IStatistics<@NonNull ISegment> getStatsTotal() {
        return fTotalStats;
    }

    /**
     * Get the statistics for each type of segment in this segment store
     *
     * @return the map of statistics per type
     * @since 1.3
     */
    @Override
    public Map<String, IStatistics<@NonNull ISegment>> getStatsPerType() {
        return fPerSegmentTypeStats;
    }

}
