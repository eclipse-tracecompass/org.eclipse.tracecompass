/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.callstack.core.tests.stubs;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * A kernel analysis stub, using a kernel state provider stub
 *
 * This reproduces only the threads statuses part of the kernel state analysis.
 *
 * @author Arnaud Fiorini
 */
public class KernelAnalysisModuleStub extends KernelAnalysisModule {

    /**
     * The ID of this analysis
     */
    public static final String ID1 = "org.eclipse.tracecompass.analysis.callstack.core.tests.kernelanalysis.stub";

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = Objects.requireNonNull(getTrace());
        return new KernelStateProviderStub(trace);
    }
}
