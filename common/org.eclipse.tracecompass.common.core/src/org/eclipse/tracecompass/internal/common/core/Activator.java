/*******************************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.common.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.tracecompass.common.core.TraceCompassActivator;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils;

/**
 * Plugin activator
 */
public class Activator extends TraceCompassActivator {

    private static final String PLUGIN_ID = "org.eclipse.tracecompass.common.core"; //$NON-NLS-1$

    /**
     * Return the singleton instance of this activator.
     *
     * @return The singleton instance
     */
    public static Activator instance() {
        return (Activator) TraceCompassActivator.getInstance(PLUGIN_ID);
    }

    /**
     * Constructor
     */
    public Activator() {
        super(PLUGIN_ID);
    }

    @Override
    protected void startActions() {
        /* Trace system specs */
        Logger logger = TraceCompassLog.getLogger(getClass());
        int nbCpus = Runtime.getRuntime().availableProcessors();
        long memory = Runtime.getRuntime().totalMemory();
        String os = System.getProperty("os.name"); //$NON-NLS-1$
        String hostName = "Unknown"; //$NON-NLS-1$
        try {
            InetAddress localMachine = InetAddress.getLocalHost();
            hostName = localMachine.getHostName();
        } catch (UnknownHostException e) {
            // ignore.
        }
        /*
         * Simple benchmark to test floating point math. Should give a decent
         * idea of the system performance. (one multiplication, one division and a branch)
         */
        double data = 1e8 - 7; // random number
        double otherData = 123.321;
        long start = System.nanoTime();
        for (int i = 0; i < 1000000; i++) {
            data /= otherData;
            data *= otherData;
            if (data < 0) {
                // should not happen
                throw new IllegalStateException("Floating point error on CPU!"); //$NON-NLS-1$
            }
        }
        long end = System.nanoTime();
        long bogoMips = (long) (1e12 / (end - start));
        TraceCompassLogUtils.traceInstant(logger, Level.SEVERE, PLUGIN_ID, "HostName", hostName, "OS", os, "Nb Processors", nbCpus, "BogoMips", bogoMips, "Memory", memory); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

    @Override
    protected void stopActions() {
        // Do nothing
    }

}
