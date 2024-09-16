/**********************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.tmf.core.tests.event;

import static org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.TABLE_SEARCH_DIRECTION_KEY;
import static org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.TABLE_SEARCH_EXPRESSIONS_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableColumnDataModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableFilterModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.EventTableQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.VirtualTableQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.EventTableLine;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.TmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.VirtualTableCell;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.CoreFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestTrace;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link TmfEventTableDataProvider}
 *
 * @author Yonni Chen
 */
@NonNullByDefault
public class TmfEventTableDataProviderTest {

    private static final TmfTestTrace TEST_TRACE = TmfTestTrace.A_TEST_10K;

    private static ITmfTrace fTrace = new TmfTraceStub();
    private static ITmfVirtualTableDataProvider<TmfEventTableColumnDataModel, EventTableLine> fProvider = new TmfEventTableDataProvider(fTrace);

    private static final String TIMESTAMP_COLUMN_NAME = "Timestamp";
    private static final String TIMESTAMP_NS_COLUMN_NAME = "Timestamp ns";
    private static final String EVENT_TYPE_COLUMN_NAME = "Event type";
    private static final String CONTENTS_COLUMN_NAME = "Contents";

    // Search direction values reused
    private static final String NEXT_DIR_UNDER_TEST = "NEXT";
    private static final String PREV_DIR_UNDER_TEST = "PREVIOUS";

    private static final String TYPE_0 = "Type-0";
    private static final String TYPE_1 = "Type-1";
    private static final String TYPE_2 = "Type-2";
    private static final String TYPE_3 = "Type-3";
    private static final String TYPE_4 = "Type-4";
    private static final String TYPE_5 = "Type-5";
    private static final String TYPE_6 = "Type-6";

    private static Map<String, Long> fColumns = Collections.emptyMap();

    /**
     * Set up resources
     *
     * @throws TmfTraceException
     *             Trace exception should not happen
     */
    @BeforeClass
    public static void beforeClass() throws TmfTraceException {
        fTrace.dispose();
        fTrace = new TmfTraceStub(TEST_TRACE.getFullPath(), ITmfTrace.DEFAULT_TRACE_CACHE_SIZE, true, null);
        fProvider = new TmfEventTableDataProvider(fTrace);
        // Make sure the columns are computed before the test
        fColumns = fetchColumnId();
    }

    private static Map<String, Long> fetchColumnId() {
        TmfTreeModel<TmfEventTableColumnDataModel> columns = fProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 0, 1)), null).getModel();
        if (columns == null) {
            return Collections.emptyMap();
        }

        List<TmfEventTableColumnDataModel> columnEntries = columns.getEntries();
        // Order should be timestamp, event type and contents
        assertEquals(TIMESTAMP_COLUMN_NAME, columnEntries.get(0).getName());
        assertEquals(EVENT_TYPE_COLUMN_NAME, columnEntries.get(1).getName());
        assertEquals(CONTENTS_COLUMN_NAME, columnEntries.get(2).getName());
        assertEquals(TIMESTAMP_NS_COLUMN_NAME, columnEntries.get(3).getName());

        Map<String, Long> expectedColumns = new LinkedHashMap<>();
        for (TmfEventTableColumnDataModel column : columnEntries) {
            expectedColumns.put(column.getName(), column.getId());
        }
        return expectedColumns;
    }

    private static String lineTimestamp(long millisecond) {
        String timestamp = TmfTimestamp.fromMillis(millisecond).toString();
        if (timestamp == null) {
            timestamp = "";
        }
        return timestamp;
    }

    private static String lineNsTimestamp(int millisecond) {
        return String.valueOf(TmfTimestamp.fromMillis(millisecond).toNanos());
    }

    /**
     * Dispose resources
     */
    @AfterClass
    public static void tearDown() {
        fTrace.dispose();
    }

    /**
     * Test columns returned by the provider.
     */
    @Test
    public void testDataProviderFetchColumn() {
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long contentsColumnId = fColumns.get(CONTENTS_COLUMN_NAME);
        Long timestampNsColumnId = fColumns.get(TIMESTAMP_NS_COLUMN_NAME);
        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);
        assertNotNull(contentsColumnId);
        assertNotNull(timestampNsColumnId);
        List<TmfEventTableColumnDataModel> expectedColumnEntries = Arrays.asList(
                new TmfEventTableColumnDataModel(timestampColumnId, -1, Collections.singletonList(TIMESTAMP_COLUMN_NAME), "", false, DataType.TIMESTAMP),
                new TmfEventTableColumnDataModel(eventTypeColumnId, -1, Collections.singletonList(EVENT_TYPE_COLUMN_NAME), "The type of this event. This normally determines the field layout.", false, DataType.STRING),
                new TmfEventTableColumnDataModel(contentsColumnId, -1, Collections.singletonList(CONTENTS_COLUMN_NAME), "The fields (or payload) of this event", false, DataType.STRING),
                new TmfEventTableColumnDataModel(timestampNsColumnId, -1, Collections.singletonList(TIMESTAMP_NS_COLUMN_NAME), "Timestamp in nanoseconds, normalized and useful for calculations", true, DataType.STRING));

        TmfModelResponse<TmfTreeModel<TmfEventTableColumnDataModel>> response = fProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 0, 1)), null);
        TmfTreeModel<TmfEventTableColumnDataModel> currentColumnModel = response.getModel();
        assertNotNull(currentColumnModel);
        List<TmfEventTableColumnDataModel> currentColumnEntries = currentColumnModel.getEntries();
        assertEquals(expectedColumnEntries, currentColumnEntries);
    }

    /**
     * Given a start index and count, we check model returned by the data
     * provider. This test doesn't provide desired columns, so it queries data
     * for all of them
     */
    @Test
    public void testDataProvider() {
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Collections.emptyList(), 0, 5, null);

        List<EventTableLine> expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(1)), new VirtualTableCell(TYPE_0), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(1))), 0, TmfTimestamp.fromMillis(1), 0, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(2)), new VirtualTableCell(TYPE_1), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(2))), 1, TmfTimestamp.fromMillis(2), 1, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(3)), new VirtualTableCell(TYPE_2), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(3))), 2, TmfTimestamp.fromMillis(3), 2, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(4)), new VirtualTableCell(TYPE_3), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(4))), 3, TmfTimestamp.fromMillis(4), 3, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(5)), new VirtualTableCell(TYPE_4), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(5))), 4, TmfTimestamp.fromMillis(5), 4, 0));

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        ITmfVirtualTableModel<EventTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 0, fTrace.getNbEvents());
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Given a start index that is out of bound and count, we check data
     * returned by the data provider.
     */
    @Test
    public void testDataProviderWithOutOfBoundIndex() {
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Collections.emptyList(), 2000000, 5, null);
        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        assertNotNull(currentModel);
        assertEquals(new ArrayList<>(fColumns.values()), currentModel.getColumnIds());
        assertTrue(currentModel.getLines().isEmpty());
    }

    /**
     * Given a start index, count and a list of desired columns, we check model
     * returned by the data provider.
     */
    @Test
    public void testDataProviderWithDesiredColumns() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        assertNotNull(eventTypeColumnId);
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Collections.singletonList(eventTypeColumnId), 5, 5, null);

        List<Long> expectedColumnsId = Arrays.asList(eventTypeColumnId);
        List<EventTableLine> expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_5)), 5, TmfTimestamp.fromMillis(6), 5, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_6)), 6, TmfTimestamp.fromMillis(7), 6, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_0)), 7, TmfTimestamp.fromMillis(8), 7, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_1)), 8, TmfTimestamp.fromMillis(9), 8, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_2)), 9, TmfTimestamp.fromMillis(10), 9, 0));

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        ITmfVirtualTableModel<EventTableLine> expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 5, fTrace.getNbEvents());
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Given a start index, count and a list of desired columns that contains a
     * non-existent column, we check model returned by the data provider.
     */
    @Test
    public void testDataProviderWithOneNonExistentColumns() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);
        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Arrays.asList(eventTypeColumnId, 10L, timestampColumnId), 150, 5, null);

        List<Long> expectedColumnsId = Arrays.asList(eventTypeColumnId, timestampColumnId);
        List<EventTableLine> expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_3), new VirtualTableCell(lineTimestamp(151))), 150, TmfTimestamp.fromMillis(151), 150, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_4), new VirtualTableCell(lineTimestamp(152))), 151, TmfTimestamp.fromMillis(152), 151, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_5), new VirtualTableCell(lineTimestamp(153))), 152, TmfTimestamp.fromMillis(153), 152, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_6), new VirtualTableCell(lineTimestamp(154))), 153, TmfTimestamp.fromMillis(154), 153, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_0), new VirtualTableCell(lineTimestamp(155))), 154, TmfTimestamp.fromMillis(155), 154, 0));

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        ITmfVirtualTableModel<EventTableLine> expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 150, fTrace.getNbEvents());
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Given a start index, count and a list of desired columns that contains
     * only non-existent columns, we check data returned by the data provider.
     */
    @Test
    public void testDataProviderWithNonExistentColumns() {
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Arrays.asList(10L, 11L), 0, 10, null);
        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        assertNotNull(currentModel);
        assertTrue(currentModel.getColumnIds().isEmpty());
        assertTrue(currentModel.getLines().isEmpty());
    }

    /**
     * Given a start index, count and a list of desired columns, we check model
     * returned by the data provider. We also apply a filter on a column
     */
    @Test
    public void testDataProviderWithSimpleFilter() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);
        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);

        Map<Long, String> tableFilter = new HashMap<>();
        tableFilter.put(eventTypeColumnId, "1");
        TmfEventTableFilterModel filterModel = new TmfEventTableFilterModel(tableFilter, null, false);
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Arrays.asList(eventTypeColumnId, timestampColumnId), 0, 5, filterModel);
        Map<String, Object> parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        parameters.put(TmfEventTableDataProvider.TABLE_FILTERS_KEY, filterModel);

        List<Long> expectedColumnsId = Arrays.asList(eventTypeColumnId, timestampColumnId);
        TmfTimestampFormat.getDefaulTimeFormat().format(TmfTimestamp.fromMillis(2).toNanos());
        List<EventTableLine> expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_1), new VirtualTableCell(lineTimestamp(2))), 0, TmfTimestamp.fromMillis(2), 1, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_1), new VirtualTableCell(lineTimestamp(9))), 1, TmfTimestamp.fromMillis(9), 8, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_1), new VirtualTableCell(lineTimestamp(16))), 2, TmfTimestamp.fromMillis(16), 15, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_1), new VirtualTableCell(lineTimestamp(23))), 3, TmfTimestamp.fromMillis(23), 22, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_1), new VirtualTableCell(lineTimestamp(30))), 4, TmfTimestamp.fromMillis(30), 29, 0));

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(parameters, null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        TmfVirtualTableModel<@NonNull EventTableLine> expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 0, 1429);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Given a start index, count and a list of desired columns, we check model
     * returned by the data provider. We also apply two filters on two columns
     */
    @Test
    public void testDataProviderWithMultipleFilter() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);
        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);

        Map<Long, String> tableFilter = new HashMap<>();
        tableFilter.put(eventTypeColumnId, "0");
        tableFilter.put(timestampColumnId, "8");
        TmfEventTableFilterModel filterModel = new TmfEventTableFilterModel(tableFilter, null, false);
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Arrays.asList(eventTypeColumnId, timestampColumnId), 0, 5, filterModel);
        Map<String, Object> parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        parameters.put(TmfEventTableDataProvider.TABLE_FILTERS_KEY, filterModel);

        List<Long> expectedColumnsId = Arrays.asList(eventTypeColumnId, timestampColumnId);
        List<EventTableLine> expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_0), new VirtualTableCell(lineTimestamp(8))), 0, TmfTimestamp.fromMillis(8), 7, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_0), new VirtualTableCell(lineTimestamp(78))), 1, TmfTimestamp.fromMillis(78), 77, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_0), new VirtualTableCell(lineTimestamp(85))), 2, TmfTimestamp.fromMillis(85), 84, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_0), new VirtualTableCell(lineTimestamp(148))), 3, TmfTimestamp.fromMillis(148), 147, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_0), new VirtualTableCell(lineTimestamp(183))), 4, TmfTimestamp.fromMillis(183), 182, 0));

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(parameters, null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        ITmfVirtualTableModel<EventTableLine> expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 0, 492);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Given a start index, count and a list of desired columns, we check model
     * returned by the data provider. We also apply a search filter on a single
     * column, where no event matches the search expression.
     */
    @Test
    public void testDataProviderWithSearchNoMatch() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);

        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);

        // Query for the index for the first matching event
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Arrays.asList(eventTypeColumnId, timestampColumnId), 0, 1, null);
        Map<String, Object> parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);

        Map<Long, String> searchExpressions = new HashMap<>();
        searchExpressions.put(eventTypeColumnId, "Does not exits");

        parameters.put(TABLE_SEARCH_EXPRESSIONS_KEY, searchExpressions);
        parameters.put(TABLE_SEARCH_DIRECTION_KEY, NEXT_DIR_UNDER_TEST);

        List<Long> expectedColumnsId = Arrays.asList(eventTypeColumnId, timestampColumnId);
        TmfTimestampFormat.getDefaulTimeFormat().format(TmfTimestamp.fromMillis(2).toNanos());
        List<EventTableLine> expectedData = Collections.emptyList();

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(parameters, null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        assertTrue(currentModel.getLines().isEmpty());
        TmfVirtualTableModel<@NonNull EventTableLine> expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 0, 10000);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Same as {@link #testDataProviderWithSearchNoMatch} -except for typo.
     */
    @Test
    public void testDataProviderWithSearchTypo() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);

        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);

        // Query for the index for the first matching event
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Arrays.asList(eventTypeColumnId, timestampColumnId), 0, 1, null);
        Map<String, Object> parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);

        Map<Long, String> searchExpressions = new HashMap<>();
        searchExpressions.put(eventTypeColumnId, "Does not exits");

        parameters.put(TABLE_SEARCH_EXPRESSIONS_KEY, searchExpressions);
        String typo = "T";
        parameters.put(TABLE_SEARCH_DIRECTION_KEY, NEXT_DIR_UNDER_TEST + typo);

        TmfTimestampFormat.getDefaulTimeFormat().format(TmfTimestamp.fromMillis(2).toNanos());

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(parameters, null);
        assertNull(response.getModel());
        assertEquals(response.getStatus(), Status.FAILED);
        assertEquals(response.getStatusMessage(), CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
    }

    /**
     * Given a start index, count and a list of desired columns, we check model
     * returned by the data provider. We also apply a search filter on a single
     * column to search NEXT from the start index.
     */
    @Test
    public void testDataProviderWithSimpleSingleColumnsSearch() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);

        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);

        // Query for the index for the first matching event
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Arrays.asList(eventTypeColumnId, timestampColumnId), 0, 1, null);
        Map<String, Object> parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);

        Map<Long, String> searchExpressions = new HashMap<>();
        searchExpressions.put(eventTypeColumnId, TYPE_2);

        parameters.put(TABLE_SEARCH_EXPRESSIONS_KEY, searchExpressions);
        parameters.put(TABLE_SEARCH_DIRECTION_KEY, NEXT_DIR_UNDER_TEST);

        List<Long> expectedColumnsId = Arrays.asList(eventTypeColumnId, timestampColumnId);
        TmfTimestampFormat.getDefaulTimeFormat().format(TmfTimestamp.fromMillis(2).toNanos());
        List<EventTableLine> expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_2), new VirtualTableCell(lineTimestamp(3))), 2, TmfTimestamp.fromMillis(3), 2, 0));
        expectedData.get(0).setActiveProperties(CoreFilterProperty.HIGHLIGHT);

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(parameters, null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        TmfVirtualTableModel<@NonNull EventTableLine> expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 2, 10000);
        assertEquals(expectedModel, currentModel);

        // Query for events with search filter active. Matching lines will be
        // tagged for highlighting
        int nbEventsRequested = 5;
        queryFilter = new EventTableQueryFilter(Arrays.asList(eventTypeColumnId, timestampColumnId), 0, nbEventsRequested, null);
        parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        parameters.put(TABLE_SEARCH_EXPRESSIONS_KEY, searchExpressions);

        response = fProvider.fetchLines(parameters, null);
        currentModel = response.getModel();
        assertNotNull(currentModel);
        assertEquals(nbEventsRequested, currentModel.getLines().size());
        expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_0), new VirtualTableCell(lineTimestamp(1))), 0, TmfTimestamp.fromMillis(1), 0, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_1), new VirtualTableCell(lineTimestamp(2))), 1, TmfTimestamp.fromMillis(2), 1, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_2), new VirtualTableCell(lineTimestamp(3))), 2, TmfTimestamp.fromMillis(3), 2, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_3), new VirtualTableCell(lineTimestamp(4))), 3, TmfTimestamp.fromMillis(4), 3, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_4), new VirtualTableCell(lineTimestamp(5))), 4, TmfTimestamp.fromMillis(5), 4, 0));
        expectedData.get(2).setActiveProperties(CoreFilterProperty.HIGHLIGHT);
        expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 0, 10000);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Given a start index, count and a list of desired columns, we check model
     * returned by the data provider. We also apply a search filter on a single
     * column to search NEXT from the start index. N number of events from the
     * first matched event will be returned by the data provider.
     */
    @Test
    public void testDataProviderWithGetDataFromSearchForwardMatch() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);

        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);

        // Query for the index for the first matching event
        int nbEventsRequested = 3;
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Arrays.asList(eventTypeColumnId, timestampColumnId), 0, nbEventsRequested, null);
        Map<String, Object> parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);

        Map<Long, String> searchExpressions = new HashMap<>();
        searchExpressions.put(eventTypeColumnId, TYPE_2);

        parameters.put(TABLE_SEARCH_EXPRESSIONS_KEY, searchExpressions);
        parameters.put(TABLE_SEARCH_DIRECTION_KEY, NEXT_DIR_UNDER_TEST);

        List<Long> expectedColumnsId = Arrays.asList(eventTypeColumnId, timestampColumnId);
        TmfTimestampFormat.getDefaulTimeFormat().format(TmfTimestamp.fromMillis(2).toNanos());
        List<EventTableLine> expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_2), new VirtualTableCell(lineTimestamp(3))), 2, TmfTimestamp.fromMillis(3), 2, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_3), new VirtualTableCell(lineTimestamp(4))), 3, TmfTimestamp.fromMillis(4), 3, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_4), new VirtualTableCell(lineTimestamp(5))), 4, TmfTimestamp.fromMillis(5), 4, 0));
        expectedData.get(0).setActiveProperties(CoreFilterProperty.HIGHLIGHT);

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(parameters, null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        TmfVirtualTableModel<@NonNull EventTableLine> expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 2, 10000);
        assertNotNull(currentModel);
        assertEquals(nbEventsRequested, currentModel.getLines().size());
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Given a start index, count and a list of desired columns, we check model
     * returned by the data provider. We also apply a search filter on a single
     * column with search direction PREVIOUS.
     */
    @Test
    public void testDataProviderWithSimpleSingleColumnsIndexBackwardsSearch() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);

        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);

        // Query for the index for the first matching event backwards, starting
        // at index 10
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Arrays.asList(eventTypeColumnId, timestampColumnId), 10, 1, null);
        Map<String, Object> parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);

        Map<Long, String> searchExpressions = new HashMap<>();
        searchExpressions.put(eventTypeColumnId, TYPE_2);

        parameters.put(TABLE_SEARCH_EXPRESSIONS_KEY, searchExpressions);
        parameters.put(TABLE_SEARCH_DIRECTION_KEY, PREV_DIR_UNDER_TEST);

        List<Long> expectedColumnsId = Arrays.asList(eventTypeColumnId, timestampColumnId);
        TmfTimestampFormat.getDefaulTimeFormat().format(TmfTimestamp.fromMillis(2).toNanos());
        List<EventTableLine> expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_2), new VirtualTableCell(lineTimestamp(10))), 9, TmfTimestamp.fromMillis(10), 9, 0));
        expectedData.get(0).setActiveProperties(CoreFilterProperty.HIGHLIGHT);

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(parameters, null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        TmfVirtualTableModel<@NonNull EventTableLine> expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 9, 10000);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Given a start index, count and a list of desired columns, we check model
     * returned by the data provider. We also apply a search filter on a single
     * column to search PREVIOUS from the start index. N number of events from
     * the first matched event will be returned by the data provider.
     */
    @Test
    public void testDataProviderWithGetDataFromSearchBackwardsMatch() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);

        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);

        // Query for the 3 events starting from the the first matching event
        // backwards, starting at index 10
        int nbEventsRequested = 3;
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(Arrays.asList(eventTypeColumnId, timestampColumnId), 10, nbEventsRequested, null);
        Map<String, Object> parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);

        Map<Long, String> searchExpressions = new HashMap<>();
        searchExpressions.put(eventTypeColumnId, TYPE_2);

        parameters.put(TABLE_SEARCH_EXPRESSIONS_KEY, searchExpressions);
        parameters.put(TABLE_SEARCH_DIRECTION_KEY, PREV_DIR_UNDER_TEST);

        List<Long> expectedColumnsId = Arrays.asList(eventTypeColumnId, timestampColumnId);
        TmfTimestampFormat.getDefaulTimeFormat().format(TmfTimestamp.fromMillis(2).toNanos());
        List<EventTableLine> expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_2), new VirtualTableCell(lineTimestamp(10))), 9, TmfTimestamp.fromMillis(10), 9, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_3), new VirtualTableCell(lineTimestamp(11))), 10, TmfTimestamp.fromMillis(11), 10, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(TYPE_4), new VirtualTableCell(lineTimestamp(12))), 11, TmfTimestamp.fromMillis(12), 11, 0));
        expectedData.get(0).setActiveProperties(CoreFilterProperty.HIGHLIGHT);

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(parameters, null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        TmfVirtualTableModel<@NonNull EventTableLine> expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 9, 10000);
        assertNotNull(currentModel);
        assertEquals(nbEventsRequested, currentModel.getLines().size());
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Given a start index, count and a list of desired columns, we check model
     * returned by the data provider. We also apply a search filter on multiple
     * columns
     */
    @Test
    public void testDataProviderWithSimpleMultiColumnsSearch() {
        Long eventTypeColumnId = fColumns.get(EVENT_TYPE_COLUMN_NAME);
        Long timestampColumnId = fColumns.get(TIMESTAMP_COLUMN_NAME);

        assertNotNull(timestampColumnId);
        assertNotNull(eventTypeColumnId);

        // Query for the index for the first matching event
        VirtualTableQueryFilter queryFilter = new EventTableQueryFilter(new ArrayList<>(fColumns.values()), 0, 1, null);
        Map<String, Object> parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);

        Map<Long, String> searchExpressions = new HashMap<>();
        searchExpressions.put(eventTypeColumnId, "T.*3");
        searchExpressions.put(timestampColumnId, "\\d*4\\d*s*");

        parameters.put(TABLE_SEARCH_EXPRESSIONS_KEY, searchExpressions);
        parameters.put(TABLE_SEARCH_DIRECTION_KEY, NEXT_DIR_UNDER_TEST);

        List<Long> expectedColumnsId = new ArrayList<>(fColumns.values());
        TmfTimestampFormat.getDefaulTimeFormat().format(TmfTimestamp.fromMillis(2).toNanos());
        List<EventTableLine> expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(4)), new VirtualTableCell(TYPE_3), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(4))), 3, TmfTimestamp.fromMillis(4), 3, 0));
        expectedData.get(0).setActiveProperties(CoreFilterProperty.HIGHLIGHT);

        TmfModelResponse<ITmfVirtualTableModel<EventTableLine>> response = fProvider.fetchLines(parameters, null);
        ITmfVirtualTableModel<EventTableLine> currentModel = response.getModel();

        TmfVirtualTableModel<@NonNull EventTableLine> expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 3, 10000);
        assertEquals(expectedModel, currentModel);

        // Query for events with search filter active. Matching lines will be
        // tagged for highlighting
        int nbEventsRequested = 5;
        queryFilter = new EventTableQueryFilter(new ArrayList<>(fColumns.values()), 0, nbEventsRequested, null);
        parameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        parameters.put(TABLE_SEARCH_EXPRESSIONS_KEY, searchExpressions);

        response = fProvider.fetchLines(parameters, null);
        currentModel = response.getModel();
        assertNotNull(currentModel);
        assertEquals(nbEventsRequested, currentModel.getLines().size());
        expectedData = Arrays.asList(
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(1)), new VirtualTableCell(TYPE_0), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(1))), 0, TmfTimestamp.fromMillis(1), 0, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(2)), new VirtualTableCell(TYPE_1), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(2))), 1, TmfTimestamp.fromMillis(2), 1, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(3)), new VirtualTableCell(TYPE_2), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(3))), 2, TmfTimestamp.fromMillis(3), 2, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(4)), new VirtualTableCell(TYPE_3), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(4))), 3, TmfTimestamp.fromMillis(4), 3, 0),
                new EventTableLine(Arrays.asList(new VirtualTableCell(lineTimestamp(5)), new VirtualTableCell(TYPE_4), new VirtualTableCell(""), new VirtualTableCell(lineNsTimestamp(5))), 4, TmfTimestamp.fromMillis(5), 4, 0));
        expectedData.get(3).setActiveProperties(CoreFilterProperty.HIGHLIGHT);
        expectedModel = new TmfVirtualTableModel<>(expectedColumnsId, expectedData, 0, 10000);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Sets a negative index to EventTableQueryFilter. Expected an
     * IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testQueryFilterIndexParameter() {
        new EventTableQueryFilter(Collections.emptyList(), -1, 5, null);
    }
}
