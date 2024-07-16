/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.common.core.log;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.trace_event_logger.LogUtils;

/**
 * Logger helper
 *
 * This is a logger helper, it will allow entry-exit analysis to be much easier.
 *
 * The events are saved in a JSON-like message in the phase of the event. It is
 * an event type but with extra information associated to it. Typical types can
 * be the following.
 * <ul>
 * <li>Durations
 * <ul>
 * <li><strong>B</strong>, Begin</li>
 * <li><strong>E</strong>, End</li>
 * <li><strong>X</strong>, Complete, this is an event with a duration field</li>
 * <li><strong>i</strong>, Instant / Info</li>
 * </ul>
 * </li>
 * <li>Asynchronous nested messages
 * <ul>
 * <li><strong>b</strong>, nested begin</li>
 * <li><strong>n</strong>, nested info</li>
 * <li><strong>e</strong>, nested end</li>
 * </ul>
 * </li>
 * <li>Flows
 * <ul>
 * <li><strong>s</strong>, flow begin</li>
 * <li><strong>t</strong>, flow step (info)</li>
 * <li><strong>f</strong>, flow end</li>
 * </ul>
 * </li>
 * <li>Object tracking
 * <ul>
 * <li><strong>N</Strong>, Object created</li>
 * <li><strong>D</Strong>, Object destroyed</li>
 * </ul>
 * </li>
 * <li>Mark Events - events that generate markers
 * <ul>
 * <li><strong>R</strong>, Marker event</li>
 * </ul>
 * </li>
 * <li>CounterEvents - events that count items
 * <ul>
 * <li><strong>C</strong>, Counter event</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * To use <strong>durations</strong> and/or <strong>flows</strong>, see
 * {@link ScopeLog} and {@link FlowScopeLog}. These 2 concepts are related.
 * Durations would typically be used to instrument simple methods, while flows
 * would be preferred if there are links to be made with other threads.
 * <p>
 * To use <strong>Asynchronous nested messages</strong>, see
 * {@link #traceAsyncStart(Logger, Level, String, String, int, Object...)}, and
 * {@link #traceAsyncEnd(Logger, Level, String, String, int, Object...)}
 * <p>
 * To use <strong>Object tracking</strong>, see
 * {@link #traceObjectCreation(Logger, Level, Object)} and
 * {@link #traceObjectDestruction(Logger, Level, Object)}
 *
 * The design philosophy of this class is very heavily inspired by the trace
 * event format of Google. The full specification is available <a
 * href=https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/edit?pli=1#>here</a>.
 * <p>
 *
 * The main goals are clarity of output and simplicity for the developer.
 * Performance is a nice to have, but is not the main concern of this helper. A
 * minor performance impact compared to simply logging the events is to be
 * expected.
 *
 * @author Matthew Khouzam
 * @since 3.0
 * @noinstantiate This class is not intended to be instantiated by clients. It
 *                is a helper class.
 */
public final class TraceCompassLogUtils {

    private TraceCompassLogUtils() {
        // do nothing
    }

    /**
     * Scope Logger helper. This will automatically log entry and exit of the
     * scope. This scope log will be shown under any scope enclosing it, but
     * will not be the source, or destination of any link to other scopes. If
     * relations should be done with other scopes, the {@link FlowScopeLog}
     * class is more appropriate.
     *
     * Usage:
     *
     * <pre>
     * {@code usage of ScopeLog}
     *  try (ScopeLog linksLogger = new ScopeLog(LOGGER, Level.CONFIG, "Perform Query")) { //$NON-NLS-1$
     *      ss.updateAllReferences();
     *      dataStore.addAll(ss.query(ts, trace));
     *  }
     * </pre>
     * <p>
     * will generate the following trace
     *
     * <pre>
     * {@code trace output}
     *  INFO: {"ts":12345,"ph":"B",tid:1,"name:Perform Query"}
     *  INFO: {"ts":"12366,"ph":"E","tid":1}
     * </pre>
     */
    public static class ScopeLog implements AutoCloseable {

        private org.eclipse.tracecompass.trace_event_logger.LogUtils.ScopeLog fsl;
        private long fTime;
        private String fLabel;

        /**
         * Scope logger constructor
         *
         * @param log
         *            the JUL logger to log to
         * @param level
         *            the log level see {@link Level}
         * @param label
         *            The label of the event pair
         * @param args
         *            Additional messages to pass for this scope, should be in
         *            pairs key, value, key2, value2.... typically arguments.
         *            Note that these arguments will be logged only at the
         *            beginning of the scope
         */
        public ScopeLog(Logger log, Level level, String label, Object... args) {
            fsl = new org.eclipse.tracecompass.trace_event_logger.LogUtils.ScopeLog(log, level, label, args);
            fTime = System.nanoTime();
            fLabel = label;

        }

        /**
         * Add a tag to the scope logger, will be written at the exit. This can
         * save space on the trace by having a field appended to an event rather
         * than writing a whole new event for a small chunk of data.
         *
         * If the timing information is important than it would be more
         * appropriate to call
         * {@link TraceCompassLogUtils#traceInstant(Logger, Level, String, Object...)}
         *
         * @param name
         *            the name of the field
         * @param value
         *            The value of the field.
         */
        public void addData(String name, Object value) {
            fsl.addData(name, value);
        }

        @Override
        public void close() {
            fsl.close();
            TraceCompassMonitorManager.getInstance().update(fLabel, System.nanoTime() - fTime);
        }
    }

    /**
     * Builder class for the {@link FlowScopeLog}. One can either set a category
     * or a parent scope before building the flow scope log. If none is set, a
     * default category called "null" will be used.
     *
     * @author Geneviève Bastien
     */
    public static class FlowScopeLogBuilder {

        private org.eclipse.tracecompass.trace_event_logger.LogUtils.FlowScopeLogBuilder fFslb;
        private String fLabel;

        /**
         * Flow scope log builder constructor
         *
         * @param logger
         *            the JUL logger
         * @param level
         *            the log level see {@link Level}
         * @param label
         *            The label of the event pair
         * @param args
         *            the messages to pass, should be in pairs key, value, key2,
         *            value2.... typically arguments
         */
        public FlowScopeLogBuilder(Logger logger, Level level, String label, Object... args) {
            fLabel = label;
            fFslb=new org.eclipse.tracecompass.trace_event_logger.LogUtils.FlowScopeLogBuilder(logger, level, label, args);
        }

        /**
         * Set a category for the flow scope. When building the scope, an ID
         * will be automatically generated.
         *
         * This method is mutually exclusive with
         * {@link #setParentScope(FlowScopeLog)}. Calling both will throw an
         * exception.
         *
         * @param category
         *            The category of this flow
         * @return This builder
         */
        public FlowScopeLogBuilder setCategory(String category) {
            fFslb.setCategory(category);
            return this;
        }

        /**
         * Set a category and ID for the flow scope.
         *
         * This method is mutually exclusive with
         * {@link #setParentScope(FlowScopeLog)}. Calling both will throw an
         * exception.
         *
         * @param category
         *            The category of this flow
         * @param id
         *            The ID of this flow
         * @return This builder
         */
        public FlowScopeLogBuilder setCategoryAndId(String category, int id) {
            fFslb.setCategoryAndId(category, id);
            return this;
        }

        /**
         * Set a parent scope for the flow scope to build. The scope will have
         * the same category and ID as the parent scope.
         *
         * This method is mutually exclusive with {@link #setCategory(String)}
         * and {@link #setCategoryAndId(String, int)}. Calling both will throw
         * an exception.
         *
         * @param parent
         *            The parent scope
         * @return This builder
         */
        @SuppressWarnings("resource")
        public FlowScopeLogBuilder setParentScope(FlowScopeLog parent) {
            fFslb.setParentScope(parent.getInner());
            return this;
        }

        /**
         * Build the flow scope log
         *
         * @return The flow scope log
         */
        public FlowScopeLog build() {
            return new FlowScopeLog(fFslb, fLabel);
        }

    }

    /**
     * Flow Scope Logger helper. It will automatically log entry and exit of the
     * scope. It can be used with other flow scopes to follow the program flow
     * across threads. To do so, these scopes save more data, so take more disk
     * space. If there is no inter-process/thread communication to follow, the
     * {@link ScopeLog} class would be more appropriate.
     *
     * Usage: this can be used to track asynchronous threads communication. This
     * can be used in scatter-gather/map-reduce operations as well as threads
     * that trigger a UI Thread operation.
     *
     * <pre>
     * {@code usage of FlowScopeLog}
     *  try (FlowScopeLog linksLogger = new FlowScopeLog(LOGGER, Level.CONFIG, "Perform Query", "category", 0x100)) { //$NON-NLS-1$
     *      Display.asynchExec(()->{
     *      try(FlowScopeLog linksLogger2 = new FlowScopeLog(LOGGER, Level.CONFIG, "Update UI", "category", linksLogger.getId()) {
     *          linksLogger.step("updating ui");
     *      };
     *      linksLogger.step("forked thread");
     *  }
     * </pre>
     * <p>
     * will generate the following trace (order not guaranteed)
     *
     * <pre>
     * {@code trace output}
     *  INFO: {"ts":12345,"ph":"s",tid:1,"name":"Perform Query", "cat":"category", "id":256}
     *  INFO: {"ts":12346","ph":"t",tid:1,"name":"forked thread","cat":"category", "id":256}
     *  INFO: {"ts":"12366,"ph":"f","tid":1,"cat":"category", "id":256}
     *  INFO: {"ts":12400,"ph":"s",tid:0,"name":"Update UI","cat":"category", "id":256}
     *  INFO: {"ts":12416","ph":"t",tid:0,"name":"updating ui", "cat":"category", "id":256}
     *  INFO: {"ts":"12420,"ph":"f","tid":0,"cat":"category", "id":256}
     * </pre>
     */
    public static class FlowScopeLog implements AutoCloseable {

        private final org.eclipse.tracecompass.trace_event_logger.LogUtils.FlowScopeLog fFsl;
        private final String fLabel;
        private final long fTime;

        /**
         * Flow scope logger constructor
         *
         * @param log
         *            the JUL logger
         * @param level
         *            the log level see {@link Level}
         * @param label
         *            The label of the event pair
         * @param category
         *            the category of the flow events
         * @param id
         *            The id of the flow
         * @param startFlow
         *            Whether this flow scope object is the start of a flow, or
         *            a step
         * @param args
         *            the messages to pass, should be in pairs key, value, key2,
         *            value2.... typically arguments
         */
        @SuppressWarnings("resource")
        private FlowScopeLog(org.eclipse.tracecompass.trace_event_logger.LogUtils.FlowScopeLogBuilder fslb, String label) {
            fTime = System.nanoTime();
            fFsl = Objects.requireNonNull(fslb.build());
            fLabel = label;

        }

        private LogUtils.FlowScopeLog getInner() {
            return fFsl;
        }

        /**
         * Flow step, it will add a stop point for an arrow
         *
         * @param label
         *            The label for this step
         * @param args
         *            the arguments to log
         */
        public void step(String label, Object... args) {
            fFsl.step(label, args);
        }

        /**
         * Add a tag to the scope logger, will be written at the exit. This can
         * save space on the trace by having a field appended to an event rather
         * than writing a whole new event for a small chunk of data.
         *
         *
         * If the timing information is important, then it would be more
         * appropriate to call {@link #step(String, Object...)}
         *
         * @param name
         *            the name of the field
         * @param value
         *            The value of the field.
         */
        public void addData(String name, Object value) {
            fFsl.addData(name, value);
        }

        /**
         * Get the ID for this scope. The ID can be injected to other components
         * that can use it for the scope loggers
         *
         * @return The ID of this scope
         */
        public int getId() {
            return fFsl.getId();
        }

        @Override
        public void close() {
            long time = System.nanoTime();
            fFsl.close();
            TraceCompassMonitorManager.getInstance().update(fLabel, time - fTime);
        }
    }

    /**
     * Trace Object Creation, logs the beginning of an object's life cycle.
     * Typically one can put this in the object's constructor. However if an
     * object is mutable, it can be tracked through phases with this method,
     * then the object can be re-used, however, the resulting analyses may yield
     * erroneous data if precautions are not taken.
     *
     * For mutable objects, save the return value of the call. This will be
     * passed to the destruction of the object and then it can be matched.
     *
     * @param logger
     *            The JUL logger
     * @param level
     *            The {@link Level} of this event.
     * @param item
     *            the Object to trace
     * @return The unique ID of this object (there may be collisions)
     */
    public static int traceObjectCreation(Logger logger, Level level, Object item) {
        return org.eclipse.tracecompass.trace_event_logger.LogUtils.traceObjectCreation(logger, level, item);
    }

    /**
     * Trace Object Destruction, logs the end of an object's life cycle.
     * Typically one can put this in the object's Dispose(). However if an
     * object is mutable, it can be tracked through phases with this method,
     * then the object can be re-used, however, the resulting analyses may yield
     * erroneous data if precautions are not taken.
     *
     * @param logger
     *            The JUL logger
     * @param level
     *            The {@link Level} of this event.
     * @param item
     *            the Object to trace
     */
    public static void traceObjectDestruction(Logger logger, Level level, Object item) {
        org.eclipse.tracecompass.trace_event_logger.LogUtils.traceObjectDestruction(logger, level, item);
    }

    /**
     * Trace Object Destruction, logs the end of an object's life cycle.
     * Typically one can put this in the object's Dispose(). However if an
     * object is mutable, it can be tracked through phases with this method,
     * then the object can be re-used, however, the resulting analyses may be
     *
     * @param logger
     *            The JUL logger
     * @param level
     *            The {@link Level} of this event.
     * @param item
     *            the Object to trace
     * @param uniqueId
     *            The unique ID
     */
    public static void traceObjectDestruction(Logger logger, Level level, Object item, int uniqueId) {
        org.eclipse.tracecompass.trace_event_logger.LogUtils.traceObjectDestruction(logger, level, item, uniqueId);
    }

    /**
     * Asynchronous events are used to specify asynchronous operations, such as
     * an asynchronous (or synchronous) draw, or a network operation. Call this
     * method at the beginning of such an operation.
     *
     * @param logger
     *            The JUL logger
     * @param level
     *            The {@link Level} of this event.
     * @param name
     *            The name of the asynchronous message
     * @param category
     *            the category of the asynchronous event
     * @param id
     *            The unique ID of a transaction
     * @param args
     *            Additional arguments to log
     */
    public static void traceAsyncStart(Logger logger, Level level, @Nullable String name, @Nullable String category, int id, Object... args) {
        org.eclipse.tracecompass.trace_event_logger.LogUtils.traceAsyncStart(logger, level, name, category, id, args);
    }

    /**
     * Asynchronous events are used to specify asynchronous operations, such as
     * an asynchronous (or synchronous) draw, or a network operation. Call this
     * method to augment the asynchronous event with nested information.
     *
     * @param logger
     *            The JUL logger
     * @param level
     *            The {@link Level} of this event.
     * @param name
     *            The name of the asynchronous message
     * @param category
     *            the category of the asynchronous event
     * @param id
     *            The unique ID of a transaction
     * @param args
     *            Additional arguments to log
     */
    public static void traceAsyncNested(Logger logger, Level level, @Nullable String name, @Nullable String category, int id, Object... args) {
        org.eclipse.tracecompass.trace_event_logger.LogUtils.traceAsyncNested(logger, level, name, category, id, args);
    }

    /**
     * Asynchronous events are used to specify asynchronous operations, such as
     * an asynchronous (or synchronous) draw, or a network operation. Call this
     * method at the end of such an operation.
     *
     * @param logger
     *            The JUL logger
     * @param level
     *            The {@link Level} of this event.
     * @param name
     *            The name of the asynchronous message
     * @param category
     *            the category of the asynchronous event
     * @param id
     *            The unique ID of a transaction
     * @param args
     *            Additional arguments to log
     */
    public static void traceAsyncEnd(Logger logger, Level level, @Nullable String name, @Nullable String category, int id, Object... args) {
        org.eclipse.tracecompass.trace_event_logger.LogUtils.traceAsyncEnd(logger, level, name, category, id, args);
    }

    /**
     * Instant events, created to indicate an item of interest has occurred,
     * similar to a standard System.out.println() or a
     * Java.util.Logger#log(Level). This one provides an event in a more
     * structured way. This should be the method to call to save data that
     * should have a zero duration, as it will ensure a log format that can then
     * be parsed by a trace type.
     *
     * @param logger
     *            The JUL logger
     * @param level
     *            The {@link Level} of this event.
     * @param name
     *            The name of the asynchronous message
     * @param args
     *            Additional arguments to log
     */
    public static void traceInstant(Logger logger, Level level, String name, Object... args) {
        org.eclipse.tracecompass.trace_event_logger.LogUtils.traceInstant(logger, level, name, args);
    }

    /**
     * The counter events can track a value or multiple values as they change
     * over time.
     *
     * @param logger
     *            The Logger
     * @param level
     *            The {@link Level} of this event.
     * @param name
     *            The name of the asynchronous message
     * @param args
     *            The counters to log in the format : "title", value
     */
    public static void traceCounter(Logger logger, Level level, @Nullable String name, Object... args) {
        org.eclipse.tracecompass.trace_event_logger.LogUtils.traceCounter(logger, level, name, args);
    }

    /**
     * The Marker events are events with a duration that define a region of
     * interest. These regions can be displayed in views as Markers or other
     * indicators.
     *
     * @param logger
     *            The Logger
     * @param level
     *            The {@link Level} of this event.
     * @param name
     *            The name of the marker message message
     * @param duration
     *            How long the marker should last
     * @param args
     *            The counters to log in the format : "title", value, note
     *            "color" and an rbga will be used
     */
    public static void traceMarker(Logger logger, Level level, @Nullable String name, long duration, Object... args) {
        org.eclipse.tracecompass.trace_event_logger.LogUtils.traceMarker(logger, level, name, duration, args);
    }
}
