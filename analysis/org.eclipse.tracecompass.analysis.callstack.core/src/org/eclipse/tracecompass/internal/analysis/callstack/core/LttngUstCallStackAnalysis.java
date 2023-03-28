/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.IHostModel;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.IHostModel.ModelDataType;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.ModelManager;
import org.eclipse.tracecompass.internal.lttng2.ust.core.callstack.LttngUstCallStackProvider;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.lttng2.ust.core.trace.layout.ILttngUstEventLayout;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableSet;

/**
 * This is the LTTng UST analysis, ported to the new CallStack structure. It
 * uses the same state provider as before
 *
 * @author Geneviève Bastien
 */
public class LttngUstCallStackAnalysis extends InstrumentedCallStackAnalysis {

    /**
     * ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.callstack.core.analysis"; //$NON-NLS-1$

    private @Nullable Set<TmfAbstractAnalysisRequirement> fAnalysisRequirements = null;

    @Override
    public boolean setTrace(ITmfTrace trace) throws TmfAnalysisException {
        if (!(trace instanceof LttngUstTrace)) {
            return false;
        }
        return super.setTrace(trace);
    }

    @Override
    public @Nullable LttngUstTrace getTrace() {
        return (LttngUstTrace) super.getTrace();
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new LttngUstCallStackProvider(Objects.requireNonNull(getTrace()));
    }

    @Override
    protected @NonNull Iterable<IAnalysisModule> getDependentAnalyses() {
        LttngUstTrace trace = getTrace();
        if (trace == null) {
            return Collections.emptyList();
        }
        IHostModel modelFor = ModelManager.getModelFor(trace.getHostId());
        EnumSet<ModelDataType> ids = EnumSet.of(ModelDataType.TID, ModelDataType.PID);
        EnumSet<ModelDataType> checked = Objects.requireNonNull(ids);
        return modelFor.getRequiredModules(checked);
    }

    @Override
    public @NonNull Iterable<TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        Set<TmfAbstractAnalysisRequirement> requirements = fAnalysisRequirements;
        if (requirements == null) {
            LttngUstTrace trace = getTrace();
            ILttngUstEventLayout layout = ILttngUstEventLayout.DEFAULT_LAYOUT;
            if (trace != null) {
                layout = trace.getEventLayout();
            }
            requirements = ImmutableSet.of(new LttngUstCallStackAnalysisRequirement(layout));
            fAnalysisRequirements = requirements;
        }
        return requirements;
    }
}
