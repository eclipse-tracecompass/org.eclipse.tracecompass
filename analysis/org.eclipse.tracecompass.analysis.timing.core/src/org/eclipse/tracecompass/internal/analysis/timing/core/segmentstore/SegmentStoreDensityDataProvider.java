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

package org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.TmfXyResponseFactory;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * This data provider will return an XY model based on a query filter. The model
 * can be used by any viewer to draw density view charts. Model returned is for
 * analysis using SegmentStore.
 *
 * @author Puru Jaiswal
 */
public class SegmentStoreDensityDataProvider extends AbstractTmfTraceDataProvider implements ITmfTreeXYDataProvider<TmfTreeDataModel> {

    /**
     * Extension point ID.
     */
    public static final String ID = "org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreDensityDataProvider"; //$NON-NLS-1$
    private static final AtomicLong TRACE_IDS = new AtomicLong();

    private final String fID;
    private final String title = Objects.requireNonNull(Messages.SegmentStoreDensityDataProvider_title);
    private final ISegmentStoreProvider fProvider;
    private final long fTotalId = TRACE_IDS.getAndIncrement();
    private final long fTraceId = TRACE_IDS.getAndIncrement();

    /**
     * Constructor
     *
     * @param trace
     *            trace provider with other properties of trace.
     * @param provider
     *            segment store provider
     * @param id
     *            analysis identifier
     */
    public SegmentStoreDensityDataProvider(ITmfTrace trace, ISegmentStoreProvider provider, String id) {
        super(trace);
        fProvider = provider;
        fID = id;
        if (provider instanceof IAnalysisModule) {
            ((IAnalysisModule) provider).waitForCompletion();
        }
    }

    @Override
    public TmfModelResponse<ITmfXyModel> fetchXY(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ISegmentStore<ISegment> segmentStore = fProvider.getSegmentStore();
        if (segmentStore == null) {
            return TmfXyResponseFactory.createFailedResponse(Objects.requireNonNull(Messages.SegmentStoreDataProvider_SegmentNotAvailable));
        }
        TimeQueryFilter queryFilter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (queryFilter == null) {
            queryFilter = FetchParametersUtils.createTimeQuery(fetchParameters);
            if (queryFilter == null) {
                return TmfXyResponseFactory.createFailedResponse(CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
            }
        }
        return getXyData(segmentStore, queryFilter);
    }

    private TmfModelResponse<ITmfXyModel> getXyData(ISegmentStore<ISegment> segmentStore, TimeQueryFilter queryFilter) {
        long startTraceTime = queryFilter.getStart();
        long endTraceTime = queryFilter.getEnd();
        int width = queryFilter.getTimesRequested().length;
        Iterable<ISegment> displayData = segmentStore.getIntersectingElements(startTraceTime, endTraceTime);

        IAnalysisModule module = (fProvider instanceof IAnalysisModule) ? (IAnalysisModule) fProvider : null;
        boolean complete = module != null && module.isQueryable(queryFilter.getEnd());

        Optional<ISegment> maxSegment = StreamSupport.stream(displayData.spliterator(), false).max(SegmentComparators.INTERVAL_LENGTH_COMPARATOR);
        long maxLength = 1;
        if (maxSegment.isPresent()) {
            maxLength = maxSegment.get().getLength();
        }

        double[] yValues = getYValues(displayData, width, maxLength);
        long[] xValues = getXValues(width, maxLength);
        ImmutableList.Builder<IYModel> builder = ImmutableList.builder();
        String totalName = getTrace().getName() + '/' + Messages.SegmentStoreDensity_TotalLabel;
        builder.add(new YModel(fTotalId, totalName, yValues));
        return TmfXyResponseFactory.create(title, xValues, builder.build(), complete);
    }

    private static long[] getXValues(int width, long maxLength) {
        double timeWidth = (double) maxLength / (double) width;
        long[] xValues = new long[width];
        for (int i = 0; i < width; i++) {
            xValues[i] = (long) (i * timeWidth);
            xValues[i] += timeWidth / 2;
        }
        return xValues;
    }

    private static double[] getYValues(Iterable<ISegment> displayData, int width, long maxLength) {
        double maxFactor = 1.0 / (maxLength + 1.0);
        double[] yValues = new double[width];
        Arrays.fill(yValues, Double.MIN_VALUE);
        for (ISegment segment : displayData) {
            double xBox = segment.getLength() * maxFactor * width;
            if (yValues[(int) xBox] < 1) {
                yValues[(int) xBox] = 1;
            } else {
                yValues[(int) xBox]++;
            }
        }
        return yValues;
    }

    @Override
    public TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        Builder<TmfTreeDataModel> builder = ImmutableList.builder();
        builder.add(new TmfTreeDataModel(fTraceId, -1, Collections.singletonList(String.valueOf(getTrace().getName()))));
        builder.add(new TmfTreeDataModel(fTotalId, fTraceId, Collections.singletonList(Objects.requireNonNull(Messages.SegmentStoreDensity_TotalLabel))));
        return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), builder.build()), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public String getId() {
        return fID;
    }
}
