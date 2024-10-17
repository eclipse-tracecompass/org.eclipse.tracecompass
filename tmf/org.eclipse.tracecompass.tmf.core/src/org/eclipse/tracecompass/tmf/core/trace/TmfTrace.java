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
 *   Patrick Tasse - Updated for removal of context clone
 *   Geneviève Bastien  - Added timestamp transforms, its saving to file and
 *                        timestamp creation functions
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.trace;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.internal.util.ByteBufferTracker;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModuleHelper;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAnalysisManager;
import org.eclipse.tracecompass.tmf.core.component.ITmfEventProvider;
import org.eclipse.tracecompass.tmf.core.component.TmfEventProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfLostEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.synchronization.ITmfTimestampTransform;
import org.eclipse.tracecompass.tmf.core.synchronization.TimestampTransformFactory;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfTraceIndexer;
import org.eclipse.tracecompass.tmf.core.trace.indexer.checkpoint.TmfCheckpointIndexer;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;

import com.google.common.collect.ImmutableList;

/**
 * Abstract implementation of ITmfTrace.
 * <p>
 * Since the concept of 'location' is trace specific, the concrete classes have
 * to provide the related methods, namely:
 * <ul>
 * <li> public ITmfLocation<?> getCurrentLocation()
 * <li> public double getLocationRatio(ITmfLocation<?> location)
 * <li> public ITmfContext seekEvent(ITmfLocation<?> location)
 * <li> public ITmfContext seekEvent(double ratio)
 * <li> public IStatus validate(IProject project, String path)
 * </ul>
 * <p>
 * When constructing an event, the concrete trace should use the trace's
 * timestamp transform to create the timestamp, by either transforming the
 * parsed time value directly or by using the method createTimestamp().
 * <p>
 * The concrete class can either specify its own indexer or use the provided
 * TmfCheckpointIndexer (default). In this case, the trace cache size will be
 * used as checkpoint interval.
 *
 * @version 1.0
 * @author Francois Chouinard
 *
 * @see ITmfEvent
 * @see ITmfTraceIndexer
 * @see ITmfEventParser
 */
public abstract class TmfTrace extends TmfEventProvider implements ITmfTrace, ITmfEventParser, ITmfTraceCompleteness, IAdaptable {

    // ------------------------------------------------------------------------
    // Class attributes
    // ------------------------------------------------------------------------

    /**
     * Basic aspects that should be valid for all trace types.
     */
    public static final @NonNull Collection<@NonNull ITmfEventAspect<?>> BASE_ASPECTS =
            ImmutableList.of(
                    TmfBaseAspects.getTimestampAspect(),
                    TmfBaseAspects.getEventTypeAspect(),
                    TmfBaseAspects.getContentsAspect()
                    );

    // ------------------------------------------------------------------------
    // Instance attributes
    // ------------------------------------------------------------------------

    // The resource used for persistent properties for this trace
    private IResource fResource;

    // The trace type id
    private @Nullable String fTraceTypeId;

    // The trace path
    private String fPath;

    // The trace cache page size
    private int fCacheSize = ITmfTrace.DEFAULT_TRACE_CACHE_SIZE;

    // The number of events collected (so far)
    private volatile long fNbEvents = 0;

    // The time span of the event stream
    private @NonNull ITmfTimestamp fStartTime = TmfTimestamp.BIG_BANG;
    private @NonNull ITmfTimestamp fEndTime = TmfTimestamp.BIG_BANG;

    // The trace streaming interval (0 = no streaming)
    private long fStreamingInterval = 0;

    // The trace indexer
    private ITmfTraceIndexer fIndexer;

    private ITmfTimestampTransform fTsTransform;

    private final Map<String, IAnalysisModule> fAnalysisModules =
            Collections.synchronizedMap(new LinkedHashMap<String, IAnalysisModule>());

    private final Map<String, IAnalysisModule> fAddedAnalysisModules =
            Collections.synchronizedMap(new LinkedHashMap<String, IAnalysisModule>());

    // Analysis modules that were removed during lifecycle of the trace that need to be disposed
    private final Set<IAnalysisModule> fToBeDisposedAnalysisModules =
            Collections.synchronizedSet(new HashSet<IAnalysisModule>());

    // ------------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------------

    /**
     * The default, parameterless, constructor
     */
    public TmfTrace() {
        super();
        fIndexer = new TmfCheckpointIndexer(this);
    }

    /**
     * Full constructor.
     *
     * @param resource
     *            The resource associated to the trace
     * @param type
     *            The type of events that will be read from this trace
     * @param path
     *            The path to the trace on the filesystem
     * @param cacheSize
     *            The trace cache size. Pass '-1' to use the default specified
     *            in {@link ITmfTrace#DEFAULT_TRACE_CACHE_SIZE}
     * @param interval
     *            The trace streaming interval. You can use '0' for post-mortem
     *            traces.
     * @throws TmfTraceException
     *             If something failed during the opening
     */
    protected TmfTrace(final IResource resource,
            final Class<? extends ITmfEvent> type,
            final String path,
            final int cacheSize,
            final long interval)
            throws TmfTraceException {
        super();
        fCacheSize = (cacheSize > 0) ? cacheSize : ITmfTrace.DEFAULT_TRACE_CACHE_SIZE;
        fStreamingInterval = interval;
        initialize(resource, path, type);
    }

    /**
     * Copy constructor
     *
     * @param trace the original trace
     * @throws TmfTraceException Should not happen usually
     */
    public TmfTrace(final TmfTrace trace) throws TmfTraceException {
        super();
        if (trace == null) {
            super.dispose();
            throw new IllegalArgumentException();
        }
        fCacheSize = trace.getCacheSize();
        fStreamingInterval = trace.getStreamingInterval();
        initialize(trace.getResource(), trace.getPath(), trace.getEventType());
    }

    /**
     * Creates the indexer instance. Classes extending this class can override
     * this to provide a different indexer implementation.
     *
     * @param interval the checkpoints interval
     *
     * @return the indexer
     */
    protected ITmfTraceIndexer createIndexer(int interval) {
        return new TmfCheckpointIndexer(this, interval);
    }

    // ------------------------------------------------------------------------
    // ITmfTrace - Initializers
    // ------------------------------------------------------------------------

    @Override
    public void initTrace(final IResource resource, final String path, final Class<? extends ITmfEvent> type, String name, String traceTypeId) throws TmfTraceException {
        if (name == null) {
            throw new IllegalArgumentException();
        }
        setName(name);
        fTraceTypeId = traceTypeId;
        initTrace(resource, path, type);
    }

    @Override
    public void initTrace(final IResource resource, final String path, final Class<? extends ITmfEvent> type) throws TmfTraceException {
        initialize(resource, path, type);
    }

    /**
     * Initialize the trace common attributes and the base component.
     *
     * @param resource the Eclipse resource (trace)
     * @param path the trace path
     * @param type the trace event type
     *
     * @throws TmfTraceException If something failed during the initialization
     */
    protected void initialize(final IResource resource,
            final String path,
            final Class<? extends ITmfEvent> type)
                    throws TmfTraceException {
        if (path == null) {
            dispose();
            throw new TmfTraceException("Invalid trace path"); //$NON-NLS-1$
        }
        fPath = path;
        fResource = resource;
        String traceName = getName();
        if (traceName.isEmpty()) {
            traceName = (resource != null) ? resource.getName() : new Path(path).lastSegment();
        }
        super.init(traceName, type);
        // register as VIP after super.init() because TmfComponent registers to signal manager there
        TmfSignalManager.registerVIP(this);
        if (fIndexer != null) {
            fIndexer.dispose();
        }
        fIndexer = createIndexer(fCacheSize);
    }

    /**
     * Indicates if the path points to an existing file/directory
     *
     * @param path the path to test
     * @return true if the file/directory exists
     */
    protected boolean fileExists(final String path) {
        final File file = new File(path);
        return file.exists();
    }

    @Override
    public void indexTrace(boolean waitForCompletion) {
        getIndexer().buildIndex(0, TmfTimeRange.ETERNITY, waitForCompletion);
    }

    /**
     * Instantiate the applicable analysis modules and executes the analysis
     * modules that are meant to be automatically executed
     *
     * @return An IStatus indicating whether the analysis could be run
     *         successfully or not
     */
    protected IStatus executeAnalysis() {
        return refreshAnalysisModulesImpl();
    }

    @Override
    public IStatus refreshAnalysisModules() {
        return refreshAnalysisModulesImpl();
    }

    @Override
    public IAnalysisModule getAnalysisModule(String analysisId) {
        IAnalysisModule module = fAnalysisModules.get(analysisId);
        if (module != null) {
            return module;
        }
        return fAddedAnalysisModules.get(analysisId);
    }


    @Override
    public Iterable<IAnalysisModule> getAnalysisModules() {
        synchronized (fAnalysisModules) {
            HashSet<IAnalysisModule> modules = new HashSet<>(fAnalysisModules.values());
            modules.addAll(fAddedAnalysisModules.values());
            return modules;
        }
    }

    @Override
    public IAnalysisModule addAnalysisModule(@NonNull IAnalysisModule module) throws TmfTraceException {
        synchronized (fAnalysisModules) {
            if (fAnalysisModules.get(module.getId()) != null) {
                throw new TmfTraceException("Can't add analysis with same ID as built-in analysis: " + module.getId()); //$NON-NLS-1$
            }
            return fAddedAnalysisModules.put(module.getId(), module);
        }
    }

    @Override
    public IAnalysisModule removeAnalysisModule(@NonNull String id) throws TmfTraceException {
        synchronized (fAnalysisModules) {
            if (fAnalysisModules.get(id) != null) {
                throw new TmfTraceException("Can't remove built-in analysis with ID: " + id); //$NON-NLS-1$
            }
            return fAddedAnalysisModules.remove(id);
        }
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        /* By default we provide only the base aspects valid for all trace types */
        return BASE_ASPECTS;
    }

    /**
     * Clears the trace
     */
    @Override
    public synchronized void dispose() {
        /* Clean up the index if applicable */
        if (getIndexer() != null) {
            getIndexer().dispose();
        }

        /* Clean up the analysis modules */
        Iterable<IAnalysisModule> analysisModules = getAnalysisModules();
        for (IAnalysisModule module : analysisModules) {
            module.dispose();
        }
        fAnalysisModules.clear();
        fAddedAnalysisModules.clear();

        /* Clean up the analysis modules removed during lifecycle of trace */
        analysisModules = fToBeDisposedAnalysisModules;
        for (IAnalysisModule module : analysisModules) {
            module.dispose();
        }
        fToBeDisposedAnalysisModules.clear();

        super.dispose();
        ByteBufferTracker.setMarked();
    }

    // ------------------------------------------------------------------------
    // ITmfTrace - Basic getters
    // ------------------------------------------------------------------------

    @Override
    public IResource getResource() {
        return fResource;
    }

    @Override
    public @Nullable String getTraceTypeId() {
        return fTraceTypeId;
    }

    @Override
    public String getPath() {
        return fPath;
    }

    @Override
    public int getCacheSize() {
        return fCacheSize;
    }

    @Override
    public long getStreamingInterval() {
        return fStreamingInterval;
    }

    /**
     * @return the trace indexer
     */
    protected ITmfTraceIndexer getIndexer() {
        return fIndexer;
    }

    // ------------------------------------------------------------------------
    // ITmfTrace - Trace characteristics getters
    // ------------------------------------------------------------------------

    @Override
    public long getNbEvents() {
        return fNbEvents;
    }

    @Override
    public @NonNull TmfTimeRange getTimeRange() {
        return new TmfTimeRange(fStartTime, fEndTime);
    }

    @Override
    public ITmfTimestamp getStartTime() {
        return fStartTime;
    }

    @Override
    public ITmfTimestamp getEndTime() {
        return fEndTime;
    }

    @Override
    public ITmfTimestamp getInitialRangeOffset() {
        final long DEFAULT_INITIAL_OFFSET_VALUE = (1L * 100 * 1000 * 1000); // .1sec
        return TmfTimestamp.fromNanos(DEFAULT_INITIAL_OFFSET_VALUE);
    }

    @Override
    public String getHostId() {
        return this.getName();
    }

    @Override
    public boolean isIndexing() {
        return fIndexer.isIndexing();
    }

    // ------------------------------------------------------------------------
    // Convenience setters
    // ------------------------------------------------------------------------

    /**
     * Set the trace cache size. Must be done at initialization time.
     *
     * @param cacheSize The trace cache size
     */
    protected void setCacheSize(final int cacheSize) {
        fCacheSize = cacheSize;
    }

    /**
     * Set the trace known number of events. This can be quite dynamic
     * during indexing or for live traces.
     *
     * @param nbEvents The number of events
     */
    protected synchronized void setNbEvents(final long nbEvents) {
        fNbEvents = (nbEvents > 0) ? nbEvents : 0;
    }

    /**
     * Update the trace events time range
     *
     * @param range the new time range
     */
    protected void setTimeRange(final @NonNull TmfTimeRange range) {
        fStartTime = range.getStartTime();
        fEndTime = range.getEndTime();
    }

    /**
     * Update the trace chronologically first event timestamp
     *
     * @param startTime the new first event timestamp
     */
    protected void setStartTime(final @NonNull ITmfTimestamp startTime) {
        fStartTime = startTime;
    }

    /**
     * Update the trace chronologically last event timestamp
     *
     * @param endTime the new last event timestamp
     */
    protected void setEndTime(final @NonNull ITmfTimestamp endTime) {
        fEndTime = endTime;
    }

    /**
     * Set the polling interval for live traces (default = 0 = no streaming).
     *
     * @param interval the new trace streaming interval
     */
    protected void setStreamingInterval(final long interval) {
        fStreamingInterval = (interval > 0) ? interval : 0;
    }

    // ------------------------------------------------------------------------
    // ITmfTrace - SeekEvent operations (returning a trace context)
    // ------------------------------------------------------------------------

    @Override
    public synchronized ITmfContext seekEvent(final long rank) {

        // A rank <= 0 indicates to seek the first event
        if (rank <= 0) {
            ITmfContext context = seekEvent((ITmfLocation) null);
            context.setRank(0);
            return context;
        }

        // Position the trace at the checkpoint
        final ITmfContext context = fIndexer.seekIndex(rank);

        // And locate the requested event context
        long pos = context.getRank();
        if (pos < rank) {
            ITmfEvent event = getNext(context);
            while ((event != null) && (++pos < rank)) {
                event = getNext(context);
            }
        }
        return context;
    }

    @Override
    public synchronized ITmfContext seekEvent(final ITmfTimestamp timestamp) {

        // A null timestamp indicates to seek the first event
        if (timestamp == null) {
            ITmfContext context = seekEvent((ITmfLocation) null);
            context.setRank(0);
            return context;
        }

        // Position the trace at the checkpoint
        ITmfContext context = fIndexer.seekIndex(timestamp);

        // And locate the requested event context
        ITmfLocation previousLocation = context.getLocation();
        long previousRank = context.getRank();
        ITmfEvent event = getNext(context);
        while (event != null && event.getTimestamp().compareTo(timestamp) < 0) {
            previousLocation = context.getLocation();
            previousRank = context.getRank();
            event = getNext(context);
        }
        if (event == null) {
            context.setLocation(null);
            context.setRank(ITmfContext.UNKNOWN_RANK);
        } else {
            context.dispose();
            context = seekEvent(previousLocation);
            context.setRank(previousRank);
        }
        return context;
    }

    // ------------------------------------------------------------------------
    // Read operations (returning an actual event)
    // ------------------------------------------------------------------------

    @Override
    public abstract ITmfEvent parseEvent(ITmfContext context);

    @Override
    public synchronized ITmfEvent getNext(final ITmfContext context) {
        // parseEvent() does not update the context
        final ITmfEvent event = parseEvent(context);
        if (event != null) {
            updateAttributes(context, event);
            context.setLocation(getCurrentLocation());
            context.increaseRank();
        }
        return event;
    }

    /**
     * Update the trace attributes
     *
     * @param context the current trace context
     * @param event the corresponding event
     * @since 1.1
     */
    protected synchronized void updateAttributes(final ITmfContext context, final @NonNull ITmfEvent event) {
        ITmfTimestamp timestamp = event.getTimestamp();
        ITmfTimestamp endTime = timestamp;
        if (event instanceof ITmfLostEvent) {
            endTime = ((ITmfLostEvent) event).getTimeRange().getEndTime();
        }
        if (fStartTime.equals(TmfTimestamp.BIG_BANG) || (fStartTime.compareTo(timestamp) > 0)) {
            fStartTime = timestamp;
        }
        if (fEndTime.equals(TmfTimestamp.BIG_CRUNCH) || (fEndTime.compareTo(endTime) < 0)) {
            fEndTime = endTime;
        }
        if (context.hasValidRank()) {
            long rank = context.getRank();
            if (fNbEvents <= rank) {
                fNbEvents = rank + 1;
            }
            if (fIndexer != null) {
                fIndexer.updateIndex(context, timestamp);
            }
        }
    }

    // ------------------------------------------------------------------------
    // TmfDataProvider
    // ------------------------------------------------------------------------

    @Override
    public synchronized ITmfContext armRequest(final ITmfEventRequest request) {
        if (executorIsShutdown()) {
            return null;
        }
        if (!TmfTimestamp.BIG_BANG.equals(request.getRange().getStartTime())
                && (request.getIndex() == 0)) {
            final ITmfContext context = seekEvent(request.getRange().getStartTime());
            request.setStartIndex((int) context.getRank());
            return context;

        }
        return seekEvent(request.getIndex());
    }

    // ------------------------------------------------------------------------
    // Signal handlers
    // ------------------------------------------------------------------------

    /**
     * Handler for the Trace Opened signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        boolean signalIsForUs = false;

        ITmfEventProvider provider = this;
        while (provider != null) {
            if (provider == signal.getTrace()) {
                signalIsForUs = true;
                break;
            }
            provider = provider.getParent();
        }

        if (!signalIsForUs) {
            return;
        }

        /*
         * The signal is either for this trace, or for a parent of this trace.
         */
        IStatus status = executeAnalysis();
        if (!status.isOK()) {
            Activator.log(status);
        }

        /* Refresh supplementary files in separate thread to prevent deadlock */
        new Thread("Refresh supplementary files") { //$NON-NLS-1$
            @Override
            public void run() {
                TmfTraceManager.refreshSupplementaryFiles(TmfTrace.this);
            }
        }.start();

        if (signal.getTrace() == this) {
            /* Additionally, the signal is directly for this trace. */
            if (getNbEvents() == 0) {
                return;
            }

            /* For a streaming trace, the range updated signal should be sent
             * by the subclass when a new safe time is determined.
             */
            if (getStreamingInterval() > 0) {
                return;
            }

            if (isComplete()) {
                final TmfTimeRange timeRange = new TmfTimeRange(getStartTime(), TmfTimestamp.BIG_CRUNCH);
                final TmfTraceRangeUpdatedSignal rangeUpdatedsignal = new TmfTraceRangeUpdatedSignal(this, this, timeRange);

                // Broadcast in separate thread to prevent deadlock
                broadcastAsync(rangeUpdatedsignal);
            }
            return;
        }
    }

    /**
     * Signal handler for the TmfTraceRangeUpdatedSignal signal
     *
     * @param signal The incoming signal
     */
    @TmfSignalHandler
    public void traceRangeUpdated(final TmfTraceRangeUpdatedSignal signal) {
        if (signal.getTrace() == this) {
            getIndexer().buildIndex(getNbEvents(), signal.getRange(), false);
        }
    }

    /**
     * Signal handler for the TmfTraceUpdatedSignal signal
     *
     * @param signal The incoming signal
     */
    @TmfSignalHandler
    public void traceUpdated(final TmfTraceUpdatedSignal signal) {
        if (signal.getSource() == getIndexer()) {
            fNbEvents = signal.getNbEvents();
            fStartTime = signal.getRange().getStartTime();
            fEndTime = signal.getRange().getEndTime();
        }
    }

    // ------------------------------------------------------------------------
    // Timestamp transformation functions
    // ------------------------------------------------------------------------

    @Override
    public ITmfTimestampTransform getTimestampTransform() {
        if (fTsTransform == null) {
            fTsTransform = TimestampTransformFactory.getTimestampTransform(getResource());
        }
        return fTsTransform;
    }

    @Override
    public void setTimestampTransform(final ITmfTimestampTransform tt) {
        fTsTransform = tt;
        TimestampTransformFactory.setTimestampTransform(getResource(), tt);
    }

    @Override
    public @NonNull ITmfTimestamp createTimestamp(long ts) {
        return TmfTimestamp.fromNanos(getTimestampTransform().transform(ts));
    }

    // ------------------------------------------------------------------------
    // IAdaptable
    // ------------------------------------------------------------------------

    //TODO: Move to ITmfTrace as default method when Bug 507246 is fixed

    /**
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        List<T> adapters = TmfTraceAdapterManager.getAdapters(this, adapter);
        if (!adapters.isEmpty()) {
            return adapters.get(0);
        }
        if (adapter.isInstance(this)) {
            return (T) this;
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // toString
    // ------------------------------------------------------------------------

    @Override
    @SuppressWarnings("nls")
    public synchronized String toString() {
        return "TmfTrace [fPath=" + fPath + ", fCacheSize=" + fCacheSize
                + ", fNbEvents=" + fNbEvents + ", fStartTime=" + fStartTime
                + ", fEndTime=" + fEndTime + ", fStreamingInterval=" + fStreamingInterval + "]";
    }

    @Override
    public boolean isComplete() {
        /*
         * Be default, all traces are "complete" which means no more data will
         * be added later
         */
        return true;
    }

    @Override
    public void setComplete(boolean isComplete) {
        /*
         * This should be overridden by trace classes that can support live
         * reading (traces in an incomplete state)
         */
    }

    private IStatus refreshAnalysisModulesImpl() {
        Map<String, IAnalysisModule> previousAnalysisModules = new HashMap<>(fAnalysisModules);
        Map<String, IAnalysisModule> newAnalysisModules = new HashMap<>();
        MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, IStatus.OK, null, null);
        /* First modules are initialized */
        Map<String, IAnalysisModuleHelper> modules = TmfAnalysisManager.getAnalysisModules(this.getClass());
        for (IAnalysisModuleHelper helper : modules.values()) {
            try {
                IAnalysisModule module = helper.newModule(this);
                if (module == null) {
                    continue;
                }
                newAnalysisModules.put(module.getId(), module);
            } catch (TmfAnalysisException e) {
                status.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, e.getMessage()));
            }
        }

        Set<String> oldAnalysisModulesKeys = new HashSet<>(previousAnalysisModules.keySet());
        Set<String> keys = new HashSet<>(newAnalysisModules.keySet());

        for (String key : keys) {
            IAnalysisModule module = newAnalysisModules.remove(key);
            if (!oldAnalysisModulesKeys.contains(key)) {
                /* Once all modules are initialized, automatic modules are executed */
                if (module != null && module.isAutomatic()) {
                    status.add(module.schedule());
                }
                previousAnalysisModules.put(key, module);
            } else {
                oldAnalysisModulesKeys.remove(key);
                if (module != null) {
                    module.dispose();
                }
            }
        }

        /* Remove all remaining analysis modules */
        for (String key : oldAnalysisModulesKeys) {
            IAnalysisModule analysisModule = previousAnalysisModules.remove(key);
            if (analysisModule != null) {
                fToBeDisposedAnalysisModules.add(analysisModule);
            }
        }
        /* Update fAnalysis with current list of analysis modules */
        synchronized (fAnalysisModules) {
            fAnalysisModules.clear();
            fAnalysisModules.putAll(previousAnalysisModules);
        }
        return status;
    }
}
