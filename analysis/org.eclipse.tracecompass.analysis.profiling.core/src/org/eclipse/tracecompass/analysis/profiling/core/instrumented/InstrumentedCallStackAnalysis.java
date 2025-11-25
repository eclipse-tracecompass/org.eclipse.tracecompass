/*******************************************************************************
 * Copyright (c) 2013, 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.instrumented;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.IDataPalette;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.CallGraph;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICallGraphProvider2;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSeries;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils.IHostIdResolver;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils.TraceHostIdResolver;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FunctionTidAspect;
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
 * @since 2.5
 */
public abstract class InstrumentedCallStackAnalysis extends TmfStateSystemAnalysisModule implements IFlameChartProvider, ICallGraphProvider2 {

    /** CallStack stack-attribute */
    public static final String CALL_STACK = "CallStack"; //$NON-NLS-1$

    /**
     * Annotations string state attribute
     *
     * @since 2.7
     */
    public static final String ANNOTATIONS = "Markers"; //$NON-NLS-1$

    private @Nullable CallStackSeries fCallStacks;

    private final CallGraphAnalysis fCallGraph;
    private boolean fIsCallStackSeriesFinalized = false;

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
    public boolean setTrace(ITmfTrace trace) throws TmfAnalysisException {
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
        ITmfStateSystem ss = getStateSystem();
        ITmfTrace trace = getTrace();
        if (ss == null || trace == null) {
            return null;
        }
        // Check before getting the CallStackSeries if the state system is
        // in its final state
        boolean isStateSystemBuilt = ss.waitUntilBuilt(0);
        if (callstacks == null) {
            List<String[]> patterns = getPatterns();
            if (patterns.isEmpty()) {
                return null;
            }
            callstacks = new CallStackSeries(ss, patterns, 0, "", getCallStackHostResolver(trace), getCallStackTidResolver()); //$NON-NLS-1$
            fCallStacks = callstacks;
        } else if (!fIsCallStackSeriesFinalized) {
            callstacks.updateRootGroup(getPatterns(), 0);
        }
        // This is the last update after the state system is completed
        if (isStateSystemBuilt) {
            fIsCallStackSeriesFinalized = true;
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

    @Override
    public boolean waitForCompletion() {
        boolean waitForCompletion = super.waitForCompletion();
        if (fAutomaticCallgraph) {
            waitForCompletion &= fCallGraph.waitForCompletion();
        }
        return waitForCompletion;
    }

    @Override
    public boolean waitForCompletion(IProgressMonitor monitor) {
        boolean waitForCompletion = super.waitForCompletion(monitor);
        if (fAutomaticCallgraph) {
            waitForCompletion &= fCallGraph.waitForCompletion(monitor);
        }
        return waitForCompletion;
    }

    /**
     * Get the patterns for the process, threads and callstack levels in the
     * state system
     *
     * @return The patterns for the different levels in the state system
     */
    protected List<String[]> getPatterns() {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }
        List<String> patterns = new ArrayList<>();
        for (int quark : ss.getQuarks("*")) { //$NON-NLS-1$
            patterns.addAll(findCallStackPatterns(ss, quark));
        }
        if (patterns.size() > 1) {
            String[] firstElement = new String[] { patterns.get(0), patterns.get(1) };
            patterns.remove(1);
            patterns.remove(0);
            List<String[]> groupDescriptorPatterns = new ArrayList<>();
            groupDescriptorPatterns.add(firstElement);
            groupDescriptorPatterns.addAll(patterns.stream().map(e -> new String @NonNull [] { e })
                    .collect(Collectors.toList()));
            return groupDescriptorPatterns;
        }
        return Collections.emptyList();
    }

    private List<String> findCallStackPatterns(ITmfStateSystem ss, int quark) {
        LinkedList<String> patterns = new LinkedList<>();
        int nCallStackChild = 0;
        if (ss.getNbAttributes() == 0) {
            return patterns;
        }
        List<Integer> subQuarks = ss.getSubAttributes(quark, false);
        boolean isCallStack = false;
        List<String> subPatterns = new LinkedList<>();
        for (Integer subQuark : subQuarks) {
            if (ss.getAttributeName(subQuark).equals(CALL_STACK)) {
                isCallStack = true;
            } else {
                @NonNull
                List<String> neighbourSubPatterns = subPatterns;
                subPatterns = findCallStackPatterns(ss, subQuark);
                if (!subPatterns.isEmpty()) {
                    nCallStackChild += 1;
                    mergeSubPatterns(subPatterns, neighbourSubPatterns);
                } else {
                    subPatterns = neighbourSubPatterns;
                }
            }
        }
        if (!subPatterns.isEmpty()) {
            patterns.addAll(subPatterns);
        }
        if (nCallStackChild > 0 || isCallStack) {
            patterns.push(ss.getAttributeName(quark));
        }
        return patterns;
    }

    private static void mergeSubPatterns(List<String> pattern1, List<String> pattern2) {
        if (!pattern2.isEmpty()) {
            // if subpatterns not empty then replace by a star everything
            // different
            for (int i = 0; i < Integer.max(pattern1.size(), pattern2.size()); i++) {
                if (i > Integer.min(pattern1.size() - 1, pattern2.size() - 1)) {
                    if (i > pattern1.size() - 1) {
                        pattern1.add(pattern2.get(i));
                    }
                    continue;
                }
                if (!pattern1.get(i).equals(pattern2.get(i))) {
                    pattern1.set(i, "*"); //$NON-NLS-1$
                }
            }
        }
    }

    @Override
    public IHostIdResolver getHostIdResolver() {
        return new CallStackHostUtils.TraceHostIdResolver(getTrace());
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
    public void addListener(IAnalysisProgressListener listener) {
        fListeners.add(listener);
    }

    @Override
    public void removeListener(IAnalysisProgressListener listener) {
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
    public @NonNull List<String> getExtraDataSets() {
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
