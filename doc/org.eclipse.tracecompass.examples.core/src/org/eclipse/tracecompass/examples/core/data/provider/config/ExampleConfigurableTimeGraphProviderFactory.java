/**********************************************************************
 * Copyright (c) 2020, 2025 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.examples.core.data.provider.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.examples.core.analysis.config.ExampleConfigurableStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TmfTimeGraphCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * An example of a time graph data provider factory with configurator and analysis module.
 *
 * This factory is also in the developer documentation of Trace Compass. If it is
 * modified here, the doc should also be updated.
 *
 * @author Geneviève Bastien
 * @author Bernd Hufmann
 */
public class ExampleConfigurableTimeGraphProviderFactory implements IDataProviderFactory {

    /** The ID */
    public static final String ID = ExampleConfigurableTimeGraphDataProvider.ID;

    private static final ExampleTimeGraphProviderWithAnalysisConfigurator fConfigurator = new ExampleTimeGraphProviderWithAnalysisConfigurator(); 
    
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName("Example configurable time graph") //$NON-NLS-1$
            .setDescription("This is an example of a configurable time graph data provider using a state system analysis as a source of data") //$NON-NLS-1$
            .setProviderType(ProviderType.TIME_GRAPH)
            .setCapabilities(new DataProviderCapabilities.Builder().setCanCreate(true).build())
            .build();

    @Override
    public @Nullable ITmfDataProvider createDataProvider(ITmfTrace trace) {
        Collection<@NonNull ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        if (traces.size() == 1) {
            return ExampleConfigurableTimeGraphDataProvider.create(trace);
        }
        return TmfTimeGraphCompositeDataProvider.create(traces, ID);
    }

    @SuppressWarnings("null")
    @Override
    public @Nullable ITmfDataProvider createDataProvider(@NonNull ITmfTrace trace, String secondaryId) {
        if (trace instanceof TmfExperiment) {
            List<ExampleConfigurableTimeGraphDataProvider> list = TmfTraceManager.getTraceSet(trace)
                .stream()
                .map(tr -> ExampleConfigurableTimeGraphDataProvider.create(tr, secondaryId))
               .toList();
            return new TmfTimeGraphCompositeDataProvider<>(list, ExampleTimeGraphProviderWithAnalysisConfigurator.generateID(secondaryId));
        }
        return ExampleConfigurableTimeGraphDataProvider.create(trace, secondaryId);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        ExampleConfigurableStateSystemAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, ExampleConfigurableStateSystemAnalysisModule.class, ExampleConfigurableStateSystemAnalysisModule.ID);
        List<IDataProviderDescriptor> descriptors = new ArrayList<>();
        if (module != null) {
            descriptors.add(DESCRIPTOR);
        }
        descriptors.addAll(fConfigurator.getDataProviderDescriptors(trace));
        return descriptors;
    }

    @Override
    public <T> @Nullable T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(ExampleTimeGraphProviderWithAnalysisConfigurator.class)) {
            return adapter.cast(fConfigurator);
        }
        return IDataProviderFactory.super.getAdapter(adapter);
    }
}
