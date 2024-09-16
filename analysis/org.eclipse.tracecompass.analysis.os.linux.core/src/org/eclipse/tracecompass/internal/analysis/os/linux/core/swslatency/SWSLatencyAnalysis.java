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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.OsStrings;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisEventBasedModule;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.segmentstore.core.SegmentStoreFactory.SegmentStoreType;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;

import com.google.common.collect.ImmutableList;

/**
 * This analysis module computes the latency between the sched_wakeup event and
 * the sched_switch event for each thread
 *
 * @author Abdellah Rahmani
 * @since 8.1
 */
public class SWSLatencyAnalysis extends AbstractSegmentStoreAnalysisEventBasedModule {

    /**
     * The ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.core.swslatency.sws"; //$NON-NLS-1$

    private static final int VERSION = 1;

    private static final Collection<ISegmentAspect> BASE_ASPECTS = ImmutableList.of(SWSThreadAspect.INSTANCE, SWSTidAspect.INSTANCE, SWSPriorityAspect.INSTANCE);

    /**
     * Constructor
     */
    public SWSLatencyAnalysis() {
        // do nothing
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return BASE_ASPECTS;
    }

    @Override
    protected int getVersion() {
        return VERSION;
    }

    @Override
    protected @NonNull SegmentStoreType getSegmentStoreType() {
        return SegmentStoreType.OnDisk;
    }

    @Override
    protected AbstractSegmentStoreAnalysisRequest createAnalysisRequest(ISegmentStore<ISegment> swsSegment, IProgressMonitor monitor) {
        return new SWSLatencyAnalysisRequest(swsSegment, monitor);
    }

    @Override
    protected @NonNull IHTIntervalReader<ISegment> getSegmentReader() {
        return SchedWS.READER;
    }

    private class SWSLatencyAnalysisRequest extends AbstractSegmentStoreAnalysisRequest {
        private final Map<Integer, SchedWS.InitialInfo> fOngoingSWS = new HashMap<>();
        private @Nullable IKernelAnalysisEventLayout fLayout;
        private final IProgressMonitor fMonitor;

        public SWSLatencyAnalysisRequest(ISegmentStore<ISegment> swsSegment, IProgressMonitor monitor) {
            super(swsSegment);
            fMonitor = monitor;
        }

        @Override
        public void handleData(ITmfEvent event) {
            super.handleData(event);
            IKernelAnalysisEventLayout layout = fLayout;
            if (layout == null) {
                IKernelTrace trace = (IKernelTrace) event.getTrace();
                layout = trace.getKernelEventLayout();
                fLayout = layout;
            }
            String eventName = event.getName();

            if (eventName.equals(layout.eventSchedProcessWakeup()) ||
                    eventName.equals(layout.eventSchedProcessWakeupNew()) ||
                    eventName.equals(layout.eventSchedProcessWaking())) {
                /* This is a sched_wakeup event */
                Integer tid = event.getContent().getFieldValue(Integer.class, layout.fieldTid());

                if (tid == null) {
                    // no information on this event/trace ?
                    return;
                }

                /* Record the event's data into the initial system call info */
                long startTime = event.getTimestamp().toNanos();
                String threadName = event.getContent().getFieldValue(String.class, layout.fieldComm());

                if (threadName == null || "".equals(threadName)) { //$NON-NLS-1$
                    threadName = "UNKNOWN"; //$NON-NLS-1$
                }

                SchedWS.InitialInfo newSchedWS = new SchedWS.InitialInfo(startTime, threadName.intern(), tid);
                fOngoingSWS.put(tid, newSchedWS);
            } else if (eventName.equals(layout.eventSchedSwitch())) {
                /* This is a sched_switch event */
                Integer tid = event.getContent().getFieldValue(Integer.class, layout.fieldNextTid());

                if (tid == null) {
                    return;
                }
                SchedWS.InitialInfo info = fOngoingSWS.remove(tid);
                if (info == null) {
                    /*
                     * We have not seen the sched_wakeup event corresponding to
                     * this thread (lost event, or before start of trace).
                     */
                    return;
                }
                long endTime = event.getTimestamp().toNanos();
                Integer priority = event.getContent().getFieldValue(Integer.class, layout.fieldNextPrio());
                SchedWS swscall = new SchedWS(info, endTime, priority == null ? -1 : priority);
                getSegmentStore().add(swscall);
            }
        }

        @Override
        public void handleCompleted() {
            fOngoingSWS.clear();
            super.handleCompleted();
        }

        @Override
        public void handleCancel() {
            fMonitor.setCanceled(true);
            super.handleCancel();
        }
    }

    private static final class SWSPriorityAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new SWSPriorityAspect();

        private SWSPriorityAspect() {
            // Do nothing
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectHelpText_SWSPrio);
        }

        @Override
        public String getName() {
            return Messages.getMessage(Messages.SegmentAspectName_SWSPrio);
        }

        @Override
        public @Nullable Object resolve(ISegment segment) {
            if (segment instanceof SchedWS) {
                return ((SchedWS) segment).getPriority();
            }
            return EMPTY_STRING;
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return (ISegment segment1, ISegment segment2) -> {
                if (segment1 == null) {
                    return 1;
                }
                if (segment2 == null) {
                    return -1;
                }
                if (segment1 instanceof SchedWS && segment2 instanceof SchedWS) {
                    int res = Integer.compare(((SchedWS) segment1).getPriority(), ((SchedWS) segment2).getPriority());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }
    }

    private static final class SWSTidAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new SWSTidAspect();

        private SWSTidAspect() {
            // Do nothing
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectHelpText_SWSTid);
        }

        @Override
        public String getName() {
            return OsStrings.tid();
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return (ISegment segment1, ISegment segment2) -> {
                if (segment1 == null) {
                    return 1;
                }
                if (segment2 == null) {
                    return -1;
                }
                if (segment1 instanceof SchedWS && segment2 instanceof SchedWS) {
                    int res = Integer.compare(((SchedWS) segment1).getTid(), ((SchedWS) segment2).getTid());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 0;
            };
        }

        @Override
        public @Nullable Integer resolve(ISegment segment) {
            if (segment instanceof SchedWS) {
                return ((SchedWS) segment).getTid();
            }
            return -1;
        }

        @Override
        public DataType getDataType() {
            return DataType.NUMBER;
        }
    }

    private static final class SWSThreadAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new SWSThreadAspect();

        private SWSThreadAspect() {
            // Do nothing
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectHelpText_SWSTid);
        }

        @Override
        public String getName() {
            return OsStrings.execName();
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return (ISegment segment1, ISegment segment2) -> {
                if (segment1 == null) {
                    return 1;
                }
                if (segment2 == null) {
                    return -1;
                }
                if (segment1 instanceof SchedWS && segment2 instanceof SchedWS) {
                    int res = ((SchedWS) segment1).getName().compareToIgnoreCase(((SchedWS) segment2).getName());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof SchedWS) {
                return ((SchedWS) segment).getName();
            }
            return null;
        }
    }
}
