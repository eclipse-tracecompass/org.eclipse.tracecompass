/**********************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.tmf.core.model;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for the common status messages
 *
 * @author Yonni Chen
 * @since 4.0
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.tmf.core.model.messages"; //$NON-NLS-1$

    /**
     * Detailed message for running status
     */
    public static @Nullable String CommonStatusMessage_Running;

    /**
     * Detailed message for completed status
     */
    public static @Nullable String CommonStatusMessage_Completed;

    /**
     * Detailed message for cancelled status cause by a progress monitor
     */
    public static @Nullable String CommonStatusMessage_TaskCancelled;

    /**
     * Detailed message for failed status cause by an analysis initialization failure
     */
    public static @Nullable String CommonStatusMessage_AnalysisInitializationFailed;

    /**
     * Detailed message for failed status cause by an analysis initialization failure
     * @since 10.1
     */
    public static @Nullable String CommonStatusMessage_AnalysisExecutionFailed;

    /**
     * Detailed message for failed status cause by trying to access to statesystem
     */
    public static @Nullable String CommonStatusMessage_StateSystemFailed;

    /**
     * Detailed message for failed status cause by incorrect start/end time query
     */
    public static @Nullable String CommonStatusMessage_IncorrectQueryInterval;

    /**
     * Detailed message for failed status cause by incorrect query parameters
     *
     * @since 5.0
     */
    public static @Nullable String CommonStatusMessage_IncorrectQueryParameters;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
