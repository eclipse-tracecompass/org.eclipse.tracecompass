/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.graph;

import java.util.Map;

/**
 * @author Arnaud Fiorini
 * @since 4.0
 *
 * Replace the deprecated EdgeType to store the underlying state value of the process
 * that is the represented by the edge. This state has to be mapped to either a blocking
 * or a passing state. This mapping is implemented through this interface.
 *
 */
public interface ITmfEdgeContextState {
    /**
     * Refer to CriticalPathAlgorithm to see how it is used.
     *
     * @return The Edge state which is used to compute the critical path.
     */
    public TmfEdgeState getEdgeState();

    /**
     * These styles describes the states when shown to the user.
     * It is typically used by the data provider or by a palette for the legend.
     *
     * @return The styles used when displaying the critical path to the user.
     */
    public Map<String, Object> getStyles();

    /**
     * setter for the enum value contained in the edge context state.
     *
     * @param contextState Set underlying enum value.
     */
    public void setContextEnum(Enum<?> contextState);

    /**
     * getter for the enum value contained in the edge context state.
     *
     * @return get the underlying enum value
     */
    public Enum<?> getContextEnum();

    /**
     * Serialize function used to store the graph on disk.
     *
     * @return A unique integer that can be mapped back the enum value
     */
    public int serialize();

    /**
     * Deserialize function used to read the graph on disk.
     *
     * @param code The integer corresponding to the enum
     * @return The Enum value
     */
    public Enum<?> deserialize(int code);

    /**
     * The context state is matchable if the underlying state represented by the edge
     * can be matched to another process when computing the critical path.
     *
     * As an example, the network state is matchable for the linux operating system.
     *
     * @return a boolean based on the enum value
     */
    public boolean isMatchable();
}
