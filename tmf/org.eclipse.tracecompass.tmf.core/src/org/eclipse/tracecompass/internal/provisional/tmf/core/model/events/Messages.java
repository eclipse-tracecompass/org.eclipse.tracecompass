/**********************************************************************
 * Copyright (c) 2019, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.internal.provisional.tmf.core.model.events;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for events data provider messages.
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.messages"; //$NON-NLS-1$
    /**
     * The events table title
     */
    public static @Nullable String EventsTableDataProvider_Title;
    /**
     * The events table data provider help text
     */
    public static @Nullable String EventsTableDataProviderFactory_DescriptionText;
    /**
     * The raw event data title
     */
    public static @Nullable String RawEventsDataProvider_Title;
    /**
     * The raw event data provider help text
     */
    public static @Nullable String RawEventsDataProviderFactory_DescriptionText;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        // Default constructor
    }
}
