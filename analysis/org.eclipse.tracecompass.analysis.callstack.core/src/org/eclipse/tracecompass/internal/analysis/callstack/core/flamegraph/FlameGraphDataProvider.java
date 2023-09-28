/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.flamegraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.IDataPalette;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.FlameChartEntryModel;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.FlameChartEntryModel.EntryType;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.IHostModel;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.AllGroupDescriptor;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.ITree;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.IWeightedTreeProvider.MetricType;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.WeightedTree;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.WeightedTreeGroupBy;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * A data provider for flame graphs, using a {@link IWeightedTreeProvider} as
 * input for the data
 *
 * TODO: Publish the presentation provider
 *
 * TODO: Find a way to advertise extra parameters (group_by, selection range)
 *
 * TODO: Use weighted tree instead of callgraph provider
 *
 * @author Geneviève Bastien
 * @param <N>
 *            The type of objects represented by each node in the tree
 * @param <E>
 *            The type of elements used to group the trees
 * @param <T>
 *            The type of the tree provided
 */
public class FlameGraphDataProvider<@NonNull N, E, @NonNull T extends WeightedTree<@NonNull N>> extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<FlameChartEntryModel>, IOutputStyleProvider {

    /**
     * Provider ID.
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.callstack.core.flamegraph"; //$NON-NLS-1$
    /**
     * The key used to specify how to group the entries of the flame graph
     */
    public static final String GROUP_BY_KEY = "group_by"; //$NON-NLS-1$
    /**
     * The key used to specify a time selection to get the callgraph for. It
     * should be a list of 2 longs
     */
    public static final String SELECTION_RANGE_KEY = "selection_range"; //$NON-NLS-1$
    /**
     * The key used to specify whether to return actions as tooltips, actions
     * keys will start with the '#' characters
     */
    public static final String TOOLTIP_ACTION_KEY = "actions"; //$NON-NLS-1$

    private static final AtomicLong ENTRY_ID = new AtomicLong();

    /**
     * Logger for Abstract Tree Data Providers.
     */
    private static final Logger LOGGER = TraceCompassLog.getLogger(FlameGraphDataProvider.class);

    /* State System attributes for the root levels */
    private static final String FUNCTION_LEVEL = "::Function"; //$NON-NLS-1$

    @SuppressWarnings("null")
    private final Comparator<WeightedTree<N>> fCctComparator2 = Comparator.comparing(WeightedTree<N>::getWeight).thenComparing(s -> String.valueOf(s.getObject()));

    private final IWeightedTreeProvider<N, E, T> fWtProvider;
    private final String fAnalysisId;
    private final long fTraceId = ENTRY_ID.getAndIncrement();
    private final ReentrantReadWriteLock fLock = new ReentrantReadWriteLock(false);

    private final Map<Long, FlameChartEntryModel> fEntries = new HashMap<>();
    private final Map<Long, WeightedTreeEntry> fCgEntries = new HashMap<>();
    private final Map<Long, Long> fEndTimes = new HashMap<>();

    private @Nullable Pair<CacheKey, TmfModelResponse<TmfTreeModel<FlameChartEntryModel>>> fCached;

    private class CacheKey {
        private final Map<String, Object> fParameters;
        private final IWeightedTreeSet<N, Object, WeightedTree<N>> fTreeSet;

        public CacheKey(Map<String, Object> parameters, IWeightedTreeSet<N, Object, WeightedTree<N>> treeset) {
            fParameters = parameters;
            fTreeSet = treeset;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fParameters, fTreeSet);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof FlameGraphDataProvider.CacheKey)) {
                return false;
            }
            return Objects.equals(fParameters, ((FlameGraphDataProvider.CacheKey) obj).fParameters)
                    && Objects.equals(fTreeSet, ((FlameGraphDataProvider.CacheKey) obj).fTreeSet);
        }
    }

    /** An internal class to describe the data for an entry */
    private static class WeightedTreeEntry {
        private ITmfStateSystem fSs;
        private Integer fQuark;

        public WeightedTreeEntry(ITmfStateSystem ss, Integer quark) {
            fSs = ss;
            fQuark = quark;
        }
    }

    private static class CalleeCustomValue<@NonNull N> extends CustomStateValue {
        private WeightedTree<N> fCallSite;

        public CalleeCustomValue(WeightedTree<N> rootFunction) {
            fCallSite = rootFunction;
        }

        @SuppressWarnings("unchecked")
        @Override
        public int compareTo(ITmfStateValue o) {
            if (!(o instanceof CalleeCustomValue)) {
                return -1;
            }
            return fCallSite.compareTo(((CalleeCustomValue<N>) o).fCallSite);
        }

        @Override
        protected Byte getCustomTypeId() {
            return 103;
        }

        @Override
        protected void serializeValue(ISafeByteBufferWriter buffer) {
            throw new UnsupportedOperationException("This state value is not meant to be written to disk"); //$NON-NLS-1$
        }

        @Override
        protected int getSerializedValueSize() {
            throw new UnsupportedOperationException("This state value is not meant to be written to disk"); //$NON-NLS-1$
        }
    }

    /**
     * Constructor
     *
     * @param trace
     *            The trace for which this data provider applies
     * @param module
     *            The weighted tree provider encapsulated by this provider
     * @param secondaryId
     *            The ID of the weighted tree provider
     */
    public FlameGraphDataProvider(ITmfTrace trace, IWeightedTreeProvider<N, E, T> module, String secondaryId) {
        super(trace);
        fWtProvider = module;
        fAnalysisId = secondaryId;
    }

    @Override
    public String getId() {
        return fAnalysisId;
    }

    @Override
    public @NonNull TmfModelResponse<TmfTreeModel<FlameChartEntryModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {

        fLock.writeLock().lock();
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphDataProvider#fetchTree") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {
            // Did we cache this tree with those parameters and the callgraph?
            // For some analyses, the returned callgraph for the same parameters
            // may vary if the analysis was done again, we need to cache for
            // callgraph as well
            SubMonitor subMonitor = Objects.requireNonNull(SubMonitor.convert(monitor, "FlameGraphDataProvider#fetchRowModel", 2)); //$NON-NLS-1$
            IWeightedTreeSet<N, Object, WeightedTree<N>> callGraph = getCallGraph(fetchParameters, subMonitor);
            if (callGraph == null) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.TASK_CANCELLED);
            }

            CacheKey cacheKey = new CacheKey(fetchParameters, callGraph);
            Pair<CacheKey, TmfModelResponse<TmfTreeModel<FlameChartEntryModel>>> cached = fCached;
            if (cached != null && cached.getFirst().equals(cacheKey)) {
                return cached.getSecond();
            }

            fEntries.clear();
            fCgEntries.clear();

            if (subMonitor.isCanceled()) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }

            long start = 0;

            // Initialize the first element of the tree
            List<FlameChartEntryModel.Builder> builder = new ArrayList<>();
            @SuppressWarnings("null")
            FlameChartEntryModel.Builder traceEntry = new FlameChartEntryModel.Builder(fTraceId, -1, getTrace().getName(), start, FlameChartEntryModel.EntryType.TRACE, -1);

            buildWeightedTreeEntries(callGraph, builder, traceEntry);

            ImmutableList.Builder<FlameChartEntryModel> treeBuilder = ImmutableList.builder();
            long end = traceEntry.getEndTime();
            for (FlameChartEntryModel.Builder builderEntry : builder) {
                treeBuilder.add(builderEntry.build());
                end = Math.max(end, builderEntry.getEndTime());
            }
            traceEntry.setEndTime(end);
            treeBuilder.add(traceEntry.build());
            List<FlameChartEntryModel> tree = treeBuilder.build();

            tree.forEach(entry -> {
                fEntries.put(entry.getId(), entry);
                fEndTimes.put(entry.getId(), entry.getEndTime());
            });

            TmfModelResponse<TmfTreeModel<FlameChartEntryModel>> response = new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), tree),
                    ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            fCached = new Pair<>(cacheKey, response);
            return response;
        } finally {
            fLock.writeLock().unlock();
        }
    }

    /**
     * @param fetchParameters
     */
    @SuppressWarnings("unchecked")
    private @Nullable IWeightedTreeSet<N, Object, WeightedTree<N>> getCallGraph(Map<String, Object> fetchParameters, SubMonitor subMonitor) {
        // Get the provider and wait for the analysis completion
        IWeightedTreeProvider<N, E, T> wtProvider = fWtProvider;
        if (wtProvider instanceof IAnalysisModule) {
            ((IAnalysisModule) wtProvider).waitForCompletion(subMonitor);
        }
        if (subMonitor.isCanceled()) {
            return null;
        }

        // Get the full or selection callgraph
        List<Long> selectionRange = DataProviderParameterUtils.extractLongList(fetchParameters, SELECTION_RANGE_KEY);
        IWeightedTreeSet<@NonNull N, E, @NonNull T> callGraph;
        if (selectionRange == null || selectionRange.size() != 2) {
            callGraph = wtProvider.getTreeSet();
        } else {
            long time0 = selectionRange.get(0);
            long time1 = selectionRange.get(1);
            callGraph = wtProvider.getSelection(TmfTimestamp.fromNanos(Math.min(time0, time1)), TmfTimestamp.fromNanos(Math.max(time0, time1)));
        }
        if (callGraph == null) {
            return null;
        }

        // Look if we need to group the callgraph
        IWeightedTreeGroupDescriptor groupDescriptor = extractGroupDescriptor(fetchParameters, wtProvider);
        if (groupDescriptor != null) {
            return WeightedTreeGroupBy.groupWeightedTreeBy(groupDescriptor, callGraph, wtProvider);
        }

        return (IWeightedTreeSet<@NonNull N, Object, WeightedTree<@NonNull N>>) callGraph;
    }

    private static @Nullable IWeightedTreeGroupDescriptor extractGroupDescriptor(Map<String, Object> fetchParameters, IWeightedTreeProvider<?, ?, ?> fcProvider) {
        Object groupBy = fetchParameters.get(GROUP_BY_KEY);
        if (groupBy == null) {
            return null;
        }
        String groupName = String.valueOf(groupBy);
        // Is it the all group descriptor
        if (groupName.equals(AllGroupDescriptor.getInstance().getName())) {
            return AllGroupDescriptor.getInstance();
        }
        // Try to find the right group descriptor
        IWeightedTreeGroupDescriptor groupDescriptor = fcProvider.getGroupDescriptor();
        while (groupDescriptor != null) {
            if (groupDescriptor.getName().equals(groupName)) {
                return groupDescriptor;
            }
            groupDescriptor = groupDescriptor.getNextGroup();
        }
        return null;
    }

    private void buildWeightedTreeEntries(IWeightedTreeSet<N, Object, WeightedTree<N>> callGraph, List<FlameChartEntryModel.Builder> builder, FlameChartEntryModel.Builder traceEntry) {
        IWeightedTreeProvider<N, E, T> wtProvider = fWtProvider;
        Collection<@NonNull ?> elements = callGraph.getElements();
        for (Object element : elements) {
            buildChildrenEntries(element, wtProvider, callGraph, builder, traceEntry);
        }
    }

    private ITmfStateSystem elementToStateSystem(IWeightedTreeProvider<N, E, T> wtProvider, IWeightedTreeSet<N, Object, WeightedTree<N>> callGraph, Object element) {
        // Create an in-memory state system for this element
        IStateHistoryBackend backend = StateHistoryBackendFactory.createInMemoryBackend("org.eclipse.tracecompass.callgraph.ss", 0L); //$NON-NLS-1$
        ITmfStateSystemBuilder ssb = StateSystemFactory.newStateSystem(backend);

        // Add the functions
        List<WeightedTree<N>> rootFunctions = new ArrayList<>(callGraph.getTreesFor(element));
        rootFunctions.sort(fCctComparator2);
        int quarkFct = ssb.getQuarkAbsoluteAndAdd(FUNCTION_LEVEL);
        Deque<Long> timestampStack = new ArrayDeque<>();
        timestampStack.push(0L);
        for (WeightedTree<N> rootFunction : rootFunctions) {
            recursivelyAddChildren(wtProvider, ssb, quarkFct, rootFunction, timestampStack);
        }
        Long endTime = timestampStack.pop();
        ssb.closeHistory(endTime);

        return ssb;
    }

    private void recursivelyAddChildren(IWeightedTreeProvider<N, E, T> wtProvider, ITmfStateSystemBuilder ssb, int quarkFct, WeightedTree<N> callSite, Deque<Long> timestampStack) {
        Long lastEnd = timestampStack.peek();
        if (lastEnd == null) {
            return;
        }
        ssb.pushAttribute(lastEnd, new CalleeCustomValue<>(callSite), quarkFct);

        // Push the children to the state system
        timestampStack.push(lastEnd);
        List<WeightedTree<N>> children = new ArrayList<>(callSite.getChildren());
        children.sort(fCctComparator2);
        for (WeightedTree<N> callsite : children) {
            recursivelyAddChildren(wtProvider, ssb, quarkFct, callsite, timestampStack);
        }
        timestampStack.pop();

        // Add the extra sites
        List<String> extraDataSets = wtProvider.getExtraDataSets();
        for (int i = 0; i < extraDataSets.size(); i++) {
            List<WeightedTree<@NonNull N>> extraDataTrees = callSite.getExtraDataTrees(i).stream().sorted(fCctComparator2).collect(Collectors.toList());
            if (extraDataTrees.isEmpty()) {
                continue;
            }
            String dataSetName = extraDataSets.get(i);
            int quarkExtra = ssb.getQuarkAbsoluteAndAdd(dataSetName);
            long extraStartTime = lastEnd;
            for (WeightedTree<@NonNull N> extraTree : extraDataTrees) {
                ssb.modifyAttribute(extraStartTime, new CalleeCustomValue<>(extraTree), quarkExtra);
                extraStartTime += extraTree.getWeight();
            }
        }

        long currentEnd = timestampStack.pop() + callSite.getWeight();
        timestampStack.push(currentEnd);
        ssb.popAttribute(currentEnd, quarkFct);
    }

    /**
     * Build the entry list for one thread
     */
    private void buildChildrenEntries(Object element, IWeightedTreeProvider<N, E, T> wtProvider, IWeightedTreeSet<N, Object, WeightedTree<N>> callGraph, List<FlameChartEntryModel.Builder> builder, FlameChartEntryModel.Builder parent) {
        // Add the entry
        FlameChartEntryModel.Builder entry = new FlameChartEntryModel.Builder(ENTRY_ID.getAndIncrement(),
                parent.getId(), (element instanceof ITree) ? String.valueOf(((ITree) element).getName()) : String.valueOf(element), 0, FlameChartEntryModel.EntryType.LEVEL, -1);
        builder.add(entry);

        // Create the hierarchy of children entries if available
        if (element instanceof ITree) {
            for (ITree child : ((ITree) element).getChildren()) {
                buildChildrenEntries(child, wtProvider, callGraph, builder, entry);
            }
        }

        // Update endtime with the children and add them to builder
        long endTime = entry.getEndTime();
        for (FlameChartEntryModel.Builder childEntry : builder) {
            if (childEntry.getParentId() == entry.getId()) {
                endTime = Math.max(childEntry.getEndTime(), endTime);
            }
        }
        entry.setEndTime(endTime);

        List<WeightedTree<N>> rootTrees = new ArrayList<>(callGraph.getTreesFor(element));
        // Create the function callsite entries
        if (rootTrees.isEmpty()) {
            return;
        }

        Deque<Long> timestampStack = new ArrayDeque<>();
        timestampStack.push(0L);

        // Get the state system to represent this callgraph
        ITmfStateSystem ss = elementToStateSystem(wtProvider, callGraph, element);
        entry.setEndTime(ss.getCurrentEndTime());

        // Add entry items for the main weighted tree levels
        int quark = ss.optQuarkAbsolute(FUNCTION_LEVEL);
        if (quark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return;
        }
        int i = 0;
        for (Integer subQuark : ss.getSubAttributes(quark, false)) {
            FlameChartEntryModel.Builder child = new FlameChartEntryModel.Builder(ENTRY_ID.getAndIncrement(), entry.getId(), String.valueOf(i), 0, EntryType.FUNCTION, i);
            child.setEndTime(ss.getCurrentEndTime());
            builder.add(child);
            i++;
            fCgEntries.put(child.getId(), new WeightedTreeEntry(ss, subQuark));
        }

        // Add items for the extra entries
        List<String> extraDataSets = wtProvider.getExtraDataSets();
        for (int set = 0; set < extraDataSets.size(); set++) {
            String dataSetName = extraDataSets.get(set);
            quark = ss.optQuarkAbsolute(dataSetName);
            if (quark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                continue;
            }
            FlameChartEntryModel.Builder child = new FlameChartEntryModel.Builder(ENTRY_ID.getAndIncrement(), entry.getId(), dataSetName, 0, EntryType.KERNEL, -1);
            child.setEndTime(ss.getCurrentEndTime());
            builder.add(child);
            fCgEntries.put(child.getId(), new WeightedTreeEntry(ss, quark));
        }
    }

    @Override
    public @NonNull TmfModelResponse<TimeGraphModel> fetchRowModel(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        SubMonitor subMonitor = Objects.requireNonNull(SubMonitor.convert(monitor, "FlameGraphDataProvider#fetchRowModel", 2)); //$NON-NLS-1$

        List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        if (times == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        List<ITimeGraphRowModel> rowModels = new ArrayList<>();

        // Get the selected entries
        Collection<Long> selected = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (selected == null) {
            // No entry selected, assume all
            selected = fEntries.keySet();
        }
        List<WeightedTreeEntry> selectedEntries = new ArrayList<>();
        Multimap<WeightedTreeEntry, Pair<Integer, Long>> requested = HashMultimap.create();

        for (Long id : selected) {
            WeightedTreeEntry entry = fCgEntries.get(id);
            if (entry != null) {
                selectedEntries.add(entry);
                requested.put(entry, new Pair<>(entry.fQuark, id));
            }
        }

        // Prepare the regexes
        Map<Integer, Predicate<Multimap<String, Object>>> predicates = new HashMap<>();
        Multimap<Integer, String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        if (subMonitor.isCanceled()) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
        }

        // For each element and callgraph, get the states
        try {
            for (WeightedTreeEntry element : requested.keySet()) {
                if (subMonitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                Collection<Pair<Integer, Long>> depths = Objects.requireNonNull(requested.get(element));
                rowModels.addAll(getStatesForElement(times, predicates, subMonitor, element.fSs, depths));
            }
        } catch (StateSystemDisposedException e) {
            // Nothing to do
        }

        return new TmfModelResponse<>(new TimeGraphModel(rowModels), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @SuppressWarnings("null")
    private List<ITimeGraphRowModel> getStatesForElement(List<Long> times, Map<Integer, Predicate<Multimap<String, Object>>> predicates, IProgressMonitor monitor,
            ITmfStateSystem ss, Collection<Pair<Integer, Long>> depths) throws StateSystemDisposedException {
        List<Integer> quarks = new ArrayList<>();
        for (Pair<Integer, Long> pair : depths) {
            quarks.add(pair.getFirst());
        }
        TreeMultimap<Integer, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        long ssEndTime = ss.getCurrentEndTime();
        for (ITmfStateInterval interval : ss.query2D(quarks, times)) {
            if (monitor.isCanceled()) {
                return Collections.emptyList();
            }
            // Ignore the null intervals of value 1 at the end of the state
            // system
            if (interval.getStartTime() == ssEndTime &&
                    interval.getStartTime() == interval.getEndTime() &&
                    interval.getValue() == null) {
                continue;
            }
            intervals.put(interval.getAttribute(), interval);
        }

        List<ITimeGraphRowModel> rows = new ArrayList<>();
        for (Pair<Integer, Long> pair : depths) {
            int quark = pair.getFirst();
            NavigableSet<ITmfStateInterval> states = intervals.get(quark);

            if (monitor.isCanceled()) {
                return Collections.emptyList();
            }
            List<ITimeGraphState> eventList = new ArrayList<>();
            Long key = Objects.requireNonNull(pair.getSecond());
            states.forEach(i -> {
                ITimeGraphState timegraphState = createTimeGraphState(i, ssEndTime);
                applyFilterAndAddState(eventList, timegraphState, key, predicates, monitor);
            });
            rows.add(new TimeGraphRowModel(key, eventList));
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private ITimeGraphState createTimeGraphState(ITmfStateInterval interval, long ssEndTime) {
        IWeightedTreeProvider<N, E, T> wtProvider = fWtProvider;
        long startTime = interval.getStartTime();
        long duration = interval.getEndTime() - startTime + (ssEndTime == interval.getEndTime() ? 0 : 1);
        Object valueObject = interval.getValue();

        if (valueObject instanceof CalleeCustomValue) {
            WeightedTree<N> callsite = ((CalleeCustomValue<N>) valueObject).fCallSite;
            String displayString = wtProvider.toDisplayString((T) callsite);
            return new TimeGraphState(startTime, duration, displayString, fWtProvider.getPalette().getStyleFor(callsite));
        }
        return new TimeGraphState(startTime, duration, Integer.MIN_VALUE);
    }

    @Override
    public @NonNull TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(Collections.emptyList(), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        if (times == null || times.size() != 1) {
            return new TmfModelResponse<>(Collections.emptyMap(), ITmfResponse.Status.FAILED, "Invalid time requested for tooltip"); //$NON-NLS-1$
        }
        List<Long> items = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (items == null || items.size() != 1) {
            return new TmfModelResponse<>(Collections.emptyMap(), ITmfResponse.Status.FAILED, "Invalid selection requested for tooltip"); //$NON-NLS-1$
        }
        Long time = times.get(0);
        Long item = items.get(0);
        WeightedTreeEntry callGraphEntry = fCgEntries.get(item);
        if (callGraphEntry == null) {
            return new TmfModelResponse<>(Collections.emptyMap(), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        WeightedTree<@NonNull N> callSite = findCallSite(callGraphEntry, time);
        if (callSite != null) {
            Object actions = fetchParameters.get(TOOLTIP_ACTION_KEY);
            if (actions == null) {
                // Return the normal tooltip
                return new TmfModelResponse<>(getTooltip(callSite), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }
            // Return the actions for this entry
            return new TmfModelResponse<>(getTooltipActions(callSite), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        return new TmfModelResponse<>(Collections.emptyMap(), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private Map<String, String> getTooltipActions(WeightedTree<@NonNull N> callSite) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        /* Goto min/max actions */
        // Try to get the statistics of the main metric
        @SuppressWarnings("unchecked")
        IStatistics<?> statistics = fWtProvider.getStatistics((T) callSite, -1);
        if (statistics != null) {
            Object minObject = statistics.getMinObject();
            if (minObject instanceof ISegment) {
                ISegment minimum = (ISegment) minObject;
                builder.put(DataProviderUtils.ACTION_PREFIX + Messages.FlameGraph_GoToMin, DataProviderUtils.createGoToTimeAction(minimum.getStart(), minimum.getEnd()));
            }
            Object maxObject = statistics.getMaxObject();
            if (maxObject instanceof ISegment) {
                ISegment maximum = (ISegment) maxObject;
                builder.put(DataProviderUtils.ACTION_PREFIX + Messages.FlameGraph_GoToMax, DataProviderUtils.createGoToTimeAction(maximum.getStart(), maximum.getEnd()));
            }
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getTooltip(WeightedTree<@NonNull N> callSite) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        // Display the object name first
        String string = callSite.getObject().toString();
        String displayString = fWtProvider.toDisplayString((T) callSite);
        builder.put(Objects.requireNonNull(Messages.FlameGraph_Object), string.equals(displayString) ? displayString : displayString + ' ' + '(' + string + ')');
        List<MetricType> additionalMetrics = fWtProvider.getAdditionalMetrics();

        // First, display the metrics that do not have statistics to not loose
        // them in the middle of stats
        MetricType metric = fWtProvider.getWeightType();
        if (!metric.hasStatistics()) {
            builder.put(metric.getTitle(), metric.format(callSite.getWeight()));
        }
        for (int i = 0; i < additionalMetrics.size(); i++) {
            MetricType otherMetric = additionalMetrics.get(i);
            if (!otherMetric.hasStatistics()) {
                builder.put(otherMetric.getTitle(), otherMetric.format(fWtProvider.getAdditionalMetric((T) callSite, i)));
            }
        }

        // Then, display the metrics with statistics
        if (metric.hasStatistics()) {
            builder.putAll(getMetricWithStatTooltip(metric, callSite, -1));
        }
        for (int i = 0; i < additionalMetrics.size(); i++) {
            MetricType otherMetric = additionalMetrics.get(i);
            if (otherMetric.hasStatistics()) {
                builder.putAll(getMetricWithStatTooltip(otherMetric, callSite, i));
            }
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getMetricWithStatTooltip(MetricType metric, WeightedTree<@NonNull N> callSite, int metricIndex) {
        Object metricValue = metricIndex < 0 ? callSite.getWeight() : fWtProvider.getAdditionalMetric((T) callSite, metricIndex);
        IStatistics<?> statistics = fWtProvider.getStatistics((T) callSite, metricIndex);
        Map<String, String> map = new LinkedHashMap<>();
        if (statistics == null || statistics.getMax() == IHostModel.TIME_UNKNOWN) {
            map.put(metric.getTitle(), metric.format(metricValue));
        } else {
            map.put(metric.getTitle(), StringUtils.EMPTY);
            String lowerTitle = metric.getTitle().toLowerCase();
            map.put("\t" + Messages.FlameGraph_Total + ' ' + lowerTitle, metric.format(statistics.getTotal())); //$NON-NLS-1$
            map.put("\t" + Messages.FlameGraph_Average + ' ' + lowerTitle, metric.format(statistics.getMean())); //$NON-NLS-1$
            map.put("\t" + Messages.FlameGraph_Max + ' ' + lowerTitle, metric.format(statistics.getMax())); //$NON-NLS-1$
            map.put("\t" + Messages.FlameGraph_Min + ' ' + lowerTitle, metric.format(statistics.getMin())); //$NON-NLS-1$
            map.put("\t" + Messages.FlameGraph_Deviation + ' ' + lowerTitle, metric.format(statistics.getStdDev())); //$NON-NLS-1$
        }
        return map;
    }

    /** Find the callsite at the time and depth requested */
    @SuppressWarnings("unchecked")
    private @Nullable WeightedTree<@NonNull N> findCallSite(WeightedTreeEntry cgEntry, Long time) {
        if (time < 0 || time > cgEntry.fSs.getCurrentEndTime()) {
            return null;
        }
        try {
            ITmfStateInterval interval = cgEntry.fSs.querySingleState(time, cgEntry.fQuark);

            Object valueObject = interval.getValue();
            if (valueObject instanceof CalleeCustomValue) {
                return ((CalleeCustomValue<N>) valueObject).fCallSite;
            }
        } catch (StateSystemDisposedException e) {
            // Nothing to do
        }
        return null;
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        IDataPalette palette = fWtProvider.getPalette();
        return new TmfModelResponse<>(new OutputStyleModel(palette.getStyles()), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }
}
