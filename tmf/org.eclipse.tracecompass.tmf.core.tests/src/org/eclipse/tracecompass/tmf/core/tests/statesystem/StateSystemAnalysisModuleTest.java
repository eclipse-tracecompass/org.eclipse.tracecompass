/*******************************************************************************
 * Copyright (c) 2013, 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.tests.statesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateIntervalStub;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateSystemTestUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider.FutureEventType;
import org.eclipse.tracecompass.tmf.core.statesystem.Messages;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.tests.TmfCoreTestPlugin;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.analysis.TestStateSystemModule;
import org.eclipse.tracecompass.tmf.tests.stubs.analysis.TestStateSystemProvider;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import com.google.common.collect.ImmutableList;

/**
 * Test the {@link TmfStateSystemAnalysisModule} class
 *
 * @author Geneviève Bastien
 */
public class StateSystemAnalysisModuleTest {

    private final class BreakingTest extends TestStateSystemProvider {

        private final int fEventNo;
        private int fEventCount = 0;
        private String fExceptionMessage;

        private BreakingTest(@NonNull ITmfTrace trace, int eventToFail, String msg) {
            super(trace, 1, 2);
            fEventNo = eventToFail;
            fExceptionMessage = msg;
        }

        @Override
        protected void eventHandle(@NonNull ITmfEvent event) {
            fEventCount++;
            if (fEventCount == fEventNo) {
                throw new IllegalArgumentException(fExceptionMessage);
            }
        }
    }

    /** Time-out tests after 1 minute. */
    @Rule
    public TestRule globalTimeout = new Timeout(1, TimeUnit.MINUTES);

    /** ID of the test state system analysis module */
    public static final String MODULE_SS = "org.eclipse.linuxtools.tmf.core.tests.analysis.sstest";
    private static final String XML_TRACE = "testfiles/stub_xml_traces/valid/analysis_dependency.xml";

    private TestStateSystemModule fModule;
    private ITmfTrace fTrace;

    /**
     * Setup test trace
     */
    @Before
    public void setupTraces() {
        TmfXmlTraceStub trace = TmfXmlTraceStubNs.setupTrace(TmfCoreTestPlugin.getAbsoluteFilePath(XML_TRACE));
        trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        fTrace = trace;

        fModule = (TestStateSystemModule) trace.getAnalysisModule(MODULE_SS);
    }

    /**
     * Some tests use traces, let's clean them here
     */
    @After
    public void cleanupTraces() {
        fTrace.dispose();
    }

    /**
     * Test the state system module execution and result
     */
    @Test
    public void testSsModule() {
        ITmfStateSystem ss = fModule.getStateSystem();
        assertNull(ss);
        fModule.schedule();
        if (fModule.waitForCompletion()) {
            ss = fModule.getStateSystem();
            assertNotNull(ss);
        } else {
            fail("Module did not complete properly");
        }
    }

    /**
     * Make sure that the state system is initialized after calling
     * {@link TmfStateSystemAnalysisModule#waitForInitialization()}.
     */
    @Test
    public void testInitialization() {
        assertNull(fModule.getStateSystem());
        fModule.schedule();

        assertTrue("Initialization succeeded", fModule.waitForInitialization());
        assertNotNull(fModule.getStateSystem());
    }

    /**
     * Test that helper returns the right properties
     */
    @Test
    public void testProperties() {

        /* The stub state system has in mem backend 2 properties */
        Map<String, String> properties = fModule.getProperties();
        assertEquals(fModule.getBackendName(), properties.get(Messages.TmfStateSystemAnalysisModule_PropertiesBackend));
        assertEquals(fModule.getId(), properties.get(org.eclipse.tracecompass.tmf.core.analysis.Messages.TmfAbstractAnalysisModule_LabelId));
    }

    private static final String CRUCIAL_EVENT = "crucialEvent";
    private static final String CRUCIAL_FIELD = "crucialInfo";

    private static void setupDependentAnalysisHandler(CyclicBarrier barrier) {
        TestStateSystemProvider.setEventHandler((ss, provider, event) -> {
            try {
                /* Wait before processing the current event */
                barrier.await();
                if (event.getName().equals(CRUCIAL_EVENT)) {
                    String crucialInfo = (String) event.getContent().getField(CRUCIAL_FIELD).getValue();
                    int quark = ss.getQuarkAbsoluteAndAdd(CRUCIAL_FIELD);
                    try {
                        ss.modifyAttribute(event.getTimestamp().toNanos(), crucialInfo, quark);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                }
                /* Wait before processing the next event */
                barrier.await();
                return true;
            } catch (InterruptedException | BrokenBarrierException e1) {
                return false;
            }

        });
    }

    /**
     * Test the {@link TmfStateSystemAnalysisModule#isQueryable(long)} method
     */
    @Test
    public void testIsQueryable() {

        CyclicBarrier barrier = new CyclicBarrier(2);
        setupDependentAnalysisHandler(barrier);

        TestStateSystemModule module = fModule;
        assertNotNull(module);

        /* Module is not started, it should be queriable */
        assertTrue(module.isQueryable(1));
        assertTrue(module.isQueryable(4));
        assertTrue(module.isQueryable(5));
        assertTrue(module.isQueryable(7));
        assertTrue(module.isQueryable(10));

        module.schedule();

        assertTrue(module.waitForInitialization());

        assertFalse(module.isQueryable(1));

        try {
            /* 2 waits for a barrier for one event */
            // event 1
            barrier.await();
            barrier.await();
            // event 2
            barrier.await();
            assertTrue(module.isQueryable(1));
            assertTrue(module.isQueryable(4));
            assertFalse(module.isQueryable(5));
            barrier.await();
            // event 3
            barrier.await();
            assertTrue(module.isQueryable(1));
            assertTrue(module.isQueryable(4));
            assertFalse(module.isQueryable(5));
            barrier.await();
            // event 4
            barrier.await();
            assertTrue(module.isQueryable(1));
            assertTrue(module.isQueryable(4));
            assertFalse(module.isQueryable(5));
            barrier.await();
            // event 5
            barrier.await();
            assertTrue(module.isQueryable(1));
            assertTrue(module.isQueryable(4));
            assertTrue(module.isQueryable(5));
            assertFalse(module.isQueryable(7));
            barrier.await();
            // event 6
            barrier.await();
            assertTrue(module.isQueryable(1));
            assertTrue(module.isQueryable(4));
            assertTrue(module.isQueryable(5));
            assertFalse(module.isQueryable(7));
            barrier.await();
            // event 7
            barrier.await();
            assertTrue(module.isQueryable(1));
            assertTrue(module.isQueryable(4));
            assertTrue(module.isQueryable(5));
            assertTrue(module.isQueryable(7));
            assertFalse(module.isQueryable(10));
            barrier.await();

            fModule.waitForCompletion();
            assertTrue(module.isQueryable(1));
            assertTrue(module.isQueryable(4));
            assertTrue(module.isQueryable(5));
            assertTrue(module.isQueryable(7));
            assertTrue(module.isQueryable(10));

            // Should return true only if later than trace time
            assertTrue(module.isQueryable(100));

        } catch (InterruptedException | BrokenBarrierException e1) {
            fail(e1.getMessage());
            fModule.cancel();
        } finally {
            TestStateSystemProvider.setEventHandler(null);
        }
    }

    /**
     * Test the {@link TmfStateSystemAnalysisModule#isQueryable(long)} method
     * when the analysis is cancelled
     */
    @Ignore("Hangs very often")
    @Test
    public void testIsQueryableCancel() {

        TestStateSystemModule module = fModule;
        assertNotNull(module);
        /* Set the queue to 1 to limit the number of events buffered */
        module.setPerEventSignalling(true);

        /* Module is not started, it should be queriable */
        assertTrue(module.isQueryable(1));
        assertTrue(module.isQueryable(4));
        assertTrue(module.isQueryable(5));
        assertTrue(module.isQueryable(7));
        assertTrue(module.isQueryable(10));

        fModule.schedule();

        assertTrue(module.waitForInitialization());

        assertFalse(module.isQueryable(1));

        // Process 2 events, then cancel
        module.signalNextEvent();
        module.signalNextEvent();
        module.cancel();
        module.setPerEventSignalling(false);

        fModule.waitForCompletion();
        assertTrue(module.isQueryable(1));
        assertTrue(module.isQueryable(4));
        assertTrue(module.isQueryable(5));
        assertTrue(module.isQueryable(7));
        assertTrue(module.isQueryable(10));
    }

    /**
     * Test that an analysis with full backend is re-read correctly
     *
     * @throws TmfAnalysisException
     *             Propagates exceptions
     */
    @Test
    public void testReReadFullAnalysis() throws TmfAnalysisException {
        TestStateSystemModule module = new TestStateSystemModule(true);
        TestStateSystemModule module2 = new TestStateSystemModule(true);
        try {
            ITmfTrace trace = fTrace;
            assertNotNull(trace);
            module.setTrace(trace);
            module2.setTrace(trace);

            // Execute the first module
            module.schedule();
            assertTrue(module.waitForCompletion());

            // Execute the second module, it should read the state system file
            File ssFile = module2.getSsFile();
            assertNotNull(ssFile);
            assertTrue(ssFile.exists());
            module2.schedule();
            assertTrue(module2.waitForCompletion());
        } finally {
            module.dispose();
            module2.dispose();
        }
    }

    /**
     * Test that an analysis whose event request throws an exception is failed
     * correctly
     *
     * @throws TmfAnalysisException
     *             Propagates exceptions
     */
    @Test
    public void testRequestFailure() throws TmfAnalysisException {
        TestStateSystemModule module = new TestStateSystemModule();
        module.setRequestAction(e -> {
            throw new IllegalArgumentException("This exception is desired and part of the test");
        });
        try {
            ITmfTrace trace = fTrace;
            assertNotNull(trace);
            module.setTrace(trace);

            // Execute the module that should throw the exception
            module.schedule();
            assertFalse(module.waitForCompletion());

        } finally {
            module.dispose();
        }
    }

    /**
     * Test the behavior of a state provider that causes a runtime exception at
     * different moments of the analysis. The analyses should be marked as
     * failed
     *
     * @throws TmfAnalysisException
     *             An exception when setting the trace
     */
    @Test
    public void testFaultyStateProvider() throws TmfAnalysisException {
        ITmfTrace trace = fTrace;
        assertNotNull(trace);

        // Test failure on the last event
        TmfStateSystemAnalysisModule module = new TestStateSystemModule() {

            @Override
            protected @NonNull ITmfStateProvider createStateProvider() {
                return new BreakingTest(trace, 7, "Expected exception: should be caught by the analysis itself");
            }

        };
        try {
            module.setTrace(trace);
            module.schedule();
            assertFalse(module.waitForCompletion());
        } finally {
            module.dispose();
        }

        // Test failure when the analysis the request finishes before the queue
        // is full
        module = new TestStateSystemModule() {

            @Override
            protected @NonNull ITmfStateProvider createStateProvider() {
                return new BreakingTest(trace, 5, "Expected exception: should be caught by either the analysis or the event request");
            }

        };
        try {
            module.setTrace(trace);
            module.schedule();
            assertFalse(module.waitForCompletion());
        } finally {
            module.dispose();
        }

        // Test failure when the queue should be full
        module = new TestStateSystemModule() {

            @Override
            protected @NonNull ITmfStateProvider createStateProvider() {
                return new BreakingTest(trace, 1, "Expected exception: should be caught by the event request thread");
            }

        };
        try {
            module.setTrace(trace);
            module.schedule();
            assertFalse(module.waitForCompletion());
        } finally {
            module.dispose();
        }
    }

    /**
     * Test a module that causes an exception before the module is initialized
     *
     * @throws TmfAnalysisException
     *             An exception when setting the trace
     */
    @Test
    public void testFailBeforeInitialization() throws TmfAnalysisException {
        ITmfTrace trace = fTrace;
        assertNotNull(trace);

        /* This module will throw an exception before it is initialized */
        TmfStateSystemAnalysisModule module = new TestStateSystemModule() {

            @Override
            protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
                throw new IllegalStateException("This exception happens before initialization");
            }

        };
        try {
            module.setTrace(trace);
            module.schedule();
            assertFalse(module.waitForInitialization());
            assertFalse(module.waitForCompletion());
        } finally {
            module.dispose();
        }
    }

    /**
     * Test adding future values to the state system
     */
    @Test
    public void testFutureEvents() {
        String futureVal = "futureVal";
        String futureStack = "futureStack";
        int oneValue = 100;
        String stack1 = "stack1";
        String stack2 = "stack2";
        try {
            TestStateSystemProvider.setEventHandler((ss, provider, event) -> {
                // Add future events
                if (event.getTimestamp().toNanos() == 1) {
                    // initialize a value quark and modify it in future
                    int valueQuark = ss.getQuarkAbsoluteAndAdd(futureVal);
                    ss.modifyAttribute(event.getTimestamp().toNanos(), oneValue, valueQuark);
                    provider.addFutureEvent(5, null, valueQuark, FutureEventType.MODIFICATION);

                    // Initialize a stack and modify it in future
                    int stackQuark = ss.getQuarkAbsoluteAndAdd(futureStack);
                    ss.pushAttribute(event.getTimestamp().toNanos(), stack1, stackQuark);
                    provider.addFutureEvent(4, stack2, stackQuark, FutureEventType.PUSH);
                    provider.addFutureEvent(7, stack2, stackQuark, FutureEventType.POP);
                    provider.addFutureEvent(8, stack1, stackQuark, FutureEventType.POP);
                }
                return true;

            });

            TestStateSystemModule module = fModule;
            module.schedule();
            assertTrue(module.waitForCompletion());

            ITmfStateSystem ss = module.getStateSystem();
            assertNotNull(ss);

            List<@NonNull ITmfStateInterval> expected = ImmutableList.of(new StateIntervalStub(1, 4, oneValue), new StateIntervalStub(5, 10, (Object) null));
            StateSystemTestUtils.testIntervalForAttributes(ss, expected, futureVal);

            expected = ImmutableList.of(new StateIntervalStub(1, 7, stack1), new StateIntervalStub(8, 10, (Object) null));
            StateSystemTestUtils.testIntervalForAttributes(ss, expected, futureStack, "1");

            expected = ImmutableList.of( new StateIntervalStub(1, 3, (Object) null),
                    new StateIntervalStub(4, 6, stack2), new StateIntervalStub(7, 10, (Object) null));
            StateSystemTestUtils.testIntervalForAttributes(ss, expected, futureStack, "2");
        } finally {
            TestStateSystemProvider.setEventHandler(null);
        }
    }

    /**
     * Test clearing persistent data
     *
     * @throws TmfAnalysisException
     *             An exception when setting the trace
     */
    @Test
    public void testClearSsModule() throws TmfAnalysisException {
        TestStateSystemModule module = new TestStateSystemModule(true);
        TestStateSystemModule module2 = new TestStateSystemModule(true);
        try {
            ITmfTrace trace = fTrace;
            assertNotNull(trace);
            module.setTrace(trace);
            module2.setTrace(trace);

            // Execute the first module
            module.schedule();
            assertTrue(module.waitForCompletion());

            // Check if state system file exists
            File ssFile = module.getSsFile();
            assertNotNull(ssFile);
            assertTrue(ssFile.exists());

            // Delete state system file while open
            module.clearPersistentData();

            // The state system file should be deleted
            ssFile = module.getSsFile();
            assertNotNull(ssFile);
            assertFalse(ssFile.exists());

            // Re-schedule
            module.schedule();
            assertTrue(module.waitForCompletion());
            // Dispose module (close file)
            module.dispose();

            module2.clearPersistentData();

            // Delete state system file while closed
            ssFile = module.getSsFile();
            assertNotNull(ssFile);
            assertFalse(ssFile.exists());
        } finally {
            module.dispose();
            module2.dispose();
        }
    }

}
