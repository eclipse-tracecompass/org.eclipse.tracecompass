/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Author:
 *     Sonia Farrah
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.ui.flamegraph2;

import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * View to display the flame graph. This uses the flameGraphNode tree generated
 * by CallGraphAnalysisUI.
 *
 * @author Sonia Farrah
 */
public class FlameGraphSelView extends FlameGraphView {

    /**
     * ID of the view
     */
    public static final String SEL_ID = "org.eclipse.tracecompass.analysis.profiling.ui.flamegraph.selection"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public FlameGraphSelView() {
        super(SEL_ID);
    }

    /**
     * Handles the update of the selection. If the selection is a range, then
     * get the call graph for this range and update the view, otherwise get
     * callgraph of the full range
     *
     * @param signal
     *            The signal
     */
    @TmfSignalHandler
    public void handleSelectionChange(TmfSelectionRangeUpdatedSignal signal) {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }
        ITmfTimestamp beginTime = signal.getBeginTime();
        ITmfTimestamp endTime = signal.getEndTime();

        if (beginTime != endTime) {
            buildFlameGraph(trace, beginTime, endTime);
        } else {
            buildFlameGraph(trace, null, null);
        }
    }
}
