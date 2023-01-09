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
package org.eclipse.tracecompass.tmf.ui.swtbot.tests.views.eventdensity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtchart.Chart;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.tests.shared.CtfTmfTestTraceUtils;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.views.xychart.XYDataProviderBaseTest;
import org.eclipse.tracecompass.tmf.ui.tests.shared.WaitUtils;
import org.junit.Test;

/**
 * SWTBot test to test EventDensityView
 *
 * @author Bernd Hufmann
 */
public class EventDensityViewTest extends XYDataProviderBaseTest {

    private static final @NonNull String TRACE_NAME = "kernel_vm";
    private static final @NonNull String TOTAL_NAME = "Total";
    private static final @NonNull String EVENT_DENSITY_VIEW_TITLE = "Event Density View";
    private static final @NonNull String MAIN_SERIES_NAME = "kernel_vm/Ungrouped/total";
    private static final @NonNull String EVENT_DENSITY_VIEW_ID = "org.eclipse.tracecompass.tmf.ui.views.eventdensity";

    /**
     * Ensure the data displayed in the chart viewer reflects the tree viewer's
     * selected entries.
     */
    @Test
    public void testManipulatingTreeViewer() {
        Chart chart = getChart();
        assertNotNull(chart);

        SWTBot swtBot = getSWTBotView().bot();
        SWTBotTree treeBot = swtBot.tree();
        WaitUtils.waitUntil(tree -> tree.rowCount() >= 1, treeBot, "The tree viewer did not finish loading.");
        SWTBotTreeItem root = treeBot.getTreeItem(TRACE_NAME);
        assertNotNull(root);
        SWTBotTreeItem totalItem = SWTBotUtils.getTreeItem(swtBot, root, TOTAL_NAME);
        assertNotNull(totalItem);

        // Verify that total item is checked
        WaitUtils.waitUntil(SWTBotTreeItem::isChecked, totalItem, "Total entry was not checked");
        assertTrue(totalItem.isChecked());
        assertFalse(root.isGrayed());
        assertFalse(totalItem.isGrayed());
        WaitUtils.waitUntil(c -> c.getSeriesSet().getSeries().length >= 1, chart, "The data series did not load.");

        // Uncheck a leaf of the tree
        totalItem.uncheck();
        assertFalse(root.isChecked());
        assertFalse(totalItem.isChecked());
        WaitUtils.waitUntil(c -> c.getSeriesSet().getSeries().length == 0, chart,
                "A data series has not been removed.");
    }

    @Override
    protected @NonNull String getMainSeriesName() {
        return MAIN_SERIES_NAME;
    }

    @Override
    protected @NonNull String getTitle() {
        return EVENT_DENSITY_VIEW_TITLE;
    }

    @Override
    protected String getViewID() {
        return EVENT_DENSITY_VIEW_ID;
    }

    @Override
    protected ITmfTrace getTestTrace() {
        return CtfTmfTestTraceUtils.getTrace(CtfTestTrace.KERNEL_VM);
    }

    @Override
    protected void disposeTestTrace() {
        CtfTmfTestTraceUtils.dispose(CtfTestTrace.KERNEL_VM);
    }
}
