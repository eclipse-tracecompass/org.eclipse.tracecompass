/*******************************************************************************
 * Copyright (c) 2018, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.model.timegraph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.tree.AbstractTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLog;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLogBuilder;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

/**
 * Class to abstract {@link ITimeGraphDataProvider} methods and fields. Handles
 * the exceptions that can be thrown by the concrete classes, and logs the time
 * taken to build the time graph models.
 *
 * @param <A>
 *            Generic type for the encapsulated
 *            {@link TmfStateSystemAnalysisModule}
 * @param <M>
 *            Generic type for the returned {@link ITimeGraphEntryModel}.
 * @author Loic Prieur-Drevon
 * @since 8.2
 */
public abstract class AbstractTimeGraphDataProvider<A extends TmfStateSystemAnalysisModule, M extends ITimeGraphEntryModel>
    extends AbstractTreeDataProvider<A, M> implements ITimeGraphDataProvider<M> {

    /**
     * Constructor
     *
     * @param trace
     *            the trace this provider represents
     * @param analysisModule
     *            the analysis encapsulated by this provider
     */
    public AbstractTimeGraphDataProvider(ITmfTrace trace, A analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public final TmfModelResponse<TimeGraphModel> fetchRowModel(Map<String, Object> parameters, @Nullable IProgressMonitor monitor) {
        A module = getAnalysisModule();
        if (!module.waitForInitialization()) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        long currentEnd = ss.getCurrentEndTime();
        Object times = parameters.get(DataProviderParameterUtils.REQUESTED_TIME_KEY);
        Object items = parameters.get(DataProviderParameterUtils.REQUESTED_ITEMS_KEY);
        if (!(times instanceof List<?>) || ((List<?>) times).isEmpty() || !(items instanceof Collection<?>)) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        Object end = Iterables.getLast(((List<?>) times));
        if (!(end instanceof Number)) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        boolean complete = ss.waitUntilBuilt(0) || ((Number) end).longValue() <= currentEnd;

        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "AbstractTimeGraphDataProvider#fetchRowModel") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {

            TimeGraphModel models = getRowModel(ss, parameters, monitor);
            if (models == null) {
                // getRowModel returns null if the query was cancelled.
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            long rows = models.getRows().size();
            long states = 0;
            for (ITimeGraphRowModel row : models.getRows()) {
                states += row.getStates().size();
            }
            scope.step("complete", "rows", rows, "states", states); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return new TmfModelResponse<>(models, complete ? Status.COMPLETED : Status.RUNNING,
                    complete ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING);
        } catch (StateSystemDisposedException | TimeRangeException | IndexOutOfBoundsException e) {
            return new TmfModelResponse<>(null, Status.FAILED, String.valueOf(e.getMessage()));
        }
    }

    @Override
    public @NonNull Multimap<@NonNull String, @NonNull Object> getFilterData(long entryId, long time, @Nullable IProgressMonitor monitor) {
        return ITimeGraphStateFilter.mergeMultimaps(ITimeGraphDataProvider.super.getFilterData(entryId, time, monitor),
                getEntryMetadata(entryId));
    }

    /**
     * Abstract method to be implemented by the providers to return rows. Lets the
     * abstract class handle waiting for {@link ITmfStateSystem} initialization and
     * progress, as well as error handling
     *
     * @param ss
     *            the {@link TmfStateSystemAnalysisModule}'s {@link ITmfStateSystem}
     * @param parameters
     *            the query's parameters
     * @param monitor
     *            progress monitor
     * @return the list of row models, null if the query was cancelled
     * @throws StateSystemDisposedException
     *             if the state system was closed during the query or could not be
     *             queried.
     */
    protected abstract @Nullable TimeGraphModel getRowModel(ITmfStateSystem ss,
            Map<String, Object> parameters, @Nullable IProgressMonitor monitor)
            throws StateSystemDisposedException;
}
