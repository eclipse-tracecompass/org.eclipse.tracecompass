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

package org.eclipse.tracecompass.analysis.profiling.core.tests.callgraph2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICallGraphProvider2;
import org.eclipse.tracecompass.analysis.profiling.core.tests.CallStackTestBase2;
import org.eclipse.tracecompass.analysis.profiling.core.tests.stubs2.CallGraphAnalysisStub;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.AggregatedCalledFunction;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.AggregatedCalledFunctionStatistics;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ModelManager;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.junit.After;
import org.junit.Test;

/**
 * Test the statistics of each node in the aggregation tree. This creates a
 * virtual state system in each test then tests the statistics of the
 * aggregation tree returned by CallGraphAnalysis.
 *
 * @author Sonia Farrah
 */
public class AggregatedCalledFunctionStatisticsTest {

    private static final String FIRST_AVERAGE_DURATION = "Test first function's average duration";
    private static final String FIRST_AVERAGE_SELF_TIME = "Test first function's average self time";
    private static final String FIRST_MAXIMUM_DURATION = "Test first function's maximum duration";
    private static final String FIRST_MAXIMUM_SELF_TIME = "Test first function's maximum self time";
    private static final String FIRST_MINIMUM_DURATION = "Test first function's minimum duration";
    private static final String FIRST_MINIMUM_SELF_TIME = "Test first function's minimum self time";
    private static final String FIRST_NUMBER_OF_SEGMENTS = "Test first function's number of segments";
    private static final String FIRST_SELF_TIME_STANDARD_DEVIATION = "Test first function's self time standard deviation";
    private static final String FIRST_STANDARD_DEVIATION = "Test first function's standard deviation";

    private static final String SECOND_AVERAGE_DURATION = "Test second function's average duration";
    private static final String SECOND_AVERAGE_SELF_TIME = "Test second function's average self time";
    private static final String SECOND_MAXIMUM_DURATION = "Test second function's maximum duration";
    private static final String SECOND_MAXIMUM_SELF_TIME = "Test second function's maximum self time";
    private static final String SECOND_MINIMUM_DURATION = "Test second function's minimum duration";
    private static final String SECOND_MINIMUM_SELF_TIME = "Test second function's minimum self time";
    private static final String SECOND_NUMBER_OF_CALLS = "Test second function's number of calls";
    private static final String SECOND_SELF_TIME_STANDARD_DEVIATION = "Test second function's self time standard deviation";
    private static final String SECOND_STANDARD_DEVIATION = "Test second function's standard deviation";

    private static final String MAIN_STANDARD_DEVIATION = "Test main's standard deviation";

    private static final String QUARK_0 = "0";
    private static final String QUARK_1 = "1";
    private static final String QUARK_2 = "2";
    private static final String QUARK_3 = "3";
    private static final double ERROR = 0.000001;

    private CallGraphAnalysisStub fCga;

    private static @NonNull ITmfStateSystemBuilder createFixture() {
        IStateHistoryBackend backend;
        backend = StateHistoryBackendFactory.createInMemoryBackend("Test", 0L);
        return StateSystemFactory.newStateSystem(backend);
    }

    private static List<ICallStackElement> getLeafElements(ICallStackElement group) {
        if (group.isLeaf()) {
            return Collections.singletonList(group);
        }
        List<ICallStackElement> leafGroups = new ArrayList<>();
        group.getChildrenElements().forEach(g -> leafGroups.addAll(getLeafElements(g)));
        return leafGroups;
    }

    private static @NonNull List<ICallStackElement> getLeafElements(ICallGraphProvider2 cga) {
        @SuppressWarnings("null")
        Collection<ICallStackElement> elements = cga.getCallGraph().getElements();
        List<ICallStackElement> leafGroups = new ArrayList<>();
        for (ICallStackElement group : elements) {
            leafGroups.addAll(getLeafElements(group));
        }
        return leafGroups;
    }

    /**
     * Dispose the callgraph analysis that has been set
     */
    @After
    public void disposeCga() {
        CallGraphAnalysisStub cga = fCga;
        if (cga != null) {
            cga.dispose();
        }
        ModelManager.disposeModels();
    }

    /**
     * The call stack's structure used in this test is shown below:
     *
     * <pre>
     *                 Aggregated tree
     *  ___ main___      ___ main___
     *   _1_    _1_  =>      _1_
     *   _1_                 _1_
     * </pre>
     */
    @Test
    public void treeStatisticsTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        // Build the state system
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        long statev = 0L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(100, (Object) null, quark);

        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = 1L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(50, (Object) null, quark);
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(90, (Object) null, quark);

        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_2);
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(30, (Object) null, quark);
        fixture.closeHistory(102);

        // Execute the CallGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        setCga(cga);
        cga.iterate();
        List<ICallStackElement> threads = getLeafElements(cga);
        // Test the threads generated by the analysis
        assertNotNull(threads);
        ICallStackElement thread = threads.get(0);
        assertNotNull(thread);
        Object[] children = cga.getCallGraph().getCallingContextTree(thread).toArray();
        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        Object[] firstFunctionChildren = firstFunction.getCallees().toArray();
        AggregatedCalledFunction secondFunction = (AggregatedCalledFunction) firstFunctionChildren[0];
        Object[] secondFunctionChildren = secondFunction.getCallees().toArray();
        AggregatedCalledFunction thirdFunction = (AggregatedCalledFunction) secondFunctionChildren[0];
        // Test the main statistics
        @NonNull AggregatedCalledFunctionStatistics mainStatistics1 = firstFunction.getFunctionStatistics();
        assertEquals("Test main's maximum duration", 100, mainStatistics1.getDurationStatistics().getMax());
        assertEquals("Test main's minimum duration", 100, mainStatistics1.getDurationStatistics().getMin());
        assertEquals("Test main's maximum self time", 20, mainStatistics1.getSelfTimeStatistics().getMax());
        assertEquals("Test main's minimum self time", 20, mainStatistics1.getSelfTimeStatistics().getMin());
        assertEquals("Test main's number of calls", 1, mainStatistics1.getDurationStatistics().getNbElements());
        assertEquals("Test main's average duration", 100, mainStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals(MAIN_STANDARD_DEVIATION, 20, mainStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals(MAIN_STANDARD_DEVIATION, Double.NaN, mainStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals(MAIN_STANDARD_DEVIATION, Double.NaN, mainStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the first function statistics
        @NonNull AggregatedCalledFunctionStatistics functionStatistics1 = secondFunction.getFunctionStatistics();
        assertEquals(FIRST_MAXIMUM_DURATION, 50, functionStatistics1.getDurationStatistics().getMax());
        assertEquals(FIRST_MINIMUM_DURATION, 30, functionStatistics1.getDurationStatistics().getMin());
        assertEquals(FIRST_MAXIMUM_SELF_TIME, 30, functionStatistics1.getSelfTimeStatistics().getMax());
        assertEquals("Test first function's mininmum self time", 20, functionStatistics1.getSelfTimeStatistics().getMin());
        assertEquals("Test first function's number of calls", 2, functionStatistics1.getDurationStatistics().getNbElements());
        assertEquals(FIRST_AVERAGE_DURATION, 40, functionStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals(FIRST_AVERAGE_SELF_TIME, 25, functionStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals(FIRST_STANDARD_DEVIATION, Double.NaN, functionStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals(FIRST_SELF_TIME_STANDARD_DEVIATION, Double.NaN, functionStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the third function statistics
        @NonNull AggregatedCalledFunctionStatistics functionStatistics2 = thirdFunction.getFunctionStatistics();
        assertEquals(SECOND_MAXIMUM_DURATION, 30, functionStatistics2.getDurationStatistics().getMax());
        assertEquals(SECOND_MINIMUM_DURATION, 30, functionStatistics2.getDurationStatistics().getMin());
        assertEquals(SECOND_MAXIMUM_SELF_TIME, 30, functionStatistics2.getSelfTimeStatistics().getMax());
        assertEquals(SECOND_MINIMUM_SELF_TIME, 30, functionStatistics2.getSelfTimeStatistics().getMin());
        assertEquals(SECOND_NUMBER_OF_CALLS, 1, functionStatistics2.getDurationStatistics().getNbElements());
        assertEquals(SECOND_AVERAGE_DURATION, 30, functionStatistics2.getDurationStatistics().getMean(), ERROR);
        assertEquals(SECOND_AVERAGE_SELF_TIME, 30, functionStatistics2.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals(SECOND_STANDARD_DEVIATION, Double.NaN, functionStatistics2.getDurationStatistics().getStdDev(), ERROR);
        assertEquals(SECOND_SELF_TIME_STANDARD_DEVIATION, Double.NaN, functionStatistics2.getSelfTimeStatistics().getStdDev(), ERROR);
    }

    /**
     * The call stack's structure used in this test is shown below:
     *
     * <pre>
     *                    Aggregated tree
     *  ___ main___        ___ main___
     *   _1_    _1_ =>         _1_
     *   _2_    _3_          _2_ _3_
     * </pre>
     */
    @Test
    public void mergeFirstLevelCalleesStatisticsTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        // Build the state system
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        Object statev = 0L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(100, (Object) null, quark);

        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = 1L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(50, (Object) null, quark);
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(90, (Object) null, quark);

        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_2);
        statev = 2L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(30, (Object) null, quark);
        statev = 3L;
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(80, (Object) null, quark);
        fixture.closeHistory(102);

        // Execute the CallGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        setCga(cga);
        cga.iterate();
        List<ICallStackElement> threads = getLeafElements(cga);
        assertNotNull(threads);
        ICallStackElement thread = threads.get(0);
        assertNotNull(thread);
        Object[] children = cga.getCallGraph().getCallingContextTree(thread).toArray();
        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        Object[] firstFunctionChildren = firstFunction.getCallees().toArray();
        AggregatedCalledFunction secondFunction = (AggregatedCalledFunction) firstFunctionChildren[0];
        AggregatedCalledFunction leaf1 = (AggregatedCalledFunction) secondFunction.getCallees().stream()
                .filter(acs -> CallStackTestBase2.getCallSiteSymbol(acs).resolve(Collections.emptySet()).equals("0x2"))
                .findAny().get();
        AggregatedCalledFunction leaf2 = (AggregatedCalledFunction) secondFunction.getCallees().stream()
                .filter(acs -> CallStackTestBase2.getCallSiteSymbol(acs).resolve(Collections.emptySet()).equals("0x3"))
                .findAny().get();
        // Test the first function statistics
        @NonNull AggregatedCalledFunctionStatistics functionStatistics1 = firstFunction.getFunctionStatistics();
        assertEquals(FIRST_MAXIMUM_DURATION, 100, functionStatistics1.getDurationStatistics().getMax());
        assertEquals(FIRST_MINIMUM_DURATION, 100, functionStatistics1.getDurationStatistics().getMin());
        assertEquals(FIRST_MAXIMUM_SELF_TIME, 20, functionStatistics1.getSelfTimeStatistics().getMax());
        assertEquals(FIRST_MINIMUM_SELF_TIME, 20, functionStatistics1.getSelfTimeStatistics().getMin());
        assertEquals(FIRST_NUMBER_OF_SEGMENTS, 1, functionStatistics1.getDurationStatistics().getNbElements());
        assertEquals(FIRST_AVERAGE_DURATION, 100, functionStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals(FIRST_AVERAGE_SELF_TIME, 20, functionStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals(FIRST_STANDARD_DEVIATION, Double.NaN, functionStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals(FIRST_STANDARD_DEVIATION, Double.NaN, functionStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the first function statistics
        @NonNull AggregatedCalledFunctionStatistics functionStatistics2 = secondFunction.getFunctionStatistics();
        assertEquals(SECOND_MAXIMUM_DURATION, 50, functionStatistics2.getDurationStatistics().getMax());
        assertEquals(SECOND_MINIMUM_DURATION, 30, functionStatistics2.getDurationStatistics().getMin());
        assertEquals(SECOND_MAXIMUM_SELF_TIME, 20, functionStatistics2.getSelfTimeStatistics().getMax());
        assertEquals(SECOND_MINIMUM_SELF_TIME, 10, functionStatistics2.getSelfTimeStatistics().getMin());
        assertEquals(SECOND_NUMBER_OF_CALLS, 2, functionStatistics2.getDurationStatistics().getNbElements());
        assertEquals(SECOND_AVERAGE_DURATION, 40, functionStatistics2.getDurationStatistics().getMean(), ERROR);
        assertEquals(SECOND_AVERAGE_SELF_TIME, 15, functionStatistics2.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals(SECOND_STANDARD_DEVIATION, Double.NaN, functionStatistics2.getDurationStatistics().getStdDev(), ERROR);
        assertEquals(SECOND_STANDARD_DEVIATION, Double.NaN, functionStatistics2.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the first leaf statistics
        AggregatedCalledFunctionStatistics leafStatistics1 = leaf1.getFunctionStatistics();
        assertEquals("Test first leaf's maximum duration", 30, leafStatistics1.getDurationStatistics().getMax());
        assertEquals("Test first leaf's minimum duration", 30, leafStatistics1.getDurationStatistics().getMin());
        assertEquals("Test first leaf's maximum self time", 30, leafStatistics1.getSelfTimeStatistics().getMax());
        assertEquals("Test first leaf's minimum self time", 30, leafStatistics1.getSelfTimeStatistics().getMin());
        assertEquals("Test first leaf's number of calls", 1, leafStatistics1.getDurationStatistics().getNbElements());
        assertEquals("Test first leaf's minimum duration", 30, leafStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test first leaf's average self time", 30, leafStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test first leaf's standard deviation", Double.NaN, leafStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test first leaf's self time standard deviation", Double.NaN, leafStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the second leaf statistics
        AggregatedCalledFunctionStatistics leafStatistics2 = leaf2.getFunctionStatistics();
        assertEquals("Test second leaf's maximum duration", 20, leafStatistics2.getDurationStatistics().getMax());
        assertEquals("Test second leaf's minimum duration", 20, leafStatistics2.getDurationStatistics().getMin());
        assertEquals("Test second leaf's maximum self time", 20, leafStatistics2.getSelfTimeStatistics().getMax());
        assertEquals("Test second leaf's minimum self time", 20, leafStatistics2.getSelfTimeStatistics().getMin());
        assertEquals("Test second leaf's number of calls", 1, leafStatistics2.getDurationStatistics().getNbElements());
        assertEquals("Test second leaf's average duration", 20, leafStatistics2.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test second leaf's average self time", 20, leafStatistics2.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test second leaf's standard deviation", Double.NaN, leafStatistics2.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test second leaf's self time standard deviation", Double.NaN, leafStatistics2.getSelfTimeStatistics().getStdDev(), ERROR);
    }

    /**
     * The call stack's structure used in this test is shown below:
     *
     * <pre>
     *              Aggregated tree
     * _1_  _1_  =>    _1_
     * _2_  _3_      _2_ _3_
     * </pre>
     */
    @Test
    public void multiFunctionRootsTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        // Create the first root function
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        Object statev = 1L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(20, (Object) null, quark);
        // Create the second root function
        fixture.modifyAttribute(30, statev, quark);
        fixture.modifyAttribute(80, (Object) null, quark);
        // Create the first root function's callee
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = 2L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(10, (Object) null, quark);
        // Create the second root function's callee
        statev = 3L;
        fixture.modifyAttribute(30, statev, quark);
        fixture.modifyAttribute(40, (Object) null, quark);
        fixture.closeHistory(81);

        // Execute the callGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        setCga(cga);
        cga.iterate();
        List<ICallStackElement> threads = getLeafElements(cga);
        // Test the threads generated by the analysis
        assertNotNull(threads);
        ICallStackElement thread = threads.get(0);
        assertNotNull(thread);
        Object[] children = cga.getCallGraph().getCallingContextTree(thread).toArray();
        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        Object[] firstFunctionChildren = firstFunction.getCallees().toArray();
        AggregatedCalledFunction function2 = (AggregatedCalledFunction) firstFunctionChildren[0];
        AggregatedCalledFunction function3 = (AggregatedCalledFunction) firstFunctionChildren[1];
        // Test the first function statistics
        @NonNull AggregatedCalledFunctionStatistics functionStatistics1 = firstFunction.getFunctionStatistics();
        assertEquals(FIRST_MAXIMUM_DURATION, 50, functionStatistics1.getDurationStatistics().getMax());
        assertEquals(FIRST_MINIMUM_DURATION, 20, functionStatistics1.getDurationStatistics().getMin());
        assertEquals(FIRST_MAXIMUM_SELF_TIME, 40, functionStatistics1.getSelfTimeStatistics().getMax());
        assertEquals(FIRST_MINIMUM_SELF_TIME, 10, functionStatistics1.getSelfTimeStatistics().getMin());
        assertEquals(FIRST_NUMBER_OF_SEGMENTS, 2, functionStatistics1.getDurationStatistics().getNbElements());
        assertEquals(FIRST_AVERAGE_DURATION, 35, functionStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals(FIRST_AVERAGE_SELF_TIME, 25, functionStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals(FIRST_STANDARD_DEVIATION, Double.NaN, functionStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals(FIRST_SELF_TIME_STANDARD_DEVIATION, Double.NaN, functionStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the second function statistics
        @NonNull AggregatedCalledFunctionStatistics functionStatistics2 = function2.getFunctionStatistics();
        assertEquals(SECOND_MAXIMUM_DURATION, 10, functionStatistics2.getDurationStatistics().getMax());
        assertEquals(SECOND_MINIMUM_DURATION, 10, functionStatistics2.getDurationStatistics().getMin());
        assertEquals(SECOND_MAXIMUM_SELF_TIME, 10, functionStatistics2.getSelfTimeStatistics().getMax());
        assertEquals(SECOND_MINIMUM_SELF_TIME, 10, functionStatistics2.getSelfTimeStatistics().getMin());
        assertEquals(SECOND_NUMBER_OF_CALLS, 1, functionStatistics2.getDurationStatistics().getNbElements());
        assertEquals(SECOND_AVERAGE_DURATION, 10, functionStatistics2.getDurationStatistics().getMean(), ERROR);
        assertEquals(SECOND_AVERAGE_SELF_TIME, 10, functionStatistics2.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals(SECOND_STANDARD_DEVIATION, Double.NaN, functionStatistics2.getDurationStatistics().getStdDev(), ERROR);
        assertEquals(SECOND_SELF_TIME_STANDARD_DEVIATION, Double.NaN, functionStatistics2.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the third function statistics
        @NonNull AggregatedCalledFunctionStatistics functionStatistics3 = function3.getFunctionStatistics();
        assertEquals("Test third function's maximum duration", 10, functionStatistics3.getDurationStatistics().getMax());
        assertEquals("Test third function's minimum duration", 10, functionStatistics3.getDurationStatistics().getMin());
        assertEquals("Test third function's maximum selftime", 10, functionStatistics3.getSelfTimeStatistics().getMax());
        assertEquals("Test third function's minimum self time", 10, functionStatistics3.getSelfTimeStatistics().getMin());
        assertEquals("Test third function's number of calls", 1, functionStatistics3.getDurationStatistics().getNbElements());
        assertEquals("Test third function's average duration", 10, functionStatistics3.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test third function's average self time", 10, functionStatistics3.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test third function's standard deviation", Double.NaN, functionStatistics3.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test third function's self time standard deviation", Double.NaN, functionStatistics3.getSelfTimeStatistics().getStdDev(), ERROR);
    }

    /**
     * Build a call stack example.This call stack's structure is shown below :
     *
     * <pre>
     *      ___ main____
     *  ___1___    _1_  _1_
     *  _2_ _3_    _2_
     *  _4_        _4_
     * </pre>
     */
    private static void buildCallStack(ITmfStateSystemBuilder fixture) {
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        // Create the first function
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        Object statev = 0L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(150, (Object) null, quark);
        // Create the first level functions
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = 1L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(50, (Object) null, quark);
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(100, (Object) null, quark);
        fixture.modifyAttribute(130, statev, quark);
        fixture.modifyAttribute(150, (Object) null, quark);
        // Create the third function
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_2);
        statev = 2L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(10, (Object) null, quark);

        statev = 3L;
        fixture.modifyAttribute(20, statev, quark);
        fixture.modifyAttribute(30, (Object) null, quark);

        statev = 2L;
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(90, (Object) null, quark);

        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_3);
        statev = 4L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(10, (Object) null, quark);

        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(80, (Object) null, quark);
        fixture.closeHistory(151);
    }

    /**
     * The call stack's structure used in this test is shown below:
     *
     * <pre>
     *                          Aggregated tree
     *     ___ main____          ____ main____
     *  ___1___    _1_ _1_            _1_
     *  _2_ _3_    _2_      =>      _2_ _3_
     *  _4_        _4_              _4_
     * </pre>
     */
    @Test
    public void mergeSecondLevelCalleesTest() {
        ITmfStateSystemBuilder fixture = createFixture();

        buildCallStack(fixture);
        // Execute the CallGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        setCga(cga);
        cga.iterate();
        List<ICallStackElement> threads = getLeafElements(cga);
        // Test the threads generated by the analysis
        assertNotNull(threads);
        // Test the threads generated by the analysis
        assertNotNull(threads);
        ICallStackElement thread = threads.get(0);
        assertNotNull(thread);
        Object[] children = cga.getCallGraph().getCallingContextTree(thread).toArray();
        AggregatedCalledFunction main = (AggregatedCalledFunction) children[0];
        Object[] mainChildren = main.getCallees().toArray();
        AggregatedCalledFunction function1 = (AggregatedCalledFunction) mainChildren[0];
        AggregatedCalledFunction function2 = (AggregatedCalledFunction) function1.getCallees().stream()
                .filter(acs -> CallStackTestBase2.getCallSiteSymbol(acs).resolve(Collections.emptySet()).equals("0x2"))
                .findAny().get();
        AggregatedCalledFunction function3 = (AggregatedCalledFunction) function1.getCallees().stream()
                .filter(acs -> CallStackTestBase2.getCallSiteSymbol(acs).resolve(Collections.emptySet()).equals("0x3"))
                .findAny().get();
        Object[] firstChildCallee = function2.getCallees().toArray();
        AggregatedCalledFunction function4 = (AggregatedCalledFunction) firstChildCallee[0];
        // Test the main function statistics
        AggregatedCalledFunctionStatistics mainStatistics1 = main.getFunctionStatistics();
        assertEquals("Test main's maximum duration", 150, mainStatistics1.getDurationStatistics().getMax());
        assertEquals("Test main's minimum duration", 150, mainStatistics1.getDurationStatistics().getMin());
        assertEquals("Test main's maximum self time", 40, mainStatistics1.getSelfTimeStatistics().getMax());
        assertEquals("Test main's minimum self time", 40, mainStatistics1.getSelfTimeStatistics().getMin());
        assertEquals("Test main's number of calls", 1, mainStatistics1.getDurationStatistics().getNbElements());
        assertEquals("Test main's average duration", 150, mainStatistics1.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test main's average self time", 40, mainStatistics1.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals(MAIN_STANDARD_DEVIATION, Double.NaN, mainStatistics1.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test main's self time standard deviation", Double.NaN, mainStatistics1.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the first function statistics
        AggregatedCalledFunctionStatistics firstFunctionStatistics = function1.getFunctionStatistics();
        assertEquals(FIRST_MAXIMUM_DURATION, 50, firstFunctionStatistics.getDurationStatistics().getMax());
        assertEquals(FIRST_MINIMUM_DURATION, 20, firstFunctionStatistics.getDurationStatistics().getMin());
        assertEquals(FIRST_MAXIMUM_SELF_TIME, 30, firstFunctionStatistics.getSelfTimeStatistics().getMax());
        assertEquals(FIRST_MINIMUM_SELF_TIME, 10, firstFunctionStatistics.getSelfTimeStatistics().getMin());
        assertEquals(FIRST_NUMBER_OF_SEGMENTS, 3, firstFunctionStatistics.getDurationStatistics().getNbElements());
        assertEquals(FIRST_AVERAGE_DURATION, 36.666666667, firstFunctionStatistics.getDurationStatistics().getMean(), ERROR);
        assertEquals(FIRST_AVERAGE_SELF_TIME, 20, firstFunctionStatistics.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals(FIRST_STANDARD_DEVIATION, 15.275252316, firstFunctionStatistics.getDurationStatistics().getStdDev(), ERROR);
        assertEquals(FIRST_SELF_TIME_STANDARD_DEVIATION, 10, firstFunctionStatistics.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the second function statistics
        AggregatedCalledFunctionStatistics secondFunctionStatistics2 = function2.getFunctionStatistics();
        assertEquals(SECOND_MAXIMUM_DURATION, 30, secondFunctionStatistics2.getDurationStatistics().getMax());
        assertEquals(SECOND_MINIMUM_DURATION, 10, secondFunctionStatistics2.getDurationStatistics().getMin());
        assertEquals(SECOND_MAXIMUM_SELF_TIME, 10, secondFunctionStatistics2.getSelfTimeStatistics().getMax());
        assertEquals(SECOND_MINIMUM_SELF_TIME, 0, secondFunctionStatistics2.getSelfTimeStatistics().getMin());
        assertEquals("Test second function's number of segments", 2, secondFunctionStatistics2.getDurationStatistics().getNbElements());
        assertEquals(SECOND_AVERAGE_DURATION, 20, secondFunctionStatistics2.getDurationStatistics().getMean(), ERROR);
        assertEquals(SECOND_AVERAGE_SELF_TIME, 5, secondFunctionStatistics2.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals(SECOND_STANDARD_DEVIATION, Double.NaN, secondFunctionStatistics2.getDurationStatistics().getStdDev(), ERROR);
        assertEquals(SECOND_SELF_TIME_STANDARD_DEVIATION, Double.NaN, secondFunctionStatistics2.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the third function statistics
        AggregatedCalledFunctionStatistics thirdFunctionStatistics3 = function3.getFunctionStatistics();
        assertEquals("Test third function's maximum duration", 10, thirdFunctionStatistics3.getDurationStatistics().getMax());
        assertEquals("Test third function's minimum duration", 10, thirdFunctionStatistics3.getDurationStatistics().getMin());
        assertEquals("Test third function's maximum self time", 10, thirdFunctionStatistics3.getSelfTimeStatistics().getMax());
        assertEquals("Test third function's minimum self time", 10, thirdFunctionStatistics3.getSelfTimeStatistics().getMin());
        assertEquals("Test third function's number of segments", 1, thirdFunctionStatistics3.getDurationStatistics().getNbElements());
        assertEquals("Test third function's average duration", 10, thirdFunctionStatistics3.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test third function's average self time", 10, thirdFunctionStatistics3.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test third function's self time deviation", Double.NaN, thirdFunctionStatistics3.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test third function's self time standard deviation", Double.NaN, thirdFunctionStatistics3.getSelfTimeStatistics().getStdDev(), ERROR);
        // Test the fourth function statistics
        AggregatedCalledFunctionStatistics fourthFunctionStatistics4 = function4.getFunctionStatistics();
        assertEquals("Test fourth function's maximum duration", 20, fourthFunctionStatistics4.getDurationStatistics().getMax());
        assertEquals("Test fourth function's minimum duration", 10, fourthFunctionStatistics4.getDurationStatistics().getMin());
        assertEquals("Test fourth function's maximum self time", 20, fourthFunctionStatistics4.getSelfTimeStatistics().getMax());
        assertEquals("Test fourth function's maximum self time", 10, fourthFunctionStatistics4.getSelfTimeStatistics().getMin());
        assertEquals("Test fourth function's number of segments", 2, fourthFunctionStatistics4.getDurationStatistics().getNbElements());
        assertEquals("Test fourth function's average duration", 15, fourthFunctionStatistics4.getDurationStatistics().getMean(), ERROR);
        assertEquals("Test fourth function's average duration", 15, fourthFunctionStatistics4.getSelfTimeStatistics().getMean(), ERROR);
        assertEquals("Test fourth function's standard deviation", Double.NaN, fourthFunctionStatistics4.getDurationStatistics().getStdDev(), ERROR);
        assertEquals("Test fourth function's self time deviation", Double.NaN, fourthFunctionStatistics4.getSelfTimeStatistics().getStdDev(), ERROR);
    }

    private void setCga(CallGraphAnalysisStub cga) {
        fCga = cga;
    }
}
