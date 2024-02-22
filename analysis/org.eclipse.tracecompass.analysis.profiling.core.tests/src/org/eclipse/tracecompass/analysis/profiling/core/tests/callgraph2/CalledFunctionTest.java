/*******************************************************************************
 * Copyright (c) 2016, 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.tests.callgraph2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.analysis.profiling.core.model.IHostModel;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.CalledFunction;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.CalledFunctionFactory;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.CalledStringFunction;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ModelManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Tests for the CalledFunction data structure
 *
 * @author Matthew Khouzam
 */
public class CalledFunctionTest {

    private final @NonNull IHostModel fModel = ModelManager.getModelFor("test");
    private CalledStringFunction fFixture = null;
    private ICalledFunction f42Fixture;
    private ICalledFunction fHiFixture;

    /**
     * Set up values
     */
    @Before
    public void setup() {
        ICalledFunction fixture = CalledFunctionFactory.create(10, 1010, "Hello", 0, 0, null, fModel);
        assertTrue(fixture instanceof CalledStringFunction);
        fFixture = (CalledStringFunction) fixture;
        ICalledFunction fixture42 = CalledFunctionFactory.create(400, 500, 0x42L, 0, 0, fFixture, fModel);
        f42Fixture = fixture42;
        ICalledFunction hiFixture = CalledFunctionFactory.create(20, 50, "Hi", 0, 0, fFixture, fModel);
        fHiFixture = hiFixture;
    }

    /**
     * Cleanup models
     */
    @After
    public void after() {
        ModelManager.disposeModels();
    }

    /**
     * This is more to make sure that the arguments are OK except for the state
     * value
     */
    public void createValid() {
        assertNotNull(CalledFunctionFactory.create(0, 0, 0L, 0, 0, null, fModel));
        assertNotNull(CalledFunctionFactory.create(0, 0, 0, 0, 0, null, fModel));
        assertNotNull(CalledFunctionFactory.create(0, 0, "", 0, 0, null, fModel));
    }

    /**
     * Test a value with a floating point memory address.
     */
    @Test
    public void createInvalidDouble() {
        assertNotNull(CalledFunctionFactory.create(0, 0, 3.14, 0, 0, null, fModel));
    }

    /**
     * Test a value with a null memory address.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createInvalidNull() {
        CalledFunctionFactory.create(0, 0, (Object) null, 0, 0, null, fModel);
    }

    /**
     * Test a value with an invalid time range
     */
    @Test(expected = IllegalArgumentException.class)
    public void createInvalidTimeRange() {
        CalledFunctionFactory.create(10, -10, "", 0, 0, null, fModel);
    }

    /**
     * Test a value with an invalid time range
     */
    @Test(expected = IllegalArgumentException.class)
    public void createInvalidTimeRangeStateLong() {
        CalledFunctionFactory.create(10, -10, 42L, 0, 0, null, fModel);
    }

    /**
     * Test a value with an invalid time range
     */
    @Test(expected = IllegalArgumentException.class)
    public void createInvalidTimeRangeStateInteger() {
        CalledFunctionFactory.create(10, -10, 42, 0, 0, null, fModel);
    }

    /**
     * Test a value with an invalid time range
     */
    @Test(expected = IllegalArgumentException.class)
    public void createInvalidTimeRangeStateString() {
        CalledFunctionFactory.create(10, -10, "42", 0, 0, null, fModel);
    }

    /**
     * Ok to add like this
     */
    @Test
    public void testAddChildOk1() {
        ICalledFunction fixture = fFixture;
        ICalledFunction hiFixture = fHiFixture;
        ICalledFunction fixture42 = f42Fixture;
        assertNotNull(fixture);
        assertNotNull(hiFixture);
        assertNotNull(fixture42);
        long fixtureST = fixture.getSelfTime();
        long hiFixtureST = hiFixture.getSelfTime();
        long fixture42ST = fixture42.getSelfTime();
        CalledFunction newchild = (CalledFunction) CalledFunctionFactory.create(100, 200, 0x64, 0, 0, fFixture, fModel);
        // Check self time at the end
        assertEquals(fixtureST - newchild.getLength(), fixture.getSelfTime());
        assertEquals(hiFixtureST, hiFixture.getSelfTime());
        assertEquals(fixture42ST, fixture42.getSelfTime());
    }

    /**
     * Ok to add like this
     */
    @Test
    public void testAddChildOk2() {
        ICalledFunction fixture = fFixture;
        ICalledFunction hiFixture = fHiFixture;
        ICalledFunction fixture42 = f42Fixture;
        assertNotNull(fixture);
        assertNotNull(hiFixture);
        assertNotNull(fixture42);
        long fixtureST = fixture.getSelfTime();
        long hiFixtureST = hiFixture.getSelfTime();
        long fixture42ST = fixture42.getSelfTime();
        CalledStringFunction newchild = CalledFunctionFactory.create(450, 490, "OK", 0, 0, f42Fixture, fModel);
        // Check self time at the end
        assertEquals(fixtureST, fixture.getSelfTime());
        assertEquals(hiFixtureST, hiFixture.getSelfTime());
        assertEquals(fixture42ST - newchild.getLength(), fixture42.getSelfTime());
    }

    /**
     * Ok to add like this
     */
    @Test
    public void testAddChildOk3() {
        ICalledFunction fixture = fFixture;
        ICalledFunction hiFixture = fHiFixture;
        ICalledFunction fixture42 = f42Fixture;
        assertNotNull(fixture);
        assertNotNull(hiFixture);
        assertNotNull(fixture42);
        long fixtureST = fixture.getSelfTime();
        long hiFixtureST = hiFixture.getSelfTime();
        long fixture42ST = fixture42.getSelfTime();
        CalledStringFunction newchild = CalledFunctionFactory.create(450, 490, "OK", 0, 0, fHiFixture, fModel);
        // Check self time at the end
        assertEquals(fixtureST, fixture.getSelfTime());
        assertEquals(hiFixtureST - newchild.getLength(), hiFixture.getSelfTime());
        assertEquals(fixture42ST, fixture42.getSelfTime());
    }

    /**
     * Test Comparison
     */
    @Test
    public void testCompareTo() {
        CalledStringFunction fixture = fFixture;
        assertEquals(0, fixture.compareTo(fixture));
        assertTrue(fixture.compareTo(f42Fixture) < 0);
        assertTrue(f42Fixture.compareTo(fixture) > 0);
    }

    /**
     * Test bad Comparison
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCompareToBad() {
        CalledStringFunction fixture = fFixture;
        assertTrue(fixture.compareTo(null) == 0);
    }

    /**
     * Test equality and inequality
     */
    @Test
    public void testEquals() {
        assertEquals(fFixture, fFixture);
        assertEquals(fHiFixture, CalledFunctionFactory.create(20, 50, "Hi", 0, 0, fFixture, fModel));
        assertNotEquals(fFixture, f42Fixture);
        assertNotEquals(fFixture, fHiFixture);
        assertNotEquals(fFixture, null);
        assertNotEquals(fFixture, new ArrayList<>());
    }

    /**
     * Test get end. Simple getter test
     */
    @Test
    public void testGetEnd() {
        assertEquals(1010, fFixture.getEnd());
        assertEquals(50, fHiFixture.getEnd());
        assertEquals(500, f42Fixture.getEnd());
    }

    /**
     * Test get parent. Can be null
     */
    @Test
    public void testGetParent() {
        assertEquals(null, fFixture.getParent());
        assertEquals(fFixture, f42Fixture.getParent());
        assertEquals(fFixture, fHiFixture.getParent());
    }

    /**
     * Test get process ID
     */
    @Test
    public void testGetProcessId() {
        assertEquals(0, fFixture.getProcessId());
        assertEquals(0, fHiFixture.getProcessId());
        assertEquals(0, f42Fixture.getProcessId());
    }

    /**
     * Test get self time
     */
    @Test
    public void testGetSelfTime() {
        assertEquals(870, fFixture.getSelfTime());
        assertEquals(30, fHiFixture.getSelfTime());
        assertEquals(100, f42Fixture.getSelfTime());
    }

    /**
     * Test get start
     */
    @Test
    public void testGetStart() {
        assertEquals(10, fFixture.getStart());
        assertEquals(20, fHiFixture.getStart());
        assertEquals(400, f42Fixture.getStart());
    }

    /**
     * Test hashcode. Reminder: hashcodes are only guaranteed to be the same for
     * the same element, two different things may return the same hash.
     */
    @Test
    public void testHashCode() {
        assertEquals(f42Fixture.hashCode(), f42Fixture.hashCode());
        ICalledFunction calledFunction = CalledFunctionFactory.create(400, 500, 0x42L, 0, 0, fFixture, fModel);
        assertEquals(f42Fixture, calledFunction);
        assertEquals(f42Fixture.hashCode(), calledFunction.hashCode());
        calledFunction = CalledFunctionFactory.create(20, 50, "Hi", 0, 0, fFixture, fModel);
        assertEquals(fHiFixture, calledFunction);
        assertEquals(fHiFixture.hashCode(), calledFunction.hashCode());
    }

    /**
     * Test toString()
     */
    @Test
    public void testToString() {
        assertEquals("[10, 1010] Duration: 1000, Self Time: 870", fFixture.toString());
        assertEquals("[400, 500] Duration: 100, Self Time: 100", f42Fixture.toString());
        assertEquals("[20, 50] Duration: 30, Self Time: 30", fHiFixture.toString());
    }
}
