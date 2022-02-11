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

import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.TmfEdgeInterval;

/**
 * Factory used to instantiate the context state in the {@link TmfEdgeInterval}.
 * It is used to deserialize the context states when reading the execution graph on disk.
 *
 * @author Arnaud Fiorini
 * @since 4.0
 */
public interface IEdgeContextStateFactory {
    /**
     * @param code Integer used to serialize/deserialize edge context state
     * @return The corresponding ITmfEdgeContextState
     */
    public abstract ITmfEdgeContextState createContextState(int code);
}
