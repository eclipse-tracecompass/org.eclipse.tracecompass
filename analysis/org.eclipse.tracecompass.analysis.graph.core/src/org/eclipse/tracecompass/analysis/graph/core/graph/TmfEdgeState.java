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

/**
 * @author Arnaud Fiorini
 * @since 4.0
 *
 */
public enum TmfEdgeState {
    /**
     * Defines a state which explains the wait cause by itself.
     */
    PASS,
    /**
     * Defines a state which can be explored further by a backward iteration
     * in the critical path algorithm.
     */
    BLOCK,
    /**
     * Other state which can be used for implementation purposes, or error handling.
     */
    UNKNOWN;
}
