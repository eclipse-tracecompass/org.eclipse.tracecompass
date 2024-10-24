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

package org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TmfTimeGraphCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.Iterables;

/**
 * Factory for the flame chart data provider
 *
 * @author Geneviève Bastien
 */
public class FlameChartDataProviderFactory implements IDataProviderFactory {

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        // Need the analysis
        return null;
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace, String secondaryId) {
        // Create with the trace or experiment first
        ITmfTreeDataProvider<? extends ITmfTreeDataModel> provider = create(trace, secondaryId);
        if (provider != null) {
            return provider;
        }
        // Otherwise, see if it's an experiment and create a composite if that's
        // the case
        Collection<ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        if (traces.size() > 1) {
            // Try creating a composite only if there are many traces,
            // otherwise, the previous call to create should have returned the
            // data provider
            return TmfTimeGraphCompositeDataProvider.create(traces, FlameChartDataProvider.ID, secondaryId);
        }
        return null;
    }

    private static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(ITmfTrace trace, String secondaryId) {
        // The trace can be an experiment, so we need to know if there are
        // multiple analysis modules with the same ID
        Iterable<IFlameChartProvider> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, IFlameChartProvider.class);
        Iterable<IFlameChartProvider> filteredModules = Iterables.filter(modules, m -> m.getId().equals(secondaryId));
        Iterator<IFlameChartProvider> iterator = filteredModules.iterator();
        if (iterator.hasNext()) {
            IFlameChartProvider module = iterator.next();
            if (iterator.hasNext()) {
                // More than one module, must be an experiment, return null so
                // the factory can try with individual traces
                return null;
            }
            module.schedule();
            return new FlameChartDataProvider(trace, module, secondaryId);
        }
        return null;
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        Iterable<IFlameChartProvider> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, IFlameChartProvider.class);
        List<IDataProviderDescriptor> descriptors = new ArrayList<>();
        Set<String> existingModules = new HashSet<>();
        for (IFlameChartProvider module : modules) {
            IAnalysisModule analysis = module;
            // Only add analysis once per trace (which could be an experiment)
            if (!existingModules.contains(analysis.getId())) {
                DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
                builder.setId(FlameChartDataProvider.ID + DataProviderConstants.ID_SEPARATOR + analysis.getId())
                        .setName(Objects.requireNonNull(analysis.getName() + " - " +Messages.FlameChartDataProvider_Title)) //$NON-NLS-1$
                        .setDescription(Objects.requireNonNull(NLS.bind(Messages.FlameChartDataProvider_Description, analysis.getHelpText())))
                        .setProviderType(ProviderType.TIME_GRAPH);
                descriptors.add(builder.build());
                existingModules.add(analysis.getId());
            }
        }
        return descriptors;
    }
}
