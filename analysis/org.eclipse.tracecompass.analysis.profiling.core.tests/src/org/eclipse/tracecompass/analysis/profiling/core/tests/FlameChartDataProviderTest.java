/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.tests.stubs2.CallStackAnalysisStub;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.registry.LinuxStyle;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FlameChartDataProvider;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FlameChartDataProviderFactory;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FlameChartEntryModel;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FlameChartEntryModel.EntryType;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test the {@link FlameChartDataProvider} class
 *
 * @author Geneviève Bastien
 */
public class FlameChartDataProviderTest extends CallStackTestBase2 {

    private static final @Nullable IProgressMonitor MONITOR = new NullProgressMonitor();
    private static final String FOR_ENTRY = " for entry ";

    private FlameChartDataProvider getDataProvider() {
        CallStackAnalysisStub module = getModule();
        assertNotNull(module);

        FlameChartDataProviderFactory factory = new FlameChartDataProviderFactory();

        FlameChartDataProvider dataProvider = (FlameChartDataProvider) factory.createProvider(getTrace(), module.getId());
        assertNotNull(dataProvider);
        return dataProvider;
    }

    /**
     * Test getting the descriptors built in the flame chart data provider
     * factory
     */
    @Test
    public void testGetDescriptors() {
        FlameChartDataProviderFactory dataProviderFactory = new FlameChartDataProviderFactory();
        @SuppressWarnings("null")
        Collection<IDataProviderDescriptor> descriptors = dataProviderFactory.getDescriptors(getTrace());
        assertEquals(descriptors.size(), 1);

        for (IDataProviderDescriptor descriptor : descriptors) {
            if (descriptor.getId().equals("org.eclipse.tracecompass.analysis.profiling.core.flamechart:org.eclipse.tracecompass.analysis.profiling.core.tests.stub")) {
                assertEquals("Test Callstack (new) - Flame Chart", descriptor.getName());
                assertEquals(IDataProviderDescriptor.ProviderType.TIME_GRAPH, descriptor.getType());
                assertEquals("Show Flame Chart provided by Analysis module: Test Callstack (new)", descriptor.getDescription());
            } else {
                fail("Unknown Entry" + descriptor.getId());
            }
        }
    }

    /**
     * Test getting the tree from the flame chart data provider
     */
    @Test
    public void testFetchTree() {
        FlameChartDataProvider dataProvider = getDataProvider();

        TmfModelResponse<@NonNull TmfTreeModel<@NonNull FlameChartEntryModel>> responseTree = dataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, Long.MAX_VALUE, 2)), new NullProgressMonitor());
        assertTrue(responseTree.getStatus() == (ITmfResponse.Status.COMPLETED));

        // Test the size of the tree
        TmfTreeModel<@NonNull FlameChartEntryModel> model = responseTree.getModel();
        assertNotNull(model);
        List<FlameChartEntryModel> modelEntries = model.getEntries();
        assertEquals(22, modelEntries.size());

        String traceName = getTrace().getName();

        // Test the hierarchy of the tree
        for (FlameChartEntryModel entry : modelEntries) {
            FlameChartEntryModel parent = FlameDataProviderTestUtils.findEntryById(modelEntries, entry.getParentId());
            switch (entry.getEntryType()) {
            case LEVEL: {
                assertNotNull(parent);
                // Verify the hierarchy of the elements
                switch (entry.getName()) {
                case "1":
                    assertEquals(traceName, parent.getName());
                    break;
                case "2":
                    assertEquals("1", parent.getName());
                    break;
                case "3":
                    assertEquals("1", parent.getName());
                    break;
                case "5":
                    assertEquals(traceName, parent.getName());
                    break;
                case "6":
                    assertEquals("5", parent.getName());
                    break;
                case "7":
                    assertEquals("5", parent.getName());
                    break;
                default:
                    fail("Unknown entry " + entry.getName());
                }
            }
                break;
            case FUNCTION:
            case KERNEL:
                assertNotNull(parent);
                assertEquals(EntryType.LEVEL, parent.getEntryType());
                break;
            case TRACE:
                assertEquals(-1, entry.getParentId());
                break;
            default:
                fail("Unknown entry " + entry);
            }
        }
    }

    /**
     * Test getting the model from the flame chart data provider
     */
    @SuppressWarnings({ "null" })
    @Test
    public void testFetchModel() {
        FlameChartDataProvider dataProvider = getDataProvider();

        TmfModelResponse<@NonNull TmfTreeModel<@NonNull FlameChartEntryModel>> responseTree = dataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, Long.MAX_VALUE, 2)), new NullProgressMonitor());
        assertTrue(responseTree.getStatus() == (ITmfResponse.Status.COMPLETED));
        TmfTreeModel<@NonNull FlameChartEntryModel> model = responseTree.getModel();
        assertNotNull(model);
        List<FlameChartEntryModel> modelEntries = model.getEntries();
        // Find the entries corresponding to threads 3 and 6 (along with pid 5)
        Set<@NonNull Long> selectedIds = new HashSet<>();
        // Thread 3
        FlameChartEntryModel tid3 = FlameDataProviderTestUtils.findEntryByNameAndType(modelEntries, "3", EntryType.LEVEL);
        assertNotNull(tid3);
        selectedIds.add(tid3.getId());
        List<FlameChartEntryModel> tid3Children = FlameDataProviderTestUtils.findEntriesByParent(modelEntries, tid3.getId());
        assertEquals(3, tid3Children.size());
        tid3Children.forEach(child -> selectedIds.add(child.getId()));

        // Pid 5
        FlameChartEntryModel pid5 = FlameDataProviderTestUtils.findEntryByNameAndType(modelEntries, "5", EntryType.LEVEL);
        assertNotNull(pid5);
        selectedIds.add(pid5.getId());
        // Thread 6
        FlameChartEntryModel tid6 = FlameDataProviderTestUtils.findEntryByNameAndType(modelEntries, "6", EntryType.LEVEL);
        assertNotNull(tid6);
        selectedIds.add(tid6.getId());
        List<FlameChartEntryModel> tid6Children = FlameDataProviderTestUtils.findEntriesByParent(modelEntries, tid6.getId());
        assertEquals(4, tid6Children.size());
        tid6Children.forEach(child -> selectedIds.add(child.getId()));

        // Get the row model for those entries with high resolution
        TmfModelResponse<@NonNull TimeGraphModel> rowResponse = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(3, 15, 50, selectedIds)), new NullProgressMonitor());
        assertEquals(ITmfResponse.Status.COMPLETED, rowResponse.getStatus());

        TimeGraphModel rowModel = rowResponse.getModel();
        assertNotNull(rowModel);
        List<@NonNull ITimeGraphRowModel> rows = rowModel.getRows();
        assertEquals(10, rows.size());

        // Verify the level entries
        verifyStates(rows, tid3, Collections.emptyList());
        verifyStates(rows, pid5, Collections.emptyList());
        verifyStates(rows, tid6, Collections.emptyList());
        // Verify function level 1 of tid 3
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid3Children, 1, EntryType.FUNCTION), ImmutableList.of(new TimeGraphState(3, 17, Integer.MIN_VALUE, "op2")));
        // Verify function level 2 of tid 3
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid3Children, 2, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(1, 4, Integer.MIN_VALUE),
                new TimeGraphState(5, 1, Integer.MIN_VALUE, "op3"),
                new TimeGraphState(6, 1, Integer.MIN_VALUE),
                new TimeGraphState(7, 6, Integer.MIN_VALUE, "op2"),
                new TimeGraphState(13, 8, Integer.MIN_VALUE)));
        // Verify kernel statuses of tid 3
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid3Children, -1, EntryType.KERNEL), ImmutableList.of(
                new TimeGraphState(3, 3, null, new OutputElementStyle(LinuxStyle.USERMODE.getLabel())),
                new TimeGraphState(6, 1, null, new OutputElementStyle(LinuxStyle.WAIT_FOR_CPU.getLabel())),
                new TimeGraphState(7, 6, null, new OutputElementStyle(LinuxStyle.USERMODE.getLabel())),
                new TimeGraphState(13, 8, null, new OutputElementStyle(LinuxStyle.WAIT_FOR_CPU.getLabel()))), true);

        // Verify function level 1 of tid 6
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid6Children, 1, EntryType.FUNCTION), ImmutableList.of(new TimeGraphState(1, 19, Integer.MIN_VALUE, "op1")));
        // Verify function level 2 of tid 6
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid6Children, 2, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(2, 5, Integer.MIN_VALUE, "op3"),
                new TimeGraphState(7, 1, Integer.MIN_VALUE),
                new TimeGraphState(8, 3, Integer.MIN_VALUE, "op2"),
                new TimeGraphState(11, 1, Integer.MIN_VALUE),
                new TimeGraphState(12, 8, Integer.MIN_VALUE, "op4")));
        // Verify function level 3 of tid 6
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid6Children, 3, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(1, 3, Integer.MIN_VALUE),
                new TimeGraphState(4, 2, Integer.MIN_VALUE, "op1"),
                new TimeGraphState(6, 3, Integer.MIN_VALUE),
                new TimeGraphState(9, 1, Integer.MIN_VALUE, "op3"),
                new TimeGraphState(10, 11, Integer.MIN_VALUE)));
        // Verify kernel statuses of tid 6
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid6Children, -1, EntryType.KERNEL), ImmutableList.of(
                new TimeGraphState(1, 5, null, new OutputElementStyle(LinuxStyle.USERMODE.getLabel())),
                new TimeGraphState(6, 2, null, new OutputElementStyle(LinuxStyle.WAIT_FOR_CPU.getLabel())),
                new TimeGraphState(8, 2, null, new OutputElementStyle(LinuxStyle.USERMODE.getLabel())),
                new TimeGraphState(10, 2, null, new OutputElementStyle(LinuxStyle.WAIT_FOR_CPU.getLabel())),
                new TimeGraphState(12, 8, null, new OutputElementStyle(LinuxStyle.USERMODE.getLabel()))), true);

        // Get the row model for those entries with low resolution
        rowResponse = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(3, 15, 2, selectedIds)), new NullProgressMonitor());
        assertEquals(ITmfResponse.Status.COMPLETED, rowResponse.getStatus());

        rowModel = rowResponse.getModel();
        assertNotNull(rowModel);
        rows = rowModel.getRows();
        assertEquals(10, rows.size());

        // Verify the level entries
        verifyStates(rows, tid3, Collections.emptyList());
        verifyStates(rows, pid5, Collections.emptyList());
        verifyStates(rows, tid6, Collections.emptyList());
        // Verify function level 1 of tid 3
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid3Children, 1, EntryType.FUNCTION), ImmutableList.of(new TimeGraphState(3, 17, Integer.MIN_VALUE, "op2")));
        // Verify function level 2 of tid 3
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid3Children, 2, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(1, 4, Integer.MIN_VALUE),
                new TimeGraphState(13, 8, Integer.MIN_VALUE)));
        // Verify kernel statuses of tid 3
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid3Children, -1, EntryType.KERNEL), ImmutableList.of(
                new TimeGraphState(3, 3, null, new OutputElementStyle(LinuxStyle.USERMODE.getLabel())),
                new TimeGraphState(13, 8, null, new OutputElementStyle(LinuxStyle.WAIT_FOR_CPU.getLabel()))), true);
        // Verify function level 1 of tid 6
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid6Children, 1, EntryType.FUNCTION), ImmutableList.of(new TimeGraphState(1, 19, Integer.MIN_VALUE, "op1")));
        // Verify function level 2 of tid 6
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid6Children, 2, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(2, 5, Integer.MIN_VALUE, "op3"),
                new TimeGraphState(12, 8, Integer.MIN_VALUE, "op4")));
        // Verify function level 3 of tid 6
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid6Children, 3, EntryType.FUNCTION), ImmutableList.of(
                new TimeGraphState(1, 3, Integer.MIN_VALUE),
                new TimeGraphState(10, 11, Integer.MIN_VALUE)));
        // Verify kernel statuses of tid 6
        verifyStates(rows, FlameDataProviderTestUtils.findEntryByDepthAndType(tid6Children, -1, EntryType.KERNEL), ImmutableList.of(
                new TimeGraphState(1, 5, null, new OutputElementStyle(LinuxStyle.USERMODE.getLabel())),
                new TimeGraphState(12, 8, null, new OutputElementStyle(LinuxStyle.USERMODE.getLabel()))), true);

        // Check arrows
        FlameChartEntryModel tid2 = FlameDataProviderTestUtils.findEntryByNameAndType(modelEntries, "2", EntryType.LEVEL);
        assertNotNull(tid2);
        List<FlameChartEntryModel> tid2Children = FlameDataProviderTestUtils.findEntriesByParent(modelEntries, tid2.getId());
        FlameChartEntryModel tid7 = FlameDataProviderTestUtils.findEntryByNameAndType(modelEntries, "7", EntryType.LEVEL);
        assertNotNull(tid7);
        List<FlameChartEntryModel> tid7Children = FlameDataProviderTestUtils.findEntriesByParent(modelEntries, tid7.getId());

        Map<@NonNull String, @NonNull Object> everythingQuery = FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, Long.MAX_VALUE, 2));
        TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> arrowResponse = dataProvider.fetchArrows(everythingQuery, new NullProgressMonitor());
        assertNotNull(arrowResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, arrowResponse.getStatus());
        List<@NonNull ITimeGraphArrow> arrowModel = arrowResponse.getModel();
        assertNotNull(arrowModel);
        assertFalse(arrowModel.isEmpty());

        FlameChartEntryModel tid2Child1 = FlameDataProviderTestUtils.findEntryByDepthAndType(tid2Children, 1, EntryType.FUNCTION);
        assertNotNull(tid2Child1);
        FlameChartEntryModel tid2Child2 = FlameDataProviderTestUtils.findEntryByDepthAndType(tid2Children, 2, EntryType.FUNCTION);
        assertNotNull(tid2Child2);
        FlameChartEntryModel tid2Child3 = FlameDataProviderTestUtils.findEntryByDepthAndType(tid2Children, 3, EntryType.FUNCTION);
        assertNotNull(tid2Child3);
        FlameChartEntryModel tid3Child2 = FlameDataProviderTestUtils.findEntryByDepthAndType(tid3Children, 2, EntryType.FUNCTION);
        assertNotNull(tid3Child2);
        FlameChartEntryModel tid6Child2 = FlameDataProviderTestUtils.findEntryByDepthAndType(tid6Children, 2, EntryType.FUNCTION);
        assertNotNull(tid6Child2);
        FlameChartEntryModel tid6Child3 = FlameDataProviderTestUtils.findEntryByDepthAndType(tid6Children, 3, EntryType.FUNCTION);
        assertNotNull(tid6Child3);
        FlameChartEntryModel tid7Child3 = FlameDataProviderTestUtils.findEntryByDepthAndType(tid7Children, 3, EntryType.FUNCTION);
        assertNotNull(tid7Child3);

        verifyArrows(arrowModel, ImmutableList.of(
                new TimeGraphArrow(tid2Child1.getId(), tid2Child2.getId(), 1, 2, 1),
                new TimeGraphArrow(tid2Child3.getId(), tid3Child2.getId(), 4, 3, 2),
                new TimeGraphArrow(tid3Child2.getId(), tid6Child3.getId(), 5, 4, 3),
                new TimeGraphArrow(tid6Child3.getId(), tid7Child3.getId(), 5, 5, 4),
                new TimeGraphArrow(tid6Child3.getId(), tid6Child2.getId(), 9, 3, 5)));
    }

    /**
     * Test following a callstack backward and forward
     */
    @SuppressWarnings("null")
    @Test
    public void testFollowEvents() {
        FlameChartDataProvider dataProvider = getDataProvider();

        TmfModelResponse<@NonNull TmfTreeModel<@NonNull FlameChartEntryModel>> responseTree = dataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, Long.MAX_VALUE, 2)), new NullProgressMonitor());
        assertTrue(responseTree.getStatus() == (ITmfResponse.Status.COMPLETED));
        TmfTreeModel<@NonNull FlameChartEntryModel> model = responseTree.getModel();
        assertNotNull(model);
        List<FlameChartEntryModel> modelEntries = model.getEntries();

        // Thread 2
        FlameChartEntryModel tid2 = FlameDataProviderTestUtils.findEntryByNameAndType(modelEntries, "2", EntryType.LEVEL);
        assertNotNull(tid2);
        List<FlameChartEntryModel> tid2Children = FlameDataProviderTestUtils.findEntriesByParent(modelEntries, tid2.getId());
        assertEquals(4, tid2Children.size());

        // For each child, make sure the response is always the same
        for (FlameChartEntryModel tid2Child : tid2Children) {
            TmfModelResponse<@NonNull TimeGraphModel> rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(6, Long.MAX_VALUE, 2, Collections.singleton(tid2Child.getId()))), MONITOR);
            if (!tid2Child.getEntryType().equals(EntryType.KERNEL)) {
                verifyFollowResponse(rowModel, 1, 7);
            }
        }

        // Go forward from time 7 till the end for one of the child element
        Set<@NonNull Long> selectedEntry = Objects.requireNonNull(Collections.singleton(tid2Children.get(2).getId()));
        TmfModelResponse<@NonNull TimeGraphModel> rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(7, Long.MAX_VALUE, 2, selectedEntry)), MONITOR);
        verifyFollowResponse(rowModel, 0, 10);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(10, Long.MAX_VALUE, 2, selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 1, 12);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(12, Long.MAX_VALUE, 2, selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 0, 20);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(20, Long.MAX_VALUE, 2, selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, -1, -1);

        // Go backward from the back
        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(ImmutableList.of(Long.MIN_VALUE, 20L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 1, 12);

        // Go backward from time 7 till the beginning
        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(ImmutableList.of(Long.MIN_VALUE, 7L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 2, 5);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(ImmutableList.of(Long.MIN_VALUE, 5L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 3, 4);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(ImmutableList.of(Long.MIN_VALUE, 4L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 2, 3);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(ImmutableList.of(Long.MIN_VALUE, 3L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, 1, 1);

        rowModel = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(ImmutableList.of(Long.MIN_VALUE, 1L), selectedEntry)), new NullProgressMonitor());
        verifyFollowResponse(rowModel, -1, -1);
    }

    private static void verifyFollowResponse(TmfModelResponse<@NonNull TimeGraphModel> rowModel, int expectedDepth, int expectedTime) {
        assertEquals(ITmfResponse.Status.COMPLETED, rowModel.getStatus());

        TimeGraphModel model = rowModel.getModel();
        if (expectedDepth < 0) {
            assertNull(model);
            return;
        }
        assertNotNull(model);
        List<@NonNull ITimeGraphRowModel> rows = model.getRows();
        assertEquals(1, rows.size());
        @SuppressWarnings("null")
        List<ITimeGraphState> row = rows.get(0).getStates();
        assertEquals(1, row.size());
        ITimeGraphState stackInterval = row.get(0);
        long depth = stackInterval.getValue();
        assertEquals(expectedDepth, depth);
        assertEquals(expectedTime, stackInterval.getStartTime());
    }

    private static void verifyStates(List<ITimeGraphRowModel> rowModels, FlameChartEntryModel entry, List<TimeGraphState> expectedStates) {
        verifyStates(rowModels, entry, expectedStates, false);
    }

    private static void verifyStates(List<ITimeGraphRowModel> rowModels, FlameChartEntryModel entry, List<TimeGraphState> expectedStates, boolean checkStyles) {
        assertNotNull(entry);
        ITimeGraphRowModel rowModel = rowModels.stream()
                .filter(model -> model.getEntryID() == entry.getId())
                .findFirst().orElse(null);
        assertNotNull(rowModel);
        @SuppressWarnings("null")
        List<ITimeGraphState> states = rowModel.getStates();
        for (int i = 0; i < states.size(); i++) {
            String entryName = entry.getName();
            if (i > expectedStates.size() - 1) {
                fail("Unexpected state at position " + i + FOR_ENTRY + entryName + ": " + states.get(i));
            }
            ITimeGraphState actual = states.get(i);
            ITimeGraphState expected = expectedStates.get(i);
            assertEquals("State start time at " + i + FOR_ENTRY + entryName, expected.getStartTime(), actual.getStartTime());
            assertEquals("Duration at " + i + FOR_ENTRY + entryName, expected.getDuration(), actual.getDuration());
            assertEquals("Label at " + i + FOR_ENTRY + entryName, expected.getLabel(), actual.getLabel());
            if (checkStyles) {
                assertEquals("Style at " + i + FOR_ENTRY + entryName, expected.getStyle(), actual.getStyle());
            }
        }
    }

    private static void verifyArrows(List<ITimeGraphArrow> arrows, List<ITimeGraphArrow> expectedArrows) {
        assertEquals(expectedArrows.size(), arrows.size());
        for (ITimeGraphArrow expectedArrow : expectedArrows) {
            for (ITimeGraphArrow arrow: arrows) {
                if (arrow.getValue() == expectedArrow.getValue()) {
                    assertEquals("Duration for arrow " + arrow.getValue(), expectedArrow.getDuration(), arrow.getDuration());
                    assertEquals("Start time for arrow " + arrow.getValue(), expectedArrow.getStartTime(), arrow.getStartTime());
                    assertEquals("Source Id for arrow " + arrow.getValue(), expectedArrow.getSourceId(), arrow.getSourceId());
                    assertEquals("Destination Id for arrow " + arrow.getValue(), expectedArrow.getDestinationId(), arrow.getDestinationId());
                }
            }
        }
    }
}
