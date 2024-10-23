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

import org.eclipse.tracecompass.internal.datastore.core.condition.SingletonTimeRangeCondition;
import org.eclipse.tracecompass.internal.provisional.datastore.core.condition.TimeRangeCondition;
import org.junit.Test;

/**
 * Test the singleton time range condition
 *
 * @author Geneviève Bastien
 */
public class SingletonTimeRangeConditionTest {

    private static final long VALUE = 5;
    private static final SingletonTimeRangeCondition CONDITION = new SingletonTimeRangeCondition(VALUE);

    /**
     * Ensure that the minimum and maximum functions return the correct values.
     */
    @Test
    public void testBounds() {
        assertEquals(VALUE, (int) CONDITION.min());
        assertEquals(VALUE, (int) CONDITION.max());
    }

    /**
     * Test that the right elements are contained in the condition.
     */
    @Test
    public void testPredicate() {
        assertFalse(CONDITION.test(-5));
        assertTrue(CONDITION.test(VALUE));
        assertFalse(CONDITION.test(15));
    }

    /**
     * Test that the right intervals intersect the condition.
     */
    @Test
    public void testIntersects() {
        assertFalse(CONDITION.intersects(Integer.MIN_VALUE, VALUE - 1));
        assertTrue(CONDITION.intersects(VALUE - 1, VALUE + 1));
        assertTrue(CONDITION.intersects(VALUE, VALUE + 1));
        assertTrue(CONDITION.intersects(VALUE - 1, VALUE));
        assertFalse(CONDITION.intersects(VALUE + 1, Integer.MAX_VALUE));
    }

    /**
     * Test that the returned subcondition has the correct bounds.
     */
    @Test
    public void testSubCondition() {
        TimeRangeCondition sub = CONDITION.subCondition(VALUE - 1, VALUE + 1);
        assertNotNull(sub);
        assertEquals(sub, CONDITION);

        // For a range where no value is include, it should return null
        sub = CONDITION.subCondition(Long.MIN_VALUE, VALUE - 1);
        assertNull(sub);

        sub = CONDITION.subCondition(VALUE + 1, VALUE + 2);
        assertNull(sub);
    }

}
