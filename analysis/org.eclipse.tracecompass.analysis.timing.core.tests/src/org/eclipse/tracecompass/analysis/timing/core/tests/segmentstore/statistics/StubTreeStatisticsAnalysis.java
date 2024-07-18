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
package org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore.statistics;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.statistics.ITreeStatistics;
import org.eclipse.tracecompass.analysis.timing.core.statistics.ITreeStatisticsAnalysis;
import org.eclipse.tracecompass.analysis.timing.core.statistics.TreeStatistics;
import org.eclipse.tracecompass.segmentstore.core.BasicSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;


/**
 * Implements {@link ITreeStatisticsAnalysis} to provide fixed
 * {@link TreeStatistics} for testing purposes.
 *
 * @author Siwei Zhang
 */
public class StubTreeStatisticsAnalysis implements ITreeStatisticsAnalysis<@NonNull ISegment> {
    private final ITreeStatistics<@NonNull ISegment> fStatsRoot;
    private final ITreeStatistics<@NonNull ISegment> fStatsSelection;

    /**
     * Constructor initializing values
     */
    public StubTreeStatisticsAnalysis() {
        // Initialize with the expected hierarchy for the full range
        fStatsRoot = createTreeStatistics("Total");
        fStatsSelection = createTreeStatistics("Selection");
    }

    @Override
    public @Nullable ITreeStatistics<@NonNull ISegment> getStatsRoot() {
        return fStatsRoot;
    }

    @Override
    public @Nullable ITreeStatistics<@NonNull ISegment> getStatsRootForRange(long start, long end, @NonNull IProgressMonitor monitor) {
        return fStatsSelection;
    }

    private static ITreeStatistics<@NonNull ISegment> createTreeStatistics(@NonNull String rootName) {
        // Create the root statistics
        TreeStatistics<@NonNull ISegment> rootStats = new TreeStatistics<>(ISegment::getLength, rootName);
        updateStatistic(rootStats, 1000);

        // Create child statistics
        TreeStatistics<@NonNull ISegment> chlid1Stats = new TreeStatistics<>(ISegment::getLength, "child1");
        updateStatistic(chlid1Stats, 2000);
        TreeStatistics<@NonNull ISegment> chlid2Stats = new TreeStatistics<>(ISegment::getLength, "child2");
        updateStatistic(chlid2Stats, 3000);

        // Create grandchild statistics
        TreeStatistics<@NonNull ISegment> grandChild11Stats = new TreeStatistics<>(ISegment::getLength, "grandChild11");
        updateStatistic(grandChild11Stats, 4000);
        TreeStatistics<@NonNull ISegment> grandChild12Stats = new TreeStatistics<>(ISegment::getLength, "grandChild12");
        updateStatistic(grandChild12Stats, 5000);
        TreeStatistics<@NonNull ISegment> grandChild21Stats = new TreeStatistics<>(ISegment::getLength, "grandChild21");
        updateStatistic(grandChild21Stats, 6000);
        TreeStatistics<@NonNull ISegment> grandChild22Stats = new TreeStatistics<>(ISegment::getLength, "grandChild22");
        updateStatistic(grandChild22Stats, 7000);

        // Add children to their respective parents
        chlid1Stats.addChild(grandChild11Stats);
        chlid1Stats.addChild(grandChild12Stats);
        chlid2Stats.addChild(grandChild21Stats);
        chlid2Stats.addChild(grandChild22Stats);
        rootStats.addChild(chlid1Stats);
        rootStats.addChild(chlid2Stats);

        return rootStats;
    }

    private static void updateStatistic(TreeStatistics<@NonNull ISegment> statistic, int number) {
        ISegment segment;
        for(int i=0; i<3; i++) {
            segment = new BasicSegment(number, number+i);
            statistic.update(segment);
        }
    }
}
