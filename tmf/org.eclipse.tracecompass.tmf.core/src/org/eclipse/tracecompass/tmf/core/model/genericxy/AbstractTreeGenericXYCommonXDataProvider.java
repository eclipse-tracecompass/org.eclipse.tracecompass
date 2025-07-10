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
package org.eclipse.tracecompass.tmf.core.model.genericxy;

import org.eclipse.tracecompass.tmf.core.model.tree.AbstractTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
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
import org.eclipse.tracecompass.tmf.core.model.ISampling;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel.DisplayType;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLog;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLogBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Abstract base class for tree-based data providers support xy chart with
 * non-time x-axis visualizations. Default visualization type is set to bars.
 * <p>
 * This class extends {@link AbstractTreeDataProvider} to handle hierarchical
 * tree data, while also implementing {@link ITmfGenericXYDataProvider} to
 * supply generic xy chart data for views. It is meant to be sub-classed by
 * concrete providers that use generic xy chart representations.
 * </p>
 *
 * @param <A>
 *            The type of analysis module used, which must extend
 *            {@link TmfStateSystemAnalysisModule}
 * @param <M>
 *            The type of tree data model, which must implement
 *            {@link ITmfTreeDataModel}
 * @author Siwei Zhang
 * @since 10.1
 */
public abstract class AbstractTreeGenericXYCommonXDataProvider<A extends TmfStateSystemAnalysisModule, M extends ITmfTreeDataModel>
    extends AbstractTreeDataProvider<A, M> implements ITmfTreeXYDataProvider<M> {

    /**
     * Constructor
     *
     * @param trace
     *            The trace associated with this data provider
     * @param analysisModule
     *            The analysis module used to compute data
     */
    public AbstractTreeGenericXYCommonXDataProvider(ITmfTrace trace, A analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public TmfModelResponse<ITmfXyModel> fetchXY(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        A module = getAnalysisModule();

        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQueryWithSamples(fetchParameters);
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

        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "AbstractTreeGenericXYCommonXDataProvider#fetchXY") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {
            Pair<ISampling, Collection<IYModel>> xAxisAndYSeriesModels = getXAxisAndYSeriesModels(ss, fetchParameters, monitor);
            if (xAxisAndYSeriesModels == null) {
                // getModels returns null if the query was cancelled.
                return TmfXyResponseFactory.createCancelledResponse(CommonStatusMessage.TASK_CANCELLED);
            }
            return TmfXyResponseFactory.create(
                    getTitle(),
                    xAxisAndYSeriesModels.getFirst(),
                    ImmutableList.copyOf(xAxisAndYSeriesModels.getSecond()),
                    getDisplayType(),
                    getXAxisDescription(),
                    complete);
        } catch (StateSystemDisposedException | TimeRangeException | IndexOutOfBoundsException e) {
            return TmfXyResponseFactory.createFailedResponse(String.valueOf(e.getMessage()));
        }
    }

    /**
     * Get the display type for series.
     *
     * @return Description for series
     */
    protected DisplayType getDisplayType() {
        return DisplayType.BAR;
    }

    /**
     * Get description for x axis.
     *
     * @return Description for x axis
     */
    protected abstract TmfXYAxisDescription getXAxisDescription();

    /**
     * Abstract method to be implemented by the providers to return the x axis
     * and height of bars. The child class should check the validity of values
     * inside query parameters.
     *
     * @param ss
     *            the {@link TmfStateSystemAnalysisModule}'s
     *            {@link ITmfStateSystem}
     * @param fetchParameters
     *            the query's filter
     * @param monitor
     *            progress monitor
     * @return a pair, the first element is x values, and the second element is
     *         Y models; null if the query was cancelled
     * @throws StateSystemDisposedException
     *             if the state system was closed during the query or could not
     *             be queried.
     */
    protected abstract @Nullable Pair<ISampling, Collection<IYModel>> getXAxisAndYSeriesModels(ITmfStateSystem ss,
            Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor)
                    throws StateSystemDisposedException;

    /**
     * Getter for the title of this provider
     *
     * @return this provider's title
     */
    protected abstract String getTitle();
}
