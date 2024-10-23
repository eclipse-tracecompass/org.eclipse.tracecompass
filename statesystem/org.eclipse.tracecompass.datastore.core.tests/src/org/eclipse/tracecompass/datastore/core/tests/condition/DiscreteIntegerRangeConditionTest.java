/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.tracecompass.datastore.core.tests.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.tracecompass.internal.datastore.core.condition.ArrayIntegerRangeCondition;
import org.eclipse.tracecompass.internal.provisional.datastore.core.condition.IntegerRangeCondition;
import org.junit.Test;

/**
 * Test the discrete integer range condition.
 *
 * @author Loïc Prieur-Drevon
 */
public class DiscreteIntegerRangeConditionTest {

    private static final int LOW = 0;
    private static final int HIGH = 10;
    private static final List<Integer> VALUES = Arrays.asList(LOW, HIGH / 2, HIGH);
    private static final IntegerRangeCondition CONDITION = new ArrayIntegerRangeCondition(VALUES);

    /**
     * Ensure that we cannot build a condition with an empty collection.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor() {
        new ArrayIntegerRangeCondition(Collections.emptyList());
    }

    /**
     * Ensure that the minimum and maximum functions return the correct values.
     */
    @Test
    public void testBounds() {
        assertEquals(LOW, CONDITION.min());
        assertEquals(HIGH, CONDITION.max());
    }

    /**
     * Test that the right elements are contained in the condition.
     */
    @Test
    public void testPredicate() {
        assertFalse(CONDITION.test(-5));
        for (Integer v : VALUES) {
            assertTrue(CONDITION.test(v));
            assertFalse(CONDITION.test(v + 1));
        }
        assertFalse(CONDITION.test(15));
    }

    /**
     * Test that modifying the list used to populate the condition does not
     * affect the condition
     */
    @Test
    public void testPredicateAndAdd() {
        List<Integer> values = new ArrayList<>();
        values.add(1);
        values.add(5);
        IntegerRangeCondition condition = new ArrayIntegerRangeCondition(values);
        assertFalse(condition.test(-5));
        for (Integer v : values) {
            assertTrue(condition.test(v));
            assertFalse(condition.test(v + 1));
        }
        assertFalse(condition.test(15));
        // Add the values to the initial set and make sure it is not part of the
        // condition
        values.add(15);
        assertFalse(condition.test(15));
    }

    /**
     * Test that the right intervals intersect the condition.
     */
    @Test
    public void testIntersects() {
        assertFalse(CONDITION.intersects(Integer.MIN_VALUE, LOW - 1));
        assertTrue(CONDITION.intersects(0, 4));
        assertFalse(CONDITION.intersects(1, 4));
        assertTrue(CONDITION.intersects(2, 8));
        assertFalse(CONDITION.intersects(6, 9));
        assertTrue(CONDITION.intersects(5, 15));
        assertFalse(CONDITION.intersects(HIGH + 1, Integer.MAX_VALUE));
    }

    /**
     * Test that the returned subcondition has the correct bounds.
     */
    @Test
    public void testSubCondition() {
        IntegerRangeCondition sub = CONDITION.subCondition(-5, 8);
        assertNotNull(sub);
        assertEquals(ArrayIntegerRangeCondition.class, sub.getClass());
        int low = sub.min();
        int high = sub.max();
        assertEquals(LOW, low);
        assertEquals(HIGH / 2, high);

        // For a range where no value is include, it should return null
        sub = CONDITION.subCondition(LOW + 1, HIGH / 2 - 1);
        assertNull(sub);

        // Test conditions for border values, sub conditions are inclusive
        sub = CONDITION.subCondition(LOW, HIGH / 2);
        assertNotNull(sub);
        low = sub.min();
        high = sub.max();
        assertEquals(LOW, low);
        assertEquals(HIGH / 2, high);
    }

}
