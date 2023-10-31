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

package org.eclipse.tracecompass.analysis.profiling.core.tests.perf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.internal.analysis.profiling.core.tree.AllGroupDescriptor;
import org.eclipse.tracecompass.internal.analysis.profiling.core.tree.WeightedTreeGroupBy;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.callgraph.CallGraph;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.junit.Test;

/**
 * Benchmarks the flame chart analysis execution and call graph execution,
 * partial callgraph for time ranges and group by of call graph.
 *
 * This base class can be extended by any performance test for analysis that
 * implement {@link ICallGraphProvider}, whether or not it also implements
 * {@link IFlameChartProvider}.
 *
 * @author Geneviève Bastien
 */
public abstract class CallStackAndGraphBenchmark {

    /**
     * Test ID for kernel analysis benchmarks
     */
    public static final String TEST_ID = "org.eclipse.tracecompass.analysis#CallStack#";

    private static final String TEST_CALLSTACK_BUILD = "Building Callstack (%s)";
    private static final String TEST_CALLSTACK_PARSESEGSTORE = "Callstack segment store (%s)";
    private static final String TEST_CALLGRAPH_BUILD = "Building CallGraph (%s)";
    private static final String TEST_CALLGRAPH_QUERY = "CallGraph Query (%s)";
    private static final String TEST_CALLGRAPH_GROUPBY = "CallGraph Group By (%s)";

    private static final byte[] SEED = { 0x45, 0x73, 0x74, 0x65, 0x6c, 0x6c, 0x65 };

    private static final int LOOP_COUNT = 5;

    private final String fName;
    private final String fAnalysisId;

    /**
     * Constructor
     *
     * @param name
     *            The name of this test
     * @param analysisId
     *            the ID of the analysis to run this benchmark on
     */
    public CallStackAndGraphBenchmark(String name, String analysisId) {
        fName = name;
        fAnalysisId = analysisId;
    }

    /**
     * Run benchmark for the trace
     *
     * @throws TmfTraceException
     *             Exceptions thrown getting the trace
     */
    @Test
    public void runCpuBenchmark() throws TmfTraceException {
        Performance perf = Performance.getDefault();
        PerformanceMeter callStackBuildPm = Objects.requireNonNull(perf.createPerformanceMeter(TEST_ID + String.format(TEST_CALLSTACK_BUILD, fName)));
        perf.tagAsSummary(callStackBuildPm, String.format(TEST_CALLSTACK_BUILD, fName), Dimension.CPU_TIME);
        PerformanceMeter callStackSegStorePm = Objects.requireNonNull(perf.createPerformanceMeter(TEST_ID + String.format(TEST_CALLSTACK_PARSESEGSTORE, fName)));
        perf.tagAsSummary(callStackSegStorePm, String.format(TEST_CALLSTACK_PARSESEGSTORE, fName), Dimension.CPU_TIME);
        PerformanceMeter callgraphBuildPm = Objects.requireNonNull(perf.createPerformanceMeter(TEST_ID + String.format(TEST_CALLGRAPH_BUILD, fName)));
        perf.tagAsSummary(callgraphBuildPm, String.format(TEST_CALLGRAPH_BUILD, fName), Dimension.CPU_TIME);
        PerformanceMeter callgraphQueryPm = perf.createPerformanceMeter(TEST_ID + String.format(TEST_CALLGRAPH_QUERY, fName));
        perf.tagAsSummary(callgraphQueryPm, String.format(TEST_CALLGRAPH_QUERY, fName), Dimension.CPU_TIME);
        PerformanceMeter callgraphGroupByPm = perf.createPerformanceMeter(TEST_ID + String.format(TEST_CALLGRAPH_GROUPBY, fName));
        perf.tagAsSummary(callgraphGroupByPm, String.format(TEST_CALLGRAPH_GROUPBY, fName), Dimension.CPU_TIME);

        boolean isFlameChartProvider = false;
        for (int i = 0; i < LOOP_COUNT; i++) {
            TmfTrace trace = null;
            try {
                trace = getTrace();
                trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));
                IAnalysisModule analysisModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, IAnalysisModule.class, fAnalysisId);
                assertTrue(analysisModule instanceof ICallGraphProvider);
                ICallGraphProvider callGraphModule = (ICallGraphProvider) analysisModule;

                if (analysisModule instanceof IFlameChartProvider) {
                    // Do the performance test for the instrumented call stack,
                    // then the call graph building
                    isFlameChartProvider = true;
                    benchmarkInstrumented((IFlameChartProvider) analysisModule, callStackBuildPm, callStackSegStorePm, callgraphBuildPm);
                } else {
                    benchmarkCallGraphProvider(callGraphModule, callgraphBuildPm);
                }

                /*
                 * Common benchmarks for both instrumented and profiled
                 * callgraphs
                 */
                // We just read the trace for the first time, so it should be
                // safe to use the end time
                long startTime = trace.getStartTime().toNanos();
                long endTime = trace.getEndTime().toNanos();
                long delta = endTime - startTime;

                // Get partial callgraphs
                SecureRandom randomGenerator = new SecureRandom(SEED);
                callgraphQueryPm.start();
                for (int j = 0; j < 50; j++) {
                    long time0 = Math.abs(randomGenerator.nextLong()) % delta;
                    long time1 = Math.abs(randomGenerator.nextLong()) % delta;
                    callGraphModule.getCallGraph(TmfTimestamp.fromNanos(startTime + Math.min(time0, time1)), TmfTimestamp.fromNanos(startTime + Math.max(time0, time1)));
                }
                callgraphQueryPm.stop();

                // Benchmark the group by. Do a few iterations in different
                // orders
                List<IWeightedTreeGroupDescriptor> descriptors = new ArrayList<>();
                descriptors.add(AllGroupDescriptor.getInstance());
                descriptors.addAll(callGraphModule.getGroupDescriptors());
                CallGraph callGraphToGroup = callGraphModule.getCallGraph();
                callgraphGroupByPm.start();
                for (int j = 0; j < 10; j++) {
                    descriptors.forEach(group -> WeightedTreeGroupBy.groupWeightedTreeBy(group, callGraphToGroup, callGraphModule));
                    Collections.reverse(descriptors);
                    descriptors.forEach(group -> WeightedTreeGroupBy.groupWeightedTreeBy(group, callGraphToGroup, callGraphModule));
                }
                callgraphGroupByPm.stop();

                /*
                 * Delete the supplementary files, so that the next iteration
                 * rebuilds the state system.
                 */
                File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(trace));
                for (File file : suppDir.listFiles()) {
                    file.delete();
                }
            } finally {
                if (trace != null) {
                    trace.dispose();
                }
            }
        }
        if (isFlameChartProvider) {
            callStackBuildPm.commit();
            callStackSegStorePm.commit();
        }
        callgraphBuildPm.commit();
        callgraphQueryPm.commit();
        callgraphGroupByPm.commit();
    }

    private static void benchmarkCallGraphProvider(ICallGraphProvider callGraphModule, PerformanceMeter callgraphBuildPm) {
        // Do the performance test for building the callgraph only
        callgraphBuildPm.start();
        TmfTestHelper.executeAnalysis((IAnalysisModule) callGraphModule);
        callgraphBuildPm.stop();
        CallGraph callGraph = callGraphModule.getCallGraph();

        assertTrue(!callGraph.getElements().isEmpty());
    }

    private static void benchmarkInstrumented(IFlameChartProvider analysisModule, PerformanceMeter callStackBuildPm, PerformanceMeter callStackSegStorePm, PerformanceMeter callgraphBuildPm) {
        // Set the instrumented analysis to not trigger the call graph
        // automatically, we will do it when ready
        if (analysisModule instanceof InstrumentedCallStackAnalysis) {
            ((InstrumentedCallStackAnalysis) analysisModule).triggerAutomatically(false);
        }

        // Benchmark the call stack analysis
        callStackBuildPm.start();
        TmfTestHelper.executeAnalysis(analysisModule);
        callStackBuildPm.stop();

        // Benchmark the segment store iteration
        ISegmentStore<ISegment> segmentStore = analysisModule.getSegmentStore();
        assertNotNull(segmentStore);
        callStackSegStorePm.start();
        // Iterate through the whole segment store
        Iterator<ISegment> iterator = segmentStore.iterator();
        while (iterator.hasNext()) {
            iterator.next();
        }
        callStackSegStorePm.stop();

        // Getting the callgraph will schedule the analysis and wait for its
        // completion
        callgraphBuildPm.start();
        CallGraph callGraph = ((ICallGraphProvider) analysisModule).getCallGraph();
        callgraphBuildPm.stop();

        assertTrue(!callGraph.getElements().isEmpty());
    }

    /**
     * Get the trace for this analysis. Every call to getTrace() should return a
     * fresh trace fully initialized. The caller is responsible to dispose the
     * trace when not required anymore
     *
     * @return A freshly initialized trace
     * @throws TmfTraceException
     *             Exceptions thrown getting the trace
     */
    protected abstract TmfTrace getTrace() throws TmfTraceException;
}
