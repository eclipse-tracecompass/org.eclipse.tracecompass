/*******************************************************************************
 * Copyright (c) 2015, 2022 École Polytechnique de Montréal
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
import org.eclipse.tracecompass.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfVertex;
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
     * @param start
     *            The starting vertex
     * @param end
     *            The end vertex
     * @return The graph of the critical path
     * @throws CriticalPathAlgorithmException
     *             an exception in the calculation occurred
     * @deprecated Use the {@link #computeCriticalPath(ITmfGraph, ITmfVertex, ITmfVertex)} instead
     */
    @Deprecated
    TmfGraph compute(TmfVertex start, @Nullable TmfVertex end) throws CriticalPathAlgorithmException;

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
