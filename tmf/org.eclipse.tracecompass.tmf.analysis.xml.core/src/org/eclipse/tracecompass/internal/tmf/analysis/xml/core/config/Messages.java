/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.analysis.xml.core.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.osgi.util.NLS;

/**
 * XML Configuration message bundle
 */
@NonNullByDefault({})
public class Messages extends NLS {

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$
    /** The name of XML analysis configuration source */
    public static String XmlConfigurationSource_Name;
    /** The description of XML analysis configuration source */
    public static String XmlConfigurationSource_Description;
    /** The name of XML analysis configuration description */
    public static String XmlConfigurationSource_ConfigDescription;
    /** The description of the path parameter*/
    public static String XmlConfigurationSource_PathDescription;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {}
}
