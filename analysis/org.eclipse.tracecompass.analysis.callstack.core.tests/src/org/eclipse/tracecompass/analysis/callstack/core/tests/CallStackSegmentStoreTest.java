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

package org.eclipse.tracecompass.analysis.callstack.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.callstack.core.tests.stubs.CallStackAnalysisStub;
import org.eclipse.tracecompass.analysis.callstack.core.tests.stubs.CalledFunctionStub;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

/**
 * Test the callstack analysis as a segment store
 *
 * @author Geneviève Bastien
 */
public class CallStackSegmentStoreTest extends CallStackTestBase {

    /**
     * Test the callstack data using the callstack object
     */
    @Test
    public void testSeriesSegmentStoreIterator() {
        CallStackAnalysisStub module = getModule();
        assertNotNull(module);

        ISegmentStore<@NonNull ISegment> segmentStore = module.getSegmentStore();
        assertNotNull(segmentStore);

        Iterator<@NonNull ISegment> iterator = segmentStore.iterator();
        assertEquals("Segment store iterator count", 18, Iterators.size(iterator));
        assertEquals("Segment store size", 18, segmentStore.size());
        assertFalse(segmentStore.isEmpty());
    }

    /**
     * Test the segment store's intersecting query methods
     */
    @Test
    public void testIntersectingSegmentStore() {
        CallStackAnalysisStub module = getModule();
        assertNotNull(module);

        ISegmentStore<@NonNull ISegment> segmentStore = module.getSegmentStore();
        assertNotNull(segmentStore);

        // Test with some boundaries: all elements that start or end at 10
        // should be included
        Iterable<@NonNull ISegment> elements = segmentStore.getIntersectingElements(10L);
        assertEquals("Intersecting 10", 9, Iterables.size(elements));

        elements = segmentStore.getIntersectingElements(10L, 15L);
        assertEquals("Between 10 and 15", 12, Iterables.size(elements));
    }

    /**
     * Test the segment store's contains method with invalid / fake inputs
     */
    @Test
    public void testContainsSegmentStoreInvalidInput() {
        CallStackAnalysisStub module = getModule();
        assertNotNull(module);

        ISegmentStore<@NonNull ISegment> segmentStore = module.getSegmentStore();
        assertNotNull(segmentStore);

        // Test wrong input
        assertFalse(segmentStore.contains(null));
        assertFalse(segmentStore.containsAll(null));
        assertTrue(segmentStore.containsAll(Collections.emptyList()));

        // Test with fake segments
        ISegment fakeSegment = new CalledFunctionStub(0L, 1L);
        assertFalse(segmentStore.contains(fakeSegment));
        fakeSegment = new CalledFunctionStub(2L, 321321L);
        assertFalse(segmentStore.contains(fakeSegment));
        fakeSegment = new CalledFunctionStub(-10L, -2L);
        assertFalse(segmentStore.contains(fakeSegment));
        fakeSegment = new CalledFunctionStub(-10L, 10L);
        assertFalse(segmentStore.contains(fakeSegment));
        fakeSegment = new CalledFunctionStub(0L, 10L);
        assertFalse(segmentStore.contains(fakeSegment));
        fakeSegment = new CalledFunctionStub(32137216321L, 10L);
        assertFalse(segmentStore.contains(fakeSegment));
        fakeSegment = new CalledFunctionStub(32137216321L, 32137216322L);
        assertFalse(segmentStore.contains(fakeSegment));
    }

    /**
     * Test the segment store's contains method with correct input
     */
    @Test
    public void testContainsSegmentStore() {
        CallStackAnalysisStub module = getModule();
        assertNotNull(module);

        ISegmentStore<@NonNull ISegment> segmentStore = module.getSegmentStore();
        assertNotNull(segmentStore);

        // Test that it contains what it really contains
        Iterator<@NonNull ISegment> segmentIterator = segmentStore.iterator();
        List<ISegment> segments = new ArrayList<>();
        while (segmentIterator.hasNext()) {
            ISegment segment = segmentIterator.next();
            assertTrue(segmentStore.contains(segment));
            segments.add(segment);
            assertTrue(segmentStore.containsAll(segments));
        }
        while (!segments.isEmpty()) {
            segments.remove(0);
            assertTrue(segmentStore.containsAll(segments));
        }
    }
}
