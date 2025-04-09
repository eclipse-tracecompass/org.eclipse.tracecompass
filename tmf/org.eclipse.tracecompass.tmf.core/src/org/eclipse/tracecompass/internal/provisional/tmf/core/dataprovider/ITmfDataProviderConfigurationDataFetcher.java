/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.provisional.tmf.core.dataprovider;

import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Interface for data providers that can fetch custom data based on provider configuration.
 * Data providers that need to return specialized data types (images, reports, etc.)
 * can implement this interface and define the data fetching logic.
 *
 * @author Kaveh Shahedi
 * @since 10.3
 */
public interface ITmfDataProviderConfigurationDataFetcher {

    /**
     * Get the data for a specific trace and configuration of the data provider.
     *
     * @param trace
     *            The trace to get the data for
     * @param configuration
     *            The configuration to get the data for
     * @return The report data
     * @throws Exception
     *             If the data cannot be retrieved
     */
    TmfModelResponse<TmfDataProviderConfigurationDataModel> getData(ITmfTrace trace, ITmfConfiguration configuration) throws Exception;
}
