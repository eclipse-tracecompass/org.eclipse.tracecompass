/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for the call stack state provider.
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    /** Title of the symbol tooltip key */
    public static @Nullable String FlameChartDataProvider_Symbol;

    /** Title of the thread ID tooltip key */
    public static @Nullable String FlameChartDataProvider_ThreadId;

    /** Title of kernel status rows */
    public static @Nullable String FlameChartDataProvider_KernelStatusTitle;

    /** Title of the dataprovider */
    public static @Nullable String FlameChartDataProvider_Title;

    /** Messages.FlameChartDataProvider_Title */
    public static @Nullable String FlameChartDataProvider_Description;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
