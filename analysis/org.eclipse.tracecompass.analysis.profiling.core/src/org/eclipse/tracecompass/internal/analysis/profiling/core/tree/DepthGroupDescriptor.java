/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.core.tree;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree.ITree;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;

/**
 * A group descriptor adapted to {@link ITree} structure for weighted tree
 * providers that do not provide group description.
 *
 * @author Geneviève Bastien
 */
public class DepthGroupDescriptor implements IWeightedTreeGroupDescriptor {

    private final int fDepth;
    private @Nullable IWeightedTreeGroupDescriptor fNextGroup;

    /**
     * Create a chain of group descriptor with elements up to depth. It will
     * return the root group. The chain of next groups will be up to depth. The
     * first-depth is 0.
     *
     * @param depth
     *            The depth of elements. If there is only one level of elements,
     *            the depth should be 0.
     * @return The root group descriptor
     */
    public static IWeightedTreeGroupDescriptor createChainForDepth(int depth) {
        DepthGroupDescriptor group = new DepthGroupDescriptor(depth);
        DepthGroupDescriptor prevGroup = group;
        for (int i = depth - 1; i >= 0; i--) {
            group = new DepthGroupDescriptor(i);
            group.fNextGroup = prevGroup;
        }
        return group;
    }

    /**
     * Constructor
     *
     * @param depth
     *            The depth of the tree element to match this group
     */
    private DepthGroupDescriptor(int depth) {
        fDepth = depth;
    }

    @Override
    public @Nullable IWeightedTreeGroupDescriptor getNextGroup() {
        return fNextGroup;
    }

    @Override
    public String getName() {
        return Messages.GroupDescriptor_Level + ' ' + fDepth;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fDepth);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof DepthGroupDescriptor) {
            return ((DepthGroupDescriptor) obj).fDepth == fDepth;
        }
        return false;
    }
}
