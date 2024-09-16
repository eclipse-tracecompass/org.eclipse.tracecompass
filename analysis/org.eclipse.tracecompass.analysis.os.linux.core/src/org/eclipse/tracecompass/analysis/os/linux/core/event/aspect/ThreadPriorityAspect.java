/*******************************************************************************
 * Copyright (c) 2015 Keba AG
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christian Mansky - Initial implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.event.aspect;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelTidAspect;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * This aspect finds the priority of the thread running from this event using
 * the {@link KernelAnalysisModule}.
 *
 * @author Christian Mansky
 * @since 2.0
 */
public final class ThreadPriorityAspect implements ITmfEventAspect<Integer> {

    /** The singleton instance */
    public static final ThreadPriorityAspect INSTANCE = new ThreadPriorityAspect();

    private ThreadPriorityAspect() {
    }

    @Override
    public final String getName() {
        return NonNullUtils.nullToEmptyString(Messages.AspectName_Prio);
    }

    @Override
    public final String getHelpText() {
        return NonNullUtils.nullToEmptyString(Messages.AspectHelpText_Prio);
    }

    @Override
    public @Nullable Integer resolve(ITmfEvent event) {
        final @NonNull ITmfTrace trace = event.getTrace();
        KernelAnalysisModule kernelAnalysis = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
        if (kernelAnalysis == null) {
            return null;
        }

        ITmfStateSystem ss = kernelAnalysis.getStateSystem();
        if (ss == null) {
            return null;
        }

        Integer tid = KernelTidAspect.INSTANCE.resolve(event);
        if (tid == null) {
            return null;
        }

        final long ts = event.getTimestamp().getValue();
        Integer execPrio = null;
        try {
            Integer cpu = 0;
            if (tid == 0) {
                /* Find the CPU this event is run on */
                cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(trace, TmfCpuAspect.class, event);
            }
            int execPrioQuark = ss.getQuarkAbsolute(Attributes.THREADS, Attributes.buildThreadAttributeName(tid, cpu), Attributes.PRIO);
            ITmfStateInterval interval = ss.querySingleState(ts, execPrioQuark);
            ITmfStateValue prioValue = interval.getStateValue();
            /* We know the prio must be an Integer */
            execPrio = prioValue.unboxInt();
        } catch (AttributeNotFoundException | StateSystemDisposedException | TimeRangeException e) {
        }
        return execPrio;
    }

    @Override
    public DataType getDataType() {
        return DataType.NUMBER;
    }
}
