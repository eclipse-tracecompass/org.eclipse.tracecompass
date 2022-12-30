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

package org.eclipse.tracecompass.internal.analysis.callstack.core.tree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * An implementation of the weighted tree set, with generic types and nodes
 *
 * @author Geneviève Bastien
 * @param <N>
 *            The type of objects represented by each node in the tree
 * @param <E>
 *            The type of elements used to group the trees. If this type extends
 *            {@link ITree}, then the elements and their associated weighted
 *            trees will be grouped in a hierarchical style
 */
public class WeightedTreeSet<@NonNull N, E> implements IWeightedTreeSet<N, E, WeightedTree<N>> {

    private final Set<E> fRootElements = new HashSet<>();
    private final Multimap<Object, WeightedTree<N>> fTrees = HashMultimap.create();

    @Override
    public Collection<E> getElements() {
        return fRootElements;
    }

    @Override
    public Collection<@NonNull WeightedTree<N>> getTreesFor(Object element) {
        return Objects.requireNonNull(fTrees.get(element));
    }

    /**
     * Add a weighted tree for an element in this set. If a tree for the same
     * object already exists, their data will be merged.
     *
     * @param dstGroup
     *            The group to which to add this tree
     * @param tree
     *            The weighted tree to add to this set. This tree may be
     *            modified and merged with others, so it should not be a tree
     *            that is part of another tree (for groupings or diff or other
     *            operations for instance)
     */
    @SuppressWarnings({ "unchecked", "null" })
    public void addWeightedTree(E dstGroup, WeightedTree<N> tree) {
        // Make sure the root element is present
        E root = dstGroup;
        if (dstGroup instanceof ITree) {
            ITree parent = ((ITree) dstGroup).getParent();
            while (parent != null) {
                root = (E) parent;
                parent = parent.getParent();
            }
            fRootElements.add(root);
        }
        fRootElements.add(root);

        // Add the tree to the appropriate group
        Collection<WeightedTree<N>> trees = fTrees.get(dstGroup);
        for (WeightedTree<N> currentTree : trees) {
            if (currentTree.getObject().equals(tree.getObject())) {
                currentTree.merge(tree);
                return;
            }
        }
        fTrees.put(dstGroup, tree);
    }
}
