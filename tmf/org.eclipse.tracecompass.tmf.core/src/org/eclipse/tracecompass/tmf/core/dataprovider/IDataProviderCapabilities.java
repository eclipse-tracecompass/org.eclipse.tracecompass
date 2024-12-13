/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.tmf.core.dataprovider;

/**
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
     * @return {@code true} if this data provider can be delete, else
     *         {@code false}
     */
    boolean canDelete();
}
