/*******************************************************************************
 * Copyright (c) 2009, 2019 Ericsson, École Polytechnique de Montréal
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
 *   Patrick Tasse - Updated for removal of context clone
 *   Patrick Tasse - Updated for ranks in experiment location
 *   Geneviève Bastien - Added support of experiment synchronization
 *                       Added the initExperiment method and default constructor
 *   Bernd Hufmann - Updated for added interfaces to ITmfEventProvider
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.trace.experiment;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.synchronization.TmfTimestampTransform;
import org.eclipse.tracecompass.internal.tmf.core.trace.experiment.TmfExperimentContext;
import org.eclipse.tracecompass.internal.tmf.core.trace.experiment.TmfExperimentContext.ContextTuple;
import org.eclipse.tracecompass.internal.tmf.core.trace.experiment.TmfExperimentLocation;
import org.eclipse.tracecompass.internal.tmf.core.trace.experiment.TmfLocationArray;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSynchronizedSignal;
import org.eclipse.tracecompass.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.tracecompass.tmf.core.synchronization.SynchronizationAlgorithm;
import org.eclipse.tracecompass.tmf.core.synchronization.SynchronizationManager;
import org.eclipse.tracecompass.tmf.core.synchronization.TimestampTransformFactory;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfPersistentlyIndexable;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfTraceIndexer;
import org.eclipse.tracecompass.tmf.core.trace.indexer.TmfBTreeTraceIndexer;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * TmfExperiment presents a time-ordered, unified view of a set of ITmfTrace:s
 * that are part of a tracing experiment.
 *
 * @version 1.0
 * @author Francois Chouinard
 */
public class TmfExperiment extends TmfTrace implements ITmfPersistentlyIndexable {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * The file name of the Synchronization
     */
    private static final String SYNCHRONIZATION_FILE_NAME = "synchronization.bin"; //$NON-NLS-1$

    /**
     * The name of the directory containing trace synchronization data. This
     * directory typically will be preserved when traces are synchronized.
     * Analysis involved in synchronization can put their supplementary files in
     * there so they are not deleted when synchronized traces are copied.
     */
    private static final String SYNCHRONIZATION_DIRECTORY = "sync_data"; //$NON-NLS-1$

    /**
     * The default index page size
     */
    public static final int DEFAULT_INDEX_PAGE_SIZE = 5000;

    /**
     * Property name for traces defining a clock offset.
     */
    private static final String CLOCK_OFFSET_PROPERTY = "clock_offset"; //$NON-NLS-1$

    /**
     * If the automatic clock offset is higher than this value, emit a warning.
     */
    private static final long CLOCK_OFFSET_THRESHOLD_NS = 500000;

    /**
     * The default confidence for the generic Tmf experiment
     *
     * @since 9.1
     */
    protected static final int DEFAULT_GENERIC_EXPERIMENT_CONFIDENCE = 1;

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * The set of traces that constitute the experiment
     */
    private boolean fInitialized = false;

    /**
     * Lock for synchronization methods. These methods cannot be 'synchronized'
     * since it makes it impossible to use an event request on the experiment
     * during synchronization (the request thread would block)
     */
    private final Lock fSyncLock = new ReentrantLock();

    // ------------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------------

    /**
     * Default constructor. Should not be used directly, but is needed for
     * extension points.
     *
     * Do not call this directly (but do not remove it either!)
     */
    public TmfExperiment() {
        super();
    }

    /**
     * Constructor of an experiment, taking the type, path, traces,
     * indexPageSize and resource
     *
     * @param type
     *            The event type
     * @param path
     *            The experiment path
     * @param traces
     *            The experiment set of traces
     * @param indexPageSize
     *            The experiment index page size. You can use
     *            {@link TmfExperiment#DEFAULT_INDEX_PAGE_SIZE} for a default
     *            value.
     * @param resource
     *            The resource associated to the experiment. You can use 'null'
     *            for no resources (tests, etc.)
     */
    public TmfExperiment(final Class<? extends ITmfEvent> type,
            final String path,
            final ITmfTrace[] traces,
            final int indexPageSize,
            final @Nullable IResource resource) {
        initExperiment(type, path, traces, indexPageSize, resource, TmfTraceType.DEFAULT_EXPERIMENT_TYPE);
    }

    @Override
    protected ITmfTraceIndexer createIndexer(int interval) {
        if (getCheckpointSize() > 0) {
            return new TmfBTreeTraceIndexer(this, interval);
        }
        return super.createIndexer(interval);
    }

    /**
     * Clears the experiment
     */
    @Override
    public synchronized void dispose() {

        // Clean up the index if applicable
        if (getIndexer() != null) {
            getIndexer().dispose();
        }

        super.dispose();
    }

    // ------------------------------------------------------------------------
    // ITmfTrace - Initializers
    // ------------------------------------------------------------------------

    @Override
    public void initTrace(final IResource resource, final String path, final Class<? extends ITmfEvent> type) {
        /* Do nothing for experiments */
    }

    /**
     * Initialization of an experiment, taking the type, path, traces,
     * indexPageSize, resource and trace type id
     *
     * @param type
     *            the event type
     * @param path
     *            the experiment path
     * @param traces
     *            the experiment set of traces
     * @param indexPageSize
     *            the experiment index page size
     * @param resource
     *            the resource associated to the experiment
     * @param traceTypeId
     *            the trace type id
     * @since 5.0
     */
    public void initExperiment(final Class<? extends ITmfEvent> type,
            final String path,
            final ITmfTrace[] traces,
            final int indexPageSize,
            final @Nullable IResource resource,
            final String traceTypeId) {
        try {
            super.initTrace(resource, path, type, getName(), traceTypeId);
        } catch (TmfTraceException e) {
            Activator.logError("Error initializing experiment", e); //$NON-NLS-1$
        }
        initExperiment(type, path, traces, indexPageSize, resource);
    }

    /**
     * Initialization of an experiment, taking the type, path, traces,
     * indexPageSize and resource
     *
     * @param type
     *            the event type
     * @param path
     *            the experiment path
     * @param traces
     *            the experiment set of traces
     * @param indexPageSize
     *            the experiment index page size
     * @param resource
     *            the resource associated to the experiment
     */
    public void initExperiment(final Class<? extends ITmfEvent> type,
            final String path,
            final ITmfTrace[] traces,
            final int indexPageSize,
            final @Nullable IResource resource) {
        setCacheSize(indexPageSize);
        setStreamingInterval(0);

        Multimap<String, ITmfTrace> tracesPerHost = HashMultimap.create();

        // traces have to be set before super.initialize()
        if (traces != null) {
            // initialize
            for (ITmfTrace trace : traces) {
                if (trace != null) {
                    tracesPerHost.put(trace.getHostId(), trace);
                    addChild(trace);
                }
            }
        }

        try {
            super.initialize(resource, path, type);
        } catch (TmfTraceException e) {
            Activator.logError("Error initializing experiment", e); //$NON-NLS-1$
        }

        if (resource != null) {
            synchronizeTraces();
        }

        /*
         * For all traces on the same host, if two or more specify different
         * clock offsets, adjust their clock offset by the average of all of
         * them.
         *
         * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=484620
         */
        Function<ITmfPropertiesProvider, @Nullable Long> offsetGetter = trace -> {
            String offset = trace.getProperties().get(CLOCK_OFFSET_PROPERTY);
            if (offset == null) {
                return null;
            }
            try {
                return Long.parseLong(offset);
            } catch (NumberFormatException e) {
                return null;
            }
        };

        for (Collection<ITmfTrace> values : tracesPerHost.asMap().values()) {
            /*
             * Only attempt to synchronize traces that provide a clock_offset
             * property.
             */
            Collection<ITmfPropertiesProvider> tracesToSynchronize = values.stream()
                    .filter(ITmfPropertiesProvider.class::isInstance)
                    .map(ITmfPropertiesProvider.class::cast)
                    .filter(trace -> offsetGetter.apply(trace) != null)
                    .collect(Collectors.toList());

            if (tracesToSynchronize.size() < 2) {
                continue;
            }

            /*
             * Only synchronize traces if they haven't previously been
             * synchronized
             */
            if (tracesToSynchronize.stream()
                    .map(trace -> ((ITmfTrace) trace).getTimestampTransform())
                    .anyMatch(transform -> !transform.equals(TmfTimestampTransform.IDENTITY))) {
                continue;
            }

            /* Calculate the average of all clock offsets */
            BigInteger sum = BigInteger.ZERO;
            for (ITmfPropertiesProvider trace : tracesToSynchronize) {
                long offset = checkNotNull(offsetGetter.apply(trace));
                sum = sum.add(BigInteger.valueOf(offset));
            }
            long average = sum.divide(BigInteger.valueOf(tracesToSynchronize.size())).longValue();

            if (average > CLOCK_OFFSET_THRESHOLD_NS) {
                Activator.logWarning("Average clock correction (" + average + ") is higher than threshold of " + //$NON-NLS-1$ //$NON-NLS-2$
                        CLOCK_OFFSET_THRESHOLD_NS + " ns for experiment " + this.toString()); //$NON-NLS-1$
            }

            /*
             * Apply the offset correction to all identified traces, but only if
             * they do not already have an equivalent one (for example, closing
             * and re-opening the same experiment should not retrigger building
             * all supplementary files).
             */
            tracesToSynchronize.forEach(t -> {
                long offset = checkNotNull(offsetGetter.apply(t));
                long delta = average - offset;

                ITmfTrace trace = (ITmfTrace) t;
                ITmfTimestampTransform currentTransform = trace.getTimestampTransform();
                ITmfTimestampTransform newTransform = TimestampTransformFactory.createWithOffset(delta);

                if (!newTransform.equals(currentTransform)) {
                    TmfTraceManager.deleteSupplementaryFiles(trace);
                    trace.setTimestampTransform(newTransform);
                    if (trace instanceof TmfTrace tmfTrace) {
                        tmfTrace.resetIndexer();
                    }
                }
            });
        }
    }

    @Override
    public IStatus validate(final IProject project, final String path) {
        return Status.OK_STATUS;
    }

    /**
     * Validates the experiment based on the traces provided. All subclasses of
     * TmfExperiment should override this method to compute its own validation.
     * If the experiment type is valid based on the provided traces, the method
     * should return TraceValidationStatus with confidence. If it is not valid,
     * an ERROR status should be returned. Note that the generic Tmf experiment
     * reserves the confidence value of 1, therefore all extending classes
     * should return a confidence level accordingly.
     *
     * @param traces
     *            list of ITmfTraces that is used to validate this experiment
     * @return status an IStatus object with validation result. Use ERROR status
     *         to indicate an error, otherwise use TraceValidationStatus with a
     *         confidence.
     * @since 9.0
     */
    public IStatus validateWithTraces(List<ITmfTrace> traces) {
        if (getClass() == TmfExperiment.class) {
            return new TraceValidationStatus(DEFAULT_GENERIC_EXPERIMENT_CONFIDENCE, Activator.PLUGIN_ID);
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "class extends TmfExperiment"); //$NON-NLS-1$
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Get the traces contained in this experiment.
     *
     * @return The array of contained traces
     */
    public List<@NonNull ITmfTrace> getTraces() {
        return getChildren(ITmfTrace.class);
    }

    /**
     * Returns the timestamp of the event at the requested index. If none,
     * returns null.
     *
     * @param index
     *            the event index (rank)
     * @return the corresponding event timestamp
     */
    public ITmfTimestamp getTimestamp(final int index) {
        final ITmfContext context = seekEvent(index);
        final ITmfEvent event = getNext(context);
        context.dispose();
        return (event != null) ? event.getTimestamp() : null;
    }

    // ------------------------------------------------------------------------
    // Request management
    // ------------------------------------------------------------------------

    @Override
    public synchronized ITmfContext armRequest(final ITmfEventRequest request) {

        // Make sure we have something to read from
        if (getChildren().isEmpty()) {
            return null;
        }

        if (!TmfTimestamp.BIG_BANG.equals(request.getRange().getStartTime())
                && request.getIndex() == 0) {
            final ITmfContext context = seekEvent(request.getRange().getStartTime());
            request.setStartIndex((int) context.getRank());
            return context;

        }

        return seekEvent(request.getIndex());
    }

    // ------------------------------------------------------------------------
    // ITmfTrace trace positioning
    // ------------------------------------------------------------------------

    @Override
    public synchronized ITmfContext seekEvent(final ITmfLocation location) {
        // Validate the location
        if (location != null && !(location instanceof TmfExperimentLocation)) {
            return null; // Throw an exception?
        }

        int length = getNbChildren();

        // Initialize the location array if necessary
        TmfLocationArray locationArray = ((location == null) ? new TmfLocationArray(length) : ((TmfExperimentLocation) location).getLocationInfo());

        ITmfLocation[] locations = locationArray.getLocations();
        long[] ranks = locationArray.getRanks();

        // Create and populate the context's traces contexts
        final TmfExperimentContext context = new TmfExperimentContext(length);

        // Position the traces
        long rank = 0;
        for (int i = 0; i < length; i++) {
            // Get the relevant trace attributes
            final ITmfContext traceContext = ((ITmfTrace) getChild(i)).seekEvent(locations[i]);
            traceContext.setRank(ranks[i]);
            // update location after seek
            locations[i] = traceContext.getLocation();
            context.setContent(i, traceContext, getChild(i).getNext(traceContext));
            rank += ranks[i];
        }

        // Finalize context
        context.setLocation(new TmfExperimentLocation(new TmfLocationArray(locations, ranks)));
        context.setRank(rank);

        return context;
    }

    // ------------------------------------------------------------------------
    // ITmfTrace - SeekEvent operations (returning a trace context)
    // ------------------------------------------------------------------------

    @Override
    public ITmfContext seekEvent(final double ratio) {
        return seekEvent(Math.round(ratio * getNbEvents()));
    }

    @Override
    public double getLocationRatio(final ITmfLocation location) {
        if (location instanceof TmfExperimentLocation) {
            long rank = 0;
            TmfLocationArray locationArray = ((TmfExperimentLocation) location).getLocationInfo();
            for (int i = 0; i < locationArray.size(); i++) {
                rank += locationArray.getRank(i);
            }
            return (double) rank / getNbEvents();
        }
        return 0.0;
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        // never used
        return null;
    }

    @Override
    public @NonNull Iterable<ITmfEventAspect<?>> getEventAspects() {
        List<ITmfTrace> traces = getTraces();
        ImmutableSet.Builder<ITmfEventAspect<?>> builder = new ImmutableSet.Builder<>();

        /* For experiments, we'll add a "trace name" aspect/column */
        builder.add(TmfBaseAspects.getTraceNameAspect());

        String commonTraceType = getCommonTraceType();
        if (commonTraceType != null) {
            /*
             * All the traces in this experiment are of the same type, let's
             * just use the normal table for that type.
             */
            builder.addAll(traces.get(0).getEventAspects());

        } else {
            /*
             * There are different trace types in the experiment, so we are
             * definitely using a TmfEventsTable. Aggregate the columns from all
             * trace types.
             */
            for (ITmfTrace trace : traces) {
                Iterable<ITmfEventAspect<?>> traceAspects = trace.getEventAspects();
                builder.addAll(traceAspects);
            }
        }
        return builder.build();
    }

    /**
     * Check if an experiment contains traces of all the same type. If so,
     * returns this type as a String. If not, returns null.
     *
     * @param experiment
     *            The experiment
     * @return The common trace type if there is one, or 'null' if there are
     *         different types.
     */
    private @Nullable String getCommonTraceType() {
        String commonTraceType = null;
        try {
            for (final ITmfTrace trace : getTraces()) {
                final IResource resource = trace.getResource();
                if (resource == null) {
                    return null;
                }

                final String traceType = resource.getPersistentProperty(TmfCommonConstants.TRACETYPE);
                if ((commonTraceType != null) && !commonTraceType.equals(traceType)) {
                    return null;
                }
                commonTraceType = traceType;
            }
        } catch (CoreException e) {
            /*
             * One of the traces didn't advertise its type, we can't infer
             * anything.
             */
            return null;
        }
        return commonTraceType;
    }

    // ------------------------------------------------------------------------
    // ITmfTrace trace positioning
    // ------------------------------------------------------------------------

    @Override
    public synchronized ITmfEvent parseEvent(final ITmfContext context) {
        final ITmfContext tmpContext = seekEvent(context.getLocation());
        return getNext(tmpContext);
    }

    @Override
    public synchronized ITmfEvent getNext(ITmfContext context) {

        // Make sure that we have something to read from
        if (getNbChildren() == 0) {
            return null;
        }

        // Validate the context
        if (!(context instanceof TmfExperimentContext)) {
            return null; // Throw an exception?
        }
        TmfExperimentContext experimentContext = (TmfExperimentContext) context;

        // Identify the "next" trace to read from
        ContextTuple next = experimentContext.getNext();

        ITmfEvent event = null;
        if (next != null) {
            event = next.getEvent();
            updateAttributes(experimentContext, event);
            experimentContext.increaseRank();

            // Update the experiment location
            ITmfLocation location = experimentContext.getLocation();
            if (location instanceof TmfExperimentLocation) {
                int trace = next.getIndex();
                ITmfContext traceContext = next.getContext();
                TmfLocationArray locationArray = new TmfLocationArray(
                        ((TmfExperimentLocation) location).getLocationInfo(),
                        trace, traceContext.getLocation(), traceContext.getRank());
                experimentContext.setLocation(new TmfExperimentLocation(locationArray));
                // queue the next event
                ITmfEvent nextEvent = getChild(trace).getNext(traceContext);
                experimentContext.setContent(trace, traceContext, nextEvent);
            }
        }

        return event;
    }

    @Override
    public ITmfTimestamp getInitialRangeOffset() {

        List<ITmfTrace> children = getChildren(ITmfTrace.class);

        if (children.isEmpty()) {
            return super.getInitialRangeOffset();
        }

        ITmfTimestamp initTs = TmfTimestamp.BIG_CRUNCH;
        for (ITmfTrace trace : children) {
            ITmfTimestamp ts = (trace).getInitialRangeOffset();
            if (ts.compareTo(initTs) < 0) {
                initTs = ts;
            }
        }
        return initTs;
    }

    /**
     * Get the path to the folder in the supplementary file where
     * synchronization-related data can be kept so they are not deleted when the
     * experiment is synchronized. Analysis involved in synchronization can put
     * their supplementary files in there so they are preserved after
     * synchronization.
     *
     * If the directory does not exist, it will be created. A return value of
     * <code>null</code> means either the trace resource does not exist or
     * supplementary resources cannot be kept.
     *
     * @param absolute
     *            If <code>true</code>, it returns the absolute path in the file
     *            system, including the supplementary file path. Otherwise, it
     *            returns only the directory name.
     * @return The path to the folder where synchronization-related
     *         supplementary files can be kept or <code>null</code> if not
     *         available.
     */
    public String getSynchronizationFolder(boolean absolute) {
        /* Set up the path to the synchronization file we'll use */
        IResource resource = this.getResource();
        String syncDirectory = null;

        try {
            /* get the directory where the file will be stored. */
            if (resource != null) {
                String fullDirectory = resource.getPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER);
                /* Create the synchronization data directory if not present */
                if (fullDirectory != null) {
                    fullDirectory = fullDirectory + File.separator + SYNCHRONIZATION_DIRECTORY;
                    File syncDir = new File(fullDirectory);
                    syncDir.mkdirs();
                }
                if (absolute) {
                    syncDirectory = fullDirectory;
                } else {
                    syncDirectory = SYNCHRONIZATION_DIRECTORY;
                }
            }
        } catch (CoreException e) {
            return null;
        }

        return syncDirectory;
    }

    /**
     * Synchronizes the traces of an experiment. By default it only tries to
     * read a synchronization file if it exists
     *
     * @return The synchronization object
     */
    public SynchronizationAlgorithm synchronizeTraces() {
        return synchronizeTraces(false);
    }

    /**
     * Synchronizes the traces of an experiment.
     *
     * @param doSync
     *            Whether to actually synchronize or just try opening a sync
     *            file
     * @return The synchronization object
     */
    public SynchronizationAlgorithm synchronizeTraces(boolean doSync) {
        fSyncLock.lock();

        try {
            String syncDirectory = getSynchronizationFolder(true);

            final File syncFile = (syncDirectory != null) ? new File(syncDirectory + File.separator + SYNCHRONIZATION_FILE_NAME) : null;

            final SynchronizationAlgorithm syncAlgo = SynchronizationManager.synchronizeTraces(syncFile, Collections.singleton(this), doSync);

            final TmfTraceSynchronizedSignal signal = new TmfTraceSynchronizedSignal(this, syncAlgo);

            /* Broadcast in separate thread to prevent deadlock */
            new Thread() {
                @Override
                public void run() {
                    broadcast(signal);
                }
            }.start();

            return syncAlgo;
        } finally {
            fSyncLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("nls")
    public synchronized String toString() {
        return "[TmfExperiment (" + getName() + ")]";
    }

    // ------------------------------------------------------------------------
    // Streaming support
    // ------------------------------------------------------------------------

    private synchronized void initializeStreamingMonitor() {

        if (fInitialized) {
            return;
        }
        fInitialized = true;

        if (getStreamingInterval() == 0) {
            final ITmfContext context = seekEvent(0);
            final ITmfEvent event = getNext(context);
            context.dispose();
            if (event == null) {
                return;
            }
            final TmfTimeRange timeRange = new TmfTimeRange(event.getTimestamp(), TmfTimestamp.BIG_CRUNCH);
            final TmfTraceRangeUpdatedSignal signal = new TmfTraceRangeUpdatedSignal(this, this, timeRange);

            // Broadcast in separate thread to prevent deadlock
            new Thread() {
                @Override
                public void run() {
                    broadcast(signal);
                }
            }.start();
            return;
        }

        final Thread thread = new Thread("Streaming Monitor for experiment " + getName()) { //$NON-NLS-1$
            private ITmfTimestamp safeTimestamp = null;
            private ITmfTimestamp lastSafeTimestamp = null;

            @Override
            public void run() {
                while (!executorIsShutdown()) {
                    if (!getIndexer().isIndexing()) {
                        ITmfTimestamp startTimestamp = TmfTimestamp.BIG_CRUNCH;
                        ITmfTimestamp endTimestamp = TmfTimestamp.BIG_BANG;

                        for (final ITmfTrace trace : getChildren(ITmfTrace.class)) {
                            if (trace.getStartTime().compareTo(startTimestamp) < 0) {
                                startTimestamp = trace.getStartTime();
                            }
                            if (trace.getStreamingInterval() != 0 && trace.getEndTime().compareTo(endTimestamp) > 0) {
                                endTimestamp = trace.getEndTime();
                            }
                        }
                        TmfTimeRange timeRange = null;
                        ITmfTimestamp safeTs = safeTimestamp;
                        if (safeTs != null && (lastSafeTimestamp == null || safeTs.compareTo(lastSafeTimestamp) > 0)) {
                            timeRange = new TmfTimeRange(startTimestamp, safeTs);
                            lastSafeTimestamp = safeTs;
                        }
                        safeTimestamp = endTimestamp;
                        if (timeRange != null) {
                            final TmfTraceRangeUpdatedSignal signal = new TmfTraceRangeUpdatedSignal(TmfExperiment.this, TmfExperiment.this, timeRange);
                            broadcast(signal);
                        }
                    }
                    try {
                        Thread.sleep(getStreamingInterval());
                    } catch (final InterruptedException e) {
                        // Do nothing
                    }
                }
            }
        };
        thread.start();
    }

    @Override
    public long getStreamingInterval() {
        long interval = 0;
        for (final ITmfTrace trace : getChildren(ITmfTrace.class)) {
            interval = Math.max(interval, trace.getStreamingInterval());
        }
        return interval;
    }

    // ------------------------------------------------------------------------
    // Signal handlers
    // ------------------------------------------------------------------------

    @Override
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        if (signal.getTrace() == this) {
            initializeStreamingMonitor();

            /* Initialize the analysis */
            MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, null, null);
            status.add(executeAnalysis());
            if (!status.isOK()) {
                Activator.log(status);
            }
            /*
             * Refresh supplementary files in separate thread to prevent
             * deadlock
             */
            new Thread("Refresh supplementary files") { //$NON-NLS-1$
                @Override
                public void run() {
                    TmfTraceManager.refreshSupplementaryFiles(TmfExperiment.this);
                }
            }.start();
        }
    }

    @Override
    public synchronized int getCheckpointSize() {
        int totalCheckpointSize = 0;
        try {
            List<ITmfTrace> children = getChildren(ITmfTrace.class);
            for (ITmfTrace trace : children) {
                if (!(trace instanceof ITmfPersistentlyIndexable)) {
                    return 0;
                }

                ITmfPersistentlyIndexable persistableIndexTrace = (ITmfPersistentlyIndexable) trace;
                int currentTraceCheckpointSize = persistableIndexTrace.getCheckpointSize();
                if (currentTraceCheckpointSize <= 0) {
                    return 0;
                }
                totalCheckpointSize += currentTraceCheckpointSize;
                // each entry in the TmfLocationArray has a rank in addition
                // of the location
                totalCheckpointSize += 8;
            }
        } catch (UnsupportedOperationException e) {
            return 0;
        }

        return totalCheckpointSize;
    }

    @Override
    public ITmfLocation restoreLocation(ByteBuffer bufferIn) {
        List<ITmfTrace> children = getChildren(ITmfTrace.class);
        int length = children.size();
        ITmfLocation[] locations = new ITmfLocation[length];
        long[] ranks = new long[length];
        for (int i = 0; i < length; ++i) {
            final ITmfTrace trace = children.get(i);
            locations[i] = ((ITmfPersistentlyIndexable) trace).restoreLocation(bufferIn);
            ranks[i] = bufferIn.getLong();
        }
        TmfLocationArray arr = new TmfLocationArray(locations, ranks);
        return new TmfExperimentLocation(arr);
    }
}
