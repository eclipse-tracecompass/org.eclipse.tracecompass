/**********************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.model.OsStrings;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * {@link TimeGraphEntryModel} for the Flame chart data
 *
 * @author Geneviève Bastien
 */
public class FlameChartEntryModel extends TimeGraphEntryModel {

    /**
     * An enumeration for the type of flame chart entries
     */
    public enum EntryType {
        /**
         * A descriptive entry, for example the one for the trace
         */
        TRACE,
        /**
         * Represent a group of the callstack analysis
         */
        LEVEL,
        /**
         * Represent an entry with function data, the actual callstack data
         */
        FUNCTION,
        /**
         * This entry will show the kernel statuses for the TID running the
         * callstack. Will not always be present
         */
        KERNEL
    }

    /**
     * A builder class for this entry model
     */
    public static class Builder {
        private final long fId;
        private final long fParentId;
        private final long fStartTime;
        private final String fName;
        private final EntryType fEntryType;
        private final int fDepth;
        private long fEndTime;
        private @Nullable HostThread fHostThread;

        /**
         * Constructor
         *
         * @param id
         *            The unique ID for this Entry model for its trace
         * @param parentId
         *            The unique ID of the parent entry model
         * @param name
         *            The name of this entry
         * @param start
         *            the thread's start time
         * @param entryType
         *            The type of this entry
         * @param depth
         *            The depth if the entry is a function entry, can be
         *            <code>-1</code> if no depth
         */
        public Builder(long id, long parentId, String name, long start, EntryType entryType, int depth) {
            fId = id;
            fParentId = parentId;
            fName = name;
            fStartTime = start;
            fEntryType = entryType;
            fDepth = depth;
            fEndTime = start;
            fHostThread = null;
        }

        /**
         * Get the unique ID for this entry / builder
         *
         * @return this entry's unique ID
         */
        public long getId() {
            return fId;
        }

        /**
         * Get this entry / builder's start time
         *
         * @return the start time
         */
        public long getStartTime() {
            return fStartTime;
        }

        /**
         * Get this entry/builder's end time
         *
         * @return the end time
         */
        public long getEndTime() {
            return fEndTime;
        }

        /**
         * Update this entry / builder's end time
         *
         * @param endTime
         *            the new end time
         */
        public void setEndTime(long endTime) {
            fEndTime = Long.max(fEndTime, endTime);
        }

        /**
         * Update this entry / builder's HostThread
         *
         * @param hostThread
         *            the new HostThread
         */
        public void setHostThread(@Nullable HostThread hostThread) {
            fHostThread = hostThread;
        }

        /**
         * Build the {@link FlameChartEntryModel} from the builder, specify the
         * parent id here to avoid race conditions
         *
         * @return the relevant {@link FlameChartEntryModel}
         */
        public FlameChartEntryModel build() {
            return new FlameChartEntryModel(fId, fParentId, Collections.singletonList(fName), fStartTime, fEndTime, fEntryType, fDepth, fHostThread);
        }

        /**
         * Get the parent ID of this entry
         *
         * @return The parent ID
         */
        public long getParentId() {
            return fParentId;
        }

        /**
         * Get the depth of this entry
         *
         * @return The depth
         */
        public int getDepth() {
            return fDepth;
        }
    }

    private final EntryType fEntryType;
    private final int fDepth;
    private @Nullable HostThread fHostThread;

    /**
     * Constructor
     *
     * @param id
     *            unique ID for this {@link FlameChartEntryModel}
     * @param parentId
     *            parent's ID to build the tree
     * @param name
     *            entry's name
     * @param startTime
     *            entry's start time
     * @param endTime
     *            entry's end time
     * @param entryType
     *            The type of this entry
     */
    public FlameChartEntryModel(long id, long parentId, List<String> name, long startTime, long endTime, EntryType entryType) {
        super(id, parentId, name, startTime, endTime);
        fEntryType = entryType;
        fDepth = -1;
    }

    /**
     * Constructor
     *
     * @param elementId
     *            unique ID for this {@link FlameChartEntryModel}
     * @param parentId
     *            parent's ID to build the tree
     * @param name
     *            entry's name
     * @param startTime
     *            entry's start time
     * @param endTime
     *            entry's end time
     * @param entryType
     *            The type of this entry
     * @param depth
     *            The depth this entry represents
     * @param hostThread
     *            The entry's unique hostThread or <code>null</code> if host
     *            thread not available or variable
     */
    public FlameChartEntryModel(long elementId, long parentId, List<String> name, long startTime, long endTime, EntryType entryType, int depth, @Nullable HostThread hostThread) {
        super(elementId, parentId, name, startTime, endTime);
        fEntryType = entryType;
        fDepth = depth;
        fHostThread = hostThread;
    }

    /**
     * Getter for the entry type
     *
     * @return The type of entry.
     */
    public EntryType getEntryType() {
        return fEntryType;
    }

    /**
     * Get the depth of this entry
     *
     * @return The depth of this entry
     */
    public int getDepth() {
        return fDepth;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!super.equals(obj)) {
            // nullness, class, name, ids
            return false;
        }
        if (!(obj instanceof FlameChartEntryModel)) {
            return false;
        }
        FlameChartEntryModel other = (FlameChartEntryModel) obj;
        return fEntryType == other.fEntryType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fEntryType);
    }

    @Override
    public String toString() {
        return super.toString() + ' ' + fEntryType.toString();
    }

    @Override
    public Multimap<String, Object> getMetadata() {
        Multimap<String, Object> map = HashMultimap.create();
        HostThread hostThread = fHostThread;
        if (hostThread != null) {
            map.put(OsStrings.tid(), hostThread.getTid());
            map.put("hostId", hostThread.getHost()); //$NON-NLS-1$
        }
        return map;
    }
}
