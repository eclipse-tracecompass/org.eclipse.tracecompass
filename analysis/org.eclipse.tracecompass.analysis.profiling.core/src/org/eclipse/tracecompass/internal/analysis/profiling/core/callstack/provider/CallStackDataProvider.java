/**********************************************************************
 * Copyright (c) 2017, 2020 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.core.callstack.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.FlameDefaultPalette;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackAnalysis;
import org.eclipse.tracecompass.internal.analysis.profiling.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.analysis.callsite.CallsiteAnalysis;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.TmfStrings;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfCallsite;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties.SymbolType;
import org.eclipse.tracecompass.tmf.core.model.annotations.Annotation;
import org.eclipse.tracecompass.tmf.core.model.annotations.AnnotationCategoriesModel;
import org.eclipse.tracecompass.tmf.core.model.annotations.AnnotationModel;
import org.eclipse.tracecompass.tmf.core.model.annotations.IAnnotation;
import org.eclipse.tracecompass.tmf.core.model.annotations.IOutputAnnotationProvider;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderUtils;
import org.eclipse.tracecompass.tmf.core.symbols.TmfResolvedSymbol;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * Call Stack Data Provider
 *
 * @author Loic Prieur-Drevon
 */
public class CallStackDataProvider extends AbstractTimeGraphDataProvider<@NonNull CallStackAnalysis, @NonNull CallStackEntryModel> implements IOutputStyleProvider, IOutputAnnotationProvider {

    private static final String ADDRESS_FORMAT = "0x%x"; //$NON-NLS-1$
    /**
     * Extension point ID.
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.internal.analysis.profiling.callstack.provider.CallStackDataProvider"; //$NON-NLS-1$
    private static final int UNKNOWN_TID = -1;

    private final Map<Integer, Integer> fQuarkToPid = new HashMap<>();

    private final @NonNull Collection<@NonNull ISymbolProvider> fProviders = new ArrayList<>();

    private static final class TimePidNameValue {

        private final long fTime;
        private final int fPid;
        private final Object fNameValue;

        public TimePidNameValue(int pid, Object nameValue, long time) {
            fTime = time;
            fPid = pid;
            fNameValue = nameValue;
        }

        /**
         * @return the time
         */
        public long getTime() {
            return fTime;
        }

        /**
         * @return the pid
         */
        public int getPid() {
            return fPid;
        }

        /**
         * @return the nameValue
         */
        public Object getNameValue() {
            return fNameValue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fNameValue, fPid, fTime);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TimePidNameValue other = (TimePidNameValue) obj;
            return Objects.equals(fNameValue, other.fNameValue) && fPid == other.fPid && fTime == other.fTime;
        }
    }

    private final LoadingCache<TimePidNameValue, @Nullable String> fTimeEventNames = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<TimePidNameValue, @Nullable String>() {
                @Override
                public @Nullable String load(TimePidNameValue value) {
                    Object nameValue = value.getNameValue();

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
                        synchronized (fProviders) {
                            name = SymbolProviderUtils.getSymbolText(fProviders, value.getPid(), value.getTime(), address);
                        }
                    }
                    return name;
                }
            });

    /**
     * Constructor
     *
     * @param trace
     *            underlying trace
     * @param module
     *            underlying {@link CallStackAnalysis} module
     */
    public CallStackDataProvider(@NonNull ITmfTrace trace, @NonNull CallStackAnalysis module) {
        super(trace, module);
    }

    @Override
    protected TmfTreeModel<@NonNull CallStackEntryModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters,
            @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {

        // make sure that function names are up-to-date
        resetFunctionNames(monitor);

        long start = ss.getStartTime();
        long end = ss.getCurrentEndTime();

        ImmutableList.Builder<CallStackEntryModel> builder = ImmutableList.builder();
        long traceId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        builder.add(new CallStackEntryModel(traceId, -1, Collections.singletonList(getTrace().getName()), start, end, CallStackEntryModel.TRACE, UNKNOWN_TID));

        List<Integer> processQuarks = ss.getQuarks(getAnalysisModule().getProcessesPattern());
        SubMonitor subMonitor = SubMonitor.convert(monitor, "CallStackDataProvider#fetchTree", processQuarks.size()); //$NON-NLS-1$
        List<@NonNull ITmfStateInterval> fullStart = ss.queryFullState(start);
        List<@NonNull ITmfStateInterval> fullEnd = ss.queryFullState(end);
        for (int processQuark : processQuarks) {

            /*
             * Default to trace entry, overwrite if a process entry exists.
             */
            long threadParentId = traceId;
            int pid = UNKNOWN_TID;
            if (processQuark != ITmfStateSystem.ROOT_ATTRIBUTE) {
                threadParentId = getId(processQuark);
                String processName = ss.getAttributeName(processQuark);
                Object processValue = fullEnd.get(processQuark).getValue();
                pid = getThreadProcessId(processName, processValue);
                builder.add(new CallStackEntryModel(threadParentId, traceId, Collections.singletonList(processName), start, end,
                        CallStackEntryModel.PROCESS, pid));
            }

            /* Create the threads under the process */
            List<Integer> threadQuarks = ss.getQuarks(processQuark, getAnalysisModule().getThreadsPattern());
            for (int threadQuark : threadQuarks) {
                int callStackQuark = ss.optQuarkRelative(threadQuark, CallStackAnalysis.CALL_STACK);
                if (callStackQuark == ITmfStateSystem.INVALID_ATTRIBUTE ||
                        callStackQuark >= fullStart.size() ||
                        threadQuark >= fullStart.size()) {
                    /*
                     * Ignore if no call stack attribute or if attributes were
                     * newly created after the full state queries.
                     */
                    continue;
                }
                String threadName = ss.getAttributeName(threadQuark);
                /*
                 * Default to process/trace entry, overwrite if a thread entry
                 * exists.
                 */
                long callStackParent = threadParentId;
                if (threadQuark != processQuark) {
                    CallStackEntryModel thread = createThread(ss, start, end, threadQuark, threadParentId, callStackQuark,
                            fullStart, fullEnd);
                    callStackParent = thread.getId();
                    builder.add(thread);
                }
                List<Integer> callStackAttributes = ss.getSubAttributes(callStackQuark, false);
                createStackEntries(callStackAttributes, start, end, pid, threadName, callStackParent, builder);
            }
            subMonitor.worked(1);
        }

        return new TmfTreeModel<>(Collections.emptyList(), builder.build());
    }

    private CallStackEntryModel createThread(ITmfStateSystem ss, long start, long end, int threadQuark, long processId, int callStackQuark,
            List<ITmfStateInterval> fullStart, List<ITmfStateInterval> fullEnd) {
        String threadName = ss.getAttributeName(threadQuark);
        long threadEnd = end + 1;
        ITmfStateInterval endInterval = fullEnd.get(callStackQuark);
        if (endInterval.getValue() == null && endInterval.getStartTime() != ss.getStartTime()) {
            threadEnd = endInterval.getStartTime();
        }
        Object threadStateValue = fullEnd.get(threadQuark).getValue();
        int threadId = getThreadProcessId(threadName, threadStateValue);
        ITmfStateInterval startInterval = fullStart.get(callStackQuark);
        long threadStart = startInterval.getValue() == null ? Long.min(startInterval.getEndTime() + 1, end) : start;
        return new CallStackEntryModel(getId(threadQuark), processId, Collections.singletonList(threadName), threadStart, threadEnd, CallStackEntryModel.THREAD, threadId);
    }

    private void createStackEntries(List<Integer> callStackAttributes, long start, long end, int pid,
            String threadName, long callStackParent, ImmutableList.Builder<CallStackEntryModel> builder) {
        int level = 1;
        for (int stackLevelQuark : callStackAttributes) {
            long id = getId(stackLevelQuark);
            builder.add(new CallStackEntryModel(id, callStackParent, Collections.singletonList(threadName), start, end, level, pid));
            fQuarkToPid.put(stackLevelQuark, pid);
            level++;
        }
    }

    private static int getThreadProcessId(String name, @Nullable Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            return UNKNOWN_TID;
        }
    }

    @Override
    protected TimeGraphModel getRowModel(ITmfStateSystem ss, @NonNull Map<@NonNull String, @NonNull Object> parameters, @Nullable IProgressMonitor monitor)
            throws StateSystemDisposedException {
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(parameters);
        if (filter == null) {
            return null;
        }
        Map<@NonNull Long, @NonNull Integer> entries = getSelectedEntries(filter);
        if (entries.size() == 1 && filter.getTimesRequested().length == 2) {
            // this is a request for a follow event.
            Entry<@NonNull Long, @NonNull Integer> entry = entries.entrySet().iterator().next();
            if (filter.getStart() == Long.MIN_VALUE) {
                return new TimeGraphModel(getFollowEvent(ss, entry, filter.getEnd(), false));
            } else if (filter.getEnd() == Long.MAX_VALUE) {
                return new TimeGraphModel(getFollowEvent(ss, entry, filter.getStart(), true));
            }
        }

        SubMonitor subMonitor = SubMonitor.convert(monitor, "CallStackDataProvider#fetchRowModel", 2); //$NON-NLS-1$

        ArrayListMultimap<Integer, ITmfStateInterval> intervals = ArrayListMultimap.create();
        Collection<Long> times = getTimes(filter, ss.getStartTime(), ss.getCurrentEndTime());
        /* Do the actual query */
        for (ITmfStateInterval interval : ss.query2D(entries.values(), times)) {
            if (subMonitor.isCanceled()) {
                return null;
            }
            intervals.put(interval.getAttribute(), interval);
        }
        subMonitor.worked(1);

        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(parameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        List<@NonNull ITimeGraphRowModel> rows = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : entries.entrySet()) {
            if (subMonitor.isCanceled()) {
                return null;
            }
            Collection<ITmfStateInterval> states = intervals.get(entry.getValue());
            Long key = Objects.requireNonNull(entry.getKey());
            List<ITimeGraphState> eventList = new ArrayList<>(states.size());
            states.forEach(state -> {
                ITimeGraphState timeGraphState = createTimeGraphState(state);
                applyFilterAndAddState(eventList, timeGraphState, key, predicates, monitor);
            });
            eventList.sort(Comparator.comparingLong(ITimeGraphState::getStartTime));
            rows.add(new TimeGraphRowModel(entry.getKey(), eventList));
        }
        subMonitor.worked(1);
        return new TimeGraphModel(rows);
    }

    private ITimeGraphState createTimeGraphState(ITmfStateInterval interval) {
        long startTime = interval.getStartTime();
        long duration = interval.getEndTime() - startTime + 1;
        Object value = interval.getValue();
        Integer pid = fQuarkToPid.get(interval.getAttribute());
        if (value != null && pid != null) {
            String name = fTimeEventNames.getUnchecked(new TimePidNameValue(pid, interval.getValue(), interval.getStartTime()));
            Object key = name == null ? value : name;
            return new TimeGraphState(startTime, duration, name, FlameDefaultPalette.getStyleFor(key));
        }
        return new TimeGraphState(startTime, duration, Integer.MIN_VALUE);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(Map<String, Object> parameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public String getId() {
        return ID;
    }

    /**
     * Clear the symbol provider cache. Load the symbol provider for the current
     * trace.
     *
     * @param monitor
     *            progress monitor
     */
    public void resetFunctionNames(@Nullable IProgressMonitor monitor) {
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
     * @param ss
     *            this data provider's state system
     * @param entry
     *            whose key is the ID and value is the quark for the entry whose
     *            next / previous state we are searching for
     * @param time
     *            selection start time
     * @param forward
     *            if going to next or previous
     * @return the next / previous state encapsulated in a row if it exists,
     *         else null
     * @throws StateSystemDisposedException
     */
    private static List<ITimeGraphRowModel> getFollowEvent(ITmfStateSystem ss, Entry<Long, Integer> entry, long time, boolean forward) throws StateSystemDisposedException {
        int parentQuark = ss.getParentAttributeQuark(entry.getValue());
        ITmfStateInterval current = ss.querySingleState(Long.max(ss.getStartTime(), Long.min(time, ss.getCurrentEndTime())), parentQuark);
        ITmfStateInterval interval = null;
        if (forward && current.getEndTime() + 1 <= ss.getCurrentEndTime()) {
            interval = ss.querySingleState(current.getEndTime() + 1, parentQuark);
        } else if (!forward && current.getStartTime() - 1 >= ss.getStartTime()) {
            interval = ss.querySingleState(current.getStartTime() - 1, parentQuark);
        }
        if (interval != null && interval.getValue() instanceof Number) {
            Object object = interval.getValue();
            if (object instanceof Number) {
                int value = ((Number) object).intValue();
                TimeGraphState state = new TimeGraphState(interval.getStartTime(), interval.getEndTime() - interval.getStartTime(), value);
                TimeGraphRowModel row = new TimeGraphRowModel(entry.getKey(), Collections.singletonList(state));
                return Collections.singletonList(row);
            }
        }
        return null;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(Map<String, Object> parameters, @Nullable IProgressMonitor monitor) {
        CallStackAnalysis analysis = getAnalysisModule();
        Map<String, String> tooltips = new HashMap<>();
        List<@NonNull Long> selected = DataProviderParameterUtils.extractSelectedItems(parameters);
        List<@NonNull Long> times = DataProviderParameterUtils.extractTimeRequested(parameters);

        // This data provider doesn't have any annotations or arrows
        Object element = parameters.get(DataProviderParameterUtils.REQUESTED_ELEMENT_KEY);
        if (element instanceof IAnnotation || element instanceof ITimeGraphArrow) {
            return new TmfModelResponse<>(tooltips, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        if (selected != null && times != null) {
            Map<@NonNull Long, @NonNull Integer> md = getSelectedEntries(selected);
            ITmfTrace trace = getTrace();
            for (Long time : times) {
                for (Entry<@NonNull Long, @NonNull Integer> entry : md.entrySet()) {
                    Long result = analysis.resolveDeviceId(entry.getValue(), time);
                    if (result != null) {
                        String deviceId = String.valueOf(result);
                        String deviceType = analysis.resolveDeviceType(entry.getValue(), time);
                        tooltips.put(deviceType, deviceId);
                        Iterable<@NonNull CallsiteAnalysis> csas = TmfTraceUtils.getAnalysisModulesOfClass(trace, CallsiteAnalysis.class);
                        for (CallsiteAnalysis csa : csas) {
                            List<@NonNull ITmfCallsite> res = csa.getCallsites(String.valueOf(trace.getUUID()), deviceType, deviceId, time);
                            if (!res.isEmpty()) {
                                tooltips.put(TmfStrings.source(), String.valueOf(res.get(0)));
                            }
                        }
                        return new TmfModelResponse<>(tooltips, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
                    }

                    ITmfStateSystem stateSystem = analysis.getStateSystem();
                    if (stateSystem != null) {
                        try {
                            Collection<@NonNull ISymbolProvider> symbolProviders = SymbolProviderManager.getInstance().getSymbolProviders(trace);
                            ITmfStateInterval interval = stateSystem.querySingleState(Objects.requireNonNull(time), Objects.requireNonNull(entry.getValue()));
                            Object value = interval.getValue();
                            if (value instanceof Number) {
                                long longValue = ((Number) value).longValue();
                                for (ISymbolProvider provider : symbolProviders) {
                                    TmfResolvedSymbol symbol = provider.getSymbol(longValue);
                                    if (symbol != null) {
                                        tooltips.put(Messages.CallStackDataProvider_toolTipState, symbol.getSymbolName());
                                        tooltips.put(Messages.CallStackDataProvider_toolTipAddress, String.format(ADDRESS_FORMAT, symbol.getBaseAddress()));
                                        break;
                                    }
                                }
                                tooltips.computeIfAbsent(Messages.CallStackDataProvider_toolTipState, unused -> String.format(ADDRESS_FORMAT, longValue));
                            } else if (value != null) {
                                tooltips.put(Messages.CallStackDataProvider_toolTipState, interval.getValueString());
                            }
                        } catch (StateSystemDisposedException e) {
                            Activator.getInstance().logError("State System Disposed", e); //$NON-NLS-1$
                        }
                    }
                }
            }
        }
        return new TmfModelResponse<>(tooltips, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull OutputStyleModel> fetchStyle(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(FlameDefaultPalette.getStyles()), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<AnnotationCategoriesModel> fetchAnnotationCategories(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new AnnotationCategoriesModel(Collections.singletonList(CallStackAnalysis.ANNOTATIONS)), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<AnnotationModel> fetchAnnotations(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        CallStackAnalysis analysis = getAnalysisModule();
        ITmfStateSystem ss = analysis.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        Map<@NonNull Long, @NonNull Integer> selectedEntries = getSelectedEntries(filter);
        List<Annotation> annotations = new ArrayList<>();
        try {
            for (Entry<@NonNull Long, @NonNull Integer> entry : selectedEntries.entrySet()) {
                long entryId = entry.getKey();
                int quark = entry.getValue();
                if (!fQuarkToPid.containsKey(quark)) {
                    continue;
                }
                int threadQuark = ss.getParentAttributeQuark(ss.getParentAttributeQuark(quark));
                int markersQuark = ss.optQuarkRelative(threadQuark, CallStackAnalysis.ANNOTATIONS);
                if (markersQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                    List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
                    for (ITmfStateInterval markerInterval : ss.query2D(Collections.singleton(markersQuark), times)) {
                        if (!markerInterval.getStateValue().isNull()) {
                            long startTime = markerInterval.getStartTime();
                            String annotValue = markerInterval.getStateValue().toString();

                            OutputElementStyle style = new OutputElementStyle(null, ImmutableMap.of(
                                    StyleProperties.COLOR, "#7D3D31", //$NON-NLS-1$
                                    StyleProperties.HEIGHT, 0.33f,
                                    StyleProperties.SYMBOL_TYPE, SymbolType.DIAMOND));
                            annotations.add(new Annotation(startTime, 0, entryId, annotValue, style));
                        }
                    }
                }
            }
        } catch (StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, e.getMessage());
        }

        return new TmfModelResponse<>(new AnnotationModel(Collections.singletonMap(CallStackAnalysis.ANNOTATIONS, annotations)), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }
}