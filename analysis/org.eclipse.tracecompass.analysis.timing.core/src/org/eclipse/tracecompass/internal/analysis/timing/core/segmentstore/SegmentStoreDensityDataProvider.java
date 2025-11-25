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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IGroupingSegmentAspect;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.TmfXyResponseFactory;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.segmentstore.core.segment.interfaces.INamedSegment;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.ISampling;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel.DisplayType;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

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
    private static final String GROUP_PREFIX = "group"; //$NON-NLS-1$

    private final String fID;
    private final ISegmentStoreProvider fProvider;
    private final long fTotalId = TRACE_IDS.getAndIncrement();
    private final long fTraceId = TRACE_IDS.getAndIncrement();
    private Iterable<IGroupingSegmentAspect> fGroupingAspects;
    private final BiMap<Long, String> fIdToType = HashBiMap.create();

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
        fGroupingAspects = Iterables.filter(provider.getSegmentAspects(), IGroupingSegmentAspect.class);
        fID = ID + DataProviderConstants.ID_SEPARATOR + id;
    }

    @Override
    public TmfModelResponse<ITmfXyModel> fetchXY(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQueryWithSamples(fetchParameters);
        if (filter == null) {
            return TmfXyResponseFactory.createFailedResponse(CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }

        IAnalysisModule module = (fProvider instanceof IAnalysisModule) ? (IAnalysisModule) fProvider : null;
        if (module != null) {
            if (monitor != null) {
                module.waitForCompletion(monitor);
            }
            if (monitor != null && monitor.isCanceled()) {
                return TmfXyResponseFactory.createCancelledResponse(CommonStatusMessage.TASK_CANCELLED);
            }
        }

        Pair<ISampling, Collection<IYModel>> pair = getXAxisAndYSeriesModels(fetchParameters, monitor);
        if (pair == null) {
            return TmfXyResponseFactory.createCancelledResponse(CommonStatusMessage.TASK_CANCELLED);
        }

        boolean complete = module == null || module.isQueryable(filter.getEnd());
        return TmfXyResponseFactory.create(
                getTitle(),
                pair.getFirst(),
                ImmutableList.copyOf(pair.getSecond()),
                getDisplayType(),
                getXAxisDescription(),
                complete);
    }

    @Override
    public TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ISegmentStoreProvider provider = fProvider;
        boolean waitResult = true;
        if (provider instanceof IAnalysisModule) {
            IAnalysisModule module = (IAnalysisModule) provider;
            IProgressMonitor mon = monitor != null ? monitor : new NullProgressMonitor();
            waitResult = module.waitForCompletion(mon);
            if (mon.isCanceled()) {
                return new TmfModelResponse<>(null, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
        }
        ISegmentStore<ISegment> segStore = provider.getSegmentStore();

        if (segStore == null) {
            if (!waitResult) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
            }
            return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), Collections.emptyList()), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }

        long start = filter.getStart();
        long end = filter.getEnd();
        final Iterable<ISegment> intersectingElements = Iterables.filter(segStore.getIntersectingElements(start, end), s -> s.getStart() >= start);
        Map<String, INamedSegment> segmentTypes = new HashMap<>();
        IAnalysisModule module = (provider instanceof IAnalysisModule) ? (IAnalysisModule) provider : null;
        boolean complete = module == null || module.isQueryable(filter.getEnd());

        // Create the list of segment types that will each create a series
        for (INamedSegment segment : Iterables.filter(intersectingElements, INamedSegment.class)) {
            if (monitor != null && monitor.isCanceled()) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            segmentTypes.put(segment.getName(), segment);
        }

        Builder<TmfTreeDataModel> nodes = new ImmutableList.Builder<>();
        nodes.add(new TmfTreeDataModel(fTraceId, -1, Collections.singletonList(String.valueOf(getTrace().getName()))));
        Map<IGroupingSegmentAspect, Map<String, Long>> names = new HashMap<>();

        for (Entry<String, INamedSegment> series : segmentTypes.entrySet()) {
            long parentId = fTraceId;
            /*
             * Create a tree sorting aspects by "Grouping aspect" much like
             * counter analyses
             */
            for (IGroupingSegmentAspect aspect : fGroupingAspects) {
                names.putIfAbsent(aspect, new HashMap<>());
                Map<String, Long> map = names.get(aspect);
                if (map == null) {
                    break;
                }
                String name = String.valueOf(aspect.resolve(series.getValue()));
                String key = GROUP_PREFIX + name;
                Long uniqueId = map.get(key);
                if (uniqueId == null) {
                    uniqueId = getUniqueId(key);
                    map.put(key, uniqueId);
                    nodes.add(new TmfTreeDataModel(uniqueId, parentId, name));
                }
                parentId = uniqueId;
            }
            long seriesId = getUniqueId(series.getKey());
            nodes.add(new TmfTreeDataModel(seriesId, parentId, series.getKey()));
        }

        return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), nodes.build()), complete ? ITmfResponse.Status.COMPLETED : ITmfResponse.Status.RUNNING,
                complete ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING);
    }

    // ISegmentStoreProvider provider = fProvider;
    private long getUniqueId(String name) {
        synchronized (fIdToType) {
            return fIdToType.inverse().computeIfAbsent(name, n -> TRACE_IDS.getAndIncrement());
        }
    }

    public TmfXYAxisDescription getXAxisDescription() {
        return new TmfXYAxisDescription("Duration", "ns", DataType.DURATION);
    }

    private static String getTitle() {
        return Objects.requireNonNull(Messages.SegmentStoreDensityDataProvider_title);
    }

    private static DisplayType getDisplayType() {
        return DisplayType.BAR;
    }

    private @Nullable Pair<@NonNull ISampling, @NonNull Collection<@NonNull IYModel>> getXAxisAndYSeriesModels(
            @NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQueryWithSamples(fetchParameters);
        if (filter == null) {
            return null;
        }
        final ISegmentStore<ISegment> segStore = fProvider.getSegmentStore();
        if (segStore == null) {
            return null;
        }

        // Get segments in time range
        Iterable<ISegment> segments = segStore.getIntersectingElements(filter.getStart(), filter.getEnd());

        // Find max duration
        long maxLength = getMaxDuration();
        if (maxLength <= 0) {
            maxLength = 1;
        }

        int nbSamples = filter.getNumberOfSamples();
        long sampleStart = 0;
        long sampleEnd = maxLength;

        // Create bins based on selected entries
        Map<@NonNull Long, @NonNull Integer> selectedEntries = getSelectedEntries(filter);
        ISampling.Ranges sampling = createEvenlyDistributedRanges(sampleStart, sampleEnd, nbSamples);
        if (sampling == null) {
            return null;
        }

        // Create bins for each selected entry
        Map<Long, double[]> entryToBins = new HashMap<>();
        for (Long entryId : selectedEntries.keySet()) {
            entryToBins.put(entryId, new double[nbSamples]);
        }

        // Process segments and fill bins
        long totalSpan = sampleEnd - sampleStart + 1;
        long step = totalSpan / nbSamples;

        for (ISegment segment : segments) {
            if (monitor != null && monitor.isCanceled()) {
                return null;
            }

            long duration = segment.getLength();
            if (step > 0 && duration >= sampleStart && duration <= sampleEnd) {
                int index = (int) ((duration - sampleStart) / step);
                if (index >= nbSamples) {
                    index = nbSamples - 1;
                }

                // Add to appropriate bins based on segment type
                String segmentType = getSegmentType(segment);
                Long entryId = getEntryIdForSegmentType(segmentType);
                if (entryId != null) {
                    double[] bins = entryToBins.get(entryId);
                    if (bins != null) {
                        bins[index]++;
                    }
                }
            }
        }

        // Create Y models
        List<@NonNull IYModel> yModels = new ArrayList<>();
        TmfXYAxisDescription yAxisDescription = new TmfXYAxisDescription("Count", "", DataType.NUMBER);

        for (Entry<Long, double[]> entry : entryToBins.entrySet()) {
            Long entryId = entry.getKey();
            double[] bins = entry.getValue();
            String name = getNameForEntryId(entryId);
            yModels.add(new YModel(entryId, name, bins, yAxisDescription));
        }

        return new Pair<>(sampling, yModels);
    }

    private static String getSegmentType(ISegment segment) {
        if (segment instanceof INamedSegment) {
            return ((INamedSegment) segment).getName();
        }
        return "Total";
    }

    private @Nullable Long getEntryIdForSegmentType(String segmentType) {
        synchronized (fIdToType) {
            return fIdToType.inverse().get(segmentType);
        }
    }

    private String getNameForEntryId(Long entryId) {
        if (entryId.equals(fTotalId)) {
            return Objects.requireNonNull(Messages.SegmentStoreDensity_TotalLabel);
        }
        synchronized (fIdToType) {
            String name = fIdToType.get(entryId);
            return name != null ? name : "Unknown";
        }
    }

    private static Map<@NonNull Long, @NonNull Integer> getSelectedEntries(SelectionTimeQueryFilter filter) {
        Map<@NonNull Long, @NonNull Integer> selectedEntries = new HashMap<>();
        Collection<Long> selectedItems = filter.getSelectedItems();

        int index = 0;
        for (Long item : selectedItems) {
            selectedEntries.put(item, index++);
        }
        return selectedEntries;
    }

    private static ISampling.@Nullable Ranges createEvenlyDistributedRanges(long start, long end, int samples) {
        if (samples <= 0 || start >= end) {
            return null;
        }

        List<ISampling.@NonNull Range<@NonNull Long>> ranges = new ArrayList<>(samples);
        long totalSpan = end - start;
        long step = totalSpan / samples;
        long remainder = totalSpan % samples;

        long current = start;
        for (int i = 0; i < samples; i++) {
            long rangeStart = current;
            long rangeEnd = current + step - 1;
            if (remainder > 0) {
                rangeEnd += 1;
                remainder--;
            }
            if (rangeEnd >= end || i == samples - 1) {
                rangeEnd = end;
            }
            ranges.add(new ISampling.Range<>(rangeStart, rangeEnd));
            current = rangeEnd + 1;
        }

        return new ISampling.Ranges(ranges);
    }

    private long getMaxDuration() {
        final ISegmentStore<ISegment> segStore = fProvider.getSegmentStore();
        if (segStore == null) {
            return -1;
        }
        Optional<ISegment> maxSegment = StreamSupport.stream(segStore.spliterator(), false)
                .max(SegmentComparators.INTERVAL_LENGTH_COMPARATOR);
        return maxSegment.map(ISegment::getLength).orElse(1L);
    }

    @Override
    public String getId() {
        return fID;
    }

    public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(ITmfTrace trace, String secondaryId) {
        // The trace can be an experiment, so we need to know if there are
        // multiple analysis modules with the same ID
        Iterable<ISegmentStoreProvider> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, ISegmentStoreProvider.class);
        Iterable<ISegmentStoreProvider> filteredModules = Iterables.filter(modules, m -> ((IAnalysisModule) m).getId().equals(secondaryId));
        Iterator<ISegmentStoreProvider> iterator = filteredModules.iterator();
        if (iterator.hasNext()) {
            ISegmentStoreProvider module = iterator.next();
            if (iterator.hasNext()) {
                // More than one module, must be an experiment, return null so
                // the factory can try with individual traces
                return null;
            }
            ((IAnalysisModule) module).schedule();
            return new SegmentStoreDensityDataProvider(trace, module, secondaryId);
        }
        return null;
    }
}
