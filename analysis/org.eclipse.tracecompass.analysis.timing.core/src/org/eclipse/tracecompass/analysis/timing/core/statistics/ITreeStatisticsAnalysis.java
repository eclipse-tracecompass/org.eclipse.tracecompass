/**********************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.statistics;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Statistics data provider that only returns the root of the statistics tree.
 * Letting the analysis module decide what the tree will look like.
 *
 * @author Siwei Zhang
 * @param <E>
 *            The type of object to calculate statistics on
 * @since 6.1
 */
public interface ITreeStatisticsAnalysis<@NonNull E> {

    /**
     * Get the root of the statistics tree for the time range. If the range
     * start is TmfTimeRange.ETERNITY.getStartTime().toNanos() and the range end
     * is TmfTimeRange.ETERNITY.getEndTime().toNanos(), it will return the
     * statistics for the entire trace.
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
     * Get the root of the statistics tree.
     *
     * @return The complete statistics
     */
    @Nullable
    ITreeStatistics<@NonNull E> getStatsRoot();
}
