/**********************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfTreeXYCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Generalized {@link SegmentStoreDensityDataProvider} factory using secondary
 * ID to identify which segment store provider to build it from.
 *
 * @author Puru Jaiswal
 */
public class SegmentStoreDensityDataProviderFactory implements IDataProviderFactory {

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        return null;
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace, String secondaryId) {

        ITmfTreeDataProvider<? extends ITmfTreeDataModel> provider = SegmentStoreDensityDataProvider.create(trace, secondaryId);

        if (provider != null) {
            return provider;
        }
        // Otherwise, see if it's an experiment and create a composite if that's
        // the case
        Collection<ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        if (traces.size() == 1) {
            return SegmentStoreScatterDataProvider.create(trace, secondaryId);
        }
        return TmfTreeXYCompositeDataProvider.create(traces,
                Objects.requireNonNull(Messages.SegmentStoreDensityDataProvider_title),
                SegmentStoreDensityDataProvider.ID, secondaryId);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        Iterable<ISegmentStoreProvider> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, ISegmentStoreProvider.class);
        List<IDataProviderDescriptor> descriptors = new ArrayList<>();
        Set<String> existingModules = new HashSet<>();
        for (ISegmentStoreProvider module : modules) {
            if (!(module instanceof IAnalysisModule)) {
                continue;
            }
            IAnalysisModule analysis = (IAnalysisModule) module;
            if (!existingModules.contains(analysis.getId())) {
                DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
                builder.setId(SegmentStoreDensityDataProvider.ID + DataProviderConstants.ID_SEPARATOR + analysis.getId())
                        .setParentId(analysis.getConfiguration() != null ? analysis.getId() : null)
                        .setName(Objects.requireNonNull(NLS.bind(Messages.SegmentStoreDensityDataProvider_title, analysis.getName())))
                        .setDescription(Objects.requireNonNull(NLS.bind(Messages.SegmentStoreDensityDataProvider_description, analysis.getHelpText())))
                        .setProviderType(ProviderType.TREE_GENERIC_XY)
                        .setConfiguration(analysis.getConfiguration());
                descriptors.add(builder.build());
                existingModules.add(analysis.getId());
            }
        }
        return descriptors;
    }
}
