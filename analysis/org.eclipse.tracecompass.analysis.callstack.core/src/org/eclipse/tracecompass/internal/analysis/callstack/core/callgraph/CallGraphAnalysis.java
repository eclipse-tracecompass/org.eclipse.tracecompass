/*******************************************************************************
 * Copyright (c) 2016, 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.internal.analysis.callstack.core.Activator;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.FlameWithKernelPalette;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.IDataPalette;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callstack.CallStack;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callstack.CallStackSeries;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callstack.CallStackSymbolFactory;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.InstrumentedCallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.IHostModel;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.ModelManager;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.ProcessStatusInterval;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

/**
 * Call stack analysis used to create a segment for each call function from an
 * entry/exit event. It builds a segment tree from the state system. An example
 * taken from the Fibonacci trace's callStack shows the structure of the segment
 * tree given by this analysis:
 *
 * <pre>
 * (Caller)  main
 *            ↓↑
 * (Callee) Fibonacci
 *           ↓↑    ↓↑
 *      Fibonacci Fibonacci
 *         ↓↑         ↓↑
 *         ...        ...
 * </pre>
 *
 * @author Sonia Farrah
 */
public class CallGraphAnalysis extends TmfAbstractAnalysisModule implements ICallGraphProvider {

    /**
     * Public ID for this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.callstack.callgraph"; //$NON-NLS-1$

    /**
     * Index of some metrics, to get its statistics. package-private so
     * aggregated called function can access it
     */
    static final int SELF_TIME_METRIC_INDEX = 0;
    static final int CPU_TIME_METRIC_INDEX = 1;
    private static final String SELF_TIME_TITLE = Objects.requireNonNull(Messages.CallGraphStats_SelfTime);
    private static final String CPU_TIME_TITLE = Objects.requireNonNull(Messages.CallGraphStats_CpuTime);
    private static final String NB_CALLS_TITLE = Objects.requireNonNull(Messages.CallGraphStats_NbCalls);
    private static final MetricType DURATION_METRIC = new MetricType(Objects.requireNonNull(Messages.CallGraphStats_Duration), DataType.NANOSECONDS, null, true);
    private static final List<MetricType> METRICS = ImmutableList.of(
            new MetricType(SELF_TIME_TITLE, DataType.NANOSECONDS, null, true),
            new MetricType(CPU_TIME_TITLE, DataType.NANOSECONDS, null, true),
            new MetricType(NB_CALLS_TITLE, DataType.NUMBER, null, false));

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final IFlameChartProvider fCsProvider;
    private final CallGraph fCallGraph = new CallGraph();

    private @Nullable Collection<ISymbolProvider> fSymbolProviders = null;
    private boolean fHasKernelStatuses = false;

    // Keep a very small cache of selection callgraphs, to avoid having to
    // compute again
    private final LoadingCache<TmfTimeRange, CallGraph> fRangeCallgraphs = Objects.requireNonNull(CacheBuilder.newBuilder()
            .maximumSize(10)
            .build(new CacheLoader<TmfTimeRange, CallGraph>() {
                @Override
                public CallGraph load(TmfTimeRange range) {
                    CallGraph cg = new CallGraph();
                    executeForRange(cg, range, new NullProgressMonitor());
                    return cg;
                }
            }));

    /**
     * Constructor
     *
     * @param csProvider
     *            The call stack provider to use with this analysis
     */
    @SuppressWarnings("null")
    public CallGraphAnalysis(IFlameChartProvider csProvider) {
        super();
        fCsProvider = csProvider;
        setName(NLS.bind(Messages.CallGraphAnalysis_NamePrefix, csProvider.getName()));
    }

    @Override
    public @NonNull String getHelpText() {
        String msg = Messages.CallGraphAnalysis_Description;
        return (msg != null) ? msg : super.getHelpText();
    }

    @SuppressWarnings("null")
    @Override
    public void setName(String name) {
        super.setName(NLS.bind(Messages.CallGraphAnalysis_NamePrefix, name));
    }

    @Override
    public @NonNull String getHelpText(ITmfTrace trace) {
        return getHelpText();
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        return true;
    }

    @Override
    protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
        return executeForRange(fCallGraph, TmfTimeRange.ETERNITY, monitor);
    }

    private boolean executeForRange(CallGraph callgraph, TmfTimeRange range, @Nullable IProgressMonitor monitor) {
        ITmfTrace trace = getTrace();
        if (monitor == null || trace == null) {
            return false;
        }
        IFlameChartProvider callstackModule = fCsProvider;
        IHostModel model = ModelManager.getModelFor(callstackModule.getHostId());

        CallStackSeries callstack = callstackModule.getCallStackSeries();
        if (callstack != null) {
            long time0 = range.getStartTime().toNanos();
            long time1 = range.getEndTime().toNanos();
            long start = Math.min(time0, time1);
            long end = Math.max(time0, time1);
            if (!iterateOverCallstackSerie(callstack, model, callgraph, start, end, monitor)) {
                return false;
            }
        }
        monitor.worked(1);
        monitor.done();
        return true;
    }

    /**
     * Iterate over a callstack series. It will do a depth-first search to
     * create the callgraph
     *
     * @param callstackSerie
     *            The series to iterate over
     * @param model
     *            The model of the host on which this callstack was running
     * @param callgraph
     *            The callgraph to fill
     * @param start
     *            the start time of the request
     * @param end
     *            The end time of the request
     * @param monitor
     *            A progress monitor
     * @return Whether the series was successfully iterated over
     */
    @VisibleForTesting
    protected boolean iterateOverCallstackSerie(CallStackSeries callstackSerie, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        // The root elements are the same as the one from the callstack series
        Collection<ICallStackElement> rootElements = callstackSerie.getRootElements();
        for (ICallStackElement element : rootElements) {
            if (monitor.isCanceled()) {
                return false;
            }
            iterateOverElement(element, model, callgraph, start, end, monitor);
        }
        return true;
    }

    private void iterateOverElement(ICallStackElement element, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        // Iterator over the children of the element until we reach the leaves
        if (element.isLeaf()) {
            iterateOverLeafElement(element, model, callgraph, start, end, monitor);
            return;
        }
        for (ICallStackElement child : element.getChildrenElements()) {
            iterateOverElement(child, model, callgraph, start, end, monitor);
        }
    }

    private void iterateOverLeafElement(ICallStackElement element, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        if (!(element instanceof InstrumentedCallStackElement)) {
            throw new IllegalStateException("Call Graph Analysis: The element does not have the right type"); //$NON-NLS-1$
        }
        InstrumentedCallStackElement insElement = (InstrumentedCallStackElement) element;
        CallStack callStack = insElement.getCallStack();

        // If there is no children for this callstack, just return
        if (callStack.getMaxDepth() == 0) {
            return;
        }
        fHasKernelStatuses |= callStack.hasKernelStatuses();
        // Start with the first function
        AbstractCalledFunction nextFunction = (AbstractCalledFunction) callStack.getNextFunction(callStack.getStartTime(), 1, null, model, start, end);
        while (nextFunction != null) {
            AggregatedCalledFunction aggregatedChild = createCallSite(CallStackSymbolFactory.createSymbol(nextFunction.getSymbol(), element, nextFunction.getStart()));
            iterateOverCallstack(element, callStack, nextFunction, 2, aggregatedChild, model, start, end, monitor);
            aggregatedChild.addFunctionCall(nextFunction);
            // Add the kernel statuses if available
            Iterable<ProcessStatusInterval> kernelStatuses = callStack.getKernelStatuses(nextFunction, Collections.emptyList());
            for (ProcessStatusInterval status : kernelStatuses) {
                aggregatedChild.addKernelStatus(status);
            }
            callgraph.addAggregatedCallSite(element, aggregatedChild);
            nextFunction = (AbstractCalledFunction) callStack.getNextFunction(nextFunction.getEnd(), 1, null, model, start, end);
        }
    }

    private void iterateOverCallstack(ICallStackElement element, CallStack callstack, ICalledFunction function, int nextLevel, AggregatedCalledFunction aggregatedCall, IHostModel model, long start, long end, IProgressMonitor monitor) {
        if (nextLevel > callstack.getMaxDepth()) {
            return;
        }
        int threadId = function.getThreadId();
        long lastSampleEnd = start;

        AbstractCalledFunction nextFunction = (AbstractCalledFunction) callstack.getNextFunction(function.getStart(), nextLevel, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));
        while (nextFunction != null) {
            // Add sampling data of the time between next function and beginning
            // of next level
            if (threadId > 0) {
                Collection<AggregatedCallSite> samplingData = model.getSamplingData(threadId, lastSampleEnd, nextFunction.getStart());
                samplingData.forEach(aggregatedCall::addChild);
                lastSampleEnd = nextFunction.getEnd();
            }
            AggregatedCalledFunction aggregatedChild = createCallSite(CallStackSymbolFactory.createSymbol(nextFunction.getSymbol(), element, nextFunction.getStart()));
            iterateOverCallstack(element, callstack, nextFunction, nextLevel + 1, aggregatedChild, model, start, end, monitor);
            aggregatedCall.addChild(nextFunction, aggregatedChild);
            nextFunction = (AbstractCalledFunction) callstack.getNextFunction(nextFunction.getEnd(), nextLevel, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));
        }
        // Get the sampling to the end of the function
        if (threadId > 0) {
            Collection<AggregatedCallSite> samplingData = model.getSamplingData(threadId, lastSampleEnd, function.getEnd() - lastSampleEnd);
            samplingData.forEach(aggregatedCall::addChild);
        }
    }

    /**
     * Get the callstack series of the providers of this analysis
     *
     * @return The collection of callstack series
     */
    public @Nullable CallStackSeries getSeries() {
        return fCsProvider.getCallStackSeries();
    }

    @Override
    protected void canceling() {
        // Do nothing
    }

    @Override
    public CallGraph getCallGraph(ITmfTimestamp start, ITmfTimestamp end) {
        return fRangeCallgraphs.getUnchecked(new TmfTimeRange(start, end));
    }

    @Override
    public CallGraph getCallGraph() {
        return fCallGraph;
    }

    @Override
    public Collection<IWeightedTreeGroupDescriptor> getGroupDescriptors() {
        List<IWeightedTreeGroupDescriptor> descriptors = new ArrayList<>();
        CallStackSeries serie = fCsProvider.getCallStackSeries();
        if (serie != null) {
            descriptors.add(serie.getRootGroup());
        }
        return descriptors;
    }

    @Override
    public AggregatedCalledFunction createCallSite(Object symbol) {
        return new AggregatedCalledFunction((ICallStackSymbol) symbol);
    }

    @Override
    public List<String> getExtraDataSets() {
        if (fHasKernelStatuses) {
            return Collections.singletonList(String.valueOf(Messages.FlameChartDataProvider_KernelStatusTitle));
        }
        return ICallGraphProvider.super.getExtraDataSets();
    }

    @Override
    public MetricType getWeightType() {
        return DURATION_METRIC;
    }

    @Override
    public List<MetricType> getAdditionalMetrics() {
        return METRICS;
    }

    @Override
    public String toDisplayString(AggregatedCallSite callsite) {
        Collection<ISymbolProvider> symbolProviders = fSymbolProviders;
        if (symbolProviders == null) {
            ITmfTrace trace = getTrace();
            if (trace == null) {
                return String.valueOf(callsite.getObject());
            }
            symbolProviders = SymbolProviderManager.getInstance().getSymbolProviders(trace);
            fSymbolProviders = symbolProviders;
        }
        return callsite.getObject().resolve(symbolProviders);
    }

    @Override
    public Object getAdditionalMetric(AggregatedCallSite object, int metricIndex) {
        if (object instanceof AggregatedCalledFunction) {
            switch (metricIndex) {
            case 0:
                return ((AggregatedCalledFunction) object).getSelfTime();
            case 1:
                long cpuTime = ((AggregatedCalledFunction) object).getCpuTime();
                return cpuTime >= 0 ? cpuTime : 0L;
            case 2:
                return ((AggregatedCalledFunction) object).getNbCalls();
            default:
                Activator.getInstance().logError("Unknown metric at position " + metricIndex); //$NON-NLS-1$
                // Unknown metric, should not happen
                break;
            }
        }
        return 0L;
    }

    @Override
    public String getTitle() {
        return Objects.requireNonNull(Messages.CallGraphAnalysis_Title);
    }

    @Override
    public @NonNull IDataPalette getPalette() {
        // Use the palette with kernel styles, at worst, kernel styles won't be
        // used
        return FlameWithKernelPalette.getInstance();
    }
}
