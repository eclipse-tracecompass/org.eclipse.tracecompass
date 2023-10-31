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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
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
}
