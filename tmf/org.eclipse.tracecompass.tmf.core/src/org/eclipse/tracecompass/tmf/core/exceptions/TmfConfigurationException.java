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

package org.eclipse.tracecompass.tmf.core.exceptions;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * TMF configuration related exception
 *
 * @author Bernd Hufmann
 * @since 9.2
 */
@NonNullByDefault
public class TmfConfigurationException extends Exception {

    /**
     * The exception version ID
     */
    private static final long serialVersionUID = -5576008495027333732L;

    /**
     * Constructor
     *
     * @param errMsg
     *            the error message
     */
    public TmfConfigurationException(String errMsg) {
        super(errMsg);
    }

    /**
     * Constructor
     *
     * @param errMsg
     *            the error message
     * @param cause
     *            the error cause (<code>null</code> is permitted which means no
     *            cause is available)
     */
    public TmfConfigurationException(String errMsg, @Nullable Throwable cause) {
        super(errMsg, cause);
    }
}
