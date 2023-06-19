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

package org.eclipse.tracecompass.internal.analysis.callstack.core.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.AggregatedThreadStatus;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.StateValues;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.registry.LinuxStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;

import com.google.common.collect.ImmutableMap;

/**
 * A data palette that adds kernel states
 *
 * @author Geneviève Bastien
 */
public class FlameWithKernelPalette implements IDataPalette {

    private static final Map<String, OutputElementStyle> STYLES;
    // Map of styles with the parent
    private static final Map<String, OutputElementStyle> STYLE_MAP = Collections.synchronizedMap(new HashMap<>());

    static {
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        // Put all the default flame styles in the map
        Map<String, OutputElementStyle> defaultStyles = FlameDefaultPalette.getInstance().getStyles();
        builder.putAll(defaultStyles);
        // Add the styles from the kernel
        for (LinuxStyle style : LinuxStyle.values()) {
            builder.put(style.getLabel(), new OutputElementStyle(null, style.toMap()));
        }
        STYLES = builder.build();
    }

    private static @Nullable FlameWithKernelPalette fInstance = null;

    private FlameWithKernelPalette() {
    }

    /**
     * Get the instance of this palette
     *
     * @return The instance of the palette
     */
    public static FlameWithKernelPalette getInstance() {
        FlameWithKernelPalette instance = fInstance;
        if (instance == null) {
            instance = new FlameWithKernelPalette();
            fInstance = instance;
        }
        return instance;
    }

    @Override
    public OutputElementStyle getStyleFor(Object object) {
        if (object instanceof AggregatedThreadStatus) {
            return getElementStyle(((AggregatedThreadStatus) object).getProcessStatus().getStateValue().unboxInt());
        }
        // Ask the default palette for the style
        return FlameDefaultPalette.getInstance().getStyleFor(object);
    }

    private static OutputElementStyle getElementStyle(int stateValue) {
        String styleFor = getStyleFor(stateValue);
        return STYLE_MAP.computeIfAbsent(styleFor, style -> new OutputElementStyle(style));
    }

    /**
     * Gets the thread state label corresponding to the state value integer for
     * threads
     *
     * @param stateValue
     *            Integer corresponding to a thread state in the state system
     * @return the thread state label
     */
    public static String getStyleFor(int stateValue) {
        switch (stateValue) {
        case StateValues.PROCESS_STATUS_UNKNOWN:
            return LinuxStyle.UNKNOWN.getLabel();
        case StateValues.PROCESS_STATUS_RUN_USERMODE:
            return LinuxStyle.USERMODE.getLabel();
        case StateValues.PROCESS_STATUS_RUN_SYSCALL:
            return LinuxStyle.SYSCALL.getLabel();
        case StateValues.PROCESS_STATUS_INTERRUPTED:
            return LinuxStyle.INTERRUPTED.getLabel();
        case StateValues.PROCESS_STATUS_WAIT_BLOCKED:
            return LinuxStyle.WAIT_BLOCKED.getLabel();
        case StateValues.PROCESS_STATUS_WAIT_FOR_CPU:
            return LinuxStyle.WAIT_FOR_CPU.getLabel();
        case StateValues.PROCESS_STATUS_WAIT_UNKNOWN:
            return LinuxStyle.WAIT_UNKNOWN.getLabel();
        default:
            return LinuxStyle.UNKNOWN.getLabel();
        }
    }

    @Override
    public Map<String, OutputElementStyle> getStyles() {
        return STYLES;
    }
}
