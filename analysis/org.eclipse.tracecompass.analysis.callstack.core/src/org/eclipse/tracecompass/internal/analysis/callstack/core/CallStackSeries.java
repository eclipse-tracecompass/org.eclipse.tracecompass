/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackHostUtils.IHostIdProvider;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackHostUtils.IHostIdResolver;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.segmentstore.core.BasicSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * A callstack series contain the information necessary to build all the
 * different callstacks from a same pattern.
 *
 * Example: Let's take a trace that registers function entry and exit for
 * threads and where events also provide information on some other stackable
 * application component:
 *
 * The structure of this callstack in the state system could be as follows:
 *
 * <pre>
 *  Per PID
 *    [pid]
 *        [tid]
 *            callstack
 *               1  {@literal ->} function name
 *               2  {@literal ->} function name
 *               3  {@literal ->} function name
 *  Per component
 *    [application component]
 *       [tid]
 *           callstack
 *               1 {@literal ->} some string
 *               2 {@literal ->} some string
 * </pre>
 *
 * There are 2 {@link CallStackSeries} in this example, one starting by "Per
 * PID" and another "Per component". For the first series, there could be 3
 * {@link ICallStackGroupDescriptor}: "Per PID/*", "*", "callstack".
 *
 * If the function names happen to be addresses in an executable and the PID is
 * the key to map those symbols to actual function names, then the first group
 * "Per PID/*" would be the symbol key group.
 *
 * Each group descriptor can get the corresponding {@link ICallStackElement}s,
 * ie, for the first group, it would be all the individual pids in the state
 * system, and for the second group, it would be the application components.
 * Each element that is not a leaf element (check with
 * {@link ICallStackElement#isLeaf()}) will have a next group descriptor that
 * can fetch the elements under it. The last group will resolve to leaf elements
 * and each leaf elements has one {@link CallStack} object.
 *
 * @author Geneviève Bastien
 */
public class CallStackSeries implements ISegmentStore<ISegment> {

    /**
     * Interface for classes that provide a thread ID at time t for a callstack.
     * The thread ID can be used to calculate extra statistics per thread, for
     * example, the CPU time of each call site.
     */
    public interface IThreadIdProvider {

        /**
         * Get the ID of callstack thread at a given time
         *
         * @param time
         *            The time of request
         * @return The ID of the thread, or {@link IHostModel#UNKNOWN_TID} if
         *         unavailable
         */
        int getThreadId(long time);

        /**
         * Return whether the value returned by this provider is variable
         * through time (ie, each function of a stack may have a different
         * thread ID), or is fixed (ie, all functions in a stack have the same
         * thread ID)
         *
         * @return If <code>true</code>, the thread ID will be identical for a
         *         stack all throughout its life, it can be therefore be used to
         *         provider other thread-related information on stack even when
         *         there are no function calls.
         */
        boolean variesInTime();
    }

    /**
     * This class uses the value of an attribute as a thread ID.
     */
    private static final class AttributeValueThreadProvider implements IThreadIdProvider {

        private final ITmfStateSystem fSs;
        private final int fQuark;
        private @Nullable ITmfStateInterval fInterval;
        private int fLastThreadId = IHostModel.UNKNOWN_TID;
        private boolean fVariesInTime = true;

        public AttributeValueThreadProvider(ITmfStateSystem ss, int quark) {
            fSs = ss;
            fQuark = quark;
            // Try to get the tid at the start and set the fVariesInTime value
            getThreadId(fSs.getStartTime());
        }

        @Override
        public int getThreadId(long time) {
            ITmfStateInterval interval = fInterval;
            int tid = fLastThreadId;
            // If interval is not null and either the tid does not vary in time
            // or the interval intersects the requested time
            if (interval != null && (!fVariesInTime || interval.intersects(time))) {
                return tid;
            }
            try {
                interval = fSs.querySingleState(time, fQuark);
                switch (interval.getStateValue().getType()) {
                case INTEGER:
                    tid = interval.getStateValue().unboxInt();
                    break;
                case LONG:
                    tid = (int) interval.getStateValue().unboxLong();
                    break;
                case STRING:
                    try {
                        tid = Integer.valueOf(interval.getStateValue().unboxStr());
                    } catch (NumberFormatException e) {
                        tid = IHostModel.UNKNOWN_TID;
                    }
                    break;
                case NULL:
                case DOUBLE:
                case CUSTOM:
                default:
                    break;
                }
                // If the interval spans the whole state system, the tid does
                // not vary in time
                if (fSs.waitUntilBuilt(0)) {
                    if (interval.intersects(fSs.getStartTime()) && interval.intersects(fSs.getCurrentEndTime() - 1)) {
                        fVariesInTime = false;
                    }
                }
            } catch (StateSystemDisposedException e) {
                interval = null;
                tid = IHostModel.UNKNOWN_TID;
            }
            fInterval = interval;
            fLastThreadId = tid;
            return tid;
        }

        @Override
        public boolean variesInTime() {
            return fVariesInTime;
        }
    }

    /**
     * This class uses the value of an attribute as a thread ID.
     */
    private static final class AttributeNameThreadProvider implements IThreadIdProvider {
        private final int fTid;

        public AttributeNameThreadProvider(ITmfStateSystem ss, int quark) {
            int tid = IHostModel.UNKNOWN_TID;
            try {
                String attributeName = ss.getAttributeName(quark);
                tid = Integer.valueOf(attributeName);
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                tid = IHostModel.UNKNOWN_TID;
            }
            fTid = tid;
        }

        @Override
        public int getThreadId(long time) {
            return fTid;
        }

        @Override
        public boolean variesInTime() {
            return false;
        }
    }

    /**
     * This class will retrieve the thread ID
     */
    private static final class CpuThreadProvider implements IThreadIdProvider {

        private final ITmfStateSystem fSs;
        private final int fCpuQuark;
        private final IHostIdProvider fHostProvider;

        public CpuThreadProvider(IHostIdProvider hostProvider, ITmfStateSystem ss, int quark, String[] path) {
            fSs = ss;
            fHostProvider = hostProvider;
            // Get the cpu quark
            List<@NonNull Integer> quarks = ss.getQuarks(quark, path);
            fCpuQuark = quarks.isEmpty() ? ITmfStateSystem.INVALID_ATTRIBUTE : quarks.get(0);
        }

        @Override
        public int getThreadId(long time) {
            if (fCpuQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                return IHostModel.UNKNOWN_TID;
            }
            // Get the CPU
            try {
                ITmfStateInterval querySingleState = fSs.querySingleState(time, fCpuQuark);

                if (querySingleState.getStateValue().isNull()) {
                    return IHostModel.UNKNOWN_TID;
                }
                int cpu = querySingleState.getStateValue().unboxInt();
                // The thread running is the one on the CPU at the beginning of
                // this interval
                long startTime = querySingleState.getStartTime();
                IHostModel model = ModelManager.getModelFor(fHostProvider.apply(startTime));
                return model.getThreadOnCpu(cpu, startTime);
            } catch (StateSystemDisposedException e) {
                // Nothing done
            }
            return IHostModel.UNKNOWN_TID;
        }

        @Override
        public boolean variesInTime() {
            return true;
        }
    }

    /**
     * Interface for describing how a callstack will get the thread ID
     */
    public interface IThreadIdResolver {

        /**
         * Get the actual thread ID provider from this resolver
         *
         * @param hostProvider
         *            The provider of the host ID for the callstack
         * @param element
         *            The leaf element of the callstack
         * @return The thread ID provider
         */
        @Nullable IThreadIdProvider resolve(IHostIdProvider hostProvider, ICallStackElement element);
    }

    /**
     * This class will resolve the thread ID provider by the value of a
     * attribute at a given depth
     */
    public static final class AttributeValueThreadResolver implements IThreadIdResolver {
        private int fLevel;

        /**
         * Constructor
         *
         * @param level
         *            The depth of the element whose value will be used to
         *            retrieve the thread ID
         */
        public AttributeValueThreadResolver(int level) {
            fLevel = level;
        }

        @Override
        public @Nullable IThreadIdProvider resolve(IHostIdProvider hostProvider, ICallStackElement element) {
            if (!(element instanceof InstrumentedCallStackElement)) {
                throw new IllegalArgumentException();
            }
            InstrumentedCallStackElement insElement = (InstrumentedCallStackElement) element;

            List<InstrumentedCallStackElement> elements = new ArrayList<>();
            InstrumentedCallStackElement el = insElement;
            while (el != null) {
                elements.add(el);
                el = el.getParentElement();
            }
            Collections.reverse(elements);
            if (elements.size() <= fLevel) {
                return null;
            }
            InstrumentedCallStackElement stackElement = elements.get(fLevel);
            return new AttributeValueThreadProvider(stackElement.getStateSystem(), stackElement.getQuark());
        }
    }

    /**
     * This class will resolve the thread ID provider by the value of a
     * attribute at a given depth
     */
    public static final class AttributeNameThreadResolver implements IThreadIdResolver {
        private int fLevel;

        /**
         * Constructor
         *
         * @param level
         *            The depth of the element whose value will be used to
         *            retrieve the thread ID
         */
        public AttributeNameThreadResolver(int level) {
            fLevel = level;
        }

        @Override
        public @Nullable IThreadIdProvider resolve(IHostIdProvider hostProvider, ICallStackElement element) {
            if (!(element instanceof InstrumentedCallStackElement)) {
                throw new IllegalArgumentException();
            }
            InstrumentedCallStackElement insElement = (InstrumentedCallStackElement) element;

            List<InstrumentedCallStackElement> elements = new ArrayList<>();
            InstrumentedCallStackElement el = insElement;
            while (el != null) {
                elements.add(el);
                el = el.getParentElement();
            }
            Collections.reverse(elements);
            if (elements.size() <= fLevel) {
                return null;
            }
            InstrumentedCallStackElement stackElement = elements.get(fLevel);
            return new AttributeNameThreadProvider(stackElement.getStateSystem(), stackElement.getQuark());
        }
    }

    /**
     * This class will resolve the thread ID from the CPU on which the callstack
     * was running at a given time
     */
    public static final class CpuResolver implements IThreadIdResolver {
        private String[] fPath;

        /**
         * Constructor
         *
         * @param path
         *            The path relative to the leaf element that will contain
         *            the CPU ID
         */
        public CpuResolver(String[] path) {
            fPath = path;
        }

        @Override
        public @Nullable IThreadIdProvider resolve(IHostIdProvider hostProvider, ICallStackElement element) {
            if (!(element instanceof InstrumentedCallStackElement)) {
                throw new IllegalArgumentException();
            }
            InstrumentedCallStackElement insElement = (InstrumentedCallStackElement) element;

            return new CpuThreadProvider(hostProvider, insElement.getStateSystem(), insElement.getQuark(), fPath);
        }
    }

    private final InstrumentedGroupDescriptor fRootGroup;
    private final String fName;
    private final @Nullable IThreadIdResolver fResolver;
    private final IHostIdResolver fHostResolver;
    private final ITmfStateSystem fStateSystem;
    private final Map<Integer, ICallStackElement> fRootElements = new HashMap<>();

    /**
     * Constructor
     *
     * @param ss
     *            The state system containing this call stack
     * @param patternPaths
     *            The patterns for the different levels of the callstack in the
     *            state system. Any further level path is relative to the
     *            previous one.
     * @param symbolKeyLevelIndex
     *            The index in the list of the list to be used as a key to the
     *            symbol provider. The data at this level must be an integer,
     *            for instance a process ID
     * @param name
     *            A name for this callstack
     * @param hostResolver
     *            The host ID resolver for this callstack
     * @param threadResolver
     *            The thread resolver
     */
    public CallStackSeries(ITmfStateSystem ss, List<String[]> patternPaths, int symbolKeyLevelIndex, String name, IHostIdResolver hostResolver, @Nullable IThreadIdResolver threadResolver) {
        // Build the groups from the state system and pattern paths
        if (patternPaths.isEmpty()) {
            throw new IllegalArgumentException("State system callstack: the list of paths should not be empty"); //$NON-NLS-1$
        }
        int startIndex = patternPaths.size() - 1;
        InstrumentedGroupDescriptor prevLevel = new InstrumentedGroupDescriptor(ss, patternPaths.get(startIndex), null, symbolKeyLevelIndex == startIndex);
        for (int i = startIndex - 1; i >= 0; i--) {
            InstrumentedGroupDescriptor level = new InstrumentedGroupDescriptor(ss, patternPaths.get(i), prevLevel, symbolKeyLevelIndex == i);
            prevLevel = level;
        }
        fStateSystem = ss;
        fRootGroup = prevLevel;
        fName = name;
        fResolver = threadResolver;
        fHostResolver = hostResolver;
    }

    /**
     * Get the root elements of this callstack series
     *
     * @return The root elements of the callstack series
     */
    public Collection<ICallStackElement> getRootElements() {
        return InstrumentedCallStackElement.getRootElements(fRootGroup, fHostResolver, fResolver, fRootElements);
    }

    /**
     * Get the root group of the callstack series
     *
     * @return The root group descriptor
     */
    public ICallStackGroupDescriptor getRootGroup() {
        return fRootGroup;
    }

    /**
     * Get the name of this callstack series
     *
     * @return The name of the callstack series
     */
    public String getName() {
        return fName;
    }

    /**
     * Query the requested callstacks and return the segments for the sampled
     * times. The returned segments will be simply {@link ISegment} when there
     * is no function at a given depth, or {@link ICalledFunction} when there is
     * an actual function.
     *
     * @param callstacks
     *            The callstack entries to query
     * @param times
     *            The complete list of times to query, they may not all be
     *            within this series's range
     * @return A map of callstack depths to a list of segments.
     */
    public Multimap<CallStackDepth, ISegment> queryCallStacks(Collection<CallStackDepth> callstacks, Collection<Long> times) {
        Map<Integer, CallStackDepth> quarks = Maps.uniqueIndex(callstacks, cs -> cs.getQuark());
        Multimap<CallStackDepth, ISegment> map = Objects.requireNonNull(ArrayListMultimap.create());
        Collection<Long> queryTimes = getTimes(fStateSystem, times);
        try {
            @SuppressWarnings("null")
            Iterable<ITmfStateInterval> query2d = fStateSystem.query2D(quarks.keySet(), queryTimes);
            for (ITmfStateInterval callInterval : query2d) {
                CallStackDepth callStackDepth = Objects.requireNonNull(quarks.get(callInterval.getAttribute()));
                if (callInterval.getValue() != null) {
                    map.put(callStackDepth, callStackDepth.getCallStack().getFunctionFromInterval(callInterval));
                } else {
                    map.put(callStackDepth, new BasicSegment(callInterval.getStartTime(), callInterval.getEndTime() + 1));
                }
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            e.printStackTrace();
        }
        return map;
    }

    private static Collection<Long> getTimes(ITmfStateSystem ss, Collection<Long> times) {
        // Filter and deduplicate the time stamps for the statesystem
        long start = ss.getStartTime();
        long end = ss.getCurrentEndTime();
        // use a HashSet to deduplicate time stamps
        Collection<Long> queryTimes = new HashSet<>();
        for (long t : times) {
            if (t >= start && t <= end) {
                queryTimes.add(t);
            }
        }
        return queryTimes;
    }

    // ---------------------------------------------------
    // Segment store methods
    // ---------------------------------------------------

    private static Collection<ICallStackElement> getLeafElements(ICallStackElement element) {
        if (element.isLeaf()) {
            return Collections.singleton(element);
        }
        List<ICallStackElement> list = new ArrayList<>();
        element.getChildrenElements().forEach(e -> list.addAll(getLeafElements(e)));
        return list;
    }

    @Override
    public int size() {
        return Iterators.size(iterator());
    }

    @Override
    public boolean isEmpty() {
        return !iterator().hasNext();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        // narrow down search when object is a segment
        if (o instanceof ICalledFunction) {
            ICalledFunction seg = (ICalledFunction) o;
            Iterable<@NonNull ISegment> iterable = getIntersectingElements(seg.getStart());
            return Iterables.contains(iterable, seg);
        }
        return false;
    }

    @SuppressWarnings("null")
    @Override
    public Iterator<ISegment> iterator() {
        ITmfStateSystem stateSystem = fRootGroup.getStateSystem();
        long start = stateSystem.getStartTime();
        long end = stateSystem.getCurrentEndTime();
        return getIntersectingElements(start, end).iterator();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("This segment store can potentially cause OutOfMemoryExceptions"); //$NON-NLS-1$
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("This segment store can potentially cause OutOfMemoryExceptions"); //$NON-NLS-1$
    }

    @Override
    public boolean add(ISegment e) {
        throw new UnsupportedOperationException("This segment store does not support adding new segments"); //$NON-NLS-1$
    }

    @Override
    public boolean containsAll(@Nullable Collection<?> c) {
        if (c == null) {
            return false;
        }
        /*
         * Check that all elements in the collection are indeed ISegments, and
         * find their min end and max start time
         */
        long minEnd = Long.MAX_VALUE;
        long maxStart = Long.MIN_VALUE;
        for (Object o : c) {
            if (o instanceof ICalledFunction) {
                ICalledFunction seg = (ICalledFunction) o;
                minEnd = Math.min(minEnd, seg.getEnd());
                maxStart = Math.max(maxStart, seg.getStart());
            } else {
                return false;
            }
        }
        if (minEnd > maxStart) {
            /*
             * all segments intersect a common range, we just need to intersect
             * a time stamp in that range
             */
            minEnd = maxStart;
        }

        /* Iterate through possible segments until we have found them all */
        Iterator<@NonNull ISegment> iterator = getIntersectingElements(minEnd, maxStart).iterator();
        int unFound = c.size();
        while (iterator.hasNext() && unFound > 0) {
            ISegment seg = iterator.next();
            for (Object o : c) {
                if (Objects.equals(o, seg)) {
                    unFound--;
                }
            }
        }
        return unFound == 0;
    }

    @Override
    public boolean addAll(@Nullable Collection<? extends ISegment> c) {
        throw new UnsupportedOperationException("This segment store does not support adding new segments"); //$NON-NLS-1$
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This segment store does not support clearing the data"); //$NON-NLS-1$
    }

    private Map<Integer, CallStack> getCallStackQuarks() {
        Map<Integer, CallStack> quarkToElement = new HashMap<>();
        // Get the leaf elements and their callstacks
        getRootElements().stream().flatMap(e -> getLeafElements(e).stream())
                .filter(e -> e instanceof InstrumentedCallStackElement)
                .map(e -> (InstrumentedCallStackElement) e)
                .forEach(e -> e.getStackQuarks().forEach(c -> quarkToElement.put(c, e.getCallStack())));
        return quarkToElement;
    }

    @SuppressWarnings("null")
    @Override
    public Iterable<ISegment> getIntersectingElements(long start, long end) {
        ITmfStateSystem stateSystem = fRootGroup.getStateSystem();
        // Start can be Long.MIN_VALUE, we need to avoid underflow
        long startTime = Math.max(Math.max(1, start) - 1, stateSystem.getStartTime());
        long endTime = Math.min(end, stateSystem.getCurrentEndTime());
        if (startTime > endTime) {
            return Collections.emptyList();
        }
        Map<Integer, CallStack> quarksToElement = getCallStackQuarks();
        try {
            Iterable<ITmfStateInterval> query2d = stateSystem.query2D(quarksToElement.keySet(), startTime, endTime);
            query2d = Iterables.filter(query2d, interval -> !interval.getStateValue().isNull());
            Function<ITmfStateInterval, ICalledFunction> fct = interval -> {
                CallStack callstack = quarksToElement.get(interval.getAttribute());
                if (callstack == null) {
                    throw new IllegalArgumentException("The quark was in that map in the first place, there must be a callstack to go with it!"); //$NON-NLS-1$
                }
                HostThread hostThread = callstack.getHostThread(interval.getStartTime());

                int pid = callstack.getSymbolKeyAt(interval.getStartTime());
                if (pid == CallStackElement.DEFAULT_SYMBOL_KEY && hostThread != null) {
                    // Try to find the pid from the tid
                    pid = ModelManager.getModelFor(hostThread.getHost()).getProcessId(hostThread.getTid(), interval.getStartTime());
                }
                if (hostThread == null) {
                    hostThread = new HostThread(StringUtils.EMPTY, IHostModel.UNKNOWN_TID);
                }

                return CalledFunctionFactory.create(interval.getStartTime(), interval.getEndTime() + 1, interval.getValue(), pid, hostThread.getTid(),
                        null, ModelManager.getModelFor(hostThread.getHost()));
            };
            return Iterables.transform(query2d, fct);
        } catch (StateSystemDisposedException e) {
            Activator.getInstance().logError("Error getting intersecting elements: StateSystemDisposed"); //$NON-NLS-1$
        }
        return Collections.emptyList();
    }

    @Override
    public void dispose() {
        // Nothing to do
    }
}
