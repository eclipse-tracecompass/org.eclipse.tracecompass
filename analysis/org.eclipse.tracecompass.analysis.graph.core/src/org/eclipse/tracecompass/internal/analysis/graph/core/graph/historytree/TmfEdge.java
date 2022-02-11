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

package org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdgeContextState;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;

/**
 * Edge of a TmfGraph
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 */
public class TmfEdge implements ITmfEdge {

    private final TmfVertex fVertexFrom;
    private final TmfVertex fVertexTo;
    private final ITmfEdgeContextState fContextState;
    private final @Nullable String fQualifier;

    /**
     * Constructor
     *
     * @param from
     *            The vertex this edge leaves from
     * @param to
     *            The vertex the edge leads to
     * @param contextState
     *            The type of this edge
     */
    public TmfEdge(TmfVertex from, TmfVertex to, ITmfEdgeContextState contextState) {
        this(from, to, contextState, null);
    }

    /**
     * Constructor
     *
     * @param from
     *            The vertex this edge leaves from
     * @param to
     *            The vertex the edge leads to
     * @param contextState
     *            The type of this edge
     * @param qualifier
     *            The qualifier accompanying this edge
     */
    public TmfEdge(TmfVertex from, TmfVertex to, ITmfEdgeContextState contextState, @Nullable String qualifier) {
        fVertexFrom = from;
        fVertexTo = to;
        fContextState = contextState;
        fQualifier = qualifier;
    }

    @Override
    public ITmfVertex getVertexFrom() {
        return fVertexFrom;
    }

    @Override
    public ITmfVertex getVertexTo() {
        return fVertexTo;
    }

    @Override
    public ITmfEdgeContextState getEdgeContextState() {
        return fContextState;
    }

    @Override
    public @Nullable String getLinkQualifier() {
        return fQualifier;
    }

    @Override
    public long getDuration() {
        return fVertexTo.getTimestamp() - fVertexFrom.getTimestamp();
    }

    @Override
    public String toString() {
        return "[" + fVertexFrom + "--" + fContextState.getContextEnum() + "->" + fVertexTo + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

}
