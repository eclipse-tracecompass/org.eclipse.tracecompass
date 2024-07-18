/**********************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.tests.statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.statistics.ITreeStatistics;
import org.eclipse.tracecompass.analysis.timing.core.statistics.TreeStatistics;

import java.util.List;
import java.util.function.Function;

/**
 * Test class to verify {@link TreeStatistics}
 *
 * @author Siwei Zhang
 */
public class TreeStatisticsTest {

    private TreeStatistics<@NonNull StatObjectStub> treeStatistics;

    /**
     * Test class setup
     */
    @Before
    public void setUp() {
        treeStatistics = new TreeStatistics<>("Root");
    }

    /**
     * Test to verify {@link TreeStatistics#addChild(ITreeStatistics)} functionality.
     * Ensures a child is added correctly and the list of children is updated.
     */
    @Test
    public void testAddChild() {
        TreeStatistics<@NonNull StatObjectStub> childStatistics = new TreeStatistics<>("Child1");
        boolean result = treeStatistics.addChild(childStatistics);
        assertTrue(result);
        assertEquals(1, treeStatistics.getChildren().size());
        assertEquals("Child1", treeStatistics.getChildren().get(0).getName());
    }

    /**
     * Test to verify {@link TreeStatistics#getChildren()} functionality.
     * Ensures the list of children is returned correctly.
     */
    @Test
    public void testGetChildren() {
        TreeStatistics<@NonNull StatObjectStub> childStatistics1 = new TreeStatistics<>("Child1");
        TreeStatistics<@NonNull StatObjectStub> childStatistics2 = new TreeStatistics<>("Child2");
        treeStatistics.addChild(childStatistics1);
        treeStatistics.addChild(childStatistics2);

        List<ITreeStatistics<@NonNull StatObjectStub>> children = treeStatistics.getChildren();
        assertEquals(2, children.size());
        assertEquals("Child1", children.get(0).getName());
        assertEquals("Child2", children.get(1).getName());
    }

    /**
     * Test to verify {@link TreeStatistics#getName()} functionality.
     * Ensures the name of the tree statistic is returned correctly.
     */
    @Test
    public void testGetName() {
        assertEquals("Root", treeStatistics.getName());
    }


    /**
     * Test to verify the constructor {@link TreeStatistics#TreeStatistics(Function, String)}.
     * Ensures the mapper and name are set correctly.
     */
    @Test
    public void testConstructorWithMapper() {
        @NonNull Function<@NonNull StatObjectStub, @Nullable Number> valueMapper = StatObjectStub::getValue;
        TreeStatistics<@NonNull StatObjectStub> mappedStatistics = new TreeStatistics<>(valueMapper, "Mapped");

        assertEquals("Mapped", mappedStatistics.getName());
    }

    /**
     * Test to verify the addition of children and grandchildren
     * in {@link TreeStatistics#addChild(ITreeStatistics)}.
     * Ensures the hierarchy is maintained correctly with two levels of children.
     */
    @Test
    public void testTwoLevelsOfChildren() {
        TreeStatistics<@NonNull StatObjectStub> childStatistics1 = new TreeStatistics<>("Child1");
        TreeStatistics<@NonNull StatObjectStub> childStatistics2 = new TreeStatistics<>("Child2");
        TreeStatistics<@NonNull StatObjectStub> grandChildStatistics1 = new TreeStatistics<>("GrandChild1");
        TreeStatistics<@NonNull StatObjectStub> grandChildStatistics2 = new TreeStatistics<>("GrandChild2");

        // Add children to root
        treeStatistics.addChild(childStatistics1);
        treeStatistics.addChild(childStatistics2);

        // Add grandchildren to one of the children
        childStatistics1.addChild(grandChildStatistics1);
        childStatistics1.addChild(grandChildStatistics2);

        @NonNull List<@NonNull ITreeStatistics<@NonNull StatObjectStub>> rootChildren = treeStatistics.getChildren();
        assertEquals(2, rootChildren.size());
        assertEquals("Child1", rootChildren.get(0).getName());
        assertEquals("Child2", rootChildren.get(1).getName());

        @NonNull List<@NonNull ITreeStatistics<@NonNull StatObjectStub>> child1Children = rootChildren.get(0).getChildren();
        assertEquals(2, child1Children.size());
        assertEquals("GrandChild1", child1Children.get(0).getName());
        assertEquals("GrandChild2", child1Children.get(1).getName());
    }
}
