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

import java.util.Comparator;
import java.util.Objects;

/**
 * Interface for vertices in a graph. A vertex in the graph is a point in time
 * when some transition start/end.
 *
 * @author Geneviève Bastien
 * @since 4.0
 */
public interface ITmfVertex extends Comparable<ITmfVertex> {
    /**
     * Compare vertices by ascending timestamps
     */
    static Comparator<ITmfVertex> ascending = Objects.requireNonNull(Comparator.nullsLast(Comparator.comparing(ITmfVertex::getTimestamp)));

    /**
     * Compare vertices by descending timestamps
     */
    static Comparator<ITmfVertex> descending = Objects.requireNonNull(Comparator.nullsLast(Comparator.comparing(ITmfVertex::getTimestamp).reversed()));

    /**
     * Returns the timestamps of this vertex
     *
     * @return the timestamp
     */
    public long getTimestamp();

}