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

    protected boolean isTooltipAvailable() {
        return getChartViewer().getWindowDuration() != 0;
    }

    protected int getHoveredIndex(List<IXYSeries> series, double xCoordinate) {
        if (series.isEmpty()) {
            return -1;
        }
        double[] xSeries = series.get(0).getXSeries();
        if ((xSeries == null) || (xSeries.length == 0)) {
            return -1;
        }
        int index = Arrays.binarySearch(xSeries, xCoordinate);
        index = (index >= 0) ? index : -index - 1;
        return (index < xSeries.length) ? index : -1;
    }

    protected boolean isValidSeriesIndex(IXYSeries series, int index) {
        double[] ySeries = series.getYSeries();
        return series.isVisible() && ySeries != null && index >= 0 && index < ySeries.length;
    }

    protected void addAdditionalTooltipItems(double xCoordinate, String seriesKey) {
        ITmfTimestamp time = TmfTimestamp.fromNanos((long) xCoordinate + getChartViewer().getTimeOffset());
        addItem(null,
                ToolTipString.fromString(Messages.TmfCommonXLineChartTooltipProvider_time),
                ToolTipString.fromTimestamp(time.toString(), time.toNanos()));
    }

    protected void addSeriesTooltipItem(IXYSeries series, int index, Format format) {
        double[] ySeries = series.getYSeries();
        if (ySeries == null || index < 0 || index >= ySeries.length) {
            return;
        }

        String label = formatSeriesLabel(series);
        double yValue = ySeries[index];
        if (format == null) {
            addItem(null, ToolTipString.fromHtml(label), ToolTipString.fromDecimal(yValue));
        } else {
            addItem(null, ToolTipString.fromHtml(label), ToolTipString.fromString(format.format(yValue)));
        }
    }

    protected String formatSeriesLabel(IXYSeries series) {
        String key = series.getId();
        String label = (key == null) ? "" : key; //$NON-NLS-1$
        Color color = series.getColor();
        if (color != null) {
            RGBA rgba = color.getRGBA();
            RGBAColor rgbaColor = new RGBAColor(rgba.rgb.red, rgba.rgb.green, rgba.rgb.blue, rgba.alpha);
            label = String.format(TmfCommonXLineChartTooltipProvider.HTML_COLOR_TOOLTIP, rgbaColor, label);
        }
        return label;
    }

    private final class CommonToolTipHandler extends TmfAbstractToolTipHandler {

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

            String firstValidSeriesKey = null;
            for (IXYSeries xySeries : series) {
                if (isValidSeriesIndex(xySeries, index)) {
                    firstValidSeriesKey = xySeries.getId();
                    break;
                }
            }
            addAdditionalTooltipItems(xCoordinate, firstValidSeriesKey);

            for (IXYSeries xySeries : series) {
                if (!isValidSeriesIndex(xySeries, index)) {
                    continue;
                }
                addSeriesTooltipItem(xySeries, index, format);
            }
        }
    }
}
