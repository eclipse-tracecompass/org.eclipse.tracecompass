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

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.TmfVertex;

/**
 * Wrapper around the legacy {@link TmfVertex} class to make it an ITmfVertex
 *
 * @author Geneviève Bastien
 */
public class TmfVertexLegacyWrapper implements ITmfVertex {

    private final IGraphWorker fWorker;
    private final TmfVertex fLegacyVertex;

    /**
     * Convert the new edge directions enum values to the legacy API
     *
     * @param direction
     *            The direction from the new API
     * @return The direction from the legacy API
     */
    public static TmfVertex.EdgeDirection newDirectionToOldDirection(ITmfGraph.EdgeDirection direction) {
        switch (direction) {
        case INCOMING_HORIZONTAL_EDGE:
            return TmfVertex.EdgeDirection.INCOMING_HORIZONTAL_EDGE;
        case INCOMING_VERTICAL_EDGE:
            return TmfVertex.EdgeDirection.INCOMING_VERTICAL_EDGE;
        case OUTGOING_HORIZONTAL_EDGE:
            return TmfVertex.EdgeDirection.OUTGOING_HORIZONTAL_EDGE;
        case OUTGOING_VERTICAL_EDGE:
            return TmfVertex.EdgeDirection.OUTGOING_VERTICAL_EDGE;
        default:
            throw new IllegalArgumentException("Unknown direction " + direction); //$NON-NLS-1$

        }
    }

    /**
     * Constructor
     *
     * @param worker
     *            The worker this vertex belongs to
     * @param vertex
     *            The legacy vertex
     */
    public TmfVertexLegacyWrapper(IGraphWorker worker, TmfVertex vertex) {
        fWorker = worker;
        fLegacyVertex = vertex;
    }

    /**
     * Get the worker this vertex belongs to
     *
     * @return The worker associated with this vertex
     */
    public IGraphWorker getWorker() {
        return fWorker;
    }

    @Override
    public int compareTo(ITmfVertex arg0) {
        if (!(arg0 instanceof TmfVertexLegacyWrapper)) {
            return 0;
        }
        return fLegacyVertex.compareTo(((TmfVertexLegacyWrapper) arg0).fLegacyVertex);
    }

    @Override
    public long getTimestamp() {
        return fLegacyVertex.getTs();
    }

    /**
     * Get the legacy vertex wrapped in this class
     *
     * @return The legacy vertex
     */
    public TmfVertex getVertex() {
        return fLegacyVertex;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TmfVertexLegacyWrapper)) {
            return false;
        }
        TmfVertexLegacyWrapper other = (TmfVertexLegacyWrapper) obj;
        return Objects.equals(fLegacyVertex, other.fLegacyVertex) && Objects.equals(fWorker, other.fWorker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fLegacyVertex, fWorker);
    }

    @Override
    public String toString() {
        return "[Legacy vertex for worker: " + fWorker + ", vertex: " + fLegacyVertex + ']'; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
