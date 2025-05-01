/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.examples.core.data.provider.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * Example data provider factory
 * 
 * @author Bernd Hufmann
 */
public class ExampleConfigurableDataTreeDataProviderFactory implements IDataProviderFactory {
    /** The factory ID */
    public static final String ID = "org.eclipse.tracecompass.examples.nomodulestats.config"; //$NON-NLS-1$

    private ExampleDataTreeDataProviderConfigurator fConfigurator = new ExampleDataTreeDataProviderConfigurator();
    
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName("Simple Event Statistics (all)") //$NON-NLS-1$
            .setDescription("Simple Event statistics all event") //$NON-NLS-1$
            .setProviderType(ProviderType.DATA_TREE)
             // Only for configurators, indicate that this data provider can create derived data providers
            .setCapabilities(new DataProviderCapabilities.Builder().setCanCreate(true).build()) 
            .build();

    @Override
    public @Nullable ITmfDataProvider createDataProvider(@NonNull ITmfTrace trace) {
        if (trace instanceof TmfExperiment) {
            return TmfTreeCompositeDataProvider.create(TmfTraceManager.getTraceSet(trace), ID);
        }
        return new ExampleConfigurableDataTreeDataProvider(trace);
    }

    @Override
    @SuppressWarnings("null")
    public @Nullable ITmfDataProvider createDataProvider(@NonNull ITmfTrace trace, String secondaryId) {
        ITmfConfiguration config = fConfigurator.getConfiguration(trace, secondaryId);
        if (trace instanceof TmfExperiment) {
            List<ExampleConfigurableDataTreeDataProvider> list = TmfTraceManager.getTraceSet(trace)
                .stream()
                .map(tr -> new ExampleConfigurableDataTreeDataProvider(tr, config))
               .toList();
            return new TmfTreeCompositeDataProvider<>(list, ExampleDataTreeDataProviderConfigurator.generateID(secondaryId));
        }
        return new ExampleConfigurableDataTreeDataProvider(trace, config);
    }

    @Override
    public @NonNull Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        List<IDataProviderDescriptor> descriptors = new ArrayList<>();
        descriptors.add(DESCRIPTOR);
        descriptors.addAll(fConfigurator.getDataProviderDescriptors(trace));
        return descriptors;
    }

    @Override
    public <T> @Nullable T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(ExampleDataTreeDataProviderConfigurator.class)) {
            return adapter.cast(fConfigurator);
        }
        return IDataProviderFactory.super.getAdapter(adapter);
    }

    @Override
    public void dispose() {
        fConfigurator.dispose();
    }
}
