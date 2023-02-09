/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.ui.flamegraph;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for the flame graph view
 *
 * @author Sonia Farrah
 */
@NonNullByDefault({})
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    /**
     * The action name for grouping
     */
    public static String FlameGraphView_GroupByName;
    /**
     * The action tooltip for grouping
     */
    public static String FlameGraphView_GroupByTooltip;

    /**
     * The action name for sorting by thread name
     */
    public static String FlameGraph_SortByThreadName;
    /**
     * The action name for sorting by thread id
     */
    public static String FlameGraph_SortByThreadId;

    /**
     * Execution of the callGraph Analysis
     */
    public static String FlameGraphView_RetrievingData;

    /**
     * Symbols provider action text
     */
    public static String FlameGraphView_ConfigureSymbolProvidersText;
    /**
     * Symbols provider action tooltip
     */
    public static String FlameGraphView_ConfigureSymbolProvidersTooltip;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
