/**********************************************************************
 * Copyright (c) 2018, 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.dataprovider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.AbstractCriticalPathModule;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.OSCriticalPathModule;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfStartAnalysisSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * {@link IDataProviderFactory} for the {@link CriticalPathDataProvider}
 *
 * @author Loic Prieur-Drevon
 */
public class OSCriticalPathDataProviderFactory implements IDataProviderFactory {

    private final Map<ITmfTrace, OSCriticalPathModule> map = new HashMap<>();


    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(CriticalPathDataProvider.ID)
            .setName(Objects.requireNonNull(Messages.CriticalPathDataProviderFactory_title))
            .setDescription(Objects.requireNonNull(Messages.CriticalPathDataProviderFactory_descriptionText))
            .setProviderType(ProviderType.TIME_GRAPH)
            .build();

    /**
     * Constructor, registers the module with the {@link TmfSignalManager}
     */
    public OSCriticalPathDataProviderFactory() {
        TmfSignalManager.register(this);
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        ITmfTrace localTrace = trace;
        if (localTrace instanceof TmfExperiment) {
            for(ITmfTrace tr: TmfTraceManager.getTraceSet(trace)) {
                ITmfTreeDataProvider<? extends ITmfTreeDataModel> pr = createProvider(tr);
                if (pr != null) {
                    return pr;
                }
            }
        }

        OSCriticalPathModule module = map.remove(trace);
        if (module == null) {
            // the DataProviderManager does not negative cache
            return null;
        }
        return new OSCriticalPathDataProvider(trace, module);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        // Note: Currently the descriptor will be returned for all types of traces
        return Collections.singletonList(DESCRIPTOR);
    }

    /**
     * {@link TmfSignalHandler} for when {@link AbstractCriticalPathModule} is started, as
     * the analysis is not registered with the trace, we use this to know to
     * associate a {@link AbstractCriticalPathModule} to a trace.
     *
     * @param startAnalysisSignal
     *            analysis started signal
     */
    @TmfSignalHandler
    public synchronized void analysisStarted(TmfStartAnalysisSignal startAnalysisSignal) {
        IAnalysisModule analysis = startAnalysisSignal.getAnalysisModule();
        if (analysis instanceof OSCriticalPathModule) {
            OSCriticalPathModule criticalPath = (OSCriticalPathModule) analysis;
            map.put(criticalPath.getTrace(), criticalPath);
        }
    }

    /**
     * Remove the closed traces' Critical Path Module to avoid resource leaks.
     *
     * @param traceClosedSignal
     *            the TMF trace closed signal
     */
    @TmfSignalHandler
    public synchronized void traceClosed(TmfTraceClosedSignal traceClosedSignal) {
        map.remove(traceClosedSignal.getTrace());
    }

}
