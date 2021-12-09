/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.ust.core.tests.analysis.cpu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.lttng2.ust.core.analysis.cpu.UstCpuAnalysisModule;
import org.eclipse.tracecompass.internal.lttng2.ust.core.analysis.cpu.UstCpuStateProvider;
import org.eclipse.tracecompass.lttng2.ust.core.tests.shared.LttngUstTestTraceUtils;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link UstCpuAnalysisModule}
 *
 * @author Arnaud Fiorini
 */
public class UstCpuAnalysisModuleTest {

    private static final @NonNull CtfTestTrace TRACE_WITHOUT_VTID = CtfTestTrace.DEBUG_INFO4;
    private static final @NonNull CtfTestTrace TRACE_WITH_VTID = CtfTestTrace.CYG_PROFILE;

    private UstCpuAnalysisModule fModule;

    /**
     * Test setup
     */
    @Before
    public void setup() {
        fModule = new UstCpuAnalysisModule();
    }

    /**
     * Test cleanup
     */
    @After
    public void tearDown() {
        fModule.dispose();
        fModule = null;
    }

    /**
     * Test for {@link UstCpuAnalysisModule#getAnalysisRequirements()}
     */
    @Test
    public void testGetAnalysisRequirements() {
        Iterable<TmfAbstractAnalysisRequirement> requirements = fModule.getAnalysisRequirements();
        assertNotNull(requirements);
        assertEquals(1, StreamSupport.stream(requirements.spliterator(), false).count());
    }

    /**
     * Test that the analysis can execute on a valid trace.
     */
    @Test
    public void testCanExecute() {
        LttngUstTrace trace = LttngUstTestTraceUtils.getTrace(TRACE_WITH_VTID);
        assertTrue(fModule.canExecute(trace));
        LttngUstTestTraceUtils.dispose(TRACE_WITH_VTID);
    }

    /**
     * Test that the analysis correctly refuses to execute on a trace that does
     * not provide vtid.
     */
    @Test
    public void testCannotExcecute() {
        LttngUstTrace invalidTrace = LttngUstTestTraceUtils.getTrace(TRACE_WITHOUT_VTID);
        assertFalse(fModule.canExecute(invalidTrace));
        LttngUstTestTraceUtils.dispose(TRACE_WITHOUT_VTID);
    }

    private void executeModule(@NonNull ITmfTrace trace) {
        try {
            fModule.setTrace(trace);
        } catch (TmfAnalysisException e) {
            fail();
        }
        fModule.schedule();
        fModule.waitForCompletion();
    }

    /**
     * Test that basic execution of the module works well.
     */
    @Test
    public void testExecution() {
        LttngUstTrace trace = LttngUstTestTraceUtils.getTrace(TRACE_WITH_VTID);
        executeModule(trace);
        ITmfStateSystem ss = fModule.getStateSystem();
        assertNotNull(ss);
        LttngUstTestTraceUtils.dispose(TRACE_WITH_VTID);
    }

    /**
     * Test that the state system generated is correct.
     */
    @Test
    public void testGetCpuForThread() {
        LttngUstTrace trace = LttngUstTestTraceUtils.getTrace(TRACE_WITH_VTID);
        executeModule(trace);
        ITmfStateSystem ss = fModule.getStateSystem();
        assertNotNull(ss);

        // Test threads quark exists
        int parentThreadsQuark = -1;
        try {
            parentThreadsQuark = ss.getQuarkAbsolute(UstCpuStateProvider.THREADS);
        } catch (AttributeNotFoundException e) {
            fail();
        }

        // Test that there is the correct number of threads
        List<@NonNull Integer> threadsQuarks = ss.getSubAttributes(parentThreadsQuark, false);
        assertEquals(1, threadsQuarks.size());

        // Test that at multiple specific time the cpu value is correct
        Integer threadQuark = threadsQuarks.iterator().next();
        try {
            // End of the trace
            ITmfStateInterval cpuState = ss.querySingleState(ss.getCurrentEndTime(), threadQuark);
            assertNotNull(cpuState.getValue());
            assertEquals(3, cpuState.getValue());
            // Beginning of the trace
            cpuState = ss.querySingleState(ss.getStartTime(), threadQuark);
            assertNotNull(cpuState.getValue());
            assertEquals(0, cpuState.getValue());
            // Middle of the trace
            cpuState = ss.querySingleState(ss.getStartTime() + (ss.getCurrentEndTime() - ss.getStartTime()) / 2, threadQuark);
            assertNotNull(cpuState.getValue());
            assertEquals(3, cpuState.getValue());
        } catch (StateSystemDisposedException e) {
            fail();
        }
        LttngUstTestTraceUtils.dispose(TRACE_WITH_VTID);
    }
}
