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

import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * A kernel state provider stub
 *
 * This reproduces the thread statuses as follows:
 *
 * <pre>
 * |- THREADS
 * |  |- <Thread number> -> Thread Status
 * |  |  |- SYSTEM_CALL
 * </pre>
 *
 * @author Arnaud Fiorini
 */
public class KernelStateProviderStub extends AbstractTmfStateProvider {

    private static final String ENTRY = "entry";

    /**
     * Constructor
     *
     * @param trace
     *            The trace to run this provider on
     */
    public KernelStateProviderStub(ITmfTrace trace) {
        super(trace, KernelAnalysisModuleStub.ID1);
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        return new KernelStateProviderStub(getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        ITmfStateSystemBuilder ssb = getStateSystemBuilder();
        if (ssb == null) {
            return;
        }
        int threadQuark = ssb.getQuarkAbsoluteAndAdd(Attributes.THREADS, event.getContent().getFieldValue(String.class, "tid"));
        boolean isEntry = event.getName().equals(ENTRY);
        long timestamp = event.getTimestamp().getValue();
        if (isEntry) {
            if (event.getName().equals("op4")) {
                int systemCallQuark = ssb.getQuarkRelativeAndAdd(threadQuark, Attributes.SYSTEM_CALL);
                ssb.modifyAttribute(timestamp, "openat", systemCallQuark);
                /* Put the process in system call mode */
                ssb.modifyAttribute(timestamp, ProcessStatus.RUN_SYTEMCALL.getStateValue().unboxValue(), threadQuark);
            } else {
                ssb.modifyAttribute(timestamp, ProcessStatus.RUN.getStateValue().unboxValue(), threadQuark);
            }
        } else {
            ssb.modifyAttribute(timestamp, ProcessStatus.WAIT_CPU.getStateValue().unboxValue(), threadQuark);
        }
    }
}
