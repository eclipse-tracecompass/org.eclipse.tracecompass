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

package org.eclipse.tracecompass.analysis.profiling.core.tests.stubs2;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.EdgeStateValue;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateIntervalStub;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * A call stack analysis stub, using a call stack state provider stub
 *
 * @author Geneviève Bastien
 */
public class CallStackAnalysisStub extends InstrumentedCallStackAnalysis {

    /**
     * The ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.profiling.core.tests.stub";

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new IllegalArgumentException();
        }
        return new CallStackProviderStub(trace);
    }

    @Override
    public List<String[]> getPatterns() {
        return super.getPatterns();
    }

    @Override
    public List<ITmfStateInterval> getLinks(long start, long end, IProgressMonitor monitor) {
        ITmfTrace trace = Objects.requireNonNull(getTrace());
        String hostId = trace.getHostId();

        HostThread tid2 = new HostThread(hostId, 2);
        HostThread tid3 = new HostThread(hostId, 3);
        HostThread tid6 = new HostThread(hostId, 6);
        HostThread tid7 = new HostThread(hostId, 7);

        List<ITmfStateInterval> intervals = List.of(new StateIntervalStub(1, 2, new EdgeStateValue(1, tid2, tid2)),
                new StateIntervalStub(4, 6, new EdgeStateValue(2, tid2, tid3)),
                new StateIntervalStub(5, 8, new EdgeStateValue(3, tid3, tid6)),
                new StateIntervalStub(5, 9, new EdgeStateValue(4, tid6, tid7)),
                new StateIntervalStub(9, 11, new EdgeStateValue(5, tid6, tid6)));

        return intervals.stream()
                .filter(i -> (i.getStartTime() >= start && i.getStartTime() <= end) || (i.getEndTime() >= start && i.getEndTime() <= end))
                .collect(Collectors.toList());
    }
}
