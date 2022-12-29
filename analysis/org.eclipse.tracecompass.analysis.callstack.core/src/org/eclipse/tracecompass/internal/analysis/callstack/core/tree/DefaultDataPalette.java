/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.QualitativePaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;

import com.google.common.collect.ImmutableMap;

/**
 * A default data palette that uses the hashCode of an object to get a color
 * palette from a small differentiated palette
 *
 * @author Geneviève Bastien
 */
public class DefaultDataPalette implements IDataPalette {

    private static final int NUM_COLORS = 12;

    // Map of base styles
    private static final Map<String, OutputElementStyle> STYLES;
    // Map of styles with the parent
    private static final Map<String, OutputElementStyle> STYLE_MAP = Collections.synchronizedMap(new HashMap<>());

    static {
        IPaletteProvider palette = new QualitativePaletteProvider.Builder().setNbColors(NUM_COLORS).build();
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

    private static @Nullable DefaultDataPalette fInstance = null;

    private DefaultDataPalette() {
        // Do nothing
    }

    /**
     * Get the instance of this palette
     *
     * @return The instance of the palette
     */
    public static DefaultDataPalette getInstance() {
        DefaultDataPalette instance = fInstance;
        if (instance == null) {
            instance = new DefaultDataPalette();
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
     * Get the style element for a given object
     *
     * @param object
     *            The object to get a style for
     * @return The output style for the object
     */
    @Override
    public OutputElementStyle getStyleFor(Object object) {
        return STYLE_MAP.computeIfAbsent(String.valueOf(Math.floorMod(object.hashCode(), NUM_COLORS)), style -> new OutputElementStyle(style));
    }
}
