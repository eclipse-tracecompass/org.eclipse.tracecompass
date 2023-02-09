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

package org.eclipse.tracecompass.internal.analysis.callstack.ui.flamegraph;

import org.eclipse.tracecompass.internal.analysis.callstack.core.flamegraph.DataProviderUtils;
import org.eclipse.tracecompass.internal.analysis.callstack.ui.Activator;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

/**
 * Actions executers for data provider actions
 *
 * @author Geneviève Bastien
 */
public final class DataProviderActionUtils {

    private DataProviderActionUtils() {
        // Nothing to do
    }

    /**
     * Execute an action
     *
     * @param value
     *            The action value
     */
    public static void executeAction(String value) {
        if (value.startsWith(DataProviderUtils.ACTION_GOTO_TIME)) {
            // Goto time or time range action
            String times = value.substring(DataProviderUtils.ACTION_GOTO_TIME.length());
            String[] split = times.split(","); //$NON-NLS-1$
            TmfSignal signal = null;
            try {
                if (split.length == 1) {
                    signal = new TmfSelectionRangeUpdatedSignal(value, TmfTimestamp.fromNanos(Long.parseLong(split[0])));
                } else if (split.length == 2) {
                    signal = new TmfSelectionRangeUpdatedSignal(value, TmfTimestamp.fromNanos(Long.parseLong(split[0])), TmfTimestamp.fromNanos(Long.parseLong(split[1])));
                }
            } catch (NumberFormatException e) {
                Activator.getDefault().logWarning("The goto time action has invalid long values as parameters: " + value); //$NON-NLS-1$
                signal = null;
            }
            if (signal != null) {
                TmfSignalManager.dispatchSignal(signal);
            }
        }
    }
}
