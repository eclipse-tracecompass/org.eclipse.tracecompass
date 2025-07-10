/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.internal.analysis.profiling.core.callstack.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackAnalysis;
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
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.Iterables;

/**
 * {@link CallStackFunctionDensityDataProvider} factory.
 *
 * @author Siwei Zhang
 * @since 2.6
 */
public class CallStackFunctionDensityDataProviderFactory implements IDataProviderFactory {

    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(CallStackFunctionDensityDataProvider.ID)
            .setName(Objects.requireNonNull(Messages.CallStackFunctionDensityDataProviderFactory_title))
            .setDescription(Objects.requireNonNull(Messages.CallStackFunctionDensityDataProviderFactory_descriptionText))
            .setProviderType(ProviderType.TREE_GENERIC_XY)
            .build();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        if (trace instanceof TmfExperiment) {
            @NonNull List<@NonNull CallStackFunctionDensityDataProvider> providers = new ArrayList<>();
            for (ITmfTrace child : TmfTraceManager.getTraceSet(trace)) {
                CallStackFunctionDensityDataProvider provider = createProviderLocal(child);
                if (provider != null) {
                    providers.add(provider);
                }
            }
            if (providers.size() == 1) {
                return providers.get(0);
            }
            if (!providers.isEmpty()) {
                String title = Objects.requireNonNull(Messages.CallStackFunctionDensityDataProviderFactory_title);
                return new TmfTreeXYCompositeDataProvider<>(providers, title, CallStackFunctionDensityDataProvider.ID);
            }
            return null;
        }
        return createProviderLocal(trace);
    }

    private static @Nullable CallStackFunctionDensityDataProvider createProviderLocal(@NonNull ITmfTrace trace) {
        Iterator<CallStackAnalysis> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, CallStackAnalysis.class).iterator();
        while (modules.hasNext()) {
            CallStackAnalysis first = modules.next();
            first.schedule();
            return new CallStackFunctionDensityDataProvider(trace, first);
        }
        return null;
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        Iterable<@NonNull CallStackAnalysis> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, CallStackAnalysis.class);
        return !Iterables.isEmpty(modules) ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }
}
