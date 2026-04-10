/*******************************************************************************
 * Copyright (c) 2014, 2020 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart;

import java.text.Format;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.List;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfAbstractToolTipHandler;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfAbstractToolTipHandler.ToolTipString;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.IAxis;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.ITmfChartTimeProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.IXYSeries;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfBaseProvider;

/**
 * Displays a tooltip on line charts. For each series, it shows the y value at
 * the selected x value. This tooltip assumes that all series share a common set
 * of X axis values. If the X series is not common, the tooltip text may not be
 * accurate.
 *
 * @since 6.0
 */
public class TmfCommonXLineChartTooltipProvider extends TmfBaseProvider {

    private static final String HTML_COLOR_TOOLTIP = "<span style=\"color:%s;\">%s</span>"; //$NON-NLS-1$
    private final CommonToolTipHandler fToolTipHandler = new CommonToolTipHandler();

    /**
     * Constructor for the tooltip provider
     *
     * @param tmfChartViewer
     *            The parent chart viewer
     */
    public TmfCommonXLineChartTooltipProvider(ITmfChartTimeProvider tmfChartViewer) {
        super(tmfChartViewer);
        register();
    }

    // ------------------------------------------------------------------------
    // TmfBaseProvider
    // ------------------------------------------------------------------------

    @Override
    public TmfAbstractToolTipHandler getTooltipHandler() {
        return fToolTipHandler;
    }

    @Override
    public void refresh() {
        // nothing to do
    }

    /**
     * Adds tooltip items that are not tied to a particular series (for example, the
     * timestamp corresponding to the hovered X coordinate).
     * <p>
     * This method is part of the provider API and is designed to be overridden by
     * subclasses that want to contribute additional context to the tooltip when the
     * user hovers the chart.
     * </p>
     * <p>
     * The default implementation adds a single "time" item computed from
     * {@code xCoordinate} and the chart viewer's time offset.
     * </p>
     *
     * @param adder
     *            Callback used to append items to the tooltip.
     * @param xCoordinate
     *            The hovered X coordinate in data space (i.e., in the same domain as
     *            the series X values, typically nanoseconds relative to the viewer's
     *            time offset).
     * @since 9.2
     */
    protected void addAdditionalTooltipItems(BiConsumer<ToolTipString, ToolTipString> adder, double xCoordinate) {
        long timeNanos = Math.round(xCoordinate) + getChartViewer().getTimeOffset();
        ITmfTimestamp time = TmfTimestamp.fromNanos(timeNanos);
        adder.accept(
                ToolTipString.fromString(Messages.TmfCommonXLineChartTooltipProvider_time),
                ToolTipString.fromTimestamp(time.toString(), time.toNanos()));
    }

    /**
     * Adds a tooltip item for a given series at the specified hovered index.
     * <p>
     * This method is part of the provider API and may be overridden to customize
     * how a series is rendered in the tooltip (e.g., formatting, units, hiding
     * specific series, etc.).
     * </p>
     * <p>
     * The default implementation:
     * <ul>
     *   <li>computes a label from {@link #formatSeriesLabel(IXYSeries)} (including
     *       the series color when available)</li>
     *   <li>reads the Y value at {@code index}</li>
     *   <li>formats it using {@code format} when non-null, otherwise uses a default
     *       decimal representation</li>
     * </ul>
     * </p>
     *
     * @param adder
     *            Callback used to append the key/value pair to the tooltip.
     * @param xySeries
     *            The series for which to add a tooltip entry.
     * @param index
     *            Hovered point index within the series arrays.
     * @param format
     *            Optional numeric formatter (typically inherited from the chart Y axis tick
     *            formatter). If {@code null}, a default decimal formatting is used.
     * @since 9.2
     */
    protected void addSeriesTooltipItem(BiConsumer<ToolTipString, ToolTipString> adder, IXYSeries xySeries, int index, Format format) {
        double[] ySeries = xySeries.getYSeries();
        if (ySeries == null || index < 0 || index >= ySeries.length) {
            return;
        }

        String label = formatSeriesLabel(xySeries);
        double yValue = ySeries[index];
        if (format == null) {
            adder.accept(
                    ToolTipString.fromHtml(label),
                    ToolTipString.fromDecimal(yValue));
        } else {
            adder.accept(
                    ToolTipString.fromHtml(label),
                    ToolTipString.fromString(format.format(yValue)));
        }
    }

    /**
     * Builds the display label for a series when shown in the tooltip.
     * <p>
     * The default implementation uses the series id and, when the series color is
     * available, wraps the label in an HTML span using that color so the tooltip
     * visually matches the series.
     * </p>
     *
     * @param xySeries
     *            The series to format.
     * @return The formatted label (potentially containing HTML).
     * @since 9.2
     */
    protected static final String formatSeriesLabel(IXYSeries xySeries) {
        String label = xySeries.getId();
        if (label == null) {
            label = ""; //$NON-NLS-1$
        }

        Color color = xySeries.getColor();
        if (color != null) {
            RGBA rgba = color.getRGBA();
            RGBAColor rgbaColor = new RGBAColor(rgba.rgb.red, rgba.rgb.green, rgba.rgb.blue, rgba.alpha);
            label = String.format(TmfCommonXLineChartTooltipProvider.HTML_COLOR_TOOLTIP, rgbaColor, label);
        }

        return label;
    }

    /**
     * Returns the id (key) of the first series that contributed an entry to the most
     * recently built tooltip.
     * <p>
     * This value is set when the tooltip is filled and can be used by subclasses to
     * correlate additional tooltip content with the first visible/valid series at the
     * hovered index.
     * </p>
     *
     * @return The id of the first valid series used for the tooltip, or {@code null}
     *         if the tooltip has not been computed yet or no valid series was found.
     * @since 9.2
     */
    protected final String getFirstValidSeriesKey() {
        return fToolTipHandler.firstValidSeriesKey;
    }

    // ======================================================================
    // TOOLTIP HANDLER
    // ======================================================================

    private final class CommonToolTipHandler extends TmfAbstractToolTipHandler {

        private String firstValidSeriesKey;

        private CommonToolTipHandler() {
            firstValidSeriesKey = null;
        }

        @Override
        public void fill(Control control, MouseEvent event, Point pt) {
            if (!isTooltipAvailable()) {
                return;
            }

            IAxis xAxis = getXAxis();
            double xCoordinate = xAxis.getDataCoordinate(pt.x);
            if (xCoordinate < 0) {
                return;
            }

            List<IXYSeries> series = getSeries();
            int index = getHoveredIndex(series, xCoordinate);
            if (index < 0) {
                return;
            }

            Format format = null;
            if (getChartViewer() instanceof TmfCommonXAxisChartViewer chartViewer) {
                format = chartViewer.getSwtChart().getAxisSet().getYAxes()[0].getTick().getFormat();
            }

            boolean firstValid = true;
            for (IXYSeries xySeries : series) {
                if (!isValidSeriesIndex(xySeries, index)) {
                    continue;
                }

                if (firstValid) {
                    firstValid = false;
                    firstValidSeriesKey = xySeries.getId();
                    addAdditionalTooltipItems((key, value) -> addItem(null, key, value), xCoordinate);
                }

                addSeriesTooltipItem((key, value) -> addItem(null, key, value), xySeries, index, format);
            }
        }

        private boolean isTooltipAvailable() {
            return getChartViewer().getWindowDuration() != 0;
        }

        private int getHoveredIndex(List<IXYSeries> series, double xCoordinate) {
            if (series.isEmpty()) {
                return -1;
            }

            double[] xSeries = series.get(0).getXSeries();
            if (xSeries == null || xSeries.length == 0) {
                return -1;
            }

            int index = Arrays.binarySearch(xSeries, xCoordinate);
            if (index < 0) {
                index = -index - 1;
                index = Math.max(0, index - 1);
            }

            return index < xSeries.length ? index : -1;
        }

        private boolean isValidSeriesIndex(IXYSeries series, int index) {
            double[] ySeries = series.getYSeries();
            return series.isVisible()
                    && ySeries != null
                    && index >= 0
                    && index < ySeries.length;
        }
    }
}
