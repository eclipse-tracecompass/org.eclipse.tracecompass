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

import java.util.Map;

/**
 * Interface describing a configuration instance.
 *
 * @author Bernd Hufmann
 * @since 9.2
 */
public interface ITmfConfiguration {
    /**
     * @return the name of configuration instance
     */
    String getName();

    /**
     * @return the ID for of the configuration instance.
     */
    String getId();

    /**
     * @return a short description of this configuration instance.
     */
    String getDescription();

    /**
     * @return the configuration source type
     */
    String getSourceTypeId();

    /**
     * @return optional parameters representing the configuration parameters used to create
     * this configuration. This is intended to be used instead of {@link #getJsonParameters()}.
     */
    Map<String, Object> getParameters();

    /**
     * @return optional JSON string representing the configuration parameters used to create
     * this configuration. This is intended to be used instead of {@link #getParameters()}.
     *
     * @since 9.5
     */
    default String getJsonParameters() {
        return ""; //$NON-NLS-1$
    }
}
