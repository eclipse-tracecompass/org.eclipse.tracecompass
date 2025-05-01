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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.examples.core.Activator;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.component.TmfComponent;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.osgi.framework.Bundle;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Example data provider configurator
 * 
 * @author Bernd Hufamnn
 */
@SuppressWarnings({"nls", "null"})
public class ExampleDataTreeDataProviderConfigurator extends TmfComponent implements ITmfDataProviderConfigurator {
    
    private static final ITmfConfigurationSourceType CONFIG_SOURCE_TYPE;
    private static final String SCHEMA = "schema/example-schema.json";
    
    private Table<String, ITmfTrace, ITmfConfiguration> fTmfConfigurationTable = HashBasedTable.create();
    
    static {
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
        IPath defaultPath = new Path(SCHEMA);
        URL url = FileLocator.find(bundle, defaultPath, null);
        File schemaFile = null;
        try {
            schemaFile = new File(FileLocator.toFileURL(url).toURI());
        } catch (URISyntaxException | IOException e) {
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ("Failed to read schema file: " + SCHEMA + e)));
        }
        CONFIG_SOURCE_TYPE = new TmfConfigurationSourceType.Builder()
                .setId("example.data.provider.config.source.type")
                .setDescription("Example configuration source to demostrate a configurator")
                .setName("Example configuration")
                .setSchemaFile(schemaFile)
                .build();
    }

    /**
     * Constructor
     */
    public ExampleDataTreeDataProviderConfigurator() {
        super("ExampleDataProviderConfigurator");
    }

    @Override
    public @NonNull List<@NonNull ITmfConfigurationSourceType> getConfigurationSourceTypes() {
        // Return one or more configuration source type that this configurator can handle
        return List.of(CONFIG_SOURCE_TYPE);
    }

    @Override
    public @NonNull IDataProviderDescriptor createDataProviderDescriptors(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        // Check if configuration exists
        if (fTmfConfigurationTable.contains(configuration.getId(), trace)) {
            throw new TmfConfigurationException("Configuration already exists with label: " + configuration.getName()); //$NON-NLS-1$
        }
        /* 
         * - Apply configuration
         *   - if needed, create analysis module with configuration and add it to the trace ITmfTrace.addAnalysisModule()
         *   - parse parameters (e.g. JSON parse) in configuration and store data
         * - Write configuration to disk (if it supposed to survive a restart)
         *   - E.g. write it in supplementary directory of the trace (or experiment) or propagate it to supplementary directory of
         *    of each trace in experiment.
         *    - Use TmfConfiguration.writeConfiguration(configuration, null);
         * - Store configuration for this trace in class storage
         */
        fTmfConfigurationTable.put(configuration.getId(), trace, configuration);

        return getDescriptorFromConfig(configuration);
    }

    @Override
    public void removeDataProviderDescriptor(@NonNull ITmfTrace trace, @NonNull IDataProviderDescriptor descriptor) throws TmfConfigurationException {
        // Check if configuration exists
        ITmfConfiguration creationConfiguration = descriptor.getConfiguration();
        if (creationConfiguration == null) {
            throw new TmfConfigurationException("Data provider was not created by a configuration"); //$NON-NLS-1$
        }
        
        String configId = creationConfiguration.getId();
        // Remove configuration from class storage
        ITmfConfiguration config = fTmfConfigurationTable.get(configId, trace);
        if (config == null) {
            return;
        }
        config = fTmfConfigurationTable.remove(configId, trace);

        /*
         * - Remove configuration
         *   - if needed, remove analysis from trace: ITmfAnalysisModule module =(ITmfTrace.removeAnalysisModule())
         *   - Call module.dispose() analysis module
         *   - Call module.clearPeristentData() (if analysis module has persistent data like a state system)
         * - Delete configuration from disk (if it was persisted)
         */
        
    }

    /**
     * Get list of configured descriptors
     * @param trace
     *            the trace
     * @return list of configured descriptors
     */
    public List<IDataProviderDescriptor> getDataProviderDescriptors(ITmfTrace trace) {
        return fTmfConfigurationTable.column(trace).values()
                .stream()
                .map(config -> getDescriptorFromConfig(config))
                .toList();
    }

    // Create descriptors per configuration
    private @NonNull static IDataProviderDescriptor getDescriptorFromConfig(ITmfConfiguration configuration) {
        DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
            return builder.setId(generateID(configuration.getId()))
                   .setConfiguration(configuration)
                   .setParentId(ExampleConfigurableDataTreeDataProviderFactory.ID)
                   .setCapabilities(new DataProviderCapabilities.Builder().setCanDelete(true).build())
                   .setDescription(configuration.getDescription())
                   .setName(configuration.getName())
                   .setProviderType(ProviderType.DATA_TREE)
                   .build();
    }

    /**
     * Gets the configuration for a trace and configId
     * @param trace
     *          the trace
     * @param configId
     *          the configId
     * @return the configuration for a trace and configId
     */
    public @Nullable ITmfConfiguration getConfiguration(ITmfTrace trace, String configId) {
        return fTmfConfigurationTable.get(configId, trace);
    }

    /**
     * Generate data provider ID using a config ID.
     * 
     * @param configId
     *          the config id
     * @return data provider ID using a config ID.
     */
    public static String generateID(String configId) {
        return ExampleConfigurableDataTreeDataProviderFactory.ID + DataProviderConstants.ID_SEPARATOR + configId;
        
    }

    /**
     * Handles trace closed signal to clean configuration table for this trace
     *
     * @param signal
     *            the close signal to handle
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        fTmfConfigurationTable.column(trace).clear();
    }
}
