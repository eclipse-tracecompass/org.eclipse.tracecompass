/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.config;

import java.io.File;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface to implement that describes a configuration source.
 *
 * @author Bernd Hufmann
 * @since 9.2
 */
public interface ITmfConfigurationSourceType {

    /**
     * Gets the name of the configuration source type.
     *
     * @return the name of the configuration source type
     */
    String getName();

    /**
     * Gets the ID for of the configuration source type.
     *
     * @return the ID for of the configuration source type.
     */
    String getId();

    /**
     * Gets a short description of this configuration source type.
     *
     * @return a short description of this configuration source type
     */
    String getDescription();

    /**
     * Gets a list of query parameter keys to be passed when creating
     * configuration instance of this type.
     *
     * @return A list of query parameter descriptors to be passed
     */
    List<ITmfConfigParamDescriptor> getConfigParamDescriptors();
    /**
     * A string containing a json-schema describing the query parameters
     * to be passed when creating a configuration instance of this type.
     *
     * Note: Use either {@link #getConfigParamDescriptors()} or {@link #getSchemaFile()}
     *
     * @return A file containing a valid json-schema or null if not used
     * @since 9.5
     */
    default @Nullable File getSchemaFile() {
        return null;
    }
}
