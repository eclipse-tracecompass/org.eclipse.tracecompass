/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.tmf.tests.stubs.model.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSource;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;

import com.google.common.collect.ImmutableList;

/**
 * Configuration Source stub for testing.
 */
@NonNullByDefault
@SuppressWarnings("javadoc")
public class TestConfigurationSource implements ITmfConfigurationSource {

    private static final ITmfConfigurationSourceType fType;
    private int fInstanceId = 0;

    public static final String STUB_ANALYSIS_TYPE_ID = "org.eclipse.tracecompass.tmf.tests.stubs.model.config.testsourcetype"; //$NON-NLS-1$
    public static final String NAME = "Stub Configuration Source"; //$NON-NLS-1$
    public static final String DESCRIPTION = "Sub Configuration Source description"; //$NON-NLS-1$
    public static final String DESCRIPTION_PREFIX = "Stub Configuration: "; //$NON-NLS-1$
    public static final String PATH_KEY = "path"; //$NON-NLS-1$
    public static final String PATH_DESCRIPTION = "path"; //$NON-NLS-1$
    private Map<String, ITmfConfiguration> fConfigurations = new ConcurrentHashMap<>();

    static {
        TmfConfigParamDescriptor.Builder descBuilder = new TmfConfigParamDescriptor.Builder();
        descBuilder.setKeyName(PATH_KEY)
                   .setDescription(PATH_DESCRIPTION);

        fType = new TmfConfigurationSourceType.Builder()
                .setId(STUB_ANALYSIS_TYPE_ID)
                .setDescription(DESCRIPTION)
                .setName(NAME)
                .setConfigParamDescriptors(ImmutableList.of(descBuilder.build())).build();
    }

    @Override
    public ITmfConfigurationSourceType getConfigurationSourceType() {
        return Objects.requireNonNull(fType);
    }

    @Override
    public ITmfConfiguration create(Map<String, Object> parameters) throws TmfConfigurationException {
        String path = (String) parameters.get("path"); //$NON-NLS-1$
        if (path == null) {
            throw new TmfConfigurationException("Missing path parameter");
        }
        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(path)
                .setId(path + fInstanceId++)
                .setDescription(DESCRIPTION_PREFIX + path)
                .setSourceTypeId(STUB_ANALYSIS_TYPE_ID);
        ITmfConfiguration config = builder.build();
        fConfigurations.put(config.getId(), config);
        return config;
    }

    @Override
    public ITmfConfiguration update(String id, Map<String, Object> parameters) throws TmfConfigurationException {
        ITmfConfiguration config = fConfigurations.get(id);
        if (config == null) {
            throw new TmfConfigurationException("Configuration doesn't exist");
        }
        return config;
    }

    @Override
    public @Nullable ITmfConfiguration remove(String id) {
        return fConfigurations.remove(id);
    }

    @Override
    public boolean contains(String id) {
        return fConfigurations.containsKey(id);
    }

    @Override
    public List<ITmfConfiguration> getConfigurations() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable ITmfConfiguration get(String id) {
        return fConfigurations.get(id);
    }
}
