/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.analysis.profiling.core.model.IHostModel;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ProcessStatusInterval;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This class represents a function call in a certain level in the call stack.
 * It's used to build an aggregation segment tree (aggregated by depth and
 * callers). Per example,the two calls to the function A() in the call graph
 * below will be combined into one node in the generated tree:
 *
 * <pre>
 *   (Depth=0)      main              main
 *               ↓↑  ↓↑   ↓↑    =>   ↓↑   ↓↑
 *   (Depth=1)  A()  B()  A()       A()   B()
 * </pre>
 *
 * @author Sonia Farrah
 */
public class AggregatedCalledFunction extends AggregatedCallSite {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final AggregatedCalledFunctionStatistics fStatistics;
    private long fDuration = 0;
    private long fSelfTime = 0;
    private long fCpuTime = IHostModel.TIME_UNKNOWN;
    private int fProcessId;
    private Map<ProcessStatus, AggregatedThreadStatus> fProcessStatuses = new HashMap<>();

    /**
     * Constructor, parent is not null
     *
     * @param symbol
     *            The symbol of the function
     */
    public AggregatedCalledFunction(ICallStackSymbol symbol) {
        super(symbol, 0);
        fStatistics = new AggregatedCalledFunctionStatistics();
        fProcessId = -1;
    }

    /**
     * copy constructor
     *
     * @param toCopy
     *            Object to copy
     */
    public AggregatedCalledFunction(AggregatedCalledFunction toCopy) {
        super(toCopy);
        fStatistics = new AggregatedCalledFunctionStatistics();
        fStatistics.merge(toCopy.fStatistics);
        fProcessId = toCopy.fProcessId;
        fDuration = toCopy.fDuration;
        fSelfTime = toCopy.fSelfTime;
        fCpuTime = toCopy.fCpuTime;
        mergeProcessStatuses(toCopy);
    }

    @Override
    public long getWeight() {
        return getDuration();
    }

    @Override
    public AggregatedCalledFunction copyOf() {
        return new AggregatedCalledFunction(this);
    }

    @Override
    protected void mergeData(WeightedTree<ICallStackSymbol> other) {
        if (!(other instanceof AggregatedCalledFunction)) {
            return;
        }
        AggregatedCalledFunction otherFct = (AggregatedCalledFunction) other;

        addToDuration(otherFct.getDuration());
        addToSelfTime(otherFct.getSelfTime());
        addToCpuTime(otherFct.getCpuTime());
        getFunctionStatistics().merge(otherFct.getFunctionStatistics(), true);
        mergeProcessStatuses(otherFct);
    }

    private void mergeProcessStatuses(AggregatedCalledFunction other) {
        Map<ProcessStatus, AggregatedThreadStatus> processStatuses = other.fProcessStatuses;
        for (Entry<ProcessStatus, AggregatedThreadStatus> entry : processStatuses.entrySet()) {
            AggregatedThreadStatus status = fProcessStatuses.get(entry.getKey());
            if (status == null) {
                status = new AggregatedThreadStatus(entry.getKey());
            }
            status.merge(entry.getValue());
            fProcessStatuses.put(entry.getKey(), status);
        }
    }

    @Override
    public Map<String, IStatistics<?>> getStatistics() {
        ImmutableMap.Builder<String, IStatistics<?>> builder = new ImmutableMap.Builder<>();
        builder.put(String.valueOf(Messages.CallGraphStats_Duration), getFunctionStatistics().getDurationStatistics());
        builder.put(String.valueOf(Messages.CallGraphStats_SelfTime), getFunctionStatistics().getSelfTimeStatistics());
        builder.put(String.valueOf(Messages.CallGraphStats_CpuTime), getFunctionStatistics().getCpuTimesStatistics());
        return builder.build();
    }

    /**
     * Add a new callee into the Callees list. If the function exists in the
     * callees list, the new callee's duration will be added to its duration and
     * it'll combine their callees.
     *
     * @param child
     *            The callee to add to this function
     * @param aggregatedChild
     *            The aggregated data of the callee
     */
    public synchronized void addChild(AbstractCalledFunction child, AggregatedCalledFunction aggregatedChild) {
        // Update the child's statistics with itself
        fSelfTime -= aggregatedChild.getDuration();
        aggregatedChild.addFunctionCall(child);
        super.addChild(aggregatedChild);
    }

    /**
     * Adds a function call to this aggregated called function data. The called
     * function must have the same symbol as this aggregate data and its
     * statistics will be added to this one. This should be a function at the
     * same level as this one.
     *
     * @param function
     *            The function that was called
     */
    public synchronized void addFunctionCall(AbstractCalledFunction function) {
        // FIXME: Aren't the statistics enough? Do we really need duration, self
        // time and cpu time here?
        addToDuration(function.getLength());
        addToSelfTime(function.getSelfTime());
        addToCpuTime(function.getCpuTime());
        fProcessId = function.getProcessId();
        getFunctionStatistics().update(function);
    }

    /**
     * Modify the function's duration
     *
     * @param duration
     *            The amount to increment the duration by
     */
    private void addToDuration(long duration) {
        fDuration += duration;
    }

    private void addToCpuTime(long cpuTime) {
        if (cpuTime != IHostModel.TIME_UNKNOWN) {
            if (fCpuTime < 0) {
                fCpuTime = 0;
            }
            fCpuTime += cpuTime;
        }
    }

    /**
     * The function's duration
     *
     * @return The duration of the function
     */
    public long getDuration() {
        return fDuration;
    }

    /**
     * The number of calls of a function
     *
     * @return The number of calls of a function
     */
    public long getNbCalls() {
        return fStatistics.getDurationStatistics().getNbElements();
    }

    /**
     * The self time of an aggregated function
     *
     * @return The self time
     */
    public long getSelfTime() {
        return fSelfTime;
    }

    /**
     * Add to the self time of an aggregated function
     *
     * @param selfTime
     *            The amount of self time to add
     */
    private void addToSelfTime(long selfTime) {
        fSelfTime += selfTime;
    }

    /**
     * The CPU time of an aggregated function
     *
     * @return The CPU time, or {@link IHostModel#TIME_UNKNOWN} if the CPU time
     *         is not known
     */
    public long getCpuTime() {
        return fCpuTime;
    }

    /**
     * The process ID of the trace application.
     *
     * @return The process Id
     */
    public int getProcessId() {
        return fProcessId;
    }

    /**
     * Add a process status interval to this called function
     *
     * @param interval
     *            The process status interval
     */
    public void addKernelStatus(ProcessStatusInterval interval) {
        ProcessStatus processStatus = interval.getProcessStatus();
        AggregatedThreadStatus status = fProcessStatuses.get(processStatus);
        if (status == null) {
            status = new AggregatedThreadStatus(processStatus);
            fProcessStatuses.put(processStatus, status);
        }
        status.update(interval);
    }

    @Override
    public @NonNull Collection<WeightedTree<ICallStackSymbol>> getExtraDataTrees(int index) {
        if (index == 0) {
            return ImmutableList.copyOf(fProcessStatuses.values());
        }
        return Collections.emptyList();
    }

    @Override
    public @Nullable IStatistics<?> getStatistics(int metricIndex) {
        if (metricIndex < 0) {
            return getFunctionStatistics().getDurationStatistics();
        }
        if (metricIndex == CallGraphAnalysis.SELF_TIME_METRIC_INDEX) {
            return getFunctionStatistics().getSelfTimeStatistics();
        }
        if (metricIndex == CallGraphAnalysis.CPU_TIME_METRIC_INDEX) {
            return getFunctionStatistics().getCpuTimesStatistics();
        }
        return null;
    }

    /**
     * The function's statistics
     *
     * @return The function's statistics
     */
    public AggregatedCalledFunctionStatistics getFunctionStatistics() {
        return fStatistics;
    }

    @Override
    public String toString() {
        return "Aggregate Function: " + getObject() + ", Duration: " + getDuration() + ", Self Time: " + fSelfTime + " on " + getNbCalls() + " calls"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }
    /**
     * Updates the statistic based on its statistics and other's one to compute the average statistic which is needed for merging
     * flame graphs together.Unlike the mergedata in which the statistics of other node are added to the statistics of this node,
     * in this function, the statistics are computed as the average of two node's statistic.
     *
     * @param other
     *            the other weighted tree
     */
    public void meanData(WeightedTree<ICallStackSymbol> other) {
        if (!(other instanceof AggregatedCalledFunction)) {
            return;
        }
        AggregatedCalledFunction otherFct = (AggregatedCalledFunction) other;
        fDuration = (fDuration  + otherFct.getDuration()) / 2;
        fSelfTime = (fSelfTime  + otherFct.getDuration()) / 2;
        fCpuTime = (fCpuTime  + otherFct.getDuration()) / 2;

        getFunctionStatistics().merge(otherFct.getFunctionStatistics(), true);
        mergeProcessStatuses(otherFct);
    }
}
