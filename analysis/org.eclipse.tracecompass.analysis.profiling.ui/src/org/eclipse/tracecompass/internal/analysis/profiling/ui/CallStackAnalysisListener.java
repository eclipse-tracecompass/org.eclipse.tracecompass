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

package org.eclipse.tracecompass.internal.analysis.profiling.ui;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.SegmentStoreStatisticsView;
import org.eclipse.tracecompass.internal.analysis.profiling.ui.flamegraph2.FlameGraphSelView;
import org.eclipse.tracecompass.internal.analysis.profiling.ui.flamegraph2.FlameGraphView;
import org.eclipse.tracecompass.internal.analysis.profiling.ui.functiondensity.FunctionDensityView;
import org.eclipse.tracecompass.internal.analysis.profiling.ui.weightedtree.WeightedTreeView;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.ITmfNewAnalysisModuleListener;
import org.eclipse.tracecompass.tmf.ui.analysis.TmfAnalysisViewOutput;

/**
 * Registers the {@link FlameChartView} to {@link IFlameChartProvider}. The
 * analysis being an abstract class, it is not possible to use the output
 * extension to add the view, but the listener fixes the issue.
 *
 * @author Geneviève Bastien
 */
public class CallStackAnalysisListener implements ITmfNewAnalysisModuleListener {

    @Override
    public void moduleCreated(@Nullable IAnalysisModule module) {
        if (module instanceof IFlameChartProvider) {
            module.registerOutput(new TmfAnalysisViewOutput(FlameChartView.ID, module.getId()));
            module.registerOutput(new TmfAnalysisViewOutput(FunctionDensityView.ID, module.getId()));
            module.registerOutput(new TmfAnalysisViewOutput(SegmentStoreStatisticsView.ID, module.getId()));
        }
        if (module instanceof IWeightedTreeProvider) {
            module.registerOutput(new TmfAnalysisViewOutput(FlameGraphView.ID, module.getId()));
            module.registerOutput(new TmfAnalysisViewOutput(FlameGraphSelView.SEL_ID, module.getId()));
            module.registerOutput(new TmfAnalysisViewOutput(WeightedTreeView.ID, module.getId()));
        }
    }
}
