/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStack;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackDepth;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackSeries;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.FlameDefaultPalette;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.FlameWithKernelPalette;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.FlameChartEntryModel.EntryType;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadEntryModel;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadStatusDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
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
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderUtils;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * This class provides the data from an instrumented callstack analysis, in the
 * form of a flamechart, ie the groups are returned hierarchically and leaf
 * groups return their callstacks.
 *
 * @author Geneviève Bastien
 */
public class FlameChartDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<FlameChartEntryModel>, IOutputStyleProvider {

    /**
     * Provider ID.
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.callstack.core.flamechart"; //$NON-NLS-1$

    private static final AtomicLong ENTRY_ID = new AtomicLong();
    /**
     * Logger for Abstract Tree Data Providers.
     */
    private static final Logger LOGGER = TraceCompassLog.getLogger(FlameChartDataProvider.class);

    private final Map<Long, FlameChartEntryModel> fEntries = new HashMap<>();
    // Key is the row ID that requires linked data (for instance a kernel row)
    // and value is the row being linked to (the one from the callstack)
    private final BiMap<Long, Long> fLinkedEntries = HashBiMap.create();
    private final Collection<ISymbolProvider> fProviders = new ArrayList<>();
    private final BiMap<Long, CallStackDepth> fIdToCallstack = HashBiMap.create();
    private final BiMap<Long, ICallStackElement> fIdToElement = HashBiMap.create();
    private final long fTraceId = ENTRY_ID.getAndIncrement();

    /** Cache for entry metadata */
    private final Map<Long, @NonNull Multimap<@NonNull String, @NonNull Object>> fEntryMetadata = new HashMap<>();

    private static class TidInformation {
        private final HostThread fTid;
        private final long fStart;
        private final long fEnd;
        /*
         * The ID of the entry in this data provider where to put the thread
         * information
         */
        private final Long fLinked;

        public TidInformation(HostThread hostThread, long start, long end, Long linked) {
            fTid = hostThread;
            fStart = start;
            fEnd = end;
            fLinked = linked;
        }

        public boolean intersects(ITimeGraphState state) {
            return !(state.getStartTime() > fEnd || (state.getStartTime() + state.getDuration()) < fStart);
        }

        public boolean precedes(ITimeGraphState state) {
            return (state.getStartTime() + state.getDuration() < fEnd);
        }

        public ITimeGraphState sanitize(ITimeGraphState state) {
            if (state.getStartTime() < fStart || state.getStartTime() + state.getDuration() > fEnd) {
                long start = Math.max(state.getStartTime(), fStart);
                long end = Math.min(state.getStartTime() + state.getDuration(), fEnd);
                return new TimeGraphState(start, end - start, state.getLabel(), state.getStyle());
            }
            return state;
        }
    }

    private static class ThreadData {
        private final ThreadStatusDataProvider fThreadDataProvider;
        private final List<ThreadEntryModel> fThreadTree = new ArrayList<>();
        private final Status fStatus;

        public ThreadData(ThreadStatusDataProvider dataProvider, List<TimeGraphEntryModel> threadTree, Status status) {
            fThreadDataProvider = dataProvider;
            for (TimeGraphEntryModel model : threadTree) {
                if (model instanceof ThreadEntryModel) {
                    fThreadTree.add((ThreadEntryModel) model);
                }
            }
            fStatus = status;
        }

        public @Nullable Map<String, String> fetchTooltip(int threadId, long time, @Nullable IProgressMonitor monitor) {
            for (ThreadEntryModel entry : fThreadTree) {
                if (entry.getThreadId() == threadId && entry.getStartTime() <= time && entry.getEndTime() >= time) {
                    TmfModelResponse<Map<String, String>> tooltip = fThreadDataProvider.fetchTooltip(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(Collections.singletonList(time), Collections.singleton(entry.getId()))),
                            monitor);
                    return tooltip.getModel();
                }
            }
            return null;
        }
    }

    private final LoadingCache<Pair<Integer, ICalledFunction>, @Nullable String> fTimeEventNames = Objects.requireNonNull(CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<Pair<Integer, ICalledFunction>, @Nullable String>() {
                @Override
                public @Nullable String load(Pair<Integer, ICalledFunction> pidInterval) {
                    Integer pid = pidInterval.getFirst();
                    ICalledFunction interval = pidInterval.getSecond();

                    Object nameValue = interval.getSymbol();
                    Long address = null;
                    String name = null;
                    if (nameValue instanceof String) {
                        name = (String) nameValue;
                        try {
                            address = Long.parseLong(name, 16);
                        } catch (NumberFormatException e) {
                            // leave name as null
                        }
                    } else if (nameValue instanceof Integer) {
                        Integer intValue = (Integer) nameValue;
                        name = "0x" + Integer.toUnsignedString(intValue, 16); //$NON-NLS-1$
                        address = intValue.longValue();
                    } else if (nameValue instanceof Long) {
                        address = (long) nameValue;
                        name = "0x" + Long.toUnsignedString(address, 16); //$NON-NLS-1$
                    }
                    if (address != null) {
                        name = SymbolProviderUtils.getSymbolText(fProviders, pid, interval.getStart(), address);
                    }
                    return name;
                }
            }));

    private final IFlameChartProvider fFcProvider;
    private final String fAnalysisId;
    private final FlameChartArrowProvider fArrowProvider;
    private @Nullable TmfModelResponse<TmfTreeModel<FlameChartEntryModel>> fCached;
    private @Nullable ThreadData fThreadData = null;

    /**
     * Constructor
     *
     * @param trace
     *            The trace for which this data provider applies
     * @param module
     *            The flame chart provider encapsulated by this provider
     * @param secondaryId
     *            The ID of the flame chart provider
     */
    public FlameChartDataProvider(ITmfTrace trace, IFlameChartProvider module, String secondaryId) {
        super(trace);
        fFcProvider = module;
        fAnalysisId = secondaryId;
        fArrowProvider = new FlameChartArrowProvider(trace);
        resetFunctionNames(new NullProgressMonitor());
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        List<ITmfStateInterval> arrows = fArrowProvider.fetchArrows(fetchParameters, monitor);
        if (monitor != null && monitor.isCanceled()) {
            return new TmfModelResponse<>(null, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
        }
        if (arrows.isEmpty()) {
            return new TmfModelResponse<>(Collections.emptyList(), Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        List<ITimeGraphArrow> tgArrows = new ArrayList<>();
        // First, get the distinct callstacks
        List<CallStackDepth> csList = new ArrayList<>();
        synchronized (fIdToCallstack) {
            // Quick copy the values to a list to avoid keeping the lock for too
            // long, adding to a hashSet takes time to calculate the CallStack's
            // hash.
            csList.addAll(fIdToCallstack.values());
        }
        Set<CallStack> callstacks = new HashSet<>();
        for (CallStackDepth csd : csList) {
            callstacks.add(csd.getCallStack());
        }

        // Find the source and destination entry for each arrow
        for (ITmfStateInterval interval : arrows) {
            if (monitor != null && monitor.isCanceled()) {
                return new TmfModelResponse<>(null, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            EdgeStateValue edge = (EdgeStateValue) interval.getValue();
            if (edge == null) {
                /*
                 * by contract all the intervals should have EdgeStateValues but
                 * need to check to avoid NPE
                 */
                continue;
            }
            Long src = findEntry(callstacks, edge.getSource(), interval.getStartTime());
            Long dst = findEntry(callstacks, edge.getDestination(), interval.getEndTime() + 1);
            if (src != null && dst != null) {
                long duration = interval.getEndTime() - interval.getStartTime() + 1;
                tgArrows.add(new TimeGraphArrow(src, dst, interval.getStartTime(), duration, edge.getId()));
            }
        }

        return new TmfModelResponse<>(tgArrows, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private @Nullable Long findEntry(Set<CallStack> callstacks, HostThread hostThread, long ts) {
        for (CallStack callstack : callstacks) {
            // Get the host thread running on the callstack and compare with
            // desired
            HostThread csHt = callstack.getHostThread(ts);
            if (csHt == null || !csHt.equals(hostThread)) {
                continue;
            }
            // We found the callstack, find the right depth and its entry id
            int currentDepth = callstack.getCurrentDepth(ts);
            CallStackDepth csd = new CallStackDepth(callstack, currentDepth);
            synchronized (fIdToCallstack) {
                return fIdToCallstack.inverse().get(csd);
            }
        }
        return null;
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameChartDataProvider#fetchTooltip") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {
            List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
            if (times == null || times.isEmpty()) {
                // No time specified
                return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }
            Map<Long, FlameChartEntryModel> entries = getSelectedEntries(fetchParameters);
            if (entries.size() != 1) {
                // Not the expected size of tooltip, just return empty
                return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }
            Entry<@NonNull Long, @NonNull FlameChartEntryModel> entry = entries.entrySet().iterator().next();
            Map<String, String> tooltip = getTooltip(entry.getKey(), entry.getValue(), times.get(0), monitor);

            return new TmfModelResponse<>(tooltip, Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
    }

    private @Nullable Map<String, String> getTooltip(Long entryId, FlameChartEntryModel entryModel, Long time, @Nullable IProgressMonitor monitor) {
        switch (entryModel.getEntryType()) {
        case FUNCTION: {
            CallStackDepth selectedDepth = fIdToCallstack.get(entryId);
            if (selectedDepth == null) {
                return null;
            }
            Multimap<CallStackDepth, ISegment> csFunctions = fFcProvider.queryCallStacks(Collections.singleton(selectedDepth), Collections.singleton(time));
            Collection<ISegment> functions = csFunctions.get(selectedDepth);
            if (functions.isEmpty()) {
                return null;
            }
            ISegment next = functions.iterator().next();
            if (!(next instanceof ICalledFunction)) {
                return null;
            }
            ICalledFunction currentFct = (ICalledFunction) next;
            Map<String, String> tooltips = new HashMap<>();
            int threadId = currentFct.getThreadId();
            if (threadId > 0) {
                tooltips.put(String.valueOf(Messages.FlameChartDataProvider_ThreadId), String.valueOf(threadId));
            }
            Object symbol = currentFct.getSymbol();
            tooltips.put(String.valueOf(Messages.FlameChartDataProvider_Symbol), symbol instanceof Long ? "0x" + Long.toHexString((Long) symbol) : String.valueOf(symbol)); //$NON-NLS-1$
            // TODO: Add symbol origin (library, language, etc) when better
            // supported
            return tooltips;
        }
        case KERNEL:
            // Get the tooltip from the the ThreadStatusDataProvider
            // First get the linked function to know which TID to retrieve
            Long csId = fLinkedEntries.get(entryId);
            if (csId == null) {
                return null;
            }
            CallStackDepth selectedDepth = fIdToCallstack.get(csId);

            if (selectedDepth == null) {
                return null;
            }
            int threadId = selectedDepth.getCallStack().getThreadId(time);
            ThreadData threadData = fThreadData;
            if (threadData == null) {
                return null;
            }
            return threadData.fetchTooltip(threadId, time, monitor);
        case LEVEL:
        case TRACE:
        default:
            return null;
        }
    }

    @Override
    public String getId() {
        return ID + ':' + fAnalysisId;
    }

    // Get an entry for a quark
    private long getEntryId(CallStackDepth stack) {
        synchronized (fIdToCallstack) {
            return fIdToCallstack.inverse().computeIfAbsent(stack, q -> ENTRY_ID.getAndIncrement());
        }
    }

    private long getEntryId(ICallStackElement instrumentedCallStackElement) {
        return fIdToElement.inverse().computeIfAbsent(instrumentedCallStackElement, q -> ENTRY_ID.getAndIncrement());
    }

    // Get a new entry for a kernel entry ID
    private long getKernelEntryId(long baseId) {
        return fLinkedEntries.inverse().computeIfAbsent(baseId, id -> ENTRY_ID.getAndIncrement());
    }

    @Override
    public TmfModelResponse<TmfTreeModel<FlameChartEntryModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        if (fCached != null) {
            return fCached;
        }

        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameChartDataProvider#fetchTree") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {
            IFlameChartProvider fcProvider = fFcProvider;
            boolean complete = fcProvider.isComplete();
            CallStackSeries callstack = fcProvider.getCallStackSeries();
            if (callstack == null) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
            }
            long start = getTrace().getStartTime().getValue();
            long end = Math.max(start, fcProvider.getEnd());

            // Initialize the first element of the tree
            ImmutableList.Builder<FlameChartEntryModel> builder = ImmutableList.builder();
            @SuppressWarnings("null")
            FlameChartEntryModel traceEntry = new FlameChartEntryModel(fTraceId, -1, Collections.singletonList(getTrace().getName()), start, end, FlameChartEntryModel.EntryType.TRACE);
            builder.add(traceEntry);

            FlameChartEntryModel callStackRoot = traceEntry;
            // If there is more than one callstack objects in the analysis,
            // create a root per series
            boolean needsKernel = false;
            for (ICallStackElement element : callstack.getRootElements()) {
                if (monitor != null && monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                needsKernel |= processCallStackElement(element, builder, callStackRoot);
            }
            // Initialize the thread status data provider
            if (needsKernel) {
                prepareKernelData(monitor, start);
            }
            List<FlameChartEntryModel> tree = builder.build();
            tree.forEach(entry -> fEntries.put(entry.getId(), entry));

            for (FlameChartEntryModel model : tree) {
                fEntryMetadata.put(model.getId(), model.getMetadata());
            }

            if (complete) {
                TmfModelResponse<TmfTreeModel<FlameChartEntryModel>> response = new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), tree),
                        ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
                fCached = response;
                return response;
            }
            return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), tree), ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING);
        }
    }

    private void prepareKernelData(@Nullable IProgressMonitor monitor, long start) {
        ThreadData data = fThreadData;
        if (data != null && data.fStatus == Status.COMPLETED) {
            return;
        }
        // FIXME: Wouldn't work correctly if trace is an experiment as it would
        // cover many hosts
        Set<ITmfTrace> tracesForHost = TmfTraceManager.getInstance().getTracesForHost(getTrace().getHostId());
        for (ITmfTrace trace : tracesForHost) {
            ThreadStatusDataProvider dataProvider = DataProviderManager.getInstance().getOrCreateDataProvider(trace, ThreadStatusDataProvider.ID, ThreadStatusDataProvider.class);
            if (dataProvider != null) {
                // Get the tree for the trace's current range
                TmfModelResponse<TmfTreeModel<TimeGraphEntryModel>> threadTreeResp = dataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(start, Long.MAX_VALUE, 2)), monitor);
                TmfTreeModel<TimeGraphEntryModel> threadTree = threadTreeResp.getModel();
                if (threadTree != null) {
                    fThreadData = new ThreadData(dataProvider, threadTree.getEntries(), threadTreeResp.getStatus());
                    break;
                }
            }
        }
    }

    private boolean processCallStackElement(ICallStackElement element, Builder<FlameChartEntryModel> builder, FlameChartEntryModel parentEntry) {
        long elementId = getEntryId(element);
        boolean needsKernel = false;

        // Is this an intermediate or leaf element
        if ((element instanceof InstrumentedCallStackElement) && element.isLeaf()) {
            // For the leaf element, add the callstack entries
            InstrumentedCallStackElement finalElement = (InstrumentedCallStackElement) element;
            CallStack callStack = finalElement.getCallStack();
            // Set the fixed hostThread to the entry if it is available
            HostThread hostThread = callStack.getHostThread();
            // Create the entry for this level
            FlameChartEntryModel entry = new FlameChartEntryModel(elementId, parentEntry.getId(), Collections.singletonList(element.getName()), parentEntry.getStartTime(), parentEntry.getEndTime(), FlameChartEntryModel.EntryType.LEVEL, -1,
                    hostThread);
            builder.add(entry);
            for (int depth = 0; depth < callStack.getMaxDepth(); depth++) {
                FlameChartEntryModel flameChartEntry = new FlameChartEntryModel(getEntryId(new CallStackDepth(callStack, depth + 1)), entry.getId(), Collections.singletonList(element.getName()), parentEntry.getStartTime(), parentEntry.getEndTime(),
                        FlameChartEntryModel.EntryType.FUNCTION, depth + 1, hostThread);
                builder.add(flameChartEntry);
                if (depth == 0 && callStack.hasKernelStatuses()) {
                    needsKernel = true;
                    builder.add(new FlameChartEntryModel(getKernelEntryId(flameChartEntry.getId()), entry.getId(), Collections.singletonList(String.valueOf(Messages.FlameChartDataProvider_KernelStatusTitle)), parentEntry.getStartTime(),
                            parentEntry.getEndTime(),
                            FlameChartEntryModel.EntryType.KERNEL, -1, hostThread));
                }
            }
            return needsKernel;
        }

        // Intermediate element, create entry and process children
        FlameChartEntryModel entry = new FlameChartEntryModel(elementId, parentEntry.getId(), Collections.singletonList(element.getName()), parentEntry.getStartTime(), parentEntry.getEndTime(), FlameChartEntryModel.EntryType.LEVEL);
        builder.add(entry);
        for (ICallStackElement child : element.getChildrenElements()) {
            needsKernel |= processCallStackElement(child, builder, entry);
        }
        return needsKernel;
    }

    // Get the selected entries with the quark
    private BiMap<Long, FlameChartEntryModel> getSelectedEntries(Map<String, Object> fetchParameters) {
        BiMap<Long, FlameChartEntryModel> selectedEntries = HashBiMap.create();

        List<Long> ids = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (ids == null) {
            return selectedEntries;
        }
        for (Long selectedItem : ids) {
            FlameChartEntryModel entryModel = fEntries.get(selectedItem);
            if (entryModel != null) {
                selectedEntries.put(selectedItem, entryModel);
            }
        }
        return selectedEntries;
    }

    @Override
    public TmfModelResponse<TimeGraphModel> fetchRowModel(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameChartDataProvider#fetchRowModel") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {

            Map<Long, FlameChartEntryModel> entries = getSelectedEntries(fetchParameters);
            List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
            if (times == null) {
                // No time specified
                return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }
            if (entries.size() == 1 && times.size() == 2) {
                // this is a request for a follow event.
                Entry<@NonNull Long, @NonNull FlameChartEntryModel> entry = entries.entrySet().iterator().next();
                if (times.get(0) == Long.MIN_VALUE) {
                    List<ITimeGraphRowModel> followEvents = getFollowEvent(entry, times.get(times.size() - 1), false);
                    TimeGraphModel model = followEvents == null ? null : new TimeGraphModel(followEvents);
                    return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
                } else if (times.get(times.size() - 1) == Long.MAX_VALUE) {
                    List<ITimeGraphRowModel> followEvents = getFollowEvent(entry, times.get(0), true);
                    TimeGraphModel model = followEvents == null ? null : new TimeGraphModel(followEvents);
                    return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
                }
            }
            // For each kernel status entry, add the first row of the callstack
            addRequiredCallstacks(entries);

            SubMonitor subMonitor = SubMonitor.convert(monitor, "FlameChartDataProvider#fetchRowModel", 2); //$NON-NLS-1$
            IFlameChartProvider fcProvider = fFcProvider;
            boolean complete = fcProvider.isComplete();

            Map<Long, List<ITimeGraphState>> csRows = getCallStackRows(fetchParameters, entries, subMonitor);
            if (csRows == null) {
                // getRowModel returns null if the query was cancelled.
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            List<ITimeGraphRowModel> collect = csRows.entrySet().stream().map(entry -> new TimeGraphRowModel(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            return new TmfModelResponse<>(new TimeGraphModel(collect), complete ? Status.COMPLETED : Status.RUNNING,
                    complete ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING);
        } catch (IndexOutOfBoundsException | TimeRangeException e) {
            return new TmfModelResponse<>(null, Status.FAILED, String.valueOf(e.getMessage()));
        }
    }

    private void addRequiredCallstacks(Map<Long, FlameChartEntryModel> entries) {
        Map<Long, FlameChartEntryModel> toAdd = new HashMap<>();
        for (Long id : entries.keySet()) {
            Long csId = fLinkedEntries.get(id);
            if (csId != null) {
                FlameChartEntryModel entry = fEntries.get(csId);
                if (entry != null) {
                    toAdd.put(csId, entry);
                }
            }
        }
        entries.putAll(toAdd);
    }

    @SuppressWarnings("null")
    private @Nullable Map<Long, List<ITimeGraphState>> getCallStackRows(Map<String, Object> fetchParameters, Map<Long, FlameChartEntryModel> entries, SubMonitor subMonitor)
            throws IndexOutOfBoundsException, TimeRangeException {

        // Get the data for the model entries that are of type function
        Map<Long, List<ITimeGraphState>> rows = new HashMap<>();
        List<TidInformation> tids = new ArrayList<>();
        Map<Long, CallStackDepth> csEntries = new HashMap<>();
        for (Entry<Long, @NonNull FlameChartEntryModel> entry : entries.entrySet()) {
            CallStackDepth selectedDepth = fIdToCallstack.get(entry.getKey());
            if (selectedDepth != null && entry.getValue().getEntryType() == EntryType.FUNCTION) {
                csEntries.put(entry.getKey(), selectedDepth);
            }
        }

        List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        Collections.sort(times);
        Multimap<CallStackDepth, ISegment> csFunctions = fFcProvider.queryCallStacks(csEntries.values(), Objects.requireNonNull(times));

        // Prepare the regexes
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        for (Map.Entry<Long, CallStackDepth> entry : csEntries.entrySet()) {
            if (subMonitor.isCanceled()) {
                return null;
            }
            Collection<ISegment> states = csFunctions.get(entry.getValue());
            Long key = Objects.requireNonNull(entry.getKey());

            // Create the time graph states for this row
            List<ITimeGraphState> eventList = new ArrayList<>(states.size());
            states.forEach(state -> {
                ITimeGraphState timeGraphState = createTimeGraphState(state);
                applyFilterAndAddState(eventList, timeGraphState, key, predicates, subMonitor);
            });
            eventList.sort(Comparator.comparingLong(ITimeGraphState::getStartTime));
            rows.put(entry.getKey(), eventList);

            // See if any more row needs to be filled with these function's data
            // TODO: Kernel might not be the only type of linked entries (for
            // instance, locations of sampling data)
            Long linked = fLinkedEntries.inverse().get(entry.getKey());
            if (linked == null || !entries.containsKey(linked)) {
                continue;
            }
            tids.addAll(getKernelTids(entry.getValue(), states, linked));
        }
        // Add an empty state to rows that do not have data
        for (Long key : entries.keySet()) {
            if (!rows.containsKey(key)) {
                rows.put(key, Collections.emptyList());
            }
        }
        if (!tids.isEmpty()) {
            rows.putAll(getKernelStates(tids, times, predicates, subMonitor));
        }
        subMonitor.worked(1);
        return rows;
    }

    private Map<Long, List<ITimeGraphState>> getKernelStates(List<TidInformation> tids, List<Long> times, Map<Integer, Predicate<Multimap<String, Object>>> predicates, SubMonitor monitor) {
        // Get the thread statuses from the thread status provider
        ThreadData threadData = fThreadData;
        if (threadData == null) {
            return Collections.emptyMap();
        }
        List<ThreadEntryModel> tree = threadData.fThreadTree;

        // FIXME: A callstack analysis may be for an experiment that span many
        // hosts, the thread data provider will be a composite and the models
        // may be for different host IDs. But for now, suppose the callstack is
        // a composite also and the trace filtered the right host.
        BiMap<Long, Integer> threadModelIds = filterThreads(tree, tids);
        SelectionTimeQueryFilter tidFilter = new SelectionTimeQueryFilter(times, threadModelIds.keySet());
        TmfModelResponse<TimeGraphModel> rowModel = threadData.fThreadDataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(tidFilter), monitor);
        TimeGraphModel rowModels = rowModel.getModel();
        if (rowModel.getStatus() == Status.CANCELLED || rowModel.getStatus() == Status.FAILED || rowModels == null) {
            return Collections.emptyMap();
        }
        return mapThreadStates(rowModels.getRows(), threadModelIds, tids, predicates, monitor);
    }

    private Map<Long, List<ITimeGraphState>> mapThreadStates(List<ITimeGraphRowModel> rowModels, BiMap<Long, Integer> threadModelIds, List<TidInformation> tids, Map<Integer, Predicate<Multimap<String, Object>>> predicates, SubMonitor monitor) {
        ImmutableMap<Long, ITimeGraphRowModel> statusRows = Maps.uniqueIndex(rowModels, m -> m.getEntryID());
        // Match the states of thread status to the requested tid lines
        Long prevId = -1L;
        List<ITimeGraphState> states = null;
        Map<Long, List<ITimeGraphState>> kernelStatuses = new HashMap<>();
        // The tid information data are ordered by id and times
        for (TidInformation tidInfo : tids) {
            // Get the ID of the linked callstack entry ID to get the filter
            // data
            Long linkedCsEntryId = fLinkedEntries.get(tidInfo.fLinked);
            if (linkedCsEntryId == null) {
                // fallback to the entry's own ID
                linkedCsEntryId = tidInfo.fLinked;
            }
            Long tidEntryId = threadModelIds.inverse().get(tidInfo.fTid.getTid());
            if (tidEntryId == null) {
                continue;
            }
            ITimeGraphRowModel rowModel = statusRows.get(tidEntryId);
            if (!Objects.equals(tidInfo.fLinked, prevId) || states == null) {
                if (states != null) {
                    kernelStatuses.put(prevId, states);
                }
                states = new ArrayList<>();
            }
            rowModel.getStates();
            for (ITimeGraphState state : rowModel.getStates()) {
                if (tidInfo.intersects(state)) {
                    ITimeGraphState timeGraphState = tidInfo.sanitize(state);
                    // Use the callstack entry's filter data
                    applyFilterAndAddState(states, timeGraphState, linkedCsEntryId, predicates, monitor);
                }
                if (!tidInfo.precedes(state)) {
                    break;
                }
            }
            prevId = tidInfo.fLinked;
        }
        if (states != null) {
            kernelStatuses.put(prevId, states);
        }
        return kernelStatuses;
    }

    private static BiMap<Long, Integer> filterThreads(List<ThreadEntryModel> model, List<TidInformation> tids) {
        // Get the entry model IDs that match requested tids
        BiMap<Long, Integer> tidEntries = HashBiMap.create();
        Set<Integer> selectedTids = new HashSet<>();
        for (TidInformation tidInfo : tids) {
            selectedTids.add(tidInfo.fTid.getTid());
        }
        for (ThreadEntryModel entryModel : model) {
            if (selectedTids.contains(entryModel.getThreadId())) {
                try {
                    tidEntries.put(entryModel.getId(), entryModel.getThreadId());
                } catch (IllegalArgumentException e) {
                    // FIXME: There may be many entries for one tid, don't rely
                    // on exception for real workflow. Works for now.
                }
            }
        }
        return tidEntries;
    }

    private static Collection<TidInformation> getKernelTids(CallStackDepth callStackDepth, Collection<ISegment> states, Long linked) {
        List<TidInformation> tids = new ArrayList<>();
        CallStack callStack = callStackDepth.getCallStack();
        if (!callStack.isTidVariable()) {
            // Find the time of the first function to know which timestamp to
            // query
            HostThread hostThread = callStack.getHostThread();
            if (hostThread != null) {
                tids.add(new TidInformation(hostThread, Long.MIN_VALUE, Long.MAX_VALUE, linked));
            }
            return tids;
        }
        // Get the thread IDs for all functions
        for (ISegment state : states) {
            if (!(state instanceof ICalledFunction)) {
                continue;
            }
            ICalledFunction function = (ICalledFunction) state;
            HostThread hostThread = callStack.getHostThread(function.getStart());
            if (hostThread != null) {
                tids.add(new TidInformation(hostThread, function.getStart(), function.getEnd(), linked));
            }
        }
        return tids;
    }

    private ITimeGraphState createTimeGraphState(ISegment state) {
        if (!(state instanceof ICalledFunction)) {
            return new TimeGraphState(state.getStart(), state.getLength(), Integer.MIN_VALUE);
        }
        ICalledFunction function = (ICalledFunction) state;
        Integer pid = function.getProcessId();
        String name = String.valueOf(fTimeEventNames.getUnchecked(new Pair<>(pid, function)));
        return new TimeGraphState(function.getStart(), function.getLength(), name, FlameDefaultPalette.getInstance().getStyleFor(state));
    }

    /**
     * Invalidate the function names cache and load the symbol providers. This
     * function should be used at the beginning of the provider, or whenever new
     * symbol providers are added
     *
     * @param monitor
     *            A progress monitor to follow this operation
     */
    public void resetFunctionNames(IProgressMonitor monitor) {
        fTimeEventNames.invalidateAll();
        synchronized (fProviders) {
            Collection<@NonNull ISymbolProvider> symbolProviders = SymbolProviderManager.getInstance().getSymbolProviders(getTrace());
            SubMonitor sub = SubMonitor.convert(monitor, "CallStackDataProvider#resetFunctionNames", symbolProviders.size()); //$NON-NLS-1$
            fProviders.clear();
            for (ISymbolProvider symbolProvider : symbolProviders) {
                fProviders.add(symbolProvider);
                symbolProvider.loadConfiguration(sub);
                sub.worked(1);
            }
        }
    }

    /**
     * Get the next or previous interval for a call stack entry ID, time and
     * direction
     *
     * @param entry
     *            whose key is the ID and value is the quark for the entry whose
     *            next / previous state we are searching for
     * @param time
     *            selection start time
     * @param forward
     *            if going to next or previous
     * @return the next / previous state encapsulated in a row if it exists,
     *         else null
     */
    private @Nullable List<ITimeGraphRowModel> getFollowEvent(Entry<Long, FlameChartEntryModel> entry, long time, boolean forward) {
        FlameChartEntryModel value = Objects.requireNonNull(entry.getValue());
        switch (value.getEntryType()) {
        case FUNCTION:
            CallStackDepth selectedDepth = fIdToCallstack.get(entry.getKey());
            if (selectedDepth == null) {
                return null;
            }
            // Ask the callstack the depth at the current time
            ITmfStateInterval nextDepth = selectedDepth.getCallStack().getNextDepth(time, forward);
            if (nextDepth == null) {
                return null;
            }
            Object depthVal = nextDepth.getValue();
            int depth = (depthVal instanceof Number) ? ((Number) depthVal).intValue() : 0;
            TimeGraphState state = new TimeGraphState(nextDepth.getStartTime(), nextDepth.getEndTime() - nextDepth.getStartTime(), depth);
            TimeGraphRowModel row = new TimeGraphRowModel(entry.getKey(), Collections.singletonList(state));
            return Collections.singletonList(row);
        case KERNEL:
        case LEVEL:
        case TRACE:
        default:
            return null;
        }
    }

    @Override
    public @NonNull Multimap<@NonNull String, @NonNull Object> getFilterData(long entryId, long time, @Nullable IProgressMonitor monitor) {
        Multimap<@NonNull String, @NonNull Object> data = ITimeGraphStateFilter.mergeMultimaps(ITimeGraphDataProvider.super.getFilterData(entryId, time, monitor),
                fEntryMetadata.getOrDefault(entryId, ImmutableMultimap.of()));
        FlameChartEntryModel entryModel = fEntries.get(entryId);
        if (entryModel == null) {
            return data;
        }
        Map<String, String> tooltip = getTooltip(entryId, entryModel, time, monitor);
        if (tooltip == null) {
            return data;
        }
        for (Entry<String, String> entry : tooltip.entrySet()) {
            data.put(entry.getKey(), entry.getValue());
        }
        return data;
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        // Use the palette with kernel styles, at worst, kernel styles won't be
        // used
        Map<String, OutputElementStyle> styles = FlameWithKernelPalette.getInstance().getStyles();
        return new TmfModelResponse<>(new OutputStyleModel(styles), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }
}
