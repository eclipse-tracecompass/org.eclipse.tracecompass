/*******************************************************************************
 * Copyright (c) 2016, 2017 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2;

import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.analysis.timing.core.statistics.Statistics;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.callgraph.ICalledFunction;

/**
 * Class to calculate statistics for an aggregated function.
 *
 * @author Sonia Farrah
 * @author Geneviève Bastien
 */
public class AggregatedCalledFunctionStatistics {

    // Duration statistics will be kept for all calls of the method, so we make
    // them on the called function themselves
    private final IStatistics<ICalledFunction> fDurations;
    // Self time statistics are on aggregated called function because self times
    // are known only at the end, once the aggregation is over
    private final IStatistics<ICalledFunction> fSelfTimes;
    private final IStatistics<ICalledFunction> fCpuTimes;
    // FIXME: Should this class manage the number of calls, or the callsite?
    // Common info with sampling, so maybe callsite
    private final IStatistics<ICalledFunction> fNbCalls;

    /**
     * Constructor
     */
    public AggregatedCalledFunctionStatistics() {
        fDurations = new Statistics<>(f -> f.getLength());
        fSelfTimes = new Statistics<>(f -> f.getSelfTime());
        fCpuTimes = new Statistics<>(f -> f.getCpuTime());
        fNbCalls = new Statistics<>(f -> f.getCpuTime());
    }

    /**
     * Update the durations and self time statistics for a function. This
     * function should be called only once all the children of the function have
     * been assigned and the self time will not change anymore, otherwise, the
     * results will not be accurate for self times.
     *
     * @param function
     *            The function to add statistics for
     */
    public void update(ICalledFunction function) {
        fDurations.update(function);
        fSelfTimes.update(function);
        fCpuTimes.update(function);
        fNbCalls.update(function);
    }

    /**
     * Update the statistics, this is used while merging nodes for the
     * aggregation tree.
     *
     * @param statisticsNode
     *            The statistics node to be merged
     */
    public void merge(AggregatedCalledFunctionStatistics statisticsNode) {
        fDurations.merge(statisticsNode.fDurations);
        fSelfTimes.merge(statisticsNode.fSelfTimes);
        fCpuTimes.merge(statisticsNode.fCpuTimes);
    }

    /**
     * Merge aggregated statistics
     *
     * @param other
     *            The other statistics to merge with this one
     * @param isGroup
     *            <code>true</code> if the statistics to be merged represent a
     *            group of individual statistics, on which statistics on the
     *            calls to children will be calculated, <code>false</code> if
     *            the statistics are to be added to this one.
     */
    public void merge(AggregatedCalledFunctionStatistics other, boolean isGroup) {
        fDurations.merge(other.fDurations);
        fSelfTimes.merge(other.fSelfTimes);
        fCpuTimes.merge(other.fCpuTimes);
    }

    /**
     * Get the statistics for the duration of the called functions
     *
     * @return The durations statistics
     */
    public IStatistics<ICalledFunction> getDurationStatistics() {
        return fDurations;
    }

    /**
     * Get the statistics for the self times of the called functions
     *
     * @return The self time statistics
     */
    public IStatistics<ICalledFunction> getSelfTimeStatistics() {
        return fSelfTimes;
    }

    /**
     * Get the statistics for the CPU times of the called functions
     *
     * @return The CPU time statistics
     */
    public IStatistics<ICalledFunction> getCpuTimesStatistics() {
        return fCpuTimes;
    }

    @Override
    public String toString() {
        return "Aggregated function statistics: Durations: " + fDurations + ", Self times " + fSelfTimes; //$NON-NLS-1$//$NON-NLS-2$
    }
}
