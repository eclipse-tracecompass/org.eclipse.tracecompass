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

import java.util.Collections;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.lttng2.ust.core.trace.ContextVtidAspect;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement.PriorityLevel;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.ImmutableSet;

/**
 * This analysis builds a state system from the cpu
 *
 * @author Arnaud Fiorini
 */
public class UstCpuAnalysisModule extends TmfStateSystemAnalysisModule {

    /**
     * Analysis ID, it should match that in the plugin.xml file
     */
    public static final @NonNull String ID = "org.eclipse.linuxtools.lttng2.ust.analysis.cpu"; //$NON-NLS-1$

    private static final @NonNull TmfAbstractAnalysisRequirement VTID_ASPECT_REQUIREMENT = new TmfAbstractAnalysisRequirement(
            Collections.emptySet(), PriorityLevel.MANDATORY) {
        @Override
        public boolean test(ITmfTrace trace) {
            if (trace instanceof LttngUstTrace) {
                Iterable<@NonNull ITmfEventAspect<?>> eventAspects = TmfTraceUtils.getEventAspects(trace, ContextVtidAspect.class);
                return eventAspects.iterator().hasNext();
            }
            return false;
        }
    };

    @Override
    public ITmfStateProvider createStateProvider() {
        return new UstCpuStateProvider(Objects.requireNonNull(getTrace()));
    }

    @Override
    public Iterable<TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return ImmutableSet.of(VTID_ASPECT_REQUIREMENT);
    }
}
