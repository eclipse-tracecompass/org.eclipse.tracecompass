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

import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface for the various graph edges. Edges are transition from/to vertices,
 * of various types, with additional qualifiers to describe what happened during
 * that transition.
 *
 * @author Geneviève Bastien
 * @since 4.0
 */
public interface ITmfEdge {

    /**
     * Get the origin vertex of this edge
     *
     * @return The origin vertex
     */
    ITmfVertex getVertexFrom();

    /**
     * Get the destination vertex of this edge
     *
     * @return The destination vertex
     */
    ITmfVertex getVertexTo();

    /**
     * Get the edge type
     *
     * @return The type of the edge
     */
    ITmfEdgeContextState getEdgeContextState();

    /**
     * Get the link qualifier, ie a descriptor for the link. This has no effect
     * on the graph or critical path
     *
     * @return The link qualifier
     */
    @Nullable String getLinkQualifier();

    /**
     * Returns the duration of the edge
     *
     * @return The duration (in nanoseconds)
     */
    long getDuration();

}
