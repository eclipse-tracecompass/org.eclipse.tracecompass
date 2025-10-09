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

package org.eclipse.tracecompass.internal.tmf.core.statesystem.provider;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the state system provider
 *
 * @author Benjamin Saint-Cyr
 * @since 5.0
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.tmf.core.statesystem.provider.messages"; //$NON-NLS-1$

    /** Label for the first column */
    public static @Nullable String TreeNodeColumnLabel;

    /** Label for the "quark" column" */
    public static @Nullable String QuarkColumnLabel;

    /** Label for the "value" column */
    public static @Nullable String ValueColumnLabel;

    /** Label for the "type" column */
    public static @Nullable String TypeColumnLabel;

    /** Label for the "start time" column */
    public static @Nullable String StartTimeColumLabel;

    /** Label for the "end time" column */
    public static @Nullable String EndTimeColumLabel;

    /** Label for the "attribute path" column */
    public static @Nullable String AttributePathColumnLabel;

    /** Data Provider factory title */
    public static @Nullable  String StateSystemDataProviderFactory_title;

    /** Data Provider factory help text */
    public static  @Nullable  String StateSystemDataProviderFactory_descriptionText;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

}

