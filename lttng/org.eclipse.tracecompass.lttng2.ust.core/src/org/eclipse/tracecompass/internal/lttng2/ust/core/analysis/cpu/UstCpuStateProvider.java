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

package org.eclipse.tracecompass.internal.lttng2.ust.core.analysis.cpu;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.lttng2.ust.core.trace.ContextVtidAspect;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;

/**
 * Creates a state system with a quark for each thread describing which CPU is
 * in use.
 *
 * This state system is useful to get the CPU number in analysis of LTTng UST
 * traces. More specifically, to get callsite information or other analyses.
 *
 * Attribute tree:
 *
 * <pre>
 * |- Threads
 * |  |- <TID> -> CPU number for this TID depending on LTTng UST events
 * </pre>
 *
 * @author Arnaud Fiorini
 */
public class UstCpuStateProvider extends AbstractTmfStateProvider {

    /**
     * State system ID
     */
    public static final @NonNull String ID = "org.eclipse.linuxtools.lttng2.ust.analysis.cpu.stateprovider"; //$NON-NLS-1$

    /** State system attribute name for the Threads to CPU mappings */
    public static final String THREADS = "Threads"; //$NON-NLS-1$

    /**
     * @param trace
     *            the trace to build the state system
     */
    public UstCpuStateProvider(@NonNull ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new UstCpuStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ssb = checkNotNull(getStateSystemBuilder());
        CtfTmfEvent ctfEvent = (CtfTmfEvent) event;
        int cpuId = ctfEvent.getCPU();
        Object vtid = TmfTraceUtils.resolveEventAspectOfClassForEvent(getTrace(), ContextVtidAspect.class, event);
        if (vtid != null) {
            int threadQuark = ssb.getQuarkAbsoluteAndAdd(THREADS, vtid.toString());
            ssb.modifyAttribute(event.getTimestamp().getValue(), cpuId, threadQuark);
        }
    }
}
