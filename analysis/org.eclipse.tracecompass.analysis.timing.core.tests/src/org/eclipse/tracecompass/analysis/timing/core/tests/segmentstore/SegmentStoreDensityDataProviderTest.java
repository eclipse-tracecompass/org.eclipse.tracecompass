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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.timing.core.tests.stubs.segmentstore.StubSegmentStoreProvider;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreDensityDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@Link SegmentStoreDensityDataProvider}
 *
 * @author Puru Jaiswal
 */
public class SegmentStoreDensityDataProviderTest {

    private static ITmfTreeXYDataProvider<@NonNull TmfTreeDataModel> fDataProvider;
    private static ITmfTreeXYDataProvider<@NonNull TmfTreeDataModel> fDataProviderNullSegments;

    @NonNull
    private static final TmfXmlTraceStub fTrace = new TmfXmlTraceStubNs();
    private static final String ID = "org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore";

    /**
     * Set-up resources
     *
     * @throws TmfAnalysisException
     *             Trace exception should not happen
     */
    @BeforeClass
    public static void init() throws TmfAnalysisException {
        StubSegmentStoreProvider fixture = getValidSegment(fTrace);
        fDataProvider = new SegmentStoreDensityDataProvider(fTrace, fixture, ID);

        StubSegmentStoreProvider fixtureNull = getValidNullSegment(fTrace);
        fDataProviderNullSegments = new SegmentStoreDensityDataProvider(fTrace, fixtureNull, ID);
    }

    /**
     * Disposing resources
     */
    @AfterClass
    public static void clean() {
        fTrace.dispose();
    }

    private static @NonNull StubSegmentStoreProvider getValidSegment(@NonNull ITmfTrace trace) throws TmfAnalysisException {
        StubSegmentStoreProvider fixture = new StubSegmentStoreProvider(false);
        fixture.setTrace(trace);
        fixture.schedule();
        fixture.waitForCompletion();
        return fixture;
    }

    private static @NonNull StubSegmentStoreProvider getValidNullSegment(@NonNull ITmfTrace trace) throws TmfAnalysisException {
        StubSegmentStoreProvider fixture = new StubSegmentStoreProvider(true);
        fixture.setTrace(trace);
        fixture.schedule();
        fixture.waitForCompletion();
        return fixture;
    }

    /**
     * Tests successful TmfXyResponseFactory.create() call
     */
    @Test
    public void testSuccessfulFetchXY() {
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TmfTreeDataModel>> treeResponse = fDataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 100, 100)), null);
        assertNotNull(treeResponse);
        TmfTreeModel<@NonNull TmfTreeDataModel> treeModel = treeResponse.getModel();
        assertNotNull(treeModel);

        List<Long> itemIds = treeModel.getEntries().stream().map(TmfTreeDataModel::getId).collect(Collectors.toList());

        Map<String, Object> fetchParameters = new HashMap<>();
        fetchParameters.put(DataProviderParameterUtils.REQUESTED_TIMERANGE_KEY, Arrays.asList(1000L, 5000L, 100L));
        fetchParameters.put(DataProviderParameterUtils.REQUESTED_ITEMS_KEY, itemIds);

        TmfModelResponse<@NonNull ITmfXyModel> response = fDataProvider.fetchXY(fetchParameters, null);
        assertNotNull(response);
        assertEquals(ITmfResponse.Status.COMPLETED, response.getStatus());

        ITmfXyModel responseModel = response.getModel();
        assertNotNull(responseModel);
        assertNotNull(responseModel.getSeriesData());

        if (!responseModel.getSeriesData().isEmpty()) {
            ISeriesModel seriesModel = responseModel.getSeriesData().iterator().next();
            assertNotNull(seriesModel);
            assertNotNull(seriesModel.getData());
            assertEquals(100, seriesModel.getData().length);
            assertEquals(ISeriesModel.DisplayType.BAR, seriesModel.getDisplayType());
        }
    }

    /**
     * Tests TmfXyResponseFactory.createFailedResponse() for null filter
     */
    @Test
    public void testFailedResponseNullFilter() {
        Map<String, Object> emptyParameters = new HashMap<>();

        TmfModelResponse<@NonNull ITmfXyModel> response = fDataProvider.fetchXY(emptyParameters, null);
        assertNotNull(response);
        assertEquals(ITmfResponse.Status.FAILED, response.getStatus());
    }

    /**
     * Tests TmfXyResponseFactory.createCancelledResponse() for cancelled monitor
     */
    @Test
    public void testCancelledResponseMonitor() {
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TmfTreeDataModel>> treeResponse = fDataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 100, 100)), null);
        assertNotNull(treeResponse);
        TmfTreeModel<@NonNull TmfTreeDataModel> treeModel = treeResponse.getModel();
        assertNotNull(treeModel);

        List<Long> itemIds = treeModel.getEntries().stream().map(TmfTreeDataModel::getId).collect(Collectors.toList());

        Map<String, Object> fetchParameters = new HashMap<>();
        fetchParameters.put(DataProviderParameterUtils.REQUESTED_TIMERANGE_KEY, Arrays.asList(1000L, 5000L, 100L));
        fetchParameters.put(DataProviderParameterUtils.REQUESTED_ITEMS_KEY, itemIds);

        NullProgressMonitor cancelledMonitor = new NullProgressMonitor();
        cancelledMonitor.setCanceled(true);

        TmfModelResponse<@NonNull ITmfXyModel> response = fDataProvider.fetchXY(fetchParameters, cancelledMonitor);
        assertNotNull(response);
        assertEquals(ITmfResponse.Status.CANCELLED, response.getStatus());
    }

    /**
     * Tests null segments provider
     */
    @Test
    public void testNullSegmentsProvider() {
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TmfTreeDataModel>> treeResponse = fDataProviderNullSegments.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, 100, 100)), null);
        assertNotNull(treeResponse);
        TmfTreeModel<@NonNull TmfTreeDataModel> treeModel = treeResponse.getModel();
        assertNotNull(treeModel);

        List<Long> itemIds = treeModel.getEntries().stream().map(TmfTreeDataModel::getId).collect(Collectors.toList());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(DataProviderParameterUtils.REQUESTED_TIMERANGE_KEY, Arrays.asList(1000L, 5000L, 100L));
        parameters.put(DataProviderParameterUtils.REQUESTED_ITEMS_KEY, itemIds);

        TmfModelResponse<@NonNull ITmfXyModel> response = fDataProviderNullSegments.fetchXY(parameters, null);
        assertNotNull(response);

        ITmfXyModel responseModel = response.getModel();
        if (responseModel != null && !responseModel.getSeriesData().isEmpty()) {
            ISeriesModel seriesModel = responseModel.getSeriesData().iterator().next();
            assertNotNull(seriesModel);
            assertNotNull(seriesModel.getData());
            assertEquals(100, seriesModel.getData().length);
        }
    }

    /**
     * Tests fetch tree
     */
    @Test
    public void testFetchTree() {
        TimeQueryFilter timeQueryFilter = new TimeQueryFilter(0, 100, 100);
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TmfTreeDataModel>> response = fDataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(timeQueryFilter), null);
        assertNotNull(response);
        assertEquals(ITmfResponse.Status.COMPLETED, response.getStatus());
    }

    /**
     * Tests provider ID
     */
    @Test
    public void testID() {
        String expectedId = SegmentStoreDensityDataProvider.ID + ":" + ID;
        assertEquals(expectedId, fDataProvider.getId());
    }
}
