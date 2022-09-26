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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.VirtualTableQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.TmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.VirtualTableCell;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.VirtualTableLine;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.segmentstore.core.BasicSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.model.CoreFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.TmfExperimentStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Tests the {@link SegmentStoreTableDataProvider} with {@link TmfExperiment}
 * using {@link SegmentStoreAnalysisModule}
 *
 * @author: Kyrollos Bekhet
 */
public class SegmentStoreTableDataProviderExperimentTest {

    private static class SecondStubSegmentStoreProvider extends AbstractSegmentStoreAnalysisModule {
        private static final int SIZE = 1000;
        private final List<@NonNull ISegment> fPreFixture;

        public SecondStubSegmentStoreProvider() {
            ImmutableList.Builder<@NonNull ISegment> builder = new Builder<>();
            int previousStartTime = 0;
            for (int i = 0; i < SIZE; i++) {
                if (i % 3 == 0) {
                    previousStartTime = i;
                }
                ISegment segment = new BasicSegment(previousStartTime, i);
                builder.add(segment);
            }
            fPreFixture = builder.build();
        }

        @Override
        protected boolean buildAnalysisSegments(@NonNull ISegmentStore<@NonNull ISegment> segmentStore, @NonNull IProgressMonitor monitor) throws TmfAnalysisException {
            return segmentStore.addAll(fPreFixture);
        }

        @Override
        protected void canceling() {
            // Do nothing
        }

        @Override
        public boolean setTrace(@NonNull ITmfTrace trace) throws TmfAnalysisException {
            if (trace instanceof TmfXmlTraceStub) {
                TmfXmlTraceStub tmfXmlTraceStub = (TmfXmlTraceStub) trace;
                tmfXmlTraceStub.addAnalysisModule(this);
            }
            return super.setTrace(trace);
        }
    }

    private static ITmfVirtualTableDataProvider<@NonNull TmfTreeDataModel, @NonNull VirtualTableLine> fMainDataProvider;
    private static ITmfVirtualTableDataProvider<@NonNull TmfTreeDataModel, @NonNull VirtualTableLine> fDataProvider;
    private static ITmfVirtualTableDataProvider<@NonNull TmfTreeDataModel, @NonNull VirtualTableLine> fInvalidDataProvider;

    private static final String START_TIME_COLUMN_NAME = "Start Time";
    private static final String END_TIME_COLUMN_NAME = "End Time";
    private static final String DURATION_COLUMN_NAME = "Duration";
    private static final String TRACE_COLUMN_NAME = "Trace";
    private static final String MAIN_TRACE_NAME = "main trace";
    private static final String SECOND_TRACE_NAME = "second trace";
    private static final String TABLE_SEARCH_EXPRESSION_KEY = "table_search_expressions"; //$NON-NLS-1$
    private static final String TABLE_SEARCH_DIRECTION_KEY = "table_search_direction"; //$NON-NLS-1$
    private static final String TABLE_COMPARATOR_EXPRESSION_KEY = "table_comparator_expression"; //$NON-NLS-1$
    private static final int BLOCK_SIZE = 5000;

    private static Map<String, Long> fColumns = Collections.emptyMap();
    private static Map<String, Long> fSingleTraceColumns = Collections.emptyMap();
    private static TmfExperimentStub fExperiment;
    private static TmfExperimentStub fSingleTraceExperiment;
    private static TmfExperimentStub fInvalidExperiment;
    private static TmfXmlTraceStubNs fMainTrace;
    private static TmfXmlTraceStubNs fSecondTrace;

    /**
     * Set-up resources
     *
     * @throws TmfAnalysisException
     *             Trace exception should not happen
     */
    @BeforeClass
    public static void init() throws TmfAnalysisException {
        fMainTrace = new TmfXmlTraceStubNs();
        fMainTrace.setName(MAIN_TRACE_NAME);
        StubSegmentStoreProvider simpleFixture = new StubSegmentStoreProvider();
        assertNotNull(fMainTrace);
        simpleFixture.setTrace(fMainTrace);

        SecondStubSegmentStoreProvider secondSimpleFixture = new SecondStubSegmentStoreProvider();
        fSecondTrace = new TmfXmlTraceStubNs();
        fSecondTrace.setName(SECOND_TRACE_NAME);
        assertNotNull(fSecondTrace);
        secondSimpleFixture.setTrace(fSecondTrace);

        fExperiment = getValidExperiment();
        assertNotNull(fExperiment);
        fMainDataProvider = getDataProvider(fExperiment);

        fSingleTraceExperiment = getSingleTraceExperiment();
        assertNotNull(fSingleTraceExperiment);
        fDataProvider = getDataProvider(fSingleTraceExperiment);

        fInvalidExperiment = getInvalidExperiment();
        assertNotNull(fInvalidExperiment);
        fInvalidDataProvider = getDataProvider(fInvalidExperiment);

        fColumns = fetchColumnId();
        fSingleTraceColumns = fetchSingleTraceColumnId();
    }

    private static Map<String, Long> fetchColumnId() {
        TmfTreeModel<@NonNull TmfTreeDataModel> columns = fMainDataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 0, 1)), null).getModel();
        if (columns == null) {
            return Collections.emptyMap();
        }
        List<@NonNull TmfTreeDataModel> columnEntries = columns.getEntries();
        assertEquals(START_TIME_COLUMN_NAME, columnEntries.get(0).getName());
        assertEquals(END_TIME_COLUMN_NAME, columnEntries.get(1).getName());
        assertEquals(DURATION_COLUMN_NAME, columnEntries.get(2).getName());
        assertEquals(StubSegmentStoreProvider.STUB_COLUMN_NAME, columnEntries.get(3).getName());
        assertEquals(TRACE_COLUMN_NAME, columnEntries.get(4).getName());

        Map<String, Long> expectedColumns = new LinkedHashMap<>();
        for (TmfTreeDataModel column : columnEntries) {
            expectedColumns.put(column.getName(), column.getId());
        }
        return expectedColumns;
    }

    private static Map<String, Long> fetchSingleTraceColumnId() {
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

    private static ITmfVirtualTableDataProvider<@NonNull TmfTreeDataModel, @NonNull VirtualTableLine> getDataProvider(@NonNull TmfExperimentStub experiment) throws TmfAnalysisException {
        IAnalysisModule m = new SegmentStoreAnalysisModule(experiment);
        experiment.addAnalysisModule(m);
        m.schedule();
        return new SegmentStoreTableDataProvider(experiment, (ISegmentStoreProvider) m, "");
    }

    private static TmfExperimentStub getSingleTraceExperiment() {
        ITmfTrace[] traces = { fMainTrace };
        return new TmfExperimentStub("", traces, BLOCK_SIZE);
    }

    private static TmfExperimentStub getInvalidExperiment() {
        ITmfTrace[] traces = {};
        return new TmfExperimentStub("", traces, BLOCK_SIZE);
    }

    private static TmfExperimentStub getValidExperiment() {
        ITmfTrace[] traces = { fMainTrace, fSecondTrace };
        return new TmfExperimentStub("", traces, BLOCK_SIZE);
    }

    /**
     * Disposes resources
     */
    @AfterClass
    public static void tearDown() {
        fExperiment.dispose();
        fSingleTraceExperiment.dispose();
        fInvalidExperiment.dispose();
        if (fMainTrace != null) {
            fMainTrace.dispose();
        }
        if (fSecondTrace != null) {
            fSecondTrace.dispose();
        }
    }

    // ---------------------------------------------------------------------
    // Tests
    // ----------------------------------------------------------------------

    /**
     * Test columns returned by the provider with an experiment that has two
     * traces
     */
    @Test
    public void testMainDataProviderFetchColumn() {
        Long startTimeColumnId = fColumns.get(START_TIME_COLUMN_NAME);
        Long endTimeColumnId = fColumns.get(END_TIME_COLUMN_NAME);
        Long durationColumnId = fColumns.get(DURATION_COLUMN_NAME);
        Long customColumnId = fColumns.get(StubSegmentStoreProvider.STUB_COLUMN_NAME);
        Long traceColumnId = fColumns.get(TRACE_COLUMN_NAME);

        assertNotNull(startTimeColumnId);
        assertNotNull(endTimeColumnId);
        assertNotNull(durationColumnId);
        assertNotNull(customColumnId);
        assertNotNull(traceColumnId);

        List<@NonNull TmfTreeDataModel> expectedColumnEntries = Arrays.asList(
                new TmfTreeDataModel(startTimeColumnId, -1, Collections.singletonList(START_TIME_COLUMN_NAME)),
                new TmfTreeDataModel(endTimeColumnId, -1, Collections.singletonList(END_TIME_COLUMN_NAME)),
                new TmfTreeDataModel(durationColumnId, -1, Collections.singletonList(DURATION_COLUMN_NAME)),
                new TmfTreeDataModel(customColumnId, -1, Collections.singletonList(StubSegmentStoreProvider.STUB_COLUMN_NAME)),
                new TmfTreeDataModel(traceColumnId, -1, Collections.singletonList(TRACE_COLUMN_NAME)));

        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TmfTreeDataModel>> response = fMainDataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 0, 1)), null);
        TmfTreeModel<@NonNull TmfTreeDataModel> currentColumnModel = response.getModel();
        assertNotNull(currentColumnModel);
        List<@NonNull TmfTreeDataModel> currentColumnEntries = Objects.requireNonNull(currentColumnModel).getEntries();
        assertEquals(expectedColumnEntries, currentColumnEntries);
    }

    /**
     * Test columns returned by the provider with an experiment that has one
     * trace. This test makes sure that the columns have the same IDs as the
     * other experiments with same aspects. This means that the aspect wrapper
     * created by the {@link SegmentStoreAnalysisModule} is created only once.
     */
    @Test
    public void testSingleTraceExperimentDataProviderFetchColumn() {
        Long startTimeColumnId = fSingleTraceColumns.get(START_TIME_COLUMN_NAME);
        Long endTimeColumnId = fSingleTraceColumns.get(END_TIME_COLUMN_NAME);
        Long durationColumnId = fSingleTraceColumns.get(DURATION_COLUMN_NAME);
        Long customColumnId = fSingleTraceColumns.get(StubSegmentStoreProvider.STUB_COLUMN_NAME);

        assertNotNull(startTimeColumnId);
        assertNotNull(endTimeColumnId);
        assertNotNull(durationColumnId);
        assertNotNull(customColumnId);

        List<@NonNull TmfTreeDataModel> expectedColumnEntries = Arrays.asList(
                new TmfTreeDataModel(startTimeColumnId, -1, Collections.singletonList(START_TIME_COLUMN_NAME)),
                new TmfTreeDataModel(endTimeColumnId, -1, Collections.singletonList(END_TIME_COLUMN_NAME)),
                new TmfTreeDataModel(durationColumnId, -1, Collections.singletonList(DURATION_COLUMN_NAME)),
                new TmfTreeDataModel(customColumnId, -1, Collections.singletonList(StubSegmentStoreProvider.STUB_COLUMN_NAME)));

        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TmfTreeDataModel>> response = fDataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 0, 1)), null);
        TmfTreeModel<@NonNull TmfTreeDataModel> currentColumnModel = response.getModel();
        assertNotNull(currentColumnModel);
        List<@NonNull TmfTreeDataModel> currentColumnEntries = Objects.requireNonNull(currentColumnModel).getEntries();
        assertEquals(expectedColumnEntries, currentColumnEntries);
    }

    /**
     * Test columns returned by the provider with an invalid experiment
     */
    @Test
    public void testInvalidExperimentDataProviderFetchColumn() {
        Long startTimeColumnId = fColumns.get(START_TIME_COLUMN_NAME);
        Long endTimeColumnId = fColumns.get(END_TIME_COLUMN_NAME);
        Long durationColumnId = fColumns.get(DURATION_COLUMN_NAME);

        assertNotNull(startTimeColumnId);
        assertNotNull(endTimeColumnId);
        assertNotNull(durationColumnId);

        List<@NonNull TmfTreeDataModel> expectedColumnEntries = Arrays.asList(
                new TmfTreeDataModel(startTimeColumnId, -1, Collections.singletonList(START_TIME_COLUMN_NAME)),
                new TmfTreeDataModel(endTimeColumnId, -1, Collections.singletonList(END_TIME_COLUMN_NAME)),
                new TmfTreeDataModel(durationColumnId, -1, Collections.singletonList(DURATION_COLUMN_NAME)));

        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TmfTreeDataModel>> response = fInvalidDataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 0, 1)), null);
        TmfTreeModel<@NonNull TmfTreeDataModel> currentColumnModel = response.getModel();
        assertNotNull(currentColumnModel);
        List<@NonNull TmfTreeDataModel> currentColumnEntries = Objects.requireNonNull(currentColumnModel).getEntries();
        assertEquals(expectedColumnEntries, currentColumnEntries);
    }

    /**
     * Test lines returned by the main data provider that contain the
     * aggregation of two segment stores.
     */
    @SuppressWarnings("null")
    @Test
    public void testFetchLinesFromDataProviderWithExperiment() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 10);
        @NonNull List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(0, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(1, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(SECOND_TRACE_NAME))),
                new VirtualTableLine(2, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(1)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(3, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(1)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(SECOND_TRACE_NAME))),
                new VirtualTableLine(4, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(2)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(5, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(2)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(SECOND_TRACE_NAME))),
                new VirtualTableLine(6, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(3)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(7, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(4)), new VirtualTableCell(lineDuration(4)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(8, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(5)), new VirtualTableCell(lineDuration(5)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(9, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(6)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fMainDataProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 0, 66535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test lines returned by the data provider that don't have a segment store
     */
    @Test
    public void testFetchLinesFronmDataProviderWithEmptyExperiment() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 5);

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fInvalidDataProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNull(currentModel);
        assertEquals(response.getStatus(), ITmfResponse.Status.FAILED);
    }

    /**
     * Test lines returned by the data provider that have a single segment store
     */
    @SuppressWarnings("null")
    @Test
    public void testFetchLinesFromDataProviderWithSingleTraceExperiment() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(new ArrayList<>(fSingleTraceColumns.values()), 0, 10);

        @NonNull List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(0, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(1, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(1)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(2, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(2)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(3, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(3)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(4, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(4)), new VirtualTableCell(lineDuration(4)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(5, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(5)), new VirtualTableCell(lineDuration(5)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(6, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(6)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(7, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(8, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(8)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))),
                new VirtualTableLine(9, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(9)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(FetchParametersUtils.virtualTableQueryToMap(queryFilter), null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fSingleTraceColumns.values()), expectedData, 0, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test lines returned by the data provider with one filter applied
     */
    @SuppressWarnings("null")
    @Test
    public void testFetchLinesWithSearchOnAnExperiment() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 5);
        @NonNull Map<@NonNull String, @NonNull Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Map<Long, String> searchMap = new HashMap<>();
        searchMap.put(fColumns.get(START_TIME_COLUMN_NAME), lineTime(21));
        fetchParameters.put(TABLE_SEARCH_EXPRESSION_KEY, searchMap);
        fetchParameters.put(TABLE_SEARCH_DIRECTION_KEY, NEXT_DIR_UNDER_TEST);

        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(42, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(43, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(SECOND_TRACE_NAME))),
                new VirtualTableLine(44, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(22)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(45, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(22)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(SECOND_TRACE_NAME))),
                new VirtualTableLine(46, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(23)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))));

        expectedData.forEach(sl -> sl.setActiveProperties(CoreFilterProperty.HIGHLIGHT));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fMainDataProvider.fetchLines(fetchParameters, null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 42, 66535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test lines returned by the data provider with two search filters applied
     */
    @SuppressWarnings("null")
    @Test
    public void testFetchLinesWithTwoSearchFiltersOnAnExperiment() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 5);
        @NonNull Map<@NonNull String, @NonNull Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Map<Long, String> searchMap = new HashMap<>();
        searchMap.put(fColumns.get(START_TIME_COLUMN_NAME), lineTime(21));
        searchMap.put(fColumns.get(TRACE_COLUMN_NAME), SECOND_TRACE_NAME);
        fetchParameters.put(TABLE_SEARCH_EXPRESSION_KEY, searchMap);
        fetchParameters.put(TABLE_SEARCH_DIRECTION_KEY, NEXT_DIR_UNDER_TEST);

        List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(43, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(SECOND_TRACE_NAME))),
                new VirtualTableLine(45, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(22)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(SECOND_TRACE_NAME))),
                new VirtualTableLine(47, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(23)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(SECOND_TRACE_NAME))),
                new VirtualTableLine(46, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(22)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(47, Arrays.asList(new VirtualTableCell(lineTime(21)), new VirtualTableCell(lineTime(22)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(SECOND_TRACE_NAME))));

        expectedData.get(0).setActiveProperties(CoreFilterProperty.HIGHLIGHT);
        expectedData.get(1).setActiveProperties(CoreFilterProperty.HIGHLIGHT);
        expectedData.get(2).setActiveProperties(CoreFilterProperty.HIGHLIGHT);
        expectedData.get(4).setActiveProperties(CoreFilterProperty.HIGHLIGHT);

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fMainDataProvider.fetchLines(fetchParameters, null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 43, 66535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test lines returned by the data provider with two search filters applied
     * where the search criteria are not met
     */
    @SuppressWarnings("null")
    @Test
    public void testFetchLinesWithTwoSearchFiltersOnASingleTraceExperiment() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 5);
        @NonNull Map<@NonNull String, @NonNull Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Map<Long, String> searchMap = new HashMap<>();
        searchMap.put(fColumns.get(START_TIME_COLUMN_NAME), lineTime(21));
        searchMap.put(fColumns.get(TRACE_COLUMN_NAME), SECOND_TRACE_NAME);
        fetchParameters.put(TABLE_SEARCH_EXPRESSION_KEY, searchMap);
        fetchParameters.put(TABLE_SEARCH_DIRECTION_KEY, NEXT_DIR_UNDER_TEST);
        List<@NonNull VirtualTableLine> expectedData = Collections.emptyList();

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fDataProvider.fetchLines(fetchParameters, null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fSingleTraceColumns.values()), expectedData, 0, 65535);
        assertEquals(expectedModel, currentModel);
    }

    /**
     * Test trace name comparison on an experiment.
     */
    @SuppressWarnings("null")
    @Test
    public void testFetchLinesWithTraceNameSorting() {
        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(Collections.emptyList(), 0, 10);
        Map<@NonNull String, @NonNull Object> fetchParameters = FetchParametersUtils.virtualTableQueryToMap(queryFilter);
        Object traceNameColumnID = fColumns.get(TRACE_COLUMN_NAME);
        assertNotNull(traceNameColumnID);
        fetchParameters.put(TABLE_COMPARATOR_EXPRESSION_KEY, traceNameColumnID);

        @NonNull List<@NonNull VirtualTableLine> expectedData = Arrays.asList(
                new VirtualTableLine(0, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(1, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(1)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(2, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(2)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(3, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(3)), new VirtualTableCell(lineDuration(3)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(4, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(4)), new VirtualTableCell(lineDuration(4)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(5, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(5)), new VirtualTableCell(lineDuration(5)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(6, Arrays.asList(new VirtualTableCell(lineTime(0)), new VirtualTableCell(lineTime(6)), new VirtualTableCell(lineDuration(6)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(7, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineDuration(0)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(8, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(8)), new VirtualTableCell(lineDuration(1)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))),
                new VirtualTableLine(9, Arrays.asList(new VirtualTableCell(lineTime(7)), new VirtualTableCell(lineTime(9)), new VirtualTableCell(lineDuration(2)), new VirtualTableCell(StubSegmentStoreProvider.STUB_COLUMN_CONTENT), new VirtualTableCell(MAIN_TRACE_NAME))));

        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull VirtualTableLine>> response = fMainDataProvider.fetchLines(fetchParameters, null);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> currentModel = response.getModel();
        assertNotNull(currentModel);
        ITmfVirtualTableModel<@NonNull VirtualTableLine> expectedModel = new TmfVirtualTableModel<>(new ArrayList<>(fColumns.values()), expectedData, 0, 66535);
        assertEquals(expectedModel, currentModel);
    }

    private static String lineTime(long milliseconds) {
        return TmfTimestamp.fromNanos(milliseconds).toString();
    }

    private static String lineDuration(long duration) {
        return new DecimalFormat("###,###.##").format(duration);
    }
}
