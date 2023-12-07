/*******************************************************************************
 * Copyright (c) 2011, 2023 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

/**
 * The class for adding logging Utility to the CTF plug-in.
 */
public class CtfCoreLoggerUtil {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * Flag to indicate if platform is running
     */
    public static final boolean IS_PLATFORM_RUNNING;
    private static Logger LOGGER;
    static {
        boolean result = false;
        try {
            result = Platform.isRunning();
        } catch (Exception exception) {
            // Assume that we aren't running.
        }
        IS_PLATFORM_RUNNING = result;
        if (!result) {
            LOGGER = Logger.getLogger(CtfCoreLoggerUtil.class.getName());
        }
    }


    /**
     * The plug-in ID
     */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.ctf.core"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor
     */
    private CtfCoreLoggerUtil() {
        // Do nothing
    }

    // ------------------------------------------------------------------------
    // Logging
    // ------------------------------------------------------------------------

    /**
     * Log a info message
     *
     * @param msg
     *            The message
     */
    public static void logInfo(String msg) {
        if (IS_PLATFORM_RUNNING) {
            Platform.getLog(CtfCoreLoggerUtil.class).log(new Status(IStatus.INFO, PLUGIN_ID, msg));
        } else {
            LOGGER.log(Level.INFO, msg);
        }
    }

    /**
     * Log an error, with an associated exception
     *
     * @param msg
     *            The error message
     * @param e
     *            The cause
     */
    public static void logError(String msg, Exception e) {
        if (IS_PLATFORM_RUNNING) {
            Platform.getLog(CtfCoreLoggerUtil.class).log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, msg, e));
        } else {
            LOGGER.log(Level.SEVERE, msg, e);
        }
    }

    /**
     * Log an error, with an associated exception
     *
     * @param msg
     *            The error message
     */
    public static void logWarning(String msg) {
        if (IS_PLATFORM_RUNNING) {
            Platform.getLog(CtfCoreLoggerUtil.class).log(new Status(IStatus.WARNING, PLUGIN_ID, msg));
        } else {
            LOGGER.log(Level.WARNING, msg);
        }
    }
}
