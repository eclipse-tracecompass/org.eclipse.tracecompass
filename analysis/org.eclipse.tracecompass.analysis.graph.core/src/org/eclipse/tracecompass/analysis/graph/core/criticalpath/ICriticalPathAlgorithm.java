/*******************************************************************************
 * Copyright (c) 2015, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.criticalpath;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;

/**
 * Interface for all critical path algorithms
 *
 * @author Francis Giraldeau
 */
public interface ICriticalPathAlgorithm {

    /**
     * Computes the critical path
     *
     * @param criticalPath
     *            The critical path graph to fill
     * @param start
     *            The starting vertex
     * @param end
     *            The end vertex
     * @return The graph of the critical path
     * @throws CriticalPathAlgorithmException
     *             an exception in the calculation occurred
     * @since 4.0
     */
    default ITmfGraph computeCriticalPath(ITmfGraph criticalPath, ITmfVertex start, @Nullable ITmfVertex end) throws CriticalPathAlgorithmException {
        throw new UnsupportedOperationException();
    }


    /**
     * Unique ID of this algorithm
     *
     * @return the ID string
     */
    String getID();

    /**
     * Human readable display name
     *
     * @return display name
     */
    String getDisplayName();

}
