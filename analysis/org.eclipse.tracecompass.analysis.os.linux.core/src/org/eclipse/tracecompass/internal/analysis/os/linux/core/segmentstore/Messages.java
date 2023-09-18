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

package org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for the segment store analysis module
 *
 * @author Hoang Thuan Pham
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.messages"; //$NON-NLS-1$

    /**
     * Name of the Priority/Thread name data provider
     */
    public static @Nullable String PriorityThreadNameStatisticsDataProviderFactory_AnalysisName;
    /**
     * Title of the Priority/Thread name data provider
     */
    public static @Nullable String PriorityThreadNameStatisticsDataProviderFactory_title;
    /**
     * Description of the Priority/Thread name data provider
     */
    public static @Nullable String PriorityThreadNameStatisticsDataProviderFactory_description;
    /**
     * The segment type format of the PriorityThreadNameStatisticsAnalysis
     */
    public static @Nullable String PriorityThreadNameStatisticsAnalysis_segmentType;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
