/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Kyrollos Bekhet - Initial API and implementation
 ******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.VirtualTableQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.TmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.VirtualTableCell;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.VirtualTableLine;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractTmfTableDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.tmf.core.TmfStrings;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils.Direction;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.CoreFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.segment.SegmentDurationAspect;
import org.eclipse.tracecompass.tmf.core.segment.SegmentEndTimeAspect;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * This data provider will return a virtual table model (wrapped in a response)
 * Based on a virtual table query filter. Model returned is for segment store
 * tables to do analysis.
 *
 * @author: Kyrollos Bekhet
 */
public class SegmentStoreTableDataProvider extends AbstractTmfTableDataProvider implements ITmfVirtualTableDataProvider<TmfTreeDataModel, VirtualTableLine> {

    /**
     * A simple class to create checkpoints to index the segments of a segment
     * store
     */
    private static class SegmentStoreIndex {
        private long fCounter;
        private long fStartTimestamp;
        private long fLength;

        public SegmentStoreIndex(long startTimeStamp, long counter, long length) {
            fStartTimestamp = startTimeStamp;
            fCounter = counter;
            fLength = length;
        }

        public long getStartTimestamp() {
            return fStartTimestamp;
        }

        public long getCounter() {
            return fCounter;
        }

        public long getLength() {
            return fLength;
        }
    }

    /**
     * A predicate implementation that is used to evaluate if a segment is the
     * first of the checkpoint.
     *
     * @author Kyrollos Bekhet.
     */
    private static class SegmentPredicate implements Predicate<ISegment> {
        private final long fStartTime;
        private long fCount;
        private long fLength;
        private boolean fDurationComparator;

        public SegmentPredicate(SegmentStoreIndex segmentIndex, String aspectName) {
            fStartTime = segmentIndex.getStartTimestamp();
            fCount = segmentIndex.getCounter();
            fLength = segmentIndex.getLength();
            fDurationComparator = aspectName.equals(SegmentDurationAspect.SEGMENT_DURATION_ASPECT.getName());
        }

        @Override
        public boolean test(ISegment segment) {
            if (isDurationValid(segment.getLength())) {
                if (segment.getStart() > fStartTime) {
                    return true;
                }
                if (segment.getStart() == fStartTime) {
                    if (fCount == 0) {
                        return true;
                    }
                    fCount--;
                }
            }
            return false;
        }

        private boolean isDurationValid(long segmentLength) {
            if (!fDurationComparator) {
                return true;
            }
            return segmentLength == fLength;
        }
    }

    /**
     * A placeholder class to wrap a segment and its rank.
     *
     * @author Kyrollos Bekhet
     */
    private static class WrappedSegment {
        private ISegment fSegment;
        private long fRank;

        public WrappedSegment(ISegment segment, long rank) {
            fSegment = segment;
            fRank = rank;
        }

        public ISegment getOriginalSegment() {
            return fSegment;
        }

        public long getRank() {
            return fRank;
        }
    }

    /**
     * A wrapper class to encapsulate the indexes with the comparator that was
     * used to create the indexes.
     *
     * @author Kyrollos Bekhet
     */
    private static final class SegmentIndexesComparatorWrapper {
        private final List<SegmentStoreIndex> fIndexes;
        private final Comparator<ISegment> fComparator;
        private final String fAspectName;

        @SuppressWarnings("null")
        public SegmentIndexesComparatorWrapper() {
            fComparator = SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(Comparator.comparingLong(ISegment::getLength));
            fIndexes = new ArrayList<>();
            fAspectName = StringUtils.EMPTY;
        }

        public SegmentIndexesComparatorWrapper(List<SegmentStoreIndex> indexes, Comparator<ISegment> comparator, String aspectName) {
            fIndexes = indexes;
            fComparator = comparator;
            fAspectName = aspectName;
        }

        public SegmentStoreIndex getIndex(int rank) {
            return fIndexes.get(rank);
        }

        public int getIndexesSize() {
            return fIndexes.size();
        }

        public Comparator<ISegment> getComparator() {
            return fComparator;
        }

        public String getAspectName() {
            return fAspectName;
        }
    }

    /**
     * The id of the data provider
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreTableDataProvider"; //$NON-NLS-1$
    private static BiMap<ISegmentAspect, Long> fAspectToIdMap = HashBiMap.create();
    private static final Format FORMATTER = new DecimalFormat("###,###.##"); //$NON-NLS-1$
    private static final int STEP = 1000;
    private static final Logger LOGGER = TraceCompassLog.getLogger(SegmentStoreTableDataProvider.class);
    private static final String TABLE_COMPARATOR_EXPRESSION_KEY = "table_comparator_expression"; //$NON-NLS-1$

    private Map<Long, SegmentIndexesComparatorWrapper> fAllIndexes;
    private SegmentIndexesComparatorWrapper fDefaultWrapper;
    private final String fId;
    private ISegmentStoreProvider fSegmentProvider;
    private boolean fIsFirstAspect;
    private int fSegmentStoreSize;

    /**
     * Constructor
     *
     * @param trace
     *            A trace on which we are interested to fetch a segment store
     *            table model.
     * @param segmentProvider
     *            The segment provider that contains the data and from which the
     *            data will be fetched.
     * @param analysisId
     *            The analysis identifier.
     */
    public SegmentStoreTableDataProvider(ITmfTrace trace, ISegmentStoreProvider segmentProvider, String analysisId) {
        super(trace);
        TraceCompassLogUtils.traceObjectCreation(LOGGER, Level.FINE, this);
        fId = analysisId;
        fIsFirstAspect = true;
        fAllIndexes = new HashMap<>();
        fDefaultWrapper = new SegmentIndexesComparatorWrapper();
        fSegmentProvider = segmentProvider;
    }

    @Override
    public void dispose() {
        TraceCompassLogUtils.traceObjectDestruction(LOGGER, Level.FINE, this, 10);
    }

    /**
     * Build the indexes which will act like checkpoints for the data provider.
     *
     * @param id
     *            the id of the aspect in the {@link fAspectToIdMap}.
     * @param comparator
     *            The comparator used to build the indexes array
     * @param aspectName
     *            The name of the aspect.
     */
    private void buildIndex(long id, Comparator<ISegment> comparator, String aspectName) {
        if (fAllIndexes.containsKey(id)) {
            return;
        }
        ISegmentStore<ISegment> segStore = fSegmentProvider.getSegmentStore();
        if (segStore != null) {
            synchronized (fAllIndexes) {
                try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "SegmentStoreTableDataProvider#buildIndex.buildingIndexes").build()) { //$NON-NLS-1$
                    TraceCompassLogUtils.traceObjectCreation(LOGGER, Level.FINE, fAllIndexes);
                    Iterable<ISegment> sortedSegmentStore = segStore.iterator(comparator);
                    List<SegmentStoreIndex> indexes = getIndexes(sortedSegmentStore);
                    if (fIsFirstAspect) {
                        fDefaultWrapper = new SegmentIndexesComparatorWrapper(indexes, comparator, aspectName);
                        fIsFirstAspect = false;
                        fAllIndexes.put(id, fDefaultWrapper);
                    } else {
                        fAllIndexes.put(id, new SegmentIndexesComparatorWrapper(indexes, comparator, aspectName));
                    }
                } catch (Exception ex) {
                    TraceCompassLogUtils.traceInstant(LOGGER, Level.SEVERE, "error build index", ex.getMessage()); //$NON-NLS-1$
                } finally {
                    TraceCompassLogUtils.traceObjectDestruction(LOGGER, Level.FINE, fAllIndexes);
                }
            }
        }
    }

    private static List<SegmentStoreIndex> getIndexes(Iterable<ISegment> segmentStore) {
        long counter = 0;
        long i = 0;
        long previousTimestamp = Long.MAX_VALUE;
        List<SegmentStoreIndex> indexes = new ArrayList<>();
        for (ISegment segment : segmentStore) {
            if (segment.getStart() == previousTimestamp) {
                counter++;
            } else {
                previousTimestamp = segment.getStart();
                counter = 0;
            }
            if (i % STEP == 0) {
                indexes.add(new SegmentStoreIndex(segment.getStart(), counter, segment.getLength()));
            }
            i++;
        }
        return indexes;
    }

    @Override
    public String getId() {
        return fId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        if (fSegmentProvider instanceof IAnalysisModule) {
            ((IAnalysisModule) fSegmentProvider).waitForCompletion();
            ISegmentStore<ISegment> segmentStore = fSegmentProvider.getSegmentStore();
            fSegmentStoreSize = segmentStore != null ? segmentStore.size() : 0;
        }
        List<TmfTreeDataModel> model = new ArrayList<>();
        for (ISegmentAspect aspect : ISegmentStoreProvider.getBaseSegmentAspects()) {
            synchronized (fAspectToIdMap) {
                long id = fAspectToIdMap.computeIfAbsent(aspect, a -> createColumnId());
                Comparator<ISegment> comparator = (Comparator<ISegment>) aspect.getComparator();
                if (comparator != null && aspect.getName().equals(SegmentEndTimeAspect.SEGMENT_END_TIME_ASPECT.getName())) {
                    comparator = comparator.reversed();
                }
                if (comparator != null) {
                    buildIndex(id, comparator, aspect.getName());
                }
                model.add(new TmfTreeDataModel(id, -1, Collections.singletonList(aspect.getName())));
            }
        }
        for (ISegmentAspect aspect : fSegmentProvider.getSegmentAspects()) {
            synchronized (fAspectToIdMap) {
                long id = fAspectToIdMap.computeIfAbsent(aspect, a -> createColumnId());
                Comparator<ISegment> comparator = (Comparator<ISegment>) aspect.getComparator();
                if (comparator != null) {
                    buildIndex(id, comparator, aspect.getName());
                }
                model.add(new TmfTreeDataModel(id, -1, Collections.singletonList(aspect.getName())));
            }
        }
        return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), model), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<ITmfVirtualTableModel<VirtualTableLine>> fetchLines(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        TraceCompassLogUtils.traceAsyncStart(LOGGER, Level.FINE, "SegmentStoreTableDataProvider#fetchLines", fId, 2); //$NON-NLS-1$
        fetchParameters.putIfAbsent(DataProviderParameterUtils.REQUESTED_COLUMN_IDS_KEY, Collections.emptyList());
        VirtualTableQueryFilter queryFilter = FetchParametersUtils.createVirtualTableQueryFilter(fetchParameters);
        if (queryFilter == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        SegmentIndexesComparatorWrapper indexesComparatorWrapper = getIndexesComparatorOrDefault(fetchParameters);
        Map<Long, ISegmentAspect> aspects = getAspectsFromColumnId(queryFilter.getColumnsId(), monitor);
        if (aspects.isEmpty()) {
            return new TmfModelResponse<>(new TmfVirtualTableModel<>(Collections.emptyList(), Collections.emptyList(), queryFilter.getIndex(), 0), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        List<Long> columnIds = new ArrayList<>(aspects.keySet());
        ISegmentStore<ISegment> segStore = fSegmentProvider.getSegmentStore();
        if (segStore != null) {
            if (segStore.isEmpty()) {
                return new TmfModelResponse<>(new TmfVirtualTableModel<>(columnIds, Collections.emptyList(), queryFilter.getIndex(), 0), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }
            if (queryFilter.getIndex() >= fSegmentStoreSize) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }
            synchronized (fAllIndexes) {
                try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "SegmentStoreTableDataProvider#fetchLines").build()) { //$NON-NLS-1$
                    TraceCompassLogUtils.traceObjectCreation(LOGGER, Level.FINER, fAllIndexes);
                    return extractRequestedLines(queryFilter, fetchParameters, segStore, aspects, indexesComparatorWrapper);
                } catch (Exception ex) {
                    TraceCompassLogUtils.traceInstant(LOGGER, Level.SEVERE, "error fetching lines ", ex.getMessage()); //$NON-NLS-1$
                } finally {
                    TraceCompassLogUtils.traceObjectDestruction(LOGGER, Level.FINER, fAllIndexes);
                }
            }
        }
        return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
    }

    private static TmfModelResponse<ITmfVirtualTableModel<VirtualTableLine>> extractRequestedLines(VirtualTableQueryFilter queryFilter, Map<String, Object> fetchParameters, ISegmentStore<ISegment> segmentStore, Map<Long, ISegmentAspect> aspects,
            SegmentIndexesComparatorWrapper indexesComparatorWrapper) {
        List<Long> columnIds = new ArrayList<>(aspects.keySet());
        List<VirtualTableLine> lines = new ArrayList<>();

        int startIndexRank = (int) (queryFilter.getIndex() / STEP);
        int actualStartQueryIndex = (int) (queryFilter.getIndex() % STEP);
        SegmentStoreIndex segIndex = indexesComparatorWrapper.getIndex(startIndexRank);
        SegmentPredicate filter = new SegmentPredicate(segIndex, indexesComparatorWrapper.getAspectName());
        long start = segIndex.getStartTimestamp();

        int endIndexRank = (int) ((queryFilter.getIndex() + queryFilter.getCount() + STEP - 1) / STEP);
        long end = getEndTimestamp(endIndexRank, indexesComparatorWrapper);

        VirtualTableQueryFilter localQueryFilter = queryFilter;
        @Nullable Predicate<ISegment> searchFilter = generateFilter(fetchParameters);

        /*
         * Search for the next or previous segment starting from the given
         * segment index
         */
        Object directionValue = fetchParameters.get(DataProviderParameterUtils.TABLE_SEARCH_DIRECTION_KEY);
        if (searchFilter != null && directionValue != null) {
            Direction direction = null;
            try {
                direction = Direction.valueOf(String.valueOf(directionValue));
            } catch (IllegalArgumentException e) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
            }
            @Nullable WrappedSegment segment = null;
            if (direction == Direction.NEXT) {
                segment = getNextWrappedSegmentMatching(searchFilter, startIndexRank, actualStartQueryIndex, segmentStore, indexesComparatorWrapper, lines, aspects, queryFilter.getCount());
            } else {
                segment = getPreviousWrappedSegmentMatching(searchFilter, startIndexRank, actualStartQueryIndex, segmentStore, indexesComparatorWrapper, lines, aspects, queryFilter.getCount());
            }
            if (segment != null) {
                localQueryFilter = new VirtualTableQueryFilter(queryFilter.getColumnsId(), segment.getRank(), queryFilter.getCount());
                long nextSegmentRank = segment.getRank() + 1;
                startIndexRank = (int) (nextSegmentRank / STEP);
                actualStartQueryIndex = (int) (nextSegmentRank % STEP);
                segIndex = indexesComparatorWrapper.getIndex(startIndexRank);
                start = segIndex.getStartTimestamp();
                endIndexRank = (int) ((nextSegmentRank + localQueryFilter.getCount() + STEP - 1) / STEP);
                end = getEndTimestamp(endIndexRank, indexesComparatorWrapper);
            }
            if ((queryFilter.getCount() == 1) || (segment == null)) {
                return new TmfModelResponse<>(new TmfVirtualTableModel<>(columnIds, lines, localQueryFilter.getIndex(), segmentStore.size()), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }
        }

        List<ISegment> newSegStore = segmentStore.getIntersectingElements(start, end, indexesComparatorWrapper.getComparator(), filter);
        for (int i = actualStartQueryIndex; i < newSegStore.size(); i++) {
            if (queryFilter.getCount() == lines.size()) {
                break;
            }
            long lineNumber = localQueryFilter.getIndex() + lines.size();
            VirtualTableLine newLine = buildSegmentStoreTableLine(aspects, newSegStore.get(i), lineNumber, searchFilter);
            lines.add(newLine);
        }
        return new TmfModelResponse<>(new TmfVirtualTableModel<>(columnIds, lines, localQueryFilter.getIndex(), segmentStore.size()), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private SegmentIndexesComparatorWrapper getIndexesComparatorOrDefault(Map<String, Object> fetchParameters) {
        @Nullable Long key = extractColumnId(fetchParameters.get(TABLE_COMPARATOR_EXPRESSION_KEY));
        return fAllIndexes.getOrDefault(key, fDefaultWrapper);
    }

    /**
     * Retrieve from a segment store the next segment starting from a given
     * index, matching the given predicate.
     *
     * @param searchFilter
     *            The predicate to match.
     * @param startIndexRank
     *            The initial start-time index rank to search from.
     * @param startQueryIndex
     *            The initial start index to query from.
     * @param segmentStore
     *            The segment store from where the segments will be fetched.
     * @param indexesComparatorWrapper
     *            The segment indexes comparator wrapper to use.
     * @param lines
     *            The virtual table lines to add the found segments to.
     * @param aspects
     *            The aspects being resolved.
     * @param count
     *            The count of desired events.
     *
     * @return A {@link WrappedSegment} that contains the matching next segment
     *         found after a given index.
     */
    private static @Nullable WrappedSegment getNextWrappedSegmentMatching(Predicate<ISegment> searchFilter, int startIndexRank, int startQueryIndex, ISegmentStore<ISegment> segmentStore,
            SegmentIndexesComparatorWrapper indexesComparatorWrapper, List<VirtualTableLine> lines, Map<Long, ISegmentAspect> aspects, int count) {
        WrappedSegment first = null;
        int startTimeIndexRank = startIndexRank;
        int actualStartQueryIndex = startQueryIndex;
        while (startTimeIndexRank < indexesComparatorWrapper.getIndexesSize()) {
            List<ISegment> segments = getIntersectingElements(segmentStore, indexesComparatorWrapper, startTimeIndexRank);
            for (int i = actualStartQueryIndex; i < segments.size(); i++) {
                ISegment segment = segments.get(i);
                if (searchFilter.test(segment)) {
                    long rank = ((long) startTimeIndexRank * STEP) + i;
                    WrappedSegment wrapped = new WrappedSegment(segment, rank);
                    lines.add(buildSegmentStoreTableLine(aspects, wrapped.getOriginalSegment(), wrapped.getRank(), searchFilter));
                    first = (first == null ? wrapped : first);
                    if (lines.size() == count) {
                        return first;
                    }
                }
            }
            actualStartQueryIndex = 0;
            startTimeIndexRank++;
        }
        return first;
    }

    /**
     * Retrieve from a segment store the previous segment starting from a given
     * index, matching the given predicate.
     *
     * @param searchFilter
     *            The predicate to match.
     * @param startIndexRank
     *            The initial start-time index rank to search from.
     * @param startQueryIndex
     *            The initial start index to query from.
     * @param segmentStore
     *            The segment store from where the segments will be fetched.
     * @param indexesComparatorWrapper
     *            The segment indexes comparator wrapper to use.
     * @param lines
     *            The virtual table lines to add the found segments to.
     * @param aspects
     *            The aspects being resolved.
     * @param count
     *            The count of desired events.
     *
     * @return A {@link WrappedSegment} that contains the matching previous
     *         segment found before a given index.
     */
    private static @Nullable WrappedSegment getPreviousWrappedSegmentMatching(Predicate<ISegment> searchFilter, int startIndexRank, int startQueryIndex, ISegmentStore<ISegment> segmentStore,
            SegmentIndexesComparatorWrapper indexesComparatorWrapper, List<VirtualTableLine> lines, Map<Long, ISegmentAspect> aspects, int count) {
        WrappedSegment first = null;
        int startTimeIndexRank = startIndexRank;
        int actualStartQueryIndex = startQueryIndex;
        while (startTimeIndexRank >= 0) {
            List<ISegment> segments = getIntersectingElements(segmentStore, indexesComparatorWrapper, startTimeIndexRank);
            for (int i = Math.min(segments.size() - 1, actualStartQueryIndex); i >= 0; i--) {
                ISegment segment = segments.get(i);
                if (searchFilter.test(segment)) {
                    long rank = ((long) startTimeIndexRank * STEP) + i;
                    WrappedSegment wrapped = new WrappedSegment(segment, rank);
                    lines.add(buildSegmentStoreTableLine(aspects, wrapped.getOriginalSegment(), wrapped.getRank(), searchFilter));
                    first = (first == null ? wrapped : first);
                    if (lines.size() == count) {
                        return first;
                    }
                }
            }
            actualStartQueryIndex = STEP;
            startTimeIndexRank--;
        }
        return first;
    }

    private static List<ISegment> getIntersectingElements(ISegmentStore<ISegment> segmentStore, SegmentIndexesComparatorWrapper indexesComparatorWrapper, int startTimeIndexRank) {
        int endTimeIndexRank = startTimeIndexRank + 1;
        SegmentStoreIndex segIndex = indexesComparatorWrapper.getIndex(startTimeIndexRank);
        SegmentPredicate filter = new SegmentPredicate(segIndex, indexesComparatorWrapper.getAspectName());
        long end = getEndTimestamp(endTimeIndexRank, indexesComparatorWrapper);
        return segmentStore.getIntersectingElements(segIndex.getStartTimestamp(), end, indexesComparatorWrapper.getComparator(), filter);
    }

    /**
     * Generates a predicate filter based on the search map found in the given
     * query parameters.
     *
     * @param fetchParameters
     *            The query parameters used to extract the search map.
     *
     * @return A predicate based on the search map found in the fetch
     *         parameters, or {@code null} if no search map was provided.
     */
    private static @Nullable Predicate<ISegment> generateFilter(Map<String, Object> fetchParameters) {
        @Nullable Map<Long, String> searchMap = extractSearchFilter(fetchParameters);
        if (searchMap == null) {
            return null;
        }
        Map<Long, Pattern> patterns = new HashMap<>();
        for (Entry<Long, String> searchEntry : searchMap.entrySet()) {
            patterns.put(searchEntry.getKey(), Pattern.compile(searchEntry.getValue()));
        }
        Map<Long, ISegmentAspect> aspects = fAspectToIdMap.inverse();
        return segment -> {
            for (Entry<Long, Pattern> patternEntry : patterns.entrySet()) {
                Pattern pattern = Objects.requireNonNull(patternEntry.getValue());
                ISegmentAspect aspect = aspects.get(patternEntry.getKey());
                if (aspect != null && !pattern.matcher(formatResolvedAspect(aspect.resolve(segment), aspect.getName())).find()) {
                    return false;
                }
            }
            return true;
        };
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<Long, String> extractSearchFilter(Map<String, Object> fetchParameters) {
        Object searchFilterObject = fetchParameters.get(DataProviderParameterUtils.TABLE_SEARCH_EXPRESSIONS_KEY);
        if (searchFilterObject instanceof Map<?, ?>) {
            return extractSimpleSearchFilter((Map<?, String>) searchFilterObject);
        }
        return null;
    }

    private static @Nullable Map<Long, String> extractSimpleSearchFilter(Map<?, String> searchFilterObject) {
        if (searchFilterObject.isEmpty()) {
            return null;
        }
        Map<Long, String> searchMap = new HashMap<>();
        for (Entry<?, String> searchEntry : searchFilterObject.entrySet()) {
            Long key = extractColumnId(searchEntry.getKey());
            if (key != null) {
                searchMap.put(key, searchEntry.getValue());
            }
        }
        return searchMap;
    }

    private static @Nullable Long extractColumnId(@Nullable Object key) {
        return AbstractTmfTableDataProvider.extractColumnId(key, true);
    }

    /**
     * Builds the table line.
     *
     * @param aspects
     *            The aspects to resolve.
     * @param segment
     *            The segment that contains the data that will fill the line.
     * @param lineNumber
     *            The line number that will be assigned to the line that will be
     *            built.
     * @param searchFilter
     *            The predicate that applies the search to set the active
     *            property
     *
     * @return Returns a SegmentStoreTableLine after resolving the aspects of a
     *         given segment
     */
    private static VirtualTableLine buildSegmentStoreTableLine(Map<Long, ISegmentAspect> aspects, ISegment segment, long lineNumber, @Nullable Predicate<ISegment> searchFilter) {
        List<VirtualTableCell> entry = new ArrayList<>(aspects.size());
        for (Entry<Long, ISegmentAspect> aspectEntry : aspects.entrySet()) {
            ISegmentAspect aspect = Objects.requireNonNull(aspectEntry.getValue());
            Object aspectResolved = aspect.resolve(segment);
            String cellContent = formatResolvedAspect(aspectResolved, aspect.getName());
            entry.add(new VirtualTableCell(cellContent));
        }
        VirtualTableLine tableLine = new VirtualTableLine(lineNumber, entry);
        if (searchFilter != null) {
            tableLine.setActiveProperties(searchFilter.test(segment) ? CoreFilterProperty.HIGHLIGHT : 0);
        }
        return tableLine;
    }

    /**
     * Returns the desired {@link ISegmentAspect}.
     *
     * @param desiredColumns
     *            The list of desired column ids that we want to retrieve
     * @param monitor
     *            The progress monitor
     *
     * @return The list of {@link ISegmentAspect} that matches the desired
     *         columns ids
     */
    private Map<Long, ISegmentAspect> getAspectsFromColumnId(List<Long> desiredColumns, @Nullable IProgressMonitor monitor) {
        Map<Long, ISegmentAspect> aspects = new LinkedHashMap<>();
        if (!desiredColumns.isEmpty()) {
            for (Long columnId : desiredColumns) {
                ISegmentAspect aspect = fAspectToIdMap.inverse().get(columnId);
                if (aspect != null) {
                    aspects.put(columnId, aspect);
                }
            }
            return aspects;
        }
        // Get all columnIds using fetchTree
        TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> response = this.fetchTree(Collections.emptyMap(), monitor);
        if (response.getStatus() == ITmfResponse.Status.COMPLETED) {
            TmfTreeModel<TmfTreeDataModel> model = response.getModel();
            if (model != null && !model.getEntries().isEmpty()) {
                Collector<? super Long, ?, List<Long>> list = Collectors.toList();
                return getAspectsFromColumnId(model.getEntries().stream().map(TmfTreeDataModel::getId).collect(list), monitor);
            }
        }
        return Objects.requireNonNull(fAspectToIdMap.inverse());
    }

    /**
     * Formats the resolved aspect into a string.
     *
     * @param aspectResolved
     *            The object that results of the resolution of the segment.
     * @param aspectName
     *            The name of the aspect used to identify the strategy of
     *            formatting.
     *
     * @return a literal string of the content of the aspect resolved object.
     */
    private static String formatResolvedAspect(@Nullable Object aspectResolved, String aspectName) {
        String aspectParsed;
        if (aspectName.equals(TmfStrings.duration())) {
            aspectParsed = NonNullUtils.nullToEmptyString(FORMATTER.format(aspectResolved));
        } else if (aspectName.equals(TmfStrings.startTime()) || aspectName.equals(TmfStrings.endTime())) {
            aspectParsed = String.valueOf(TmfTimestamp.fromNanos((Long) Objects.requireNonNull(aspectResolved)));

        } else {
            aspectParsed = aspectResolved == null ? StringUtils.EMPTY : String.valueOf(aspectResolved);
        }
        return aspectParsed;
    }

    private static long getEndTimestamp(int position, SegmentIndexesComparatorWrapper indexComparatorWrapper) {
        if (position >= indexComparatorWrapper.getIndexesSize()) {
            boolean isEndTimeComparatorUsed = indexComparatorWrapper.getAspectName().equals(SegmentEndTimeAspect.SEGMENT_END_TIME_ASPECT.getName());
            if (isEndTimeComparatorUsed) {
                return 0;
            }
            return Long.MAX_VALUE;
        }
        return indexComparatorWrapper.getIndex(position).getStartTimestamp();
    }
}
