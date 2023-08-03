/*******************************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.swslatency;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for Sched_Wakeup Sched_switch latency analysis.
 *
 * @author Abdellah Rahmani
 * @since 8.1
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.analysis.os.linux.core.swslatency.messages"; //$NON-NLS-1$

    /** Sched_Wakeup/Sched_switch TID aspect help text */
    public static @Nullable String SegmentAspectHelpText_SWSTid;

    /** Sched_Wakeup/Sched_switch priority aspect help text */
    public static @Nullable String SegmentAspectHelpText_SWSPrio;

    /** Sched_Wakeup/Sched_switch priority aspect name */
    public static @Nullable String SegmentAspectName_SWSPrio;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

    /**
     * Helper method to expose externalized strings as non-null objects.
     */
    static String getMessage(@Nullable String msg) {
        if (msg == null) {
            return StringUtils.EMPTY;
        }
        return msg;
    }
}
