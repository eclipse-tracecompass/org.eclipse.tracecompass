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

package org.eclipse.tracecompass.analysis.profiling.core.tests.callgraph2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.analysis.profiling.core.tests.CallStackTestBase2;
import org.eclipse.tracecompass.analysis.profiling.core.tests.FlameDataProviderTestUtils;
import org.eclipse.tracecompass.analysis.profiling.core.tests.stubs2.CallStackAnalysisStub;
import org.eclipse.tracecompass.internal.analysis.profiling.core.flamegraph.FlameGraphDataProvider;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FlameChartEntryModel;
import org.eclipse.tracecompass.internal.analysis.profiling.core.tree.AllGroupDescriptor;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

/**
 * Test the {@link FlameGraphDataProvider} class
 *
 * @author Geneviève Bastien
 */
public class FlameGraphDataProviderTest extends CallStackTestBase2 {

    private static final String EXPECTED_FILE_PATH = "testfiles/dp/";
    private static final String WITH_PARENT = " with parent ";

    private static final @NonNull Map<@NonNull String, @NonNull Object> TREE_PARAMETERS = ImmutableMap.of(
            DataProviderParameterUtils.REQUESTED_TIME_KEY, ImmutableList.of(0, Long.MAX_VALUE));

    // Keep the map of values to style, to make sure a same value always has the
    // same style
    private static final Map<String, String> VALUE_TO_STYLE = new HashMap<>();

    /**
     * Test the {@link FlameGraphDataProvider} for the test callstack, with all
     * items separately
     *
     * @throws IOException
     *             if an I/O error occurs reading from the expected value file
     *             or a malformed or unmappable byte sequence is read
     */
    @Test
    public void testFlameGraphDataProviderAllItems() throws IOException {
        CallStackAnalysisStub cga = getModule();
        FlameGraphDataProvider<?, ?, ?> provider = new FlameGraphDataProvider<>(getTrace(), cga, cga.getId());
        Map<@NonNull Long, FlameChartEntryModel> idsToNames = assertAndGetTree(provider, "expectedFgTreeFull", Collections.emptyMap());
        assertRowsRequests(provider, idsToNames, "Full", 19);
        assertWrongTooltipQueries(provider, idsToNames);
    }

    /**
     * Test the {@link FlameGraphDataProvider} for the test callstack, with
     * items grouped by process
     *
     * @throws IOException
     *             if an I/O error occurs reading from the expected value file
     *             or a malformed or unmappable byte sequence is read
     */
    @Test
    public void testFlameGraphDataProviderGroupByProcess() throws IOException {
        CallStackAnalysisStub cga = getModule();
        FlameGraphDataProvider<?, ?, ?> provider = new FlameGraphDataProvider<>(getTrace(), cga, cga.getId());
        Map<@NonNull Long, FlameChartEntryModel> idsToNames = assertAndGetTree(provider, "expectedFgTreeProcess", ImmutableMap.of(FlameGraphDataProvider.GROUP_BY_KEY, "Processes/*"));
        assertRowsRequests(provider, idsToNames, "Process", 38);
        assertWrongTooltipQueries(provider, idsToNames);
    }

    /**
     * Test the {@link FlameGraphDataProvider} for the test callstack, with all
     * items grouped together.
     *
     * @throws IOException
     *             if an I/O error occurs reading from the expected value file
     *             or a malformed or unmappable byte sequence is read
     */
    @Test
    public void testFlameGraphDataProviderGrouped() throws IOException {
        CallStackAnalysisStub cga = getModule();
        FlameGraphDataProvider<?, ?, ?> provider = new FlameGraphDataProvider<>(getTrace(), cga, cga.getId());
        Map<@NonNull Long, FlameChartEntryModel> idsToNames = assertAndGetTree(provider, "expectedFgTreeOne", ImmutableMap.of(FlameGraphDataProvider.GROUP_BY_KEY, AllGroupDescriptor.getInstance().getName()));
        assertRowsRequests(provider, idsToNames, "One", 72);
        assertWrongTooltipQueries(provider, idsToNames);
    }

    /**
     * Test the {@link FlameGraphDataProvider} for the test callstack, with only
     * the callgraph for a time selection.
     *
     * @throws IOException
     *             if an I/O error occurs reading from the expected value file
     *             or a malformed or unmappable byte sequence is read
     */
    @Test
    public void testFlameGraphDataProviderSelection() throws IOException {
        CallStackAnalysisStub cga = getModule();
        FlameGraphDataProvider<?, ?, ?> provider = new FlameGraphDataProvider<>(getTrace(), cga, cga.getId());
        Map<@NonNull Long, FlameChartEntryModel> idsToNames = assertAndGetTree(provider, "expectedFgTreeSelection", ImmutableMap.of(FlameGraphDataProvider.SELECTION_RANGE_KEY, ImmutableList.of(5, 15)));
        assertRowsRequests(provider, idsToNames, "Selection", 10);
        assertWrongTooltipQueries(provider, idsToNames);
    }

    private static void assertRowsRequests(FlameGraphDataProvider<?, ?, ?> provider, Map<Long, FlameChartEntryModel> idsToNames, String resultFileSuffix, long maxDuration) throws IOException {
        String filePrefix = "expectedFgRow" + resultFileSuffix;
        // Test getting all the states
        Builder<Long> builder = ImmutableList.builder();
        for (long i = 0; i < maxDuration; i++) {
            builder.add(i);
        }
        assertRows(provider, idsToNames, builder.build(), filePrefix, "All");

        // Test getting only the first and last states
        assertRows(provider, idsToNames, ImmutableList.of(0L, maxDuration - 1), filePrefix, "2Times");

        // Test getting the states for the last half of the flamegraph
        builder = ImmutableList.builder();
        for (long i = maxDuration / 2; i < maxDuration; i++) {
            builder.add(i);
        }
        assertRows(provider, idsToNames, builder.build(), filePrefix, "Zoom");
    }

    private static Map<@NonNull Long, FlameChartEntryModel> assertAndGetTree(FlameGraphDataProvider<?, ?, ?> provider, String filePath, @NonNull Map<@NonNull String, @NonNull Object> additionalParameters) throws IOException {
        Map<@NonNull String, @NonNull Object> parameters = new HashMap<>(TREE_PARAMETERS);
        parameters.putAll(additionalParameters);
        @SuppressWarnings("null")
        TmfModelResponse<TmfTreeModel<@NonNull FlameChartEntryModel>> treeResponse = provider.fetchTree(parameters, null);
        assertNotNull(treeResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, treeResponse.getStatus());
        TmfTreeModel<@NonNull FlameChartEntryModel> treeModel = treeResponse.getModel();
        assertNotNull(treeModel);
        List<FlameChartEntryModel> treeEntries = treeModel.getEntries();

        List<String> expectedStrings = Files.readAllLines(Paths.get(EXPECTED_FILE_PATH + filePath));
        assertEquals(expectedStrings.size(), treeEntries.size());
        for (int i = 0; i < expectedStrings.size(); i++) {
            String expectedString = expectedStrings.get(i);
            String[] split = expectedString.split(",");

            FlameChartEntryModel parent = null;
            if (!split[5].equals("-")) {
                parent = FlameDataProviderTestUtils.findEntryByNameAndType(treeEntries, split[5], getEntryType(split[4]));
                assertNotNull("parent entry of " + split[0] + ' ' + split[1] + WITH_PARENT + split[5], parent);
            }

            // Find the entry with type and name with the parent entry, since
            // function entries have similar name, look with the parent
            FlameChartEntryModel fgEntry = parent == null ? FlameDataProviderTestUtils.findEntryByNameAndType(treeEntries, split[1], getEntryType(split[0]))
                    : FlameDataProviderTestUtils.findEntryByNameAndType(FlameDataProviderTestUtils.findEntriesByParent(treeEntries, parent.getId()), split[1], getEntryType(split[0]));
            assertNotNull("Expecting entry " + split[0] + ' ' + split[1] + WITH_PARENT + split[5], fgEntry);
            assertEquals("Start time of entry " + split[0] + ' ' + split[1] + WITH_PARENT + split[5], Long.parseLong(split[2]), fgEntry.getStartTime());
            assertEquals("End time of entry " + split[0] + ' ' + split[1] + WITH_PARENT + split[5], Long.parseLong(split[3]), fgEntry.getEndTime());
            assertEquals("Parent ID of entry " + split[0] + ' ' + split[1] + WITH_PARENT + split[5], parent == null ? -1 : parent.getId(), fgEntry.getParentId());
        }
        Map<@NonNull Long, FlameChartEntryModel> map = new HashMap<>();
        for (FlameChartEntryModel fgModel : treeEntries) {
            map.put(fgModel.getId(), fgModel);
        }
        return map;
    }

    private static void assertWrongTooltipQueries(FlameGraphDataProvider<?, ?, ?> provider, Map<@NonNull Long, FlameChartEntryModel> idsToNames) {
        @NonNull List<@NonNull Long> wrongEntry = Collections.singletonList(13213L);
        @NonNull List<@NonNull Long> twoEntries = Objects.requireNonNull(List.of(1L, 2L));
        FlameChartEntryModel entry = FlameDataProviderTestUtils.findEntryByNameAndType(idsToNames.values(), "1", FlameChartEntryModel.EntryType.FUNCTION);
        assertNotNull(entry);
        @NonNull List<@NonNull Long> correctEntry = Collections.singletonList(entry.getId());

        /* No params */
        TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> tooltipResponse = provider.fetchTooltip(Collections.emptyMap(), new NullProgressMonitor());
        assertNotNull(tooltipResponse);
        assertEquals(ITmfResponse.Status.FAILED, tooltipResponse.getStatus());
        Map<@NonNull String, @NonNull String> tooltipModel = tooltipResponse.getModel();
        if (tooltipModel != null) {
            assertTrue(tooltipModel.isEmpty());
        }
        /* Full query */
        tooltipResponse = provider.fetchTooltip(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(0L, Long.MAX_VALUE, 2, idsToNames.keySet())), new NullProgressMonitor());
        assertNotNull(tooltipResponse);
        assertEquals(ITmfResponse.Status.FAILED, tooltipResponse.getStatus());
        tooltipModel = tooltipResponse.getModel();
        if (tooltipModel != null) {
            assertTrue(tooltipModel.isEmpty());
        }
        /* Query with two entries */
        tooltipResponse = provider.fetchTooltip(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(3L, 3L, 1, twoEntries)), new NullProgressMonitor());
        assertNotNull(tooltipResponse);
        assertEquals(ITmfResponse.Status.FAILED, tooltipResponse.getStatus());
        tooltipModel = tooltipResponse.getModel();
        if (tooltipModel != null) {
            assertTrue(tooltipModel.isEmpty());
        }
        /* Query with wrong entry */
        tooltipResponse = provider.fetchTooltip(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(3L, 3L, 1, wrongEntry)), new NullProgressMonitor());
        assertNotNull(tooltipResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, tooltipResponse.getStatus());
        tooltipModel = tooltipResponse.getModel();
        if (tooltipModel != null) {
            assertTrue(tooltipModel.isEmpty());
        }
        /* Off query */
        tooltipResponse = provider.fetchTooltip(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(321837218L, 321837218L, 1, correctEntry)), new NullProgressMonitor());
        assertNotNull(tooltipResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, tooltipResponse.getStatus());
        tooltipModel = tooltipResponse.getModel();
        if (tooltipModel != null) {
            assertTrue(tooltipModel.isEmpty());
        }
        /* Wide query */
        tooltipResponse = provider.fetchTooltip(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(0L, 3L, 2, correctEntry)), new NullProgressMonitor());
        assertNotNull(tooltipResponse);
        assertEquals(ITmfResponse.Status.FAILED, tooltipResponse.getStatus());
        tooltipModel = tooltipResponse.getModel();
        if (tooltipModel != null) {
            assertTrue(tooltipModel.isEmpty());
        }
    }

    @SuppressWarnings("null")
    private static FlameChartEntryModel.EntryType getEntryType(String string) {
        return FlameChartEntryModel.EntryType.valueOf(string.toUpperCase());
    }

    private static void assertRows(FlameGraphDataProvider<?, ?, ?> provider, Map<Long, FlameChartEntryModel> idsToNames, @NonNull List<Long> requestedTimes, String filePath, String descriptor) throws IOException {
        @SuppressWarnings("null")
        TmfModelResponse<TimeGraphModel> rowResponse = provider.fetchRowModel(prepareRowParameters(idsToNames.keySet(), requestedTimes), null);
        assertNotNull(rowResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, rowResponse.getStatus());
        TimeGraphModel rowModel = rowResponse.getModel();
        assertNotNull(rowModel);
        Map<Long, @NonNull ITimeGraphRowModel> rows = new HashMap<>();
        for (ITimeGraphRowModel oneRow : rowModel.getRows()) {
            rows.put(oneRow.getEntryID(), oneRow);
        }
        // ensure row order
        Collection<FlameChartEntryModel> models = idsToNames.values();

        List<String> expectedStrings = Files.readAllLines(Paths.get(EXPECTED_FILE_PATH + filePath + descriptor));
        assertEquals(expectedStrings.size(), rows.size());
        for (int i = 0; i < expectedStrings.size(); i++) {
            String expectedString = expectedStrings.get(i);
            String[] split = expectedString.split(":");
            FlameChartEntryModel fgEntry = findRowEntry(split[0], models);
            assertNotNull(descriptor + ":Entry exists " + split[0], fgEntry);

            ITimeGraphRowModel row = rows.get(fgEntry.getId());
            assertNotNull(descriptor + ": Row entry exists" + split[0], row);

            @NonNull String[] rowStates = split[1].split(",");
            assertTooltip(provider, fgEntry.getId(), Long.parseLong(rowStates[0]), false, rowStates[3], Long.parseLong(rowStates[1]) + " ns");
            assertTooltip(provider, fgEntry.getId(), Long.parseLong(rowStates[0]), true, rowStates[3], Long.parseLong(rowStates[1]) + " ns");

            assertEqualsStates(split[1], row.getStates(), descriptor + ": " + split[0], getEntryType(split[0].split(",")[0]));
        }
    }

    private static void assertTooltip(FlameGraphDataProvider<?, ?, ?> provider, Long entryId, Long requestedTime, boolean isAction, String expectedObject, String expectedDuration) {
        TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> tooltipResponse = provider.fetchTooltip(prepareTooltipParameters(entryId, requestedTime, isAction), new NullProgressMonitor());
        assertNotNull(tooltipResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, tooltipResponse.getStatus());
        Map<@NonNull String, @NonNull String> tooltipModel = tooltipResponse.getModel();
        assertNotNull(tooltipModel);
        if (expectedObject.equals("null")) {
            assertTrue(tooltipModel.isEmpty());
        } else if (List.of(ProcessStatus.RUN.toString(), ProcessStatus.WAIT_CPU.toString()).contains(expectedObject)) {
            if (isAction) {
                assertNotNull(tooltipModel);
            } else {
                assertEquals(expectedObject, tooltipModel.get("Object"));
                assertEquals(expectedDuration, tooltipModel.get("Duration"));
            }
        } else {
            if (isAction) {
                assertNotNull(tooltipModel);
            } else {
                assertEquals(expectedObject, tooltipModel.get("Object"));
                assertEquals(expectedDuration, tooltipModel.get("\tTotal duration"));
            }
        }
    }

    private static FlameChartEntryModel findRowEntry(String entryDetails, Collection<FlameChartEntryModel> models) {
        String[] details = entryDetails.split(",");
        FlameChartEntryModel parentEntry = FlameDataProviderTestUtils.findEntryByNameAndType(models, details[3], getEntryType(details[2]));
        if (parentEntry == null) {
            return null;
        }
        return FlameDataProviderTestUtils.findEntryByNameAndType(FlameDataProviderTestUtils.findEntriesByParent(models, parentEntry.getId()), details[1], getEntryType(details[0]));
    }

    private static @NonNull Map<@NonNull String, @NonNull Object> prepareRowParameters(@NonNull Set<Long> ids, @NonNull List<Long> requestedTimes) {
        return ImmutableMap.of(DataProviderParameterUtils.REQUESTED_TIME_KEY, requestedTimes, DataProviderParameterUtils.REQUESTED_ITEMS_KEY, ids);
    }

    private static @NonNull Map<@NonNull String, @NonNull Object> prepareTooltipParameters(Long entryId, Long requestedTime, boolean isAction) {
        if (isAction) {
            return ImmutableMap.of(
                    DataProviderParameterUtils.REQUESTED_TIME_KEY, Collections.singletonList(requestedTime),
                    DataProviderParameterUtils.REQUESTED_ITEMS_KEY, Collections.singletonList(entryId),
                    FlameGraphDataProvider.TOOLTIP_ACTION_KEY, Collections.EMPTY_LIST);

        }
        return ImmutableMap.of(
                DataProviderParameterUtils.REQUESTED_TIME_KEY, Collections.singletonList(requestedTime),
                DataProviderParameterUtils.REQUESTED_ITEMS_KEY, Collections.singletonList(entryId));
    }

    private static void assertEqualsStates(String string, @NonNull List<@NonNull ITimeGraphState> states, String descriptor, FlameChartEntryModel.EntryType entryType) {
        String[] stringStates = string.split(",");
        for (int i = 0; i < stringStates.length / 4; i++) {
            assertTrue(descriptor + " has state " + i, states.size() > i);
            ITimeGraphState state = states.get(i);
            assertEquals(descriptor + ": start time at position " + i, Long.parseLong(stringStates[i * 4]), state.getStartTime());
            assertEquals(descriptor + ": duration at position " + i, Long.parseLong(stringStates[i * 4 + 1]), state.getDuration());
            String strValue = stringStates[i * 4 + 2];
            OutputElementStyle style = state.getStyle();
            if (strValue.equals("-")) {
                assertNull(descriptor + ": style at position " + i, style);
            } else {
                assertNotNull(descriptor + ": existing style at position " + i, style);
                String parentKey = style.getParentKey();

                if (!entryType.equals(FlameChartEntryModel.EntryType.KERNEL)) {
                    // The style should be a string that represents a number if
                    // it is not a kernel entry, so make sure it can be parsed
                    // as integer
                    try {
                        Integer.parseInt(parentKey);
                    } catch (NumberFormatException e) {
                        fail("Unexpected style: " + parentKey);
                    }
                }
                String expectedStyle = VALUE_TO_STYLE.computeIfAbsent(strValue, str -> parentKey);
                assertEquals(descriptor + ": style at position " + i, expectedStyle, parentKey);
            }
            assertEquals(descriptor + ": no value at position " + i, Integer.MIN_VALUE, state.getValue());
            assertEquals(descriptor + ": label at position " + i, stringStates[i * 4 + 3], String.valueOf(state.getLabel()));
        }
        assertEquals(descriptor + " no extra state", stringStates.length / 4, states.size());
    }
}
