/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree;

import org.eclipse.jdt.annotation.Nullable;

/**
 * This interface describes a group for elements in a weighted tree structure.
 * If the elements in the {@link IWeightedTreeSet} implement the {@link ITree}
 * interface, then the {@link IWeightedTreeProvider} can provide group
 * descriptors to describe each level of elements.
 *
 * Example: If the trees represent a callstack for threads that can be grouped
 * and the trace also provide some other stackable application component,
 * grouping of elements can be done in different ways:
 *
 * A possible group hierarchy would be the following:
 *
 * <pre>
 *  Per PID:
 *    [pid]
 *        [tid]
 *            data
 * </pre>
 *
 * or
 *
 * <pre>
 *  With additional component information:
 *    [pid]
 *       [application component]
 *          data
 *          [tid]
 *              data
 * </pre>
 *
 * In the first case, there would be 2 groups, and in the second 3 groups. This
 * allows to give human-readable names to groups that are otherwise simply
 * levels in a tree hierarchy.
 *
 * @author Geneviève Bastien
 */
public interface IWeightedTreeGroupDescriptor {

    /**
     * Get the group descriptor at the next level.
     *
     * @return The next group or <code>null</code> if this is a leaf level
     */
    @Nullable IWeightedTreeGroupDescriptor getNextGroup();

    /**
     * Get the human-readable name for this group descriptor
     *
     * @return The name of this group descriptor
     */
    String getName();
}
