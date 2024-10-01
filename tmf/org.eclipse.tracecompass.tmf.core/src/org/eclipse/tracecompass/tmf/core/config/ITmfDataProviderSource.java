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

import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Interface for creating data providers. Implementers only return the data provider descriptors.
 * Instantiating the actual data provider(s) should be done using {@link DataProviderManager}.
 * @since 9.5
 */
public interface ITmfDataProviderSource extends ITmfConfigurationTypesProvider {
    /**
     * Create a list of data provider descriptors based on input parameters.
     *
     * @param typeId
     *            The Configuration TypeId
     * @param trace
     *            The trace (or experiment) instance
     * @param jsonParameters
     *            The configuration parameters as valid json string
     * @return List of data provider descriptor
     * @throws TmfConfigurationException
     *             if an error occurs
     */
//    List<IDataProviderDescriptor> createDataProviderDescriptors(String typeId, ITmfTrace trace, Map<String, Object> parameters) throws TmfConfigurationException;
    List<IDataProviderDescriptor> createDataProviderDescriptors(String typeId, ITmfTrace trace, String jsonParameters) throws TmfConfigurationException;

    /**
     * Remove a data provider provider that was created based on input parameters.
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
