/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.core.flamegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TmfTimeGraphCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.annotations.VisibleForTesting;

/**
 * Factory for the flame graph data provider
 *
 * @author Geneviève Bastien
 */
public class FlameGraphDataProviderFactory implements IDataProviderFactory {

    private static Map<String, FlameGraphDataProvider<?, ?, ?>> INSTANCES = new HashMap<>();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        // Need the analysis
        return null;
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace, String secondaryId) {
        Collection<ITmfTrace> traces = TmfTraceManager.getTraceSetWithExperiment(trace);
        // Create a composite data provider for all traces
        List<FlameGraphDataProvider<?, ?, ?>> providers = new ArrayList<>();
        for (ITmfTrace child : traces) {
            FlameGraphDataProvider<?, ?, ?> childProvider = create(child, secondaryId);
            if (childProvider != null) {
                providers.add(childProvider);
            }
        }
        if (providers.isEmpty()) {
            return null;
        } else if (providers.size() == 1) {
            return providers.get(0);
        }
        return new TmfTimeGraphCompositeDataProvider<>(providers, FlameGraphDataProvider.ID + ':' + secondaryId);
    }

    private static @Nullable FlameGraphDataProvider<?, ?, ?> create(ITmfTrace trace, String secondaryId) {
        FlameGraphDataProvider<?, ?, ?> dataProvider = INSTANCES.get(secondaryId);
        if (dataProvider != null) {
            return dataProvider;
        }
        // The trace can be an experiment, so we need to know if there are
        // multiple analysis modules with the same ID
        IAnalysisModule analysisModule = trace.getAnalysisModule(secondaryId);
        if (!(analysisModule instanceof IWeightedTreeProvider)) {
            return null;
        }
        analysisModule.schedule();
        return new FlameGraphDataProvider<>(trace, (IWeightedTreeProvider<?, ?, ?>) analysisModule, FlameGraphDataProvider.ID + ':' + secondaryId);
    }

    /**
     * Adds a reference to a data provider identified by the id, but not
     * associated with a trace. Useful for data provider unit testing where
     * fixtures of data are used without a trace
     *
     * @param id
     *            ID of the data provider. A <code>null</code> value will remove
     *            the data provider from the instance list
     * @param dataProvider
     *            The data provider
     */
    @VisibleForTesting
    public static void registerDataProviderWithId(String id, @Nullable FlameGraphDataProvider<?, ?, ?> dataProvider) {
        if (dataProvider == null) {
            INSTANCES.remove(id);
            return;
        }
        INSTANCES.put(id, dataProvider);
    }
}
