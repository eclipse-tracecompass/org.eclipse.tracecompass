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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreDensityDataProvider;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreDensityDataProviderFactory;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.model.SeriesModel;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
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
    private static final double[] yValues = new double[] { 15.0, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 15.0, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 15.0,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 15.0, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 15.0, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 15.0, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 15.0, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324 };
    private static final long[] xValues = new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5 };
    private static final long[] xValuesNull = new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0 };
    private static final double[] yValuesNull = new double[] { 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324,
            4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324, 4.9E-324 };
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
        IDataProviderFactory fp = new SegmentStoreDensityDataProviderFactory();
        assertNull(fp.createProvider(fTrace));
        assertNotNull(fp.createProvider(fTrace, ID));
        assertTrue(fp.getDescriptors(fTrace).isEmpty());
        fDataProvider = new SegmentStoreDensityDataProvider(fTrace, fixture, ID);
        StubSegmentStoreProvider fixtureNull = getValidNullSegment(fTrace);
        IDataProviderFactory fpNullSegment = new SegmentStoreDensityDataProviderFactory();
        assertNull(fpNullSegment.createProvider(fTrace));
        assertNotNull(fpNullSegment.createProvider(fTrace, ID));
        assertTrue(fpNullSegment.getDescriptors(fTrace).isEmpty());
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
     * Tests data model returned by the fetch XY
     */
    @Test()
    public void testDataProviderFetchXY() {
        TimeQueryFilter timeQueryFilter = new TimeQueryFilter(0, 100, 100);
        TmfModelResponse<@NonNull ITmfXyModel> response = fDataProvider.fetchXY(FetchParametersUtils.timeQueryToMap(timeQueryFilter), null);
        assertNotNull(response);
        ITmfXyModel responseModel = response.getModel();
        assertNotNull(responseModel);
        SeriesModel seriesResponse = (SeriesModel) responseModel.getSeriesData().toArray()[0];
        assertTrue(Arrays.equals(yValues, seriesResponse.getData()));
        assertTrue(Arrays.equals(xValues, seriesResponse.getXAxis()));
    }

    /**
     * Tests data model returned by the fetch XY
     */
    @Test()
    public void testDataProviderNullFetchXY() {
        TimeQueryFilter timeQueryFilter = new TimeQueryFilter(0, 100, 100);
        TmfModelResponse<@NonNull ITmfXyModel> response = fDataProviderNullSegments.fetchXY(FetchParametersUtils.timeQueryToMap(timeQueryFilter), null);
        assertNotNull(response);
        ITmfXyModel responseModel = response.getModel();
        assertNotNull(responseModel);
        SeriesModel seriesResponse = (SeriesModel) responseModel.getSeriesData().toArray()[0];
        assertTrue(Arrays.equals(yValuesNull, seriesResponse.getData()));
        assertTrue(Arrays.equals(xValuesNull, seriesResponse.getXAxis()));
    }

    /**
     * Tests fetch tree of the data provider
     */
    @Test()
    public void testFetchTree() {
        TimeQueryFilter timeQueryFilter = new TimeQueryFilter(0, 100, 100);
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TmfTreeDataModel>> response = fDataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(timeQueryFilter), null);
        assertNotNull(response);
    }

    /**
     * Tests provider ID
     */
    @Test
    public void testID() {
        assertTrue(fDataProvider.getId().equals(ID));
    }
}
