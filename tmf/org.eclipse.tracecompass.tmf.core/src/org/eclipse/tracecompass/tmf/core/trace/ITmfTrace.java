/*******************************************************************************
 * Copyright (c) 2009, 2017 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Francois Chouinard - Updated as per TMF Trace Model 1.0
 *   Geneviève Bastien  - Added timestamp transforms and timestamp
 *                        creation functions
 *   Patrick Tasse - Add support for folder elements
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.trace;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.math.SaturatedArithmetic;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.ITmfEventProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.filter.ITmfFilter;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypePreferences;
import org.eclipse.tracecompass.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfTraceIndexer;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;

/**
 * The event stream structure in TMF. In its basic form, a trace has:
 * <ul>
 * <li>an associated Eclipse resource
 * <li>a path to its location on the file system
 * <li>the type of the events it contains
 * <li>the number of events it contains
 * <li>the time range (span) of the events it contains
 * </ul>
 * Concrete ITmfTrace classes have to provide a parameter-less constructor and
 * an initialization method (<i>initTrace</i>) if they are to be opened from the
 * Project View. Also, a validation method (<i>validate</i>) has to be provided
 * to ensure that the trace is of the correct type.
 * <p>
 * A trace can be accessed simultaneously from multiple threads by various
 * application components. To avoid obvious multi-threading issues, the trace
 * uses an ITmfContext as a synchronization aid for its read operations.
 * <p>
 * A proper ITmfContext can be obtained by performing a seek operation on the
 * trace. Seek operations can be performed for a particular event (by rank or
 * timestamp) or for a plain trace location.
 * <p>
 * <b>Example 1</b>: Process a whole trace
 *
 * <pre>
 * ITmfContext context = trace.seekEvent(0);
 * ITmfEvent event = trace.getNext(context);
 * while (event != null) {
 *     processEvent(event);
 *     event = trace.getNext(context);
 * }
 * </pre>
 *
 * <b>Example 2</b>: Process 50 events starting from the 1000th event
 *
 * <pre>
 * int nbEventsRead = 0;
 * ITmfContext context = trace.seekEvent(1000);
 * ITmfEvent event = trace.getNext(context);
 * while (event != null && nbEventsRead < 50) {
 *     nbEventsRead++;
 *     processEvent(event);
 *     event = trace.getNext(context);
 * }
 * </pre>
 *
 * <b>Example 3</b>: Process the events between 2 timestamps (inclusive)
 *
 * <pre>
 * ITmfTimestamp startTime = ...;
 * ITmfTimestamp endTime = ...;
 * ITmfContext context = trace.seekEvent(startTime);
 * ITmfEvent event = trace.getNext(context);
 * while (event != null && event.getTimestamp().compareTo(endTime) <= 0) {
 *     processEvent(event);
 *     event = trace.getNext(context);
 * }
 * </pre>
 *
 * A trace is also an event provider so it can process event requests
 * asynchronously (and coalesce compatible, concurrent requests).
 * <p>
 *
 * <b>Example 4</b>: Process a whole trace (see ITmfEventRequest for variants)
 *
 * <pre>
 * ITmfRequest request = new TmfEventRequest&lt;MyEventType&gt;(MyEventType.class) {
 *     &#064;Override
 *     public void handleData(MyEventType event) {
 *         super.handleData(event);
 *         processEvent(event);
 *     }
 *
 *     &#064;Override
 *     public void handleCompleted() {
 *         finish();
 *         super.handleCompleted();
 *     }
 * };
 *
 * fTrace.handleRequest(request);
 * if (youWant) {
 *     request.waitForCompletion();
 * }
 * </pre>
 *
 * @version 1.0
 * @author Francois Chouinard
 *
 * @see ITmfContext
 * @see ITmfEvent
 * @see ITmfTraceIndexer
 * @see ITmfEventParser
 */
public interface ITmfTrace extends ITmfEventProvider {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * The default trace cache size
     */
    public static final int DEFAULT_TRACE_CACHE_SIZE = 1000;

    // ------------------------------------------------------------------------
    // Initializers
    // ------------------------------------------------------------------------

    /**
     * Initialize a newly instantiated "empty" trace object. This is used to
     * properly parameterize an ITmfTrace instantiated with its parameterless
     * constructor.
     * <p>
     * Typically, the parameterless constructor will provide the block size and
     * its associated parser and indexer.
     *
     * @param resource
     *            the trace resource
     * @param path
     *            the trace path. The path should suitable for passing to
     *            <code>java.io.File(String)</code> and should use the
     *            platform-dependent path separator.
     * @param type
     *            the trace event type
     * @throws TmfTraceException
     *             If we couldn't open the trace
     */
    void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException;

    /**
     * Initialize a newly instantiated "empty" trace object. This is used to
     * properly parameterize an ITmfTrace instantiated with its parameterless
     * constructor.
     * <p>
     * Typically, the parameterless constructor will provide the block size and
     * its associated parser and indexer.
     *
     * @param resource
     *            the trace resource
     * @param path
     *            the trace path
     * @param type
     *            the trace event type
     * @param name
     *            the trace name
     * @param traceTypeId
     *            the trace type id
     * @throws TmfTraceException
     *             If we couldn't open the trace
     */
    void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type, String name, String traceTypeId) throws TmfTraceException;

    /**
     * Validate that the trace is of the correct type. The implementation should
     * return a TraceValidationStatus to indicate success with a certain level
     * of confidence.
     *
     * @param project
     *            the eclipse project
     * @param path
     *            the trace path. The path should suitable for passing to
     *            <code>java.io.File(String)</code> and should use the
     *            platform-dependent path separator.
     *
     * @return an IStatus object with validation result. Use severity OK to
     *         indicate success.
     * @see TraceValidationStatus
     */
    IStatus validate(IProject project, String path);

    // ------------------------------------------------------------------------
    // Basic getters
    // ------------------------------------------------------------------------

    /**
     * @return the associated trace resource
     */
    IResource getResource();

    /**
     * Get the trace type id
     *
     * @return the trace type id
     */
    @Nullable String getTraceTypeId();

    /**
     * @return the trace path
     */
    String getPath();

    /**
     * @return the trace cache size
     */
    int getCacheSize();

    /**
     * Index the trace. Depending on the trace type, this could be done at the
     * constructor or initTrace phase too, so this could be implemented as a
     * no-op.
     *
     * @param waitForCompletion
     *            Should we block the caller until indexing is finished, or not.
     */
    void indexTrace(boolean waitForCompletion);

    // ------------------------------------------------------------------------
    // Analysis getters
    // ------------------------------------------------------------------------

    /**
     * Returns an analysis module with the given ID.
     *
     * @param id
     *            The analysis module ID
     * @return The {@link IAnalysisModule} object, or null if an analysis with
     *         the given ID does no exist.
     */
    @Nullable IAnalysisModule getAnalysisModule(String id);

    /**
     * Get a list of all analysis modules currently available for this trace.
     *
     * @return An iterable view of the analysis modules
     */
    @NonNull Iterable<@NonNull IAnalysisModule> getAnalysisModules();


    /**
     * Add an analysis module.
     *
     * It will replace a previously added analysis module with the same ID which
     * will be returned. Callers have to call {@link IAnalysisModule#dispose()}
     * and {@link IAnalysisModule#clearPersistentData()}, if required.
     *
     * @param module
     *            the analysis module to add
     * @return the replaced analysis module if it exists or null
     * @throws TmfTraceException
     *             If analysis module cannot be added, for example, a module
     *             with same ID already exists as built-in analysis
     * @since 9.5
     */
    default @Nullable IAnalysisModule addAnalysisModule(@NonNull IAnalysisModule module) throws TmfTraceException {
        throw new TmfTraceException("Not Implemented"); //$NON-NLS-1$
    }

    /**
     * Remove an analysis module that was previously added by calling
     * ({@link #addAnalysisModule(IAnalysisModule)}.
     *
     * It will return the removed analysis module with the given ID if it
     * exists. Callers have to call {@link IAnalysisModule#dispose()} and
     * {@link IAnalysisModule#clearPersistentData()}, if required.
     *
     * @param id
     *            the ID of the analysis module to remove
     * @return the removed analysis module with the given ID or null if
     *         it doesn't exist.
     * @throws TmfTraceException
     *             If analysis module cannot be removed, for example, a module
     *             with same ID already exists as built-in analysis
     * @since 9.5
     */
    default @Nullable IAnalysisModule removeAnalysisModule(@NonNull String id) throws TmfTraceException {
        throw new TmfTraceException("Not Implemented"); //$NON-NLS-1$
    }

    /**
     * Refresh the analysis modules for this traces
     *
     * @return An IStatus indicating whether the analysis could be run
     *         successfully or not
     * @since 9.4
     */
    default IStatus refreshAnalysisModules() {
        return Status.OK_STATUS;
    }

    // ------------------------------------------------------------------------
    // Aspect getters
    // ------------------------------------------------------------------------

    /**
     * Return the pre-defined set of event aspects exposed by this trace.
     *
     * It should not be null, but could be empty. You are suggested to use at
     * least the ones defined in {@link TmfTrace#BASE_ASPECTS}.
     *
     * @return The event aspects for this trace
     */
    @NonNull Iterable<@NonNull ITmfEventAspect<?>> getEventAspects();

    // ------------------------------------------------------------------------
    // Trace characteristics getters
    // ------------------------------------------------------------------------

    /**
     * @return the number of events in the trace
     */
    long getNbEvents();

    /**
     * @return the trace time range
     */
    @NonNull TmfTimeRange getTimeRange();

    /**
     * @return the timestamp of the first trace event
     */
    @NonNull ITmfTimestamp getStartTime();

    /**
     * @return the timestamp of the last trace event
     */
    @NonNull ITmfTimestamp getEndTime();

    /**
     * @return the streaming interval in ms (0 if not a streaming trace)
     */
    long getStreamingInterval();

    // ------------------------------------------------------------------------
    // Trace positioning getters
    // ------------------------------------------------------------------------

    /**
     * @return the current trace location
     */
    ITmfLocation getCurrentLocation();

    /**
     * Returns the ratio (proportion) corresponding to the specified location.
     *
     * @param location
     *            a trace specific location
     * @return a floating-point number between 0.0 (beginning) and 1.0 (end)
     */
    double getLocationRatio(ITmfLocation location);

    // ------------------------------------------------------------------------
    // SeekEvent operations (returning a trace context)
    // ------------------------------------------------------------------------

    /**
     * Position the trace at the specified (trace specific) location.
     * <p>
     * A null location is interpreted as seeking for the first event of the
     * trace.
     * <p>
     * If not null, the location requested must be valid otherwise the returned
     * context is undefined (up to the implementation to recover if possible).
     * <p>
     *
     * @param location
     *            the trace specific location
     * @return a context which can later be used to read the corresponding event
     */
    ITmfContext seekEvent(ITmfLocation location);

    /**
     * Position the trace at the 'rank'th event in the trace.
     * <p>
     * A rank <= 0 is interpreted as seeking for the first event of the trace.
     * <p>
     * If the requested rank is beyond the last trace event, the context
     * returned will yield a null event if used in a subsequent read.
     *
     * @param rank
     *            the event rank
     * @return a context which can later be used to read the corresponding event
     */
    ITmfContext seekEvent(long rank);

    /**
     * Position the trace at the first event with the specified timestamp. If
     * there is no event with the requested timestamp, a context pointing to the
     * next chronological event is returned.
     * <p>
     * A null timestamp is interpreted as seeking for the first event of the
     * trace.
     * <p>
     * If the requested timestamp is beyond the last trace event, the context
     * returned will yield a null event if used in a subsequent read.
     *
     * @param timestamp
     *            the timestamp of desired event
     * @return a context which can later be used to read the corresponding event
     */
    ITmfContext seekEvent(ITmfTimestamp timestamp);

    /**
     * Position the trace at the event located at the specified ratio in the
     * trace file.
     * <p>
     * The notion of ratio (0.0 <= r <= 1.0) is trace specific and left
     * voluntarily vague. Typically, it would refer to the event proportional
     * rank (arguably more intuitive) or timestamp in the trace file.
     *
     * @param ratio
     *            the proportional 'rank' in the trace
     * @return a context which can later be used to read the corresponding event
     */
    ITmfContext seekEvent(double ratio);

    /**
     * Returns the initial range offset
     *
     * @return the initial range offset
     */
    ITmfTimestamp getInitialRangeOffset();

    /**
     * Returns the ID of the host this trace is from. The host ID is not
     * necessarily the hostname, but should be a unique identifier for the
     * machine on which the trace was taken. It can be used to determine if two
     * traces were taken on the exact same machine (timestamp are already
     * synchronized, resources with same id are the same if taken at the same
     * time, etc).
     *
     * @return The host id of this trace
     */
    @NonNull String getHostId();

    // ------------------------------------------------------------------------
    // Timestamp transformation functions
    // ------------------------------------------------------------------------

    /**
     * Returns the timestamp transformation for this trace
     *
     * @return the timestamp transform
     */
    ITmfTimestampTransform getTimestampTransform();

    /**
     * Sets the trace's timestamp transform
     *
     * @param tt
     *            The timestamp transform for all timestamps of this trace
     */
    void setTimestampTransform(final ITmfTimestampTransform tt);

    /**
     * Creates a timestamp for this trace, using the transformation formula
     *
     * @param ts
     *            The time in nanoseconds with which to create the timestamp
     * @return The new timestamp
     */
    @NonNull ITmfTimestamp createTimestamp(long ts);

    /**
     * Build a new trace context.
     *
     * @param selection
     *            The selected time range
     * @param windowRange
     *            The visible window's time range
     * @param editorFile
     *            The file representing the selected editor
     * @param filter
     *            The currently applied filter. 'null' for none.
     * @return The newly created context
     * @since 2.0
     */
    default @NonNull TmfTraceContext createTraceContext(@NonNull TmfTimeRange selection, @NonNull TmfTimeRange windowRange, @Nullable IFile editorFile, @Nullable ITmfFilter filter) {
        return new TmfTraceContext(selection, windowRange, editorFile, filter);
    }

    /**
     * Read the start time of the trace quickly, without requiring it to be
     * indexed.
     *
     * @return the trace's start time. Null if the trace is empty or failed.
     * @since 3.0
     */
    default ITmfTimestamp readStart() {
        ITmfContext context = seekEvent(0L);
        ITmfEvent event = getNext(context);
        context.dispose();
        return (event != null) ? event.getTimestamp() : null;
    }

    /**
     * Read the end time of the trace quickly, without requiring it to be
     * indexed.
     *
     * @return the trace's end time. Null if the trace is empty or failed.
     * @since 3.0
     */
    default ITmfTimestamp readEnd() {
        /*
         * Subclasses should override this method when a faster implementation
         * is possible.
         */
        indexTrace(true);
        return (getNbEvents() != 0) ? getEndTime() : null;
    }

    /**
     * Get the trace {@link UUID}, a unique identifier that should always be the
     * same if the trace and the children do not change.
     *
     * The default implementation relies on the trace name, the length of the file
     * and the last modified date.
     *
     * @return A {@link UUID} that when it changes is a good indication that the
     *         trace files have changed. An unchanged {@link UUID} may or may not
     *         indicate that the trace has changed.
     * @since 3.1
     */
    default @Nullable UUID getUUID() {
        /*
         * Best effort get UUID, if an implementation has more info, it should override
         * this.
         */
        StringBuilder sb = new StringBuilder();
        File file = new File(getPath());
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                sb.append(childFile.getName());
                sb.append(childFile.length());
                sb.append(childFile.lastModified());
            }
        } else if (file.exists()) {
            sb.append(file.getName());
            sb.append(file.length());
            sb.append(file.lastModified());
        }
        for (ITmfEventProvider child : getChildren()) {
            if (child instanceof ITmfTrace) {
                sb.append(String.valueOf(((ITmfTrace) child).getUUID()));
            }
        }
        return UUID.nameUUIDFromBytes(Objects.requireNonNull(sb.toString().getBytes(Charset.defaultCharset())));
    }

    /**
     * Get initial time range
     *
     * @return get the initial time range
     * @since 3.2
     */
    public default TmfTimeRange getInitialTimeRange() {
        ITmfTimestamp startTime = getStartTime();
        String traceTypeId = getTraceTypeId();
        long initialTimeRange = traceTypeId != null ? TraceTypePreferences.getInitialTimeRange(traceTypeId, getInitialRangeOffset().toNanos()) : getInitialRangeOffset().toNanos();
        return new TmfTimeRange(startTime, TmfTimestamp.fromNanos(SaturatedArithmetic.add(startTime.toNanos(), initialTimeRange)));    }

    /**
     * Get the indexing status of trace
     *
     * @return True if the trace is still being indexed, false if not
     * @since 5.0
     */
    public default boolean isIndexing() {
        return false;
    }
}
