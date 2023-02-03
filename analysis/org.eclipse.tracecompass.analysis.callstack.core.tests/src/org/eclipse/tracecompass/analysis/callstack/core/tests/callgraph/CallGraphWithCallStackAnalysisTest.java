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

package org.eclipse.tracecompass.analysis.callstack.core.tests.callgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.callstack.core.tests.CallStackTestBase;
import org.eclipse.tracecompass.analysis.callstack.core.tests.stubs.CallStackAnalysisStub;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.AggregatedCalledFunction;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.InstrumentedCallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.CompositeHostModel;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.ICpuTimeProvider;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.IHostModel;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.ModelManager;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.junit.After;
import org.junit.Test;

/**
 * Test the callgraph analysis with the call stack trace and module
 *
 * @author Geneviève Bastien
 */
public class CallGraphWithCallStackAnalysisTest extends CallStackTestBase {

    private static final String UNKNOWN = "Unknown process in callstack";
    private static final String UNKNOWN_PROCESS = UNKNOWN + ": ";
    private static final String UNKNOWN_SYMBOL = "Unknown symbol for thread 2: ";

    /**
     * Clean up the memory
     */
    @After
    public void cleanUp() {
        // Model objects use weak hash map, we garbage-collect here to "make
        // sure" there are no artifacts in memory; manual timely GC never
        // guaranteed-
        System.gc();
    }

    /**
     * Test the callgraph with a small trace
     */
    @Test
    public void testCallGraph() {
        CallStackAnalysisStub cga = getModule();
        CallGraph cg = cga.getCallGraph();

        try {
            @SuppressWarnings("null")
            Collection<ICallStackElement> elements = cg.getElements();
            for (ICallStackElement group : elements) {
                String firstLevelName = group.getName();
                switch (firstLevelName) {
                case "1":
                    verifyProcess1(cg, group);
                    break;
                case "5":
                    verifyProcess5(cg, group);
                    break;
                default:
                    fail(UNKNOWN);
                }
            }
        } finally {
            cga.dispose();
        }
    }

    @SuppressWarnings("null")
    private static void verifyProcess1(CallGraph cg, ICallStackElement element) {
        Collection<ICallStackElement> secondLevels = element.getChildrenElements();
        assertEquals(2, secondLevels.size());
        for (ICallStackElement secondLevel : secondLevels) {
            assertTrue(secondLevel instanceof InstrumentedCallStackElement);
            assertTrue(secondLevel.isLeaf());
            String secondLevelName = secondLevel.getName();
            Collection<AggregatedCallSite> children = cg.getCallingContextTree(secondLevel);
            switch (secondLevelName) {
            case "2":
                assertEquals(2, children.size());
                for (AggregatedCallSite child : children) {
                    assertTrue(child instanceof AggregatedCalledFunction);
                    AggregatedCalledFunction func = (AggregatedCalledFunction) child;
                    switch (getCallSiteSymbol(func).resolve(Collections.emptySet())) {
                    case "op1":
                        assertEquals(9, func.getDuration());
                        assertEquals(5, func.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, func.getCpuTime());
                        assertEquals(1, func.getNbCalls());
                        assertEquals(1, func.getProcessId());
                        assertEquals(1, func.getCallees().size());
                        AggregatedCalledFunction next = (AggregatedCalledFunction) func.getCallees().iterator().next();
                        assertNotNull(next);
                        assertEquals(4, next.getDuration());
                        assertEquals(3, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(1, next.getProcessId());
                        assertEquals("op2", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(1, next.getCallees().size());
                        AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                        assertNotNull(third);
                        assertEquals(1, third.getDuration());
                        assertEquals(1, third.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, third.getCpuTime());
                        assertEquals(1, third.getNbCalls());
                        assertEquals(1, third.getProcessId());
                        assertEquals("op3", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                        assertEquals(0, third.getCallees().size());
                        break;
                    case "op4":
                        assertEquals(8, func.getDuration());
                        assertEquals(8, func.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, func.getCpuTime());
                        assertEquals(1, func.getNbCalls());
                        assertEquals(1, func.getProcessId());
                        assertEquals(0, func.getCallees().size());
                        break;
                    default:
                        fail(UNKNOWN_SYMBOL + getCallSiteSymbol(func));
                    }
                }
                break;
            case "3":
                assertEquals(1, children.size());
                AggregatedCalledFunction func = (AggregatedCalledFunction) children.iterator().next();
                assertEquals("op2", getCallSiteSymbol(func).resolve(Collections.emptySet()));

                assertEquals(17, func.getDuration());
                assertEquals(10, func.getSelfTime());
                assertEquals(IHostModel.TIME_UNKNOWN, func.getCpuTime());
                assertEquals(1, func.getNbCalls());
                assertEquals(1, func.getProcessId());
                assertEquals(2, func.getCallees().size());
                for (AggregatedCallSite nextChild : func.getCallees()) {
                    assertTrue(nextChild instanceof AggregatedCalledFunction);
                    AggregatedCalledFunction next = (AggregatedCalledFunction) nextChild;
                    switch (getCallSiteSymbol(next).resolve(Collections.emptySet())) {
                    case "op3":
                        assertEquals(1, next.getDuration());
                        assertEquals(1, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(1, next.getProcessId());
                        assertEquals(0, next.getCallees().size());
                        break;
                    case "op2":
                        assertEquals(6, next.getDuration());
                        assertEquals(6, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(1, next.getProcessId());
                        assertEquals(0, next.getCallees().size());
                        break;
                    default:
                        fail(UNKNOWN_SYMBOL + getCallSiteSymbol(func));
                    }
                }

                break;
            default:
                fail(UNKNOWN_PROCESS + secondLevelName);
            }
        }
    }

    @SuppressWarnings("null")
    private static void verifyProcess5(CallGraph cg, ICallStackElement element) {
        Collection<ICallStackElement> secondLevels = element.getChildrenElements();
        assertEquals(2, secondLevels.size());
        for (ICallStackElement secondLevel : secondLevels) {
            assertTrue(secondLevel instanceof InstrumentedCallStackElement);
            assertTrue(secondLevel.isLeaf());
            String secondLevelName = secondLevel.getName();
            Collection<AggregatedCallSite> children = cg.getCallingContextTree(secondLevel);
            switch (secondLevelName) {
            case "6": {
                assertEquals(1, children.size());
                AggregatedCalledFunction func = (AggregatedCalledFunction) children.iterator().next();
                assertNotNull(func);
                assertEquals("op1", getCallSiteSymbol(func).resolve(Collections.emptySet()));
                assertEquals(19, func.getDuration());
                assertEquals(3, func.getSelfTime());
                assertEquals(IHostModel.TIME_UNKNOWN, func.getCpuTime());
                assertEquals(1, func.getNbCalls());
                assertEquals(5, func.getProcessId());
                assertEquals(3, func.getCallees().size());

                for (AggregatedCallSite nextChild : func.getCallees()) {
                    assertTrue(nextChild instanceof AggregatedCalledFunction);
                    AggregatedCalledFunction next = (AggregatedCalledFunction) nextChild;
                    switch (getCallSiteSymbol(next).resolve(Collections.emptySet())) {
                    case "op2": {
                        assertEquals(3, next.getDuration());
                        assertEquals(2, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(5, next.getProcessId());
                        assertEquals("op2", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(1, next.getCallees().size());
                        AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                        assertNotNull(third);
                        assertEquals(1, third.getDuration());
                        assertEquals(1, third.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, third.getCpuTime());
                        assertEquals(1, third.getNbCalls());
                        assertEquals(5, third.getProcessId());
                        assertEquals("op3", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                        assertEquals(0, third.getCallees().size());
                    }
                        break;
                    case "op3": {
                        assertEquals(5, next.getDuration());
                        assertEquals(3, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(5, next.getProcessId());
                        assertEquals("op3", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(1, next.getCallees().size());
                        AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                        assertNotNull(third);
                        assertEquals(2, third.getDuration());
                        assertEquals(2, third.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, third.getCpuTime());
                        assertEquals(1, third.getNbCalls());
                        assertEquals(5, third.getProcessId());
                        assertEquals("op1", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                        assertEquals(0, third.getCallees().size());
                    }
                        break;
                    case "op4":
                        assertEquals(8, next.getDuration());
                        assertEquals(8, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(5, next.getProcessId());
                        assertEquals("op4", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(0, next.getCallees().size());
                        break;
                    default:
                        fail("Unknown symbol for second level of tid 6");
                    }
                }
            }
                break;
            case "7": {
                assertEquals(1, children.size());
                AggregatedCalledFunction func = (AggregatedCalledFunction) children.iterator().next();
                assertNotNull(func);
                assertEquals("op5", getCallSiteSymbol(func).resolve(Collections.emptySet()));
                assertEquals(19, func.getDuration());
                assertEquals(7, func.getSelfTime());
                assertEquals(IHostModel.TIME_UNKNOWN, func.getCpuTime());
                assertEquals(1, func.getNbCalls());
                assertEquals(5, func.getProcessId());
                assertEquals(1, func.getCallees().size());

                // Verify children
                Iterator<AggregatedCallSite> iterator = func.getCallees().iterator();
                AggregatedCalledFunction next = (AggregatedCalledFunction) iterator.next();
                assertNotNull(next);
                assertEquals(12, next.getDuration());
                assertEquals(11, next.getSelfTime());
                assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                assertEquals(3, next.getNbCalls());
                assertEquals(5, next.getProcessId());
                assertEquals("op2", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                assertEquals(1, next.getCallees().size());
                AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                assertNotNull(third);
                assertEquals(1, third.getDuration());
                assertEquals(1, third.getSelfTime());
                assertEquals(IHostModel.TIME_UNKNOWN, third.getCpuTime());
                assertEquals(1, third.getNbCalls());
                assertEquals(5, third.getProcessId());
                assertEquals("op3", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                assertEquals(0, third.getCallees().size());
            }
                break;
            default:
                fail(UNKNOWN_PROCESS + secondLevelName);
            }
        }
    }

    private CallGraphAnalysis getCallGraphModule() throws TmfAnalysisException {
        CallGraphAnalysis cga = new CallGraphAnalysis(Objects.requireNonNull(getModule()));
        cga.setId(getModule().getId());
        cga.setTrace(getTrace());

        cga.schedule();
        cga.waitForCompletion();
        return cga;
    }

    /**
     * Test a callgraph with a callstack that provides CPU times
     *
     * @throws TmfAnalysisException
     *             Propagates exceptions
     */
    @Test
    public void testCallGraphWithCpuTime() throws TmfAnalysisException {
        IHostModel model = ModelManager.getModelFor(getTrace().getHostId());
        // Assign it to a variable because the model uses weak hash map, we
        // don't want it garbage-collected before the end of the test.
        ICpuTimeProvider cpuTimeProvider = new ICpuTimeProvider() {

            @Override
            public long getCpuTime(int tid, long start, long end) {
                // TID 7 was out of CPU from 3 to 4
                if (tid == 7) {
                    long beginTime = Math.max(start, 3);
                    long endTime = Math.min(end, 4);
                    if (endTime - beginTime > 0) {
                        return (end - start) - (endTime - beginTime);
                    }
                }
                // TID 3 was out of CPU from 8 to 11
                if (tid == 3) {
                    long beginTime = Math.max(start, 8);
                    long endTime = Math.min(end, 11);
                    if (endTime - beginTime > 0) {
                        return (end - start) - (endTime - beginTime);
                    }
                }
                // TID 2 was out of CPU from 13 to 18
                if (tid == 2) {
                    long beginTime = Math.max(start, 13);
                    long endTime = Math.min(end, 18);
                    if (endTime - beginTime > 0) {
                        return (end - start) - (endTime - beginTime);
                    }
                }
                return end - start;
            }

            @Override
            public @NonNull Collection<@NonNull String> getHostIds() {
                return Collections.singleton("callstack.xml");
            }
        };
        ((CompositeHostModel) model).setCpuTimeProvider(cpuTimeProvider);

        CallGraphAnalysis cga = getCallGraphModule();
        CallGraph callGraph = cga.getCallGraph();
        try {
            @SuppressWarnings("null")
            Collection<ICallStackElement> groups = callGraph.getElements();
            for (ICallStackElement group : groups) {
                String firstLevelName = group.getName();
                switch (firstLevelName) {
                case "1":
                    // Make sure the symbol key is correctly resolved
                    verifyProcess1CpuTime(callGraph, group);
                    break;
                case "5":
                    // Make sure the symbol key is correctly resolved
                    verifyProcess5CpuTime(callGraph, group);
                    break;
                default:
                    fail(UNKNOWN);
                }
            }
        } finally {
            cga.dispose();
        }
    }

    @SuppressWarnings("null")
    private static void verifyProcess1CpuTime(CallGraph callGraph, ICallStackElement element) {
        Collection<ICallStackElement> secondLevels = element.getChildrenElements();
        assertEquals(2, secondLevels.size());
        for (ICallStackElement secondLevel : secondLevels) {
            assertTrue(secondLevel instanceof InstrumentedCallStackElement);
            assertTrue(secondLevel.isLeaf());
            String secondLevelName = secondLevel.getName();
            Collection<AggregatedCallSite> children = callGraph.getCallingContextTree(secondLevel);
            switch (secondLevelName) {
            case "2":
                assertEquals(2, children.size());
                for (AggregatedCallSite child : children) {
                    assertTrue(child instanceof AggregatedCalledFunction);
                    AggregatedCalledFunction func = (AggregatedCalledFunction) child;
                    switch (getCallSiteSymbol(func).resolve(Collections.emptySet())) {
                    case "op1":
                        assertEquals(9, func.getDuration());
                        assertEquals(5, func.getSelfTime());
                        assertEquals(9, func.getCpuTime());
                        assertEquals(1, func.getNbCalls());
                        assertEquals(1, func.getProcessId());
                        assertEquals(1, func.getCallees().size());
                        AggregatedCalledFunction next = (AggregatedCalledFunction) func.getCallees().iterator().next();
                        assertNotNull(next);
                        assertEquals(4, next.getDuration());
                        assertEquals(3, next.getSelfTime());
                        assertEquals(4, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(1, next.getProcessId());
                        assertEquals("op2", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(1, next.getCallees().size());
                        AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                        assertNotNull(third);
                        assertEquals(1, third.getDuration());
                        assertEquals(1, third.getSelfTime());
                        assertEquals(1, third.getCpuTime());
                        assertEquals(1, third.getNbCalls());
                        assertEquals(1, third.getProcessId());
                        assertEquals("op3", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                        assertEquals(0, third.getCallees().size());
                        break;
                    case "op4":
                        assertEquals(8, func.getDuration());
                        assertEquals(8, func.getSelfTime());
                        assertEquals(3, func.getCpuTime());
                        assertEquals(1, func.getNbCalls());
                        assertEquals(1, func.getProcessId());
                        assertEquals(0, func.getCallees().size());
                        break;
                    default:
                        fail(UNKNOWN_SYMBOL + getCallSiteSymbol(func));
                    }
                }
                break;
            case "3":
                assertEquals(1, children.size());
                AggregatedCalledFunction func = (AggregatedCalledFunction) children.iterator().next();
                assertEquals("op2", getCallSiteSymbol(func).resolve(Collections.emptySet()));

                assertEquals(17, func.getDuration());
                assertEquals(10, func.getSelfTime());
                assertEquals(14, func.getCpuTime());
                assertEquals(1, func.getNbCalls());
                assertEquals(1, func.getProcessId());
                assertEquals(2, func.getCallees().size());
                for (AggregatedCallSite nextChild : func.getCallees()) {
                    assertTrue(nextChild instanceof AggregatedCalledFunction);
                    AggregatedCalledFunction next = (AggregatedCalledFunction) nextChild;
                    switch (getCallSiteSymbol(next).resolve(Collections.emptySet())) {
                    case "op3":
                        assertEquals(1, next.getDuration());
                        assertEquals(1, next.getSelfTime());
                        assertEquals(1, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(1, next.getProcessId());
                        assertEquals(0, next.getCallees().size());
                        break;
                    case "op2":
                        assertEquals(6, next.getDuration());
                        assertEquals(6, next.getSelfTime());
                        assertEquals(3, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(1, next.getProcessId());
                        assertEquals(0, next.getCallees().size());
                        break;
                    default:
                        fail("Unknown symbol for thread 3: " + getCallSiteSymbol(func));
                    }
                }

                break;
            default:
                fail(UNKNOWN_PROCESS + secondLevelName);
            }
        }
    }

    @SuppressWarnings("null")
    private static void verifyProcess5CpuTime(CallGraph callGraph, ICallStackElement element) {
        Collection<ICallStackElement> secondLevels = element.getChildrenElements();
        assertEquals(2, secondLevels.size());
        for (ICallStackElement secondLevel : secondLevels) {
            assertTrue(secondLevel instanceof InstrumentedCallStackElement);
            assertTrue(secondLevel.isLeaf());
            String secondLevelName = secondLevel.getName();
            Collection<AggregatedCallSite> children = callGraph.getCallingContextTree(secondLevel);
            switch (secondLevelName) {
            case "6": {
                assertEquals(1, children.size());
                AggregatedCalledFunction func = (AggregatedCalledFunction) children.iterator().next();
                assertNotNull(func);
                assertEquals("op1", getCallSiteSymbol(func).resolve(Collections.emptySet()));
                assertEquals(19, func.getDuration());
                assertEquals(3, func.getSelfTime());
                assertEquals(19, func.getCpuTime());
                assertEquals(1, func.getNbCalls());
                assertEquals(5, func.getProcessId());
                assertEquals(3, func.getCallees().size());

                for (AggregatedCallSite nextChild : func.getCallees()) {
                    assertTrue(nextChild instanceof AggregatedCalledFunction);
                    AggregatedCalledFunction next = (AggregatedCalledFunction) nextChild;
                    switch (getCallSiteSymbol(next).resolve(Collections.emptySet())) {
                    case "op2": {
                        assertEquals(3, next.getDuration());
                        assertEquals(2, next.getSelfTime());
                        assertEquals(3, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(5, next.getProcessId());
                        assertEquals("op2", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(1, next.getCallees().size());
                        AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                        assertNotNull(third);
                        assertEquals(1, third.getDuration());
                        assertEquals(1, third.getSelfTime());
                        assertEquals(1, third.getCpuTime());
                        assertEquals(1, third.getNbCalls());
                        assertEquals(5, third.getProcessId());
                        assertEquals("op3", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                        assertEquals(0, third.getCallees().size());
                    }
                        break;
                    case "op3": {
                        assertEquals(5, next.getDuration());
                        assertEquals(3, next.getSelfTime());
                        assertEquals(5, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(5, next.getProcessId());
                        assertEquals("op3", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(1, next.getCallees().size());
                        AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                        assertNotNull(third);
                        assertEquals(2, third.getDuration());
                        assertEquals(2, third.getSelfTime());
                        assertEquals(2, third.getCpuTime());
                        assertEquals(1, third.getNbCalls());
                        assertEquals(5, third.getProcessId());
                        assertEquals("op1", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                        assertEquals(0, third.getCallees().size());
                    }
                        break;
                    case "op4":
                        assertEquals(8, next.getDuration());
                        assertEquals(8, next.getSelfTime());
                        assertEquals(8, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(5, next.getProcessId());
                        assertEquals("op4", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(0, next.getCallees().size());
                        break;
                    default:
                        fail("Unknown symbol for second level of tid 6: " + getCallSiteSymbol(next));
                    }
                }
            }
                break;
            case "7": {
                /*
                 * pid1 --- tid2 1e1 ------------- 10x1 12e4------------20x |
                 * 3e2-------7x | 4e3--5x |-- tid3 3e2
                 * --------------------------------20x 5e3--6x 7e2--------13x
                 *
                 * pid5 --- tid6 1e1 -----------------------------------20x |
                 * 2e3 ---------7x 12e4------------20x | 4e1--6x |-- tid7 1e5
                 * -----------------------------------20x 2e2 +++ 6x 9e2 ++++
                 * 13x 15e2 ++ 19x 10e3 + 11x
                 */
                assertEquals(1, children.size());
                AggregatedCalledFunction func = (AggregatedCalledFunction) children.iterator().next();
                assertNotNull(func);
                assertEquals("op5", getCallSiteSymbol(func).resolve(Collections.emptySet()));
                assertEquals(19, func.getDuration());
                assertEquals(7, func.getSelfTime());
                assertEquals(18, func.getCpuTime());
                assertEquals(1, func.getNbCalls());
                assertEquals(5, func.getProcessId());
                assertEquals(1, func.getCallees().size());

                // Verify children
                Iterator<AggregatedCallSite> iterator = func.getCallees().iterator();
                AggregatedCalledFunction next = (AggregatedCalledFunction) iterator.next();
                assertNotNull(next);
                assertEquals(12, next.getDuration());
                assertEquals(11, next.getSelfTime());
                assertEquals(11, next.getCpuTime());
                assertEquals(3, next.getNbCalls());
                assertEquals(5, next.getProcessId());
                assertEquals("op2", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                assertEquals(1, next.getCallees().size());
                AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                assertNotNull(third);
                assertEquals(1, third.getDuration());
                assertEquals(1, third.getSelfTime());
                assertEquals(1, third.getCpuTime());
                assertEquals(1, third.getNbCalls());
                assertEquals(5, third.getProcessId());
                assertEquals("op3", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                assertEquals(0, third.getCallees().size());
            }
                break;
            default:
                fail(UNKNOWN_PROCESS + secondLevelName);
            }
        }
    }

    /**
     * Test the callgraph for a time selection, with a small trace
     */
    @Test
    public void testSelectionCallGraph() {
        CallStackAnalysisStub cga = getModule();
        CallGraph cg = cga.getCallGraph(TmfTimestamp.fromNanos(1), TmfTimestamp.fromNanos(10));
        try {
            @SuppressWarnings("null")
            Collection<ICallStackElement> elements = cg.getElements();
            for (ICallStackElement group : elements) {
                String firstLevelName = group.getName();
                switch (firstLevelName) {
                case "1":
                    verifyProcess1Selection(cg, group);
                    break;
                case "5":
                    verifyProcess5Selection(cg, group);
                    break;
                default:
                    fail(UNKNOWN);
                }
            }

            // Test that another call to the callgraph returns the exact same
            // object
            CallGraph cg2 = cga.getCallGraph(TmfTimestamp.fromNanos(1), TmfTimestamp.fromNanos(10));
            assertTrue(cg == cg2);
        } finally {
            cga.dispose();
        }
    }

    @SuppressWarnings("null")
    private static void verifyProcess1Selection(CallGraph cg, ICallStackElement element) {
        Collection<ICallStackElement> secondLevels = element.getChildrenElements();
        assertEquals(2, secondLevels.size());
        for (ICallStackElement secondLevel : secondLevels) {
            assertTrue(secondLevel instanceof InstrumentedCallStackElement);
            assertTrue(secondLevel.isLeaf());
            String secondLevelName = secondLevel.getName();
            Collection<AggregatedCallSite> children = cg.getCallingContextTree(secondLevel);
            switch (secondLevelName) {
            case "2":
                assertEquals(1, children.size());
                for (AggregatedCallSite child : children) {
                    assertTrue(child instanceof AggregatedCalledFunction);
                    AggregatedCalledFunction func = (AggregatedCalledFunction) child;
                    switch (getCallSiteSymbol(func).resolve(Collections.emptySet())) {
                    case "op1":
                        assertEquals(9, func.getDuration());
                        assertEquals(5, func.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, func.getCpuTime());
                        assertEquals(1, func.getNbCalls());
                        assertEquals(1, func.getProcessId());
                        assertEquals(1, func.getCallees().size());
                        AggregatedCalledFunction next = (AggregatedCalledFunction) func.getCallees().iterator().next();
                        assertNotNull(next);
                        assertEquals(4, next.getDuration());
                        assertEquals(3, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(1, next.getProcessId());
                        assertEquals("op2", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(1, next.getCallees().size());
                        AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                        assertNotNull(third);
                        assertEquals(1, third.getDuration());
                        assertEquals(1, third.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, third.getCpuTime());
                        assertEquals(1, third.getNbCalls());
                        assertEquals(1, third.getProcessId());
                        assertEquals("op3", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                        assertEquals(0, third.getCallees().size());
                        break;
                    default:
                        fail(UNKNOWN_SYMBOL + getCallSiteSymbol(func));
                    }
                }
                break;
            case "3":
                assertEquals(1, children.size());
                AggregatedCalledFunction func = (AggregatedCalledFunction) children.iterator().next();
                assertEquals("op2", getCallSiteSymbol(func).resolve(Collections.emptySet()));

                assertEquals(7, func.getDuration());
                assertEquals(3, func.getSelfTime());
                assertEquals(IHostModel.TIME_UNKNOWN, func.getCpuTime());
                assertEquals(1, func.getNbCalls());
                assertEquals(1, func.getProcessId());
                assertEquals(2, func.getCallees().size());
                for (AggregatedCallSite nextChild : func.getCallees()) {
                    assertTrue(nextChild instanceof AggregatedCalledFunction);
                    AggregatedCalledFunction next = (AggregatedCalledFunction) nextChild;
                    switch (getCallSiteSymbol(next).resolve(Collections.emptySet())) {
                    case "op3":
                        assertEquals(1, next.getDuration());
                        assertEquals(1, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(1, next.getProcessId());
                        assertEquals(0, next.getCallees().size());
                        break;
                    case "op2":
                        assertEquals(3, next.getDuration());
                        assertEquals(3, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(1, next.getProcessId());
                        assertEquals(0, next.getCallees().size());
                        break;
                    default:
                        fail(UNKNOWN_SYMBOL + getCallSiteSymbol(func));
                    }
                }

                break;
            default:
                fail(UNKNOWN_PROCESS + secondLevelName);
            }
        }
    }

    @SuppressWarnings("null")
    private static void verifyProcess5Selection(CallGraph cg, ICallStackElement element) {
        Collection<ICallStackElement> secondLevels = element.getChildrenElements();
        assertEquals(2, secondLevels.size());
        for (ICallStackElement secondLevel : secondLevels) {
            assertTrue(secondLevel instanceof InstrumentedCallStackElement);
            assertTrue(secondLevel.isLeaf());
            String secondLevelName = secondLevel.getName();
            Collection<AggregatedCallSite> children = cg.getCallingContextTree(secondLevel);
            switch (secondLevelName) {
            case "6": {
                assertEquals(1, children.size());
                AggregatedCalledFunction func = (AggregatedCalledFunction) children.iterator().next();
                assertNotNull(func);
                assertEquals("op1", getCallSiteSymbol(func).resolve(Collections.emptySet()));
                assertEquals(9, func.getDuration());
                assertEquals(2, func.getSelfTime());
                assertEquals(IHostModel.TIME_UNKNOWN, func.getCpuTime());
                assertEquals(1, func.getNbCalls());
                assertEquals(5, func.getProcessId());
                assertEquals(2, func.getCallees().size());

                for (AggregatedCallSite nextChild : func.getCallees()) {
                    assertTrue(nextChild instanceof AggregatedCalledFunction);
                    AggregatedCalledFunction next = (AggregatedCalledFunction) nextChild;
                    switch (getCallSiteSymbol(next).resolve(Collections.emptySet())) {
                    case "op2": {
                        assertEquals(2, next.getDuration());
                        assertEquals(1, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(5, next.getProcessId());
                        assertEquals("op2", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(1, next.getCallees().size());
                        AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                        assertNotNull(third);
                        assertEquals(1, third.getDuration());
                        assertEquals(1, third.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, third.getCpuTime());
                        assertEquals(1, third.getNbCalls());
                        assertEquals(5, third.getProcessId());
                        assertEquals("op3", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                        assertEquals(0, third.getCallees().size());
                    }
                        break;
                    case "op3": {
                        assertEquals(5, next.getDuration());
                        assertEquals(3, next.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                        assertEquals(1, next.getNbCalls());
                        assertEquals(5, next.getProcessId());
                        assertEquals("op3", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                        assertEquals(1, next.getCallees().size());
                        AggregatedCalledFunction third = (AggregatedCalledFunction) next.getCallees().iterator().next();
                        assertNotNull(third);
                        assertEquals(2, third.getDuration());
                        assertEquals(2, third.getSelfTime());
                        assertEquals(IHostModel.TIME_UNKNOWN, third.getCpuTime());
                        assertEquals(1, third.getNbCalls());
                        assertEquals(5, third.getProcessId());
                        assertEquals("op1", getCallSiteSymbol(third).resolve(Collections.emptySet()));
                        assertEquals(0, third.getCallees().size());
                    }
                        break;
                    default:
                        fail("Unknown symbol for second level of tid 6");
                    }
                }
            }
                break;
            case "7": {
                assertEquals(1, children.size());
                AggregatedCalledFunction func = (AggregatedCalledFunction) children.iterator().next();
                assertNotNull(func);
                assertEquals("op5", getCallSiteSymbol(func).resolve(Collections.emptySet()));
                assertEquals(9, func.getDuration());
                assertEquals(4, func.getSelfTime());
                assertEquals(IHostModel.TIME_UNKNOWN, func.getCpuTime());
                assertEquals(1, func.getNbCalls());
                assertEquals(5, func.getProcessId());
                assertEquals(1, func.getCallees().size());

                // Verify children
                Iterator<AggregatedCallSite> iterator = func.getCallees().iterator();
                AggregatedCalledFunction next = (AggregatedCalledFunction) iterator.next();
                assertNotNull(next);
                assertEquals(5, next.getDuration());
                assertEquals(5, next.getSelfTime());
                assertEquals(IHostModel.TIME_UNKNOWN, next.getCpuTime());
                assertEquals(2, next.getNbCalls());
                assertEquals(5, next.getProcessId());
                assertEquals("op2", getCallSiteSymbol(next).resolve(Collections.emptySet()));
                assertEquals(0, next.getCallees().size());
            }
                break;
            default:
                fail(UNKNOWN_PROCESS + secondLevelName);
            }
        }
    }
}
