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

import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.TmfGraphLegacyWrapper;

/**
 * The factory to create execution graphs
 *
 * @author Geneviève Bastien
 * @since 3.1
 */
public class TmfGraphFactory {

    /**
     * Create an in-memory graph. If the trace is too large, building it may
     * cause OutOfMemoryExceptions.
     *
     * @return A new in-memory graph
     */
    public static ITmfGraph createSimpleGraph() {
        return new TmfGraphLegacyWrapper();
    }

}
