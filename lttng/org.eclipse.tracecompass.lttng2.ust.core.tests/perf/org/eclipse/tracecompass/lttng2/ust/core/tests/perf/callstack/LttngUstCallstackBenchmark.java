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

package org.eclipse.tracecompass.lttng2.ust.core.tests.perf.callstack;

import java.util.Arrays;

import org.eclipse.tracecompass.analysis.profiling.core.tests.perf.CallStackAndGraphBenchmark;
import org.eclipse.tracecompass.ctf.core.tests.shared.CtfBenchmarkTrace;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.lttng2.ust.core.callstack2.LttngUstCallStackAnalysis;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.tests.shared.CtfTmfTestTraceUtils;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Benchmarks the {@link LttngUstCallStackAnalysis} class and its associated
 * {@link CallGraphAnalysis}
 *
 * @author Geneviève Bastien
 */
@RunWith(Parameterized.class)
public class LttngUstCallstackBenchmark extends CallStackAndGraphBenchmark {

    private static String getPathFromCtfTestTrace(CtfTestTrace testTrace) {
        CtfTmfTrace ctftrace = CtfTmfTestTraceUtils.getTrace(testTrace);
        String path = ctftrace.getPath();
        if (path == null) {
            throw new IllegalArgumentException("Path shouldn't be null");
        }
        ctftrace.dispose();
        return path;
    }

    /**
     * Get the traces to benchmark
     *
     * @return The arrays of parameters
     */
    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
                { CtfTestTrace.CYG_PROFILE.name(), getPathFromCtfTestTrace(CtfTestTrace.CYG_PROFILE) },
                { CtfBenchmarkTrace.UST_QMLSCENE.name(), CtfBenchmarkTrace.UST_QMLSCENE.getTracePath().toString() },
        });
    }

    private final String fTestTrace;

    /**
     * Constructor
     *
     * @param name
     *            A name for this test
     * @param tracePath
     *            The absolute path to the trace to test
     */
    public LttngUstCallstackBenchmark(String name, String tracePath) {
        super(name, LttngUstCallStackAnalysis.ID);
        fTestTrace = tracePath;
    }

    @Override
    protected TmfTrace getTrace() throws TmfTraceException {
        LttngUstTrace trace = new LttngUstTrace();
        trace.initTrace(null, fTestTrace, ITmfEvent.class);
        return trace;
    }
}
