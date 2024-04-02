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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.junit.Test;

/**
 * Test the {@link WeightedTree} class
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public class WeightedTreeTest {

    private static final String OBJECT_NAME1 = "obj1";
    private static final String OBJECT_NAME2 = "obj2";
    private static final String OBJECT_NAME3 = "obj3";
    private static final String OBJECT_NAME4 = "obj4";
    private static final String OBJECT_NAME5 = "obj5";

    /**
     * Test the constructors
     */
    @Test
    public void testConstructors() {
        // Test the default constructor with only object
        WeightedTree<String> wt = new WeightedTree<>(OBJECT_NAME1);
        assertEquals("default constructor name", OBJECT_NAME1, wt.getObject());
        assertEquals("default constructor initial weight", 0, wt.getWeight());
        assertTrue("default constructor no children", wt.getChildren().isEmpty());
        assertEquals("default depth", 1, wt.getMaxDepth());

        // Test the constructor with initial weight
        int initialWeight = 150;
        wt = new WeightedTree<>(OBJECT_NAME1, initialWeight);
        assertEquals("constructor with weight name", OBJECT_NAME1, wt.getObject());
        assertEquals("constructor with weight initial weight", initialWeight, wt.getWeight());
        assertTrue("constructor with weight no children", wt.getChildren().isEmpty());
        assertEquals("constructor with weight depth", 1, wt.getMaxDepth());
    }

    /**
     * Test the {@link WeightedTree#merge(WeightedTree)} method
     */
    @Test
    public void testSimpleMerge() {
        int initialWeight = 150;
        WeightedTree<String> wt = new WeightedTree<>(OBJECT_NAME1, initialWeight);

        // Merge without children
        WeightedTree<String> wt2 = new WeightedTree<>(OBJECT_NAME1, initialWeight);
        wt.merge(wt2);
        assertEquals("Value after merge", initialWeight * 2, wt.getWeight());
        assertEquals("merged tree unmodified", initialWeight, wt2.getWeight());
    }

    /**
     * Test merging trees for different objects, exception expected
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMergeWrongObject() {
        int initialWeight = 150;
        WeightedTree<String> wt = new WeightedTree<>(OBJECT_NAME1, initialWeight);
        WeightedTree<String> wt2 = new WeightedTree<>(OBJECT_NAME2, initialWeight);
        wt.merge(wt2);
    }

    /**
     * Test the {@link WeightedTree#addToWeight(long)} method
     */
    @Test
    public void testAddToWeight() {
        int initialWeight = 150;
        WeightedTree<String> wt = new WeightedTree<>(OBJECT_NAME1, initialWeight);
        assertEquals("initial weight", initialWeight, wt.getWeight());

        wt.addToWeight(initialWeight);
        assertEquals("initial weight", initialWeight * 2, wt.getWeight());
    }

    /**
     * Test the {@link WeightedTree#copyOf()} method
     */
    @Test
    public void testCopyOf() {
        int initialWeight = 150;
        int childWeight = 50;
        WeightedTree<String> wt = new WeightedTree<>(OBJECT_NAME1, initialWeight);

        // Test the copy without children
        WeightedTree<String> wtCopy = wt.copyOf();
        assertEquals("same weight", wt.getWeight(), wtCopy.getWeight());

        // Make sure modifying the copy does not affect the original
        wtCopy.addToWeight(initialWeight);
        assertEquals("New copy weight", initialWeight * 2, wtCopy.getWeight());
        assertEquals("Unchanged original weight", initialWeight, wt.getWeight());

        // Add a child to wt and copy, children are also copied
        WeightedTree<String> child = new WeightedTree<>(OBJECT_NAME1, childWeight);
        wt.addChild(child);
        wtCopy = wt.copyOf();
        assertEquals("Same weight", wt.getWeight(), wtCopy.getWeight());
        Collection<WeightedTree<String>> children = wtCopy.getChildren();
        assertEquals("No children copied", 1, children.size());

        // Make sure modifying the child does not affect the original child
        WeightedTree<String> childCopy = children.iterator().next();
        childCopy.addToWeight(childWeight);
        assertEquals("New child copy weight", childWeight * 2, childCopy.getWeight());
        assertEquals("Unchanged original child weight", childWeight, child.getWeight());

    }

    /**
     * Test the {@link WeightedTree#addChild(WeightedTree)} method
     */
    @Test
    public void testAddChild() {
        int initialWeight = 150;
        WeightedTree<String> wt = new WeightedTree<>(OBJECT_NAME1, initialWeight);

        int childWeight = 30;
        WeightedTree<String> child1 = new WeightedTree<>(OBJECT_NAME2, childWeight);

        // Add a first child
        wt.addChild(child1);
        assertEquals("Unchanged parent weight", initialWeight, wt.getWeight());
        assertEquals("Unchanged child weight", childWeight, child1.getWeight());
        assertEquals("Children of parent", 1, wt.getChildren().size());
        assertTrue("No child to child", child1.getChildren().isEmpty());
        WeightedTree<String> treeChild = wt.getChildren().iterator().next();
        assertEquals("Child of parent", child1, treeChild);

        // Add a second child for different object
        WeightedTree<String> child2 = new WeightedTree<>(OBJECT_NAME3, childWeight);
        wt.addChild(child2);
        assertEquals("Unchanged parent weight", initialWeight, wt.getWeight());
        assertEquals("Unchanged child weight", childWeight, child2.getWeight());
        assertEquals("Children of parent", 2, wt.getChildren().size());

        // Add a third child to merge with child1
        WeightedTree<String> child3 = new WeightedTree<>(OBJECT_NAME2, childWeight);
        wt.addChild(child3);
        assertEquals("Unchanged parent weight", initialWeight, wt.getWeight());
        assertEquals("Unchanged child weight", childWeight, child3.getWeight());
        assertEquals("Children of parent", 2, wt.getChildren().size());
        assertEquals("New tree child weight", childWeight * 2, treeChild.getWeight());

        assertEquals("Max depth", 2, wt.getMaxDepth());

        // Add wt as a child to a new parent tree
        WeightedTree<String> parent = new WeightedTree<>(OBJECT_NAME4, initialWeight * 2);
        parent.addChild(wt);
        assertFalse("Parent's child", parent.getChildren().isEmpty());
        treeChild = parent.getChildren().iterator().next();
        assertEquals("no children lost", 2, treeChild.getChildren().size());
        assertEquals("Final max depth", 3, parent.getMaxDepth());
    }

    /**
     * Test the {@link WeightedTree#merge(WeightedTree)} method for objects that
     * have many levels of similar children
     */
    @Test
    public void testDeepMerge() {
        /**
         * Here's the layout of the objects to merge
         *
         * <pre>
         *        1                 1
         *       / \ \             / \ \
         *      1   2 3           1   2 5
         *     / \  |            / \  | |
         *    4   5 2           3   4 1 2
         *
         * Expected:
         *          1*2
         *     /     |   \  \
         *   1*2    2*2   3  5
         *  / | \   / \      |
         * 3 4*2 5 1   2     2
         * </pre>
         */
        int level0Weight = 150;
        int level1Weight = 40;
        int level2Weight = 10;

        // Prepare the objects to merge
        // First object
        WeightedTree<String> wtParent1 = new WeightedTree<>(OBJECT_NAME1, level0Weight);
        WeightedTree<String> wtLevel1 = new WeightedTree<>(OBJECT_NAME1, level1Weight);
        wtLevel1.addChild(new WeightedTree<>(OBJECT_NAME4, level2Weight));
        wtLevel1.addChild(new WeightedTree<>(OBJECT_NAME5, level2Weight));
        wtParent1.addChild(wtLevel1);

        wtLevel1 = new WeightedTree<>(OBJECT_NAME2, level1Weight);
        wtLevel1.addChild(new WeightedTree<>(OBJECT_NAME2, level2Weight));
        wtParent1.addChild(wtLevel1);

        wtParent1.addChild(new WeightedTree<>(OBJECT_NAME3, level1Weight));

        // Second object
        WeightedTree<String> wtParent2 = new WeightedTree<>(OBJECT_NAME1, level0Weight);
        wtLevel1 = new WeightedTree<>(OBJECT_NAME1, level1Weight);
        wtLevel1.addChild(new WeightedTree<>(OBJECT_NAME3, level2Weight));
        wtLevel1.addChild(new WeightedTree<>(OBJECT_NAME4, level2Weight));
        wtParent2.addChild(wtLevel1);

        wtLevel1 = new WeightedTree<>(OBJECT_NAME2, level1Weight);
        wtLevel1.addChild(new WeightedTree<>(OBJECT_NAME1, level2Weight));
        wtParent2.addChild(wtLevel1);

        wtLevel1 = new WeightedTree<>(OBJECT_NAME5, level1Weight);
        wtLevel1.addChild(new WeightedTree<>(OBJECT_NAME2, level2Weight));
        wtParent2.addChild(wtLevel1);

        // Merge the objects and test its content
        wtParent1.merge(wtParent2);

        assertEquals("Level 0 Weight", level0Weight * 2, wtParent1.getWeight());
        Collection<WeightedTree<String>> level1Children = wtParent1.getChildren();
        assertEquals("Level 0 Nb children", 4, level1Children.size());
        assertEquals("Max depth", 3, wtParent1.getMaxDepth());

        for (WeightedTree<String> level1Child : level1Children) {
            switch (level1Child.getObject()) {
            case OBJECT_NAME1:
            {
                assertEquals("Level 1 Weight 1", level1Weight * 2, level1Child.getWeight());
                Collection<WeightedTree<String>> level2Children = level1Child.getChildren();
                assertEquals("Level 1 Nb children 1", 3, level2Children.size());
                for (WeightedTree<String> level2Child : level2Children) {
                    switch (level2Child.getObject()) {
                    case OBJECT_NAME3: // Fall-through, same weight and children
                    case OBJECT_NAME5:
                        assertEquals("Level 2-1 Weight", level2Weight, level2Child.getWeight());
                        assertTrue("Empty children at last level", level2Child.getChildren().isEmpty());
                        break;
                    case OBJECT_NAME4:
                        assertEquals("Level 2-1 Weight", level2Weight * 2, level2Child.getWeight());
                        assertTrue("Empty children at last level", level2Child.getChildren().isEmpty());
                        break;
                    default:
                        fail("Unknown child " + level2Child.getObject());
                        break;
                    }
                }
            }
                break;
            case OBJECT_NAME2:
            {
                assertEquals("Level 1 Weight 2", level1Weight * 2, level1Child.getWeight());
                Collection<WeightedTree<String>> level2Children = level1Child.getChildren();
                assertEquals("Level 1 Nb children 2", 2, level2Children.size());
                for (WeightedTree<String> level2Child : level2Children) {
                    switch (level2Child.getObject()) {
                    case OBJECT_NAME1: // Fall-through, same weight and children
                    case OBJECT_NAME2:
                        assertEquals("Level 2-2 Weight", level2Weight, level2Child.getWeight());
                        assertTrue("Empty children at last level", level2Child.getChildren().isEmpty());
                        break;
                    default:
                        fail("Unknown child " + level2Child.getObject());
                        break;
                    }
                }
            }
                break;
            case OBJECT_NAME3:
                assertEquals("Level 1 Weight 3", level1Weight, level1Child.getWeight());
                assertTrue("Empty children at last level", level1Child.getChildren().isEmpty());
                break;
            case OBJECT_NAME5:
                assertEquals("Level 1 Weight 4", level1Weight, level1Child.getWeight());
                Collection<WeightedTree<String>> level2Children = level1Child.getChildren();
                assertEquals("Level 1 Nb children 4", 1, level2Children.size());
                for (WeightedTree<String> level2Child : level2Children) {
                    switch (level2Child.getObject()) {
                    case OBJECT_NAME2:
                        assertEquals("Level 2-2 Weight", level2Weight, level2Child.getWeight());
                        assertTrue("Empty children at last level", level2Child.getChildren().isEmpty());
                        break;
                    default:
                        fail("Unknown child " + level2Child.getObject());
                        break;
                    }
                }
                break;
            default:
                fail("Unknown child " + level1Child.getObject());
                break;
            }
        }
    }

}
