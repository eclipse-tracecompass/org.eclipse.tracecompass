/*******************************************************************************
 * Copyright (c) 2019, 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.tests.weighted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.analysis.profiling.core.tests.stubs.weighted.SimpleTree;
import org.eclipse.tracecompass.analysis.profiling.core.tree.ITree;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTreeSet;
import org.junit.Test;

/**
 * Test the {@link WeightedTreeSet} class
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public class WeightedTreeSetTest {

    private static final String OBJ1 = "obj1";
    private static final String OBJ2 = "obj2";

    /**
     * Test adding trees to non-{@link ITree} elements in the tree
     */
    @Test
    public void testAddingSimpleData() {
        int initialWeight = 10;
        String element1 = "element1";
        String element2 = "element2";

        // Initialization
        WeightedTreeSet<String, String> treeSet = new WeightedTreeSet<>();
        Collection<String> elements = treeSet.getElements();
        assertTrue(elements.isEmpty());
        assertTrue(treeSet.getTreesFor(element1).isEmpty());
        assertTrue(treeSet.getTreesFor(element2).isEmpty());

        // Trees for element1

        // Add a first tree to the set and make sure we can retrieve it
        WeightedTree<String> wt = new WeightedTree<>(OBJ1, initialWeight);
        treeSet.addWeightedTree(element1, wt);
        elements = treeSet.getElements();
        assertEquals(1, elements.size());
        assertEquals(element1, elements.iterator().next());
        Collection<WeightedTree<String>> trees = treeSet.getTreesFor(element1);
        assertEquals(1, trees.size());
        assertEquals(wt, trees.iterator().next());

        // Add a second tree to the set for the same object, should be merged
        wt = new WeightedTree<>(OBJ1, initialWeight);
        treeSet.addWeightedTree(element1, wt);
        elements = treeSet.getElements();
        assertEquals(1, elements.size());
        assertEquals(element1, elements.iterator().next());
        trees = treeSet.getTreesFor(element1);
        assertEquals(1, trees.size());
        WeightedTree<String> tree = trees.iterator().next();
        assertEquals(wt.getObject(), tree.getObject());
        assertEquals(initialWeight * 2, tree.getWeight());

        // Add a third tree for another object, added
        wt = new WeightedTree<>(OBJ2, initialWeight);
        treeSet.addWeightedTree(element1, wt);
        elements = treeSet.getElements();
        assertEquals(1, elements.size());
        assertEquals(element1, elements.iterator().next());
        Collection<WeightedTree<String>> el1Trees = treeSet.getTreesFor(element1);
        assertEquals(2, el1Trees.size());

        // Trees for a second element

        // Add tree to second element, first element should not be affected
        wt = new WeightedTree<>(OBJ1, initialWeight);
        treeSet.addWeightedTree(element2, wt);
        elements = treeSet.getElements();
        assertEquals(2, elements.size());
        trees = treeSet.getTreesFor(element1);
        // Trees for element1 are identical to before
        assertEquals(el1Trees, trees);
        trees = treeSet.getTreesFor(element2);
        assertEquals(1, trees.size());
    }

    /**
     * Test adding trees to {@link ITree} elements in the tree
     */
    @Test
    public void testAddingTreeData() {
        int initialWeight = 10;
        SimpleTree element1 = new SimpleTree("element1");
        SimpleTree element2 = new SimpleTree("element2");
        SimpleTree element3 = new SimpleTree("element3");
        element1.addChild(element2);
        element1.addChild(element3);
        SimpleTree element4 = new SimpleTree("element3");

        // Initialization
        WeightedTreeSet<String, SimpleTree> treeSet = new WeightedTreeSet<>();
        Collection<SimpleTree> elements = treeSet.getElements();
        assertTrue(elements.isEmpty());
        assertTrue(treeSet.getTreesFor(element1).isEmpty());
        assertTrue(treeSet.getTreesFor(element2).isEmpty());
        assertTrue(treeSet.getTreesFor(element3).isEmpty());
        assertTrue(treeSet.getTreesFor(element4).isEmpty());

        // Trees for element2, that has a parent

        // Add a first tree to the set and make sure we can retrieve it
        WeightedTree<String> wt = new WeightedTree<>(OBJ1, initialWeight);
        treeSet.addWeightedTree(element2, wt);
        elements = treeSet.getElements();
        assertEquals(1, elements.size());
        // The main element should be the parent
        assertEquals(element1, elements.iterator().next());
        Collection<WeightedTree<String>> trees = treeSet.getTreesFor(element2);
        assertEquals(1, trees.size());
        assertEquals(wt, trees.iterator().next());
        assertTrue(treeSet.getTreesFor(element1).isEmpty());

        // Add a tree to a second child
        wt = new WeightedTree<>(OBJ1, initialWeight);
        treeSet.addWeightedTree(element3, wt);
        // Base element should still be only the parent
        elements = treeSet.getElements();
        assertEquals(1, elements.size());
        assertEquals(element1, elements.iterator().next());
        trees = treeSet.getTreesFor(element3);
        assertEquals(1, trees.size());

        // Add second tree to a second child, should be merged
        wt = new WeightedTree<>(OBJ1, initialWeight);
        treeSet.addWeightedTree(element3, wt);
        // Base element should still be only the parent
        elements = treeSet.getElements();
        assertEquals(1, elements.size());
        assertEquals(element1, elements.iterator().next());
        trees = treeSet.getTreesFor(element3);
        assertEquals(1, trees.size());
        WeightedTree<String> tree = trees.iterator().next();
        assertEquals(wt.getObject(), tree.getObject());
        assertEquals(initialWeight * 2, tree.getWeight());

    }

}
