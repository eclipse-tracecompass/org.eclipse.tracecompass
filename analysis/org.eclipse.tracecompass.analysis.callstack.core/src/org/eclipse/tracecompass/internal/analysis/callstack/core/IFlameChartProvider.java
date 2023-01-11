/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Interface that can be implemented by components who provide call stacks as
 * part of their data.
 *
 * @author Geneviève Bastien
 */
public interface IFlameChartProvider extends IAnalysisModule, ISegmentStoreProvider {

    /**
     * Get the callstacks series provided by this analysis.
     *
     * @return The callstack series or null if it is not available yet
     */
    @Nullable CallStackSeries getCallStackSeries();

    /**
     * Get the ID of the host this callstack provider is for
     *
     * TODO: Deprecate me, now using the interfaces from
     * {@link CallStackHostUtils}
     *
     * @return The ID of the host
     */
    String getHostId();

    /**
     * Return whether this analysis is complete
     *
     * @return <code>true</code> if the analysis is completed, whether failed or
     *         not, <code>false</code> if it is currently running
     */
    boolean isComplete();

    /**
     * Return the current end time of this flame chart. The return value of this
     * method may change as long as the {@link #isComplete()} method returns
     * <code>false</code>. When the flame chart is complete, then this value
     * should stay the same.
     *
     * If the value is not known, impossible to compute or not available, the
     * return value should be {@link Integer#MIN_VALUE}.
     *
     * @return The end time of the flame chart, in nanoseconds, or
     *         {@link Integer#MIN_VALUE} if end time is not available.
     */
    long getEnd();

    /**
     * Query the requested callstacks and return the segments for the sampled
     * times. The returned segments will be simply {@link ISegment} when there
     * is no function at a given depth, or {@link ICalledFunction} when there is
     * an actual function.
     *
     * @param collection
     *            The callstack entries to query
     * @param times
     *            The complete list of times to query, they may not all be
     *            within this series's range
     * @return A map of callstack depths to a list of segments.
     */
    default Multimap<CallStackDepth, ISegment> queryCallStacks(Collection<CallStackDepth> collection, Collection<Long> times) {
        CallStackSeries callStackSeries = getCallStackSeries();
        if (callStackSeries == null) {
            return Objects.requireNonNull(ArrayListMultimap.create());
        }
        return callStackSeries.queryCallStacks(collection, times);
    }
}
