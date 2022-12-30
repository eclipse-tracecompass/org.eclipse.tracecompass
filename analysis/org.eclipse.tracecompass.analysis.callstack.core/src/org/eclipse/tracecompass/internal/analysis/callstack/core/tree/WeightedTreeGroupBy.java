/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
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
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;

/**
 * A class containing helper methods to group aggregated callgraph data by the
 * different available groups
 *
 * @author Geneviève Bastien
 */
public final class WeightedTreeGroupBy {

    private WeightedTreeGroupBy() {
        // Nothing to do
    }

    /**
     * Group callgraph groups by one of the descriptor.
     *
     * @param <N>
     *            The type of objects represented by each node in the tree
     * @param <E>
     *            The type of elements used to group the trees. If this type
     *            extends {@link ITree}, then the elements and their associated
     *            weighted trees will be grouped in a hierarchical style
     * @param <T>
     *            The type of the tree provided
     *
     * @param groupBy
     *            The group descriptor by which to group the call graph
     *            elements.
     * @param weightedTreeSet
     *            The weighted tree set to group trees for
     * @param provider
     *            The weighted tree provider
     * @return A weighted tree set that is the result of the grouping by the
     *         descriptor
     */
    public static <@NonNull N, E, T extends WeightedTree<N>> WeightedTreeSet<N, Object> groupWeightedTreeBy(IWeightedTreeGroupDescriptor groupBy, IWeightedTreeSet<N, E, T> weightedTreeSet, IWeightedTreeProvider<N, E, T> provider) {
        // Fast return: just aggregated all groups together
        if (groupBy.equals(AllGroupDescriptor.getInstance())) {
            return groupWeightedTreeByAll(weightedTreeSet);
        }

        return searchForGroups(groupBy, weightedTreeSet, provider);
    }

    private static <@NonNull N, E, T extends WeightedTree<N>> WeightedTreeSet<N, Object> searchForGroups(IWeightedTreeGroupDescriptor groupBy, IWeightedTreeSet<N, E, T> callGraph, IWeightedTreeProvider<N, E, T> provider) {
        IWeightedTreeGroupDescriptor groupDescriptor = provider.getGroupDescriptor();
        int level = 0;
        while (groupDescriptor != null && !groupDescriptor.equals(groupBy)) {
            groupDescriptor = groupDescriptor.getNextGroup();
            level++;
        }

        WeightedTreeSet<N, Object> newCg = new WeightedTreeSet<>();

        Collection<E> elements = callGraph.getElements();
        for (E element : elements) {
            Object groupElement = (element instanceof ITree) ? ((ITree) element).copyElement() : Objects.requireNonNull(element);
            recurseAddElementData(element, groupElement, callGraph, newCg, 0, level);
        }
        return newCg;
    }

    /**
     * @param originalElement
     *            The element to get the trees for
     *
     * @param groupElement
     *            The last group element
     *
     * @param treeSet
     *            The original weighted tree set
     *
     * @param newTreeSet
     *            The new weighted tree set to fill
     *
     * @param elDepth
     *            The current element depth
     *
     * @param groupDepth
     *            The depth from which elements will be aggregated. When elDepht
     *            < groupDepth, the element's tree are added as is in the new
     *            treeset, otherwise, they are merged with the trees for the
     *            element at the group depth
     */
    @SuppressWarnings({ "unchecked", "null" })
    private static <@NonNull N, E, T extends WeightedTree<N>> void recurseAddElementData(E originalElement, Object groupElement, IWeightedTreeSet<@NonNull N, E, T> treeSet, WeightedTreeSet<@NonNull N, Object> newTreeSet, int elDepth,
            int groupDepth) {

        // Add the current level of trees to the new tree set
        for (T tree : treeSet.getTreesFor(originalElement)) {
            newTreeSet.addWeightedTree(groupElement, tree.copyOf());
        }

        // Recursively add the next level of elements
        if (originalElement instanceof ITree) {
            ITree treeEl = (ITree) originalElement;
            Collection<ITree> children = treeEl.getChildren();
            for (@NonNull ITree child : children) {
                ITree nextGroupEl = (ITree) groupElement;
                if (elDepth < groupDepth) {
                    nextGroupEl = child.copyElement();
                    ((ITree) groupElement).addChild(nextGroupEl);
                }
                recurseAddElementData((E) child, nextGroupEl, treeSet, newTreeSet, elDepth + 1, groupDepth);
            }
        }
    }

    private static <@NonNull N, E, T extends WeightedTree<N>> WeightedTreeSet<N, Object> groupWeightedTreeByAll(IWeightedTreeSet<N, E, T> weightedTree) {
        WeightedTreeSet<N, Object> newTreeSet = new WeightedTreeSet<>();
        Collection<E> elements = weightedTree.getElements();
        String mainGroup = "All"; //$NON-NLS-1$
        for (E element : elements) {
            recurseAddElementData(element, mainGroup, weightedTree, newTreeSet);
        }
        return newTreeSet;
    }

    /**
     * @param element
     *            The element to get the trees for
     *
     * @param groupElement
     *            The last group element
     *
     * @param treeSet
     *            The original weighted tree set
     *
     * @param newTreeSet
     *            The new weighted tree set to fill
     */
    @SuppressWarnings({ "unchecked", "null" })
    private static <@NonNull N, E, T extends WeightedTree<N>> void recurseAddElementData(E element, String group, IWeightedTreeSet<@NonNull N, E, T> treeSet, WeightedTreeSet<@NonNull N, Object> newTreeSet) {

        // Add the current level of trees to the new tree set
        for (T tree : treeSet.getTreesFor(element)) {
            newTreeSet.addWeightedTree(group, tree.copyOf());
        }

        // Recursively add the next level of elements
        if (element instanceof ITree) {
            ITree treeEl = (ITree) element;
            Collection<?> children = treeEl.getChildren();
            for (Object child : children) {
                recurseAddElementData((E) child, group, treeSet, newTreeSet);
            }
        }
    }
}
