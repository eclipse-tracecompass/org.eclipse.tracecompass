/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.config;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Interface for configuring data providers for given experiment.
 *
 * @since 9.5
 */
public interface ITmfDataProviderConfigSource {

    /**
     * @return {@link ITmfConfigurationSourceType} of this configuration source
     */
    ITmfConfigurationSourceType getConfigurationSourceType();

    /**
     * Check if a this configuration source is applies to a give data provider.
     *
     * @param dataProviderId
     *          the data provider ID
     * @return true if it applies else false
     */
    boolean appliesToDataProvider(String dataProviderId);

    /**
     * Create a list of data provider descriptors base on input parameters.
     *
     * @param parameters
     *            The input parameter
     * @param trace
     *            The trace (or experiment) instance
     * @param srcDataProviderId
     *            The sourceDataProviderId
     * @return a new {@link ITmfConfiguration} if successful
     * @throws TmfConfigurationException
     *             If the creation of the configuration fails
     */
    ITmfConfiguration create(Map<String, Object> parameters, ITmfTrace trace, String srcDataProviderId) throws TmfConfigurationException;

    /**
     * Removes a configuration instance.
     *
     * @param id
     *            The configuration ID of the configuration to remove
     * @param trace
     *            The trace (or experiment) instance
     * @param srcDataProviderId
     *            The sourceDataProviderId
     * @return removed {@link ITmfConfiguration} instance if remove or null if
     *         not found
     */
    @Nullable ITmfConfiguration remove(String id, ITmfTrace trace, String srcDataProviderId);

    /**
     * Dispose the configuration source.
     */
    void dispose();
}
