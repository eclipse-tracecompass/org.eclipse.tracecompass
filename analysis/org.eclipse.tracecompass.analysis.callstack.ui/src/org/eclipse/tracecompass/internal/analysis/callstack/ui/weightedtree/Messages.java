/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.ui.weightedtree;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the weighted tree view
 *
 * @author Geneviève Bastien
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    /** Text for the 'Other' slice of the pie */
    public static @Nullable String WeightedTreeViewer_Other;
    /** Title of the element column */
    public static @Nullable String WeightedTreeViewer_Element;
    /** Title of the weight column */
    public static @Nullable String WeightedTreeViewer_Weight;
    /** Label for the total column */
    public static @Nullable String WeightedTreeViewer_LabelTotal;
    /** Label for the selection column */
    public static @Nullable String WeightedTreeViewer_LabelSelection;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
