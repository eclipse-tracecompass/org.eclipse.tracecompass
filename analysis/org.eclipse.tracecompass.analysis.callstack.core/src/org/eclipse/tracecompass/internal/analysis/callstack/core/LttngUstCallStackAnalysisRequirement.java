/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.util.Collection;

import org.eclipse.tracecompass.internal.lttng2.ust.core.callstack.Messages;
import org.eclipse.tracecompass.lttng2.ust.core.trace.layout.ILttngUstEventLayout;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfCompositeAnalysisRequirement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Analysis requirement implementation for LTTng Call Stack Analysis.
 *
 * @author Bernd Hufmann
 *
 */
public class LttngUstCallStackAnalysisRequirement extends TmfCompositeAnalysisRequirement {

    /**
     * Constructor
     *
     * @param layout
     *            The event layout (non-null)
     */
    public LttngUstCallStackAnalysisRequirement(ILttngUstEventLayout layout) {
        super(getSubRequirements(layout), PriorityLevel.AT_LEAST_ONE);

        addInformation(nullToEmptyString(Messages.LttnUstCallStackAnalysisModule_EventsLoadingInformation));
    }

    private static Collection<TmfAbstractAnalysisRequirement> getSubRequirements(ILttngUstEventLayout layout) {

        // Requirement for the cyg_profile events
        TmfAnalysisEventRequirement cygProfile = new TmfAnalysisEventRequirement(
                ImmutableList.of(layout.eventCygProfileFuncEntry(), layout.eventCygProfileFuncExit()),
                PriorityLevel.MANDATORY);

        // Requirement for the cyg_profile_fast events
        TmfAbstractAnalysisRequirement cygProfileFast = new TmfAnalysisEventRequirement(
                ImmutableList.of(layout.eventCygProfileFastFuncEntry(), layout.eventCygProfileFastFuncExit()),
                PriorityLevel.MANDATORY);

        return ImmutableSet.of(cygProfile, cygProfileFast);
    }
}
