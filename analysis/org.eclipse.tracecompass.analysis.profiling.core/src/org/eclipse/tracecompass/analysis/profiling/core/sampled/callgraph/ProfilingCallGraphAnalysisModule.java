/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.sampled.callgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.CallGraph;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICallGraphProvider2;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.IEventCallStackProvider;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSymbolFactory;
import org.eclipse.tracecompass.internal.analysis.profiling.core.Activator;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.Messages;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * The callgraph analysis module
 *
 * @author Geneviève Bastien
 * @since 2.5
 */
public abstract class ProfilingCallGraphAnalysisModule extends TmfAbstractAnalysisModule implements ICallGraphProvider2, IEventCallStackProvider {

    private static final MetricType WEIGHT_METRIC = new MetricType(Objects.requireNonNull(Messages.CallGraphStats_NbCalls), DataType.NUMBER, null);

    private @Nullable ITmfEventRequest fRequest;
    private final Set<ICallStackElement> fRootElements = new HashSet<>();

    private @Nullable CallGraph fFullRangeCallGraph;
    private @Nullable Collection<ISymbolProvider> fSymbolProviders = null;

    /**
     * Get the root elements from this call graph hierarchy
     *
     * @return The root elements
     */
    protected Collection<ICallStackElement> getRootElements() {
        return fRootElements;
    }

    /**
     * Add a root element to call graph
     *
     * @param element
     *            The root element to add
     */
    public void addRootElement(ICallStackElement element) {
        fRootElements.add(element);
    }

    @Override
    public CallGraph getCallGraph(ITmfTimestamp start, ITmfTimestamp end) {
        CallGraph cg = executeForRange(new TmfTimeRange(start, end));
        if (cg == null) {
            return CallGraph.EMPTY_GRAPH;
        }
        return cg;
    }

    @Override
    public CallGraph getCallGraph() {
        CallGraph cg = fFullRangeCallGraph;
        if (cg == null) {
            return CallGraph.EMPTY_GRAPH;
        }
        return cg;
    }

    @Override
    public MetricType getWeightType() {
        return WEIGHT_METRIC;
    }

    @Override
    public String getTitle() {
        return "Call Graph"; //$NON-NLS-1$
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
            fSymbolProviders  = symbolProviders;
        }
        return callsite.getObject().resolve(symbolProviders);
    }

    @Override
    public AggregatedCallSite createCallSite(Object symbol) {
        return new AggregatedStackTraces((ICallStackSymbol) symbol);
    }

    /**
     * Get a callsite from a stack trace. The array should be non-zero length and
     * the symbol at position 0 is the top of the stack, ie the last symbol called.
     *
     * @param dstGroup
     *            The element to which to add this stack trace
     * @param stackTrace
     *            The stack trace to add to the group
     * @param ts
     *            The timestamp at which this stack trace is added
     * @return The callsite
     * @throws ArrayIndexOutOfBoundsException
     *             Exception thrown if the stackTrace is empty
     */
    public AggregatedCallSite getCallSite(ICallStackElement dstGroup, long[] stackTrace, long ts) {
        if (stackTrace.length == 0) {
            throw new ArrayIndexOutOfBoundsException("Get callsite, the received array should not be null"); //$NON-NLS-1$
        }
        // Create the callsite for this stack trace
        AggregatedCallSite prevCallsite = createCallSite(CallStackSymbolFactory.createSymbol(stackTrace[stackTrace.length - 1], dstGroup, ts));
        for (int i = stackTrace.length - 2; i >= 0; i--) {
            AggregatedCallSite callsite = createCallSite(CallStackSymbolFactory.createSymbol(stackTrace[i], dstGroup, ts));
            callsite.addChild(prevCallsite);
            prevCallsite = callsite;
        }
        return prevCallsite;
    }

    /**
     * Method to implement to get the call stack from an event. It returns a
     * stack trace in the form or a Pair of {@link ICallStackElement} and
     * {@link AggregatedCallSite}.
     *
     * @param event
     *            The trace event to process
     * @return A pair of callstack element and aggregated callsite from this
     *         event
     */
    protected abstract @Nullable Pair<ICallStackElement, AggregatedCallSite> getProfiledStackTrace(ITmfEvent event);

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        CallGraph callgraph = executeForRange(TmfTimeRange.ETERNITY);
        if (callgraph == null) {
            return false;
        }
        fFullRangeCallGraph = callgraph;
        return true;
    }

    private @Nullable CallGraph executeForRange(TmfTimeRange range) {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new NullPointerException("Trace has not been set, yet the analysis is being run!"); //$NON-NLS-1$
        }
        /* Cancel any previous request */
        ITmfEventRequest request = fRequest;
        if ((request != null) && (!request.isCompleted())) {
            request.cancel();
        }

        try {
            CallGraph callGraph = new CallGraph();
            request = new ProfilingEventRequest(trace, callGraph, range);
            fRequest = request;
            trace.sendRequest(request);

            request.waitForCompletion();
            if (!request.isCompleted()) {
                return null;
            }
            return callGraph;
        } catch (InterruptedException e) {
            Activator.getInstance().logError("Request interrupted", e); //$NON-NLS-1$
        }
        return null;
    }

    @Override
    protected void canceling() {
        ITmfEventRequest req = fRequest;
        if ((req != null) && (!req.isCompleted())) {
            req.cancel();
        }
    }

    private class ProfilingEventRequest extends TmfEventRequest {

        private final ITmfTrace fTrace;
        private final CallGraph fCallGraph;

        /**
         * Constructor
         *
         * @param trace
         *            The trace
         * @param callgraph
         *            The callgraph to fill
         * @param range
         *            The time range of this request
         */
        public ProfilingEventRequest(ITmfTrace trace, CallGraph callgraph, TmfTimeRange range) {
            super(TmfEvent.class,
                    range,
                    0,
                    ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.BACKGROUND);
            fTrace = trace;
            fCallGraph = callgraph;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            if (event.getTrace() == fTrace) {
                processEvent(event);
            } else if (fTrace instanceof TmfExperiment) {
                /*
                 * If the request is for an experiment, check if the event is
                 * from one of the child trace
                 */
                for (ITmfTrace childTrace : ((TmfExperiment) fTrace).getTraces()) {
                    if (childTrace == event.getTrace()) {
                        processEvent(event);
                    }
                }
            }
        }

        private void processEvent(ITmfEvent event) {
            Pair<ICallStackElement, AggregatedCallSite> perfCallSite = getProfiledStackTrace(event);
            if (perfCallSite == null) {
                return;
            }
            fCallGraph.addAggregatedCallSite(perfCallSite.getFirst(), perfCallSite.getSecond());
        }
    }

}
