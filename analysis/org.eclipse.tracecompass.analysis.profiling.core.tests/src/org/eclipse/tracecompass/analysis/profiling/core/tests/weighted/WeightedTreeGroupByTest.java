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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.analysis.profiling.core.tests.stubs.weighted.SimpleTree;
import org.eclipse.tracecompass.analysis.profiling.core.tests.stubs.weighted.SimpleWeightedTreeProvider;
import org.eclipse.tracecompass.analysis.profiling.core.tests.stubs.weighted.WeightedTreeTestData;
import org.eclipse.tracecompass.analysis.profiling.core.tree.ITree;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTreeGroupBy;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTreeSet;
import org.eclipse.tracecompass.internal.analysis.profiling.core.tree.AllGroupDescriptor;
import org.eclipse.tracecompass.internal.analysis.profiling.core.tree.DepthGroupDescriptor;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test the {@link WeightedTreeGroupBy} class
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public class WeightedTreeGroupByTest {

    private class WeightedTreeExpected {
        public long duration;
        public Map<String, WeightedTreeExpected> children;

        public WeightedTreeExpected(long dur, Map<String, WeightedTreeExpected> childMap) {
            duration = dur;
            children = childMap;
        }
    }

    private Map<String, WeightedTreeExpected> getExpectedAll() {
        return ImmutableMap.of(
                "op1", new WeightedTreeExpected(28, ImmutableMap.of(
                        "op2", new WeightedTreeExpected(8, ImmutableMap.of(
                                "op3", new WeightedTreeExpected(2, Collections.emptyMap()))),
                        "op3", new WeightedTreeExpected(5, ImmutableMap.of(
                                "op1", new WeightedTreeExpected(2, Collections.emptyMap()))),
                        "op4", new WeightedTreeExpected(8, Collections.emptyMap()))),
                "op4", new WeightedTreeExpected(8, Collections.emptyMap()),
                "op2", new WeightedTreeExpected(17, ImmutableMap.of(
                        "op3", new WeightedTreeExpected(1, Collections.emptyMap()),
                        "op2", new WeightedTreeExpected(6, Collections.emptyMap()))),
                "op5", new WeightedTreeExpected(15, ImmutableMap.of(
                        "op2", new WeightedTreeExpected(12, ImmutableMap.of(
                                "op3", new WeightedTreeExpected(1, Collections.emptyMap()))))));
    }

    private Map<String, WeightedTreeExpected> getExpectedL11() {
        return ImmutableMap.of(
                "op1", new WeightedTreeExpected(9, ImmutableMap.of(
                        "op2", new WeightedTreeExpected(5, ImmutableMap.of(
                                "op3", new WeightedTreeExpected(1, Collections.emptyMap()))))),
                "op4", new WeightedTreeExpected(8, Collections.emptyMap()),
                "op2", new WeightedTreeExpected(17, ImmutableMap.of(
                        "op3", new WeightedTreeExpected(1, Collections.emptyMap()),
                        "op2", new WeightedTreeExpected(6, Collections.emptyMap()))));
    }

    private Map<String, WeightedTreeExpected> getExpectedL12() {
        return ImmutableMap.of(
                "op1", new WeightedTreeExpected(19, ImmutableMap.of(
                        "op3", new WeightedTreeExpected(5, ImmutableMap.of(
                                "op1", new WeightedTreeExpected(2, Collections.emptyMap()))),
                        "op2", new WeightedTreeExpected(3, ImmutableMap.of(
                                "op3", new WeightedTreeExpected(1, Collections.emptyMap()))),
                        "op4", new WeightedTreeExpected(8, Collections.emptyMap()))),
                "op5", new WeightedTreeExpected(15, ImmutableMap.of(
                        "op2", new WeightedTreeExpected(12, ImmutableMap.of(
                                "op3", new WeightedTreeExpected(1, Collections.emptyMap()))))));
    }

    private Map<String, WeightedTreeExpected> getExpectedL21() {
        return ImmutableMap.of(
                "op1", new WeightedTreeExpected(9, ImmutableMap.of(
                        "op2", new WeightedTreeExpected(5, ImmutableMap.of(
                                "op3", new WeightedTreeExpected(1, Collections.emptyMap()))))),
                "op4", new WeightedTreeExpected(8, Collections.emptyMap()));
    }

    private Map<String, WeightedTreeExpected> getExpectedL22() {
        return ImmutableMap.of(
                "op2", new WeightedTreeExpected(17, ImmutableMap.of(
                        "op3", new WeightedTreeExpected(1, Collections.emptyMap()),
                        "op2", new WeightedTreeExpected(6, Collections.emptyMap()))));
    }

    private Map<String, WeightedTreeExpected> getExpectedL23() {
        return ImmutableMap.of(
                "op1", new WeightedTreeExpected(19, ImmutableMap.of(
                        "op2", new WeightedTreeExpected(3, ImmutableMap.of(
                                "op3", new WeightedTreeExpected(1, Collections.emptyMap()))),
                        "op3", new WeightedTreeExpected(5, ImmutableMap.of(
                                "op1", new WeightedTreeExpected(2, Collections.emptyMap()))),
                        "op4", new WeightedTreeExpected(8, Collections.emptyMap()))));
    }

    private Map<String, WeightedTreeExpected> getExpectedL24() {
        return ImmutableMap.of(
                "op5", new WeightedTreeExpected(15, ImmutableMap.of(
                        "op2", new WeightedTreeExpected(12, ImmutableMap.of(
                                "op3", new WeightedTreeExpected(1, Collections.emptyMap()))))));
    }

    private static SimpleWeightedTreeProvider getProvider(boolean withDescriptor) {
        SimpleWeightedTreeProvider provider = new SimpleWeightedTreeProvider();
        provider.setSpecificGroupDescriptor(withDescriptor);
        return provider;
    }

    /**
     * Test the group by all level for a weighted tree, with a tree that
     * provides groups
     */
    @Test
    public void testGroupByAll() {
        SimpleWeightedTreeProvider wtProvider = getProvider(true);

        groupByAll(wtProvider);
    }

    /**
     * Test the group by intermediate level for a weighted tree, with a tree
     * that provides groups
     */
    @Test
    public void testGroupByLevel1() {
        SimpleWeightedTreeProvider wtProvider = getProvider(true);
        IWeightedTreeGroupDescriptor groupDescriptor = wtProvider.getGroupDescriptor();
        assertNotNull(groupDescriptor);

        groupByLevel1(wtProvider, groupDescriptor);
    }

    /**
     * Test the group by leaf level of the weighted tree, with a tree that
     * provides groups
     */
    @Test
    public void testGroupByLevel2() {
        SimpleWeightedTreeProvider wtProvider = getProvider(true);
        IWeightedTreeGroupDescriptor groupDescriptor = wtProvider.getGroupDescriptor();
        assertNotNull(groupDescriptor);
        groupDescriptor = groupDescriptor.getNextGroup();
        assertNotNull(groupDescriptor);

        groupByLevel2(wtProvider, groupDescriptor);
    }

    /**
     * Test changing the grouping for an analysis, with a tree that provides
     * groups
     */
    @Test
    public void testMultiGroupBys() {
        SimpleWeightedTreeProvider wtProvider = getProvider(true);
        IWeightedTreeGroupDescriptor groupDescriptor1 = wtProvider.getGroupDescriptor();
        assertNotNull(groupDescriptor1);
        IWeightedTreeGroupDescriptor groupDescriptor2 = groupDescriptor1.getNextGroup();
        assertNotNull(groupDescriptor2);

        // First, group by process
        groupByLevel1(wtProvider, groupDescriptor1);

        // Then, regroup by thread
        groupByLevel2(wtProvider, groupDescriptor2);

        // Then, group by all
        groupByAll(wtProvider);

        // Group by process again
        groupByLevel1(wtProvider, groupDescriptor1);

        // Group by all
        groupByAll(wtProvider);

        // Finally by thread
        groupByLevel2(wtProvider, groupDescriptor2);

    }

    /**
     * Test the group by all level for a weighted tree, with a tree that does
     * not provide groups
     */
    @Test
    public void testGroupByAllNoGrouping() {
        SimpleWeightedTreeProvider wtProvider = getProvider(false);

        groupByAll(wtProvider);
    }

    /**
     * Test the group by intermediate level for a weighted tree, with a tree
     * that does not provider groups
     */
    @Test
    public void testGroupByLevel1NoGrouping() {
        SimpleWeightedTreeProvider wtProvider = getProvider(false);
        IWeightedTreeGroupDescriptor groupDescriptor = wtProvider.getGroupDescriptor();
        assertTrue(groupDescriptor instanceof DepthGroupDescriptor);

        groupByLevel1(wtProvider, groupDescriptor);
    }

    /**
     * Test the group by leaf level of the weighted tree, with a tree that does
     * not provider groups
     */
    @Test
    public void testGroupByLevel2NoGrouping() {
        SimpleWeightedTreeProvider wtProvider = getProvider(false);
        IWeightedTreeGroupDescriptor groupDescriptor = wtProvider.getGroupDescriptor();
        assertTrue(groupDescriptor instanceof DepthGroupDescriptor);
        groupDescriptor = groupDescriptor.getNextGroup();
        assertTrue(groupDescriptor instanceof DepthGroupDescriptor);
        assertNull(groupDescriptor.getNextGroup());

        groupByLevel2(wtProvider, groupDescriptor);
    }

    /**
     * Test changing the grouping for an analysis, with a tree does not provider
     * groups
     */
    @Test
    public void testMultiGroupBysNoGrouping() {
        SimpleWeightedTreeProvider wtProvider = getProvider(false);
        IWeightedTreeGroupDescriptor groupDescriptor1 = wtProvider.getGroupDescriptor();
        assertTrue(groupDescriptor1 instanceof DepthGroupDescriptor);
        IWeightedTreeGroupDescriptor groupDescriptor2 = groupDescriptor1.getNextGroup();
        assertTrue(groupDescriptor2 instanceof DepthGroupDescriptor);

        // First, group by process
        groupByLevel1(wtProvider, groupDescriptor1);

        // Then, regroup by thread
        groupByLevel2(wtProvider, groupDescriptor2);

        // Then, group by all
        groupByAll(wtProvider);

        // Group by process again
        groupByLevel1(wtProvider, groupDescriptor1);

        // Group by all
        groupByAll(wtProvider);

        // Finally by thread
        groupByLevel2(wtProvider, groupDescriptor2);

    }

    private void groupByAll(SimpleWeightedTreeProvider wtProvider) {
        WeightedTreeSet<String, Object> wts = WeightedTreeGroupBy.groupWeightedTreeBy(AllGroupDescriptor.getInstance(), wtProvider.getTreeSet(), wtProvider);
        Collection<@NonNull ?> elements = wts.getElements();
        assertEquals(1, elements.size());

        for (Object element : elements) {
            Collection<WeightedTree<String>> trees = wts.getTreesFor(element);
            compareCcts("Group By All", getExpectedAll(), trees);
        }
    }

    /**
     * Test the group by intermediate level for a weighted tree
     */
    private void groupByLevel1(SimpleWeightedTreeProvider wtProvider, IWeightedTreeGroupDescriptor descriptor) {

        WeightedTreeSet<String, Object> wts = WeightedTreeGroupBy.groupWeightedTreeBy(descriptor, wtProvider.getTreeSet(), wtProvider);
        Collection<?> elements = wts.getElements();
        assertEquals(2, elements.size());

        for (Object element : elements) {
            assertTrue(element instanceof SimpleTree);
            SimpleTree treeEl = (SimpleTree) element;
            // Objects should be equal to the ones from the tree
            if (treeEl.getName().equals(WeightedTreeTestData.OBJ_L11.getName())) {
                assertTrue(((SimpleTree) element).getChildren().isEmpty());
                Collection<WeightedTree<String>> trees = wts.getTreesFor(element);
                compareCcts("obj11", getExpectedL11(), trees);
            } else if (treeEl.getName().equals(WeightedTreeTestData.OBJ_L12.getName())) {
                assertTrue(((SimpleTree) element).getChildren().isEmpty());
                Collection<WeightedTree<String>> trees = wts.getTreesFor(element);
                compareCcts("obj12", getExpectedL12(), trees);
            } else {
                fail("Unexpected element: " + element);
            }
        }
    }

    /**
     * Test the group by leaf level of the weighted tree
     */
    private void groupByLevel2(SimpleWeightedTreeProvider wtProvider, IWeightedTreeGroupDescriptor descriptor) {

        WeightedTreeSet<String, Object> wts = WeightedTreeGroupBy.groupWeightedTreeBy(descriptor, wtProvider.getTreeSet(), wtProvider);
        Collection<?> elements = wts.getElements();
        assertEquals(2, elements.size());

        for (Object element : elements) {
            assertTrue(element instanceof SimpleTree);
            SimpleTree treeEl = (SimpleTree) element;
            // Objects should be equal to the ones from the tree
            if (treeEl.getName().equals(WeightedTreeTestData.OBJ_L11.getName())) {
                Collection<WeightedTree<String>> trees = wts.getTreesFor(element);
                assertTrue(trees.isEmpty());
                Collection<ITree> children = ((ITree) element).getChildren();
                assertEquals(2, children.size());
                for (Object child : children) {
                    assertTrue(child instanceof SimpleTree);
                    SimpleTree childTreeEl = (SimpleTree) child;
                    // Objects should be equal to the ones from the tree
                    if (childTreeEl.getName().equals(WeightedTreeTestData.OBJ_L21.getName())) {
                        trees = wts.getTreesFor(child);
                        compareCcts("obj21", getExpectedL21(), trees);
                    } else if (childTreeEl.getName().equals(WeightedTreeTestData.OBJ_L22.getName())) {
                        trees = wts.getTreesFor(child);
                        compareCcts("obj22", getExpectedL22(), trees);
                    } else {
                        fail("Unexpected element: " + child);
                    }
                }
            } else if (treeEl.getName().equals(WeightedTreeTestData.OBJ_L12.getName())) {
                Collection<WeightedTree<String>> trees = wts.getTreesFor(element);
                assertTrue(trees.isEmpty());
                Collection<ITree> children = ((ITree) element).getChildren();
                assertEquals(2, children.size());
                for (Object child : children) {
                    assertTrue(child instanceof SimpleTree);
                    SimpleTree childTreeEl = (SimpleTree) child;
                    // Objects should be equal to the ones from the tree
                    if (childTreeEl.getName().equals(WeightedTreeTestData.OBJ_L23.getName())) {
                        trees = wts.getTreesFor(child);
                        compareCcts("obj23", getExpectedL23(), trees);
                    } else if (childTreeEl.getName().equals(WeightedTreeTestData.OBJ_L24.getName())) {
                        trees = wts.getTreesFor(child);
                        compareCcts("obj24", getExpectedL24(), trees);
                    } else {
                        fail("Unexpected element: " + child);
                    }
                }
            } else {
                fail("Unexpected element: " + element);
            }
        }

    }

    private void compareCcts(String prefix, Map<String, WeightedTreeExpected> expected, Collection<WeightedTree<String>> trees) {
        assertEquals(prefix + " size", expected.size(), trees.size());
        for (WeightedTree<String> tree : trees) {
            WeightedTreeExpected wtExpected = expected.get(tree.getObject());
            assertNotNull(wtExpected);
            assertEquals(prefix + " object " + tree.getObject(), wtExpected.duration, tree.getWeight());
            compareCcts(prefix + tree.getObject() + ", ", wtExpected.children, tree.getChildren());
        }
    }

}
