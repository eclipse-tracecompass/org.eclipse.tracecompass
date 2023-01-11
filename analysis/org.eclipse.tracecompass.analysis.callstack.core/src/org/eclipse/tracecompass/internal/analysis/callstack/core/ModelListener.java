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

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.tid.TidAnalysisModule;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.ITmfNewAnalysisModuleListener;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Listener for the {@link CompositeHostModel} that waits for new modules and
 * adds them to the appropriate model concept.
 *
 * @author Geneviève Bastien
 */
public class ModelListener implements ITmfNewAnalysisModuleListener {

    private static final Comparator<ITmfStateInterval> INTERVAL_COMPARATOR = new Comparator<ITmfStateInterval>() {

        @Override
        public int compare(@Nullable ITmfStateInterval o1, @Nullable ITmfStateInterval o2) {
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return o1.getEndTime() < o2.getEndTime() ? -1 : o1.getEndTime() == o2.getEndTime() ? 0 : 1;
        }
    };

    /**
     * Interface to be implemented by module wrapper to return the module.
     * Package-private, as only the composite model should need this
     */
    interface IModuleWrapper {
        Optional<IAnalysisModule> getModule();
    }

    private Map<TidAnalysisModule, TidAnalysisWrapper> fTidModules = new WeakHashMap<>();

    @Override
    public void moduleCreated(@Nullable IAnalysisModule module) {
        if (module instanceof ICpuTimeProvider) {
            ICpuTimeProvider provider = (ICpuTimeProvider) module;
            for (String hostId : provider.getHostIds()) {
                IHostModel model = ModelManager.getModelFor(hostId);
                if (model instanceof CompositeHostModel) {
                    ((CompositeHostModel) model).setCpuTimeProvider(provider);
                }
            }
        }

        if (module instanceof IThreadOnCpuProvider) {
            IThreadOnCpuProvider provider = (IThreadOnCpuProvider) module;
            for (String hostId : provider.getHostIds()) {
                IHostModel model = ModelManager.getModelFor(hostId);
                if (model instanceof CompositeHostModel) {
                    ((CompositeHostModel) model).setThreadOnCpuProvider(provider);
                }
            }
        }

        if (module instanceof TidAnalysisModule) {
            TidAnalysisModule provider = (TidAnalysisModule) module;
            ITmfTrace trace = provider.getTrace();
            if (trace != null) {
                IHostModel model = ModelManager.getModelFor(trace.getHostId());
                TidAnalysisWrapper tidAnalysisWrapper = new TidAnalysisWrapper(provider, trace.getHostId());
                fTidModules.put(provider, tidAnalysisWrapper);
                ((CompositeHostModel) model).setThreadOnCpuProvider(trace, tidAnalysisWrapper);
                ((CompositeHostModel) model).setCpuTimeProvider(trace, tidAnalysisWrapper);
            }
        }

        if (module instanceof ISamplingDataProvider) {
            ISamplingDataProvider provider = (ISamplingDataProvider) module;
            for (String hostId : provider.getHostIds()) {
                IHostModel model = ModelManager.getModelFor(hostId);
                if (model instanceof CompositeHostModel) {
                    ((CompositeHostModel) model).setSamplingDataProvider(provider);
                }
            }
        }

        if (module instanceof KernelAnalysisModule) {
            KernelAnalysisModule provider = (KernelAnalysisModule) module;
            ITmfTrace trace = provider.getTrace();
            if (trace != null) {
                IHostModel model = ModelManager.getModelFor(trace.getHostId());
                ((CompositeHostModel) model).setKernelModule(trace, provider);
            }
        }
    }

    private static class TidAnalysisWrapper implements IThreadOnCpuProvider, ICpuTimeProvider, IModuleWrapper {

        private final WeakReference<@Nullable TidAnalysisModule> fModule;
        private final Collection<String> fHostIds;

        public TidAnalysisWrapper(TidAnalysisModule module, String hostId) {
            fHostIds = Collections.singleton(hostId);
            fModule = new WeakReference<>(module);
        }

        @Override
        public @Nullable Integer getThreadOnCpuAtTime(int cpu, long time, boolean block) {
            TidAnalysisModule module = fModule.get();
            if (module == null) {
                return null;
            }
            // Wait for the module to be queryable if blocking is requested
            while (block && !module.isQueryable(time)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // just get out of the loop
                    break;
                }
            }
            return module.getThreadOnCpuAtTime(cpu, time);
        }

        @Override
        public @NonNull Collection<@NonNull String> getHostIds() {
            return fHostIds;
        }

        @Override
        public long getCpuTime(int tid, long start, long realEnd) {
            TidAnalysisModule module = fModule.get();
            if (module == null) {
                return IHostModel.TIME_UNKNOWN;
            }
            ITmfStateSystem stateSystem = module.getStateSystem();
            if (stateSystem == null) {
                return IHostModel.TIME_UNKNOWN;
            }
            long cpuTime = 0;

            if (tid < 0) {
                return IHostModel.TIME_UNKNOWN;
            }

            long time = Long.max(start, stateSystem.getStartTime());
            final long end = Math.min(realEnd, stateSystem.getCurrentEndTime());
            boolean found = false;
            try {
                while (time < end) {
                    found = false;
                    // check if the process was on a CPU at the time
                    List<ITmfStateInterval> states = stateSystem.queryFullState(time);
                    for (ITmfStateInterval interval : states) {
                        if (interval.getStateValue().unboxLong() == tid) {
                            long endTime = Math.min(end, interval.getEndTime() + 1);
                            cpuTime += endTime - time;
                            time = endTime + 1;
                            found = true;
                        }
                    }
                    if (!found) {
                        Collections.sort(states, INTERVAL_COMPARATOR);
                        while (time < end && !found) {
                            ITmfStateInterval interval = states.remove(0);
                            time = interval.getEndTime() + 1;
                            if (time > end) {
                                continue;
                            }
                            ITmfStateInterval next = stateSystem.querySingleState(time, interval.getAttribute());
                            if (next.getStateValue().unboxLong() == tid) {
                                long endTime = Math.min(end, next.getEndTime() + 1);
                                cpuTime += endTime - time;
                                time = endTime + 1;
                                found = true;
                            } else {
                                int pos = Collections.binarySearch(states, next, INTERVAL_COMPARATOR);
                                if (pos < 0) {
                                    states.add(-pos - 1, next);
                                } else {
                                    states.add(interval);
                                }
                            }
                        }
                    }
                }
            } catch (StateSystemDisposedException e) {
                return IHostModel.TIME_UNKNOWN;
            }
            return cpuTime;
        }

        @SuppressWarnings("null")
        @Override
        public Optional<IAnalysisModule> getModule() {
            TidAnalysisModule module = fModule.get();
            return module == null ? Optional.empty() : Optional.of(module);
        }
    }
}
