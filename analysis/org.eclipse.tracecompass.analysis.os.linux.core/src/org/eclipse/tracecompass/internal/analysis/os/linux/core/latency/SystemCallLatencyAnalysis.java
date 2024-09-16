/*******************************************************************************
 * Copyright (c) 2015, 2016 EfficiOS Inc., Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.core.latency;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.model.OsStrings;
import org.eclipse.tracecompass.analysis.os.linux.core.tid.TidAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisEventBasedModule;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IGroupingSegmentAspect;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.SyscallLookup;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;
import org.eclipse.tracecompass.segmentstore.core.SegmentStoreFactory.SegmentStoreType;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.lookup.TmfCallsite;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author Alexandre Montplaisir
 * @since 2.0
 */
public class SystemCallLatencyAnalysis extends AbstractSegmentStoreAnalysisEventBasedModule {

    /**
     * The ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.latency.syscall"; //$NON-NLS-1$
    private static final String RET_FIELD = "ret"; //$NON-NLS-1$
    private static final int VERSION = 2;

    private static final Collection<ISegmentAspect> BASE_ASPECTS = ImmutableList.of(SyscallNameAspect.INSTANCE, SyscallTidAspect.INSTANCE, SyscallRetAspect.INSTANCE, SyscallComponentAspect.INSTANCE, SyscallFileAspect.INSTANCE);

    /**
     * Constructor
     */
    public SystemCallLatencyAnalysis() {
        // do nothing
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new IllegalStateException();
        }
        IAnalysisModule module = trace.getAnalysisModule(TidAnalysisModule.ID);
        if (module == null) {
            return Collections.emptySet();
        }
        return ImmutableSet.of(module);
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
    protected AbstractSegmentStoreAnalysisRequest createAnalysisRequest(ISegmentStore<ISegment> syscalls, IProgressMonitor monitor) {
        return new SyscallLatencyAnalysisRequest(syscalls, monitor);
    }

    @Override
    protected @NonNull IHTIntervalReader<ISegment> getSegmentReader() {
        return SystemCall.READER;
    }

    private class SyscallLatencyAnalysisRequest extends AbstractSegmentStoreAnalysisRequest {

        private final Map<Integer, SystemCall.InitialInfo> fOngoingSystemCalls = new HashMap<>();
        private @Nullable IKernelAnalysisEventLayout fLayout;
        private final IProgressMonitor fMonitor;

        public SyscallLatencyAnalysisRequest(ISegmentStore<ISegment> syscalls, IProgressMonitor monitor) {
            super(syscalls);
            fMonitor = monitor;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            IKernelAnalysisEventLayout layout = fLayout;
            if (layout == null) {
                IKernelTrace trace = (IKernelTrace) event.getTrace();
                layout = trace.getKernelEventLayout();
                fLayout = layout;
            }
            final String eventName = event.getName();

            if (eventName.startsWith(layout.eventSyscallEntryPrefix()) ||
                    eventName.startsWith(layout.eventCompatSyscallEntryPrefix())) {
                /* This is a system call entry event */

                Integer tid;
                try {
                    tid = KernelTidAspect.INSTANCE.resolve(event, true, fMonitor);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (tid == null) {
                    // no information on this event/trace ?
                    return;
                }

                /* Record the event's data into the intial system call info */
                // String syscallName = fLayout.getSyscallNameFromEvent(event);
                long startTime = event.getTimestamp().toNanos();
                String syscallName = eventName.substring(layout.eventSyscallEntryPrefix().length());

                SystemCall.InitialInfo newSysCall = new SystemCall.InitialInfo(startTime, syscallName.intern(), tid);
                fOngoingSystemCalls.put(tid, newSysCall);

            } else if (eventName.startsWith(layout.eventSyscallExitPrefix())) {
                /* This is a system call exit event */

                Integer tid;
                try {
                    tid = KernelTidAspect.INSTANCE.resolve(event, true, fMonitor);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (tid == null) {
                    return;
                }

                SystemCall.InitialInfo info = fOngoingSystemCalls.remove(tid);
                if (info == null) {
                    /*
                     * We have not seen the entry event corresponding to this
                     * exit (lost event, or before start of trace).
                     */
                    return;
                }

                long endTime = event.getTimestamp().toNanos();
                Integer ret = event.getContent().getFieldValue(Integer.class, RET_FIELD);
                SystemCall syscall = new SystemCall(info, endTime, ret == null ? -1 : ret);
                getSegmentStore().add(syscall);
            }
        }

        @Override
        public void handleCompleted() {
            fOngoingSystemCalls.clear();
            super.handleCompleted();
        }

        @Override
        public void handleCancel() {
            fMonitor.setCanceled(true);
            super.handleCancel();
        }
    }

    private static final class SyscallNameAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new SyscallNameAspect();

        private SyscallNameAspect() {
            // Do nothing
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectHelpText_SystemCall);
        }

        @Override
        public String getName() {
            return Messages.getMessage(Messages.SegmentAspectName_SystemCall);
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
                if (segment1 instanceof SystemCall && segment2 instanceof SystemCall) {
                    int res = ((SystemCall) segment1).getName().compareTo(((SystemCall) segment2).getName());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof SystemCall) {
                return ((SystemCall) segment).getName();
            }
            return EMPTY_STRING;
        }
    }

    private static final class SyscallTidAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new SyscallTidAspect();

        private SyscallTidAspect() {
            // Do nothing
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectHelpText_SystemCallTid);
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
                if (segment1 instanceof SystemCall && segment2 instanceof SystemCall) {
                    int res = Integer.compare(((SystemCall) segment1).getTid(), ((SystemCall) segment2).getTid());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable Integer resolve(ISegment segment) {
            if (segment instanceof SystemCall) {
                return ((SystemCall) segment).getTid();
            }
            return -1;
        }

        @Override
        public DataType getDataType() {
            return DataType.NUMBER;
        }
    }

    private static final class SyscallComponentAspect implements IGroupingSegmentAspect {
        public static final ISegmentAspect INSTANCE = new SyscallComponentAspect();

        private SyscallComponentAspect() {
            // Do nothing
        }

        @Override
        public String getName() {
            return Messages.getMessage(Messages.SystemCallLatencyAnalysis_componentName);
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SystemCallLatencyAnalysis_componentDescription);
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
                if (segment1 instanceof SystemCall && segment2 instanceof SystemCall) {
                    int res = SyscallLookup.getInstance().getComponent(((SystemCall) segment1).getName()).compareTo(SyscallLookup.getInstance().getComponent(((SystemCall) segment2).getName()));
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable Object resolve(ISegment segment) {
            if (segment instanceof SystemCall) {
                return SyscallLookup.getInstance().getComponent(((SystemCall) segment).getName());
            }
            return EMPTY_STRING;
        }
    }

    private static final class SyscallRetAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new SyscallRetAspect();

        private SyscallRetAspect() {
            // Do nothing
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SegmentAspectHelpText_SystemCallRet);
        }

        @Override
        public String getName() {
            return Messages.getMessage(Messages.SegmentAspectName_SystemCallRet);
        }

        @Override
        public @Nullable Object resolve(ISegment segment) {
            if (segment instanceof SystemCall) {
                return ((SystemCall) segment).getReturnValue();
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
                if (segment1 instanceof SystemCall && segment2 instanceof SystemCall) {
                    int res = Integer.compare(((SystemCall) segment1).getReturnValue(), ((SystemCall) segment2).getReturnValue());
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

    }

    private static final class SyscallFileAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new SyscallFileAspect();

        private SyscallFileAspect() {
            // Do nothing
        }

        @Override
        public String getName() {
            return Messages.getMessage(Messages.SystemCallLatencyAnalysis_fileName);
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SystemCallLatencyAnalysis_fileDescription);
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
                if (segment1 instanceof SystemCall && segment2 instanceof SystemCall) {
                    int res = SyscallLookup.getInstance().getFile(((SystemCall) segment1).getName()).compareTo(SyscallLookup.getInstance().getFile(((SystemCall) segment2).getName()));
                    return (res != 0 ? res : SegmentComparators.INTERVAL_START_COMPARATOR.thenComparing(SegmentComparators.INTERVAL_END_COMPARATOR).compare(segment1, segment2));
                }
                return 1;
            };
        }

        @Override
        public @Nullable Object resolve(ISegment segment) {
            if (segment instanceof SystemCall) {
                return SyscallLookup.getInstance().getFile(((SystemCall) segment).getName());
            }
            return EMPTY_STRING;
        }
    }

    /**
     * Callsite aspect for system calls
     */
    public static final class SyscallCallsiteAspect implements ISegmentAspect {

        /**
         * Instance
         */
        public static final ISegmentAspect INSTANCE = new SyscallCallsiteAspect();

        private SyscallCallsiteAspect() {
            // Do nothing
        }

        @Override
        public String getName() {
            return Messages.getMessage(Messages.SystemCallLatencyAnalysis_sourceLookupName);
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.SystemCallLatencyAnalysis_sourceLookupDescription);
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }

        @Override
        public @Nullable Object resolve(ISegment segment) {
            String file = (String) SyscallFileAspect.INSTANCE.resolve(segment);
            if (file == null || file.isEmpty()) {
                return null;
            }
            return new TmfCallsite(file, 0L);
        }

    }
}
