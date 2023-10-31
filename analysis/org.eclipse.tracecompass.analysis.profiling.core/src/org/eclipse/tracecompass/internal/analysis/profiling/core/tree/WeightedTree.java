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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree.IWeightedTreeProvider;

/**
 * A Weighted Tree class to describe hierarchical data with a weight. This class
 * is a concrete class to describe a simple weighted tree, but it is also meant
 * to be extended to support other metrics associated with each tree, apart from
 * the weight.
 *
 * Note that the weight is such that the sum of the weight of the children is
 * smaller or equal to the weight of the parent. Failure to comply to this will
 * result in undefined behaviors when viewing the results.
 *
 * Also, if a child is added to the weighted tree for an object that is already
 * present in the children of this tree, their data will be merged.
 *
 * @author Geneviève Bastien
 * @param <T>
 *            The type of objects in this tree
 */
public class WeightedTree<@NonNull T> implements Comparable<WeightedTree<T>> {

    private final T fObject;
    private final Map<Object, WeightedTree<T>> fChildren = new HashMap<>();
    private @Nullable WeightedTree<T> fParent;
    private long fWeight = 0;

    /**
     * Constructor
     *
     * @param object
     *            The object that goes with this tree.
     */
    public WeightedTree(T object) {
        this(object, 0);
    }

    /**
     * Constructor
     *
     * @param object
     *            The object that goes with this tree.
     * @param initialWeight
     *            The initial length of this object
     */
    public WeightedTree(T object, long initialWeight) {
        fObject = object;
        fParent = null;
        fWeight = initialWeight;
    }

    /**
     * Copy constructor
     *
     * @param copy
     *            The tree to copy
     */
    protected WeightedTree(WeightedTree<T> copy) {
        fObject = copy.fObject;
        for (WeightedTree<T> entry : copy.fChildren.values()) {
            fChildren.put(entry.getObject(), entry.copyOf());
        }
        fParent = copy.fParent;
        fWeight = copy.fWeight;
    }

    /**
     * Get the weight of this tree. The unit of this weight will depend on the
     * metric it represents.
     *
     * @return The weight of this tree
     */
    public long getWeight() {
        return fWeight;
    }

    /**
     * Make a copy of this tree, with its statistics. Implementing classes
     * should make sure they copy all fields of the tree, including the
     * statistics.
     *
     * This constructor recursively copies all the children.
     *
     * @return A copy of this weighted tree
     */
    public WeightedTree<T> copyOf() {
        return new WeightedTree<>(this);
    }

    /**
     * Get the object associated with this tree
     *
     * @return The object for this tree
     */
    public T getObject() {
        return fObject;
    }

    /**
     * Get the parent of this tree
     *
     * @return The parent of this tree
     */
    protected @Nullable WeightedTree<T> getParent() {
        return fParent;
    }

    /**
     * Sets the parent of this tree
     *
     * @param parent
     *            The parent tree
     */
    protected void setParent(WeightedTree<T> parent) {
        fParent = parent;
    }

    /**
     * Get the children of this tree
     *
     * @return A collection of children trees
     */
    public Collection<WeightedTree<T>> getChildren() {
        return fChildren.values();
    }

    /**
     * Add value to the weight of this tree
     *
     * @param weight
     *            the amount to add to the length
     */
    public void addToWeight(long weight) {
        fWeight += weight;
    }

    /**
     * Add a child to this tree. If a child for the same object already exists,
     * the data for both children will be merged.
     *
     * @param child
     *            the child tree to add
     */
    public void addChild(WeightedTree<T> child) {
        WeightedTree<T> childTree = fChildren.get(child.getObject());
        if (childTree == null) {
            child.setParent(this);
            fChildren.put(child.getObject(), child);
            return;
        }
        childTree.merge(child);
    }

    /**
     * Merge a tree's data with this one. This method will modify the current
     * tree.
     *
     * It will first call {@link #mergeData(WeightedTree)} that needs to be
     * implemented for each implementation of this class.
     *
     * It will then merge the children of both trees by adding the other's
     * children to this one.
     *
     * @param other
     *            The tree to merge. It has to have the same object as the
     *            current tree otherwise it will throw an
     *            {@link IllegalArgumentException}
     */
    public final void merge(WeightedTree<T> other) {
        if (!other.getObject().equals(getObject())) {
            throw new IllegalArgumentException("AggregatedStackTraces: trying to merge stack traces of different symbols"); //$NON-NLS-1$
        }
        fWeight += other.fWeight;
        mergeData(other);
        mergeChildren(other);
    }

    /**
     * Merge the data of two trees. This should modify the current tree's
     * specific data. It is called by {@link #merge(WeightedTree)} and this
     * method MUST NOT touch the children of the tree.
     *
     * @param other
     *            The tree to merge to this one
     */
    protected void mergeData(WeightedTree<T> other) {
        // Nothing to do in main class
    }

    /**
     * Get the statistics for a metric at index. If the index {@literal <} 0,
     * then the metric is the main weight.
     *
     * @param metricIndex
     *            The index in the list of the metric metric to get. If
     *            {@literal <} 0, then the metric is the weight.
     * @return The statistics for the metric or <code>null</code> if not
     *         available
     */
    public @Nullable IStatistics<?> getStatistics(int metricIndex) {
        return null;
    }

    /**
     * Merge the children trees
     *
     * @param other
     *            The tree to merge to this one
     */
    private void mergeChildren(WeightedTree<T> other) {
        for (WeightedTree<T> otherChildSite : other.fChildren.values()) {
            T childObject = otherChildSite.getObject();
            WeightedTree<T> childSite = fChildren.get(childObject);
            if (childSite == null) {
                fChildren.put(childObject, otherChildSite.copyOf());
            } else {
                // combine children
                childSite.merge(otherChildSite);
            }
        }
    }

    /**
     * Get the maximum depth under and including this tree. A depth of 1 means
     * there is one element under and including this element.
     *
     * @return The maximum depth under and including this tree. The minimal
     *         value for the depth is 1.
     */
    public int getMaxDepth() {
        int maxDepth = 0;
        for (WeightedTree<T> child : getChildren()) {
            maxDepth = Math.max(maxDepth, child.getMaxDepth());
        }
        return maxDepth + 1;
    }

    /**
     * Get other children of this tree that are not its direct descendants. It
     * can be used for instance to represent extra data, for example kernel
     * statuses for a callstack.
     *
     * A {@link IWeightedTreeProvider} will advertise those potential children
     * data that come with this tree, and consumers can then call this method
     * with the index of this extra type, if the tree has more than one extra
     * data set
     *
     * @param index
     *            The index of this extra children set, as provided by the
     *            {@link IWeightedTreeProvider#getExtraDataSets()} method.
     *
     * @return The extra children trees
     */
    public Collection<WeightedTree<@NonNull T>> getExtraDataTrees(int index) {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "[" + fObject + "]: " + fWeight; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public int compareTo(WeightedTree<@NonNull T> o) {
        return Long.compare(fWeight, o.fWeight);
    }
}
