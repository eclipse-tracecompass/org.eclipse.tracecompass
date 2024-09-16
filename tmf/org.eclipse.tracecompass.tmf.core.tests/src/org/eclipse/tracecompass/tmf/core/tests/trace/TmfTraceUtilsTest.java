/*******************************************************************************
 * Copyright (c) 2014, 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.tests.trace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.counters.core.aspects.CounterAspect;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.tests.analysis.AnalysisManagerTest;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.tests.stubs.analysis.TestAnalysis;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Test suite for {@link TmfTraceUtils}
 */
public class TmfTraceUtilsTest {

    private static final @NonNull TestEventAspect TEST_ASPECT = new TestEventAspect();

    private static final TmfTestTrace TEST_TRACE = TmfTestTrace.A_TEST_10K;

    private TmfTrace fTrace;

    // ------------------------------------------------------------------------
    // Test trace class definition
    // ------------------------------------------------------------------------
    private static final @NonNull Collection<ITmfEventAspect<?>> EVENT_ASPECTS;
    static {
        ImmutableList.Builder<ITmfEventAspect<?>> builder = ImmutableList.builder();
        builder.add(new TmfCpuAspect() {
            @Override
            public Integer resolve(ITmfEvent event) {
                return 1;
            }
        });
        builder.addAll(TmfTrace.BASE_ASPECTS);
        EVENT_ASPECTS = builder.build();
    }

    private static class TmfTraceStubWithAspects extends TmfTraceStub {

        public TmfTraceStubWithAspects(String path) throws TmfTraceException {
            super(path, ITmfTrace.DEFAULT_TRACE_CACHE_SIZE, false, null);
        }

        @Override
        public Iterable<ITmfEventAspect<?>> getEventAspects() {
            return EVENT_ASPECTS;
        }

    }

    private static class TestEventAspect implements ITmfEventAspect<@NonNull Integer> {

        public static final Integer RESOLVED_VALUE = 2;
        public static final @NonNull String ASPECT_NAME = "test";

        @Override
        public @NonNull String getName() {
            return ASPECT_NAME;
        }

        @Override
        public @NonNull String getHelpText() {
            return ASPECT_NAME;
        }

        @Override
        public @Nullable Integer resolve(@NonNull ITmfEvent event) {
            return RESOLVED_VALUE;
        }

        @Override
        public DataType getDataType() {
            return DataType.NUMBER;
        }

    }

    // ------------------------------------------------------------------------
    // Housekeeping
    // ------------------------------------------------------------------------

    /**
     * Test setup
     */
    @Before
    public void setUp() {
        try {
            fTrace = new TmfTraceStubWithAspects(TEST_TRACE.getFullPath());
            TmfSignalManager.deregister(fTrace);
            fTrace.indexTrace(true);
        } catch (final TmfTraceException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test cleanup
     */
    @After
    public void tearDown() {
        fTrace.dispose();
        fTrace = null;
    }

    // ------------------------------------------------------------------------
    // Test methods
    // ------------------------------------------------------------------------

    /**
     * Test the {@link TmfTraceUtils#getAnalysisModuleOfClass} method.
     */
    @Test
    public void testGetModulesByClass() {
        TmfTrace trace = fTrace;
        assertNotNull(trace);

        /* Open the trace, the modules should be populated */
        trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));

        Iterable<TestAnalysis> testModules = TmfTraceUtils.getAnalysisModulesOfClass(trace, TestAnalysis.class);
        assertTrue(testModules.iterator().hasNext());
        /*
         * FIXME: The exact count depends on the context the test is run (full test
         * suite or this file only), but there must be at least 2 modules
         */
        assertTrue(Iterables.size(testModules) >= 2);

        TestAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, TestAnalysis.class, AnalysisManagerTest.MODULE_PARAM);
        assertNotNull(module);
        IAnalysisModule traceModule = trace.getAnalysisModule(AnalysisManagerTest.MODULE_PARAM);
        assertNotNull(traceModule);
        assertEquals(module, traceModule);

    }

    /**
     * Test the {@link TmfTraceUtils#getAnalysisModuleOfClass} method.
     */
    @Test
    public void testGetModulesByClassForHost() {
        TmfTrace trace = fTrace;
        assertNotNull(trace);

        /*
         * Open the trace, the modules should be populated and make sure the signal is
         * received by the trace manager
         */
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, trace, null);
        trace.traceOpened(signal);
        TmfTraceManager.getInstance().traceOpened(signal);

        Iterable<TestAnalysis> testModules = TmfTraceUtils.getAnalysisModulesOfClass(trace.getHostId(), TestAnalysis.class);
        assertTrue(testModules.iterator().hasNext());
        /*
         * FIXME: The exact count depends on the context the test is run (full test
         * suite or this file only), but there must be at least 2 modules
         */
        assertTrue(Iterables.size(testModules) >= 2);
    }

    /**
     * Test the
     * {@link TmfTraceUtils#resolveEventAspectOfClassForEvent(ITmfTrace, Class, ITmfEvent)}
     * method.
     */
    @Test
    public void testResolveEventAspectsOfClassForEvent() {
        TmfTrace trace = fTrace;
        assertNotNull(trace);

        ITmfContext context = trace.seekEvent(0L);
        ITmfEvent event = trace.getNext(context);
        assertNotNull(event);

        /* Make sure the CPU aspect returns the expected value */
        Object cpuObj = TmfTraceUtils.resolveEventAspectOfClassForEvent(trace, TmfCpuAspect.class, event);
        assertNotNull(cpuObj);
        assertEquals(1, cpuObj);

    }

    /**
     * Test the
     * {@link TmfTraceUtils#resolveAspectOfNameForEvent(ITmfTrace, String, ITmfEvent)}
     * method.
     */
    @Test
    public void testResolveEventAspectsOfNameForEvent() {
        TmfTrace trace = fTrace;
        assertNotNull(trace);

        ITmfContext context = trace.seekEvent(0L);
        ITmfEvent event = trace.getNext(context);
        assertNotNull(event);

        /* Make sure the CPU aspect returns the expected value */
        Object cpuObj = TmfTraceUtils.resolveAspectOfNameForEvent(trace, "cpu", event);
        assertNotNull(cpuObj);
        assertEquals(1, cpuObj);

    }

    /**
     * Test the {@link TmfTraceUtils#getEventAspects(ITmfTrace, Class)} method
     */
    @Test
    public void testGetAspects() {
        TmfTrace trace = fTrace;

        assertNotNull(trace);

        Iterable<@NonNull ITmfEventAspect<?>> aspect = TmfTraceUtils.getEventAspects(trace, TestEventAspect.class);
        assertNotNull(aspect);
        assertEquals(1, Iterables.size(aspect));
        assertTrue(Iterables.contains(aspect, TEST_ASPECT));

        Iterable<@NonNull ITmfEventAspect<?>> cpuAspect = TmfTraceUtils.getEventAspects(trace, TmfCpuAspect.class);
        assertNotNull(cpuAspect);
        assertEquals("CPU", cpuAspect.iterator().next().getName());

        Iterable<@NonNull ITmfEventAspect<?>> badAspect = TmfTraceUtils.getEventAspects(trace, CounterAspect.class);
        assertNotNull(badAspect);
        assertTrue(Iterables.isEmpty(badAspect));
    }

    /**
     * Test the {@link TmfTraceUtils#registerEventAspect(ITmfEventAspect)} method
     */
    @Test
    public void testAdditionalAspects() {
        TmfTrace trace = fTrace;

        assertNotNull(trace);

        ITmfContext context = trace.seekEvent(0L);
        ITmfEvent event = trace.getNext(context);
        assertNotNull(event);

        // Make sure the aspect is not resolved
        Object obj = TmfTraceUtils.resolveEventAspectOfClassForEvent(trace, TestEventAspect.class, event);
        assertNull(obj);

        obj = TmfTraceUtils.resolveAspectOfNameForEvent(trace, TestEventAspect.ASPECT_NAME, event);
        assertNull(obj);

        Integer val = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(trace, TestEventAspect.class, event);
        assertNull(val);

        // Register the aspect
        TmfTraceUtils.registerEventAspect(TEST_ASPECT);
        // See that the aspect is resolved now
        obj = TmfTraceUtils.resolveEventAspectOfClassForEvent(trace, TestEventAspect.class, event);
        assertNotNull(obj);
        assertEquals(TestEventAspect.RESOLVED_VALUE, obj);

        // See if it is resolved by name as well
        obj = TmfTraceUtils.resolveAspectOfNameForEvent(trace, TestEventAspect.ASPECT_NAME, event);
        assertNotNull(obj);
        assertEquals(TestEventAspect.RESOLVED_VALUE, obj);

        // See if it is resolved by Integer type as well
        val = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(trace, TestEventAspect.class, event);
        assertNotNull(val);
        assertEquals(TestEventAspect.RESOLVED_VALUE, val);
    }
}
