/*******************************************************************************
 * Copyright (c) 2022, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdgeContextState;
import org.eclipse.tracecompass.analysis.graph.core.graph.TmfEdgeState;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.Messages;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.TmfEdge;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;

import com.google.common.collect.ImmutableMap;

/**
 * Edge Context State implementation that describes the different states that we can have in the linux OS.
 *
 * This class describes the styles, the mapping between the context states and the edge states (PASS or BLOCK).
 * It also replaces the deprecated EdgeType enum and does the mapping between the deprecated value and the new one.
 *
 * @author Arnaud Fiorini
 */
public class OSEdgeContextState implements ITmfEdgeContextState {

    OSEdgeContextEnum fContextState = OSEdgeContextEnum.DEFAULT;
    private static EnumMap<OSEdgeContextEnum, Map<String, Object>> fStyles;
    static {
        fStyles = new EnumMap<>(OSEdgeContextEnum.class);
        fStyles.put(OSEdgeContextEnum.NO_EDGE,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Unknown),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x40, 0x3b, 0x33),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.EPS,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Unknown),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x40, 0x3b, 0x33),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.UNKNOWN,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Unknown),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x40, 0x3b, 0x33),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.DEFAULT,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Unknown),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x40, 0x3b, 0x33),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.RUNNING,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Running),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x33, 0x99, 0x00),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.BLOCKED,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Blocked),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(220, 20, 60),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.INTERRUPTED,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Interrupted),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0xff, 0xdc, 0x00),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.PREEMPTED,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Preempted),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0xc8, 0x64, 0x00),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.TIMER,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Timer),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x33, 0x66, 0x99),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.NETWORK,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_Network),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0xff, 0x9b, 0xff),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.USER_INPUT,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_UserInput),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x5a, 0x01, 0x01),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.BLOCK_DEVICE,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_BlockDevice),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x66, 0x00, 0xcc),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
        fStyles.put(OSEdgeContextEnum.IPI,
                ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.TmfEdge_IPI),
                        StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(0x66, 0x66, 0xcc),
                        StyleProperties.HEIGHT, 1.0f,
                        StyleProperties.OPACITY, 1.0f,
                        StyleProperties.STYLE_GROUP, String.valueOf(Messages.TmfEdge_GroupBlocked)));
    }

    /**
     * Enum that replaces the previously known {link TmfEdge.EdgeType} enum.
     * @author Arnaud Fiorini
     */
    public enum OSEdgeContextEnum {

        /** Special type of edge meaning there is no edge, to differentiate with null edges at the beginning and end of workers */
        NO_EDGE(0),
        /** Special edge, so it is possible to have two vertices at the same timestamp. */
        EPS(1),
        /** Unknown edge */
        UNKNOWN(2),
        /** Default type for an edge */
        DEFAULT(3),
        /** Worker is running */
        RUNNING(4),
        /** Worker is blocked */
        BLOCKED(5),
        /** Worker is in an interrupt state */
        INTERRUPTED(6),
        /** Worker is preempted */
        PREEMPTED(7),
        /** In a timer */
        TIMER(8),
        /** Edge represents a network communication */
        NETWORK(9),
        /** Worker is waiting for user input */
        USER_INPUT(10),
        /** Block device */
        BLOCK_DEVICE(11),
        /** inter-processor interrupt */
        IPI(12);

        private int code;
        private static Map<Integer, OSEdgeContextEnum> fMap;
        static {
            fMap = new HashMap<>();
            fMap.put(0, NO_EDGE);
            fMap.put(1, EPS);
            fMap.put(2, UNKNOWN);
            fMap.put(3, DEFAULT);
            fMap.put(4, RUNNING);
            fMap.put(5, BLOCKED);
            fMap.put(6, INTERRUPTED);
            fMap.put(7, PREEMPTED);
            fMap.put(8, TIMER);
            fMap.put(9, NETWORK);
            fMap.put(10, USER_INPUT);
            fMap.put(11, BLOCK_DEVICE);
            fMap.put(12, IPI);
        }

        OSEdgeContextEnum(int code) {
            this.code = code;
        }

        /**
         * Used to deserialize this enum when storing it to disk.
         * @param code integer representation of the enum.
         * @return the enum value corresponding to the code.
         */
        public static OSEdgeContextEnum fromValue(int code) {
            return fMap.getOrDefault(code, UNKNOWN);
        }

        /**
         * Used to serialize this enum when storing it to disk.
         * @return the integer representation of the enum.
         */
        public int serialize() {
            return this.code;
        }
    }

    /**
     * Constructor from the enum value.
     * @param contextStateEnum enum context state
     */
    public OSEdgeContextState(OSEdgeContextEnum contextStateEnum) {
        fContextState = contextStateEnum;
    }

    /**
     * Constructor from the int representation of the enum values
     * @param code integer representation of the enum.
     */
    public OSEdgeContextState(int code) {
        fContextState = (OSEdgeContextEnum) deserialize(code);
    }

    /**
     * Legacy constructor to instantiate the new object with the old enum.
     * @param type old edge type
     */
    public OSEdgeContextState(TmfEdge.EdgeType type) {
        switch (type) {
        case BLOCKED:
            fContextState = OSEdgeContextEnum.BLOCKED;
            break;
        case BLOCK_DEVICE:
            fContextState = OSEdgeContextEnum.BLOCK_DEVICE;
            break;
        case EPS:
            fContextState = OSEdgeContextEnum.EPS;
            break;
        case DEFAULT:
            fContextState = OSEdgeContextEnum.DEFAULT;
            break;
        case INTERRUPTED:
            fContextState = OSEdgeContextEnum.INTERRUPTED;
            break;
        case IPI:
            fContextState = OSEdgeContextEnum.IPI;
            break;
        case NETWORK:
            fContextState = OSEdgeContextEnum.NETWORK;
            break;
        case PREEMPTED:
            fContextState = OSEdgeContextEnum.PREEMPTED;
            break;
        case RUNNING:
            fContextState = OSEdgeContextEnum.RUNNING;
            break;
        case TIMER:
            fContextState = OSEdgeContextEnum.TIMER;
            break;
        case USER_INPUT:
            fContextState = OSEdgeContextEnum.USER_INPUT;
            break;
        case UNKNOWN:
        default:
            fContextState = OSEdgeContextEnum.UNKNOWN;
            break;
        }
    }

    /**
     * Getter that returns the old edge type from the context state enum
     * @return the old edge type from {@link TmfEdge}
     */
    public TmfEdge.EdgeType getOldEdgeType() {
        switch (fContextState) {
        case BLOCKED:
            return TmfEdge.EdgeType.BLOCKED;
        case BLOCK_DEVICE:
            return TmfEdge.EdgeType.BLOCK_DEVICE;
        case DEFAULT:
            return TmfEdge.EdgeType.DEFAULT;
        case EPS:
            return TmfEdge.EdgeType.EPS;
        case INTERRUPTED:
            return TmfEdge.EdgeType.INTERRUPTED;
        case IPI:
            return TmfEdge.EdgeType.IPI;
        case NETWORK:
            return TmfEdge.EdgeType.NETWORK;
        case NO_EDGE:
            return TmfEdge.EdgeType.DEFAULT;
        case PREEMPTED:
            return TmfEdge.EdgeType.PREEMPTED;
        case RUNNING:
            return TmfEdge.EdgeType.RUNNING;
        case TIMER:
            return TmfEdge.EdgeType.TIMER;
        case USER_INPUT:
            return TmfEdge.EdgeType.USER_INPUT;
        case UNKNOWN:
        default:
            return TmfEdge.EdgeType.UNKNOWN;
        }
    }

    @Override
    public TmfEdgeState getEdgeState() {
        switch (fContextState) {
        case IPI:
        case USER_INPUT:
        case BLOCK_DEVICE:
        case TIMER:
        case INTERRUPTED:
        case PREEMPTED:
        case RUNNING:
        case UNKNOWN:
        case NO_EDGE:
            return TmfEdgeState.PASS;
        case NETWORK:
        case BLOCKED:
            return TmfEdgeState.BLOCK;
        case EPS:
        case DEFAULT:
        default:
            return TmfEdgeState.UNKNOWN;
        }
    }
    @Override
    public boolean isMatchable() {
        return fContextState == OSEdgeContextEnum.NETWORK ? true : false;
    }

    @Override
    public Map<String, Object> getStyles() {
        return fStyles.getOrDefault(fContextState, fStyles.get(OSEdgeContextEnum.DEFAULT));
    }

    /**
     * Static implementation of getStyles for the enum values directly.
     *
     * It is mainly useful to implement style palettes.
     * @param contextState enum value of the context state.
     * @return the style for the enum value
     */
    public static Map<String, Object> getStyles(OSEdgeContextEnum contextState) {
        return fStyles.getOrDefault(contextState, fStyles.get(OSEdgeContextEnum.DEFAULT));
    }

    @Override
    public void setContextEnum(Enum<?> contextState) {
        fContextState = (OSEdgeContextEnum) contextState;
    }

    @Override
    public Enum<?> getContextEnum() {
        return fContextState;
    }

    @Override
    public int serialize() {
        return fContextState.ordinal();
    }

    @Override
    public Enum<?> deserialize(int code) {
        return OSEdgeContextEnum.fromValue(code);
    }

}
