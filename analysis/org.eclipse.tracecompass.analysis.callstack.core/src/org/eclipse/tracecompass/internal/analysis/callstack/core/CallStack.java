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
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackHostUtils.IHostIdProvider;
import org.eclipse.tracecompass.internal.analysis.callstack.core.CallStackSeries.IThreadIdProvider;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.IHostModel;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.ModelManager;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.ProcessStatusInterval;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * Represents the actual callstack for one element. The callstack is a stack of
 * calls, whether function calls, executions, sub-routines that have a certain
 * depth and where durations at each depth is in the form of a reverse pyramid,
 * ie, a call at level n+1 will have start_n+1 {@literal >=} start_n and end_n+1
 * {@literal <=} end_n.
 *
 * TODO: Is that true? the reverse pyramid?
 *
 * @author Geneviève Bastien
 */
public class CallStack {

    private static final String CALLSTACK_DEPTH = "CallStack depth "; //$NON-NLS-1$
    private static final String IS_TOO_LARGE = " is too large"; //$NON-NLS-1$

    private final @Nullable ICallStackElement fSymbolKeyElement;
    private final @Nullable IThreadIdProvider fThreadIdProvider;
    private final ITmfStateSystem fStateSystem;
    private final List<Integer> fQuarks;
    private final IHostIdProvider fHostProvider;

    /**
     * Constructor
     *
     * @param ss
     *            The state system containing the callstack
     * @param quarks
     *            The quarks corresponding to each of the depth levels
     * @param element
     *            The element this callstack belongs to
     * @param hostIdProvider
     *            The provider of the host ID for this callstack
     * @param threadIdProvider
     *            The provider of the thread ID for this callstack
     */
    public CallStack(ITmfStateSystem ss, List<Integer> quarks, ICallStackElement element, IHostIdProvider hostIdProvider, @Nullable IThreadIdProvider threadIdProvider) {
        fSymbolKeyElement = element;
        fThreadIdProvider = threadIdProvider;
        fStateSystem = ss;
        fQuarks = quarks;
        fHostProvider = hostIdProvider;
    }

    /**
     * Get the maximum depth of this callstack
     *
     * @return The maximum depth of the callstack
     */
    public int getMaxDepth() {
        return fQuarks.size();
    }

    /**
     * Get the list of calls at a given depth
     *
     * @param depth
     *            The requested depth
     * @param startTime
     *            The start of the period for which to get the call list
     * @param endTime
     *            The end of the period for which to get the call list
     * @param resolution
     *            The resolution of the calls. TODO: what is a resolution?
     * @param monitor
     *            The progress monitor to follow the progress of this query
     * @return The list of called functions at this depth
     */
    public List<ICalledFunction> getCallListAtDepth(int depth, long startTime, long endTime, long resolution, IProgressMonitor monitor) {
        if (depth > getMaxDepth()) {
            throw new ArrayIndexOutOfBoundsException(CALLSTACK_DEPTH + depth + IS_TOO_LARGE);
        }
        try {
            Integer quark = fQuarks.get(depth - 1);
            // Since functions in state system end at the time - 1, we need to
            // query that time too to include the function that ends at 'start'
            long start = Math.max(fStateSystem.getStartTime(), startTime - 1);
            long end = Math.min(fStateSystem.getCurrentEndTime(), endTime);
            if (start > end) {
                return Collections.emptyList();
            }
            List<ITmfStateInterval> stackIntervals = StateSystemUtils.queryHistoryRange(fStateSystem, quark, start, end, resolution, monitor);
            List<ICalledFunction> callList = new ArrayList<>(stackIntervals.size());

            for (ITmfStateInterval callInterval : stackIntervals) {
                if (monitor.isCanceled()) {
                    return Collections.emptyList();
                }
                if (!callInterval.getStateValue().isNull()) {
                    int threadId = getThreadId(callInterval.getStartTime());
                    callList.add(CalledFunctionFactory.create(callInterval.getStartTime(), callInterval.getEndTime() + 1, callInterval.getValue(), getSymbolKeyAt(callInterval.getStartTime()), threadId,
                            null, ModelManager.getModelFor(getHostId(callInterval.getStartTime()))));
                }
            }
            return callList;
        } catch (AttributeNotFoundException | StateSystemDisposedException | TimeRangeException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Get the quark for a given depth
     *
     * @param depth
     *            The requested depth
     * @return Get the quark for the function at a given depth
     */
    public Integer getQuarkAtDepth(int depth) {
        return fQuarks.get(depth - 1);
    }

    @SuppressWarnings("null")
    private String getHostId(long time) {
        return fHostProvider.apply(time);
    }

    /**
     * Get the function call at a given depth that either begins or ends after
     * the requested time.
     *
     * @param time
     *            The time to query
     * @param depth
     *            The depth of the requested function
     * @return The next function call at this level
     */
    public @Nullable ICalledFunction getNextFunction(long time, int depth) {
        if (depth > getMaxDepth()) {
            throw new ArrayIndexOutOfBoundsException(CALLSTACK_DEPTH + depth + IS_TOO_LARGE);
        }
        if (time > fStateSystem.getCurrentEndTime()) {
            return null;
        }
        try {
            ITmfStateInterval interval = fStateSystem.querySingleState(time, fQuarks.get(depth - 1));
            while ((interval.getStateValue().isNull() || (interval.getStartTime() < time)) && interval.getEndTime() + 1 < fStateSystem.getCurrentEndTime()) {
                interval = fStateSystem.querySingleState(interval.getEndTime() + 1, fQuarks.get(depth - 1));
            }
            if (!interval.getStateValue().isNull() && interval.getStartTime() >= time) {
                return CalledFunctionFactory.create(interval.getStartTime(), interval.getEndTime() + 1, interval.getValue(), getSymbolKeyAt(interval.getStartTime()), getThreadId(interval.getStartTime()), null,
                        ModelManager.getModelFor(getHostId(time)));
            }
        } catch (StateSystemDisposedException e) {
            // Nothing done
        }
        return null;
    }

    /**
     * Get the next function call after or including the requested time. To get
     * the function after a previous function was retrieved, the
     * {@link ICalledFunction#getEnd()} can be used for the requested time.
     *
     * @param time
     *            The time of the request
     * @param depth
     *            The depth FIXME: with the parent, do we need depth?
     * @param parent
     *            The parent function call
     * @param model
     *            The operating system model to retrieve extra information.
     *            FIXME: Since we have the host ID, the model may not be
     *            necessary
     * @param start
     *            The time of the start of the function. If the function starts
     *            earlier, this time will be used as start time.
     * @param end
     *            The time of the end of the function. If the function ends
     *            later, this time will be used as end time.
     * @return The next function call
     */
    public @Nullable ICalledFunction getNextFunction(long time, int depth, @Nullable ICalledFunction parent, IHostModel model, long start, long end) {
        if (depth > getMaxDepth()) {
            throw new ArrayIndexOutOfBoundsException(CALLSTACK_DEPTH + depth + IS_TOO_LARGE);
        }
        long endTime = (parent == null ? fStateSystem.getCurrentEndTime() : parent.getEnd());
        if (time > endTime || time >= end) {
            return null;
        }
        try {
            ITmfStateInterval interval = fStateSystem.querySingleState(time, fQuarks.get(depth - 1));
            while ((interval.getStateValue().isNull() || interval.getEndTime() < start) && interval.getEndTime() + 1 < endTime) {
                interval = fStateSystem.querySingleState(interval.getEndTime() + 1, fQuarks.get(depth - 1));
            }
            if (!interval.getStateValue().isNull() && interval.getStartTime() < end) {
                return CalledFunctionFactory.create(Math.max(start, interval.getStartTime()), Math.min(end, interval.getEndTime() + 1), interval.getValue(), getSymbolKeyAt(interval.getStartTime()), getThreadId(interval.getStartTime()), parent,
                        model);
            }
        } catch (StateSystemDisposedException e) {
            // Nothing done
        }
        return null;
    }

    /**
     * Get the next depth of this callstack, from the selected time. This
     * function is used to navigate the callstack forward or backward
     *
     * @param time
     *            The reference time from which to calculate the next depth
     * @param forward
     *            If <code>true</code>, the returned depth is the next depth
     *            after the requested time, otherwise, it will return the next
     *            depth before.
     * @return The interval whose value is a number referring to the next depth
     *         (or null if the stack is empty at this time), or
     *         <code>null</code> if there is no next depth to this callstack
     */
    public @Nullable ITmfStateInterval getNextDepth(long time, boolean forward) {
        Integer oneQuark = fQuarks.get(0);
        int parent = fStateSystem.getParentAttributeQuark(oneQuark);
        long queryTime = Long.max(fStateSystem.getStartTime(), Long.min(time, fStateSystem.getCurrentEndTime()));
        try {
            ITmfStateInterval currentDepth = fStateSystem.querySingleState(queryTime, parent);
            ITmfStateInterval interval = null;
            if (forward && currentDepth.getEndTime() + 1 <= fStateSystem.getCurrentEndTime()) {
                interval = fStateSystem.querySingleState(currentDepth.getEndTime() + 1, parent);
            } else if (!forward && currentDepth.getStartTime() - 1 >= fStateSystem.getStartTime()) {
                interval = fStateSystem.querySingleState(currentDepth.getStartTime() - 1, parent);
            }
            if (interval != null) {
                return interval;
            }
        } catch (StateSystemDisposedException e) {
            // Nothing done
        }
        return null;
    }

    /**
     * Get the depth of this callstack, at the time of query.
     *
     * @param time
     *            The reference time from which to calculate the next depth
     * @return The depth of the callstack at the time of query. If there is no
     *         value, it will return 0
     */
    public int getCurrentDepth(long time) {
        // Check the time
        if (time < fStateSystem.getStartTime() || time > fStateSystem.getCurrentEndTime()) {
            return 0;
        }
        Integer oneQuark = fQuarks.get(0);
        int parent = fStateSystem.getParentAttributeQuark(oneQuark);
        try {
            ITmfStateInterval currentDepth = fStateSystem.querySingleState(time, parent);
            Object value = currentDepth.getValue();
            if (!(value instanceof Integer)) {
                return 0;
            }
            return (Integer) value;
        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        return 0;
    }

    /**
     * Iterate over the callstack in a depth-first manner
     *
     * @param startTime
     *            The start time of the iteration
     * @param endTime
     *            The end time of the iteration
     * @param consumer
     *            The consumer to consume the function calls
     */
    public void iterateOverCallStack(long startTime, long endTime, Consumer<ICalledFunction> consumer) {
        // TODO Do we need this? If so, implement me!
    }

    /**
     * Get the symbol key for this callstack at a given time
     *
     * @param time
     *            The time of query
     * @return The symbol key or {@link CallStackElement#DEFAULT_SYMBOL_KEY} if
     *         not available
     */
    public int getSymbolKeyAt(long time) {
        if (fSymbolKeyElement != null) {
            return fSymbolKeyElement.getSymbolKeyAt(time);
        }
        return CallStackElement.DEFAULT_SYMBOL_KEY;
    }

    /**
     * Update the quarks list. Only the quarks at positions higher than the size
     * of the quarks will be copied in the list. The ones currently present
     * should not change.
     *
     * @param subAttributes
     *            The new complete list of attributes
     */
    public void updateAttributes(List<Integer> subAttributes) {
        fQuarks.addAll(fQuarks.size(), subAttributes.subList(fQuarks.size(), subAttributes.size()));
    }

    /**
     * Get the ID of the thread running this callstack at time t. This method is
     * used in conjunction with other trace data to get the time spent on the
     * CPU for this call.
     *
     * @param time
     *            The time of query
     * @return The thread ID or <code>-1</code> if not available.
     */
    public int getThreadId(long time) {
        if (fThreadIdProvider != null) {
            return fThreadIdProvider.getThreadId(time);
        }
        return -1;
    }

    /**
     * Get the ID of the thread running this callstack at time t. This method is
     * used in conjunction with other trace data to get the time spent on the
     * CPU for this call.
     *
     * @param time
     *            The time of query
     * @return The thread ID or <code>-1</code> if not available.
     */
    public @Nullable HostThread getHostThread(long time) {
        int tid = getThreadId(time);
        if (tid < 0) {
            return null;
        }
        return new HostThread(getHostId(time), tid);
    }

    /**
     * Get the unique {@link HostThread} for this callstack. This returns a
     * value only if the TID is not variable in time _and_ it is defined
     *
     * @return The {@link HostThread} that spans this callstack or
     *         <code>null</code> if TID is variable or it is not defined.
     */
    public @Nullable HostThread getHostThread() {
        if (!isTidVariable()) {
            return getHostThread(fStateSystem.getStartTime());
        }
        return null;
    }

    /**
     * Return whether the TID is variable through time for this callstack or if
     * it fixed
     *
     * @return <code>true</code> if the TID may vary during the trace or if it
     *         is fixed and can be cached.
     */
    public boolean isTidVariable() {
        if (fThreadIdProvider != null) {
            return fThreadIdProvider.variesInTime();
        }
        // No TID provider, so does not vary
        return false;
    }

    /**
     * Get the start time of this callstack
     *
     * @return The start time of the callstack
     */
    public long getStartTime() {
        return fStateSystem.getStartTime();
    }

    /**
     * Get the end time of this callstack
     *
     * @return The end time of the callstack
     */
    public long getEndTime() {
        return fStateSystem.getCurrentEndTime();
    }

    /**
     * Callstacks may save extra attributes in the state system. By convention,
     * these attributes will be located in the parent attribute of the
     * callstack, under their given name and its value will be queried by this
     * method at a given time.
     *
     * Extra attributes can be used for instance for some callstacks to save the
     * CPU on which they were running at the beginning of a function call. This
     * CPU can then be cross-referenced with other trace data to retrieve the
     * thread ID.
     *
     * @param name
     *            The name of the extra attribute to get
     * @param time
     *            The time at which to query
     * @return The value of attribute at the requested time
     */
    public @Nullable Object getExtraAttribute(String name, long time) {
        if (time < getStartTime() || time > getEndTime()) {
            return null;
        }

        int parentQuark = fStateSystem.getParentAttributeQuark(fQuarks.get(0));
        if (parentQuark < 0) {
            return null;
        }
        parentQuark = fStateSystem.getParentAttributeQuark(parentQuark);
        if (parentQuark < 0) {
            return null;
        }
        try {
            int quark = fStateSystem.optQuarkRelative(parentQuark, name);
            if (quark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                ITmfStateInterval state = fStateSystem.querySingleState(time, quark);
                switch (state.getStateValue().getType()) {
                case INTEGER:
                    return state.getStateValue().unboxInt();
                case LONG:
                    return state.getStateValue().unboxLong();
                case STRING:
                    return state.getStateValue().unboxStr();
                case CUSTOM:
                case DOUBLE:
                case NULL:
                default:
                    break;
                }
            }
        } catch (StateSystemDisposedException e) {
            // Nothing done
        }
        return null;
    }

    /**
     * Get whether this callstack has kernel statuses available
     *
     * @return <code>true</code> if kernel statuses are available for this
     *         callstack
     */
    public boolean hasKernelStatuses() {
        IHostModel model = ModelManager.getModelFor(getHostId(getEndTime()));
        return model.isThreadStatusAvailable();
    }

    /**
     * Get the kernel statuses that span a given function
     *
     * @param function
     *            The function for which to get the kernel statuses
     * @param times
     *            The times at which to query kernel statuses. An empty
     *            collection will return all intervals.
     * @return An iterator over the kernel status. The iterator can be empty is
     *         statuses are not available or if the function is outside the
     *         range of the available data.
     */
    public Iterable<ProcessStatusInterval> getKernelStatuses(ICalledFunction function, Collection<Long> times) {
        IHostModel model = ModelManager.getModelFor(getHostId(function.getStart()));
        int resolution = 1;
        // Filter the times
        if (!times.isEmpty()) {
            // Filter the times overlapping this function and calculate a
            // resolution
            List<Long> filtered = new ArrayList<>();
            for (Long time : times) {
                if (function.intersects(time)) {
                    filtered.add(time);
                }
            }
            Collections.sort(filtered);
            resolution = !filtered.isEmpty() ? (int) (filtered.get(filtered.size() - 1) - filtered.get(0) / filtered.size()) : resolution;
            return model.getThreadStatusIntervals(function.getThreadId(), function.getStart(), function.getEnd(), resolution);
        }
        return model.getThreadStatusIntervals(function.getThreadId(), function.getStart(), function.getEnd(), resolution);
    }

    /**
     * Transforms a state interval from the state system into a
     * {@link ICalledFunction}. The function allows to retrieve data from this
     * function, for instance, the thread ID, the symbol provider, etc.
     *
     * Note: It is the responsibility of the caller to make sure that the
     * interval does not have a null-value, otherwise, a NullPointerException
     * will be thrown.
     *
     * @param callInterval
     *            The state interval
     * @return An {@link ICalledFunction} object, with its fields resolved
     */
    public ICalledFunction getFunctionFromInterval(ITmfStateInterval callInterval) {
        int threadId = getThreadId(callInterval.getStartTime());
        return CalledFunctionFactory.create(callInterval.getStartTime(),
                callInterval.getEndTime() + 1,
                callInterval.getValue(),
                getSymbolKeyAt(callInterval.getStartTime()),
                threadId,
                null,
                ModelManager.getModelFor(getHostId(callInterval.getStartTime())));
    }

    @Override
    public int hashCode() {
        return Objects.hash(fSymbolKeyElement, fThreadIdProvider, fStateSystem, fQuarks, fHostProvider);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof CallStack)) {
            return false;
        }
        CallStack other = (CallStack) obj;
        return (Objects.equals(fSymbolKeyElement, other.fSymbolKeyElement) &&
                Objects.equals(fThreadIdProvider, other.fThreadIdProvider) &&
                Objects.equals(fStateSystem, other.fStateSystem) &&
                Objects.equals(fQuarks, other.fQuarks) &&
                Objects.equals(fHostProvider, other.fHostProvider));
    }

    @Override
    public String toString() {
        return "Callstack for quarks " + fQuarks; //$NON-NLS-1$
    }
}
