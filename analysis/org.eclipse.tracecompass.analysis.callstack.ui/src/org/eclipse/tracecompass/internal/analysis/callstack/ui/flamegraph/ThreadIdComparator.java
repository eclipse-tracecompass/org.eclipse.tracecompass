/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.ui.flamegraph;

import java.util.Comparator;

import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.FlameChartEntryModel;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.FlameChartEntryModel.EntryType;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * Comparator to compare by thread name.
 *
 * @author Bernd Hufmann
 *
 */
class ThreadIdComparator implements Comparator<ITimeGraphEntry> {

    private static final Comparator<ITimeGraphEntry> INSTANCE = new ThreadIdComparator();

    private ThreadIdComparator() {
        // Nothing to do
    }

    public static Comparator<ITimeGraphEntry> getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
        if (o1 instanceof TimeGraphEntry && o2 instanceof TimeGraphEntry) {
            ITmfTreeDataModel entryModel1 = ((TimeGraphEntry) o1).getEntryModel();
            ITmfTreeDataModel entryModel2 = ((TimeGraphEntry) o2).getEntryModel();
            if (entryModel1 instanceof FlameChartEntryModel && entryModel2 instanceof FlameChartEntryModel) {
                FlameChartEntryModel fcEntry1 = (FlameChartEntryModel) entryModel1;
                FlameChartEntryModel fcEntry2 = (FlameChartEntryModel) entryModel2;
                // If any of the entry is a function, compare their depth
                if (fcEntry1.getEntryType() == EntryType.FUNCTION && fcEntry2.getEntryType() == EntryType.FUNCTION) {
                    return Integer.compare(fcEntry1.getDepth(), fcEntry2.getDepth());
                }
            }
        }
        // Fallback where all entries are equal
        return 0;
    }
}
