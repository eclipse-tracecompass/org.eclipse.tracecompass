/**********************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore;

import static org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore.StubSegmentStoreProvider.NEXT_DIR_UNDER_TEST;
import static org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore.StubSegmentStoreProvider.PREV_DIR_UNDER_TEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.VirtualTableQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.TmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.VirtualTableCell;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.VirtualTableLine;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.CoreFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link SegmentStoreTableDataProvider} with a single
 * {@link TmfTrace} using {@link SegmentStoreAnalysisModule}
 *
 * @author: Kyrollos Bekhet
 */
public class SegmentStoreTableDataProviderTest {

    private static ITmfVirtualTableDataProvider<@NonNull TmfTreeDataModel, @NonNull VirtualTableLine> fDataProvider;

    private static final String START_TIME_COLUMN_NAME = "Start Time";
    private static final String END_TIME_COLUMN_NAME = "End Time";
    private static final String DURATION_COLUMN_NAME = "Duration";
    private static final String TABLE_COMPARATOR_EXPRESSION_KEY = "table_comparator_expression"; //$NON-NLS-1$
    private static final String TABLE_SEARCH_EXPRESSION_KEY = "table_search_expressions"; //$NON-NLS-1$
    private static final String TABLE_SEARCH_DIRECTION_KEY = "table_search_direction"; //$NON-NLS-1$

    private static Map<String, Long> fColumns = Collections.emptyMap();
    private static TmfXmlTraceStub fTrace;

    /**
     * Set-up resources
     *
     * @throws TmfAnalysisException
     *             Trace exception should not happen
     */
    @SuppressWarnings("null")
    @BeforeClass
    public static void init() throws TmfAnalysisException {
        fTrace = new TmfXmlTraceStubNs();
        @NonNull SegmentStoreAnalysisModule fixture = getValidSegment(fTrace);
        ITmfTrace trace = fTrace;
        assertNotNull(trace);
        fDataProvider = new SegmentStoreTableDataProvider(fTrace, fixture, "org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore");
        fColumns = fetchColumnId();
    }

    private static @NonNull SegmentStoreAnalysisModule getValidSegment(@NonNull ITmfTrace trace) throws TmfAnalysisException {
        StubSegmentStoreProvider preFixture = new StubSegmentStoreProvider();
        preFixture.setTrace(trace);
        SegmentStoreAnalysisModule fixture = new SegmentStoreAnalysisModule(trace);
        if (trace instanceof TmfXmlTraceStubNs) {
            ((TmfXmlTraceStubNs) trace).addAnalysisModule(fixture);
        }
        fixture.schedule();
        fixture.waitForCompletion();
        return fixture;
    }

    private static Map<String, Long> fetchColumnId() {
        TmfTreeModel<@NonNull TmfTreeDataModel> columns = fDataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 0, 1)), null).getModel();
        if (columns == null) {
            return Collections.emptyMap();
        }
        List<@NonNull TmfTreeDataModel> columnEntries = columns.getEntries();
        assertEquals(START_TIME_COLUMN_NAME, columnEntries.get(0).getName());
        assertEquals(END_TIME_COLUMN_NAME, columnEntries.get(1).getName());
        assertEquals(DURATION_COLUMN_NAME, columnEntries.get(2).getName());
        assertEquals(StubSegmentStoreProvider.STUB_COLUMN_NAME, columnEntries.get(3).getName());

        Map<String, Long> expectedColumns = new LinkedHashMap<>();
        for (TmfTreeDataModel column : columnEntries) {
            expectedColumns.put(column.getName(), column.getId());
        }
        return expectedColumns;
    }

    /**
     * Disposes resources
     */
    @AfterClass
    public static void tearDown() {
        fTrace.dispose();
    }

    // --------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    /**
     * Test column returned by the provider.
     */
    @Test
    public void testDataProviderFetchColumn() {
        Long startTimeColumnId = fColumns.get(START_TIME_COLUMN_NAME);
        Long endTimeColumnId = fColumns.get(END_TIME_COLUMN_NAME);
        Long durationColumnId = fColumns.get(DURATION_COLUMN_NAME);
        Long customColumndId = fColumns.get(StubSegmentStoreProvider.STUB_COLUMN_NAME);

        assertNotNull(startTimeColumnId);
        assertNotNull(endTimeColumnId);
        assertNotNull(durationColumnId);
        assertNotNull(customColumndId);

        List<@NonNull TmfTreeDataModel> expectedColumnEntries = Arrays.asList(
                new TmfTreeDataModel(startTimeColumnId, -1, Collections.singletonList(START_TIME_COLUMN_NAME)),
                new TmfTreeDataModel(endTimeColumnId, -1, Collections.singletonList(END_TIME_COLUMN_NAME)),
                new TmfTreeDataModel(durationColumnId, -1, Collections.singletonList(DURATION_COLUMN_NAME)),
                new TmfTreeDataModel(customColumndId, -1, Collections.singletonList(StubSegmentStoreProvider.STUB_COLUMN_NAME)));

        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TmfTreeDataModel>> response = fDataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 0, 1)), null);
        TmfTreeModel<@NonNull TmfTreeDataModel> currentColumnModel = response.getModel();
        assertNotNull(currentColumnModel);
        List<@NonNull TmfTreeDataModel> currentColumnEntries = Objects.requireNonNull(currentColumnModel).getEntries();
        assertEquals(expectedColumnEntries, currentColumnEntries);
    }

    /**
     * Test lines returned by the provider if the query starts from index zero.
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLineZeroIndex() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 5);
        @NonNull List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(0, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(1, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(1)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(2, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(2)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(3, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(3)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(4, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(4)), new VirtualTableCell(lineDuration(4)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 0, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test lines returned by the provider if the query starts from a non zero
     * index.
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLineNonZeroIndex() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 10, 5);
        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(10, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(10)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(11, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(11)), new VirtualTableCell(lineDuration(4)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(12, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(12)), new VirtualTableCell(lineDuration(5)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(13, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(13)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(14, Arrays.asList(new VirtualTableCell(lineTime(14)), new VirtualTableCell(lineTime(14)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 10, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test the performance and efficiency of the fetch method of the provider
     * by requesting a segment with a big rank in the segment store.
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLineTimeOut() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 3200, 5);
        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(3200, Arrays.asList(new VirtualTableCell(lineTime(3199)), new VirtualTableCell(lineTime(3200)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(3201, Arrays.asList(new VirtualTableCell(lineTime(3199)), new VirtualTableCell(lineTime(3201)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(3202, Arrays.asList(new VirtualTableCell(lineTime(3199)), new VirtualTableCell(lineTime(3202)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(3203, Arrays.asList(new VirtualTableCell(lineTime(3199)), new VirtualTableCell(lineTime(3203)), new VirtualTableCell(lineDuration(4)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(3204, Arrays.asList(new VirtualTableCell(lineTime(3199)), new VirtualTableCell(lineTime(3204)), new VirtualTableCell(lineDuration(5)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 3200, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test lines returned by the provider.
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLineCornerIndex() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 1000, 5);
        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(1000, Arrays.asList(new VirtualTableCell(lineTime(994)), new VirtualTableCell(lineTime(1000)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(1001, Arrays.asList(new VirtualTableCell(lineTime(1001)), new VirtualTableCell(lineTime(1001)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(1002, Arrays.asList(new VirtualTableCell(lineTime(1001)), new VirtualTableCell(lineTime(1002)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(1003, Arrays.asList(new VirtualTableCell(lineTime(1001)), new VirtualTableCell(lineTime(1003)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(1004, Arrays.asList(new VirtualTableCell(lineTime(1001)), new VirtualTableCell(lineTime(1004)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 1000, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test lines returned by the provider starting from a given index and with
     * a search filter applied.
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLineWithSearch() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 5);
        @NonNull Map<@NonNull String, @NonNull Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Map<Long, String> searchMap = new HashMap<>();
        searchMap.put(fColumns.get(START_TIME_COLUMN_NAME), lineTime(7000));
        fetchParameters.put(TABLE_SEARCH_EXPRESSION_KEY, searchMap);
        fetchParameters.put(TABLE_SEARCH_DIRECTION_KEY, NEXT_DIR_UNDER_TEST);

        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(7000, Arrays.asList(new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(7001, Arrays.asList(new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineTime(7001)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(7002, Arrays.asList(new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineTime(7002)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(7003, Arrays.asList(new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineTime(7003)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(7004, Arrays.asList(new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineTime(7004)), new VirtualTableCell(lineDuration(4)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));
        expectedData.forEach(sl -> sl.setActiveProperties(CoreFilterProperty.HIGHLIGHT));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(fetchParameters, null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 7000, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test lines returned by the provider starting from a given index and with
     * a search filter applied, here in the previous direction.
     *
     * Same as {@link #testDataProviderFetchLineWithSearch} -except previous.
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLineWithSearchPrevious() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 65534, 5);
        @NonNull Map<@NonNull String, @NonNull Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Map<Long, String> searchMap = new HashMap<>();
        searchMap.put(fColumns.get(START_TIME_COLUMN_NAME), lineTime(7000));
        fetchParameters.put(TABLE_SEARCH_EXPRESSION_KEY, searchMap);
        fetchParameters.put(TABLE_SEARCH_DIRECTION_KEY, PREV_DIR_UNDER_TEST);

        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(7006, Arrays.asList(new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineTime(7006)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(7005, Arrays.asList(new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineTime(7005)), new VirtualTableCell(lineDuration(5)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(7004, Arrays.asList(new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineTime(7004)), new VirtualTableCell(lineDuration(4)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(7003, Arrays.asList(new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineTime(7003)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(7002, Arrays.asList(new VirtualTableCell(lineTime(7000)), new VirtualTableCell(lineTime(7002)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));
        expectedData.forEach(sl -> sl.setActiveProperties(CoreFilterProperty.HIGHLIGHT));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(fetchParameters, null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 7006, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test null model returned upon invalid search direction provided (typo).
     *
     * Same as {@link #testDataProviderFetchLineWithSearch} -except for typo.
     */
    @Test
    public void testDataProviderFetchLineWithSearchTypo() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 5);
        @NonNull Map<@NonNull String, @NonNull Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Map<Long, String> searchMap = new HashMap<>();
        searchMap.put(fColumns.get(START_TIME_COLUMN_NAME), lineTime(7000));
        fetchParameters.put(TABLE_SEARCH_EXPRESSION_KEY, searchMap);
        String typo = "T";
        fetchParameters.put(TABLE_SEARCH_DIRECTION_KEY, NEXT_DIR_UNDER_TEST + typo);

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(fetchParameters, null);
        assertNull(response.getModel());
        assertEquals(response.getStatus(), Status.FAILED);
        assertEquals(response.getStatusMessage(), CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
    }

    /**
     * This test tests the case if we went beyond the end time stored in the
     * index
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLineCrossIndexes() {
        int count = 2000;
        long previousStartTime = 0;
        List<@NonNull VirtualTableLine> expectedData = new ArrayList<>();
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(new ArrayList<>(fColumns.values()), 0, count);
        for (long i = 0; i < count; i++) {
            if (i % 7 == 0) {
                previousStartTime = i;
            }
            expectedData.add(new VirtualTableLine(i,
                    Arrays.asList(new VirtualTableCell(lineTime(previousStartTime)), new VirtualTableCell(lineTime(i)), new VirtualTableCell(lineDuration(i - previousStartTime)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));
        }

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 0, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test lines returned by the provider with an end comparator requested in
     * the query parameters.
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLineWithEndTimeComparator() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 5);

        @NonNull Map<@NonNull String, @NonNull Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Object endTimeColumnID = fColumns.get(END_TIME_COLUMN_NAME);
        assertNotNull(endTimeColumnID);
        fetchParameters.put(TABLE_COMPARATOR_EXPRESSION_KEY, endTimeColumnID);
        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(0, Arrays.asList(new VirtualTableCell(lineTime(65534)), new VirtualTableCell(lineTime(65534)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(1, Arrays.asList(new VirtualTableCell(lineTime(65527)), new VirtualTableCell(lineTime(65533)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(2, Arrays.asList(new VirtualTableCell(lineTime(65527)), new VirtualTableCell(lineTime(65532)), new VirtualTableCell(lineDuration(5)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(3, Arrays.asList(new VirtualTableCell(lineTime(65527)), new VirtualTableCell(lineTime(65531)), new VirtualTableCell(lineDuration(4)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(4, Arrays.asList(new VirtualTableCell(lineTime(65527)), new VirtualTableCell(lineTime(65530)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(fetchParameters, null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 0, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test lines returned by the provider with a duration comparator requested
     * in the query parameters.
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLineWithDurationComparator() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 5);
        Map<String, Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Object durationColumnID = fColumns.get(DURATION_COLUMN_NAME);
        assertNotNull(durationColumnID);
        fetchParameters.put(TABLE_COMPARATOR_EXPRESSION_KEY, durationColumnID);

        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(0, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(1, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(2, Arrays.asList(new VirtualTableCell(lineTime(14)), new VirtualTableCell(lineTime(14)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(3, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(4, Arrays.asList(new VirtualTableCell(lineTime(28)), new VirtualTableCell(lineTime(28)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(fetchParameters, null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 0, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test last lines in the segment store returned by the provider with an end
     * comparator requested in the query parameters.
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLastLinesWithEndComparator() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 65529, 5);
        Map<String, Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Object endTimeColumnID = fColumns.get(END_TIME_COLUMN_NAME);
        assertNotNull(endTimeColumnID);
        fetchParameters.put(TABLE_COMPARATOR_EXPRESSION_KEY, endTimeColumnID);

        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(65529, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(5)), new VirtualTableCell(lineDuration(5)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(65530, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(4)), new VirtualTableCell(lineDuration(4)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(65531, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(3)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(65532, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(2)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(65533, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(1)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(fetchParameters, null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 65529, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test last lines in the segment store returned by the provider with a
     * duration comparator requested in the query parameters.
     */
    @SuppressWarnings("null")
    @Test
    public void testDataProviderFetchLastLinesWithDurationComparator() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 65529, 5);
        Map<String, Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Object durationColumnID = fColumns.get(DURATION_COLUMN_NAME);
        assertNotNull(durationColumnID);
        fetchParameters.put(TABLE_COMPARATOR_EXPRESSION_KEY, durationColumnID);

        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(65529, Arrays.asList(new VirtualTableCell(lineTime(65492)), new VirtualTableCell(lineTime(65498)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(65530, Arrays.asList(new VirtualTableCell(lineTime(65499)), new VirtualTableCell(lineTime(65505)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(65531, Arrays.asList(new VirtualTableCell(lineTime(65506)), new VirtualTableCell(lineTime(65512)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(65532, Arrays.asList(new VirtualTableCell(lineTime(65513)), new VirtualTableCell(lineTime(65519)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(65533, Arrays.asList(new VirtualTableCell(lineTime(65520)), new VirtualTableCell(lineTime(65526)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(fetchParameters, null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 65529, 65535);
        assertEquals(expectedModel, currentModel);
    }

    private static String lineTime(long milliseconds) {
        return TmfTimestamp.fromNanos(milliseconds).toString();
    }

    private static String lineDuration(long duration) {
        return new DecimalFormat("###,###.##").format(duration);
    }

}
