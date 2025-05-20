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

package org.eclipse.tracecompass.tmf.core.config;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;

/**
 * Interface to implement for providing a configuration source.
 *
 * @author Bernd Hufmann
 * @since 9.2
 */
public interface ITmfConfigurationSource {
    /**
     * @return {@link ITmfConfigurationSourceType} of this configuration source
     */
    ITmfConfigurationSourceType getConfigurationSourceType();

    /**
     * Creates a new configuration instance.
     * <p>
     * The parameters to be provided are described by the
     * {@link ITmfConfigurationSourceType#getSchemaFile()} or by the list of
     * {@link ITmfConfigurationSourceType#getConfigParamDescriptors()}.
     *
     * @param parameters
     *            The query parameters used to create a configuration instance.
     * @return a new {@link ITmfConfiguration} if successful
     * @throws TmfConfigurationException
     *             If the creation of the configuration fails
     * @deprecated As of version 10.0, use {@link #create(ITmfConfiguration)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    ITmfConfiguration create(Map<String, Object> parameters) throws TmfConfigurationException;

    /**
     * Creates a new configuration instance.
     * <p>
     * The input configuration instance will have default parameters (e.g. name,
     * description or sourceTypeId) and custom parameters which are described by
     * the corresponding {@link ITmfConfigurationSourceType#getSchemaFile()} or
     * by the list of
     * {@link ITmfConfigurationSourceType#getConfigParamDescriptors()}.
     *
     * @param configuration
     *            The input configuration parameters to create a configuration instance.
     * @return a new {@link ITmfConfiguration} if successful
     * @throws TmfConfigurationException
     *             If the creation of the configuration fails
     * @since 9.5
     */
    default ITmfConfiguration create(ITmfConfiguration configuration) throws TmfConfigurationException {
        return create(configuration.getParameters());
    }

    /**
     * Updates a configuration instance.
     * <p>
     * The parameters to be provided are described by the
     * {@link ITmfConfigurationSourceType#getSchemaFile()} or by the list of
     * {@link ITmfConfigurationSourceType#getConfigParamDescriptors()}.
     *
     * @param id
     *            The configuration ID of the configuration to update
     * @param parameters
     *            The query parameters used to update a configuration instance
     * @return a new {@link ITmfConfiguration} if successful
     * @throws TmfConfigurationException
     *             If the update of the configuration fails
     * @deprecated As of version 10.0, use {@link #update(String, ITmfConfiguration)} instead
     */
    @Deprecated(since = "10.0", forRemoval = true)
    ITmfConfiguration update(String id, Map<String, Object> parameters) throws TmfConfigurationException;

    /**
     * Updates a configuration instance.
     * <p>
     * The input configuration instance will have default parameters (e.g. name,
     * description or sourceTypeId) and custom parameters which are described by
     * the corresponding {@link ITmfConfigurationSourceType#getSchemaFile()} or
     * by the list of
     * {@link ITmfConfigurationSourceType#getConfigParamDescriptors()}.
     *
     * @param id
     *            The configuration ID of the configuration to update
     * @param configuration
     *            The input configuration parameters to update a configuration
     *            instance.
     * @return a new {@link ITmfConfiguration} if successful
     * @throws TmfConfigurationException
     *             If the update of the configuration fails
     * @since 9.5
     */
    default ITmfConfiguration update(String id, ITmfConfiguration configuration) throws TmfConfigurationException {
        return update(id, configuration.getParameters());
    }

    /**
     * Gets a configuration instance.
     *
     * @param id
     *            The configuration ID of the configuration to remove
     * @return {@link ITmfConfiguration} instance or null if not found
     */
    @Nullable ITmfConfiguration get(String id);

    /**
     * Removes a configuration instance.
     *
     * @param id
     *            The configuration ID of the configuration to remove
     * @return removed {@link ITmfConfiguration} instance if remove or null if
     *         not found
     */
    @Nullable ITmfConfiguration remove(String id);

    /**
     * Checks if configuration instance exists.
     *
     * @param id
     *            The configuration ID of the configuration to check
     * @return true if it exists else false
     */
    boolean contains(String id);

    /**
     * Gets all configuration instances
     *
     * @return list of all configuration instances
     */
    List<ITmfConfiguration> getConfigurations();

    /**
     * Dispose the configuration source.
     */
    void dispose();
}
