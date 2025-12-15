/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.common.core.tests.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Test cases for logger (line sensitive!)
 *
 * @author Matthew Khouzam
 */
public class LoggerTest {

    private StringOutputStream fLog;
    private Logger fLogger;
    private StreamHandler fStreamHandler;

    private static String eventUnifyId(String event) {
        return event.replaceFirst("\\\"id\\\"\\:\\\"0x[0-9A-Fa-f]+\\\"", "\"id\":\"0x1234\"");
    }

    private static class StringOutputStream extends OutputStream {

        private List<String> fMessages = new ArrayList<>();
        private StringBuilder sb = new StringBuilder();
        private boolean secondLine = false;
        private boolean start = true;

        @Override
        public void write(int b) throws IOException {
            // We don't care about carriage return (Windows). We only need to
            // rely on \n to detect the next line
            if (b == '\r') {
                return;
            }

            if (b != '\n') {
                if (secondLine) {
                    if (start) {
                        sb.append((char) b);
                    }
                    if (b == ',') {
                        start = true;
                    }
                }
            } else {
                if (secondLine) {
                    fMessages.add(eventUnifyId(sb.toString()));
                    sb = new StringBuilder();
                    secondLine = false;
                } else {
                    secondLine = true;
                }
            }
        }

        public List<String> getMessages() {
            return fMessages;
        }
    }

    /**
     * Set up logger
     */
    @Before
    public void before() {
        fLogger = Logger.getAnonymousLogger();
        fLog = new StringOutputStream();
        fStreamHandler = new StreamHandler(fLog, new SimpleFormatter());
        fStreamHandler.setLevel(Level.FINER);
        fLogger.setLevel(Level.ALL);
        fLogger.addHandler(fStreamHandler);
    }

    private static void assertLogMessage(String message, String expectedLevel, String expectedPhase, String expectedName) {
        String[] parts = message.split(":", 2);
        assertEquals(expectedLevel, parts[0]);
        JsonObject json = JsonParser.parseString(parts[1].trim()).getAsJsonObject();
        assertEquals(expectedPhase, json.get("ph").getAsString());
        assertTrue(json.get("tid").getAsInt() > 0);
        assertTrue(json.get("pid").getAsInt() > 0);
        if (expectedName != null) {
            assertEquals(expectedName, json.get("name").getAsString());
        }
    }

    /**
     * Test simple logging
     */
    @Test
    public void testHelloWorld() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.INFO, "world")) {
            // do something
            new Object();
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "INFO", "B", "world");
        assertLogMessage(fLog.getMessages().get(1), "INFO", "E", null);
    }

    /**
     * Test nesting
     */
    @Test
    public void testNesting() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.INFO, "foo")) {
            // do something
            new Object();
            try (TraceCompassLogUtils.ScopeLog log1 = new TraceCompassLogUtils.ScopeLog(logger, Level.INFO, "bar")) {
                // do something
                new Object();
            }
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "INFO", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "INFO", "B", "bar");
        assertLogMessage(fLog.getMessages().get(2), "INFO", "E", null);
        assertLogMessage(fLog.getMessages().get(3), "INFO", "E", null);
    }

    /**
     * Test nesting with filtering
     */
    @Test
    public void testNestingFiltered() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.FINE, "foo")) {
            // do something
            new Object();
            try (TraceCompassLogUtils.ScopeLog log1 = new TraceCompassLogUtils.ScopeLog(logger, Level.FINER, "bar")) {
                // do something
                new Object();
                try (TraceCompassLogUtils.ScopeLog log2 = new TraceCompassLogUtils.ScopeLog(logger, Level.FINEST, "baz")) {
                    // do something
                    new Object();
                }
            }
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "FINE", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "FINER", "B", "bar");
        assertLogMessage(fLog.getMessages().get(2), "FINER", "E", null);
        assertLogMessage(fLog.getMessages().get(3), "FINE", "E", null);
    }

    /**
     * Test nesting with different loglevels
     */
    @Test
    public void testNestingLogLevels() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.WARNING, "foo")) {
            try (TraceCompassLogUtils.ScopeLog log1 = new TraceCompassLogUtils.ScopeLog(logger, Level.FINE, "bar")) {
                // do something
                new Object();
            }
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "WARNING", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "FINE", "B", "bar");
        assertLogMessage(fLog.getMessages().get(2), "FINE", "E", null);
        assertLogMessage(fLog.getMessages().get(3), "WARNING", "E", null);
    }

    /**
     * Test nesting with additional data
     */
    @Test
    public void testNestingWithData() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.WARNING, "foo")) {
            try (TraceCompassLogUtils.ScopeLog log1 = new TraceCompassLogUtils.ScopeLog(logger, Level.FINE, "bar")) {
                // do something
                log1.addData("return", false);
            }
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "WARNING", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "FINE", "B", "bar");
        assertLogMessage(fLog.getMessages().get(2), "FINE", "E", null);
        assertLogMessage(fLog.getMessages().get(3), "WARNING", "E", null);
    }

    /**
     * Test flow with filtering
     */
    @Test
    public void testFlowFiltered() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (FlowScopeLog log = new FlowScopeLogBuilder(logger, Level.FINE, "foo").setCategory("mycat").build()) {
            // do something
            new Object();
            try (FlowScopeLog log1 = new FlowScopeLogBuilder(logger, Level.FINER, "bar", "big", "ben").setParentScope(log).build()) {
                // do something
                new Object();
                try (FlowScopeLog log2 = new FlowScopeLogBuilder(logger, Level.FINEST, "baz").setParentScope(log1).build()) {
                    // do something
                    new Object();
                }
            }
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "FINE", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "FINE", "s", "foo");
        assertLogMessage(fLog.getMessages().get(2), "FINER", "B", "bar");
        assertLogMessage(fLog.getMessages().get(3), "FINER", "t", "bar");
        assertLogMessage(fLog.getMessages().get(4), "FINER", "E", null);
        assertLogMessage(fLog.getMessages().get(5), "FINE", "E", null);
    }

    /**
     * Test flow with different loglevels
     */
    @Test
    public void testFlowLogLevels() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (FlowScopeLog log = new FlowScopeLogBuilder(logger, Level.WARNING, "foo").setCategory("mydog").build()) {
            try (FlowScopeLog log1 = new FlowScopeLogBuilder(logger, Level.FINE, "bar").setParentScope(log).build()) {
                log1.step("barked");
                new Object();
            }
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "WARNING", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "WARNING", "s", "foo");
        assertLogMessage(fLog.getMessages().get(2), "FINE", "B", "bar");
        assertLogMessage(fLog.getMessages().get(3), "FINE", "t", "bar");
        assertLogMessage(fLog.getMessages().get(4), "FINE", "t", "barked");
        assertLogMessage(fLog.getMessages().get(5), "FINE", "E", null);
        assertLogMessage(fLog.getMessages().get(6), "WARNING", "E", null);
    }

    /**
     * Test flow where child flow sets category and id instead of parent
     */
    @Test
    public void testFlowWithUnsetParent() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (FlowScopeLog log = new FlowScopeLogBuilder(logger, Level.WARNING, "foo").setCategory("mydog").build()) {
            try (FlowScopeLog log1 = new FlowScopeLogBuilder(logger, Level.FINE, "bar").setCategoryAndId("mydog", log.getId()).build()) {
                log1.step("barked");
                new Object();
            }
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "WARNING", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "WARNING", "s", "foo");
        assertLogMessage(fLog.getMessages().get(2), "FINE", "B", "bar");
        assertLogMessage(fLog.getMessages().get(3), "FINE", "t", "bar");
        assertLogMessage(fLog.getMessages().get(4), "FINE", "t", "barked");
        assertLogMessage(fLog.getMessages().get(5), "FINE", "E", null);
        assertLogMessage(fLog.getMessages().get(6), "WARNING", "E", null);
    }

    /**
     * Test flow with different loglevels
     */
    @Test
    public void testFlowWithData() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (FlowScopeLog log = new FlowScopeLogBuilder(logger, Level.WARNING, "foo").setCategory("myspider").build()) {
            try (FlowScopeLog log1 = new FlowScopeLogBuilder(logger, Level.FINE, "bar").setParentScope(log).build()) {
                // do something
                log1.addData("return", false);
            }
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "WARNING", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "WARNING", "s", "foo");
        assertLogMessage(fLog.getMessages().get(2), "FINE", "B", "bar");
        assertLogMessage(fLog.getMessages().get(3), "FINE", "t", "bar");
        assertLogMessage(fLog.getMessages().get(4), "FINE", "E", null);
        assertLogMessage(fLog.getMessages().get(5), "WARNING", "E", null);
    }

    /**
     * Test the flow scope builder without calling any other method than the
     * constructor
     */
    @Test
    public void testFlowBuilderNoExtra() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (FlowScopeLog log = new FlowScopeLogBuilder(logger, Level.WARNING, "foo").build()) {
            // do something
            new Object();
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "WARNING", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "WARNING", "s", "foo");
        assertLogMessage(fLog.getMessages().get(2), "WARNING", "E", null);
    }

    /**
     * Test the flow scope builder calling
     * {@link FlowScopeLogBuilder#setParentScope(FlowScopeLog)}, then
     * {@link FlowScopeLogBuilder#setCategory(String)}.
     */
    @Test(expected = IllegalStateException.class)
    public void testFlowBuilderCatThenParent() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (FlowScopeLog log = new FlowScopeLogBuilder(logger, Level.WARNING, "foo").setCategory("myspider").build()) {
            try (FlowScopeLog log1 = new FlowScopeLogBuilder(logger, Level.FINE, "bar").setParentScope(log).setCategory("myspider").build()) {
                // do something
                new Object();
            }
        }
    }

    /**
     * Test the flow scope builder calling
     * {@link FlowScopeLogBuilder#setParentScope(FlowScopeLog)}, then
     * {@link FlowScopeLogBuilder#setCategory(String)}.
     */
    @Test(expected = IllegalStateException.class)
    public void testFlowBuilderParentThenCat() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (FlowScopeLog log = new FlowScopeLogBuilder(logger, Level.WARNING, "foo").setCategory("myspider").build()) {
            try (FlowScopeLog log1 = new FlowScopeLogBuilder(logger, Level.FINE, "bar").setCategory("myspider").setParentScope(log).build()) {
                // do something
                new Object();
            }
        }
    }

    /**
     * Test nesting with different arguments
     */
    @Test
    public void testAttributes() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.WARNING, "foo", "Pen:Pineapple", "Apple:Pen")) {
            // do something
            new Object();
        }
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.WARNING, "foo", "Pen:Pineapple:Apple:Pen")) {
            // do something
            new Object();
        }
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.WARNING, "foo", "pen", "pineapple", "apple", "pen", "number_of_badgers", 12)) {
            // do something
            new Object();
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "WARNING", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "WARNING", "E", null);
        assertLogMessage(fLog.getMessages().get(2), "WARNING", "B", "foo");
        assertLogMessage(fLog.getMessages().get(3), "WARNING", "E", null);
        assertLogMessage(fLog.getMessages().get(4), "WARNING", "B", "foo");
        assertLogMessage(fLog.getMessages().get(5), "WARNING", "E", null);
    }

    /**
     * Test with an odd number of args.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAttributeFail3Args() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.WARNING, "foo", "Pen:Pineapple", "Apple", "Pen")) {
            // do something
            new Object();
        }
    }

    /**
     * Test with a repeating key.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAttributeFailRepeatedArgs() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.WARNING, "foo", "badger", "badger", "badger", "badger")) {
            // do something
            new Object();
        }
    }

    /**
     * Test nesting with an exception
     */
    @Test
    public void testNestingException() {
        Logger logger = fLogger;
        assertNotNull(logger);
        try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(logger, Level.INFO, "foo")) {
            try (TraceCompassLogUtils.ScopeLog log1 = new TraceCompassLogUtils.ScopeLog(logger, Level.INFO, "bar")) {
                // do something
                new Object();
                throw new Exception("test");
            }
        } catch (Exception e) {
            assertEquals("test", e.getMessage());
        }
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "INFO", "B", "foo");
        assertLogMessage(fLog.getMessages().get(1), "INFO", "B", "bar");
        assertLogMessage(fLog.getMessages().get(2), "INFO", "E", null);
        assertLogMessage(fLog.getMessages().get(3), "INFO", "E", null);
    }

    private static final class LivingObject {

        private final @NonNull Logger fLog;

        public LivingObject(@NonNull Logger logger) {
            fLog = logger;
            TraceCompassLogUtils.traceObjectCreation(fLog, Level.FINE, this);
        }

        @Override
        protected void finalize() throws Throwable {
            TraceCompassLogUtils.traceObjectDestruction(fLog, Level.FINE, this);
            super.finalize();
        }

    }

    /**
     * Test two objects lifecycles
     *
     * @throws Throwable
     *             error in finalizes
     */
    @Test
    public void testObjectLifespan() throws Throwable {
        Logger logger = fLogger;
        assertNotNull(logger);
        {
            LivingObject first = new LivingObject(logger);
            LivingObject second = new LivingObject(logger);
            assertNotNull(first);
            assertNotNull(second);
            // This will surely trigger some static analysis. This is for
            // testing purposes.
            first.finalize();
            second.finalize();
        }

        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "FINE", "N", "LivingObject");
        assertLogMessage(fLog.getMessages().get(1), "FINE", "N", "LivingObject");
        assertLogMessage(fLog.getMessages().get(2), "FINE", "D", "LivingObject");
        assertLogMessage(fLog.getMessages().get(3), "FINE", "D", "LivingObject");
    }

    /**
     * Test two objects lifecycles
     */
    @Test
    public void testCollectionLifespan() {
        Logger logger = fLogger;
        assertNotNull(logger);
        {
            List<String> avengers = new ArrayList<>();
            int uniqueID = TraceCompassLogUtils.traceObjectCreation(logger, Level.FINE, avengers);
            avengers.add("Cap");
            avengers.add("Arrow");
            avengers.add("Thor");
            avengers.add("Iron");
            TraceCompassLogUtils.traceObjectDestruction(logger, Level.FINE, avengers, uniqueID);

        }

        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "FINE", "N", "ArrayList");
        assertLogMessage(fLog.getMessages().get(1), "FINE", "D", "ArrayList");
    }

    /**
     * Test instant events
     */
    @Test
    public void testInstant() {
        Logger logger = fLogger;
        assertNotNull(logger);
        TraceCompassLogUtils.traceInstant(logger, Level.FINE, "hello", "foo", "bar");
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "FINE", "i", "hello");
    }

    /**
     * Test asynchronous messages
     */
    @Test
    public void testAsyncMessages() {
        Logger logger = fLogger;
        assertNotNull(logger);
        TraceCompassLogUtils.traceAsyncStart(logger, Level.FINE, "network connect", "net", 10);
        TraceCompassLogUtils.traceAsyncStart(logger, Level.FINER, "network lookup", "net", 10);
        TraceCompassLogUtils.traceAsyncNested(logger, Level.FINER, "network cache", "net", 10);
        // anon message
        TraceCompassLogUtils.traceAsyncStart(logger, Level.FINER, null, null, 0);
        TraceCompassLogUtils.traceAsyncEnd(logger, Level.FINER, null, null, 0);

        TraceCompassLogUtils.traceAsyncEnd(logger, Level.FINER, "network lookup", "net", 10, "OK");
        TraceCompassLogUtils.traceAsyncEnd(logger, Level.FINE, "network connect", "net", 10, "OK");

        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "FINE", "b", "network connect");
        assertLogMessage(fLog.getMessages().get(1), "FINER", "b", "network lookup");
        assertLogMessage(fLog.getMessages().get(2), "FINER", "n", "network cache");
        assertLogMessage(fLog.getMessages().get(3), "FINER", "b", null);
        assertLogMessage(fLog.getMessages().get(4), "FINER", "e", null);
        assertLogMessage(fLog.getMessages().get(5), "FINER", "e", "network lookup");
        assertLogMessage(fLog.getMessages().get(6), "FINE", "e", "network connect");
    }

    /**
     * Test that null values in arguments are properly handled
     */
    @Test
    public void testNullArguments() {
        Logger logger = fLogger;
        assertNotNull(logger);
        TraceCompassLogUtils.traceInstant(logger, Level.INFO, "test null value", "nullvalue", null);
        TraceCompassLogUtils.traceInstant(logger, Level.INFO, "test null key", null, "value");

        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "INFO", "i", "test null value");
        assertLogMessage(fLog.getMessages().get(1), "INFO", "i", "test null key");
    }

    /**
     * Test counters
     */
    @Test
    public void testCounter() {
        Logger logger = fLogger;
        assertNotNull(logger);

        TraceCompassLogUtils.traceCounter(logger, Level.FINER, "counter", "cats", 0);
        TraceCompassLogUtils.traceCounter(logger, Level.FINER, "counter", "cats", 10);
        TraceCompassLogUtils.traceCounter(logger, Level.FINER, "counter", "cats", 0);

        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "FINER", "C", "counter");
        assertLogMessage(fLog.getMessages().get(1), "FINER", "C", "counter");
        assertLogMessage(fLog.getMessages().get(2), "FINER", "C", "counter");
    }

    /**
     * Test Marker
     */
    @Test
    public void testMarker() {
        Logger logger = fLogger;
        assertNotNull(logger);
        TraceCompassLogUtils.traceMarker(logger, Level.CONFIG, "instant", 0);
        TraceCompassLogUtils.traceMarker(logger, Level.CONFIG, "colored", 15, "color", 0xaabccdd);
        fStreamHandler.flush();
        assertLogMessage(fLog.getMessages().get(0), "CONFIG", "R", "instant");
        assertLogMessage(fLog.getMessages().get(1), "CONFIG", "R", "colored");
    }

}
