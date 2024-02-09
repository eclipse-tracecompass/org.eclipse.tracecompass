/*******************************************************************************
 * Copyright (c) 2015, 2021 Ericsson and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.kernel.ui.swtbot.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.matchers.WidgetOfType;
import org.eclipse.swtbot.swt.finder.results.IntResult;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCanvas;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.tracecompass.ctf.core.tests.shared.LttngTraceGenerator;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.resources.ResourcesView;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotTimeGraph;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphMarkerAxis;
import org.eclipse.ui.IWorkbenchPart;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * SWTBot tests for Resources view
 *
 * @author Patrick Tasse
 */
public class ResourcesViewTest extends KernelTimeGraphViewTestBase {

    private static final String CHECK_SELECTED = "Check selected";
    private static final String CHECK_ALL = "Check all";
    private static final String CHECK_SUBTREE = "Check subtree";
    private static final String UNCHECK_SELECTED = "Uncheck selected";
    private static final String UNCHECK_ALL = "Uncheck all";
    private static final String UNCHECK_SUBTREE = "Uncheck subtree";

    private static final String NEXT_MARKER = "Next Marker";
    private static final String PREVIOUS_MARKER = "Previous Marker";
    private static final String SELECT_NEXT_STATE_CHANGE = "Select Next State Change";
    private static final String SELECT_PREVIOUS_STATE_CHANGE = "Select Previous State Change";
    private static final String ADD_BOOKMARK = "Add Bookmark...";
    private static final String REMOVE_BOOKMARK = "Remove Bookmark";
    private static final String ADD_BOOKMARK_DIALOG = "Add Bookmark";
    private static final String HIDE_EMPTY_ROWS = "Hide Empty Rows";
    private static final String LOST_EVENTS = "Lost Events";
    private static final String OK = "OK";

    private static final String PIN_VIEW = "Pin View";
    private static final String RESET_THE_TIME_SCALE_TO_DEFAULT = "Reset the Time Scale to Default";
    private static final String SELECT_NEXT_RESOURCE = "Select Next Resource";
    private static final String SELECT_PREVIOUS_RESOURCE = "Select Previous Resource";
    private static final String SHOW_LEGEND = "Show Legend";
    private static final String SHOW_VIEW_FILTERS = "Show View Filters";
    private static final String ZOOM_IN = "Zoom In";
    private static final String ZOOM_OUT = "Zoom Out";

    private static final @NonNull ITmfTimestamp LOST_EVENT_TIME1 = TmfTimestamp.fromNanos(1368000272697356476L);
    private static final @NonNull ITmfTimestamp LOST_EVENT_END1 = TmfTimestamp.fromNanos(1368000272703627994L);
    private static final @NonNull ITmfTimestamp LOST_EVENT_TIME2 = TmfTimestamp.fromNanos(1368000272728168642L);
    private static final @NonNull ITmfTimestamp LOST_EVENT_END2 = TmfTimestamp.fromNanos(1368000272739505272L);
    private static final @NonNull ITmfTimestamp LOST_EVENT_TIME3 = TmfTimestamp.fromNanos(1368000272759154310L);
    private static final @NonNull ITmfTimestamp LOST_EVENT_END3 = TmfTimestamp.fromNanos(1368000272760559004L);
    private static final @NonNull ITmfTimestamp CPU0_TIME1 = TmfTimestamp.fromNanos(1368000272651208498L);
    private static final @NonNull ITmfTimestamp CPU0_TIME2 = TmfTimestamp.fromNanos(1368000272651853000L);
    private static final @NonNull ITmfTimestamp CPU0_TIME3 = TmfTimestamp.fromNanos(1368000272652067834L);
    private static final @NonNull ITmfTimestamp CPU0_TIME4 = TmfTimestamp.fromNanos(1368000272652282668L);
    private static final @NonNull ITmfTimestamp CPU0_TIME5 = TmfTimestamp.fromNanos(1368000272652497502L);
    private static final @NonNull ITmfTimestamp START_FOR_EMPTY_ROWS_TEST = TmfTimestamp.fromNanos(1368000272651423332L);
    private static final @NonNull ITmfTimestamp END_FOR_EMPTY_ROWS_TEST = TmfTimestamp.fromNanos(1368000272651836454L);

    private static final String CPU0_THREADS = "CPU 0 Threads";
    private static final int TOP_MARGIN = 1;
    private static final Point TOGGLE_SIZE = new Point(7, 8);
    private static final Point HIDE_SIZE = new Point(16, 16);

    @Override
    protected SWTBotView getViewBot() {
        return fBot.viewById(ResourcesView.ID);
    }

    @Override
    protected List<String> getLegendValues() {
        return Arrays.asList("Idle", "Usermode", "System call", "Interrupt", "Soft Irq", "Soft Irq raised");
    }

    @Override
    protected SWTBotView openView() {
        SWTBotUtils.openView(ResourcesView.ID);
        return getViewBot();
    }

    @Override
    protected List<String> getToolbarTooltips() {
        return Arrays.asList(HIDE_EMPTY_ROWS, SHOW_VIEW_FILTERS, SHOW_LEGEND, SEPARATOR,
                RESET_THE_TIME_SCALE_TO_DEFAULT, SELECT_PREVIOUS_STATE_CHANGE, SELECT_NEXT_STATE_CHANGE, SEPARATOR,
                ADD_BOOKMARK, PREVIOUS_MARKER, NEXT_MARKER, SEPARATOR,
                SELECT_PREVIOUS_RESOURCE, SELECT_NEXT_RESOURCE, ZOOM_IN, ZOOM_OUT, SEPARATOR,
                PIN_VIEW);
    }

    /**
     * Before Test
     */
    @Override
    @Before
    public void before() {
        SWTBotView viewBot = getViewBot();
        viewBot.show();
        super.before();
        viewBot.setFocus();
    }

    /**
     * Test keyboard marker navigation using '.' and ','
     */
    @Test
    public void testKeyboardSelectNextPreviousMarker() {
        SWTBotView viewBot = getViewBot();
        SWTBotCanvas canvas = viewBot.bot().canvas(1);
        testNextPreviousMarker(
                () -> {
                    canvas.setFocus();
                    canvas.pressShortcut(KeyStroke.getInstance('.'));
                },
                () -> {
                    canvas.setFocus();
                    canvas.pressShortcut(Keystrokes.SHIFT, KeyStroke.getInstance('.'));
                },
                () -> {
                    canvas.setFocus();
                    canvas.pressShortcut(KeyStroke.getInstance(','));
                },
                () -> {
                    canvas.setFocus();
                    canvas.pressShortcut(Keystrokes.SHIFT, KeyStroke.getInstance(','));
                });
    }

    /**
     * Test tool bar buttons "Next Marker" and "Previous Marker"
     */
    @Test
    public void testToolBarSelectNextPreviousMarker() {
        SWTBotView viewBot = getViewBot();
        testNextPreviousMarker(
                () -> viewBot.toolbarButton(NEXT_MARKER).click(),
                () -> viewBot.toolbarButton(NEXT_MARKER).click(SWT.SHIFT),
                () -> viewBot.toolbarButton(PREVIOUS_MARKER).click(),
                () -> viewBot.toolbarButton(PREVIOUS_MARKER).click(SWT.SHIFT));
    }

    private void testNextPreviousMarker(Runnable nextMarker, Runnable shiftNextMarker, Runnable previousMarker, Runnable shiftPreviousMarker) {
        SWTBotView viewBot = getViewBot();
        /* set selection to trace start time */
        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, START_TIME));
        timeGraphIsReadyCondition(new TmfTimeRange(START_TIME, START_TIME), START_TIME);

        /* set focus on time graph */
        SWTBotTimeGraph timeGraph = new SWTBotTimeGraph(viewBot.bot());
        timeGraph.setFocus();

        /* select first item */
        timeGraph.getEntry(LttngTraceGenerator.getName()).select();

        /* click "Next Marker" 3 times */
        nextMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME1, LOST_EVENT_END1), LOST_EVENT_TIME1);
        nextMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME2, LOST_EVENT_END2), LOST_EVENT_TIME2);
        nextMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME3, LOST_EVENT_END3), LOST_EVENT_TIME3);

        /* shift-click "Previous Marker" 3 times */
        shiftPreviousMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME3, LOST_EVENT_TIME3), LOST_EVENT_TIME3);
        shiftPreviousMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME3, LOST_EVENT_TIME2), LOST_EVENT_TIME2);
        shiftPreviousMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME3, LOST_EVENT_TIME1), LOST_EVENT_TIME1);

        /* shift-click "Next Marker" 3 times */
        shiftNextMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME3, LOST_EVENT_END1), LOST_EVENT_END1);
        shiftNextMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME3, LOST_EVENT_END2), LOST_EVENT_END2);
        shiftNextMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME3, LOST_EVENT_END3), LOST_EVENT_END3);

        /* click "Previous Marker" 3 times */
        previousMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME2, LOST_EVENT_END2), LOST_EVENT_TIME2);
        previousMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME1, LOST_EVENT_END1), LOST_EVENT_TIME1);
        previousMarker.run();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME1, LOST_EVENT_END1), LOST_EVENT_TIME1);
    }

    /**
     * Test "Show Markers" view menu
     */
    @Test
    public void testShowMarkers() {
        SWTBotView viewBot = getViewBot();
        /* set selection to trace start time */
        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, START_TIME));
        timeGraphIsReadyCondition(new TmfTimeRange(START_TIME, START_TIME), START_TIME);

        /* set focus on time graph */
        SWTBotTimeGraph timeGraph = new SWTBotTimeGraph(viewBot.bot());
        timeGraph.setFocus();

        /* select first item */
        timeGraph.getEntry(LttngTraceGenerator.getName()).select();

        assertTrue(viewBot.viewMenu(LOST_EVENTS).isChecked());

        /* check that "Next Marker" and "Previous Marker" are enabled */
        SWTBotUtils.waitUntil(button -> button.isEnabled(), viewBot.toolbarButton(NEXT_MARKER), () -> "NEXT_MARKER is not enabled");
        SWTBotUtils.waitUntil(button -> button.isEnabled(), viewBot.toolbarButton(PREVIOUS_MARKER), () -> "PREVIOUS_MARKER is not enabled");

        /* disable Lost Events markers */
        viewBot.viewMenu(LOST_EVENTS).click();

        /* check that "Next Marker" and "Previous Marker" are disabled */
        SWTBotUtils.waitUntil(button -> !button.isEnabled(), viewBot.toolbarButton(NEXT_MARKER), () -> "NEXT_MARKER is enabled");
        SWTBotUtils.waitUntil(button -> !button.isEnabled(), viewBot.toolbarButton(PREVIOUS_MARKER), () -> "PREVIOUS_MARKER is enabled");

        /* enable Lost Events markers */
        viewBot.viewMenu(LOST_EVENTS).click();

        timeGraphIsReadyCondition(new TmfTimeRange(START_TIME, START_TIME), START_TIME);

        /* check that "Next Marker" and "Previous Marker" are enabled */
        SWTBotUtils.waitUntil(button -> button.isEnabled(), viewBot.toolbarButton(NEXT_MARKER), () -> "NEXT_MARKER is not enabled");
        SWTBotUtils.waitUntil(button -> button.isEnabled(), viewBot.toolbarButton(PREVIOUS_MARKER), () -> "PREVIOUS_MARKER is not enabled");
    }

    /**
     * Test "Show Empty Rows" view menu
     */
    @Test
    public void testShowRows() {
        SWTBotView viewBot = getViewBot();
        /* change window range to 10 ms */
        TmfTimeRange range = new TmfTimeRange(START_FOR_EMPTY_ROWS_TEST, END_FOR_EMPTY_ROWS_TEST);
        TmfSignalManager.dispatchSignal(new TmfWindowRangeUpdatedSignal(this, range));

        IWorkbenchPart part = viewBot.getViewReference().getPart(false);
        assertTrue(part instanceof AbstractTimeGraphView);
        AbstractTimeGraphView abstractTimeGraphView = (AbstractTimeGraphView) part;
        viewBot.bot().waitUntil(ConditionHelpers.timeGraphRangeCondition(abstractTimeGraphView, Objects.requireNonNull(TmfTraceManager.getInstance().getActiveTrace()), range));
        fBot.waitUntil(ConditionHelpers.windowRange(range));

        SWTBotTimeGraph timeGraph = new SWTBotTimeGraph(viewBot.bot());
        timeGraph.setFocus();

        int count = getVisibleItems(timeGraph);

        viewBot.toolbarButton(HIDE_EMPTY_ROWS).click();

        /* Verify that number active entries changed */
        SWTBotUtils.waitUntil(graph -> getVisibleItems(graph) < count, timeGraph,
                () -> "Fewer number of visible entries expected: (count: " + count + ", actual: " + getVisibleItems(timeGraph) + ")");

        timeGraph.setFocus();
        viewBot.toolbarButton(HIDE_EMPTY_ROWS).click();

        /* Verify that number active entries changed */
        SWTBotUtils.waitUntil(graph -> getVisibleItems(graph) == count, timeGraph,
                () -> "More number of visible entries expected: (count: " + count + ", actual: " + getVisibleItems(timeGraph) + ")");
    }

    /**
     * Test "Next Event" tool bar button sub-menu
     */
    /* SWTBot doesn't support clicking the same tool bar sub-menu twice */
    @Ignore
    @Test
    public void testMarkerNavigationSubMenu() {
        SWTBotView viewBot = getViewBot();
        /* set selection to trace start time */
        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, START_TIME));
        timeGraphIsReadyCondition(new TmfTimeRange(START_TIME, START_TIME), START_TIME);

        /* set focus on time graph */
        SWTBotTimeGraph timeGraph = new SWTBotTimeGraph(viewBot.bot());
        timeGraph.setFocus();

        /* select first item */
        timeGraph.getEntry(LttngTraceGenerator.getName()).select();

        /* disable Lost Events navigation */
        viewBot.toolbarDropDownButton(NEXT_MARKER).menuItem(LOST_EVENTS).click();

        /* click "Next Marker" */
        viewBot.toolbarButton(NEXT_MARKER).click();
        timeGraphIsReadyCondition(new TmfTimeRange(START_TIME, START_TIME), START_TIME);

        /* enable Lost Events navigation */
        viewBot.toolbarDropDownButton(NEXT_MARKER).menuItem(LOST_EVENTS).click();

        /* click "Next Marker" */
        viewBot.toolbarButton(NEXT_MARKER).click();
        timeGraphIsReadyCondition(new TmfTimeRange(LOST_EVENT_TIME1, LOST_EVENT_END1), LOST_EVENT_TIME1);
    }

    /**
     * Test tool bar button "Add Bookmark..." and "Remove Bookmark"
     */
    @Test
    public void testAddRemoveBookmark() {
        SWTBotView viewBot = getViewBot();
        /* change window range to 10 ms */
        TmfTimeRange range = new TmfTimeRange(START_TIME, START_TIME.normalize(10000000L, ITmfTimestamp.NANOSECOND_SCALE));
        TmfSignalManager.dispatchSignal(new TmfWindowRangeUpdatedSignal(this, range));
        fBot.waitUntil(ConditionHelpers.windowRange(range));

        /* set selection to trace start time */
        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, START_TIME));
        timeGraphIsReadyCondition(new TmfTimeRange(START_TIME, START_TIME), START_TIME);

        /* set focus on time graph */
        SWTBotTimeGraph timeGraph = new SWTBotTimeGraph(viewBot.bot());
        timeGraph.setFocus();

        /* select first CPU resource */
        timeGraph.getEntry(LttngTraceGenerator.getName(), CPU0_THREADS).select();

        /* click "Select Next State Change" 2 times */
        viewBot.toolbarButton(SELECT_NEXT_STATE_CHANGE).click();
        timeGraphIsReadyCondition(new TmfTimeRange(CPU0_TIME1, CPU0_TIME1), CPU0_TIME1);
        viewBot.toolbarButton(SELECT_NEXT_STATE_CHANGE).click();
        timeGraphIsReadyCondition(new TmfTimeRange(CPU0_TIME2, CPU0_TIME2), CPU0_TIME2);

        /* click "Add Bookmark..." and fill Add Bookmark dialog */
        viewBot.toolbarButton(ADD_BOOKMARK).click();
        SWTBot dialogBot = fBot.shell(ADD_BOOKMARK_DIALOG).bot();
        dialogBot.text().setText("B1");
        dialogBot.button(OK).click();

        /*
         * click "Select Next State Change" 2 times and shift-click "Select Next
         * State Change
         */
        viewBot.toolbarButton(SELECT_NEXT_STATE_CHANGE).click();
        timeGraphIsReadyCondition(new TmfTimeRange(CPU0_TIME3, CPU0_TIME3), CPU0_TIME3);
        viewBot.toolbarButton(SELECT_NEXT_STATE_CHANGE).click();
        timeGraphIsReadyCondition(new TmfTimeRange(CPU0_TIME4, CPU0_TIME4), CPU0_TIME4);
        viewBot.toolbarButton(SELECT_NEXT_STATE_CHANGE).click(SWT.SHIFT);
        timeGraphIsReadyCondition(new TmfTimeRange(CPU0_TIME4, CPU0_TIME5), CPU0_TIME5);

        /* click "Add Bookmark..." and fill Add Bookmark dialog */
        viewBot.toolbarButton(ADD_BOOKMARK).click();
        dialogBot = fBot.shell(ADD_BOOKMARK_DIALOG).bot();
        dialogBot.text().setText("B2");
        dialogBot.button(OK).click();

        /* click "Previous Marker" */
        viewBot.toolbarButton(PREVIOUS_MARKER).click();
        timeGraphIsReadyCondition(new TmfTimeRange(CPU0_TIME2, CPU0_TIME2), CPU0_TIME2);

        /* click "Remove Bookmark" */
        viewBot.toolbarButton(REMOVE_BOOKMARK).click();

        /* click "Next Marker" */
        viewBot.toolbarButton(NEXT_MARKER).click();
        timeGraphIsReadyCondition(new TmfTimeRange(CPU0_TIME4, CPU0_TIME5), CPU0_TIME5);

        /* click "Remove Bookmark" */
        viewBot.toolbarButton(REMOVE_BOOKMARK).click();

        /* click "Previous Marker" */
        viewBot.toolbarButton(PREVIOUS_MARKER).click();
        timeGraphIsReadyCondition(new TmfTimeRange(CPU0_TIME4, CPU0_TIME5), CPU0_TIME5);

        /* click "Select Previous State Change" */
        viewBot.toolbarButton(SELECT_PREVIOUS_STATE_CHANGE).click();
        timeGraphIsReadyCondition(new TmfTimeRange(CPU0_TIME4, CPU0_TIME4), CPU0_TIME4);
    }

    /**
     * Test the marker axis
     */
    @Test
    public void testMarkerAxis() {
        SWTBotView viewBot = getViewBot();
        /* center window range of first lost event range */
        ITmfTimestamp startTime = LOST_EVENT_TIME1.normalize(-10000000L, ITmfTimestamp.NANOSECOND_SCALE);
        ITmfTimestamp endTime = LOST_EVENT_END1.normalize(10000000L, ITmfTimestamp.NANOSECOND_SCALE);
        TmfTimeRange range = new TmfTimeRange(startTime, endTime);
        TmfSignalManager.dispatchSignal(new TmfWindowRangeUpdatedSignal(this, range));
        fBot.waitUntil(ConditionHelpers.windowRange(range));

        /* set selection to window start time */
        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, startTime));
        timeGraphIsReadyCondition(new TmfTimeRange(startTime, startTime), startTime);

        /* get marker axis size with one category */
        final TimeGraphMarkerAxis markerAxis = viewBot.bot().widget(WidgetOfType.widgetOfType(TimeGraphMarkerAxis.class));
        final Point size1 = getSize(markerAxis);

        /* add bookmark at window start time */
        viewBot.toolbarButton(ADD_BOOKMARK).click();
        SWTBot dialogBot = fBot.shell(ADD_BOOKMARK_DIALOG).bot();
        dialogBot.text().setText("B");
        dialogBot.button(OK).click();

        /* get marker axis size with two categories */
        final Point size2 = getSize(markerAxis);
        final int rowHeight = size2.y - size1.y;

        /*
         * get the state area bounds, since we don't know the name space width
         */
        final TimeGraphControl timeGraph = viewBot.bot().widget(WidgetOfType.widgetOfType(TimeGraphControl.class));
        int x0 = getXForTime(timeGraph, startTime.toNanos());
        int x1 = getXForTime(timeGraph, endTime.toNanos());

        /*
         * click at the center of the marker axis width and first row height, it
         * should be within the lost event range
         */
        final SWTBotCanvas markerAxisCanvas = new SWTBotCanvas(markerAxis);
        markerAxisCanvas.click((x0 + x1) / 2, TOP_MARGIN + rowHeight / 2);
        fBot.waitUntil(ConditionHelpers.selectionRange(new TmfTimeRange(LOST_EVENT_TIME1, LOST_EVENT_END1)));

        /*
         * click near the left of the marker axis width and center of second row
         * height, it should be on the bookmark label
         */
        markerAxisCanvas.click(x0 + 2, TOP_MARGIN + rowHeight + rowHeight / 2);
        fBot.waitUntil(ConditionHelpers.selectionRange(new TmfTimeRange(startTime, startTime)));

        /* click "Remove Bookmark" */
        viewBot.toolbarButton(REMOVE_BOOKMARK).click();
        assertEquals(size1, getSize(markerAxis));

        /* click the 'expanded' icon to collapse */
        markerAxisCanvas.click(TOGGLE_SIZE.x / 2, TOGGLE_SIZE.y / 2);
        assertEquals(TOGGLE_SIZE.y, getSize(markerAxis).y);

        /* click the 'collapsed' icon to expand */
        markerAxisCanvas.click(TOGGLE_SIZE.x / 2, TOGGLE_SIZE.y / 2);
        assertEquals(size1, getSize(markerAxis));

        /* click on the 'X' icon to hide the 'Lost Events' marker category */
        markerAxisCanvas.click(TOGGLE_SIZE.x + HIDE_SIZE.x / 2, TOP_MARGIN + HIDE_SIZE.y / 2);
        assertEquals(0, getSize(markerAxis).y);

        /* show Lost Events markers */
        viewBot.viewMenu(LOST_EVENTS).click();
        SWTBotUtils.waitUntil(ma -> size1.equals(getSize(ma)), markerAxis, "Lost Events did not reappear");
    }

    /**
     * Test the filter
     */
    @Test
    public void testFilter() {
        /* change window range to 1 ms */
        TmfTimeRange range = new TmfTimeRange(START_TIME, START_TIME.normalize(1000000L, ITmfTimestamp.NANOSECOND_SCALE));
        TmfSignalManager.dispatchSignal(new TmfWindowRangeUpdatedSignal(this, range));
        timeGraphIsReadyCondition(new TmfTimeRange(START_TIME, START_TIME), START_TIME);
        SWTBotToolbarButton filterButton = getViewBot().toolbarButton(ResourcesViewTest.SHOW_VIEW_FILTERS);
        filterButton.click();
        SWTBot bot = fBot.shell("Filter").activate().bot();
        /*
         * The filtered tree initialization triggers a delayed refresh job that
         * can interfere with the tree selection. Wait for new refresh jobs to
         * avoid this.
         */
        SWTBotTree treeBot = bot.tree();
        // set filter text
        SWTBotTreeItem treeItem = treeBot.getTreeItem(LttngTraceGenerator.getName());
        bot.text().setText("24");
        fBot.waitUntil(ConditionHelpers.treeItemCount(treeItem, 2));
        // clear filter text
        bot.text().setText(LttngTraceGenerator.getName());
        fBot.waitUntil(ConditionHelpers.treeItemCount(treeItem, 75));
        // get how many items there are
        int checked = SWTBotUtils.getTreeCheckedItemCount(treeBot);
        assertEquals("default", 76, checked);
        // test "uncheck all button"
        bot.button(UNCHECK_ALL).click();
        checked = SWTBotUtils.getTreeCheckedItemCount(treeBot);
        assertEquals(0, checked);
        // test check all
        bot.button(CHECK_ALL).click();
        checked = SWTBotUtils.getTreeCheckedItemCount(treeBot);
        assertEquals(CHECK_ALL, 76, checked);
        // test uncheck inactive
        treeBot.getTreeItem(LttngTraceGenerator.getName()).select("CPU 1 States");
        bot.button(UNCHECK_ALL).click();
        bot.button(CHECK_SELECTED).click();
        checked = SWTBotUtils.getTreeCheckedItemCount(treeBot);
        assertEquals(CHECK_SELECTED, 2, checked);
        // test check subtree
        bot.button(UNCHECK_ALL).click();
        bot.button(CHECK_SUBTREE).click();
        checked = SWTBotUtils.getTreeCheckedItemCount(treeBot);
        assertEquals(CHECK_SUBTREE, 2, checked);
        // test uncheck selected
        bot.button(CHECK_ALL).click();
        bot.button(UNCHECK_SELECTED).click();
        checked = SWTBotUtils.getTreeCheckedItemCount(treeBot);
        assertEquals(UNCHECK_SELECTED, 75, checked);
        // test uncheck subtree
        bot.button(CHECK_ALL).click();
        bot.button(UNCHECK_SUBTREE).click();
        checked = SWTBotUtils.getTreeCheckedItemCount(treeBot);
        assertEquals(UNCHECK_SELECTED, 75, checked);
        // test filter
        bot.button(UNCHECK_ALL).click();
        checked = SWTBotUtils.getTreeCheckedItemCount(treeBot);
        assertEquals(0, checked);
        bot.text().setText("CPU 2");
        fBot.waitUntil(ConditionHelpers.treeItemCount(treeItem, 75));
        bot.button(CHECK_ALL).click();
        checked = SWTBotUtils.getTreeCheckedItemCount(treeBot);
        assertEquals("Filtered", 76, checked);
        bot.button("OK").click();
    }

    private void timeGraphIsReadyCondition(@NonNull TmfTimeRange selectionRange, @NonNull ITmfTimestamp visibleTime) {
        IWorkbenchPart part = getViewBot().getViewReference().getPart(false);
        fBot.waitUntil(ConditionHelpers.timeGraphIsReadyCondition((AbstractTimeGraphView) part, selectionRange, visibleTime));
    }

    private static int getXForTime(TimeGraphControl timeGraph, long time) {
        return UIThreadRunnable.syncExec(new Result<Integer>() {
            @Override
            public Integer run() {
                return timeGraph.getXForTime(time);
            }
        });
    }

    private static Point getSize(Control control) {
        return UIThreadRunnable.syncExec(new Result<Point>() {
            @Override
            public Point run() {
                return control.getSize();
            }
        });
    }

    private static int getVisibleItems(SWTBotTimeGraph timegraph) {
        return UIThreadRunnable.syncExec(Display.getDefault(), new IntResult() {
            @Override
            public Integer run() {
                int count = 0;
                TimeGraphControl control = timegraph.widget;
                ITimeGraphEntry[] expandedElements = control.getExpandedElements();
                for (ITimeGraphEntry entry : expandedElements) {
                    Rectangle itemBounds = control.getItemBounds(entry);
                    if (itemBounds.height > 0) {
                        count++;
                    }
                }
                return count;
            }
        });
    }
}
