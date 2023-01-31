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

package org.eclipse.tracecompass.analysis.callstack.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.callstack.core.tests.stubs.CallStackAnalysisStub;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStack;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackSeries;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.CalledFunctionFactory;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.InstrumentedCallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.IHostModel;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.ModelManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.Test;

/**
 * Test the callstack data structure and traversal
 *
 * @author Geneviève Bastien
 */
public class CallStackTest extends CallStackTestBase {

    private static final @NonNull IProgressMonitor MONITOR = new NullProgressMonitor();
    private static final long START_TIME = 1L;
    private static final long END_TIME = 20L;

    /**
     * Test the callstack data using the callstack object
     */
    @Test
    public void testCallStackTraversal() {
        CallStackAnalysisStub module = getModule();
        assertNotNull(module);

        CallStackSeries callstack = module.getCallStackSeries();
        assertNotNull(callstack);

        @SuppressWarnings("null")
        Collection<ICallStackElement> processes = callstack.getRootElements();
        assertEquals(2, processes.size());

        for (ICallStackElement element : processes) {
            assertNull(element.getParentElement());
            // Make sure the element does not return any call list
            switch (element.getName()) {
            case "1":
                // Make sure the symbol key is correctly resolved
                assertEquals(1, element.getSymbolKeyAt(START_TIME));
                assertEquals(1, element.getSymbolKeyAt(END_TIME));
                verifyProcess1(element);
                break;
            case "5":
                // Make sure the symbol key is correctly resolved
                assertEquals(5, element.getSymbolKeyAt(START_TIME));
                assertEquals(5, element.getSymbolKeyAt(END_TIME));
                verifyProcess5(element);
                break;
            default:
                fail("Unknown process in callstack");
            }
        }
    }

    @SuppressWarnings("null")
    private void verifyProcess1(ICallStackElement element) {
        Collection<ICallStackElement> children = element.getChildrenElements();
        IHostModel model = ModelManager.getModelFor("");
        ITmfTrace trace = getTrace();
        assertNotNull(trace);
        for (ICallStackElement thread : children) {
            assertEquals(element, thread.getParentElement());
            assertTrue(thread instanceof InstrumentedCallStackElement);
            assertTrue(thread.isLeaf());
            assertNull(thread.getNextGroup());
            CallStack callStack = ((InstrumentedCallStackElement) thread).getCallStack();
            // Make sure the element does not return any call list
            switch (thread.getName()) {
            case "2": {
                // Make sure the symbol key is correctly resolved
                assertEquals(1, thread.getSymbolKeyAt(START_TIME));
                assertEquals(1, thread.getSymbolKeyAt(END_TIME));

                assertEquals(1, callStack.getSymbolKeyAt(START_TIME));
                assertEquals(1, callStack.getSymbolKeyAt(END_TIME));

                /* Check the first level */
                List<ICalledFunction> callList = callStack.getCallListAtDepth(1, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(2, callList.size());
                assertEquals(CalledFunctionFactory.create(1L, 10L, "op1", 1, 2, null, model), callList.get(0));
                assertEquals(CalledFunctionFactory.create(12L, 20L, "op4", 1, 2, null, model), callList.get(1));

                /* Check the second level */
                callList = callStack.getCallListAtDepth(2, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(3L, 7L, "op2", 1, 2, null, model), callList.get(0));

                /* Check the third level */
                callList = callStack.getCallListAtDepth(3, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(4L, 5L, "op3", 1, 2, null, model), callList.get(0));

                /* Check the host thread */
                assertEquals(new HostThread(trace.getHostId(), 2), callStack.getHostThread(1L));
                assertEquals(new HostThread(trace.getHostId(), 2), callStack.getHostThread(20L));
            }
                break;
            case "3": {
                // Make sure the symbol key is correctly resolved
                assertEquals(1, element.getSymbolKeyAt(START_TIME));
                assertEquals(1, element.getSymbolKeyAt(END_TIME));

                assertEquals(1, callStack.getSymbolKeyAt(START_TIME));
                assertEquals(1, callStack.getSymbolKeyAt(END_TIME));

                /* Check the first level */
                List<ICalledFunction> callList = callStack.getCallListAtDepth(1, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(3L, 20L, "op2", 1, 3, null, model), callList.get(0));

                /* Check the second level */
                callList = callStack.getCallListAtDepth(2, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(2, callList.size());
                assertEquals(CalledFunctionFactory.create(5L, 6L, "op3", 1, 3, null, model), callList.get(0));
                assertEquals(CalledFunctionFactory.create(7L, 13L, "op2", 1, 3, null, model), callList.get(1));

                /* Check the host thread */
                assertEquals(new HostThread(trace.getHostId(), 3), callStack.getHostThread(1L));
                assertEquals(new HostThread(trace.getHostId(), 3), callStack.getHostThread(20L));
            }
                break;
            default:
                fail("Unknown thread child of process 5");
            }
        }
    }

    @SuppressWarnings("null")
    private void verifyProcess5(ICallStackElement element) {
        Collection<ICallStackElement> children = element.getChildrenElements();
        IHostModel model = ModelManager.getModelFor("");
        ITmfTrace trace = getTrace();
        assertNotNull(trace);
        for (ICallStackElement thread : children) {
            assertTrue(thread instanceof InstrumentedCallStackElement);
            assertTrue(thread.isLeaf());
            assertNull(thread.getNextGroup());
            CallStack callStack = ((InstrumentedCallStackElement) thread).getCallStack();
            // Make sure the element does not return any call list
            switch (thread.getName()) {
            case "6": {
                // Make sure the symbol key is correctly resolved
                assertEquals(5, thread.getSymbolKeyAt(START_TIME));
                assertEquals(5, thread.getSymbolKeyAt(END_TIME));

                assertEquals(5, callStack.getSymbolKeyAt(START_TIME));
                assertEquals(5, callStack.getSymbolKeyAt(END_TIME));

                /* Check the first level */
                List<ICalledFunction> callList = callStack.getCallListAtDepth(1, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(1L, 20L, "op1", 1, 6, null, model), callList.get(0));

                /* Check the second level */
                callList = callStack.getCallListAtDepth(2, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(3, callList.size());
                assertEquals(CalledFunctionFactory.create(2L, 7L, "op3", 1, 6, null, model), callList.get(0));
                assertEquals(CalledFunctionFactory.create(8L, 11L, "op2", 1, 6, null, model), callList.get(1));
                assertEquals(CalledFunctionFactory.create(12L, 20L, "op4", 1, 6, null, model), callList.get(2));

                /* Check the third level */
                callList = callStack.getCallListAtDepth(3, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(2, callList.size());
                assertEquals(CalledFunctionFactory.create(4L, 6L, "op1", 1, 6, null, model), callList.get(0));
                assertEquals(CalledFunctionFactory.create(9L, 10L, "op3", 1, 6, null, model), callList.get(1));

                /* Check the host thread */
                assertEquals(new HostThread(trace.getHostId(), 6), callStack.getHostThread(1L));
                assertEquals(new HostThread(trace.getHostId(), 6), callStack.getHostThread(20L));
            }
                break;
            case "7": {
                // Make sure the symbol key is correctly resolved
                assertEquals(5, thread.getSymbolKeyAt(START_TIME));
                assertEquals(5, thread.getSymbolKeyAt(END_TIME));

                assertEquals(5, callStack.getSymbolKeyAt(START_TIME));
                assertEquals(5, callStack.getSymbolKeyAt(END_TIME));

                /* Check the first level */
                List<ICalledFunction> callList = callStack.getCallListAtDepth(1, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(1L, 20L, "op5", 1, 6, null, model), callList.get(0));

                /* Check the second level */
                callList = callStack.getCallListAtDepth(2, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(3, callList.size());
                assertEquals(CalledFunctionFactory.create(2L, 6L, "op2", 1, 6, null, model), callList.get(0));
                assertEquals(CalledFunctionFactory.create(9L, 13L, "op2", 1, 6, null, model), callList.get(1));
                assertEquals(CalledFunctionFactory.create(15L, 19L, "op2", 1, 6, null, model), callList.get(2));

                /* Check the third level */
                callList = callStack.getCallListAtDepth(3, START_TIME, END_TIME, 1, MONITOR);
                assertEquals(1, callList.size());
                assertEquals(CalledFunctionFactory.create(10L, 11L, "op3", 1, 6, null, model), callList.get(0));

                /* Check the host thread */
                assertEquals(new HostThread(trace.getHostId(), 7), callStack.getHostThread(1L));
                assertEquals(new HostThread(trace.getHostId(), 7), callStack.getHostThread(20L));
            }
                break;
            default:
                fail("Unknown thread child of process 5");
            }
        }
    }

    @SuppressWarnings("null")
    private CallStack getElementToTest() {
        // Return the second callstack level of process 5 / thread 7
        CallStackAnalysisStub module = getModule();
        assertNotNull(module);

        CallStackSeries callstack = module.getCallStackSeries();
        assertNotNull(callstack);

        Collection<ICallStackElement> processes = callstack.getRootElements();
        assertEquals(2, processes.size());

        Iterator<ICallStackElement> processIt = processes.iterator();
        processIt.next();
        ICallStackElement process = processIt.next();
        assertEquals("5", process.getName());
        Collection<ICallStackElement> threads = process.getChildrenElements();
        assertEquals(2, threads.size());

        Iterator<ICallStackElement> iterator = threads.iterator();
        iterator.next();
        ICallStackElement thread = iterator.next();
        assertEquals("7", thread.getName());
        assertTrue(thread instanceof InstrumentedCallStackElement);
        assertTrue(thread.isLeaf());
        return ((InstrumentedCallStackElement) thread).getCallStack();
    }

    /**
     * Test getting the function calls with different ranges and resolutions
     */
    @SuppressWarnings("null")
    @Test
    public void testCallStackRanges() {
        CallStack element = getElementToTest();

        /**
         * <pre>
         * Function calls for this element:
         * (2, 6), (9, 13), (15, 19)
         * </pre>
         */

        /* Following test with a resolution of 1 */
        int resolution = 1;
        // Test a range before the first element
        List<ICalledFunction> callList = element.getCallListAtDepth(2, START_TIME, START_TIME, resolution, MONITOR);
        assertEquals(0, callList.size());

        // Test a range including the start of a function call
        callList = element.getCallListAtDepth(2, START_TIME, 2L, resolution, MONITOR);
        assertEquals(1, callList.size());

        // Test a range in the middle of one function call
        callList = element.getCallListAtDepth(2, START_TIME, 4L, resolution, MONITOR);
        assertEquals(1, callList.size());

        // Test a range including not fully including 2 function calls
        callList = element.getCallListAtDepth(2, 4L, 10L, resolution, MONITOR);
        assertEquals(2, callList.size());

        // Test a range outside the trace range
        callList = element.getCallListAtDepth(2, END_TIME + 1, END_TIME + 3, resolution, MONITOR);
        assertEquals(0, callList.size());

        // Test the full range of the trace
        callList = element.getCallListAtDepth(2, START_TIME, END_TIME, resolution, MONITOR);
        assertEquals(3, callList.size());

        // Test a range after the first call with a resolution that should skip
        // one call
    }

    /**
     * Test getting the {@link CallStack#getNextFunction(long, int)} method
     */
    @Test
    public void testCallStackNext() {
        CallStack element = getElementToTest();
        IHostModel model = ModelManager.getModelFor("");

        /**
         * <pre>
         * Function calls for this element:
         * (2, 6), (9, 13), (15, 19)
         * </pre>
         */

        ICalledFunction function = element.getNextFunction(START_TIME, 2);
        assertNotNull(function);
        assertEquals(CalledFunctionFactory.create(2L, 6L, "op2", 1, 6, null, model), function);

        function = element.getNextFunction(function.getEnd(), 2);
        assertNotNull(function);
        assertEquals(CalledFunctionFactory.create(9L, 13L, "op2", 1, 6, null, model), function);

        function = element.getNextFunction(function.getEnd(), 2);
        assertNotNull(function);
        assertEquals(CalledFunctionFactory.create(15L, 19L, "op2", 1, 6, null, model), function);

        function = element.getNextFunction(function.getEnd(), 2);
        assertNull(function);
    }
}
