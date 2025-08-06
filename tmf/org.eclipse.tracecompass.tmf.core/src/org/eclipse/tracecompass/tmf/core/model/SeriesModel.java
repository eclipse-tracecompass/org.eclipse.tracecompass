/*******************************************************************************
 * Copyright (c) 2017, 2025 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.model;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;

import com.google.gson.annotations.SerializedName;

/**
 * This is a basic implementation of {@link ISeriesModel}
 *
 * @author Geneviève Bastien
 * @since 4.0
 */
public class SeriesModel implements ISeriesModel {

    /**
     * Default name for X axis
     */
    private static final String DEFAULT_XAXIS_NAME = "X Axis"; //$NON-NLS-1$

    /**
     * Default unit type for X axis
     */
    private static final String DEFAULT_XAXIS_UNIT = ""; //$NON-NLS-1$

    /**
     * Default name for Y axis
     */
    private static final String DEFAULT_YAXIS_NAME = "Y Axis"; //$NON-NLS-1$

    /**
     * Default unit type for y axis
     */
    private static final String DEFAULT_YAXIS_UNIT = ""; //$NON-NLS-1$

    /**
     * transient to avoid serializing for tests, as IDs may not be the same from one
     * run to the other, due to how they are generated.
     */
    @SerializedName("id")
    private final transient long fId;

    @SerializedName("name")
    private final String fName;

    @SerializedName("xValues")
    private final ISampling fSampling;

    @SerializedName("yValues")
    private final double[] fYValues;

    @SerializedName("xValuesDescription")
    private final TmfXYAxisDescription fXAxis;

    @SerializedName("yValuesDescription")
    private final TmfXYAxisDescription fYAxis;

    @SerializedName("dataType")
    private final DisplayType fDisplayType;

    @SerializedName("properties")
    private final int[] fProperties;

    /**
     * Constructor
     *
     * @param id
     *            The unique ID of the associated entry
     * @param name
     *            The name of the series
     * @param xValues
     *            The x values of this series
     * @param yValues
     *            The y values of this series
     * @since 4.2
     */
    @Deprecated(since = "10.1", forRemoval = true)
    public SeriesModel(long id, String name, long[] xValues, double[] yValues) {
        this(id, name, xValues, yValues, new TmfXYAxisDescription(DEFAULT_XAXIS_NAME, DEFAULT_XAXIS_UNIT), new TmfXYAxisDescription(DEFAULT_YAXIS_NAME, DEFAULT_YAXIS_UNIT), DisplayType.LINE, new int[xValues.length]);
    }

    /**
     * Constructor
     *
     * @param id
     *            The unique ID of the associated entry
     * @param name
     *            The name of the series
     * @param xValues
     *            The x values of this series
     * @param yValues
     *            The y values of this series
     * @since 10.1
     */
    public SeriesModel(long id, String name, ISampling xValues, double[] yValues) {
        this(id, name, xValues, yValues, new TmfXYAxisDescription(DEFAULT_XAXIS_NAME, DEFAULT_XAXIS_UNIT), new TmfXYAxisDescription(DEFAULT_YAXIS_NAME, DEFAULT_YAXIS_UNIT), DisplayType.LINE, new int[xValues.size()]);
    }

    /**
     * Constructor with axis description
     *
     * @param id
     *            The unique ID of the associated entry
     * @param name
     *            The name of the series
     * @param xValues
     *            The x values of this series
     * @param yValues
     *            The y values of this series
     * @param xAxis
     *            X Axis description
     * @param yAxis
     *            Y Axis description
     * @param displayType
     *            Display type
     * @param properties
     *            The properties values for this series. Some priority values
     *            are available in {@link CoreFilterProperty}
     * @deprecated Use {@link ISampling} for xValues instead.
     */
    @Deprecated(since = "10.1", forRemoval = true)
    private SeriesModel(long id, String name, long[] xValues, double[] yValues, TmfXYAxisDescription xAxis, TmfXYAxisDescription yAxis, DisplayType displayType, int[] properties) {
        fId = id;
        fName = name;
        fSampling = new ISampling.Timestamps(xValues);
        fYValues = yValues;
        fXAxis = xAxis;
        fYAxis = yAxis;
        fDisplayType = displayType;
        fProperties = properties;
    }

    /**
     * Constructor that accepts a {@link ISampling} instead of raw x-values.
     *
     * @param id
     *            The unique ID of the associated entry
     * @param name
     *            The name of the series
     * @param sampling
     *            The X-axis sampling (e.g., timestamps or categories)
     * @param yValues
     *            The Y values of this series
     * @param xAxis
     *            X Axis description
     * @param yAxis
     *            Y Axis description
     * @param displayType
     *            Display type
     * @param properties
     *            The properties values for this series
     * @since 10.1
     */
    public SeriesModel(long id, String name, ISampling sampling, double[] yValues,
            TmfXYAxisDescription xAxis, TmfXYAxisDescription yAxis,
            DisplayType displayType, int[] properties) {
        fId = id;
        fName = name;
        fSampling = sampling;
        fYValues = yValues;
        fXAxis = xAxis;
        fYAxis = yAxis;
        fDisplayType = displayType;
        fProperties = properties;
    }

    @Override
    public long getId() {
        return fId;
    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public TmfXYAxisDescription getXAxisDescription() {
        return fXAxis;
    }

    @Override
    public TmfXYAxisDescription getYAxisDescription() {
        return fYAxis;
    }

    @Override
    public DisplayType getDisplayType() {
        return fDisplayType;
    }

    @Override
    public long[] getXAxis() {
        return  (fSampling instanceof ISampling.Timestamps ts) ? ts.timestamps() : new long[0];
    }

    @Override
    public ISampling getSampling() {
        return fSampling;
    }

    @Override
    public double[] getData() {
        return fYValues;
    }

    @Override
    public int[] getProperties() {
        return fProperties;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SeriesModel other = (SeriesModel) obj;
        return fName.equals(other.getName())
                && fId == other.getId()
                && Objects.equals(fSampling, other.getSampling())
                && Arrays.equals(fYValues, other.getData())
                && fXAxis.equals(other.getXAxisDescription())
                && fYAxis.equals(other.getYAxisDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fId, fName, fSampling, fYValues, fXAxis, fYAxis);
    }

    /**
     * Series model builder
     *
     * @author Simon Delisle
     * @since 5.0
     */
    public static class SeriesModelBuilder {
        private final long id;
        private final String name;
        private final ISampling sampling;
        private final double[] yValues;
        private @Nullable TmfXYAxisDescription xAxis;
        private @Nullable TmfXYAxisDescription yAxis;
        private @Nullable DisplayType displayType;
        private int @Nullable [] properties;

        /**
         * Constructor
         *
         * @param id
         *            The unique ID of the associated entry
         * @param name
         *            The name of the series
         * @param xValues
         *            The x values of this series
         * @param yValues
         *            The y values of this series
         * @deprecated Use {@link ISampling} for xValues instead.
         */
        @Deprecated(since = "10.1", forRemoval = true)
        public SeriesModelBuilder(long id, String name, long[] xValues, double[] yValues) {
            this(id, name, new ISampling.Timestamps(xValues), yValues);
        }

        /**
         * Constructor using {@link ISampling}.
         *
         * @param id
         *            The unique ID of the associated entry
         * @param name
         *            The name of the series
         * @param sampling
         *            The sampling of X values
         * @param yValues
         *            The y values of this series
         * @since 10.1
         */
        public SeriesModelBuilder(long id, String name, ISampling sampling, double[] yValues) {
            this.id = id;
            this.name = name;
            this.sampling = sampling;
            this.yValues = yValues;
        }

        /**
         * Add X axis description
         *
         * @param axis
         *            Axis description
         * @return {@link SeriesModelBuilder}
         */
        public SeriesModelBuilder xAxisDescription(TmfXYAxisDescription axis) {
            this.xAxis = axis;
            return this;
        }

        /**
         * Add Y axis description
         *
         * @param axis
         *            Axis description
         * @return {@link SeriesModelBuilder}
         */
        public SeriesModelBuilder yAxisDescription(TmfXYAxisDescription axis) {
            this.yAxis = axis;
            return this;
        }

        /**
         * Set the display type
         *
         * @param type
         *            Display type
         * @return {@link SeriesModelBuilder}
         */
        public SeriesModelBuilder seriesDisplayType(DisplayType type) {
            this.displayType = type;
            return this;
        }

        /**
         * Set the properties
         *
         * @param properties
         *            Properties
         * @return {@link SeriesModelBuilder}
         */
        public SeriesModelBuilder setProperties(int[] properties) {
            this.properties = properties;
            return this;
        }

        /**
         * Build a {@link SeriesModel}
         *
         * @return {@link SeriesModel}
         */
        public SeriesModel build() {
            return new SeriesModel(id, name, sampling, yValues,
                    xAxis != null ? xAxis : new TmfXYAxisDescription(DEFAULT_XAXIS_NAME, DEFAULT_XAXIS_UNIT),
                    yAxis != null ? yAxis : new TmfXYAxisDescription(DEFAULT_YAXIS_NAME, DEFAULT_YAXIS_UNIT),
                    displayType != null ? displayType : DisplayType.LINE,
                    properties != null ? properties : new int[sampling.size()]);
        }
    }
}
