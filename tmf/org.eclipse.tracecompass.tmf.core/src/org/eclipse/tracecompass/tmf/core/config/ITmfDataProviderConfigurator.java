/*******************************************************************************
 * Copyright (c) 2024, 2025 Ericsson
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

import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Interface for creating data providers. Implementers only return a data provider descriptor.
 * Instantiating the actual data provider(s) will be done using {@link DataProviderManager}.
 * @since 9.5
 */
public interface ITmfDataProviderConfigurator {

    /**
     * @return a list of {@link ITmfConfigurationSourceType}
     */
    List<ITmfConfigurationSourceType> getConfigurationSourceTypes();

    /**
     * Prepares a data provider based on input parameters and returns its
     * corresponding data provider descriptor.
     * <p>
     * The input configuration instance will have default parameters (e.g. name,
     * description or sourceTypeId) and custom parameters which are described by
     * the corresponding {@link ITmfConfigurationSourceType#getSchemaFile()} or
     * by the list of
     * {@link ITmfConfigurationSourceType#getConfigParamDescriptors()}.
     * <p>
     * The data provider descriptor shall return the parent data provider ID
     * through {@link IDataProviderDescriptor#getParentId()}, as well as the
     * creation configuration through
     * {@link IDataProviderDescriptor#getConfiguration()}.
     * <p>
     * The created data provider's ID and any created analysis module's ID should be
     * appended with {@link DataProviderConstants#CONFIG_SEPARATOR} followed
     * by the configuration ID.
     *
     * @param trace
     *            The trace (or experiment) instance
     * @param configuration
     *            The configuration parameters.
     * @return a data provider descriptor corresponding to the derived data
     *         provider.
     *
     * @throws TmfConfigurationException
     *             if an error occurs
     */
    IDataProviderDescriptor createDataProviderDescriptors(ITmfTrace trace, ITmfConfiguration configuration) throws TmfConfigurationException;

    /**
     * Remove a data provider provider that was created by
     * {@link #createDataProviderDescriptors(ITmfTrace, ITmfConfiguration)}
     *
     * @param trace
     *            The trace (or experiment) instance
     * @param descriptor
     *            The data provider descriptor
     * @throws TmfConfigurationException
     *             if an error occurs
     */
    void removeDataProviderDescriptor(ITmfTrace trace, IDataProviderDescriptor descriptor) throws TmfConfigurationException;
}
