/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Francis Giraldeau - Initial implementation and API
 *   Geneviève Bastien - Initial implementation and API
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree;

import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;

/**
 * Timed vertex for TmfGraph
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 * @since 3.0
 */
public class TmfVertex implements ITmfVertex {

    /**
     * Compare vertices by ascending timestamps
     */
    public static Comparator<TmfVertex> ascending = Objects.requireNonNull(Comparator.nullsLast(Comparator.comparing(TmfVertex::getTimestamp)));

    /**
     * Compare vertices by descending timestamps
     */
    public static Comparator<TmfVertex> descending = Objects.requireNonNull(Comparator.nullsLast(Comparator.comparing(TmfVertex::getTimestamp).reversed()));

    private final long fTimestamp;
    private final int fWorkerId;

    /**
     * Constructor
     *
     * @param timestamp
     *            The timestamp of this edge
     * @param workerId
     *            An identifier for the worker this vertex belongs to
     */
    public TmfVertex(long timestamp, Integer workerId) {
        fTimestamp = timestamp;
        fWorkerId = workerId;
    }

    /*
     * Getters and setters
     */
    @Override
    public long getTimestamp() {
        return fTimestamp;
    }

    /**
     * Returns the ID of the worker this vertex belongs to
     *
     * @return the vertex's worker id
     */
    public int getWorkerId() {
        return fWorkerId;
    }

    @Override
    public int compareTo(@Nullable ITmfVertex other) {
        if (other == null) {
            return 1;
        }
        return Long.compare(fTimestamp, other.getTimestamp());
    }

    @Override
    public String toString() {
        return "[w" + fWorkerId + "," + fTimestamp + "]"; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        // Equal when attribute and timestamp are the same
        if (!(obj instanceof TmfVertex)) {
            return false;
        }
        TmfVertex other = (TmfVertex) obj;
        return this.fWorkerId == other.fWorkerId && this.fTimestamp == other.fTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.fWorkerId, this.fTimestamp);
    }

}
