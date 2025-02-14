/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.tmf.core.model.xy;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.TmfXyResponseFactory;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.AbstractTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLog;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLogBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Class to abstract {@link ITmfTreeXYDataProvider} methods and fields for data
 * provider that share the values of X axis for all series (for instance line
 * charts). Handles the exceptions that can be thrown by the concrete classes,
 * and logs the time taken to build the XY models.
 *
 * @param <A>
 *            Generic type for the encapsulated
 *            {@link TmfStateSystemAnalysisModule}
 * @param <M>
 *            Generic type for the returned {@link ITmfTreeDataModel}.
 * @author Loic Prieur-Drevon
 * @since 8.2
 */
public abstract class AbstractTreeCommonXDataProvider<A extends TmfStateSystemAnalysisModule, M extends ITmfTreeDataModel>
    extends AbstractTreeDataProvider<A, M> implements ITmfTreeXYDataProvider<M> {

    /**
     * Constructor
     *
     * @param trace
     *            the trace this provider represents
     * @param analysisModule
     *            the analysis encapsulated by this provider
     */
    public AbstractTreeCommonXDataProvider(ITmfTrace trace, A analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public final TmfModelResponse<ITmfXyModel> fetchXY(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        A module = getAnalysisModule();

        // TODO server: Parameters validation should be handle separately. It
        // can be either in the data provider itself or before calling it. It
        // will avoid the creation of filters and the content of the map can be
        // use directly.
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return TmfXyResponseFactory.createFailedResponse(CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        TmfModelResponse<ITmfXyModel> res = verifyParameters(module, filter, monitor);
        if (res != null) {
            return res;
        }

        ITmfStateSystem ss = Objects.requireNonNull(module.getStateSystem(),
                "Statesystem should have been verified by verifyParameters"); //$NON-NLS-1$
        long currentEnd = ss.getCurrentEndTime();
        boolean complete = ss.waitUntilBuilt(0) || filter.getEnd() <= currentEnd;

        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "AbstractTreeXyDataProvider#fetchXY") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {
            Collection<IYModel> yModels = getYSeriesModels(ss, fetchParameters, monitor);
            if (yModels == null) {
                // getModels returns null if the query was cancelled.
                return TmfXyResponseFactory.createCancelledResponse(CommonStatusMessage.TASK_CANCELLED);
            }
            return TmfXyResponseFactory.create(getTitle(), filter.getTimesRequested(), ImmutableList.copyOf(yModels), complete);
        } catch (StateSystemDisposedException | TimeRangeException | IndexOutOfBoundsException e) {
            return TmfXyResponseFactory.createFailedResponse(String.valueOf(e.getMessage()));
        }
    }

    /**
     * Abstract method to be implemented by the providers to return trees. Lets the
     * abstract class handle waiting for {@link ITmfStateSystem} initialization and
     * progress, as well as error handling
     *
     * @param ss
     *            the {@link TmfStateSystemAnalysisModule}'s {@link ITmfStateSystem}
     * @param fetchParameters
     *            the query's filter
     * @param monitor
     *            progress monitor
     * @return the map of models, null if the query was cancelled
     * @throws StateSystemDisposedException
     *             if the state system was closed during the query or could not be
     *             queried.
     */
    protected abstract @Nullable Collection<IYModel> getYSeriesModels(ITmfStateSystem ss,
            Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor)
                    throws StateSystemDisposedException;

    /**
     * Getter for the title of this provider
     *
     * @return this provider's title
     */
    protected abstract String getTitle();

}
