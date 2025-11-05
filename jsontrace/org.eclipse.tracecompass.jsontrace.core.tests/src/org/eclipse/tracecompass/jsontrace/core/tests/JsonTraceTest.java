/*******************************************************************************
 * Copyright (c) 2018, 2020 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.jsontrace.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.jsontrace.core.tests.stub.JsonStubTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Test generic Json trace
 *
 * @author Simon Delisle
 */
public class JsonTraceTest {

    private final Gson gson = new Gson();

    /**
     * Test the unsorted json trace
     *
     * @throws TmfTraceException
     *             If there is a problem while initializing the trace
     */
    @Test
    public void testSortedTrace() throws TmfTraceException {
        String path = "traces/sortedTrace.json"; //$NON-NLS-1$
        long nbEvents = 5;
        ITmfTimestamp startTime = TmfTimestamp.fromNanos(1);
        ITmfTimestamp endTime = TmfTimestamp.fromNanos(5);
        testJsonTrace(path, nbEvents, startTime, endTime, "{\"events\":[]}");
    }

    /**
     * Test the unsorted json trace
     *
     * @throws TmfTraceException
     *             If there is a problem while initializing the trace
     */
    @Test
    public void testUnsortedTrace() throws TmfTraceException {
        String path = "traces/unsortedTrace.json"; //$NON-NLS-1$
        long nbEvents = 5;
        ITmfTimestamp startTime = TmfTimestamp.fromNanos(1);
        ITmfTimestamp endTime = TmfTimestamp.fromNanos(5);
        testJsonTrace(path, nbEvents, startTime, endTime, "{\"events\":[]}");
    }

    /**
     * Test the json trace with metadata
     *
     * @throws TmfTraceException
     *             If there is a problem while initializing the trace
     */
    @Test
    public void testMetadataBeginTrace() throws TmfTraceException {
        String path = "traces/traceMetadataBegin.json"; //$NON-NLS-1$
        long nbEvents = 6;
        ITmfTimestamp startTime = TmfTimestamp.fromNanos(1730000000000L);
        ITmfTimestamp endTime = TmfTimestamp.fromNanos(1730000005000L);
        testJsonTrace(path, nbEvents, startTime, endTime,
                "{\"events\":[], \"metadata\":{\"source\":\"sensor-A\",\"version\":1,\"generatedAt\":\"2025-10-23T12:00:00Z\"}}");
    }

    /**
     * Test the json trace with metadata
     *
     * @throws TmfTraceException
     *             If there is a problem while initializing the trace
     */
    @Test
    public void testMetadataEndTrace() throws TmfTraceException {
        String path = "traces/traceMetadataEnd.json"; //$NON-NLS-1$
        long nbEvents = 5;
        ITmfTimestamp startTime = TmfTimestamp.fromNanos(1730000000000L);
        ITmfTimestamp endTime = TmfTimestamp.fromNanos(1730000004000L);
        testJsonTrace(path, nbEvents, startTime, endTime,
                "{\"events\":[], \"metadata\":{\"source\":\"sensor-A\",\"version\":1,\"generatedAt\":\"2025-10-23T12:00:00Z\"}}");
    }

    /**
     * Test the json trace with metadata
     *
     * @throws TmfTraceException
     *             If there is a problem while initializing the trace
     */
    @Test
    public void testMetadataBeginEndTrace() throws TmfTraceException {
        String path = "traces/traceMetadataBeginEnd.json"; //$NON-NLS-1$
        long nbEvents = 5;
        ITmfTimestamp startTime = TmfTimestamp.fromNanos(1730000000000L);
        ITmfTimestamp endTime = TmfTimestamp.fromNanos(1730000004000L);
        testJsonTrace(path, nbEvents, startTime, endTime,
                "{\"events\":[], \"metadataStart\":{\"source\":\"sensor-A\",\"version\":1,\"generatedAt\":\"2025-10-23T12:00:00Z\"},"
                + "\"metadataEnd\":{\"checksum\":\"abc123\",\"recordCount\":5,\"processedAt\":\"2025-10-23T12:05:00Z\"}}");
    }

    private void testJsonTrace(String path, long expectedNbEvents, ITmfTimestamp startTime, ITmfTimestamp endTime, String metadata)
            throws TmfTraceException {
        ITmfTrace trace = new JsonStubTrace();
        try {
            IStatus validate = trace.validate(null, path);
            assertTrue(validate.getMessage(), validate.isOK());
            trace.initTrace(null, path, ITmfEvent.class);
            ITmfContext context = trace.seekEvent(0.0);
            ITmfEvent event = trace.getNext(context);
            long count = 0;
            long prevTs = -1;
            while (event != null) {
                count++;
                @NonNull
                ITmfTimestamp currentTime = event.getTimestamp();
                assertNotNull(currentTime);
                // Make sure that the event are ordered
                assertTrue(currentTime.toNanos() >= prevTs);
                prevTs = currentTime.toNanos();
                event = trace.getNext(context);
            }
            assertEquals(expectedNbEvents, count);
            assertEquals(expectedNbEvents, trace.getNbEvents());
            assertEquals(startTime.toNanos(), trace.getStartTime().toNanos());
            assertEquals(endTime.toNanos(), trace.getEndTime().toNanos());
            JsonElement expectedElement = gson.fromJson(metadata, JsonElement.class);
            JsonElement actualElement = gson.fromJson(((JsonStubTrace) trace).fMetadata, JsonElement.class);
            assertEquals(expectedElement, actualElement);
        } finally {
            trace.dispose();
        }
    }
}
