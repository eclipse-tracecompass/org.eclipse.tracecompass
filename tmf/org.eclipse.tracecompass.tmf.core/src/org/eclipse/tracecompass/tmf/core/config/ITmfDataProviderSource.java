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
public interface ITmfDataProviderSource {
    /**
     * Create a list of data provider descriptors base on input parameters.
     *
     * @param srcDataProviderId
     *            The sourceDataProviderId
     * @param trace
     *            The trace (or experiment) instance
     * @param configId
     *            The configuration ID
     * @return List of data provider descriptor
     * @throws TmfConfigurationException
     *             if an error occurs
     */
    List<IDataProviderDescriptor> getDataProviderDescriptors(String srcDataProviderId, ITmfTrace trace, String configId) throws TmfConfigurationException;
}
