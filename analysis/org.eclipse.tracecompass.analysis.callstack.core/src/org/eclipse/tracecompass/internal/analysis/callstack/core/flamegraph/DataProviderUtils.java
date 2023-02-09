/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.flamegraph;

/**
 * Utility class to deal with data providers additional functionalities, like
 * actions to execute on certain entries and states.
 *
 * @author Geneviève Bastien
 */
public final class DataProviderUtils {

    /**
     * The prefix character indicating an action tooltip
     */
    public static final String ACTION_PREFIX = "#"; //$NON-NLS-1$

    /**
     * The string indicating an action to go to time or range, it should be
     * followed by a comma-separated list of long values representing the
     * timestamp to go to.
     */
    public static final String ACTION_GOTO_TIME = "TIME:"; //$NON-NLS-1$

    private DataProviderUtils() {
        // Private constructor
    }

    /**
     * Create an action string that means a goto time
     *
     * @param time
     *            The time to go to
     * @return The action string
     */
    public static String createGoToTimeAction(long time) {
        return ACTION_GOTO_TIME + time;
    }

    /**
     * Create an action string that means a goto time range
     *
     * @param time1
     *            The beginning of the time range to go to
     * @param time2
     *            The end of the time range to go to
     * @return The action string
     */
    public static String createGoToTimeAction(long time1, long time2) {
        return ACTION_GOTO_TIME + time1 + ',' + time2;
    }
}
