/*******************************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.core.swslatency;

import org.eclipse.tracecompass.analysis.os.linux.core.model.OsStrings;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.datastore.core.serialization.SafeByteBufferFactory;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.IPrioritySegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.segment.interfaces.INamedSegment;
import org.eclipse.tracecompass.tmf.core.model.ICoreElementResolver;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A Linux kernel sched_wakeup/ sched_switch event, represented as an
 * {@link ISegment}.
 *
 * @author Abdellah Rahmani
 * @since 8.1
 */
public final class SchedWS implements INamedSegment, IPrioritySegment, ICoreElementResolver {

    private static final long serialVersionUID = 4183872871733170072L;

    /**
     * The reader for this segment class
     */
    public static final IHTIntervalReader<ISegment> READER = buffer -> new SchedWS(buffer.getLong(), buffer.getLong(), buffer.getString(), buffer.getInt(), buffer.getInt());

    /**
     * The subset of information that is available from the sched wakeup/switch
     * entry event.
     */
    public static class InitialInfo {

        private long fStartTime;
        private String fName;
        private int fTid;

        /**
         * @param startTime
         *            Start time of the sched_wakeup event
         * @param name
         *            Name of the thread to wake-up
         * @param tid
         *            The TID of the thread to wake-up
         */
        public InitialInfo(
                long startTime,
                String name,
                int tid) {
            fStartTime = startTime;
            fName = name.intern();
            fTid = tid;
        }
    }

    private final long fStartTime;
    private final long fEndTime;
    private final String fName;
    private final int fTid;
    private final int fPriority;

    /**
     * @param info
     *            Initial information of sched_wakeup / sched_switch event
     * @param endTime
     *            End time of the sched_switch event
     * @param priority
     *            The priority of the thread to wake-up
     */
    public SchedWS(
            InitialInfo info,
            long endTime, int priority) {
        fStartTime = info.fStartTime;
        fName = info.fName;
        fEndTime = endTime;
        fTid = info.fTid;
        fPriority = priority;
    }

    private SchedWS(long startTime, long endTime, String name, int tid, int priority) {
        fStartTime = startTime;
        fEndTime = endTime;
        fName = name;
        fTid = tid;
        fPriority = priority;
    }

    @Override
    public long getStart() {
        return fStartTime;
    }

    @Override
    public long getEnd() {
        return fEndTime;
    }

    /**
     * Get the name of the sched_wakeup / switch_event event
     *
     * @return Name
     */
    @Override
    public String getName() {
        return fName;
    }

    /**
     * Get the thread ID for this sched_wakeup / sched_switch event
     *
     * @return The ID of the thread
     */
    public int getTid() {
        return fTid;
    }

    /**
     * Get the priority of the thread to wake-up
     *
     * @return The priority value of the thread
     */
    @Override
    public int getPriority() {
        return fPriority;
    }

    @Override
    public int getSizeOnDisk() {
        return 2 * Long.BYTES + SafeByteBufferFactory.getStringSizeInBuffer(fName) + 2 * Integer.BYTES;
    }

    @Override
    public void writeSegment(ISafeByteBufferWriter buffer) {
        buffer.putLong(fStartTime);
        buffer.putLong(fEndTime);
        buffer.putString(fName);
        buffer.putInt(fTid);
        buffer.putInt(fPriority);
    }

    @Override
    public int compareTo(ISegment o) {
        int ret = INamedSegment.super.compareTo(o);
        if (ret != 0) {
            return ret;
        }
        return toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return "Start Time = " + getStart() + //$NON-NLS-1$
                "; End Time = " + getEnd() + //$NON-NLS-1$
                "; Duration = " + getLength() + //$NON-NLS-1$
                "; Name = " + getName(); //$NON-NLS-1$
    }

    @Override
    public Multimap<String, Object> getMetadata() {
        Multimap<String, Object> map = HashMultimap.create();
        map.put(OsStrings.tid(), fTid);
        return map;
    }
}
