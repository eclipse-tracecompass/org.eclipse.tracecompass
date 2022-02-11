/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.base;

import java.util.Map;

import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState.OSEdgeContextEnum;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;

import com.google.common.collect.ImmutableMap;

/**
 * The main color palette for the critical path analysis. When the incubator
 * weighted tree feature is integrated in main Trace Compass, this class should
 * implement IDataPalette.
 *
 * @author Geneviève Bastien
 */
public class OSCriticalPathPalette {

    private static final Map<String, OutputElementStyle> STATE_MAP;

    static {
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        builder.put(OSEdgeContextEnum.RUNNING.name(), new OutputElementStyle(null, OSEdgeContextState.getStyles(OSEdgeContextEnum.RUNNING)));
        builder.put(OSEdgeContextEnum.INTERRUPTED.name(), new OutputElementStyle(null, OSEdgeContextState.getStyles(OSEdgeContextEnum.INTERRUPTED)));
        builder.put(OSEdgeContextEnum.PREEMPTED.name(), new OutputElementStyle(null, OSEdgeContextState.getStyles(OSEdgeContextEnum.PREEMPTED)));
        builder.put(OSEdgeContextEnum.TIMER.name(), new OutputElementStyle(null, OSEdgeContextState.getStyles(OSEdgeContextEnum.TIMER)));
        builder.put(OSEdgeContextEnum.BLOCK_DEVICE.name(), new OutputElementStyle(null, OSEdgeContextState.getStyles(OSEdgeContextEnum.BLOCK_DEVICE)));
        builder.put(OSEdgeContextEnum.NETWORK.name(), new OutputElementStyle(null, OSEdgeContextState.getStyles(OSEdgeContextEnum.NETWORK)));
        builder.put(OSEdgeContextEnum.USER_INPUT.name(), new OutputElementStyle(null, OSEdgeContextState.getStyles(OSEdgeContextEnum.USER_INPUT)));
        builder.put(OSEdgeContextEnum.IPI.name(), new OutputElementStyle(null, OSEdgeContextState.getStyles(OSEdgeContextEnum.IPI)));
        builder.put(OSEdgeContextEnum.BLOCKED.name(), new OutputElementStyle(null, OSEdgeContextState.getStyles(OSEdgeContextEnum.BLOCKED)));
        builder.put(OSEdgeContextEnum.UNKNOWN.name(), new OutputElementStyle(null, OSEdgeContextState.getStyles(OSEdgeContextEnum.UNKNOWN)));
        STATE_MAP = builder.build();
    }

    /**
     * Get the map of all styles provided by this palette. These are the base
     * styles, mapping to the key for each style. Styles for object can then
     * refer to those base styles as parents.
     *
     * @return The map of style name to full style description.
     */
    public static Map<String, OutputElementStyle> getStyles() {
        return STATE_MAP;
    }

}
