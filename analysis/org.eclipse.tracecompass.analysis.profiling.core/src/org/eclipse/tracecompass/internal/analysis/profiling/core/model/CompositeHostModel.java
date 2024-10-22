/*******************************************************************************
 * Copyright (c) 2016, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelThreadInformationProvider;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.analysis.profiling.core.model.ICpuTimeProvider;
import org.eclipse.tracecompass.analysis.profiling.core.model.IHostModel;
import org.eclipse.tracecompass.analysis.profiling.core.model.ISamplingDataProvider;
import org.eclipse.tracecompass.analysis.profiling.core.model.IThreadOnCpuProvider;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ModelListener.IModuleWrapper;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Operating system model based on analyses who implement certain interfaces to
 * provider the necessary information.
 *
 * @author Geneviève Bastien
 */
public class CompositeHostModel implements IHostModel {
    private final Multimap<ITmfTrace, Object> fTraceObjectMap = HashMultimap.create();

    private final Set<ICpuTimeProvider> fCpuTimeProviders = Objects.requireNonNull(Collections.newSetFromMap(new WeakHashMap<>()));
    private final Set<IThreadOnCpuProvider> fThreadOnCpuProviders = Objects.requireNonNull(Collections.newSetFromMap(new WeakHashMap<>()));
    private final Set<ISamplingDataProvider> fSamplingDataProviders = Objects.requireNonNull(Collections.newSetFromMap(new WeakHashMap<>()));
    private final Set<KernelAnalysisModule> fKernelModules = Objects.requireNonNull(Collections.newSetFromMap(new WeakHashMap<>()));

    private final String fHostId;

    /**
     * Constructor
     *
     * @param hostId
     *            The ID of the host this model is for
     */
    public CompositeHostModel(String hostId) {
        fHostId = hostId;
        TmfSignalManager.register(this);
    }

    @Override
    public int getThreadOnCpu(int cpu, long t, boolean block) {
        for (IThreadOnCpuProvider provider : fThreadOnCpuProviders) {
            Integer tid = provider.getThreadOnCpuAtTime(cpu, t, block);
            if (tid != null && tid != IHostModel.UNKNOWN_TID) {
                return tid;
            }
        }
        return IHostModel.UNKNOWN_TID;
    }

    @Override
    public long getCpuTime(int tid, long start, long end) {
        for (ICpuTimeProvider provider : fCpuTimeProviders) {
            long cpuTime = provider.getCpuTime(tid, start, end);
            if (cpuTime != IHostModel.TIME_UNKNOWN) {
                return cpuTime;
            }
        }
        return IHostModel.TIME_UNKNOWN;
    }

    @Override
    public Collection<AggregatedCallSite> getSamplingData(int tid, long start, long end) {
        for (ISamplingDataProvider provider : fSamplingDataProviders) {
            Collection<AggregatedCallSite> samples = provider.getSamplingData(tid, start, end);
            if (!samples.isEmpty()) {
                return samples;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public int getProcessId(int tid, long t) {
        Optional<Integer> pid = fKernelModules.stream()
                .map(module -> KernelThreadInformationProvider.getProcessId(module, tid, t))
                .filter(Objects::nonNull)
                .findFirst();
        return pid.isEmpty() ? IHostModel.UNKNOWN_TID : pid.get();
    }

    @Override
    public @Nullable String getExecName(int tid, long t) {
        Optional<String> execName = fKernelModules.stream()
                .map(module -> KernelThreadInformationProvider.getExecutableName(module, tid))
                .filter(Objects::nonNull)
                .findFirst();
        return execName.isPresent() ? execName.get() : null;
    }

    /**
     * Set a CPU time provider for this host model
     *
     * @param provider
     *            The CPU time provider
     */
    public void setCpuTimeProvider(ICpuTimeProvider provider) {
        fCpuTimeProviders.add(provider);
    }

    /**
     * Set a CPU time provider for this host model
     *
     * @param trace
     *            The trace associated with this provider
     * @param provider
     *            The CPU time provider
     */
    public void setCpuTimeProvider(ITmfTrace trace, ICpuTimeProvider provider) {
        fCpuTimeProviders.add(provider);
        fTraceObjectMap.put(trace, provider);
    }

    /**
     * Set a thread on CPU provider for this host model
     *
     * @param provider
     *            The thread on CPU time provider
     */
    public void setThreadOnCpuProvider(IThreadOnCpuProvider provider) {
        fThreadOnCpuProviders.add(provider);
    }

    /**
     * Set a thread on CPU provider for this host model
     *
     * @param trace
     *            The trace associated with this provider
     * @param provider
     *            The thread on CPU time provider
     */
    public void setThreadOnCpuProvider(ITmfTrace trace, IThreadOnCpuProvider provider) {
        fThreadOnCpuProviders.add(provider);
        fTraceObjectMap.put(trace, provider);
    }

    /**
     * Set a sampling data provider for this host model
     *
     * @param provider
     *            The sampling data provider
     */
    public void setSamplingDataProvider(ISamplingDataProvider provider) {
        fSamplingDataProviders.add(provider);
    }

    /**
     * Set a sampling data provider for this host model
     *
     * @param trace
     *            The trace associated with this provider
     * @param provider
     *            The sampling data provider
     */
    public void setSamplingDataProvider(ITmfTrace trace, ISamplingDataProvider provider) {
        fSamplingDataProviders.add(provider);
        fTraceObjectMap.put(trace, provider);
    }

    /**
     * Set a kernel module for this host model
     *
     * @param trace
     *            The trace this module belongs to
     * @param module
     *            The kernel analysis module
     */
    public void setKernelModule(ITmfTrace trace, KernelAnalysisModule module) {
        fKernelModules.add(module);
        fTraceObjectMap.put(trace, module);
    }

    @Override
    public String toString() {
        return String.valueOf(getClass());
    }

    /**
     * Iterator class to allow 2-way iteration over intervals of a given
     * attribute. Not thread-safe!
     */
    private static class ThreadStatusIterator implements Iterator<ProcessStatusInterval> {

        private final Iterator<ITmfStateInterval> fIntervalIterator;
        private final long fStart;
        private final long fEnd;
        private final int fTid;

        public ThreadStatusIterator(long start, long end, int tid, Iterator<ITmfStateInterval> iter) {
            fIntervalIterator = iter;
            fStart = start;
            fEnd = end;
            fTid = tid;
        }

        @Override
        public boolean hasNext() {
            return fIntervalIterator.hasNext();
        }

        @Override
        public ProcessStatusInterval next() {
            if (!fIntervalIterator.hasNext()) {
                throw new NoSuchElementException();
            }
            ITmfStateInterval interval = fIntervalIterator.next();
            long start = Math.max(interval.getStartTime(), fStart);
            long end = Math.min(interval.getEndTime() + 1, fEnd);
            return new ProcessStatusInterval(start, end, fTid, ProcessStatus.getStatusFromStateValue(interval.getStateValue()), null);
        }
    }

    /**
     * Iterator class to allow 2-way iteration over intervals of a given
     * attribute. Not thread-safe!
     */
    private static class ThreadStatusIterable implements Iterable<ProcessStatusInterval> {

        private final long fStart;
        private final long fEnd;
        private KernelAnalysisModule fModule;
        private int fTid;
        private long fResolution;

        public ThreadStatusIterable(long start, long end, KernelAnalysisModule module, int tid, long resolution) {
            fStart = start;
            fEnd = end;
            fModule = module;
            fTid = tid;
            fResolution = resolution;
        }

        @Override
        public Iterator<ProcessStatusInterval> iterator() {
            return new ThreadStatusIterator(fStart, fEnd, fTid, KernelThreadInformationProvider.getStatusIntervalsForThread(fModule, fTid, fStart, fEnd, fResolution));
        }
    }

    @Override
    public Iterable<ProcessStatusInterval> getThreadStatusIntervals(int tid, long start, long end, long resolution) {
        if (tid == IHostModel.UNKNOWN_TID) {
            return Objects.requireNonNull(Collections.emptyList());
        }
        Iterable<KernelAnalysisModule> modules = TmfTraceUtils.getAnalysisModulesOfClass(fHostId, KernelAnalysisModule.class);
        if (modules.iterator().hasNext()) {
            KernelAnalysisModule module = modules.iterator().next();
            return new ThreadStatusIterable(start, end, module, tid, resolution);
        }
        return Objects.requireNonNull(Collections.emptyList());
    }

    @Override
    public Map<Integer, Iterable<ProcessStatusInterval>> getThreadStatusIntervals(Collection<Integer> tids, Collection<Long> times, IProgressMonitor monitor) {
        Iterable<KernelAnalysisModule> modules = TmfTraceUtils.getAnalysisModulesOfClass(fHostId, KernelAnalysisModule.class);
        if (modules.iterator().hasNext()) {
            KernelAnalysisModule module = modules.iterator().next();
            @NonNull Map<Integer, List<ITmfStateInterval>> kernelStatusIntervals = KernelThreadInformationProvider.getStatusIntervalsForThreads(module, tids, times, monitor);
            // Mapping ITmfStateInterval to ProcessStatusInterval
            Map<Integer, Iterable<ProcessStatusInterval>> threadStatusIntervals = new HashMap<>();
            for (Entry<Integer, List<ITmfStateInterval>> intervalsEntry : kernelStatusIntervals.entrySet()) {
                List<ITmfStateInterval> intervals = Objects.requireNonNull(intervalsEntry.getValue());
                threadStatusIntervals.put(intervalsEntry.getKey(), intervals.stream()
                        .map(i -> {
                            Object state = i.getValue();
                            if (state instanceof String) {
                                return new ProcessStatusInterval(i.getStartTime(), i.getEndTime() + 1, intervalsEntry.getKey(), ProcessStatus.RUN_SYTEMCALL, i.getValueString());
                            }
                            return new ProcessStatusInterval(i.getStartTime(), i.getEndTime() + 1, intervalsEntry.getKey(), ProcessStatus.getStatusFromStateValue(i.getStateValue()), null);
                        })
                        .collect(Collectors.toList()));
            }
            return threadStatusIntervals;
        }
        return Objects.requireNonNull(Collections.emptyMap());
    }

    @Override
    public long getStartTime() {
        Optional<Long> start = fKernelModules.stream().map(v -> {
            ITmfStateSystem ss = v.getStateSystem();
            @NonNull Long startTime = ss != null ? ss.getStartTime() : Long.MIN_VALUE;
            return startTime;
        }).max(Long::compare);
        if (start.isPresent()) {
            return start.get();
        }
        return Long.MIN_VALUE;
    }

    @Override
    public long getEndTime() {
        Optional<Long> end = fKernelModules.stream().map(v -> {
            ITmfStateSystem ss = v.getStateSystem();
            @NonNull Long endTime = ss != null ? ss.getCurrentEndTime() : Long.MAX_VALUE;
            return endTime;
        }).min(Long::compare);
        if (end.isPresent()) {
            return end.get();
        }
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isSamplingDataAvailable() {
        return !fSamplingDataProviders.isEmpty();
    }

    @Override
    public boolean isThreadStatusAvailable() {
        Iterable<KernelAnalysisModule> modules = TmfTraceUtils.getAnalysisModulesOfClass(fHostId, KernelAnalysisModule.class);
        return modules.iterator().hasNext();
    }

    /**
     * Remove model elements associated with the trace being closed
     *
     * @param signal
     *            The traced closed signal
     */
    @TmfSignalHandler
    public void traceClosed(final TmfTraceClosedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        TmfTraceManager.getTraceSetWithExperiment(trace).forEach(t -> {
            Collection<Object> objects = fTraceObjectMap.removeAll(t);
            for (Object object : objects) {
                if (object instanceof ICpuTimeProvider) {
                    fCpuTimeProviders.remove(object);
                }
                if (object instanceof IThreadOnCpuProvider) {
                    fThreadOnCpuProviders.remove(object);
                }
                if (object instanceof ISamplingDataProvider) {
                    fSamplingDataProviders.remove(object);
                }
                if (object instanceof KernelAnalysisModule) {
                    fKernelModules.remove(object);
                }
            }
        });

        /* Dispose all models when the last trace is closed. */
        if (TmfTraceManager.getInstance().getOpenedTraces().isEmpty()) {
            ModelManager.disposeModels();
        }
    }

    @Override
    public void dispose() {
        TmfSignalManager.deregister(this);
    }

    @Override
    public Collection<IAnalysisModule> getRequiredModules(EnumSet<ModelDataType> requiredData) {
        List<IAnalysisModule> list = new ArrayList<>();
        if (requiredData.contains(ModelDataType.PID) || requiredData.contains(ModelDataType.EXEC_NAME) ||
                requiredData.contains(ModelDataType.KERNEL_STATES)) {
            // Add the kernel modules
            list.addAll(fKernelModules);
        }
        if (requiredData.contains(ModelDataType.TID)) {
            list.addAll(getModulesFrom(fThreadOnCpuProviders));
        }
        if (requiredData.contains(ModelDataType.CPU_TIME)) {
            list.addAll(getModulesFrom(fCpuTimeProviders));
        }
        if (requiredData.contains(ModelDataType.SAMPLING_DATA)) {
            list.addAll(getModulesFrom(fSamplingDataProviders));
        }
        return list;
    }

    private static Collection<IAnalysisModule> getModulesFrom(Collection<?> set) {
        List<IAnalysisModule> list = new ArrayList<>();
        for (Object obj : set) {
            if (obj instanceof IAnalysisModule) {
                list.add((IAnalysisModule) obj);
            } else if (obj instanceof IModuleWrapper) {
                Optional<IAnalysisModule> module = ((IModuleWrapper) obj).getModule();
                if (module.isPresent()) {
                    list.add(module.get());
                }
            }
        }
        return list;
    }
}
