/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.tmf.core.dataprovider;

import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;

/**
 * Interface to implement to indicate capabilities of a data provider, such as
 * "canCreate" and "canDelete" capability.
 *
 * "canCreate" indicates that a given data provider can create a derived data
 * provider. "canDelete" indicates that a given data provider can be deleted.
 *
 * Call method {@link IDataProviderFactory#getAdapter(Class)} with class
 * {@link ITmfDataProviderConfigurator} to obtain an instance of
 * {@link ITmfDataProviderConfigurator}, which implements the "canCreate" and
 * "canDelete" capabilities.
 *
 * @since 9.5
 * @author Bernd Hufmann
 */
public interface IDataProviderCapabilities {
    /**
     * Whether the data provider can create derived data providers.
     *
     * @return {@code true} if this data provider can create a derived data
     *         provider, else {@code false}
     */
    boolean canCreate();

    /**
     * Whether the data provider can be deleted.
     *
     * @return {@code true} if this data provider can be deleted, else
     *         {@code false}
     */
    boolean canDelete();
}
