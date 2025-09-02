/**********************************************************************
 * Copyright (c) 2020, 2024 Ericsson
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsModel;
import org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore.statistics.StubSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.IDataAspect;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreStatisticsAspects.NamedStatistics;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.TableColumnDescriptor;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.model.ITableColumnDescriptor;
import org.eclipse.tracecompass.tmf.core.model.filters.FilterTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class to verify {@link SegmentStoreStatisticsDataProvider}
 *
 * @author Bernd Hufmann
 * @author Siwei Zhang
 */
public class SegmentStoreStatisticsDataProviderTest extends AbstractSegmentStoreStatisticsDataProviderTest {

    // ------------------------------------------------------------------------
    // Test data
    // ------------------------------------------------------------------------

    private static final @NonNull List<@NonNull String> EXPECTED_HEADER_LIST = Arrays.asList("Label", "Minimum", "Maximum", "Average", "Std Dev", "Count", "Total", "Min Time Range", "Max Time Range");
    private static final @NonNull List<@NonNull DataType> EXPECTED_DATATYPE_LIST = Arrays.asList(DataType.STRING, DataType.STRING, DataType.STRING, DataType.STRING, DataType.STRING, DataType.STRING, DataType.STRING, DataType.TIME_RANGE, DataType.TIME_RANGE);
    private static final @NonNull List<@NonNull String> EXPECTED_TOOLTIP_LIST = Arrays.asList("", "", "", "", "", "", "", "", "");

    private static final List<@NonNull List<@NonNull String>> LIST_OF_EXPECTED_LABELS_FULL = Arrays.asList(
            Arrays.asList("", "0", "65.534 µs", "32.767 µs", "18.918 µs", "65535", "2.147 s", "[0,0]", "[65534,131068]"),
            Arrays.asList("Total", "0", "65.534 µs", "32.767 µs", "18.918 µs", "65535", "2.147 s", "[0,0]", "[65534,131068]"),
            Arrays.asList("even", "0", "65.534 µs", "32.767 µs", "18.919 µs", "32768", "1.074 s", "[0,0]", "[65534,131068]"),
            Arrays.asList("odd", "1 ns", "65.533 µs", "32.767 µs", "18.918 µs", "32767", "1.074 s", "[1,2]", "[65533,131066]"));

    private static final @NonNull List<@NonNull List<@NonNull String>> LIST_OF_EXPECTED_LABELS_SELECTION = Arrays.asList(
            Arrays.asList("Selection", "512 ns", "4.096 µs", "2.304 µs", "1.035 µs", "3585", "8.26 ms", "[512,1024]", "[4096,8192]"),
            Arrays.asList("even", "512 ns", "4.096 µs", "2.304 µs", "1.035 µs", "1793", "4.131 ms", "[512,1024]", "[4096,8192]"),
            Arrays.asList("odd", "513 ns", "4.095 µs", "2.304 µs", "1.035 µs", "1792", "4.129 ms", "[513,1026]", "[4095,8190]"));

    private static final @NonNull List<@NonNull StatisticsHolder> EXPECTED_STATS_FULL = Arrays.asList(
            new StatisticsHolder("", 0, -1, 0, 65534, 32767.0, 18918.46, 65535, 2147385345.0, 0, 0, 65534, 131068),
            new StatisticsHolder("Total", 4, 0, 0, 65534, 32767.0, 18918.46, 65535, 2147385345.0, 0, 0, 65534, 131068),
            new StatisticsHolder("even", 5, 4, 0, 65534, 32767.0, 18918.90, 32768, 1073709056.0, 0, 0, 65534, 131068),
            new StatisticsHolder("odd", 6, 4, 1, 65533, 32767.0, 18918.32, 32767, 1073676289.0, 1, 2, 65533, 131066));

    private static final @NonNull List<@NonNull StatisticsHolder> EXPECTED_STATS_SELECTION = Arrays.asList(
            new StatisticsHolder("Selection", 7, 0, 512, 4096, 2304.0, 1035.04, 3585, 8259840.0, 512, 1024, 4096, 8192),
            new StatisticsHolder("even", 8, 7, 512, 4096, 2304.0, 1035.48, 1793, 4131072.0, 512, 1024, 4096, 8192),
            new StatisticsHolder("odd", 9, 7, 513, 4095, 2304.0, 1034.9, 1792, 4128768.0, 513, 1026, 4095, 8190));

    private static final List<@NonNull List<@NonNull String>> LIST_OF_EXPECTED_LABELS_WITH_MAPPER_FULL = Arrays.asList(
            Arrays.asList("My", "0", "65534", "32767.0", "18918.46928268775", "65535", "2.147385345E9", "[0,0]", "[65534,131068]"),
            Arrays.asList("MyTotal", "0", "65534", "32767.0", "18918.46928268775", "65535", "2.147385345E9", "[0,0]", "[65534,131068]"),
            Arrays.asList("Myeven", "0", "65534", "32767.0", "18918.90229373787", "32768", "1.073709056E9", "[0,0]", "[65534,131068]"),
            Arrays.asList("Myodd", "1", "65533", "32767.0", "18918.32494346861", "32767", "1.073676289E9", "[1,2]", "[65533,131066]"));

    private static final @NonNull List<@NonNull StatisticsHolder> EXPECTED_STATS_WITH_MAPPER_FULL = Arrays.asList(
            new StatisticsHolder("My", 1, -1, 0, 65534, 32767.0, 18918.46, 65535, 2147385345.0, 0, 0, 65534, 131068),
            new StatisticsHolder("MyTotal", 10, 1, 0, 65534, 32767.0, 18918.46, 65535, 2147385345.0, 0, 0, 65534, 131068),
            new StatisticsHolder("Myeven", 11, 10, 0, 65534, 32767.0, 18918.90, 32768, 1073709056.0, 0, 0, 65534, 131068),
            new StatisticsHolder("Myodd", 12, 10, 1, 65533, 32767.0, 18918.32, 32767, 1073676289.0, 1, 2, 65533, 131066));

    private static final String USER_DEFINED_EXTRA_HEADER = "userDefinedHeader";
    private static final String USER_DEFINED_EXTRA_VALUE = "userDefinedValue";
    private static final DataType USER_DEFINED_DATATYPE = DataType.STRING;
    private static final @NonNull List<@NonNull String> EXPECTED_HEADER_LIST_USER_DEFINED = Stream.concat(EXPECTED_HEADER_LIST.stream(), Stream.of(USER_DEFINED_EXTRA_HEADER)).collect(Collectors.toList());
    private static final @NonNull List<@NonNull DataType> EXPECTED_DATATYPE_LIST_USER_DEFINED = Stream.concat(EXPECTED_DATATYPE_LIST.stream(), Stream.of(USER_DEFINED_DATATYPE)).collect(Collectors.toList());
    private static final @NonNull List<@NonNull String> EXPECTED_TOOLTIP_LIST_USER_DEFINED = Stream.concat(EXPECTED_TOOLTIP_LIST.stream(), Stream.of("")).collect(Collectors.toList());
    private static final List<@NonNull List<@NonNull String>> LIST_OF_EXPECTED_LABELS_FULL_USER_DEFINED = LIST_OF_EXPECTED_LABELS_FULL.stream()
            .map(list -> Stream.concat(list.stream(), Stream.of(USER_DEFINED_EXTRA_VALUE))
                    .collect(Collectors.toList()))
            .collect(Collectors.toList());
    private static final @NonNull List<@NonNull StatisticsHolderUserDefined> EXPECTED_STATS_FULL_USER_DEFINED = Arrays.asList(
            new StatisticsHolderUserDefined("", 2, -1, 0, 65534, 32767.0, 18918.46, 65535, 2147385345.0, 0, 0, 65534, 131068, USER_DEFINED_EXTRA_VALUE),
            new StatisticsHolderUserDefined("Total", 13, 2, 0, 65534, 32767.0, 18918.46, 65535, 2147385345.0, 0, 0, 65534, 131068, USER_DEFINED_EXTRA_VALUE),
            new StatisticsHolderUserDefined("even", 14, 13, 0, 65534, 32767.0, 18918.90, 32768, 1073709056.0, 0, 0, 65534, 131068, USER_DEFINED_EXTRA_VALUE),
            new StatisticsHolderUserDefined("odd", 15, 13, 1, 65533, 32767.0, 18918.32, 32767, 1073676289.0, 1, 2, 65533, 131066, USER_DEFINED_EXTRA_VALUE));

    private static List<ITableColumnDescriptor> fExpectedDescriptors;
    private static List<ITableColumnDescriptor> fExpectedDescriptorsUserDefined;

    private static SegmentStoreStatisticsDataProvider fTestDataProvider;
    private static SegmentStoreStatisticsDataProvider fTestDataProvider2;
    private static SegmentStoreStatisticsDataProvider fTestDataProviderWithUserDefinedAspect;
    private static SegmentStoreStatisticsDataProvider fTestDataProviderWithModuleError;

    private static TmfXmlTraceStub fTrace;

    // ------------------------------------------------------------------------
    // Test setup and cleanup
    // ------------------------------------------------------------------------
    /**
     * Test class setup
     *
     * @throws TmfAnalysisException
     *             thrown when analysis fails
     */
    @BeforeClass
    public static void init() throws TmfAnalysisException {
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
        @NonNull
        StubSegmentStatisticsAnalysis fixture = getValidSegmentStats(fTrace);
        ITmfTrace trace = fTrace;
        assertNotNull(trace);
        fTestDataProvider = new SegmentStoreStatisticsDataProvider(trace, fixture, "org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore");
        fTestDataProvider2 = new SegmentStoreStatisticsDataProvider(trace, fixture, "org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore");
        fTestDataProvider2.setLabelMapper(e -> "My" + e);
        fTestDataProvider2.setMapper(String::valueOf);
        @NonNull IDataAspect<@NonNull NamedStatistics> userDefinedAspect = new IDataAspect<@NonNull NamedStatistics>() {
            @Override
            public String getName() {
                return USER_DEFINED_EXTRA_HEADER;
            }

            @Override
            public @Nullable Object apply(NamedStatistics input) {
                return USER_DEFINED_EXTRA_VALUE;
            }
        };
        fTestDataProviderWithUserDefinedAspect = new SegmentStoreStatisticsDataProvider(trace, fixture, "org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore", Arrays.asList(userDefinedAspect));

        fixture = getValidSegmentStats(fTrace, true);
        fTestDataProviderWithModuleError = new SegmentStoreStatisticsDataProvider(trace, fixture, "org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore");

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

        if (fTestDataProviderWithModuleError != null) {
            fTestDataProviderWithModuleError.dispose();
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
     * {@link SegmentStoreStatisticsDataProvider#fetchTree(Map, org.eclipse.core.runtime.IProgressMonitor)}
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
     * {@link SegmentStoreStatisticsDataProvider#fetchTree(Map, org.eclipse.core.runtime.IProgressMonitor)}
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
     * {@link SegmentStoreStatisticsDataProvider#fetchTree(Map, org.eclipse.core.runtime.IProgressMonitor)}
     * for the full trace
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
     * Test to verify {@link SegmentStoreStatisticsDataProvider} with user
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

    /**
     * Verify that data provider query returns FAILED if analysis failed.
     */
    @Test
    public void testModuleExecutionError() {
        long start = 1024;
        long end = 4096;
        FilterTimeQueryFilter filter = new FilterTimeQueryFilter(start, end, 2, true);
        Map<@NonNull String, @NonNull Object> fetchParameters = FetchParametersUtils.filteredTimeQueryToMap(filter);
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull SegmentStoreStatisticsModel>> response = fTestDataProviderWithModuleError.fetchTree(fetchParameters, new NullProgressMonitor());
        assertNotNull(response);
        assertEquals(ITmfResponse.Status.FAILED, response.getStatus());
        TmfTreeModel<@NonNull SegmentStoreStatisticsModel> treeModel = response.getModel();
        assertNull(treeModel);
    }


    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private static @NonNull StubSegmentStatisticsAnalysis getValidSegmentStats(@NonNull ITmfTrace trace) throws TmfAnalysisException {
        return getValidSegmentStats(trace, false);
    }

    private static @NonNull StubSegmentStatisticsAnalysis getValidSegmentStats(@NonNull ITmfTrace trace, boolean moduleError) throws TmfAnalysisException {
        StubSegmentStatisticsAnalysis fixture = new StubSegmentStatisticsAnalysis() {
            @Override
            public boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {
                if (moduleError) {
                    throw new TmfAnalysisException("Failure");
                }
                return super.executeAnalysis(monitor);
            }
        };
        fixture.setTrace(trace);
        fixture.getDependentAnalyses();
        fixture.schedule();
        fixture.waitForCompletion();
        return fixture;
    }
}
