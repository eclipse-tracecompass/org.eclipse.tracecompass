/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.examples.core.data.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfLostEvent;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Simple events statistics data provider
 * 
 * @author Bernd Hufmann
 */
@SuppressWarnings("null")
@NonNullByDefault
public class ExampleDataTreeDataProvider implements ITmfTreeDataProvider<TmfTreeDataModel> {
    private static long fCount = 0;

    private @Nullable ITmfTrace fTrace;
    private @Nullable StatsPerTypeRequest fRequest;
    private @Nullable List<TmfTreeDataModel> fCachedResult = null;

    /**
     * Constructor
     * @param trace
     *          the trace (not experiment)
     */
    public ExampleDataTreeDataProvider(ITmfTrace trace) {
        fTrace = trace;
    }

    @Override
    public @NonNull String getId() {
        return "org.eclipse.tracecompass.examples.nomodulestats"; //$NON-NLS-1$
    }

    @Override
    public @NonNull TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {

        ITmfTrace trace = fTrace;
        if (trace == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        StatsPerTypeRequest request = fRequest;
        if (request == null) {
            // Start new request
            request = new StatsPerTypeRequest(trace, TmfTimeRange.ETERNITY);
            trace.sendRequest(request);
            TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), Collections.emptyList()); //$NON-NLS-1$ //$NON-NLS-2$
            fRequest = request;
            return new TmfModelResponse<>(model, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        if (request.isCancelled()) {
            TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), Collections.emptyList()); //$NON-NLS-1$ //$NON-NLS-2$
            return new TmfModelResponse<>(model, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
        }

        if (!request.isCompleted()) {
            TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), Collections.emptyList()); //$NON-NLS-1$ //$NON-NLS-2$
            return new TmfModelResponse<>(model, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        List<TmfTreeDataModel> values = fCachedResult;
        if (values == null) {
            long traceId = fCount++;
            values = new ArrayList<>();
            long total = 0;
            for (Entry<String, Long> entry : request.getResults().entrySet()) {
                values.add(new TmfTreeDataModel(fCount++, traceId, List.of(entry.getKey(), String.valueOf(entry.getValue()))));
                total += entry.getValue();
            }
            TmfTreeDataModel traceEntry = new TmfTreeDataModel(traceId, -1, List.of(trace.getName(), String.valueOf(total)));
            values.add(0, traceEntry);
            fCachedResult = values;
        }
        TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), values); //$NON-NLS-1$ //$NON-NLS-2$
        return new TmfModelResponse<>(model, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }


    private class StatsPerTypeRequest extends TmfEventRequest {

        /* Map in which the results are saved */
        private final Map<@NonNull String, @NonNull Long> stats;

        public StatsPerTypeRequest(ITmfTrace trace, TmfTimeRange range) {
            super(trace.getEventType(), range, 0, ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.BACKGROUND);
            this.stats = new HashMap<>();
        }

        public Map<@NonNull String, @NonNull Long> getResults() {
            return stats;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            if (event.getTrace() == fTrace) {
                String eventType = event.getName();
                /*
                 * Special handling for lost events: instead of counting just
                 * one, we will count how many actual events it represents.
                 */
                if (event instanceof ITmfLostEvent) {
                    ITmfLostEvent le = (ITmfLostEvent) event;
                    incrementStats(eventType, le.getNbLostEvents());
                    return;
                }

                /* For standard event types, just increment by one */
                incrementStats(eventType, 1L);
            }
        }

        private void incrementStats(@NonNull String key, long count) {
            stats.merge(key, count, Long::sum);
        }
    }

    @Override
    public void dispose() {
        fRequest = null;
        fCachedResult = null;
    }
}
