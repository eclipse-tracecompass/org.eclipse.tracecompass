/*******************************************************************************
 * Copyright (c) 2013, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackHostUtils;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackHostUtils.TraceHostIdResolver;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackSeries;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.IDataPalette;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * The base classes for analyses who want to populate the CallStack state
 * system.
 *
 * If the elements in the callstack are grouped by process ID / thread ID, the
 * default state provider {@link CallStackStateProvider} can be extended, and
 * implement how to retrieve function entry and exit and process/thread IDs.
 *
 * @author Matthew Khouzam
 * @author Geneviève Bastien
 */
public abstract class InstrumentedCallStackAnalysis extends TmfStateSystemAnalysisModule implements IFlameChartProvider, ICallGraphProvider {

    /** CallStack stack-attribute */
    public static final String CALL_STACK = "CallStack"; //$NON-NLS-1$

    private static final String[] DEFAULT_PROCESSES_PATTERN = new String[] { CallStackStateProvider.PROCESSES, "*" }; //$NON-NLS-1$
    private static final String[] DEFAULT_THREADS_PATTERN = new String[] { "*" }; //$NON-NLS-1$

    private static final List<String[]> PATTERNS = ImmutableList.of(DEFAULT_PROCESSES_PATTERN, DEFAULT_THREADS_PATTERN);

    private @Nullable CallStackSeries fCallStacks;

    private final CallGraphAnalysis fCallGraph;

    /**
     * Listeners
     */
    private final ListenerList<IAnalysisProgressListener> fListeners = new ListenerList<>(ListenerList.IDENTITY);

    /**
     * Whether the callgraph execution will be triggered automatically after
     * build.
     */
    private boolean fAutomaticCallgraph = true;

    /**
     * Abstract constructor (should only be called via the sub-classes'
     * constructors.
     */
    protected InstrumentedCallStackAnalysis() {
        super();
        fCallGraph = new CallGraphAnalysis(this);
    }

    @Override
    public boolean setTrace(@NonNull ITmfTrace trace) throws TmfAnalysisException {
        if (!super.setTrace(trace)) {
            return false;
        }
        return fCallGraph.setTrace(trace);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        fCallGraph.setName(name);
    }

    @Override
    public synchronized @Nullable CallStackSeries getCallStackSeries() {
        CallStackSeries callstacks = fCallStacks;
        if (callstacks == null) {
            ITmfStateSystem ss = getStateSystem();
            ITmfTrace trace = getTrace();
            if (ss == null || trace == null) {
                return null;
            }
            callstacks = new CallStackSeries(ss, getPatterns(), 0, "", getCallStackHostResolver(trace), getCallStackTidResolver()); //$NON-NLS-1$
            fCallStacks = callstacks;
        }
        return callstacks;
    }

    /**
     * Get the callstack host ID resolver for this instrumented series. The
     * default is to use the host name of the trace.
     *
     * @param trace
     *            The trace this analysis is run on
     * @return The host ID resolver
     */
    protected TraceHostIdResolver getCallStackHostResolver(ITmfTrace trace) {
        return new CallStackHostUtils.TraceHostIdResolver(trace);
    }

    /**
     * Get the callstack TID resolver for this instrumented series. The default
     * is to use the name of the second attribute as the thread ID.
     *
     * @return The thread ID resolver
     */
    protected @Nullable IThreadIdResolver getCallStackTidResolver() {
        return new CallStackSeries.AttributeValueThreadResolver(1);
    }

    @Override
    protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
        fCallGraph.setId(getId());
        boolean ret = super.executeAnalysis(monitor);
        if (!ret) {
            return ret;
        }
        ISegmentStore<ISegment> segmentStore = getSegmentStore();
        if (segmentStore != null) {
            sendUpdate(segmentStore);
        }
        if (fAutomaticCallgraph) {
            fCallGraph.schedule();
        }
        return true;
    }

    /**
     * Get the patterns for the process, threads and callstack levels in the
     * state system
     *
     * @return The patterns for the different levels in the state system
     */
    protected List<String[]> getPatterns() {
        return PATTERNS;
    }

    @Override
    public @NonNull String getHostId() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return ""; //$NON-NLS-1$
        }
        return trace.getHostId();
    }

    /**
     * Get the edges (links) of the callstack
     *
     * @param start
     *            start time of the arrows to sample
     * @param end
     *            end time of the arrows to sample
     * @param monitor
     *            monitor to cancel the job
     *
     * @return a list of the edges, as {@link ITmfStateInterval}s with
     *         {@link EdgeStateValue}s.
     */
    @SuppressWarnings("null")
    public List<ITmfStateInterval> getLinks(long start, long end, IProgressMonitor monitor) {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }

        Collection<Integer> quarks = getEdgeQuarks();
        if (quarks.isEmpty()) {
            return Collections.emptyList();
        }

        // collect all the sampled edge intervals
        List<ITmfStateInterval> list = new ArrayList<>();
        try {
            for (ITmfStateInterval interval : ss.query2D(quarks, start, end)) {
                Object value = interval.getValue();
                if (monitor.isCanceled()) {
                    return Collections.emptyList();
                } else if (value instanceof EdgeStateValue) {
                    list.add(interval);
                }
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return Collections.emptyList();
        }
        return list;
    }

    @Override
    public CallGraph getCallGraph(ITmfTimestamp start, ITmfTimestamp end) {
        fCallGraph.schedule();
        fCallGraph.waitForCompletion();
        return fCallGraph.getCallGraph(start, end);
    }

    @Override
    public CallGraph getCallGraph() {
        fCallGraph.schedule();
        fCallGraph.waitForCompletion();
        return fCallGraph.getCallGraph();
    }

    @Override
    public Collection<IWeightedTreeGroupDescriptor> getGroupDescriptors() {
        fCallGraph.schedule();
        fCallGraph.waitForCompletion();
        return fCallGraph.getGroupDescriptors();
    }

    @Override
    public String getTitle() {
        return fCallGraph.getTitle();
    }

    @Override
    public void dispose() {
        super.dispose();
        fCallGraph.dispose();
    }

    @Override
    public AggregatedCallSite createCallSite(Object symbol) {
        return fCallGraph.createCallSite(symbol);
    }

    /**
     * Get the edges (links) of the callstack
     *
     * @return a list of the edges
     */
    @Override
    public @Nullable ISegmentStore<ISegment> getSegmentStore() {
        CallStackSeries series = getCallStackSeries();
        if (series == null) {
            return null;
        }
        return series;
    }

    @Override
    public void addListener(@NonNull IAnalysisProgressListener listener) {
        fListeners.add(listener);
    }

    @Override
    public void removeListener(@NonNull IAnalysisProgressListener listener) {
        fListeners.remove(listener);
    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        if (getCallStackTidResolver() != null) {
            return ImmutableList.of(FunctionTidAspect.TID_ASPECT, SymbolAspect.SYMBOL_ASPECT);
        }
        return Collections.singletonList(SymbolAspect.SYMBOL_ASPECT);
    }

    /**
     * Returns all the listeners
     *
     * @return latency listeners
     */
    protected Iterable<IAnalysisProgressListener> getListeners() {
        List<IAnalysisProgressListener> listeners = new ArrayList<>();
        for (Object listener : fListeners.getListeners()) {
            if (listener != null) {
                listeners.add((IAnalysisProgressListener) listener);
            }
        }
        return listeners;
    }

    /**
     * Send the segment store to all its listener
     *
     * @param store
     *            The segment store to broadcast
     */
    protected void sendUpdate(final ISegmentStore<ISegment> store) {
        for (IAnalysisProgressListener listener : getListeners()) {
            listener.onComplete(this, store);
        }
    }

    /**
     * Set whether the callgraph execution should be triggered automatically
     * after building the callstack or if it should wait to be requested
     *
     * @param trigger
     *            {@code true} means the callgraph analysis will be executed
     *            after the callstack, {@code false} means it will be executed
     *            on demand only.
     */
    public void triggerAutomatically(boolean trigger) {
        fAutomaticCallgraph = trigger;
    }

    /**
     * Get the quarks to query to get the Edges in the call stack
     *
     * @return list of quarks who's intervals should have
     *         {@link EdgeStateValue}s
     */
    protected Collection<Integer> getEdgeQuarks() {
        return Collections.emptyList();
    }

    @Override
    public boolean isComplete() {
        // Initialization error, but the analysis is completed
        if (!waitForInitialization()) {
            return true;
        }
        ITmfStateSystem stateSystem = getStateSystem();
        if (stateSystem == null) {
            throw new IllegalStateException("The initialiation is complete, so the state system must not be null"); //$NON-NLS-1$
        }
        return stateSystem.waitUntilBuilt(0);
    }

    @Override
    public long getEnd() {
        // Initialization error, but the analysis is completed
        if (!waitForInitialization()) {
            return Integer.MIN_VALUE;
        }
        ITmfStateSystem stateSystem = getStateSystem();
        if (stateSystem == null) {
            throw new IllegalStateException("The initialiation is complete, so the state system must not be null"); //$NON-NLS-1$
        }
        return stateSystem.getCurrentEndTime();
    }

    @Override
    public @NonNull List<@NonNull String> getExtraDataSets() {
        return fCallGraph.getExtraDataSets();
    }

    @Override
    public MetricType getWeightType() {
        return fCallGraph.getWeightType();
    }

    @Override
    public List<MetricType> getAdditionalMetrics() {
        return fCallGraph.getAdditionalMetrics();
    }

    @Override
    public String toDisplayString(AggregatedCallSite object) {
        return fCallGraph.toDisplayString(object);
    }

    @Override
    public Object getAdditionalMetric(AggregatedCallSite object, int metricIndex) {
        return fCallGraph.getAdditionalMetric(object, metricIndex);
    }

    @Override
    public IDataPalette getPalette() {
        // Schedule the analysis (it will likely be needed) but don't wait for
        // completion as this should be a fast return.
        fCallGraph.schedule();
        return fCallGraph.getPalette();
    }
}
