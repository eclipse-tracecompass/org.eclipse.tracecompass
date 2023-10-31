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

package org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A basic interface for a tree structure, i.e., hierarchical data where each
 * node can be linked to a specific object.
 *
 * @author Geneviève Bastien
 */
public interface ITree {

    /**
     * Get the name of this tree element, it should be human-readable as it will
     * be displayed to the user
     *
     * @return The name of this tree element
     */
    String getName();

    /**
     * Get the parent of this tree element
     *
     * @return The parent of this object
     */
    @Nullable ITree getParent();

    /**
     * Get the children of this object
     *
     * @return A collection of children elements
     */
    Collection<ITree> getChildren();

    /**
     * Add a child to this tree object. This method should make sure to set the
     * parent of the child to the current object
     *
     * @param child
     *            The child object
     */
    void addChild(ITree child);

    /**
     * Set the parent of this tree object
     *
     * @param parent
     *            The parent of the object, it can be <code>null</code> if there
     *            is no parent
     */
    void setParent(@Nullable ITree parent);

    /**
     * Create a new element that is a copy of the current object. This copy
     * should copy only the object's data, not the hierarchy as callers of this
     * method may want to create a new hierarchy for those elements.
     *
     * @return a new element, copy of the current element
     */
    ITree copyElement();

    /**
     * Get the depth of this object, recursively with its children. An object
     * with no children would have a depth of 1.
     *
     * @param tree
     *            The object for which to get the depth
     * @return The depth
     */
    static int getDepth(ITree tree) {
        Collection<ITree> children = tree.getChildren();
        if (children.isEmpty()) {
            return 1;
        }
        int depth = 1;
        for (ITree child : children) {
            depth = Math.max(depth, getDepth(child) + 1);
        }
        return depth;
    }
}
