package org.eclipse.tracecompass.analysis.timing.core.statistics;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Statistic data provider that only returns the root of the statistics tree.
 * Letting the analysis module to decide what the tree would look like.
 *
 * @param <E>
 *            The type of object to calculate statistics on
 * @author ezhasiw
 */
public interface ITreeStatisticsAnalysis<@NonNull E> {

    /**
     * Get the root of statistics tree for the time range. If the range start is
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
     * @return The per type statistics, or <code>null</code> if data source is
     *         invalid or if the request is canceled
     */
    @Nullable
    ITreeStatistics<@NonNull E> getStatsRootForRange(long start, long end, IProgressMonitor monitor);

    /**
     * Get the root of statistics tree
     *
     * @return The complete statistics
     */
    @Nullable
    ITreeStatistics<@NonNull E> getStatsRoot();
}
