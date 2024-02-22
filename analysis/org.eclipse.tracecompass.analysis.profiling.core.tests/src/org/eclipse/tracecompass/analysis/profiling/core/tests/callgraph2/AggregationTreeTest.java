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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.CallGraph;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICallGraphProvider2;
import org.eclipse.tracecompass.analysis.profiling.core.tests.ActivatorTest;
import org.eclipse.tracecompass.analysis.profiling.core.tests.CallStackTestBase2;
import org.eclipse.tracecompass.analysis.profiling.core.tests.stubs2.CallGraphAnalysisStub;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.AggregatedCalledFunction;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ModelManager;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test the CallGraphAnalysis. This creates a virtual state system in each test
 * and tests the aggregation tree returned by the CallGraphAnalysis.
 *
 * @author Sonia Farrah
 */
public class AggregationTreeTest {
    private static final String CALLSTACK_FILE = "testfiles/traces/callstack.xml";

    private static final String FIRST_FUNCTION = "Children number: First function";
    private static final String FIRST_FUNCTION_DURATION = "Test first function's duration";
    private static final String FIRST_FUNCTION_NUMBER_OF_CALLS = "Test first function's number of calls";
    private static final String FIRST_FUNCTION_SELF_TIME = "Test first function's self time";

    private static final String SECOND_FUNCTION = "Children number: Second function";
    private static final String SECOND_FUNCTION_DURATION = "Test second function's duration";
    private static final String SECOND_FUNCTION_NUMBER_OF_CALLS = "Test second function's number of calls";
    private static final String SECOND_FUNCTION_SELF_TIME = "Test second function's self time";

    private static final String THIRD_FUNCTION = "Children number: Third function";
    private static final String THIRD_FUNCTION_DURATION = "Test third function's duration";
    private static final String THIRD_FUNCTION_NUMBER_OF_CALLS = "Test third function's number of calls";
    private static final String THIRD_FUNCTION_SELF_TIME = "Test third function's self time";

    private static final String THREAD = "Thread";
    private static final String THREAD_NAME = "Thread name";
    private static final String THREAD_NODES_FOUND = "Number of thread nodes found";
    private static final String ROOT_FUNCTIONS = "Number of root functions";

    private ITmfTrace fTrace;

    private static final String QUARK_0 = "0";
    private static final String QUARK_1 = "1";
    private static final String QUARK_2 = "2";
    private static final String QUARK_3 = "3";
    private static final Integer SMALL_AMOUNT_OF_SEGMENT = 3;
    private static final int LARGE_AMOUNT_OF_SEGMENTS = 1000;

    private static @NonNull ITmfStateSystemBuilder createFixture() {
        IStateHistoryBackend backend;
        backend = StateHistoryBackendFactory.createInMemoryBackend("Test", 0L);
        return StateSystemFactory.newStateSystem(backend);
    }

    private CallGraphAnalysisStub fCga;

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
     * Open a flamegraph
     */
    @Before
    public void before() {
        TmfXmlTraceStub trace = new TmfXmlTraceStubNs();
        IPath filePath = ActivatorTest.getAbsoluteFilePath(CALLSTACK_FILE);
        IStatus status = trace.validate(null, filePath.toOSString());
        if (!status.isOK()) {
            fail(status.getException().getMessage());
        }
        try {
            trace.initTrace(null, filePath.toOSString(), TmfEvent.class);
        } catch (TmfTraceException e) {
            fail(e.getMessage());
        }
        fTrace = trace;
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, trace, null);
        trace.traceOpened(signal);
        TmfTraceManager.getInstance().traceOpened(signal);
    }

    /**
     * Cleanup models
     */
    @After
    public void after() {
        ModelManager.disposeModels();
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
        ITmfTrace trace = fTrace;
        if (trace != null) {
            TmfTraceManager.getInstance().traceClosed(new TmfTraceClosedSignal(this, trace));
            trace.dispose();
        }
    }

    /**
     * Test an empty state system.
     */
    @Test
    public void emptyStateSystemTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        fixture.closeHistory(1002);
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        setCga(cga);
        cga.iterate();
        Collection<ICallStackElement> threads = getLeafElements(cga);
        assertNotNull(threads);
        assertEquals("Number of threads found", 0, threads.size());
    }

    /**
     * Test cascade state system. The call stack's structure used in this test
     * is shown below:
     *
     * <pre>
     *  ________
     *   ______
     *    ____
     *
     * </pre>
     */
    @Test
    public void cascadeTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        // Build the state system
        long start = 1;
        long end = 1001;
        int threadQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH);
        int parentQuark = fixture.getQuarkRelativeAndAdd(threadQuark, CallGraphAnalysisStub.CALLSTACK_PATH);
        fixture.updateOngoingState(TmfStateValue.newValueLong(100), threadQuark);
        for (int i = 1; i <= SMALL_AMOUNT_OF_SEGMENT; i++) {
            int quark = fixture.getQuarkRelativeAndAdd(parentQuark, Integer.toString(i));
            long statev = i;
            fixture.modifyAttribute(start, (Object) null, quark);
            fixture.modifyAttribute(start + i, statev, quark);
            fixture.modifyAttribute(end - i, (Object) null, quark);
        }

        fixture.closeHistory(1002);
        // Execute the CallGraphAnalysis

        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        setCga(cga);
        cga.iterate();
        List<ICallStackElement> threads = getLeafElements(cga);
        // Test the threads generated by the analysis
        assertNotNull(threads);
        assertEquals(THREAD_NODES_FOUND, 1, threads.size());
        ICallStackElement group = threads.get(0);
        CallGraph cg = cga.getCallGraph();
        assertNotNull(group);
        assertEquals(ROOT_FUNCTIONS, 1, cg.getCallingContextTree(group).size());
        assertEquals("Thread id", THREAD, group.getName());
        Object[] children = cg.getCallingContextTree(group).toArray();
        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        assertEquals(FIRST_FUNCTION, 1, firstFunction.getCallees().size());
        Object @NonNull [] firstFunctionChildren = firstFunction.getCallees().toArray();
        AggregatedCalledFunction secondFunction = (AggregatedCalledFunction) firstFunctionChildren[0];
        assertEquals(SECOND_FUNCTION, 1, secondFunction.getCallees().size());
        Object @NonNull [] secondFunctionChildren = secondFunction.getCallees().toArray();
        AggregatedCalledFunction thirdFunction = (AggregatedCalledFunction) secondFunctionChildren[0];
        assertEquals(THIRD_FUNCTION, 0, thirdFunction.getCallees().size());
        // Test duration
        assertEquals(FIRST_FUNCTION_DURATION, 998, firstFunction.getDuration());
        assertEquals(SECOND_FUNCTION_DURATION, 996, secondFunction.getDuration());
        assertEquals(THIRD_FUNCTION_DURATION, 994, thirdFunction.getDuration());
        // Test self time
        assertEquals(FIRST_FUNCTION_SELF_TIME, 2, firstFunction.getSelfTime());
        assertEquals(SECOND_FUNCTION_SELF_TIME, 2, secondFunction.getSelfTime());
        assertEquals(THIRD_FUNCTION_SELF_TIME, 994, thirdFunction.getSelfTime());
        // Test number of calls
        assertEquals("Test first function's nombre of calls", 1, firstFunction.getNbCalls());
        assertEquals("Test second function's nombre of calls", 1, secondFunction.getNbCalls());
        assertEquals("Test third function's nombre of calls", 1, thirdFunction.getNbCalls());
    }

    /**
     * Test a state system with a two calls for the same function. The call
     * stack's structure used in this test is shown below:
     *
     * <pre>
     *                 Aggregated tree
     *  ___ main___      ___ main___
     *   _1_    _1_  =>      _1_
     *   _1_                 _1_
     * </pre>
     */
    @Test
    public void treeTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        // Build the state system
        int threadQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH);
        int parentQuark = fixture.getQuarkRelativeAndAdd(threadQuark, CallGraphAnalysisStub.CALLSTACK_PATH);
        fixture.updateOngoingState(TmfStateValue.newValueDouble(0.001), threadQuark);
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
        @NonNull
        List<ICallStackElement> threads = getLeafElements(cga);
        // Test the threads generated by the analysis
        assertNotNull(threads);
        assertEquals(THREAD_NODES_FOUND, 1, threads.size());
        assertEquals(THREAD_NAME, THREAD, threads.get(0).getName());
        @SuppressWarnings("null")
        Object[] children = cga.getCallGraph().getCallingContextTree(threads.get(0)).toArray();
        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        assertEquals(FIRST_FUNCTION, 1, firstFunction.getCallees().size());
        Object[] firstFunctionChildren = firstFunction.getCallees().toArray();
        AggregatedCalledFunction secondFunction = (AggregatedCalledFunction) firstFunctionChildren[0];
        assertEquals(SECOND_FUNCTION, 1, secondFunction.getCallees().size());
        Object[] secondFunctionChildren = secondFunction.getCallees().toArray();
        AggregatedCalledFunction thirdFunction = (AggregatedCalledFunction) secondFunctionChildren[0];
        assertEquals(THIRD_FUNCTION, 0, thirdFunction.getCallees().size());
        // Test duration
        assertEquals(FIRST_FUNCTION_DURATION, 100, firstFunction.getDuration());
        assertEquals(SECOND_FUNCTION_DURATION, 80, secondFunction.getDuration());
        assertEquals(THIRD_FUNCTION_DURATION, 30, thirdFunction.getDuration());
        // Test self time
        assertEquals(FIRST_FUNCTION_SELF_TIME, 20, firstFunction.getSelfTime());
        assertEquals(SECOND_FUNCTION_SELF_TIME, 50, secondFunction.getSelfTime());
        assertEquals(THIRD_FUNCTION_SELF_TIME, 30, thirdFunction.getSelfTime());
        // Test number of calls
        assertEquals(FIRST_FUNCTION_NUMBER_OF_CALLS, 1, firstFunction.getNbCalls());
        assertEquals(SECOND_FUNCTION_NUMBER_OF_CALLS, 2, secondFunction.getNbCalls());
        assertEquals(THIRD_FUNCTION_NUMBER_OF_CALLS, 1, thirdFunction.getNbCalls());
    }

    /**
     * Test the callees merge. The call stack's structure used in this test is
     * shown below:
     *
     * <pre>
     *                    Aggregated tree
     *  ___ main___        ___ main___
     *   _1_    _1_ =>         _1_
     *   _2_    _3_          _2_ _3_
     * </pre>
     */
    @SuppressWarnings("null")
    @Test
    public void mergeFirstLevelCalleesTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        // Build the state system
        int threadQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, "123");
        int parentQuark = fixture.getQuarkRelativeAndAdd(threadQuark, CallGraphAnalysisStub.CALLSTACK_PATH);
        fixture.updateOngoingState(TmfStateValue.newValueDouble(0.001), threadQuark);
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        Long statev = 0L;
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
        String @NonNull [] tp = { "*" };
        String @NonNull [] pp = { CallGraphAnalysisStub.PROCESS_PATH };
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture, ImmutableList.of(pp, tp));
        setCga(cga);
        cga.iterate();
        List<ICallStackElement> threads = getLeafElements(cga);

        CallGraph cg = cga.getCallGraph();
        // Test the threads generated by the analysis
        assertNotNull(threads);
        assertEquals(THREAD_NODES_FOUND, 1, threads.size());
        assertEquals(THREAD_NAME, "123", threads.get(0).getName());
        assertEquals(ROOT_FUNCTIONS, 1, cg.getCallingContextTree(threads.get(0)).size());
        Object[] children = cg.getCallingContextTree(threads.get(0)).toArray();

        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        assertEquals(FIRST_FUNCTION, 1, firstFunction.getCallees().size());
        Object[] firstFunctionChildren = firstFunction.getCallees().toArray();
        AggregatedCalledFunction secondFunction = (AggregatedCalledFunction) firstFunctionChildren[0];
        assertEquals(SECOND_FUNCTION, 2, secondFunction.getCallees().size());
        AggregatedCalledFunction leaf1 = (AggregatedCalledFunction) secondFunction.getCallees().stream()
                .filter(acs -> CallStackTestBase2.getCallSiteSymbol(acs).resolve(Collections.emptySet()).equals("0x2"))
                .findAny().get();
        AggregatedCalledFunction leaf2 = (AggregatedCalledFunction) secondFunction.getCallees().stream()
                .filter(acs -> CallStackTestBase2.getCallSiteSymbol(acs).resolve(Collections.emptySet()).equals("0x3"))
                .findAny().get();
        assertEquals("Children number: First leaf function", 0, leaf1.getCallees().size());
        assertEquals("Children number: Second leaf function", 0, leaf2.getCallees().size());
        // Test duration
        assertEquals(FIRST_FUNCTION_DURATION, 100, firstFunction.getDuration());
        assertEquals(SECOND_FUNCTION_DURATION, 80, secondFunction.getDuration());
        assertEquals("Test first leaf's duration", 30, leaf1.getDuration());
        assertEquals("Test second leaf's duration", 20, leaf2.getDuration());
        // Test self time
        assertEquals(FIRST_FUNCTION_SELF_TIME, 20, firstFunction.getSelfTime());
        assertEquals(SECOND_FUNCTION_SELF_TIME, 30, secondFunction.getSelfTime());
        assertEquals("Test first leaf's self time", 30, leaf1.getSelfTime());
        assertEquals("Test second leaf's self time", 20, leaf2.getSelfTime());
        // Test number of calls
        assertEquals(FIRST_FUNCTION_NUMBER_OF_CALLS, 1, firstFunction.getNbCalls());
        assertEquals(SECOND_FUNCTION_NUMBER_OF_CALLS, 2, secondFunction.getNbCalls());
        assertEquals("Test first leaf's number of calls", 1, leaf1.getNbCalls());
        assertEquals("Test second leaf's number of calls", 1, leaf2.getNbCalls());
    }

    /**
     * Build a call stack example.This call stack's structure is shown below :
     *
     * <pre>
     *  ___ main____
     *  ___1___    _1_
     *  _2_ _3_    _2_
     *  _4_        _4_
     * </pre>
     */
    private static void buildCallStack(ITmfStateSystemBuilder fixture) {
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        // Create the first function
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        Long statev = 0L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(100, (Object) null, quark);
        // Create the first level functions
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = 1L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(50, (Object) null, quark);
        fixture.modifyAttribute(60, statev, quark);
        fixture.modifyAttribute(100, (Object) null, quark);
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
        fixture.closeHistory(102);
    }

    /**
     * Test the merge of The callees children. The call stack's structure used
     * in this test is shown below:
     *
     * <pre>
     *                      Aggregated tree
     *   ___ main____        ____ main____
     *  ___1___    _1_            _1_
     *  _2_ _3_    _2_  =>      _2_ _3_
     *  _4_        _4_          _4_
     * </pre>
     */
    @SuppressWarnings("null")
    @Test
    public void mergeSecondLevelCalleesTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        buildCallStack(fixture);
        // Execute the CallGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        setCga(cga);
        cga.iterate();
        List<ICallStackElement> threads = getLeafElements(cga);
        CallGraph cg = cga.getCallGraph();
        // Test the threads generated by the analysis
        assertNotNull(threads);
        assertEquals(THREAD_NODES_FOUND, 1, threads.size());
        assertEquals(THREAD_NAME, THREAD, threads.get(0).getName());
        assertEquals(ROOT_FUNCTIONS, 1, cg.getCallingContextTree(threads.get(0)).size());
        Object[] children = cg.getCallingContextTree(threads.get(0)).toArray();
        AggregatedCalledFunction main = (AggregatedCalledFunction) children[0];
        assertEquals("Children number: main", 1, main.getCallees().size());
        Object[] mainChildren = main.getCallees().toArray();
        AggregatedCalledFunction function1 = (AggregatedCalledFunction) mainChildren[0];
        assertEquals("Children number: first function", 2, function1.getCallees().size());
        AggregatedCalledFunction function2 = (AggregatedCalledFunction) function1.getCallees().stream()
                .filter(acs -> CallStackTestBase2.getCallSiteSymbol(acs).resolve(Collections.emptySet()).equals("0x2"))
                .findAny().get();
        AggregatedCalledFunction function3 = (AggregatedCalledFunction) function1.getCallees().stream()
                .filter(acs -> CallStackTestBase2.getCallSiteSymbol(acs).resolve(Collections.emptySet()).equals("0x3"))
                .findAny().get();
        assertEquals("Children number: First child", 1, function2.getCallees().size());
        assertEquals("Children number: Second child", 0, function3.getCallees().size());
        Object[] firstChildCallee = function2.getCallees().toArray();
        AggregatedCalledFunction function4 = (AggregatedCalledFunction) firstChildCallee[0];
        assertEquals("Children number: leaf function", 0, function4.getCallees().size());
        // Test duration
        assertEquals("Test main's duration", 100, main.getDuration());
        assertEquals(FIRST_FUNCTION_DURATION, 90, function1.getDuration());
        assertEquals("Test first child's duration", 40, function2.getDuration());
        assertEquals("Test second child's duration", 10, function3.getDuration());
        assertEquals("Test leaf's duration", 30, function4.getDuration());
        // Test self time
        assertEquals("Test main's self time", 10, main.getSelfTime());
        assertEquals(FIRST_FUNCTION_SELF_TIME, 40, function1.getSelfTime());
        assertEquals("Test first child's self time", 10,
                function2.getSelfTime());
        assertEquals("Test second child's self time", 10, function3.getSelfTime());
        assertEquals("Test leaf's self time", 30, function4.getSelfTime());
        // Test number of calls
        assertEquals("Test main's number of calls", 1, main.getNbCalls());
        assertEquals(FIRST_FUNCTION_NUMBER_OF_CALLS, 2, function1.getNbCalls());
        assertEquals("Test first child's number of calls", 2, function2.getNbCalls());
        assertEquals("Test second child's number of calls", 1, function3.getNbCalls());
        assertEquals("Test leaf's number of calls", 2, function4.getNbCalls());
    }

    /**
     * Test state system with a large amount of segments. All segments have the
     * same length. The call stack's structure used in this test is shown below:
     *
     * <pre>
     * _____
     * _____
     * _____
     * .....
     * </pre>
     */
    @Test
    public void largeTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        for (int i = 0; i < LARGE_AMOUNT_OF_SEGMENTS; i++) {
            Long statev = (long) i;
            fixture.pushAttribute(0, statev, parentQuark);
        }
        for (int i = 0; i < LARGE_AMOUNT_OF_SEGMENTS; i++) {
            fixture.popAttribute(10, parentQuark);
        }
        fixture.closeHistory(11);
        // Execute the callGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        setCga(cga);
        cga.iterate();
        List<ICallStackElement> threads = getLeafElements(cga);
        // Test the threads generated by the analysis
        assertNotNull(threads);
        assertEquals(THREAD_NAME, THREAD, threads.get(0).getName());
        @SuppressWarnings("null")
        Object[] children = cga.getCallGraph().getCallingContextTree(threads.get(0)).toArray();
        AggregatedCalledFunction parent = (AggregatedCalledFunction) children[0];
        for (int i = 1; i < LARGE_AMOUNT_OF_SEGMENTS; i++) {
            children = parent.getCallees().toArray();
            AggregatedCalledFunction child = (AggregatedCalledFunction) children[0];
            parent = child;
        }
    }

    /**
     * Test mutliRoots state system.This tests if a root function called twice
     * will be merged into one function or not. The call stack's structure used
     * in this test is shown below:
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
        Long statev = 1L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(20, (Object) null, quark);
        // Create the second root function
        fixture.modifyAttribute(30, statev, quark);
        fixture.modifyAttribute(50, (Object) null, quark);
        // Create the first root function's callee
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = 2L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(10, (Object) null, quark);
        // Create the second root function's callee
        statev = 3L;
        fixture.modifyAttribute(30, statev, quark);
        fixture.modifyAttribute(40, (Object) null, quark);
        fixture.closeHistory(51);

        // Execute the callGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        setCga(cga);
        cga.iterate();
        List<ICallStackElement> threads = getLeafElements(cga);
        CallGraph cg = cga.getCallGraph();
        // Test the threads generated by the analysis
        assertNotNull(threads);
        assertEquals(THREAD_NODES_FOUND, 1, threads.size());
        ICallStackElement group = threads.get(0);
        assertNotNull(group);
        assertEquals("Thread id", THREAD, group.getName());
        assertEquals(ROOT_FUNCTIONS, 1, cg.getCallingContextTree(group).size());
        Object[] children = cg.getCallingContextTree(group).toArray();
        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        assertEquals(FIRST_FUNCTION, 2, firstFunction.getCallees().size());
        Object[] firstFunctionChildren = firstFunction.getCallees().toArray();
        AggregatedCalledFunction function2 = (AggregatedCalledFunction) firstFunctionChildren[0];
        AggregatedCalledFunction function3 = (AggregatedCalledFunction) firstFunctionChildren[1];
        assertEquals(SECOND_FUNCTION, 0, function2.getCallees().size());
        assertEquals(THIRD_FUNCTION, 0, function3.getCallees().size());
        // Test duration
        assertEquals(FIRST_FUNCTION_DURATION, 40, firstFunction.getDuration());
        assertEquals(SECOND_FUNCTION_DURATION, 10, function2.getDuration());
        assertEquals(THIRD_FUNCTION_DURATION, 10, function3.getDuration());
        // Test self time
        assertEquals(FIRST_FUNCTION_SELF_TIME, 20, firstFunction.getSelfTime());
        assertEquals(SECOND_FUNCTION_SELF_TIME, 10, function2.getSelfTime());
        assertEquals(THIRD_FUNCTION_SELF_TIME, 10, function2.getSelfTime());
        // Test number of calls
        assertEquals(FIRST_FUNCTION_NUMBER_OF_CALLS, 2, firstFunction.getNbCalls());
        assertEquals(SECOND_FUNCTION_NUMBER_OF_CALLS, 1, function2.getNbCalls());
        assertEquals(THIRD_FUNCTION_NUMBER_OF_CALLS, 1, function3.getNbCalls());
    }

    /**
     * Test mutliRoots state system. The call stack's structure used in this
     * test is shown below:
     *
     * <pre>
     *                Aggregated tree
     * _0_  _1_   =>   _0_  _1_
     * _2_  _2_        _2_  _2_
     *
     * </pre>
     */
    @SuppressWarnings("null")
    @Test
    public void multiFunctionRootsSecondTest() {
        ITmfStateSystemBuilder fixture = createFixture();
        int parentQuark = fixture.getQuarkAbsoluteAndAdd(CallGraphAnalysisStub.PROCESS_PATH, CallGraphAnalysisStub.THREAD_PATH, CallGraphAnalysisStub.CALLSTACK_PATH);
        // Create the first root function
        int quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_0);
        Long statev = 0L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(20, (Object) null, quark);
        // Create the second root function
        statev = 1L;
        fixture.modifyAttribute(30, statev, quark);
        fixture.modifyAttribute(50, (Object) null, quark);
        // Create the first root function's callee
        quark = fixture.getQuarkRelativeAndAdd(parentQuark, QUARK_1);
        statev = 2L;
        fixture.modifyAttribute(0, statev, quark);
        fixture.modifyAttribute(10, (Object) null, quark);
        // Create the second root function's callee
        fixture.modifyAttribute(30, statev, quark);
        fixture.modifyAttribute(40, (Object) null, quark);
        fixture.closeHistory(51);

        // Execute the callGraphAnalysis
        CallGraphAnalysisStub cga = new CallGraphAnalysisStub(fixture);
        setCga(cga);
        cga.iterate();
        List<ICallStackElement> threads = getLeafElements(cga);
        CallGraph cg = cga.getCallGraph();
        // Test the threads generated by the analysis
        assertNotNull(threads);
        assertEquals(THREAD_NODES_FOUND, 1, threads.size());
        assertEquals(THREAD_NAME, THREAD, threads.get(0).getName());
        assertEquals(ROOT_FUNCTIONS, 2, cg.getCallingContextTree(threads.get(0)).size());
        Object[] children = cg.getCallingContextTree(threads.get(0)).toArray();
        AggregatedCalledFunction firstFunction = (AggregatedCalledFunction) children[0];
        AggregatedCalledFunction secondFunction = (AggregatedCalledFunction) children[1];

        assertEquals(FIRST_FUNCTION, 1, firstFunction.getCallees().size());
        assertEquals(SECOND_FUNCTION, 1, secondFunction.getCallees().size());
        Object[] firstFunctionChildren = firstFunction.getCallees().toArray();
        Object[] secondFunctionChildren = secondFunction.getCallees().toArray();
        AggregatedCalledFunction function3 = (AggregatedCalledFunction) firstFunctionChildren[0];
        AggregatedCalledFunction function4 = (AggregatedCalledFunction) secondFunctionChildren[0];

        assertEquals("Children number: third function", 0, function3.getCallees().size());
        assertEquals("Children number: fourth function", 0, function4.getCallees().size());
        // Test duration
        assertEquals(SECOND_FUNCTION_DURATION, 20, firstFunction.getDuration());
        assertEquals(SECOND_FUNCTION_DURATION, 20, secondFunction.getDuration());
        assertEquals("Test first leaf's duration", 10, function3.getDuration());
        assertEquals("Test second leaf's duration", 10, function4.getDuration());
        // Test self time
        assertEquals(FIRST_FUNCTION_SELF_TIME, 10, firstFunction.getSelfTime());
        assertEquals(SECOND_FUNCTION_DURATION, 10, secondFunction.getSelfTime());
        assertEquals(SECOND_FUNCTION_SELF_TIME, 10, function3.getSelfTime());
        assertEquals(SECOND_FUNCTION_SELF_TIME, 10, function4.getSelfTime());
        // Test number of calls
        assertEquals(FIRST_FUNCTION_NUMBER_OF_CALLS, 1, firstFunction.getNbCalls());
        assertEquals(FIRST_FUNCTION_NUMBER_OF_CALLS, 1, secondFunction.getNbCalls());
        assertEquals(THIRD_FUNCTION_NUMBER_OF_CALLS, 1, function3.getNbCalls());
        assertEquals(THIRD_FUNCTION_NUMBER_OF_CALLS, 1, function4.getNbCalls());
    }

    /**
     * Gets the call graph analysis
     *
     * @return the call graph analysis
     */
    protected CallGraphAnalysisStub getCga() {
        return fCga;
    }

    private void setCga(CallGraphAnalysisStub cga) {
        fCga = cga;
    }

    /**
     * Get a trace for when a trace is needed
     *
     * @return A trace, that has no relation with the callgraphs of these tests
     */
    protected @NonNull ITmfTrace getTrace() {
        return Objects.requireNonNull(fTrace);
    }
}
