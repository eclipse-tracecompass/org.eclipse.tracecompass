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

import java.util.List;
import java.util.Map;

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
     *
     * The data provider descriptor shall return the parent data provider ID
     * through {@link IDataProviderDescriptor#getParentId()}, as well as the
     * creation configuration through
     * {@link IDataProviderDescriptor#getConfiguration()}.
     *
     * @param typeId
     *            The Configuration type ID specified in corresponding
     *            {@link ITmfConfigurationSourceType}
     * @param trace
     *            The trace (or experiment) instance
     * @param parameters
     *            The configuration parameters.
     * @return a data provider descriptor corresponding to the derived data
     *         provider.
     *
     * @throws TmfConfigurationException
     *             if an error occurs
     */
    IDataProviderDescriptor createDataProviderDescriptors(String typeId, ITmfTrace trace, Map<String, Object> parameters) throws TmfConfigurationException;

    /**
     * Remove a data provider provider that was created by
     * {@link #createDataProviderDescriptors(String, ITmfTrace, Map)}
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
