/*******************************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.tests.swslatency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.swslatency.SWSLatencyAnalysis;
import org.eclipse.tracecompass.analysis.os.linux.core.swslatency.SchedWS;
import org.eclipse.tracecompass.analysis.os.linux.core.swslatency.SchedWS.InitialInfo;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.Activator;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.trace.TmfXmlKernelTraceStub;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test suite for the {@link SWSLatencyAnalysis} class
 *
 * @author Abdellah Rahmani
 */
public class SWSLatencyTest {

    private static final String SWS_USAGE_FILE = "testfiles/sws_analysis.xml";
    private IKernelTrace fTrace;
    private SWSLatencyAnalysis fModule;

    private static void deleteSuppFiles(ITmfTrace trace) {
        /* Remove supplementary files */
        if (trace != null) {
            File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(trace));
            for (File file : suppDir.listFiles()) {
                file.delete();
            }
        }
    }

    /**
     * Setup the trace for the tests
     */
    @Before
    public void setUp() {
        IKernelTrace trace = new TmfXmlKernelTraceStub();
        IPath filePath = Activator.getAbsoluteFilePath(SWS_USAGE_FILE);
        IStatus status = trace.validate(null, filePath.toOSString());
        if (!status.isOK()) {
            fail(status.getException().getMessage());
        }
        try {
            trace.initTrace(null, filePath.toOSString(), TmfEvent.class);
        } catch (TmfTraceException e) {
            fail(e.getMessage());
        }
        deleteSuppFiles(trace);
        ((TmfTrace) trace).traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        fModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, SWSLatencyAnalysis.class, SWSLatencyAnalysis.ID);
        assertNotNull(fModule);
        fModule.schedule();
        fModule.waitForCompletion();
        fTrace = trace;
    }

    /**
     * Dispose everything
     */
    @After
    public void cleanup() {
        ITmfTrace testTrace = fTrace;
        if (testTrace != null) {
            testTrace.dispose();
        }
    }

    /**
     * This will load the analysis and test it. as it depends on Kernel, this
     * test runs the kernel trace first then the analysis
     */
    @SuppressWarnings("null")
    @Test
    public void testSmallTraceSequential() {
        SWSLatencyAnalysis swsModule = fModule;
        assertNotNull(swsModule);
        ISegmentStore<@NonNull ISegment> segmentStore = swsModule.getSegmentStore();
        assertNotNull(segmentStore);
        assertEquals(false, segmentStore.isEmpty());
        assertEquals(5, segmentStore.size());

        List<InitialInfo> info = ImmutableList.of(new InitialInfo(0, "proc1", 2), new InitialInfo(3, "proc3", 3),
                new InitialInfo(6, "proc4", 4), new InitialInfo(10, "proc1", 1), new InitialInfo(3, "proc2", 2));
        List<SchedWS> expected = ImmutableList.of(new SchedWS(info.get(0), 1, 20), new SchedWS(info.get(1), 5, 0),
                new SchedWS(info.get(2), 10, 20), new SchedWS(info.get(3), 15, 20), new SchedWS(info.get(4), 25, 20));

        Iterator<ISegment> it = segmentStore.iterator();
        for (int i = 0; i < expected.size(); i++) {
            if (it.hasNext()) {
                SchedWS segment = (SchedWS) it.next();
                long startTime = expected.get(i).getStart();
                long endTime = expected.get(i).getEnd();
                long duration = expected.get(i).getLength();
                int threadID = expected.get(i).getTid();
                int priority = expected.get(i).getPriority();
                String name = expected.get(i).getName();

                assertEquals("Start time of the segment " + i, startTime, segment.getStart());
                assertEquals("End time of the segment " + i, endTime, segment.getEnd());
                assertEquals("Duration of the segment " + i, duration, segment.getLength());
                assertEquals("TID in segment " + i, threadID, segment.getTid());
                assertEquals("Priority of the process in segment " + i, priority, segment.getPriority());
                assertEquals("Name of the process in segment " + i, name, segment.getName());
            }
        }
    }
}