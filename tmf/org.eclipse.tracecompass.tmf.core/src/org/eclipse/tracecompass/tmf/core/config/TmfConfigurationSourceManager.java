/*******************************************************************************
 * Copyright (c) 2023, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.tmf.core.config;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.Activator;

import com.google.common.collect.ImmutableList;

/**
 * Manager of the org.eclipse.tracecompass.tmf.core.config extension point.
 *
 * @since 9.2
 * @author Bernd Hufmann
 */
public class TmfConfigurationSourceManager {

    /** Extension point ID */
    private static final String CONFIG_EXTENSION_POINT_ID = "org.eclipse.tracecompass.tmf.core.config"; //$NON-NLS-1$

    /** Extension point element 'source' */
    private static final String SOURCE_TYPE_ELEM = "source"; //$NON-NLS-1$

    /** Extension point element 'class' */
    private static final String SOURCE_ATTR = "class"; //$NON-NLS-1$

    /** Extension point element 'dpSource'*/
    private static final String DP_SOURCE_TYPE_ELEM = "dpSource"; //$NON-NLS-1$

    private Map<ITmfConfigurationSourceType, ITmfConfigurationSource> fGlobalDescriptors = new ConcurrentHashMap<>();

    private Map<ITmfConfigurationSourceType, ITmfDataProviderConfigSource> fDpDescriptors = new ConcurrentHashMap<>();

    private static @Nullable TmfConfigurationSourceManager fInstance;

    private TmfConfigurationSourceManager() {
    }

    /**
     * Get the configuration type manager singleton instance.
     *
     * @return the {@link TmfConfigurationSourceManager} instance
     */
    public synchronized static TmfConfigurationSourceManager getInstance() {
        TmfConfigurationSourceManager instance = fInstance;
        if (instance == null) {
            instance = new TmfConfigurationSourceManager();
            instance.init();
            fInstance = instance;
        }
        return instance;
    }

    /**
     * Disposes the instance.
     */
    public synchronized void dispose() {
        fGlobalDescriptors.values().forEach(t -> t.dispose());
        fGlobalDescriptors.clear();

        fDpDescriptors.values().forEach(t -> t.dispose());
        fDpDescriptors.clear();
    }

    /**
     * Gets a list of all available configuration source types
     *
     * @return a list of all available {@Link ITmfConfigurationSourceType}s
     */
    public List<ITmfConfigurationSourceType> getConfigurationSourceTypes() {
        ImmutableList.Builder<ITmfConfigurationSourceType> builder = new ImmutableList.Builder<>();
        builder.addAll(fGlobalDescriptors.keySet());
        return builder.build();
    }

    /**
     * Gets a list of available configuration source types for a given data
     * provider
     *
     * @param dataProviderId
     *            a data provider ID
     * @return a list of available {@Link ITmfConfigurationSourceType}s for a
     *         given data provider
     * @since 9.4
     */
    public List<ITmfConfigurationSourceType> getConfigurationSourceTypes(String dataProviderId) {
        ImmutableList.Builder<ITmfConfigurationSourceType> builder = new ImmutableList.Builder<>();
        @SuppressWarnings("null")
        Map<ITmfConfigurationSourceType, ITmfDataProviderConfigSource> filteredMap =
                fDpDescriptors.entrySet()
                .stream()
                .filter(entry -> entry.getValue().appliesToDataProvider(dataProviderId))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        builder.addAll(filteredMap.keySet());
        return builder.build();
    }

    /**
     * Gets the {@link ITmfConfigurationSource} for a given
     * {@Link ITmfConfigurationSourceType@getId()}
     *
     * @param typeId
     *            The configuration source type ID
     * @return Gets the {@link ITmfConfigurationSource} or null if it doesn't
     *         exist
     */
    public @Nullable ITmfConfigurationSource getConfigurationSource(@Nullable String typeId) {
        ITmfConfigurationSourceType desc = getDescriptor(fGlobalDescriptors, typeId);
        return desc == null ? null : fGlobalDescriptors.get(desc);
    }

    /**
     * Gets the {@link ITmfDataProviderConfigSource} for a given
     * {@Link ITmfConfigurationSourceType#getId()}
     *
     * @param typeId
     *            The configuration source type ID
     * @return Gets the {@link ITmfDataProviderConfigSource} or null if it doesn't
     *         exist
     * @since 9.4
     */
    public @Nullable ITmfDataProviderConfigSource getDataProviderConfigurationSource(@Nullable String typeId) {
        ITmfConfigurationSourceType desc = getDescriptor(fDpDescriptors, typeId);
        return desc == null ? null : fDpDescriptors.get(desc);
    }

    private static @Nullable ITmfConfigurationSourceType getDescriptor(Map<ITmfConfigurationSourceType, ?> map, @Nullable String typeId) {
        if (typeId == null) {
            return null;
        }
        Optional<ITmfConfigurationSourceType> optional = map.keySet().stream().filter(desc -> desc.getId().equals(typeId)).findAny();
        return optional.isEmpty() ? null : optional.get();
    }

    private void init() {
        // Populate the Categories and Trace Types
        IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(CONFIG_EXTENSION_POINT_ID);
        for (IConfigurationElement ce : config) {
            String elementName = ce.getName();
            if (elementName.equals(SOURCE_TYPE_ELEM)) {
                String source = ce.getAttribute(SOURCE_ATTR);
                if (source != null) {
                    ITmfConfigurationSource sourceInstance = null;
                    try {
                        sourceInstance = (ITmfConfigurationSource) ce.createExecutableExtension(SOURCE_ATTR);
                    } catch (CoreException e) {
                        Activator.logError("ITmfConfigurationSource cannot be instantiated.", e); //$NON-NLS-1$
                    }
                    if (sourceInstance != null) {
                        fGlobalDescriptors.put(sourceInstance.getConfigurationSourceType(), sourceInstance);
                    }
                }
            } else if (elementName.equals(DP_SOURCE_TYPE_ELEM)) {
                String source = ce.getAttribute(SOURCE_ATTR);
                if (source != null) {
                    ITmfDataProviderConfigSource sourceInstance = null;
                    try {
                        sourceInstance = (ITmfDataProviderConfigSource) ce.createExecutableExtension(SOURCE_ATTR);
                    } catch (CoreException e) {
                        Activator.logError("ITmfDataProviderConfigSource cannot be instantiated.", e); //$NON-NLS-1$
                    }
                    if (sourceInstance != null) {
                        fDpDescriptors.put(sourceInstance.getConfigurationSourceType(), sourceInstance);
                    }
                }
            }
        }
    }
}
