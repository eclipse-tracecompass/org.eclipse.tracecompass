/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.examples.core;


import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 */
public class Activator extends Plugin {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    /**
     * The shared instance
     */
    private static Activator fPlugin;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor
     */
    public Activator() {
        setDefault(this);
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Returns the plug-in instance.
     *
     * @return the plug-in instance.
     */
    public static Activator getDefault() {
        return fPlugin;
    }

    // Sets plug-in instance
    private static void setDefault(Activator plugin) {
        fPlugin = plugin;
    }

    // ------------------------------------------------------------------------
    // Plugin
    // ------------------------------------------------------------------------

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        setDefault(this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        setDefault(null);
        super.stop(context);
    }
}

