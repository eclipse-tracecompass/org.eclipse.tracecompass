/*******************************************************************************
 * Copyright (c) 2014, 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.kernel;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils.QuarkIterator;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.interval.TmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue.Type;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;

/**
 * Information provider utility class that retrieves thread-related information
 * from a Linux Kernel Analysis
 *
 * @author Geneviève Bastien
 * @since 2.0
 */
public final class KernelThreadInformationProvider {

    private KernelThreadInformationProvider() {
    }

    /**
     * Get the ID of the thread running on the CPU at time ts
     *
     * TODO: This method may later be replaced by an aspect, when the aspect can
     * resolve to something that is not an event
     *
     * @param module
     *            The kernel analysis instance to run this method on
     * @param cpuId
     *            The CPU number the process is running on
     * @param ts
     *            The timestamp at which we want the running process
     * @return The TID of the thread running on CPU cpuId at time ts or
     *         {@code null} if either no thread is running or we do not know.
     */
    public static @Nullable Integer getThreadOnCpu(KernelAnalysisModule module, long cpuId, long ts) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return null;
        }
        try {
            int cpuQuark = ss.getQuarkAbsolute(Attributes.CPUS, Long.toString(cpuId), Attributes.CURRENT_THREAD);
            ITmfStateInterval interval = ss.querySingleState(ts, cpuQuark);
            ITmfStateValue val = interval.getStateValue();
            if (val.getType().equals(Type.INTEGER)) {
                return val.unboxInt();
            }
        } catch (AttributeNotFoundException | StateSystemDisposedException | TimeRangeException e) {
        }
        return null;
    }

    /**
     * The the threads that have been scheduled on the given CPU(s), for the
     * given time range. Threads with TID 0 (swapper threads) will never be
     * included.
     *
     * @param module
     *            The kernel analysis module to query
     * @param cpus
     *            The list of cpus
     * @param rangeStart
     *            The start of the time range
     * @param rangeEnd
     *            The end of the time range
     * @return A set of all the thread IDs that are run on said CPUs on the time
     *         range. Empty set if there is no thread on the CPUs in this time
     *         range. Null if the information is not available.
     * @since 2.5
     */
    public static @Nullable Set<Integer> getThreadsOfCpus(KernelAnalysisModule module, Collection<Long> cpus, long rangeStart, long rangeEnd) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return null;
        }

        Set<Long> uniqueCpus = ImmutableSet.copyOf(cpus);

        int threadsQuark = ss.optQuarkAbsolute(Attributes.THREADS);
        if (threadsQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return null;
        }

        List<Integer> threadQuarks = ss.getSubAttributes(threadsQuark, false);
        return threadQuarks.stream()
                /*
                 * Keep only the quarks of threads that are on at least one of
                 * the wanted CPUs' run queue.
                 */
                .filter(threadQuark -> {
                    int threadCurrentCpuQuark = ss.optQuarkRelative(threadQuark, Attributes.CURRENT_CPU_RQ);
                    if (threadCurrentCpuQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                        return false;
                    }

                    /*
                     * Check if the thread was seen on any of the requested
                     * CPUs.
                     */
                    QuarkIterator it = new QuarkIterator(ss, threadCurrentCpuQuark, rangeStart, rangeEnd);
                    while (it.hasNext()) {
                        Object o = it.next().getValue();
                        if (o instanceof Number && uniqueCpus.contains(((Number) o).longValue())) {
                            return true;
                        }
                    }
                    return false;
                })

                /* Convert the thread quarks to their corresponding TIDs */
                .map(ss::getAttributeName)
                /* Ignore swapper threads */
                .filter(attribName -> !attribName.startsWith(Attributes.THREAD_0_PREFIX))
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * Predicate indicating if a thread state value is considered active or not.
     */
    private static final Predicate<ITmfStateValue> IS_STATE_VALUE_ACTIVE = stateValue -> {
        return !(stateValue.isNull() ||
                stateValue.equals(ProcessStatus.UNKNOWN.getStateValue()) ||
                stateValue.equals(ProcessStatus.WAIT_BLOCKED.getStateValue()) ||
                stateValue.equals(ProcessStatus.WAIT_UNKNOWN.getStateValue()));
    };

    /**
     * Return all the threads that are considered active in the given time
     * range. Threads with TID 0 (swapper threads) will never be included.
     *
     * @param module
     *            The kernel analysis module to query
     * @param rangeStart
     *            The start of the time range
     * @param rangeEnd
     *            The end of the time range
     * @return A set of all the thread IDs that are considered active in the
     *         time range. Empty set if there are none or if unavailable for the
     *         time range.
     * @since 2.5
     */
    public static Set<Integer> getActiveThreadsForRange(KernelAnalysisModule module, long rangeStart, long rangeEnd) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return Collections.emptySet();
        }

        // Check the time range to avoid throwing an exception
        long start = Math.max(rangeStart, ss.getStartTime());
        long end = Math.min(rangeEnd, ss.getCurrentEndTime());
        if (start > end) {
            return Collections.emptySet();
        }

        int threadsQuark = ss.optQuarkAbsolute(Attributes.THREADS);
        if (threadsQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return Collections.emptySet();
        }

        List<ITmfStateInterval> fullQueryAtStart;
        try {
            fullQueryAtStart = ss.queryFullState(start);
        } catch (StateSystemDisposedException e) {
            return Collections.emptySet();
        }

        List<Integer> threadQuarks = ss.getSubAttributes(threadsQuark, false);
        return threadQuarks.stream()
                /*
                 * Keep only the quarks of threads that are considered active at
                 * some point in the time range.
                 */
                .filter(threadQuark -> {
                    /*
                     * If the thread was active at range start, we can already
                     * consider it active.
                     */
                    ITmfStateInterval intervalAtStart = fullQueryAtStart.get(threadQuark);
                    if (IS_STATE_VALUE_ACTIVE.test(intervalAtStart.getStateValue())) {
                        return true;
                    }

                    /*
                     * If it was inactive, and it remains in the exact same
                     * state for the whole time range, we can conclude it is
                     * inactive for the whole range.
                     *
                     * Note this will not catch cases where the threads goes
                     * from one inactive state to another, this will be found
                     * with the range query below.
                     */
                    if (intervalAtStart.getEndTime() >= end) {
                        return false;
                    }

                    QuarkIterator it = new QuarkIterator(ss, threadQuark, start, end);
                    while (it.hasNext()) {
                        ITmfStateInterval interval = it.next();
                        if (IS_STATE_VALUE_ACTIVE.test(interval.getStateValue())) {
                            return true;
                        }
                    }
                    /*
                     * We haven't found an active state value in the whole
                     * range.
                     */
                    return false;
                })

                /* Convert the thread quarks to their corresponding TIDs */
                .map(ss::getAttributeName)
                /* Ignore swapper threads */
                .filter(attribName -> !attribName.startsWith(Attributes.THREAD_0_PREFIX))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    /**
     * Get the TIDs of the threads from an analysis
     *
     * @param module
     *            The kernel analysis instance to run this method on
     * @return The set of TIDs corresponding to the threads
     */
    public static Collection<Integer> getThreadIds(KernelAnalysisModule module) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return Collections.emptySet();
        }
        int threadQuark;
        try {
            threadQuark = ss.getQuarkAbsolute(Attributes.THREADS);
            Set<Integer> tids = new TreeSet<>();
            for (Integer quark : ss.getSubAttributes(threadQuark, false)) {
                final @NonNull String attributeName = ss.getAttributeName(quark);
                tids.add(attributeName.startsWith(Attributes.THREAD_0_PREFIX) ? 0 : Integer.parseInt(attributeName));
            }
            return tids;
        } catch (AttributeNotFoundException e) {
            // Do Nothing
        }
        return Collections.emptySet();
    }

    /**
     * Get the parent process ID of a thread
     *
     * @param module
     *            The kernel analysis instance to run this method on
     * @param threadId
     *            The thread ID of the process for which to get the parent
     * @param ts
     *            The timestamp at which to get the parent
     * @return The parent PID or {@code null} if the PPID is not found.
     */
    public static @Nullable Integer getParentPid(KernelAnalysisModule module, Integer threadId, long ts) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return null;
        }
        Integer ppidNode;
        try {
            ppidNode = ss.getQuarkAbsolute(Attributes.THREADS, threadId.toString(), Attributes.PPID);
            ITmfStateInterval ppidInterval = ss.querySingleState(ts, ppidNode);
            ITmfStateValue ppidValue = ppidInterval.getStateValue();

            if (ppidValue.getType().equals(Type.INTEGER)) {
                return Integer.valueOf(ppidValue.unboxInt());
            }
        } catch (AttributeNotFoundException | StateSystemDisposedException | TimeRangeException e) {
        }
        return null;
    }

    /**
     * Get the process ID of a thread
     *
     * @param module
     *            The kernel analysis instance to run this method on
     * @param threadId
     *            The ID of the thread for which to get the process ID
     * @param ts
     *            The timestamp at which to get the parent
     * @return The process ID or {@code null} if the pid is not found.
     * @since 2.5
     */
    public static @Nullable Integer getProcessId(KernelAnalysisModule module, Integer threadId, long ts) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return null;
        }
        try {
            int pidNode = ss.optQuarkAbsolute(Attributes.THREADS, threadId.toString());
            if (pidNode == ITmfStateSystem.INVALID_ATTRIBUTE) {
                /* The thread is invalid, return null */
                return null;
            }
            pidNode = ss.optQuarkRelative(pidNode, Attributes.PID);
            if (pidNode == ITmfStateSystem.INVALID_ATTRIBUTE) {
                /* The attribute is not there, thread is the process */
                return threadId;
            }
            ITmfStateInterval pidInterval = ss.querySingleState(ts, pidNode);
            Object pid = pidInterval.getValue();

            if (pid instanceof Integer) {
                return (Integer) pid;
            }
        } catch (StateSystemDisposedException | TimeRangeException e) {
        }
        return null;
    }

    /**
     * Get the executable name of the thread ID. If the thread ID was used
     * multiple time or the name changed in between, it will return the last
     * name the thread has taken, or {@code null} if no name is found
     *
     * @param module
     *            The kernel analysis instance to run this method on
     * @param threadId
     *            The thread ID of the process for which to get the name
     * @return The last executable name of this process, or {@code null} if not
     *         found
     *
     *         TODO: use
     *         {@link #getExecutableName(KernelAnalysisModule, Integer, long)}
     *         to handle TID reuse
     */
    public static @Nullable String getExecutableName(KernelAnalysisModule module, Integer threadId) {
        ITmfStateSystem stateSystem = module.getStateSystem();
        if (stateSystem == null) {
            return null;
        }
        return getExecutableName(module, threadId, stateSystem.getCurrentEndTime());
    }

    /**
     * Get the executable name of the thread ID at a specified time. If the
     * thread ID was used multiple time or the name changed in between, it will
     * return the last name the thread has taken before the given timestamp, or
     * {@code null} if no name is found before the time.
     *
     * In other words, if:
     * <ul>
     * <li>trace start < <em>ts</em> < start of the first process: the TID was
     * never seen to be used in this trace, and
     * {@link #getExecutableName(KernelAnalysisModule, Integer, long)} returns
     * null</li>
     * <li><em>ts</em> > trace end: the name of the last thread to run is
     * returned. (could be null if the tid was never used)</li>
     * <li>thread start > <em>ts</em> > thread end: the thread name will be the
     * thread name of the process at that time. This may be a parent as the
     * thread inherrits the parent's name until it is changed.</li>
     * <li>thread end > <em>ts</em> > next thread start: the last valid
     * executable name is returned</li>
     * </ul>
     *
     * @param module
     *            The kernel analysis instance to run this method on
     * @param threadId
     *            The thread ID of the process for which to get the name
     * @param ts
     *            timestamp to query at. It will look up the thread name of the
     *            current or closest previous lifetime
     * @return The last executable name of this process for a given time, or
     *         {@code null} if not found.
     * @since 4.1
     */
    public static @Nullable String getExecutableName(KernelAnalysisModule module, Integer threadId, long ts) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return null;
        }
        int execNameNode = ss.optQuarkAbsolute(Attributes.THREADS, threadId.toString(), Attributes.EXEC_NAME);
        if (execNameNode == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return null;
        }
        QuarkIterator reversedIterator = new QuarkIterator(ss, execNameNode, ts);
        while (reversedIterator.hasPrevious()) {
            Object value = reversedIterator.previous().getValue();
            if (value instanceof String) {
                return (String) value;
            }
        }
        return null;
    }

    /**
     * Get the priority of this thread at time ts
     *
     * @param module
     *            The kernel analysis instance to run this method on
     * @param threadId
     *            The ID of the thread to query
     * @param ts
     *            The timestamp at which to query
     * @return The priority of the thread or <code>-1</code> if not available
     */
    public static int getThreadPriority(KernelAnalysisModule module, int threadId, long ts) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return -1;
        }
        int prioQuark = ss.optQuarkAbsolute(Attributes.THREADS, String.valueOf(threadId), Attributes.PRIO);
        if (prioQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return -1;
        }
        try {
            return ss.querySingleState(ts, prioQuark).getStateValue().unboxInt();
        } catch (StateSystemDisposedException e) {
            return -1;
        }
    }

    /**
     * Get the status intervals for a given thread with a resolution
     *
     * @param module
     *            The kernel analysis instance to run this method on
     * @param threadId
     *            The ID of the thread to get the intervals for
     * @param start
     *            The start time of the requested range
     * @param end
     *            The end time of the requested range
     * @param resolution
     *            The resolution or the minimal time between the requested
     *            intervals. If interval times are smaller than resolution, only
     *            the first interval is returned, the others are ignored.
     * @param monitor
     *            A progress monitor for this task
     * @return The list of status intervals for this thread, an empty list is
     *         returned if either the state system is {@code null} or the quark
     *         is not found
     */
    public static List<ITmfStateInterval> getStatusIntervalsForThread(KernelAnalysisModule module, Integer threadId, long start, long end, long resolution, IProgressMonitor monitor) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }

        try {
            int threadQuark = ss.getQuarkAbsolute(Attributes.THREADS, threadId.toString());
            List<ITmfStateInterval> statusIntervals = StateSystemUtils.queryHistoryRange(ss, threadQuark, Math.max(start, ss.getStartTime()), Math.min(end - 1, ss.getCurrentEndTime()), resolution, monitor);
            return statusIntervals;
        } catch (AttributeNotFoundException | StateSystemDisposedException | TimeRangeException e) {
            // Do Nothing
        }
        return Collections.emptyList();
    }

    /**
     * Get an iterator for the status intervals of a given thread in a time
     * range
     *
     * @param module
     *            The kernel analysis instance to run this method on
     * @param threadId
     *            The ID of the thread to get the intervals for
     * @param start
     *            The start time of the requested range
     * @param end
     *            The end time of the requested range
     * @param resolution
     *            The resolution, ie the number of nanoseconds between kernel
     *            status queries. A value lower or equal to 1 will return all
     *            intervals
     * @return The list of status intervals for this thread, an empty list is
     *         returned if either the state system is {@code null} or the quark
     *         is not found
     * @since 3.0
     */
    public static Iterator<ITmfStateInterval> getStatusIntervalsForThread(KernelAnalysisModule module, Integer threadId, long start, long end, long resolution) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return NonNullUtils.checkNotNull(Collections.emptyListIterator());
        }

        int threadQuark = ss.optQuarkAbsolute(Attributes.THREADS, threadId.toString());
        if (threadQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return NonNullUtils.checkNotNull(Collections.emptyListIterator());
        }
        return new StateSystemUtils.QuarkIterator(ss, threadQuark, start, end - 1, resolution);
    }

    /**
     * Get a map of each tid with their associated thread status intervals
     * containing syscalls if present
     *
     * @param module
     *            The kernel analysis instance to run this method on
     * @param threadIds
     *            The ID of the threads to get the intervals for
     * @param times
     *            The times to execute the query on
     * @param monitor
     *            The monitor to cancel the query
     * @return A Map associating the intervals to each tid
     * @since 8.2
     */
    public static Map<Integer, List<ITmfStateInterval>> getStatusIntervalsForThreads(KernelAnalysisModule module, Collection<Integer> threadIds, Collection<Long> times, IProgressMonitor monitor) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return Objects.requireNonNull(Collections.emptyMap());
        }
        List<Long> currentTimes = times.stream().filter(time -> (time > ss.getCurrentEndTime() || time < ss.getStartTime())).sorted().collect(Collectors.toList());
        if (currentTimes.isEmpty()) {
            return Objects.requireNonNull(Collections.emptyMap());
        }
        Map<Integer, String> quarkToThreadIds = getQuarkToThreadIds(module, threadIds);
        TreeMultimap<String, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        Map<Integer, List<ITmfStateInterval>> kernelStatuses = new HashMap<>();
        try {
            for (ITmfStateInterval interval : ss.query2D(quarkToThreadIds.keySet(), currentTimes)) {
                if (monitor.isCanceled()) {
                    return Objects.requireNonNull(Collections.emptyMap());
                }
                String threadId = quarkToThreadIds.get(interval.getAttribute());
                if (threadId != null) {
                    intervals.put(threadId, interval);
                }
            }
            for (Integer threadId : threadIds) {
                NavigableSet<ITmfStateInterval> states = intervals.get(threadId.toString());
                if (monitor.isCanceled()) {
                    return Collections.emptyMap();
                }
                NavigableSet<ITmfStateInterval> syscalls = intervals.get(threadId.toString() + Attributes.SYSTEM_CALL);
                if (syscalls != null) {
                    kernelStatuses.put(threadId, states.stream().map(i -> addSyscallInformation(i, syscalls)).collect(Collectors.toList()));
                    continue;
                }
                kernelStatuses.put(threadId, Objects.requireNonNull(states.stream().collect(Collectors.toList())));
            }
            return kernelStatuses;
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            // Do nothing
        }
        return Objects.requireNonNull(Collections.emptyMap());
    }

    private static ITmfStateInterval addSyscallInformation(ITmfStateInterval interval, NavigableSet<ITmfStateInterval> syscalls) {
        Object status = interval.getValue();
        if (status instanceof Integer && ((int) status) == StateValues.PROCESS_STATUS_RUN_SYSCALL) {
            // intervals are sorted by start time
            ITmfStateInterval syscall = syscalls.floor(interval);

            if (syscall != null) {
                Object value = syscall.getValue();
                if (value instanceof String) {
                    return new TmfStateInterval(interval.getStartTime(), interval.getEndTime(), interval.getAttribute(), String.valueOf(value));
                }
            }
        }
        return interval;
    }

    /**
     * @param module
     *            The analysis module to query the state system
     * @param threadIds
     *            the threadIds to select the quarks
     * @return A map associating the quarks to the thread ids or their syscall
     *         quarks
     */
    private static Map<Integer, String> getQuarkToThreadIds(KernelAnalysisModule module, Collection<Integer> threadIds) {
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return Objects.requireNonNull(Collections.emptyMap());
        }
        Map<Integer, String> quarkToThreadIds = new HashMap<>();
        for (Integer threadId : threadIds) {
            int threadQuark = ss.optQuarkAbsolute(Attributes.THREADS, threadId.toString());
            quarkToThreadIds.put(threadQuark, threadId.toString());
            int syscallQuark = ss.optQuarkRelative(threadQuark, Attributes.SYSTEM_CALL);
            if (syscallQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                quarkToThreadIds.put(syscallQuark, threadId.toString() + Attributes.SYSTEM_CALL);
            }
        }
        return quarkToThreadIds;
    }
}
