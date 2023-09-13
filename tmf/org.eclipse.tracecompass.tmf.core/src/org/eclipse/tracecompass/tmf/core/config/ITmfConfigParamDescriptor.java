/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.tmf.core.config;

/**
 * Interface to implement to describe a configuration parameter.
 *
 * @since 9.2
 * @author Bernd Hufmann
 */
public interface ITmfConfigParamDescriptor {
    /**
     * The unique name of the key to show to the user
     *
     * @return the key name
     */
    String getKeyName();

    /**
     * The data type string, e.g. use NUMBER for numbers, or STRING as strings
     *
     * @return the key name
     */
    String getDataType();

    /**
     * If parameter needs to in the query parameters or not
     *
     * @return true if required else false
     */
    boolean isRequired();

    /**
     * Optional description that can be shown to the user
     *
     * @return description of this parameter
     */
    String getDescription();
}
