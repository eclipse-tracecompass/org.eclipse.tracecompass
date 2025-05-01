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
import java.util.ArrayList;
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
import org.eclipse.tracecompass.examples.core.analysis.ExampleStateSystemAnalysisModule;
import org.eclipse.tracecompass.examples.core.analysis.config.ExampleConfigurableStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.component.TmfComponent;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.osgi.framework.Bundle;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Example data provider configurator
 * 
 * @author Bernd Hufmann
 */
@SuppressWarnings({"nls", "null"})
public class ExampleTimeGraphProviderWithAnalysisConfigurator extends TmfComponent implements ITmfDataProviderConfigurator {
    
    private static final ITmfConfigurationSourceType CONFIG_SOURCE_TYPE;
    private static final String SCHEMA = "schema/example-schema-cpu.json";
    
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
                .setId("example.time.graph.data.provider.config.source.type")
                .setDescription("Example configuration source to demostrate a configurator")
                .setName("Example configuration")
                .setSchemaFile(schemaFile)
                .build();
    }

    /**
     * Constructor
     */
    public ExampleTimeGraphProviderWithAnalysisConfigurator() {
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
        applyConfiguration(configuration, trace);
        writeConfiguration(configuration, trace);
        fTmfConfigurationTable.put(configuration.getId(), trace, configuration);
        return getDescriptorFromConfig(configuration);
    }

    @Override
    public void removeDataProviderDescriptor(@NonNull ITmfTrace trace, @NonNull IDataProviderDescriptor descriptor) throws TmfConfigurationException {
        ITmfConfiguration creationConfiguration = descriptor.getConfiguration();
        if (creationConfiguration == null) {
            throw new TmfConfigurationException("Data provider was not created by a configuration"); //$NON-NLS-1$
        }
        
        String configId = creationConfiguration.getId();
        ITmfConfiguration config = fTmfConfigurationTable.get(configId, trace);
        if (config == null) {
            return;
        }
        config = fTmfConfigurationTable.remove(configId, trace);
        deleteConfiguration(creationConfiguration, trace);
        removeConfiguration(creationConfiguration, trace);
    }

    private static void applyConfiguration(ITmfConfiguration configuration, ITmfTrace trace) throws TmfConfigurationException {
        // Apply only to each trace in experiment
        if (trace instanceof TmfExperiment) {
            for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                applyConfiguration(configuration, tr);
            }
            return;
        }
        ExampleConfigurableStateSystemAnalysisModule module = new ExampleConfigurableStateSystemAnalysisModule(configuration);
        try {
            // 
            if (module.setTrace(trace)) {
                IAnalysisModule oldModule = trace.addAnalysisModule(module);
                // Sanity check
                if (oldModule != null) {
                    oldModule.dispose();
                    oldModule.clearPersistentData();
                }
            } else {
                // in case analysis module doesn't apply to the trace
                module.dispose();
            }
        } catch (TmfAnalysisException | TmfTraceException e) {
            // Should not happen
            module.dispose();
        }
    }

    private static void writeConfiguration(ITmfConfiguration configuration, ITmfTrace trace) throws TmfConfigurationException {
        IPath path = getConfigurationRootFolder(trace);
        TmfConfiguration.writeConfiguration(configuration, path);
    }

    private static void deleteConfiguration(ITmfConfiguration configuration, ITmfTrace trace) throws TmfConfigurationException {
        IPath traceConfig = getConfigurationRootFolder(trace);
        traceConfig = traceConfig.append(File.separator).append(configuration.getId()).addFileExtension(TmfConfiguration.JSON_EXTENSION);
        File configFile = traceConfig.toFile();
        if ((!configFile.exists()) || !configFile.delete()) {
            throw new TmfConfigurationException("InAndOut configuration file can't be deleted from trace: configId=" + configuration.getId()); //$NON-NLS-1$
        }
    }

    private static void removeConfiguration(ITmfConfiguration configuration, ITmfTrace trace) throws TmfConfigurationException {
        String analysisId = generateID(ExampleStateSystemAnalysisModule.ID, configuration.getId());
        for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
            // Remove and clear persistent data
            try {
                IAnalysisModule module = tr.removeAnalysisModule(analysisId);
                if (module != null) {
                    module.dispose();
                    module.clearPersistentData();
                }
            } catch (TmfTraceException e) {
                throw new TmfConfigurationException("Error removing analysis module from trace: analysis ID=" + analysisId, e); //$NON-NLS-1$
            }
        }
    }

    private static List<ITmfConfiguration> readConfigurations(ITmfTrace trace) throws TmfConfigurationException {
        IPath rootPath = getConfigurationRootFolder(trace);
        File folder = rootPath.toFile();
        List<ITmfConfiguration> list = new ArrayList<>();
        if (folder.exists()) {
            File[] listOfFiles = folder.listFiles();
            for (File file : listOfFiles) {
                IPath path = new Path(file.getName());
                if (path.getFileExtension().equals(TmfConfiguration.JSON_EXTENSION)) {
                    ITmfConfiguration config = TmfConfiguration.fromJsonFile(file);
                    list.add(config);
                }
            }
        }
        return list;
    }
    
    private static IPath getConfigurationRootFolder(ITmfTrace trace) {
        String supplFolder = TmfTraceManager.getSupplementaryFileDir(trace);
        IPath supplPath = new Path(supplFolder);
        return supplPath
                .addTrailingSeparator()
                .append(ExampleConfigurableTimeGraphProviderFactory.ID);
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
    private static IDataProviderDescriptor getDescriptorFromConfig(ITmfConfiguration configuration) {
        DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
            return builder.setId(generateID(generateID(ExampleConfigurableStateSystemAnalysisModule.ID, configuration.getId())))
                   .setConfiguration(configuration)
                   .setParentId(ExampleConfigurableTimeGraphProviderFactory.ID)
                   .setCapabilities(new DataProviderCapabilities.Builder().setCanDelete(true).build())
                   .setDescription(configuration.getDescription())
                   .setName(configuration.getName())
                   .setProviderType(ProviderType.TIME_GRAPH)
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
        return generateID(ExampleConfigurableTimeGraphProviderFactory.ID, configId);
        
    }

    /**
     * Generate data provider ID using a base ID and config ID.
     * 
     * @param baseId
     *          the base id
     * @param configId
     *          the config id
     * @return generated ID
     */
    public static String generateID(String baseId, String configId) {
        if (configId == null) {
            return baseId;
        }
        return baseId + DataProviderConstants.ID_SEPARATOR + configId;
    }

    
    /**
     * Signal handler for opened trace signal. Will populate trace
     * configurations
     *
     * @param signal
     *            the signal to handle
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        if (trace == null) {
            return;
        }
        try {
            if (trace instanceof TmfExperiment) {
                List<ITmfConfiguration> configs = readConfigurations(trace);
                for (ITmfConfiguration config : configs) {
                    if (!fTmfConfigurationTable.contains(config.getId(), trace)) {
                        fTmfConfigurationTable.put(config.getId(), trace, config);
                        applyConfiguration(config, trace);
                    }
                }
            }
        } catch (TmfConfigurationException e) {
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ("Error applying configurations for trace " + trace.getName() + ", exception" + e))); //$NON-NLS-1$
        }
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
