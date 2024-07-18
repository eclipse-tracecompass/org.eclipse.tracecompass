/**********************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsModel;
import org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore.statistics.StubTreeStatisticsAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.IDataAspect;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreStatisticsAspects.NamedStatistics;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreTreeStatisticsDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.TableColumnDescriptor;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.ITableColumnDescriptor;
import org.eclipse.tracecompass.tmf.core.model.filters.FilterTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class to verify {@link SegmentStoreTreeStatisticsDataProvider}
 *
 * @author Siwei Zhang
 */
public class SegmentStoreTreeStatisticsDataProviderTest extends AbstractSegmentStoreStatisticsDataProviderTest{

    // ------------------------------------------------------------------------
    // Test data
    // ------------------------------------------------------------------------

    private static final @NonNull List<@NonNull String> EXPECTED_HEADER_LIST = Arrays.asList("Label", "Minimum", "Maximum", "Average", "Std Dev", "Count", "Total", "Min Time Range", "Max Time Range");
    private static final @NonNull List<@NonNull DataType> EXPECTED_DATATYPE_LIST = Arrays.asList(DataType.STRING, DataType.STRING, DataType.STRING, DataType.STRING, DataType.STRING, DataType.STRING, DataType.STRING, DataType.TIME_RANGE, DataType.TIME_RANGE);
    private static final @NonNull List<@NonNull String> EXPECTED_TOOLTIP_LIST = Arrays.asList("", "", "", "", "", "", "", "", "");

    private static final List<@NonNull List<@NonNull String>> LIST_OF_EXPECTED_LABELS_FULL = Arrays.asList(
            Arrays.asList("", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[1000,1000]", "[1000,1002]"),
            Arrays.asList("Total", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[1000,1000]", "[1000,1002]"),
            Arrays.asList("child1", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[2000,2000]", "[2000,2002]"),
            Arrays.asList("grandChild11", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[4000,4000]", "[4000,4002]"),
            Arrays.asList("grandChild12", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[5000,5000]", "[5000,5002]"),
            Arrays.asList("child2", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[3000,3000]", "[3000,3002]"),
            Arrays.asList("grandChild21", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[6000,6000]", "[6000,6002]"),
            Arrays.asList("grandChild22", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[7000,7000]", "[7000,7002]"));

    private static final @NonNull List<@NonNull List<@NonNull String>> LIST_OF_EXPECTED_LABELS_SELECTION = Arrays.asList(
            Arrays.asList("Selection", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[1000,1000]", "[1000,1002]"),
            Arrays.asList("child1", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[2000,2000]", "[2000,2002]"),
            Arrays.asList("grandChild11", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[4000,4000]", "[4000,4002]"),
            Arrays.asList("grandChild12", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[5000,5000]", "[5000,5002]"),
            Arrays.asList("child2", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[3000,3000]", "[3000,3002]"),
            Arrays.asList("grandChild21", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[6000,6000]", "[6000,6002]"),
            Arrays.asList("grandChild22", "0", "2 ns", "1 ns", "1 ns", "3", "3 ns", "[7000,7000]", "[7000,7002]"));

    private static final @NonNull List<@NonNull StatisticsHolder> EXPECTED_STATS_FULL = Arrays.asList(
            new StatisticsHolder("", 0, -1, 0, 2, 1.0, 1.0, 3, 3.0, 1000, 1000, 1000, 1002),
            new StatisticsHolder("Total", 3, 0, 0, 2, 1.0, 1.0, 3, 3.0, 1000, 1000, 1000, 1002),
            new StatisticsHolder("child1", 4, 3, 0, 2, 1.0, 1.0, 3, 3.0, 2000, 2000, 2000, 2002),
            new StatisticsHolder("grandChild11", 5, 4, 0, 2, 1.0, 1.0, 3, 3.0, 4000, 4000, 4000, 4002),
            new StatisticsHolder("grandChild12", 6, 4, 0, 2, 1.0, 1.0, 3, 3.0, 5000, 5000, 5000, 5002),
            new StatisticsHolder("child2", 7, 3, 0, 2, 1.0, 1.0, 3, 3.0, 3000, 3000, 3000, 3002),
            new StatisticsHolder("grandChild21", 8, 7, 0, 2, 1.0, 1.0, 3, 3.0, 6000, 6000, 6000, 6002),
            new StatisticsHolder("grandChild22", 9, 7, 0, 2, 1.0, 1.0, 3, 3.0, 7000, 7000, 7000, 7002));

    private static final @NonNull List<@NonNull StatisticsHolder> EXPECTED_STATS_SELECTION = Arrays.asList(
            new StatisticsHolder("Selection", 10, 0, 0, 2, 1.0, 1.0, 3, 3.0, 1000, 1000, 1000, 1002),
            new StatisticsHolder("child1", 11, 10, 0, 2, 1.0, 1.0, 3, 3.0, 2000, 2000, 2000, 2002),
            new StatisticsHolder("grandChild11", 12, 11, 0, 2, 1.0, 1.0, 3, 3.0, 4000, 4000, 4000, 4002),
            new StatisticsHolder("grandChild12", 13, 11, 0, 2, 1.0, 1.0, 3, 3.0, 5000, 5000, 5000, 5002),
            new StatisticsHolder("child2", 14, 10, 0, 2, 1.0, 1.0, 3, 3.0, 3000, 3000, 3000, 3002),
            new StatisticsHolder("grandChild21", 15, 14, 0, 2, 1.0, 1.0, 3, 3.0, 6000, 6000, 6000, 6002),
            new StatisticsHolder("grandChild22", 16, 14, 0, 2, 1.0, 1.0, 3, 3.0, 7000, 7000, 7000, 7002));

    private static final List<@NonNull List<@NonNull String>> LIST_OF_EXPECTED_LABELS_WITH_MAPPER_FULL = Arrays.asList(
            Arrays.asList("My", "0", "2", "1.0", "1.0", "3", "3.0", "[1000,1000]", "[1000,1002]"),
            Arrays.asList("MyTotal", "0", "2", "1.0", "1.0", "3", "3.0", "[1000,1000]", "[1000,1002]"),
            Arrays.asList("Mychild1", "0", "2", "1.0", "1.0", "3", "3.0", "[2000,2000]", "[2000,2002]"),
            Arrays.asList("MygrandChild11", "0", "2", "1.0", "1.0", "3", "3.0", "[4000,4000]", "[4000,4002]"),
            Arrays.asList("MygrandChild12", "0", "2", "1.0", "1.0", "3", "3.0", "[5000,5000]", "[5000,5002]"),
            Arrays.asList("Mychild2", "0", "2", "1.0", "1.0", "3", "3.0", "[3000,3000]", "[3000,3002]"),
            Arrays.asList("MygrandChild21", "0", "2", "1.0", "1.0", "3", "3.0", "[6000,6000]", "[6000,6002]"),
            Arrays.asList("MygrandChild22", "0", "2", "1.0", "1.0", "3", "3.0", "[7000,7000]", "[7000,7002]"));

    private static final @NonNull List<@NonNull StatisticsHolder> EXPECTED_STATS_WITH_MAPPER_FULL = Arrays.asList(
            new StatisticsHolder("My", 1, -1, 0, 2, 1.0, 1.0, 3, 3.0, 1000, 1000, 1000, 1002),
            new StatisticsHolder("MyTotal", 17, 1, 0, 2, 1.0, 1.0, 3, 3.0, 1000, 1000, 1000, 1002),
            new StatisticsHolder("Mychild1", 18, 17, 0, 2, 1.0, 1.0, 3, 3.0, 2000, 2000, 2000, 2002),
            new StatisticsHolder("MygrandChild11", 19, 18, 0, 2, 1.0, 1.0, 3, 3.0, 4000, 4000, 4000, 4002),
            new StatisticsHolder("MygrandChild12", 20, 18, 0, 2, 1.0, 1.0, 3, 3.0, 5000, 5000, 5000, 5002),
            new StatisticsHolder("Mychild2", 21, 17, 0, 2, 1.0, 1.0, 3, 3.0, 3000, 3000, 3000, 3002),
            new StatisticsHolder("MygrandChild21", 22, 21, 0, 2, 1.0, 1.0, 3, 3.0, 6000, 6000, 6000, 6002),
            new StatisticsHolder("MygrandChild22", 23, 21, 0, 2, 1.0, 1.0, 3, 3.0, 7000, 7000, 7000, 7002));

    private static final String USER_DEFINED_EXTRA_HEADER = "userDefinedHeader";
    private static final String USER_DEFINED_EXTRA_VALUE = "userDefinedValue";
    private static final @NonNull DataType USER_DEFINED_DATATYPE = DataType.STRING;
    private static final @NonNull List<@NonNull String> EXPECTED_HEADER_LIST_USER_DEFINED = Stream.concat(EXPECTED_HEADER_LIST.stream(), Stream.of(USER_DEFINED_EXTRA_HEADER)).collect(Collectors.toList());
    private static final @NonNull List<@NonNull DataType> EXPECTED_DATATYPE_LIST_USER_DEFINED = Stream.concat(EXPECTED_DATATYPE_LIST.stream(), Stream.of(USER_DEFINED_DATATYPE)).collect(Collectors.toList());
    private static final @NonNull List<@NonNull String> EXPECTED_TOOLTIP_LIST_USER_DEFINED = Stream.concat(EXPECTED_TOOLTIP_LIST.stream(), Stream.of("")).collect(Collectors.toList());
    private static final List<@NonNull List<@NonNull String>> LIST_OF_EXPECTED_LABELS_FULL_USER_DEFINED = LIST_OF_EXPECTED_LABELS_FULL.stream()
            .map(list -> Stream.concat(list.stream(), Stream.of(USER_DEFINED_EXTRA_VALUE))
                    .collect(Collectors.toList()))
            .collect(Collectors.toList());
    private static final @NonNull List<@NonNull StatisticsHolderUserDefined> EXPECTED_STATS_FULL_USER_DEFINED = Arrays.asList(
            new StatisticsHolderUserDefined("", 2, -1, 0, 2, 1.0, 1.0, 3, 3.0, 1000, 1000, 1000, 1002, USER_DEFINED_EXTRA_VALUE),
            new StatisticsHolderUserDefined("Total", 24, 2, 0, 2, 1.0, 1.0, 3, 3.0, 1000, 1000, 1000, 1002, USER_DEFINED_EXTRA_VALUE),
            new StatisticsHolderUserDefined("child1", 25, 24, 0, 2, 1.0, 1.0, 3, 3.0, 2000, 2000, 2000, 2002, USER_DEFINED_EXTRA_VALUE),
            new StatisticsHolderUserDefined("grandChild11", 26, 25, 0, 2, 1.0, 1.0, 3, 3.0, 4000, 4000, 4000, 4002, USER_DEFINED_EXTRA_VALUE),
            new StatisticsHolderUserDefined("grandChild12", 27, 25, 0, 2, 1.0, 1.0, 3, 3.0, 5000, 5000, 5000, 5002, USER_DEFINED_EXTRA_VALUE),
            new StatisticsHolderUserDefined("child2", 28, 24, 0, 2, 1.0, 1.0, 3, 3.0, 3000, 3000, 3000, 3002, USER_DEFINED_EXTRA_VALUE),
            new StatisticsHolderUserDefined("grandChild21", 29, 28, 0, 2, 1.0, 1.0, 3, 3.0, 6000, 6000, 6000, 6002, USER_DEFINED_EXTRA_VALUE),
            new StatisticsHolderUserDefined("grandChild22", 30, 28, 0, 2, 1.0, 1.0, 3, 3.0, 7000, 7000, 7000, 7002, USER_DEFINED_EXTRA_VALUE));

    private static List<ITableColumnDescriptor> fExpectedDescriptors;
    private static List<ITableColumnDescriptor> fExpectedDescriptorsUserDefined;

    private static SegmentStoreTreeStatisticsDataProvider fTestDataProvider;
    private static SegmentStoreTreeStatisticsDataProvider fTestDataProvider2;
    private static SegmentStoreTreeStatisticsDataProvider fTestDataProviderWithUserDefinedAspect;

    private static TmfXmlTraceStub fTrace;

    // ------------------------------------------------------------------------
    // Test setup and cleanup
    // ------------------------------------------------------------------------
    /**
     * Test class setup
     */
    @BeforeClass
    public static void init() {
        resetIds();
        fExpectedDescriptors = new ArrayList<>();
        for (int i = 0; i < EXPECTED_HEADER_LIST.size(); i++) {
            fExpectedDescriptors.add(new TableColumnDescriptor.Builder()
                    .setText(EXPECTED_HEADER_LIST.get(i))
                    .setTooltip(EXPECTED_TOOLTIP_LIST.get(i))
                    .setDataType(EXPECTED_DATATYPE_LIST.get(i))
                    .build());
        }
        fExpectedDescriptorsUserDefined = new ArrayList<>();
        for (int i = 0; i < EXPECTED_HEADER_LIST_USER_DEFINED.size(); i++) {
            fExpectedDescriptorsUserDefined.add(new TableColumnDescriptor.Builder()
                    .setText(EXPECTED_HEADER_LIST_USER_DEFINED.get(i))
                    .setTooltip(EXPECTED_TOOLTIP_LIST_USER_DEFINED.get(i))
                    .setDataType(EXPECTED_DATATYPE_LIST_USER_DEFINED.get(i))
                    .build());
        }

        fTrace = new TmfXmlTraceStubNs();
        StubTreeStatisticsAnalysis analysis = new StubTreeStatisticsAnalysis();
        ITmfTrace trace = fTrace;
        assertNotNull(trace);
        fTestDataProvider = new SegmentStoreTreeStatisticsDataProvider(trace, analysis, "org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore");
        fTestDataProvider2 = new SegmentStoreTreeStatisticsDataProvider(trace, analysis, "org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore");
        fTestDataProvider2.setLabelMapper(e -> "My" + e);
        fTestDataProvider2.setMapper(String::valueOf);
        @NonNull IDataAspect<@NonNull NamedStatistics> userDefinedAspect = new IDataAspect<>() {
            @Override
            public String getName() {
                return USER_DEFINED_EXTRA_HEADER;
            }

            @Override
            public @Nullable Object apply(NamedStatistics input) {
                return USER_DEFINED_EXTRA_VALUE;
            }
        };
        fTestDataProviderWithUserDefinedAspect = new SegmentStoreTreeStatisticsDataProvider(trace, analysis, "org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore", Arrays.asList(userDefinedAspect));
    }

    /**
     * Test class clean-up
     */
    @AfterClass
    public static void cleanup() {
        if (fTestDataProvider != null) {
            fTestDataProvider.dispose();
        }

        if (fTestDataProvider2 != null) {
            fTestDataProvider2.dispose();
        }

        if (fTestDataProviderWithUserDefinedAspect != null) {
            fTestDataProviderWithUserDefinedAspect.dispose();
        }

        if (fTrace != null) {
            fTrace.dispose();
        }
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------

    /**
     * Test to verify
     * {@link SegmentStoreTreeStatisticsDataProvider#fetchTree(Map, org.eclipse.core.runtime.IProgressMonitor)}
     * for the full trace
     */
    @Test
    public void testFetchTreeFullRange() {
        Map<@NonNull String, @NonNull Object> fetchParameters = new HashMap<>();
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull SegmentStoreStatisticsModel>> response = fTestDataProvider.fetchTree(fetchParameters, new NullProgressMonitor());
        assertNotNull(response);

        TmfTreeModel<@NonNull SegmentStoreStatisticsModel> treeModel = response.getModel();
        assertNotNull(treeModel);

        assertEquals("Header list size", EXPECTED_HEADER_LIST.size(), treeModel.getHeaders().size());
        assertEquals("Header list", EXPECTED_HEADER_LIST, treeModel.getHeaders());

        List<@NonNull ITableColumnDescriptor> columnDescriptors = treeModel.getColumnDescriptors();
        assertEquals("Header descriptor list size", EXPECTED_HEADER_LIST.size(), columnDescriptors.size());

        assertEquals("Column descriptor list", fExpectedDescriptors, columnDescriptors);

        assertNull("Scope", treeModel.getScope());

        List<@NonNull SegmentStoreStatisticsModel> entries = treeModel.getEntries();
        assertNotNull("Entries", entries);

        verifyEntries(LIST_OF_EXPECTED_LABELS_FULL,
                EXPECTED_STATS_FULL,
                entries,
                0,
                EXPECTED_STATS_FULL.size());
    }

    /**
     * Test to verify
     * {@link SegmentStoreTreeStatisticsDataProvider#fetchTree(Map, org.eclipse.core.runtime.IProgressMonitor)}
     * for a specific time range
     */
    @Test
    public void testFetchTreeSpecificRange() {
        long start = 1024;
        long end = 4096;
        FilterTimeQueryFilter filter = new FilterTimeQueryFilter(start, end, 2, true);
        Map<@NonNull String, @NonNull Object> fetchParameters = FetchParametersUtils.filteredTimeQueryToMap(filter);
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull SegmentStoreStatisticsModel>> response = fTestDataProvider.fetchTree(fetchParameters, new NullProgressMonitor());
        assertNotNull(response);

        TmfTreeModel<@NonNull SegmentStoreStatisticsModel> treeModel = response.getModel();
        assertNotNull(treeModel);

        assertEquals("Header list size", EXPECTED_HEADER_LIST.size(), treeModel.getHeaders().size());
        assertEquals("Header list", EXPECTED_HEADER_LIST, treeModel.getHeaders());

        List<@NonNull ITableColumnDescriptor> columnDescriptors = treeModel.getColumnDescriptors();
        assertEquals("Header descriptor list size", EXPECTED_HEADER_LIST.size(), columnDescriptors.size());

        assertEquals("Column descriptor list", fExpectedDescriptors, columnDescriptors);

        assertNull("Scope", treeModel.getScope());

        List<@NonNull SegmentStoreStatisticsModel> entries = treeModel.getEntries();
        assertNotNull("Entries", entries);

        verifyEntries(LIST_OF_EXPECTED_LABELS_FULL,
                EXPECTED_STATS_FULL,
                entries,
                0,
                EXPECTED_STATS_FULL.size() + EXPECTED_STATS_SELECTION.size());
        verifyEntries(LIST_OF_EXPECTED_LABELS_SELECTION,
                EXPECTED_STATS_SELECTION,
                entries,
                LIST_OF_EXPECTED_LABELS_FULL.size(),
                EXPECTED_STATS_FULL.size() + EXPECTED_STATS_SELECTION.size());
    }

    /**
     * Test to verify
     * {@link SegmentStoreTreeStatisticsDataProvider#fetchTree(Map, org.eclipse.core.runtime.IProgressMonitor)}
     * for the full trace with mappers
     */
    @Test
    public void testFetchTreeWithMapperFullRange() {
        Map<@NonNull String, @NonNull Object> fetchParameters = new HashMap<>();
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull SegmentStoreStatisticsModel>> response = fTestDataProvider2.fetchTree(fetchParameters, new NullProgressMonitor());
        assertNotNull(response);

        TmfTreeModel<@NonNull SegmentStoreStatisticsModel> treeModel = response.getModel();
        assertNotNull(treeModel);

        assertEquals("Header list size", EXPECTED_HEADER_LIST.size(), treeModel.getHeaders().size());
        assertEquals("Header list", EXPECTED_HEADER_LIST, treeModel.getHeaders());

        List<@NonNull ITableColumnDescriptor> columnDescriptors = treeModel.getColumnDescriptors();
        assertEquals("Header descriptor list size", EXPECTED_HEADER_LIST.size(), columnDescriptors.size());

        assertEquals("Column descriptor list", fExpectedDescriptors, columnDescriptors);

        assertNull("Scope", treeModel.getScope());

        List<@NonNull SegmentStoreStatisticsModel> entries = treeModel.getEntries();
        assertNotNull("Entries", entries);

        verifyEntries(LIST_OF_EXPECTED_LABELS_WITH_MAPPER_FULL,
                EXPECTED_STATS_WITH_MAPPER_FULL,
                entries,
                0,
                EXPECTED_STATS_WITH_MAPPER_FULL.size());
    }

    /**
     * Test to verify {@link SegmentStoreTreeStatisticsDataProvider} with user
     * defined aspects for the full trace
     */
    @Test
    public void testFetchTreeWithUserDefinedAspectsFullRange() {
        Map<@NonNull String, @NonNull Object> fetchParameters = new HashMap<>();
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull SegmentStoreStatisticsModel>> response = fTestDataProviderWithUserDefinedAspect.fetchTree(fetchParameters, new NullProgressMonitor());
        assertNotNull(response);

        TmfTreeModel<@NonNull SegmentStoreStatisticsModel> treeModel = response.getModel();
        assertNotNull(treeModel);

        assertEquals("Header list size", EXPECTED_HEADER_LIST_USER_DEFINED.size(), treeModel.getHeaders().size());
        assertEquals("Header list", EXPECTED_HEADER_LIST_USER_DEFINED, treeModel.getHeaders());

        List<@NonNull ITableColumnDescriptor> columnDescriptors = treeModel.getColumnDescriptors();
        assertEquals("Header descriptor list size", EXPECTED_HEADER_LIST_USER_DEFINED.size(), columnDescriptors.size());

        assertEquals("Column descriptor list", fExpectedDescriptorsUserDefined, columnDescriptors);

        assertNull("Scope", treeModel.getScope());

        List<@NonNull SegmentStoreStatisticsModel> entries = treeModel.getEntries();
        assertNotNull("Entries", entries);

        verifyEntriesWithUserDefinedAspect(LIST_OF_EXPECTED_LABELS_FULL_USER_DEFINED,
                EXPECTED_STATS_FULL_USER_DEFINED,
                entries,
                0,
                EXPECTED_STATS_FULL_USER_DEFINED.size());
    }
}
