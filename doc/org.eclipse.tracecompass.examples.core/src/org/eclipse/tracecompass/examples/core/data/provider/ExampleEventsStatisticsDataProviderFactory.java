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

package org.eclipse.tracecompass.examples.core.data.provider;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * Example data provider factory
 */
public class ExampleEventsStatisticsDataProviderFactory implements IDataProviderFactory {
    private static final String ID = "org.eclipse.tracecompass.examples.nomodulestats"; //$NON-NLS-1$

    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName("Simple Event Statistics") //$NON-NLS-1$
            .setDescription("Simple event statistics") //$NON-NLS-1$
            .setProviderType(ProviderType.DATA_TREE)
            .build();

    @Override
    public @Nullable ITmfDataProvider createDataProvider(@NonNull ITmfTrace trace) {
        if (trace instanceof TmfExperiment) {
            return TmfTreeCompositeDataProvider.create(TmfTraceManager.getTraceSet(trace), ID);
        }
        return new ExampleEventsStatisticsDataProvider(trace);
    }

    @SuppressWarnings("null")
    @Override
    public @NonNull Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        return List.of(DESCRIPTOR);
    }
}
