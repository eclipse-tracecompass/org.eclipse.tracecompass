/*******************************************************************************
 * Copyright (c) 2016, 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.callstack.ui.swtbot.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.tracecompass.analysis.callstack.core.tests.callgraph.AggregationTreeTest;
import org.eclipse.tracecompass.analysis.callstack.core.tests.stubs.CallGraphAnalysisStub;
import org.eclipse.tracecompass.internal.analysis.callstack.core.flamegraph.FlameGraphDataProvider;
import org.eclipse.tracecompass.internal.analysis.callstack.core.flamegraph.FlameGraphDataProviderFactory;
import org.eclipse.tracecompass.internal.analysis.callstack.ui.flamegraph.FlameGraphView;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers.SWTBotTestCondition;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotTimeGraph;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.tests.shared.WaitUtils;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.ui.IViewPart;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/**
 * Unit tests for the flame graph view
 *
 * @author Matthew Khouzam
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class FlameGraphTest extends AggregationTreeTest {

    private static final String SECONDARY_ID = "org.eclipse.tracecompass.analysis.callstack.ui.swtbot.test";
    private static final String FLAMEGRAPH_ID = FlameGraphView.ID;

    private SWTWorkbenchBot fBot;
    private SWTBotView fViewBot;
    private FlameGraphView fFg;
    /** The Log4j logger instance. */
    private TimeGraphViewer fTimeGraphViewer;
    private SWTBotTimeGraph fTimeGraph;

    /**
     * Initialization
     */
    @BeforeClass
    public static void beforeClass() {
        SWTBotUtils.initialize();
        Thread.currentThread().setName("SWTBotTest");
        /* set up for swtbot */
        SWTBotPreferences.TIMEOUT = 20000; /* 20 second timeout */
        SWTBotPreferences.KEYBOARD_LAYOUT = "EN_US";
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        SWTBotUtils.closeView("welcome", bot);
        /* Switch perspectives */
        SWTBotUtils.switchToTracingPerspective();
        /* Finish waiting for eclipse to load */
        WaitUtils.waitForJobs();
    }

    /**
     * Open a flamegraph
     */
    @Override
    @Before
    public void before() {
        // Open the flame graph first
        fBot = new SWTWorkbenchBot();
        SWTBotUtils.openView(FLAMEGRAPH_ID, SECONDARY_ID);
        SWTBotView view = fBot.viewById(FLAMEGRAPH_ID);
        assertNotNull(view);
        fViewBot = view;
        FlameGraphView flamegraph = UIThreadRunnable.syncExec((Result<FlameGraphView>) () -> {
            IViewPart viewRef = fViewBot.getViewReference().getView(true);
            return (viewRef instanceof FlameGraphView) ? (FlameGraphView) viewRef : null;
        });
        assertNotNull(flamegraph);
        fTimeGraphViewer = flamegraph.getTimeGraphViewer();
        assertNotNull(fTimeGraphViewer);
        SWTBotUtils.maximize(flamegraph);
        fFg = flamegraph;
        fTimeGraph = new SWTBotTimeGraph(view.bot());
        // Now open the trace
        super.before();
    }

    /**
     * Clean up after a test, reset the views and reset the states of the
     * timegraph by pressing reset on all the resets of the legend
     */
    @Override
    @After
    public void after() {
        super.after();
        // Calling maximize again will minimize
        FlameGraphView fg = fFg;
        if (fg != null) {
            SWTBotUtils.maximize(fg);
        }
        // Reset the data provider so old data is not used to build view
        FlameGraphDataProviderFactory.registerDataProviderWithId(SECONDARY_ID, null);
        // Setting the input to null so the view can be emptied, to avoid race
        // conditions with subsequent tests
        TimeGraphViewer tg = fTimeGraphViewer;
        if (tg != null) {
            UIThreadRunnable.syncExec(() -> {
                tg.setInput(null);
                tg.refresh();
            });
        }
        fBot.waitUntil(new SWTBotTestCondition() {
            @Override
            public boolean test() throws Exception {
                return fTimeGraph.getEntries().length == 0;
            }

            @Override
            public String getFailureMessage() {
                return "Flame graph not emptied";
            }
        });
        fViewBot.close();
        fBot.waitUntil(ConditionHelpers.viewIsClosed(fViewBot));
    }

    private void loadFlameGraph() {
        CallGraphAnalysisStub cga = Objects.requireNonNull(getCga());
        ITmfTrace trace = getTrace();
        fFg.setTrace(trace);
        // Add the new provider and rebuild the view
        FlameGraphDataProvider<?, ?, ?> dp = new FlameGraphDataProvider<>(trace, cga, FlameGraphDataProvider.ID + ':' + SECONDARY_ID);
        FlameGraphDataProviderFactory.registerDataProviderWithId(SECONDARY_ID, dp);
        UIThreadRunnable.syncExec(() -> fFg.buildFlameGraph(trace, null, null));
        fBot.waitUntil(new SWTBotTestCondition() {
            @Override
            public boolean test() throws Exception {
                return fTimeGraph.getEntries().length > 0;
            }

            @Override
            public String getFailureMessage() {
                return "Flame graph not ready";
            }
        });
    }

    private ITimeGraphEntry selectRoot() {
        UIThreadRunnable.syncExec(() -> {
            fTimeGraphViewer.selectNextItem();
            fTimeGraphViewer.selectNextItem();
            fTimeGraphViewer.selectNextItem();
        });
        ITimeGraphEntry entry = fTimeGraphViewer.getSelection();
        assertNotNull(entry);
        return entry;
    }

    private static ITimeEvent getFirstEvent(ITimeGraphEntry actualEntry) {
        Optional<@NonNull ? extends ITimeEvent> actualEventOpt = StreamSupport.stream(Spliterators.spliteratorUnknownSize(actualEntry.getTimeEventsIterator(), Spliterator.NONNULL), false)
                .filter(i -> (i instanceof TimeEvent)).filter(j -> !(j instanceof NullTimeEvent))
                .findFirst();
        assertTrue(actualEventOpt.isPresent());
        return actualEventOpt.get();
    }

    @Override
    public void emptyStateSystemTest() {
        super.emptyStateSystemTest();
        loadFlameGraph();
        ITimeGraphEntry entry = fTimeGraphViewer.getSelection();
        assertNull(entry);
    }

    @Override
    public void cascadeTest() {
        super.cascadeTest();
        loadFlameGraph();
        ITimeGraphEntry entry = selectRoot();
        assertEquals(3, entry.getChildren().size());
        ITimeGraphEntry actualEntry = entry.getChildren().get(1);
        ITimeEvent actualEvent = getFirstEvent(actualEntry);
        assertNotNull(actualEvent);
        assertEquals(996, actualEvent.getDuration());
    }

    @Override
    public void mergeFirstLevelCalleesTest() {
        super.mergeFirstLevelCalleesTest();
        loadFlameGraph();
        ITimeGraphEntry entry = selectRoot();
        assertEquals(3, entry.getChildren().size());
        ITimeGraphEntry actualEntry = entry.getChildren().get(1);
        ITimeEvent actualEvent = getFirstEvent(actualEntry);
        assertNotNull(actualEvent);
        assertEquals(80, actualEvent.getDuration());
    }

    @Override
    public void multiFunctionRootsSecondTest() {
        super.multiFunctionRootsSecondTest();
        loadFlameGraph();
        ITimeGraphEntry entry = selectRoot();
        assertEquals(2, entry.getChildren().size());
        ITimeGraphEntry actualEntry = entry.getChildren().get(1);
        ITimeEvent actualEvent = getFirstEvent(actualEntry);
        assertNotNull(actualEvent);
        assertEquals(10, actualEvent.getDuration());
    }

    @Override
    public void mergeSecondLevelCalleesTest() {
        super.mergeSecondLevelCalleesTest();
        loadFlameGraph();
        ITimeGraphEntry entry = selectRoot();
        assertEquals(4, entry.getChildren().size());
        ITimeGraphEntry actualEntry = entry.getChildren().get(1);
        ITimeEvent actualEvent = getFirstEvent(actualEntry);
        assertNotNull(actualEvent);
        assertEquals(90, actualEvent.getDuration());
    }

    @Override
    public void multiFunctionRootsTest() {
        super.multiFunctionRootsTest();
        loadFlameGraph();
        ITimeGraphEntry entry = selectRoot();
        assertEquals(2, entry.getChildren().size());
        ITimeGraphEntry actualEntry = entry.getChildren().get(1);
        ITimeEvent actualEvent = getFirstEvent(actualEntry);
        assertNotNull(actualEvent);
        assertEquals(10, actualEvent.getDuration());
    }

    /**
     * Also test statistics tooltip
     */
    @Override
    public void treeTest() {
        super.treeTest();
        loadFlameGraph();
        ITimeGraphEntry entry = selectRoot();
        assertEquals(3, entry.getChildren().size());
        ITimeGraphEntry actualEntry = entry.getChildren().get(1);
        ITimeEvent actualEvent = getFirstEvent(actualEntry);
        assertNotNull(actualEvent);
        assertEquals(80, actualEvent.getDuration());
        FlameGraphView fg = fFg;
        Map<String, String> tooltip = fg.getPresentationProvider().getEventHoverToolTipInfo(actualEvent);
        assertTrue(tooltip.toString().contains("duration=80 ns"));
        assertTrue(tooltip.toString().contains("duration=40 ns"));
        tooltip = fg.getPresentationProvider().getEventHoverToolTipInfo(actualEvent, 5);
        assertTrue(tooltip.toString().contains("duration=80 ns"));
        assertTrue(tooltip.toString().contains("duration=40 ns"));
    }

    /**
     * Takes too much ram
     */
    @Ignore
    @Override
    public void largeTest() {
        // Do nothing
    }
}
