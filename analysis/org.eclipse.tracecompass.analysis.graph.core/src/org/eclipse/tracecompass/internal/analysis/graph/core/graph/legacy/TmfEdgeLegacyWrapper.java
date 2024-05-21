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
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.TmfEdge;

/**
 * Wrapper around the legacy {@link TmfEdge} class to make it an ITmfEdge
 *
 * @author Geneviève Bastien
 */
public class TmfEdgeLegacyWrapper implements ITmfEdge {

    private TmfEdge fLegacyEdge;
    private TmfVertexLegacyWrapper fFrom;
    private TmfVertexLegacyWrapper fTo;

    /**
     * Convert the new edge type enum values to the legacy API
     *
     * @param contextState
     *            The edge type from the new API
     * @return The edge type from the legacy API
     */
    public static TmfEdge.EdgeType newTypeToOldType(OSEdgeContextState contextState) {
        return contextState.getOldEdgeType();
    }

    /**
     * Convert the legacy API edge type enum values to the new API
     *
     * @param type
     *            The edge type from the legacy API
     * @return The edge type from the new API
     */
    public static OSEdgeContextState oldTypeToNewType(TmfEdge.EdgeType type) {
        return new OSEdgeContextState(type);
    }

    /**
     * Constructor
     *
     * @param edge
     *            The edge to wrap in this class
     * @param from
     *            The new vertex this edge is from
     * @param to
     *            The new vertex this edge goes to
     */
    public TmfEdgeLegacyWrapper(TmfEdge edge, TmfVertexLegacyWrapper from, TmfVertexLegacyWrapper to) {
        fLegacyEdge = edge;
        fFrom = from;
        fTo = to;
    }

    @Override
    public ITmfVertex getVertexFrom() {
        return fFrom;
    }

    @Override
    public ITmfVertex getVertexTo() {
        return fTo;
    }

    @Override
    public OSEdgeContextState getEdgeContextState() {
        return oldTypeToNewType(fLegacyEdge.getType());
    }

    @Override
    public @Nullable String getLinkQualifier() {
        return fLegacyEdge.getLinkQualifier();
    }

    @Override
    public long getDuration() {
        return fLegacyEdge.getDuration();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof TmfEdgeLegacyWrapper)) {
            return false;
        }
        TmfEdgeLegacyWrapper other = (TmfEdgeLegacyWrapper) obj;
        return Objects.equals(fLegacyEdge, other.fLegacyEdge) && Objects.equals(fFrom, other.fFrom) && Objects.equals(fTo, other.fTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fLegacyEdge, fTo, fFrom);
    }

    @Override
    public String toString() {
        return "[Legacy edge: " + fLegacyEdge + ']'; //$NON-NLS-1$
    }

}
