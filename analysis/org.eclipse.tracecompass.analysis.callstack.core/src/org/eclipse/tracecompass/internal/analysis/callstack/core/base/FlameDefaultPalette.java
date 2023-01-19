/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.presentation.RotatingPaletteProvider;

import com.google.common.collect.ImmutableMap;

/**
 * Class to manage the colors of the flame chart and flame graph views
 *
 * @author Geneviève Bastien
 */
public final class FlameDefaultPalette implements IDataPalette {

    /**
     * The state index for the multiple state
     */
    private static final int NUM_COLORS = 360;
    private static final String DEFAULT_STYLE = "0"; //$NON-NLS-1$

    private static final Map<String, OutputElementStyle> STYLES;
    // Map of styles with the parent
    private static final Map<String, OutputElementStyle> STYLE_MAP = Collections.synchronizedMap(new HashMap<>());

    static {
        IPaletteProvider palette = new RotatingPaletteProvider.Builder().setNbColors(NUM_COLORS).build();
        int i = 0;
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        for (RGBAColor color : palette.get()) {
            builder.put(String.valueOf(i), new OutputElementStyle(null, ImmutableMap.of(
                    StyleProperties.STYLE_NAME, String.valueOf(i),
                    StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(color.getRed(), color.getGreen(), color.getBlue()),
                    StyleProperties.OPACITY, (float) color.getAlpha() / 255)));
            i++;
        }
        STYLES = builder.build();
    }

    private static @Nullable FlameDefaultPalette fInstance = null;

    private FlameDefaultPalette() {
        // Do nothing
    }

    /**
     * Get the instance of this palette
     *
     * @return The instance of the palette
     */
    public static FlameDefaultPalette getInstance() {
        FlameDefaultPalette instance = fInstance;
        if (instance == null) {
            instance = new FlameDefaultPalette();
            fInstance = instance;
        }
        return instance;
    }

    /**
     * Get the map of styles for this palette
     *
     * @return The styles
     */
    @Override
    public Map<String, OutputElementStyle> getStyles() {
        return STYLES;
    }

    /**
     * Get the style element for a given value
     *
     * @param callsite
     *            The value to get an element for
     * @return The output style
     */
    @Override
    public OutputElementStyle getStyleFor(Object callsite) {
        if (callsite instanceof AggregatedCallSite) {
            ICallStackSymbol value = ((AggregatedCallSite) callsite).getObject();
            int hashCode = value.hashCode();
            return STYLE_MAP.computeIfAbsent(String.valueOf(Math.floorMod(hashCode, NUM_COLORS)), style -> new OutputElementStyle(style));
        }
        if (callsite instanceof ICalledFunction) {
            Object value = ((ICalledFunction) callsite).getSymbol();
            int hashCode = value.hashCode();
            return STYLE_MAP.computeIfAbsent(String.valueOf(Math.floorMod(hashCode, NUM_COLORS)), style -> new OutputElementStyle(style));
        }
        if (callsite instanceof TimeGraphState) {
            OutputElementStyle elStyle = ((TimeGraphState) callsite).getStyle();
            return elStyle == null ? STYLE_MAP.computeIfAbsent(DEFAULT_STYLE, style -> new OutputElementStyle(style)) : elStyle;
        }
        return STYLE_MAP.computeIfAbsent(DEFAULT_STYLE, style -> new OutputElementStyle(style));
    }
}
