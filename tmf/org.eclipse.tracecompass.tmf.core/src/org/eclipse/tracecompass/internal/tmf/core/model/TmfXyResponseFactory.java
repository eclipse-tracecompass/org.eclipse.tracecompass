/**********************************************************************
 * Copyright (c) 2017, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.tmf.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.ISampling;
import org.eclipse.tracecompass.tmf.core.model.SeriesModel.SeriesModelBuilder;
import org.eclipse.tracecompass.tmf.core.model.TmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel.DisplayType;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;

import com.google.common.collect.Lists;

/**
 * This class creates instance of {@link TmfModelResponse}
 *
 * @author Yonni Chen
 * @since 4.0
 */
public final class TmfXyResponseFactory {

    /**
     * Constructor
     */
    private TmfXyResponseFactory() {

    }

    /**
     * Create a {@link TmfModelResponse} for values with a common X axis values,
     * with a either RUNNING or COMPLETED status. Model is not null, it's either
     * partial or full.
     *
     * @param title
     *            Chart title
     * @param xValues
     *            The x values requested by the viewer
     * @param yModels
     *            Collection of IYModel
     * @param isComplete
     *            Tells whether the computed model is complete or partial
     * @return A {@link TmfModelResponse} with either a running status or a
     *         completed status
     * @deprecated Use {@link ISampling} for xValues instead.
     */
    @Deprecated(since = "10.2", forRemoval = true)
    public static TmfModelResponse<ITmfXyModel> create(String title, long[] xValues, Collection<IYModel> yModels, boolean isComplete) {
        List<ISeriesModel> series = Lists.transform(new ArrayList<>(yModels), model -> {
            SeriesModelBuilder builder = new SeriesModelBuilder(model.getId(), model.getName(), xValues, model.getData());
            TmfXYAxisDescription yAxis = model.getYAxisDescription();
            if (yAxis != null) {
                builder.yAxisDescription(yAxis);
            }
            return builder.build();
        });
        ITmfXyModel model = new TmfXyModel(title, series);

        if (isComplete) {
            return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, Objects.requireNonNull(CommonStatusMessage.COMPLETED));
        }
        return new TmfModelResponse<>(model, ITmfResponse.Status.RUNNING, Objects.requireNonNull(CommonStatusMessage.RUNNING));
    }

    /**
     * Create a {@link TmfModelResponse} for values with common sampling values,
     * with a either RUNNING or COMPLETED status. Model is not null, it's either
     * partial or full.
     *
     * @param title
     *            Chart title
     * @param samples
     *            The samples requested by the viewer
     * @param yModels
     *            Collection of IYModel
     * @param isComplete
     *            Tells whether the computed model is complete or partial
     * @return A {@link TmfModelResponse} with either a running status or a
     *         completed status
     */
    public static TmfModelResponse<ITmfXyModel> create(String title, ISampling samples, Collection<IYModel> yModels, boolean isComplete) {
        List<ISeriesModel> series = Lists.transform(new ArrayList<>(yModels), model -> {
            SeriesModelBuilder builder = new SeriesModelBuilder(model.getId(), model.getName(), samples, model.getData());
            TmfXYAxisDescription yAxis = model.getYAxisDescription();
            if (yAxis != null) {
                builder.yAxisDescription(yAxis);
            }
            return builder.build();
        });
        ITmfXyModel model = new TmfXyModel(title, series);

        if (isComplete) {
            return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, Objects.requireNonNull(CommonStatusMessage.COMPLETED));
        }
        return new TmfModelResponse<>(model, ITmfResponse.Status.RUNNING, Objects.requireNonNull(CommonStatusMessage.RUNNING));
    }

    /**
     * Create a {@link TmfModelResponse} for values with common sampling values,
     * with a either RUNNING or COMPLETED status. Model is not null, it's either
     * partial or full.
     *
     * @param title
     *            Chart title
     * @param samples
     *            The samples requested by the viewer
     * @param yModels
     *            Collection of IYModel
     * @param displayType
     *            The type of display, see {@link DisplayType}
     * @param isComplete
     *            Tells whether the computed model is complete or partial
     * @return A {@link TmfModelResponse} with either a running status or a
     *         completed status
     */
    public static TmfModelResponse<ITmfXyModel> create(String title, ISampling samples, Collection<IYModel> yModels, DisplayType displayType, boolean isComplete) {
        List<ISeriesModel> series = Lists.transform(new ArrayList<>(yModels), model -> {
            SeriesModelBuilder builder = new SeriesModelBuilder(model.getId(), model.getName(), samples, model.getData());
            TmfXYAxisDescription yAxis = model.getYAxisDescription();
            if (yAxis != null) {
                builder.yAxisDescription(yAxis);
            }
            builder.seriesDisplayType(displayType);
            return builder.build();
        });
        ITmfXyModel model = new TmfXyModel(title, series);

        if (isComplete) {
            return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, Objects.requireNonNull(CommonStatusMessage.COMPLETED));
        }
        return new TmfModelResponse<>(model, ITmfResponse.Status.RUNNING, Objects.requireNonNull(CommonStatusMessage.RUNNING));
    }

    /**
     * Create a {@link TmfModelResponse} for values with common sampling values
     * and x axis descriptions, with a either RUNNING or COMPLETED status. Model
     * is not null, it's either partial or full.
     *
     * @param title
     *            Chart title
     * @param samples
     *            The samples requested by the viewer
     * @param yModels
     *            Collection of IYModel
     * @param displayType
     *            The type of display, see {@link DisplayType}
     * @param xAxisDescription
     *            The description for x axis
     * @param isComplete
     *            Tells whether the computed model is complete or partial
     * @return A {@link TmfModelResponse} with either a running status or a
     *         completed status
     */
    public static TmfModelResponse<ITmfXyModel> create(String title, ISampling samples, Collection<IYModel> yModels, DisplayType displayType, TmfXYAxisDescription xAxisDescription, boolean isComplete) {
        List<ISeriesModel> series = Lists.transform(new ArrayList<>(yModels), model -> {
            SeriesModelBuilder builder = new SeriesModelBuilder(model.getId(), model.getName(), samples, model.getData());
            builder.xAxisDescription(xAxisDescription);
            TmfXYAxisDescription yAxis = model.getYAxisDescription();
            if (yAxis != null) {
                builder.yAxisDescription(yAxis);
            }
            builder.seriesDisplayType(displayType);
            return builder.build();
        });
        ITmfXyModel model = new TmfXyModel(title, series);

        if (isComplete) {
            return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, Objects.requireNonNull(CommonStatusMessage.COMPLETED));
        }
        return new TmfModelResponse<>(model, ITmfResponse.Status.RUNNING, Objects.requireNonNull(CommonStatusMessage.RUNNING));
    }

    /**
     * Create a {@link TmfModelResponse} with a either RUNNING or COMPLETED status.
     * Model is not null, it's either partial or full.
     *
     * @param title
     *            Chart title
     * @param yModels
     *            Collection of IYModel
     * @param isComplete
     *            Tells whether the computed model is complete or partial
     * @return A {@link TmfModelResponse} with either a running status or a
     *         completed status
     */
    public static TmfModelResponse<ITmfXyModel> create(String title, List<ISeriesModel> yModels, boolean isComplete) {
        ITmfXyModel model = new TmfXyModel(title, yModels);

        if (isComplete) {
            return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, Objects.requireNonNull(CommonStatusMessage.COMPLETED));
        }
        return new TmfModelResponse<>(model, ITmfResponse.Status.RUNNING, Objects.requireNonNull(CommonStatusMessage.RUNNING));
    }

    /**
     * Create a {@link TmfModelResponse} with a FAILED status. Model inside of
     * returned response is null.
     *
     * @param message
     *            A detailed message of why the response has a failed status
     * @return A {@link TmfModelResponse} with a failed status and null model
     */
    public static TmfModelResponse<ITmfXyModel> createFailedResponse(String message) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, message);
    }

    /**
     * Create a {@link TmfModelResponse} with a CANCELLED status. Model inside of
     * returned response is null.
     *
     * @param message
     *            A detailed message of why the response has a cancelled status
     * @return A {@link TmfModelResponse} with a cancelled status and null model
     */
    public static TmfModelResponse<ITmfXyModel> createCancelledResponse(String message) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, message);
    }

    /**
     * Create a {@link TmfModelResponse} with a COMPLETED status but empty model.
     *
     * @param message
     *            A detailed message of why the response is empty
     * @return A {@link TmfModelResponse} with a COMPLETED status and empty model
     */
    public static TmfModelResponse<ITmfXyModel> createEmptyResponse(String message) {
        ITmfXyModel model = new TmfXyModel(StringUtils.EMPTY, Collections.emptyList());

        return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, message);
    }
}
