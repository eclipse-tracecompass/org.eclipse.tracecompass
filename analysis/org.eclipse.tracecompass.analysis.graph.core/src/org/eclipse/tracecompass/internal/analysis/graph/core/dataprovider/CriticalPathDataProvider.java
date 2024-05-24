/**********************************************************************
 * Copyright (c) 2018, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.dataprovider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.AbstractCriticalPathModule;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdgeContextState;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraphVisitor;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfGraphStatistics;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphStateFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Data Provider for the Critical Path.
 *
 * @author Loic Prieur-Drevon
 */
public class CriticalPathDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<@NonNull CriticalPathEntry>, IOutputStyleProvider {

    /**
     * Extension point ID for the provider
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.analysis.graph.core.dataprovider.CriticalPathDataProvider"; //$NON-NLS-1$

    /**
     * Atomic long to assign each entry the same unique ID every time the data
     * provider is queried
     */
    private static final AtomicLong ATOMIC_LONG = new AtomicLong();

    private @NonNull AbstractCriticalPathModule fCriticalPathModule;

    private static final @NonNull Map<@NonNull String, @NonNull OutputElementStyle> STATE_MAP = new HashMap<>();

    /**
     * Remember the unique mappings from hosts to their entry IDs
     */
    private final Map<String, Long> fHostIdToEntryId = new HashMap<>();
    /**
     * Remember the unique mappings from workers to their entry IDs
     */
    private final BiMap<IGraphWorker, Long> fWorkerToEntryId = HashBiMap.create();

    private final LoadingCache<IGraphWorker, CriticalPathVisitor> fHorizontalVisitorCache = CacheBuilder.newBuilder()
            .maximumSize(10).build(new CacheLoader<IGraphWorker, CriticalPathVisitor>() {

                @Override
                public CriticalPathVisitor load(IGraphWorker key) throws Exception {
                    ITmfGraph criticalPath = fCriticalPathModule.getCriticalPathGraph();
                    return new CriticalPathVisitor(criticalPath, key);
                }
            });

    /**
     * FIXME when switching between traces, the current worker is set to null, do
     * this to remember the last arrows used.
     */
    private List<@NonNull ITimeGraphArrow> fLinks;

    /** Cache for entry metadata */
    private final Map<Long, @NonNull Multimap<@NonNull String, @NonNull Object>> fEntryMetadata = new HashMap<>();

    /**
     * Constructor
     *
     * @param trace
     *            The trace for which we will be providing the time graph models
     * @param criticalPathProvider
     *            the critical path module for this trace
     */
    public CriticalPathDataProvider(@NonNull ITmfTrace trace, @NonNull AbstractCriticalPathModule criticalPathProvider) {
        super(trace);
        fCriticalPathModule = criticalPathProvider;
    }

    @Override
    public synchronized @NonNull TmfModelResponse<@NonNull TmfTreeModel<@NonNull CriticalPathEntry>> fetchTree(
            Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfGraph graph = fCriticalPathModule.getCriticalPathGraph();
        if (graph == null) {
            return new TmfModelResponse<>(null, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        IGraphWorker current = getCurrent();
        if (current == null) {
            return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        CriticalPathVisitor visitor = fHorizontalVisitorCache.getUnchecked(current);
        for (CriticalPathEntry model : visitor.getEntries()) {
            fEntryMetadata.put(model.getId(), model.getMetadata());
        }
        return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), visitor.getEntries()), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    /**
     * Get the current {@link IGraphWorker} from the critical path module
     *
     * @return the current graph worker if it is set, else null.
     */
    private @Nullable IGraphWorker getCurrent() {
        Object obj = fCriticalPathModule.getParameter(AbstractCriticalPathModule.PARAM_WORKER);
        if (obj == null) {
            return null;
        }
        if (!(obj instanceof IGraphWorker)) {
            throw new IllegalStateException("Wrong type for critical path module parameter " + //$NON-NLS-1$
                    AbstractCriticalPathModule.PARAM_WORKER +
                    " expected IGraphWorker got " + //$NON-NLS-1$
                    obj.getClass().getSimpleName());
        }
        return (IGraphWorker) obj;
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TimeGraphModel> fetchRowModel(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        IGraphWorker graphWorker = getCurrent();
        if (graphWorker == null) {
            return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        CriticalPathVisitor visitor = fHorizontalVisitorCache.getIfPresent(graphWorker);
        if (visitor == null) {
            return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        // TODO server: Parameters validation should be handle separately. It
        // can be either in the data provider itself or before calling it. It
        // will avoid the creation of filters and the content of the map can be
        // use directly.
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        List<@NonNull ITimeGraphRowModel> rowModels = new ArrayList<>();
        for (Long id : filter.getSelectedItems()) {
            /*
             * need to use asMap, so that we don't return a row for an ID that does not
             * belong to this provider, else fStates.get(id) might return an empty
             * collection for an id from another data provider.
             */
            Collection<ITimeGraphState> states = visitor.fStates.asMap().get(id);
            if (states != null) {
                List<ITimeGraphState> filteredStates = new ArrayList<>();
                for (ITimeGraphState state : states) {
                    if (overlaps(state.getStartTime(), state.getDuration(), filter.getTimesRequested())) {
                        // Reset the properties for this state before filtering
                        state.setActiveProperties(0);
                        applyFilterAndAddState(filteredStates, state, id, predicates, monitor);
                    }
                }
                rowModels.add(new TimeGraphRowModel(id, filteredStates));
            }
        }
        return new TmfModelResponse<>(new TimeGraphModel(rowModels), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private static final boolean overlaps(long start, long duration, long[] times) {
        int pos = Arrays.binarySearch(times, start);
        if (pos >= 0) {
            // start is one of the times
            return true;
        }
        int ins = -pos - 1;
        if (ins >= times.length) {
            // start is larger than the last time
            return false;
        }
        /*
         * the first queried time which is larger than the state start time, is also
         * smaller than the state end time. I.e. at least one queried time is in the
         * state range
         */
        return times[ins] <= start + duration;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        // TODO server: Parameters validation should be handle separately. It
        // can be either in the data provider itself or before calling it. It
        // will avoid the creation of filters and the content of the map can be
        // use directly.
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        return new TmfModelResponse<>(getLinkList(filter.getStart(), filter.getEnd()), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        IGraphWorker worker = fWorkerToEntryId.inverse().get(filter.getSelectedItems().iterator().next());
        if (worker == null) {
            return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        Map<@NonNull String, @NonNull String> info = worker.getWorkerInformation(filter.getStart());
        return new TmfModelResponse<>(info, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private final class CriticalPathVisitor implements ITmfGraphVisitor {
        private final ITmfGraph fGraph;
        /**
         * The {@link IGraphWorker} for which the view (tree / states) are computed
         */
        private final Map<String, CriticalPathEntry> fHostEntries = new HashMap<>();
        private final Map<IGraphWorker, CriticalPathEntry> fWorkers = new LinkedHashMap<>();
        private final TmfGraphStatistics fStatistics = new TmfGraphStatistics();

        /**
         * Store the states in a {@link TreeMultimap} so that they are grouped by entry
         * and sorted by start time.
         */
        private final TreeMultimap<Long, ITimeGraphState> fStates = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparingLong(ITimeGraphState::getStartTime));
        private long fStart;
        private long fEnd;
        private List<@NonNull CriticalPathEntry> fCached;
        /**
         * Cache the links once the graph has been traversed.
         */
        private @Nullable List<@NonNull ITimeGraphArrow> fGraphLinks;

        private CriticalPathVisitor(ITmfGraph graph, IGraphWorker worker) {
            fGraph = graph;
            fStart = getTrace().getStartTime().toNanos();
            fEnd = getTrace().getEndTime().toNanos();

            ITmfVertex head = graph.getHead();
            if (head != null) {
                fStart = Long.min(fStart, head.getTimestamp());
                for (IGraphWorker w : graph.getWorkers()) {
                    ITmfVertex tail = graph.getTail(w);
                    if (tail != null) {
                        fEnd = Long.max(fEnd, tail.getTimestamp());
                    }
                }
            }
            fStatistics.computeGraphStatistics(graph, worker);
        }

        @Override
        public void visitHead(ITmfVertex node) {
            IGraphWorker owner = fGraph.getParentOf(node);
            if (owner == null) {
                return;
            }
            if (fWorkers.containsKey(owner)) {
                return;
            }
            ITmfVertex first = fGraph.getHead(owner);
            ITmfVertex last = fGraph.getTail(owner);
            if (first == null || last == null) {
                return;
            }
            fStart = Long.min(getTrace().getStartTime().toNanos(), first.getTimestamp());
            fEnd = Long.max(getTrace().getEndTime().toNanos(), last.getTimestamp());
            Long sum = fStatistics.getSum(owner);
            Double percent = fStatistics.getPercent(owner);

            // create host entry
            String host = owner.getHostId();
            long parentId = fHostIdToEntryId.computeIfAbsent(host, h -> ATOMIC_LONG.getAndIncrement());
            fHostEntries.computeIfAbsent(host, h -> new CriticalPathEntry(parentId, -1L, Collections.singletonList(host), fStart, fEnd, sum, percent));

            long entryId = fWorkerToEntryId.computeIfAbsent(owner, w -> ATOMIC_LONG.getAndIncrement());
            CriticalPathEntry entry = new CriticalPathEntry(entryId, parentId, owner, fStart, fEnd, sum, percent);

            fWorkers.put(owner, entry);
        }

        @Override
        public void visit(ITmfEdge link, boolean horizontal) {
            if (horizontal) {
                IGraphWorker parent = fGraph.getParentOf(link.getVertexFrom());
                Long id = fWorkerToEntryId.get(parent);
                if (id != null) {
                    String linkQualifier = link.getLinkQualifier();
                    ITimeGraphState ev = new TimeGraphState(link.getVertexFrom().getTimestamp(), link.getDuration(), linkQualifier, getMatchingState(link.getEdgeContextState(), false));
                    fStates.put(id, ev);
                }
            } else {
                IGraphWorker parentFrom = fGraph.getParentOf(link.getVertexFrom());
                IGraphWorker parentTo = fGraph.getParentOf(link.getVertexTo());
                CriticalPathEntry entryFrom = fWorkers.get(parentFrom);
                CriticalPathEntry entryTo = fWorkers.get(parentTo);
                List<ITimeGraphArrow> graphLinks = fGraphLinks;
                if (graphLinks != null && entryFrom != null && entryTo != null) {
                    ITimeGraphArrow lk = new TimeGraphArrow(entryFrom.getId(), entryTo.getId(), link.getVertexFrom().getTimestamp(),
                            link.getVertexTo().getTimestamp() - link.getVertexFrom().getTimestamp(), getMatchingState(link.getEdgeContextState(), true));
                    graphLinks.add(lk);
                }
            }
        }

        public @NonNull List<@NonNull CriticalPathEntry> getEntries() {
            if (fCached != null) {
                return fCached;
            }

            fGraph.scanLineTraverse(fGraph.getHead(), this);
            List<@NonNull CriticalPathEntry> list = new ArrayList<>(fHostEntries.values());
            list.addAll(fWorkers.values());
            fCached = list;
            return list;
        }

        public synchronized List<@NonNull ITimeGraphArrow> getGraphLinks() {
            if (fGraphLinks == null) {
                // the graph has not been traversed yet
                fGraphLinks = new ArrayList<>();
                fGraph.scanLineTraverse(fGraph.getHead(), this);
            }
            return fGraphLinks;
        }

        @Override
        public void visit(@NonNull ITmfVertex vertex) {
            // Nothing to do
        }
    }

    /**
     * Critical path typically has relatively few links, so we calculate and save
     * them all, but just return those in range
     */
    private @Nullable List<@NonNull ITimeGraphArrow> getLinkList(long startTime, long endTime) {
        IGraphWorker current = getCurrent();
        List<@NonNull ITimeGraphArrow> graphLinks = fLinks;
        if (current == null) {
            if (graphLinks != null) {
                return graphLinks;
            }
            return Collections.emptyList();
        }
        CriticalPathVisitor visitor = fHorizontalVisitorCache.getIfPresent(current);
        if (visitor == null) {
            return Collections.emptyList();
        }
        graphLinks = visitor.getGraphLinks();
        fLinks = graphLinks;
        return getLinksInRange(graphLinks, startTime, endTime);
    }

    private static List<@NonNull ITimeGraphArrow> getLinksInRange(List<ITimeGraphArrow> allLinks, long startTime, long endTime) {
        List<@NonNull ITimeGraphArrow> linksInRange = new ArrayList<>();
        for (ITimeGraphArrow link : allLinks) {
            if (link.getStartTime() <= endTime &&
                    link.getStartTime() + link.getDuration() >= startTime) {
                linksInRange.add(link);
            }
        }
        return linksInRange;
    }

    /**
     * Get the styles of the edge context state when visiting the critical path graph.
     *
     * This is used to show the correct styles directly to the user.
     *
     * @param contextState Context state to return element style from the process state
     * @param arrow specify if it is an arrow or not
     * @return element style
     */
    protected @NonNull OutputElementStyle getMatchingState(ITmfEdgeContextState contextState, boolean arrow) {
        OutputElementStyle outputElementStyle = new OutputElementStyle(null, contextState.getStyles());
        STATE_MAP.put(contextState.getContextEnum().name(), outputElementStyle);
        return outputElementStyle;
    }

    @Override
    public @NonNull Multimap<@NonNull String, @NonNull Object> getFilterData(long entryId, long time, @Nullable IProgressMonitor monitor) {
        return ITimeGraphStateFilter.mergeMultimaps(ITimeGraphDataProvider.super.getFilterData(entryId, time, monitor),
                fEntryMetadata.getOrDefault(entryId, ImmutableMultimap.of()));
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull OutputStyleModel> fetchStyle(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STATE_MAP), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }
}
