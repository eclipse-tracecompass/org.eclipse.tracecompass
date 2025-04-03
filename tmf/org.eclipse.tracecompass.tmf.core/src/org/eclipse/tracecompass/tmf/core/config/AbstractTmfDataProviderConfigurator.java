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
package org.eclipse.tracecompass.tmf.core.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * This class meant to be extended by data provider factories that want to be
 * able to handle configurations.
 *
 * @since 9.6
 */
public abstract class AbstractTmfDataProviderConfigurator implements ITmfDataProviderConfigurator{

    /**
     * Constructor
     */
    protected AbstractTmfDataProviderConfigurator() {
        TmfSignalManager.register(this);
    }

    /**
     * The json file extension
     * @since 9.5
     */
    public static final String JSON_EXTENSION = "json"; //$NON-NLS-1$

    private Table<String, ITmfTrace, ITmfConfiguration> fTmfConfigurationTable = HashBasedTable.create();

    /**
     * @return a table mapping configuration id and trace (exp) to its configuration
     */
    protected Table<String, ITmfTrace, ITmfConfiguration> getConfigurationTable(){
        return fTmfConfigurationTable;
    }

    @Override
    public @NonNull IDataProviderDescriptor createDataProviderDescriptors(ITmfTrace trace, ITmfConfiguration configuration) throws TmfConfigurationException {

        if (configuration.getName().equals(TmfConfiguration.UNKNOWN)) {
            throw new TmfConfigurationException("Missing configuration name"); //$NON-NLS-1$
        }

        if (configuration.getSourceTypeId().equals(TmfConfiguration.UNKNOWN)) {
            throw new TmfConfigurationException("Missing configuration type"); //$NON-NLS-1$
        }

        String description = configuration.getDescription();
        if (configuration.getDescription().equals(TmfConfiguration.UNKNOWN)) {
            description = "Data provider defined by configuration " + configuration.getName(); //$NON-NLS-1$
        }

        TmfConfiguration.Builder builder = new TmfConfiguration.Builder();
        builder.setId(configuration.getId())
               .setSourceTypeId(configuration.getSourceTypeId())
               .setName(configuration.getName())
               .setDescription(description)
               .setParameters(configuration.getParameters())
               .build();

        ITmfConfiguration config = builder.build();

        applyConfiguration(trace, config, true);
        if (fTmfConfigurationTable.contains(config.getId(), trace)) {
            throw new TmfConfigurationException("Configuration already existis with label: " + config.getName()); //$NON-NLS-1$
        }
        fTmfConfigurationTable.put(config.getId(), trace, config);
        return getDescriptorFromConfig(config);
    }

    /**
     * @param config
     *            a configuration
     * @return A data provider descriptor based on the configuration parameter
     */
    protected abstract IDataProviderDescriptor getDescriptorFromConfig(ITmfConfiguration config);

    /**
     * This is the method that handles what happens when a configuration is
     * applied
     *
     * @param trace
     *            trace to which the configuration should be applied
     * @param config
     *            the configuration to be applied
     * @param writeConfig
     *            true if the configuration should be written to disk, false
     *            otherwise
     */
    protected abstract void applyConfiguration(ITmfTrace trace, ITmfConfiguration config, boolean writeConfig);

    @Override
    public void removeDataProviderDescriptor(ITmfTrace trace, IDataProviderDescriptor descriptor) throws TmfConfigurationException {
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
        removeConfiguration(trace, config);
    }

    /**
     * This is the method that handles what happens when a configuration is
     * removed (e.g. remove analysis, dp etc)
     *
     * @param trace
     *            trace to which the configuration should be applied
     * @param config
     *            the configuration to be applied
     */
    protected abstract void removeConfiguration(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration config);

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
                for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                   // Read configurations from sub-trace
                   List<ITmfConfiguration> configs = readConfigurations(tr);
                   readAndApplyConfiguration(trace, configs);
                }
            } else {
                // Read configurations trace
                List<ITmfConfiguration> configs = readConfigurations(trace);
                readAndApplyConfiguration(trace, configs);
            }
       } catch (TmfConfigurationException e) {
           Activator.logError("Error applying configurations for trace " + trace.getName(), e); //$NON-NLS-1$
       }
    }

    /**
     * Handles trace closed signal
     *
     * @param signal
     *            the close signal to handle
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        fTmfConfigurationTable.column(trace).clear();
    }

    private void readAndApplyConfiguration(ITmfTrace trace, List<ITmfConfiguration> configs) throws TmfConfigurationException {
        for (ITmfConfiguration config : configs) {
            if (!fTmfConfigurationTable.contains(config.getId(), trace)) {
                fTmfConfigurationTable.put(config.getId(), trace, config);
                applyConfiguration(trace, config, false);
            }
        }
    }

    /**
     * Reads the configurations for a given trace
     *
     * @param trace
     *            the trace to read configurations from
     * @return list of configurations if any
     * @throws TmfConfigurationException
     *             if an error occurs
     */
    private @NonNull List<ITmfConfiguration> readConfigurations(@NonNull ITmfTrace trace) throws TmfConfigurationException {
        IPath rootPath = getConfigurationRootFolder(trace);
        File folder = rootPath.toFile();
        List<ITmfConfiguration> list = new ArrayList<>();
        if (folder.exists()) {
            File[] listOfFiles = folder.listFiles();
            for (File file : listOfFiles) {
                IPath path = new Path(file.getName());
                if (path.getFileExtension().equals(JSON_EXTENSION)) {
                    ITmfConfiguration config = TmfConfiguration.fromJsonFile(file);
                    list.add(config);
                }
            }
        }
        return list;
    }

    /**
     * Serialize {@link ITmfConfiguration} to JSON file with name configId.json
     *
     * @param configuration
     *            the configuration to serialize
     * @param rootPath
     *            the root path to store the configuration
     * @throws TmfConfigurationException
     *             if an error occurs
     * @since 9.5
     */
    protected static void writeConfiguration(ITmfConfiguration configuration, IPath rootPath) throws TmfConfigurationException {
        IPath supplPath = rootPath;
        File folder = supplPath.toFile();
        if (!folder.exists()) {
            folder.mkdir();
        }
        supplPath = supplPath.addTrailingSeparator().append(configuration.getId()).addFileExtension(JSON_EXTENSION);
        File file = supplPath.toFile();
        try (Writer writer = new FileWriter(file)) {
            writer.append(new Gson().toJson(configuration));
        } catch (IOException | JsonParseException e) {
            Activator.logError(e.getMessage(), e);
            throw new TmfConfigurationException("Error writing configuration.", e); //$NON-NLS-1$
        }
    }

    /**
     * @param trace
     *            the trace to which the configuration should be applied to
     * @return the path where the configuration should be stored
     */
    @SuppressWarnings("null")
    protected @NonNull abstract IPath getConfigurationRootFolder(@NonNull ITmfTrace trace);

    /**
     * Disposes the signal manager
     */
    protected void dispose() {
        TmfSignalManager.dispose();
    }

}
