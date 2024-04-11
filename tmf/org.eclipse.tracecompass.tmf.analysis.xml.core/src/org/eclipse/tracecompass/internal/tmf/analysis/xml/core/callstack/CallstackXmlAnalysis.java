/*******************************************************************************
 * Copyright (c) 2017, 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.analysis.xml.core.callstack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.IDataPalette;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.CallGraph;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICallGraphProvider2;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackHostUtils.IHostIdResolver;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSeries;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.SymbolAspect;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FunctionTidAspect;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.Activator;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.callstack.CallstackXmlModuleHelper.ISubModuleHelper;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlUtils;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfAnalysisModuleWithStateSystems;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableList;

/**
 *
 *
 * @author Geneviève Bastien
 */
public class CallstackXmlAnalysis extends TmfAbstractAnalysisModule implements IFlameChartProvider, ITmfAnalysisModuleWithStateSystems, ICallGraphProvider2 {

    private final Path fSourceFile;
    private final ISubModuleHelper fHelper;
    private @Nullable IAnalysisModule fModule = null;
    private @Nullable CallStackSeries fCallStacks = null;
    private final CallGraphAnalysis fCallGraph;
    private boolean fHasTid = false;

    private final ListenerList<IAnalysisProgressListener> fListeners = new ListenerList<>(ListenerList.IDENTITY);

    /**
     * Constructor
     *
     * @param sourceFile
     *            The source file containing this callstack analysis
     * @param helper
     *            The helper for the dependent module
     */
    public CallstackXmlAnalysis(Path sourceFile, ISubModuleHelper helper) {
        super();
        fSourceFile = sourceFile;
        fHelper = helper;
        fCallGraph = new CallGraphAnalysis(this);
    }

    @Override
    public @Nullable CallStackSeries getCallStackSeries() {
        CallStackSeries series = fCallStacks;
        if (series == null) {
            IAnalysisModule module = getAnalysisModule();
            if (!(module instanceof ITmfAnalysisModuleWithStateSystems)) {
                return null;
            }
            Iterator<ITmfStateSystem> stateSystems = ((ITmfAnalysisModuleWithStateSystems) module).getStateSystems().iterator();
            if (!stateSystems.hasNext()) {
                return null;
            }
            ITmfStateSystem ss = stateSystems.next();
            Path xmlFile = fSourceFile;
            final String pathString = xmlFile.toString();
            Element doc = TmfXmlUtils.getElementInFile(pathString, TmfXmlStrings.CALLSTACK, getId());
            if (doc == null) {
                fCallStacks = null;
                return null;
            }

            /* parser for defined Fields */
            List<Element> callStackElements = TmfXmlUtils.getChildElements(doc, TmfXmlStrings.CALLSTACK_GROUP);
            if (callStackElements.size() > 1) {
                Activator.logWarning("More than one callstack series defined. Only the first one will be displayed"); //$NON-NLS-1$
            } else if (callStackElements.isEmpty()) {
                fCallStacks = null;
                return null;
            }
            Element callStackElement = callStackElements.get(0);

            List<String[]> patterns = new ArrayList<>();
            for (Element child : TmfXmlUtils.getChildElements(callStackElement, TmfXmlStrings.CALLSTACK_LEVEL)) {
                String attribute = child.getAttribute(TmfXmlStrings.CALLSTACK_PATH);
                patterns.add(attribute.split("/")); //$NON-NLS-1$
            }

            // Build the thread resolver
            List<Element> childElements = TmfXmlUtils.getChildElements(callStackElement, TmfXmlStrings.CALLSTACK_THREAD);
            IThreadIdResolver resolver = null;
            if (!childElements.isEmpty()) {
                Element threadElement = childElements.get(0);
                String attribute = threadElement.getAttribute(TmfXmlStrings.CALLSTACK_THREADCPU);
                if (!attribute.isEmpty()) {
                    resolver = new CallStackSeries.CpuResolver(attribute.split("/")); //$NON-NLS-1$
                } else {
                    attribute = threadElement.getAttribute(TmfXmlStrings.CALLSTACK_THREADLEVEL);
                    if (!attribute.isEmpty()) {
                        String type = threadElement.getAttribute(TmfXmlStrings.CALLSTACK_THREADLEVEL_TYPE);
                        if (type.equals(TmfXmlStrings.CALLSTACK_THREADLEVEL_VALUE)) {
                            resolver = new CallStackSeries.AttributeValueThreadResolver(Integer.valueOf(attribute));
                        } else {
                            resolver = new CallStackSeries.AttributeNameThreadResolver(Integer.valueOf(attribute));
                        }
                    }
                }
            }
            fHasTid = (resolver != null);

            // Build the host resolver
            childElements = TmfXmlUtils.getChildElements(callStackElement, TmfXmlStrings.CALLSTACK_HOST);
            IHostIdResolver hostResolver = null;
            if (!childElements.isEmpty()) {
                Element hostElement = childElements.get(0);
                String attribute = hostElement.getAttribute(TmfXmlStrings.CALLSTACK_THREADLEVEL);
                if (!attribute.isEmpty()) {
                    String type = hostElement.getAttribute(TmfXmlStrings.CALLSTACK_THREADLEVEL_TYPE);
                    if (type.equals(TmfXmlStrings.CALLSTACK_THREADLEVEL_VALUE)) {
                        hostResolver = new CallStackHostUtils.AttributeValueHostResolver(Integer.valueOf(attribute));
                    } else {
                        hostResolver = new CallStackHostUtils.AttributeNameHostResolver(Integer.valueOf(attribute));
                    }
                }
            }
            hostResolver = hostResolver == null ? new CallStackHostUtils.TraceHostIdResolver(Objects.requireNonNull(getTrace())) : hostResolver;
            fHasTid = resolver != null;
            series = new CallStackSeries(ss, patterns, 0, callStackElement.getAttribute(TmfXmlStrings.NAME), hostResolver, resolver);
            fCallStacks = series;
        }
        return series;
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule == null) {
            return false;
        }
        boolean ret = analysisModule.waitForCompletion(monitor);
        if (!ret) {
            return ret;
        }
        ISegmentStore<ISegment> segmentStore = getSegmentStore();
        if (segmentStore != null) {
            sendUpdate(segmentStore);
        }
        fCallGraph.schedule();
        return true;
    }

    @Override
    protected void canceling() {
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule != null) {
            analysisModule.cancel();
        }
        fCallGraph.cancel();
    }

    @Override
    public void dispose() {
        /*
         * The sub-analyses are not registered to the trace directly, so we need
         * to tell them when the trace is disposed.
         */
        super.dispose();
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule != null) {
            analysisModule.dispose();
        }
        fCallGraph.dispose();
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
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new NullPointerException("Trace should not be null at this point"); //$NON-NLS-1$
        }
        IAnalysisModule module = getAnalysisModule();

        if (module == null) {
            return Collections.emptyList();
        }
        return Collections.singleton(module);

    }

    private synchronized @Nullable IAnalysisModule getAnalysisModule() {
        IAnalysisModule module = fModule;
        if (module == null) {
            ITmfTrace trace = getTrace();
            if (trace == null) {
                return null;
            }
            module = fHelper.getAnalysis(trace);
            if (module != null) {
                fModule = module;
            }
        }
        return module;
    }

    @Override
    public @Nullable ITmfStateSystem getStateSystem(String id) {
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule instanceof ITmfAnalysisModuleWithStateSystems) {
            return ((ITmfAnalysisModuleWithStateSystems) analysisModule).getStateSystem(id);
        }
        return null;
    }

    @Override
    public Iterable<ITmfStateSystem> getStateSystems() {
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule instanceof ITmfAnalysisModuleWithStateSystems) {
            return ((ITmfAnalysisModuleWithStateSystems) analysisModule).getStateSystems();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean waitForInitialization() {
        IAnalysisModule analysisModule = getAnalysisModule();
        if (analysisModule instanceof ITmfAnalysisModuleWithStateSystems) {
            return ((ITmfAnalysisModuleWithStateSystems) analysisModule).waitForInitialization();
        }
        return false;
    }

    @Override
    public Collection<IWeightedTreeGroupDescriptor> getGroupDescriptors() {
        fCallGraph.schedule();
        fCallGraph.waitForCompletion();
        return fCallGraph.getGroupDescriptors();
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
    public AggregatedCallSite createCallSite(Object symbol) {
        return fCallGraph.createCallSite(symbol);
    }

    @Override
    public String getTitle() {
        return fCallGraph.getTitle();
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
        if (fHasTid) {
            return ImmutableList.of(FunctionTidAspect.TID_ASPECT, SymbolAspect.SYMBOL_ASPECT);
        }
        return Collections.singletonList(SymbolAspect.SYMBOL_ASPECT);
    }

    @Override
    public @Nullable ISegmentStore<ISegment> getSegmentStore() {
        CallStackSeries series = getCallStackSeries();
        if (series == null) {
            return null;
        }
        return series;
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

    @Override
    public boolean isComplete() {
        // Initialization error, but the analysis is completed
        if (!waitForInitialization()) {
            return true;
        }
        Iterator<ITmfStateSystem> iterator = getStateSystems().iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("The initialization is complete, so the state system must not be null"); //$NON-NLS-1$
        }
        return iterator.next().waitUntilBuilt(0);
    }

    @Override
    public long getEnd() {
        // Initialization error, but the analysis is completed
        if (!waitForInitialization()) {
            return Integer.MIN_VALUE;
        }
        Iterator<ITmfStateSystem> iterator = getStateSystems().iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("The initialization is complete, so the state system must not be null"); //$NON-NLS-1$
        }
        return iterator.next().getCurrentEndTime();
    }

    @Override
    public List<String> getExtraDataSets() {
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
        fCallGraph.schedule();
        fCallGraph.waitForCompletion();
        return fCallGraph.getPalette();
    }

    @Override
    public IHostIdResolver getHostIdResolver() {
        return new CallStackHostUtils.TraceHostIdResolver(getTrace());
    }

}
