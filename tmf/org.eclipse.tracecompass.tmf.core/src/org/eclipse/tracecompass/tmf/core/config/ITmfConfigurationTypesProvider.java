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

/**
 * Interface to implement for providing a configuration source.
 *
 * @author Bernd Hufmann
 * @since 9.5
 */
public interface ITmfConfigurationTypesProvider {
    /**
     * @return a list of {@link ITmfConfigurationSourceType}
     */
    List<ITmfConfigurationSourceType> getConfigurationSourceTypes();
}
