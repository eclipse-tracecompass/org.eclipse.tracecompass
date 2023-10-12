/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.analysis.xml.core.config;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlAnalysisModuleSource;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSource;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;

import com.google.common.collect.ImmutableList;

/**
 * Implementation of {@link ITmfConfigurationSource} for managing data
 * driven-analysis in XML files.
 *
 * @author Bernd Hufmann
 */
public class XmlConfigurationSource implements ITmfConfigurationSource {

    private static final ITmfConfigurationSourceType fType;

    private static final String XML_ANALYSIS_TYPE_ID = "org.eclipse.tracecompass.tmf.core.config.xmlsourcetype"; //$NON-NLS-1$
    private static final String NAME = nullToEmptyString(Messages.XmlConfigurationSource_Name);
    private static final String DESCRIPTION = nullToEmptyString(Messages.XmlConfigurationSource_Description);
    private static final String PATH_KEY = "path"; //$NON-NLS-1$
    private static final String PATH_DESCRIPTION = nullToEmptyString(Messages.XmlConfigurationSource_PathDescription);
    private Map<String, ITmfConfiguration> fConfigurations = new ConcurrentHashMap<>();

    static {
        TmfConfigParamDescriptor.Builder descriptorBuilder = new TmfConfigParamDescriptor.Builder()
                .setKeyName(PATH_KEY)
                .setDescription(PATH_DESCRIPTION);

        fType = new TmfConfigurationSourceType.Builder()
                .setId(XML_ANALYSIS_TYPE_ID)
                .setDescription(DESCRIPTION)
                .setName(NAME)
                .setConfigParamDescriptors(ImmutableList.of(descriptorBuilder.build())).build();
    }

    /**
     * Default Constructor
     */
    @SuppressWarnings("null")
    public XmlConfigurationSource() {
        for (Entry<@NonNull String, @NonNull File> entry : XmlUtils.listFiles().entrySet()) {
            ITmfConfiguration config = createConfiguration(entry.getValue());
            fConfigurations.put(config.getId(), config);
        }
    }

    @Override
    public ITmfConfigurationSourceType getConfigurationSourceType() {
        return fType;
    }

    @Override
    public ITmfConfiguration create(Map<String, Object> parameters) throws TmfConfigurationException {
        return createOrUpdateXml(null, parameters);
    }

    @Override
    public @Nullable ITmfConfiguration get(String id) {
        return fConfigurations.get(id);
    }

    @Override
    public ITmfConfiguration update(String id, Map<String, Object> parameters) throws TmfConfigurationException {
        ITmfConfiguration config = fConfigurations.get(id);
        if (config == null) {
            throw new TmfConfigurationException("No such configuration with ID: " + id); //$NON-NLS-1$
        }
        return createOrUpdateXml(config, parameters);
    }

    @Override
    public @Nullable ITmfConfiguration remove(String id) {
        if (fConfigurations.get(id) == null) {
            return null;
        }

        if (!XmlUtils.listFiles().containsKey(id)) {
            return null;
        }

        XmlUtils.deleteFiles(ImmutableList.of(id));
        XmlUtils.saveFilesStatus();
        XmlAnalysisModuleSource.notifyModuleChange();
        return fConfigurations.remove(id);
    }

    @Override
    public List<ITmfConfiguration> getConfigurations() {
        return ImmutableList.copyOf(fConfigurations.values());
    }

    @Override
    public boolean contains(String id) {
        return fConfigurations.containsKey(id);
    }

    private static @Nullable File getFile(Map<String, Object> parameters) {
        String path = (String) parameters.get(PATH_KEY);
        if (path == null) {
            return null;
        }
        return new File(path);
    }

    private ITmfConfiguration createOrUpdateXml(@Nullable ITmfConfiguration existingConfig, Map<String, Object> parameters) throws TmfConfigurationException {
        File file = getFile(parameters);
        if (file == null) {
            throw new TmfConfigurationException("Missing path"); //$NON-NLS-1$
        }

        ITmfConfiguration config = createConfiguration(file);

        IStatus status = XmlUtils.xmlValidate(file);
        if (status.isOK()) {
            if (existingConfig == null) {
                status = XmlUtils.addXmlFile(file);
            } else {
                if (!existingConfig.getId().equals(config.getId())) {
                    throw new TmfConfigurationException("File mismatch"); //$NON-NLS-1$
                }
                XmlUtils.updateXmlFile(file);
            }
            if (status.isOK()) {
                XmlAnalysisModuleSource.notifyModuleChange();
                XmlUtils.saveFilesStatus();
                fConfigurations.put(config.getId(), config);
                return config;
            }
        }
        String statusMessage = status.getMessage();
        String message = statusMessage != null? statusMessage : "Failed to update xml analysis configuration"; //$NON-NLS-1$
        if (status.getException() != null) {
            throw new TmfConfigurationException(message, status.getException());
        }
        throw new TmfConfigurationException(message);
    }

    @SuppressWarnings("null")
    private static String getName(String file) {
        return new Path(file).removeFileExtension().toString();
    }

    private static ITmfConfiguration createConfiguration(File file) {
        String id = file.getName();
        String name = getName(file.getName());
        String description = NLS.bind(Messages.XmlConfigurationSource_ConfigDescription, name);
        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(name)
                .setId(id)
                .setDescription(description.toString())
                .setSourceTypeId(XML_ANALYSIS_TYPE_ID);
       return builder.build();
    }
}
