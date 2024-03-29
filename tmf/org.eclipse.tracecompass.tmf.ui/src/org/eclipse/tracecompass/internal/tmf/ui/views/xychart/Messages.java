/*******************************************************************************
 * Copyright (c) 2018, 2020 Ericsson, Draeger, Auriga
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.views.xychart;

import org.eclipse.osgi.util.NLS;

/**
 * Messages class
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.tmf.ui.views.xychart.messages"; //$NON-NLS-1$
    /**
     * Title for the "Lock Y Axis" dialog
     */
    public static String TmfChartView_LockYAxis;
    /**
     * Text for the checkbox in the "Lock Y Axis" dialog
     */
    public static String TmfChartView_LockButton;
    /**
     * Label for the lower range input in the "Lock Y Axis" dialog
     */
    public static String TmfChartView_LowerYAxisRange;
    /**
     * Label for the upper range input in the "Lock Y Axis" dialog
     */
    public static String TmfChartView_UpperYAxisRange;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
