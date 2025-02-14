/*******************************************************************************
 * Copyright (c) 2013, 2025 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *   Bernd Hufmann - Integrated history builder functionality
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.statesystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.statesystem.backends.partial.PartialHistoryBackend;
import org.eclipse.tracecompass.internal.tmf.core.statesystem.backends.partial.PartialInMemoryBackend;
import org.eclipse.tracecompass.internal.tmf.core.statesystem.backends.partial.PartialStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.backend.ICustomStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.IPartialStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.snapshot.StateSnapshot;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider.FutureEventType;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceCompleteness;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.ScopeLog;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Abstract analysis module to generate a state system. It is a base class that
 * can be used as a shortcut by analysis who just need to build a single state
 * system with a state provider.
 *
 * Analysis implementing this class should only need to provide a state system
 * and optionally a backend (default to NULL) and, if required, a filename
 * (defaults to the analysis'ID)
 *
 * @author Geneviève Bastien
 */
public abstract class TmfStateSystemAnalysisModule extends TmfAbstractAnalysisModule
        implements ITmfAnalysisModuleWithStateSystems {

    private static final Logger LOGGER = TraceCompassLog.getLogger(TmfStateSystemAnalysisModule.class);

    private static final String EXTENSION = ".ht"; //$NON-NLS-1$

    private final CountDownLatch fInitialized = new CountDownLatch(1);
    private final Object fRequestSyncObj = new Object();

    private @Nullable ITmfStateSystemBuilder fStateSystem;
    private @Nullable ITmfEventRequest fRequest;
    private @Nullable TmfTimeRange fTimeRange = null;

    private int fNbRead = 0;
    private boolean fInitializationSucceeded;

    private volatile @Nullable ITmfStateProvider fStateProvider;
    private @Nullable Integer fProviderVersion = null;

    /**
     * State system backend types
     *
     * @author Geneviève Bastien
     */
    protected enum StateSystemBackendType {
        /** Full history in file */
        FULL,
        /** In memory state system */
        INMEM,
        /** Null history */
        NULL,
        /** State system backed with partial history */
        PARTIAL,
        /**
         * Custom backend on its own. If one uses it then they need to override
         * {@link TmfStateSystemAnalysisModule#getCustomBackend(String, ITmfStateProvider)}
         * @since 7.2
         */
        CUSTOM
    }

    /**
     * Retrieve a state system belonging to trace, by passing the ID of the relevant
     * analysis module.
     *
     * This will start the execution of the analysis module, and start the
     * construction of the state system, if needed.
     *
     * @param trace
     *            The trace for which you want the state system
     * @param moduleId
     *            The ID of the state system analysis module
     * @return The state system, or null if there was no match or the module was not
     *         initialized correctly
     */
    public static @Nullable ITmfStateSystem getStateSystem(ITmfTrace trace, String moduleId) {
        TmfStateSystemAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, TmfStateSystemAnalysisModule.class, moduleId);
        if (module != null) {
            ITmfStateSystem ss = module.getStateSystem();
            if (ss != null) {
                return ss;
            }
            IStatus status = module.schedule();
            if (status.isOK()) {
                return module.waitForInitialization() ? module.getStateSystem() : null;
            }
        }
        return null;
    }

    /**
     * Get the state provider for this analysis module
     *
     * @return the state provider
     */
    protected abstract ITmfStateProvider createStateProvider();

    /**
     * Get the state system backend type used by this module
     *
     * @return The {@link StateSystemBackendType}
     */
    protected StateSystemBackendType getBackendType() {
        /* Using full history by default, sub-classes can override */
        return StateSystemBackendType.FULL;
    }

    /**
     * Get the supplementary file name where to save this state system. The default
     * is the ID of the analysis followed by the extension.
     *
     * @return The supplementary file name
     */
    protected String getSsFileName() {
        return getId() + EXTENSION;
    }

    /**
     * Get the state system generated by this analysis, or null if it is not yet
     * created.
     *
     * @return The state system
     */
    @Nullable
    public ITmfStateSystem getStateSystem() {
        return fStateSystem;
    }

    @Override
    public Map<String, Integer> getProviderVersions() {
        Integer providerVersion = fProviderVersion;
        if (providerVersion == null) {
            return Collections.emptyMap();
        }
        ImmutableMap.Builder<String, Integer> builder = new Builder<>();
        for (ITmfStateSystem ss : getStateSystems()) {
            builder.put(ss.getSSID(), providerVersion);
        }
        return builder.build();
    }

    /**
     * @since 2.0
     */
    @Override
    public boolean waitForInitialization() {
        try {
            fInitialized.await();
        } catch (InterruptedException e) {
            return false;
        }
        return fInitializationSucceeded;
    }

    @Override
    protected void onFail() {
        super.onFail();
        // Make sure any analysis waiting for initialization can continue
        if (fInitialized.getCount() > 0) {
            analysisReady(false);
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public boolean isQueryable(long ts) {
        /*
         * Return true if there is no state provider available (the analysis is not
         * being built)
         */
        ITmfStateProvider provider = fStateProvider;
        if (provider == null) {
            return true;
        }
        return ts <= provider.getLatestSafeTime();
    }

    // ------------------------------------------------------------------------
    // TmfAbstractAnalysisModule
    // ------------------------------------------------------------------------

    /**
     * Get the file where to save the results of the analysis
     *
     * @return The file to save the results in
     * @since 2.3
     */
    @VisibleForTesting
    protected @Nullable File getSsFile() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return null;
        }
        String directory = TmfTraceManager.getSupplementaryFileDir(trace);
        File htFile = new File(directory + getSsFileName());
        return htFile;
    }

    @Override
    protected boolean executeAnalysis(@Nullable final IProgressMonitor monitor) {
        IProgressMonitor mon = (monitor == null ? new NullProgressMonitor() : monitor);
        final ITmfStateProvider provider = createStateProvider();
        fProviderVersion = provider.getVersion();

        String id = getId();

        /*
         * FIXME: State systems should make use of the monitor, to be cancelled
         */
        try (ScopeLog log = new ScopeLog(LOGGER, Level.FINE, "StateSystemAnalysis:executing", "id", id)) { //$NON-NLS-1$ //$NON-NLS-2$
            /* Get the state system according to backend */
            StateSystemBackendType backend = getBackendType();

            ITmfTrace trace = getTrace();
            if (trace == null) {
                // Analysis was cancelled in the meantime
                analysisReady(false);
                return false;
            }
            File htFile;
            switch (backend) {
            case FULL:
                htFile = getSsFile();
                if (htFile == null) {
                    return false;
                }
                createFullHistory(id, provider, htFile);
                break;
            case PARTIAL:
                htFile = getSsFile();
                if (htFile == null) {
                    return false;
                }
                createPartialHistory(id, provider, htFile);
                break;
            case INMEM:
                createInMemoryHistory(id, provider);
                break;
            case NULL:
                createNullHistory(id, provider);
                break;
            case CUSTOM:
                createCustomHistory(id, provider);
                break;
            default:
                break;
            }
        } catch (TmfTraceException e) {
            analysisReady(false);
            return false;
        }
        return !mon.isCanceled();
    }

    /**
     * Make the module available and set whether the initialization succeeded or
     * not. If not, no state system is available and
     * {@link #waitForInitialization()} should return false.
     *
     * @param success
     *            True if the initialization succeeded, false otherwise
     */
    private void analysisReady(boolean succeeded) {
        fInitializationSucceeded = succeeded;
        fInitialized.countDown();
    }

    @Override
    protected void canceling() {
        ITmfEventRequest req = fRequest;
        if ((req != null) && (!req.isCompleted())) {
            req.cancel();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fStateSystem != null) {
            fStateSystem.dispose();
        }
    }

    // ------------------------------------------------------------------------
    // History creation methods
    // ------------------------------------------------------------------------

    /*
     * Load the history file matching the target trace. If the file already exists,
     * it will be opened directly. If not, it will be created from scratch.
     */
    private void createFullHistory(String id, ITmfStateProvider provider, File htFile) throws TmfTraceException {

        /* If the target file already exists, do not rebuild it uselessly */
        // TODO for now we assume it's complete. Might be a good idea to check
        // at least if its range matches the trace's range.

        if (htFile.exists()) {
            /* Load an existing history */
            final int version = provider.getVersion();
            try {
                IStateHistoryBackend backend = StateHistoryBackendFactory.createHistoryTreeBackendExistingFile(
                        id, htFile, version);
                fStateSystem = StateSystemFactory.newStateSystem(backend, false);
                analysisReady(true);
                return;
            } catch (IOException e) {
                /*
                 * There was an error opening the existing file. Perhaps it was corrupted,
                 * perhaps it's an old version? We'll just fall-through and try to build a new
                 * one from scratch instead.
                 */
            }
        }

        /* Size of the blocking queue to use when building a state history */
        final int QUEUE_SIZE = 10000;

        try {
            IStateHistoryBackend backend = StateHistoryBackendFactory.createHistoryTreeBackendNewFile(
                    id, htFile, provider.getVersion(), provider.getStartTime(), QUEUE_SIZE);
            fStateSystem = StateSystemFactory.newStateSystem(backend);
            provider.assignTargetStateSystem(fStateSystem);
            build(provider);
        } catch (IOException e) {
            /*
             * If it fails here however, it means there was a problem writing to the disk,
             * so throw a real exception this time.
             */
            throw new TmfTraceException(e.toString(), e);
        }
    }

    /*
     * Create a new state system backed with a partial history. A partial history is
     * similar to a "full" one (which you get with {@link #newFullHistory}), except
     * that the file on disk is much smaller, but queries are a bit slower.
     *
     * Also note that single-queries are implemented using a full-query underneath,
     * (which are much slower), so this might not be a good fit for a use case where
     * you have to do lots of single queries.
     */
    private void createPartialHistory(String id, ITmfStateProvider provider, File htPartialFile)
            throws TmfTraceException {
        /*
         * The order of initializations is very tricky (but very important!) here. We
         * need to follow this pattern: (1 is done before the call to this method)
         * <ol>
         * <li>Instantiate realStateProvider</li>
         * <li>Instantiate realBackend</li>
         * <li>Instantiate partialBackend, with prereqs:
         * <ol>
         * <li>Instantiate partialProvider, via realProvider.getNew()</li>
         * <li>Instantiate nullBackend (partialSS's backend)</li>
         * <li>Instantiate partialSS</li>
         * <li>partialProvider.assignSS(partialSS)</li>
         * </ol>
         * <li>Instantiate realSS</li>
         * <li>partialSS.assignUpstream(realSS)</li>
         * <li>realProvider.assignSS (realSS)</li>
         * <li>Call HistoryBuilder(realProvider, realSS, partialBackend) to build the
         * thing.</li></li>
         */

        /* Size of the blocking queue to use when building a state history */
        final int QUEUE_SIZE = 10000;

        final long granularity = 50000;

        /* 2 */
        IStateHistoryBackend realBackend = null;
        try {
            realBackend = StateHistoryBackendFactory.createHistoryTreeBackendNewFile(
                    id, htPartialFile, provider.getVersion(), provider.getStartTime(), QUEUE_SIZE);
        } catch (IOException e) {
            throw new TmfTraceException(e.toString(), e);
        }

        /* 3a */
        ITmfStateProvider partialProvider = provider.getNewInstance();

        /*
         * 3b-3c, constructor needs PartialInMemoryBackend in order to save
         * temporary the intervals requested by the query2D()
         */
        IPartialStateHistoryBackend backend = new PartialInMemoryBackend("partial", 0L); //$NON-NLS-1$
        PartialStateSystem pss = new PartialStateSystem(backend);

        /* 3d */
        partialProvider.assignTargetStateSystem(pss);

        /* 3 */
        IStateHistoryBackend partialBackend = new PartialHistoryBackend(id + ".partial", partialProvider, pss, realBackend, granularity, backend); //$NON-NLS-1$

        /* 4 */
        ITmfStateSystemBuilder realSS = StateSystemFactory.newStateSystem(partialBackend);

        /* 5 */
        pss.assignUpstream(realSS);

        /* 6 */
        provider.assignTargetStateSystem(realSS);

        /* 7 */
        fStateSystem = realSS;

        build(provider);
    }

    /*
     * Create a new state system using a null history back-end. This means that no
     * history intervals will be saved anywhere, and as such only {@link
     * ITmfStateSystem#queryOngoingState} will be available.
     */
    private void createNullHistory(String id, ITmfStateProvider provider) {
        IStateHistoryBackend backend = StateHistoryBackendFactory.createNullBackend(id);
        fStateSystem = StateSystemFactory.newStateSystem(backend);
        provider.assignTargetStateSystem(fStateSystem);
        build(provider);
    }

    /*
     * Create a new state system using in-memory interval storage. This should only
     * be done for very small state system, and will be naturally limited to 2^31
     * intervals.
     */
    private void createInMemoryHistory(String id, ITmfStateProvider provider) {
        IStateHistoryBackend backend = StateHistoryBackendFactory.createInMemoryBackend(id, provider.getStartTime());
        fStateSystem = StateSystemFactory.newStateSystem(backend);
        provider.assignTargetStateSystem(fStateSystem);
        build(provider);
    }

    private void createCustomHistory(String id, ITmfStateProvider provider) throws TmfTraceException {
        ICustomStateHistoryBackend backend = getCustomBackend(id, provider);
        if (backend.isBuilt()) {
            try {
                fStateSystem = StateSystemFactory.newStateSystem(backend, false);
                analysisReady(true);
                return;
            } catch (IOException e) {
                throw new TmfTraceException("Could not create custom backend", e); //$NON-NLS-1$
            }
        }
        @NonNull ITmfStateSystemBuilder stateSystemBuilder = StateSystemFactory.newStateSystem(backend);
        fStateSystem = stateSystemBuilder;
        provider.assignTargetStateSystem(stateSystemBuilder);
        build(provider);
    }

    /**
     * If analysis uses custom backend it must override this method to create
     * the backend and prepare it for incoming data.
     *
     * @param id
     *            the ID of the analysis module
     * @param provider
     *            the state provider to be used for analysis
     * @return The state system backend.
     * @throws TmfTraceException
     *             In case of issues while creating custom backend.
     * @since 7.2
     */
    protected ICustomStateHistoryBackend getCustomBackend(String id, ITmfStateProvider provider)
            throws TmfTraceException {
        throw new TmfTraceException("Custom backend is not implemented"); //$NON-NLS-1$
    }

    private void disposeProvider(boolean deleteFiles) {
        ITmfStateProvider provider = fStateProvider;
        boolean shouldDeleteFiles = deleteFiles;
        if (provider != null) {
            provider.dispose();
            Throwable failureCause = provider.getFailureCause();
            /*
             * Fail the analysis if the provider failed and force the file deletion
             */
            if (failureCause != null) {
                fail(failureCause);
                shouldDeleteFiles = true;
            }
        }
        fStateProvider = null;
        if (shouldDeleteFiles && (fStateSystem != null)) {
            fStateSystem.removeFiles();
        }
        completingBuild(shouldDeleteFiles);
    }

    /**
     * The state system and analysis construction has completed. The state provider
     * has been disposed, either at the end of the analysis, when it is cancelled or
     * has failed. Children classes should override this if they need to close or
     * dispose of anything at the end of the analysis execution.
     *
     * @param deleteFiles
     *            If <code>true</code>, files that were built during the execution
     *            should be deleted, otherwise they should be saved.
     * @since 3.3
     */
    protected void completingBuild(boolean deleteFiles) {
        // Nothing to do, classes may override this
    }

    private void build(ITmfStateProvider provider) {
        if (fStateSystem == null) {
            throw new IllegalArgumentException();
        }

        /*
         * Note we have to do this before fStateProvider is assigned. After that, the
         * signal listener below will start sending real trace events through the state
         * provider.
         */
        loadInitialState(provider);

        /* Continue on initializing the event request to read trace events. */
        ITmfEventRequest request = fRequest;
        if ((request != null) && (!request.isCompleted())) {
            request.cancel();
        }

        fTimeRange = TmfTimeRange.ETERNITY;
        final ITmfTrace trace = provider.getTrace();
        if (!isCompleteTrace(trace)) {
            fTimeRange = trace.getTimeRange();
        }

        fStateProvider = provider;
        synchronized (fRequestSyncObj) {
            startRequest();
            request = fRequest;
        }

        /*
         * The state system object is now created, we can consider this module
         * "initialized" (components can retrieve it and start doing queries).
         */
        analysisReady(true);

        /*
         * Block the executeAnalysis() construction is complete (so that the progress
         * monitor displays that it is running).
         */
        try {
            if (request != null) {
                request.waitForCompletion();
                if (request.isFailed()) {
                    Throwable failureCause = request.getFailureCause();
                    if (failureCause != null) {
                        fail(failureCause);
                    } else {
                        fail(new RuntimeException("Event request failed without a cause")); //$NON-NLS-1$
                    }
                }
            }
        } catch (InterruptedException e) {
            fail(e);
        }
    }

    /**
     * Batch-load the initial state, if there is any.
     */
    private void loadInitialState(ITmfStateProvider provider) {
        final ITmfTrace trace = provider.getTrace();
        File path = new File(trace.getPath());
        path = path.isDirectory() ? path : path.getParentFile();
        if (path == null) {
            return;
        }
        for (ITmfStateSystem ss : getStateSystems()) {
            if (ss instanceof ITmfStateSystemBuilder) {
                StateSnapshot snapshot = StateSnapshot.read(path.toPath(), ss.getSSID());
                if (snapshot == null || provider.getVersion() != snapshot.getVersion()) {
                    /*
                     * No statedump found, nothing to pre-load or Do not load the statedump if its
                     * version does not match the current provider.
                     */

                    continue;
                }

                List<List<String>> paths = new ArrayList<>();
                /* create quark list */
                for (Entry<List<String>, ITmfStateInterval> attributeSnapshot : snapshot.getStates().entrySet()) {
                    List<String> attributePath = Objects.requireNonNull(attributeSnapshot.getKey());
                    ITmfStateInterval state = Objects.requireNonNull(attributeSnapshot.getValue());
                    while (paths.size() <= state.getAttribute()) {
                        paths.add(Collections.singletonList("Dummy" + paths.size())); //$NON-NLS-1$
                    }
                    paths.set(state.getAttribute(), attributePath);
                }
                /*
                 * Populate quarks in order
                 */
                int i = 0;
                for (List<String> attributePath : paths) {
                    int quark = ((ITmfStateSystemBuilder) ss).getQuarkAbsoluteAndAdd(attributePath.toArray(new String[attributePath.size()]));
                    if (i != quark) {
                        Activator.logWarning("Quark for analysis " + getClass().getCanonicalName() + " not the same ( " + quark + " != " + i + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    }
                    i++;
                }

                /* Load the statedump into the statesystem */
                for (ITmfStateInterval interval : snapshot.getStates().values()) {
                    Object initialState = interval.getValue();
                    int attribute = interval.getAttribute();
                    provider.addFutureEvent(interval.getStartTime(), initialState, attribute, FutureEventType.MODIFICATION);
                    if (interval.getEndTime() != Long.MIN_VALUE) {
                        provider.addFutureEvent(interval.getEndTime() + 1, (Object) null, attribute, FutureEventType.MODIFICATION);
                    }
                }
            }
        }
    }

    /**
     * A request to build a state system from a state provider
     *
     * @since 2.3
     */
    @VisibleForTesting
    protected class StateSystemEventRequest extends TmfEventRequest {

        private final ITmfStateProvider sci;
        private final ITmfTrace trace;

        /**
         * Constructor
         *
         * @param sp
         *            The state provider used to build the state system
         * @param timeRange
         *            The requested time range for the request
         * @param index
         *            The event number at which to start the request
         */
        public StateSystemEventRequest(ITmfStateProvider sp, TmfTimeRange timeRange, int index) {
            super(ITmfEvent.class,
                    timeRange,
                    index,
                    ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.BACKGROUND,
                    TmfStateSystemAnalysisModule.this.getDependencyLevel());
            this.sci = sp;
            trace = sci.getTrace();

        }

        /**
         * Constructor
         *
         * @param sp
         *            The state provider used to build the state system
         * @param timeRange
         *            The requested time range for the request
         * @param index
         *            The event number at which to start the request
         * @param nbRequested
         *            The number of events requested
         * @since 4.1
         */
        public StateSystemEventRequest(ITmfStateProvider sp, TmfTimeRange timeRange, int index, int nbRequested) {
            super(ITmfEvent.class,
                    timeRange,
                    index,
                    nbRequested,
                    ITmfEventRequest.ExecutionType.BACKGROUND,
                    TmfStateSystemAnalysisModule.this.getDependencyLevel());
            sci = sp;
            trace = sci.getTrace();

        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            processEvent(event, trace);
        }

        private void processEvent(final ITmfEvent event, ITmfTrace tmfTrace) {
            if (event.getTrace() == tmfTrace) {
                sci.processEvent(event);
            } else if (tmfTrace instanceof TmfExperiment) {
                /*
                 * If the request is for an experiment, check if the event is from one of the
                 * child trace
                 */
                for (ITmfTrace childTrace : ((TmfExperiment) tmfTrace).getTraces()) {
                    processEvent(event, childTrace);
                }
            }
        }

        @Override
        public void handleSuccess() {
            super.handleSuccess();
            if (isCompleteTrace(trace)) {
                disposeProvider(false);
            } else {
                fNbRead += getNbRead();
                synchronized (fRequestSyncObj) {
                    final TmfTimeRange timeRange = fTimeRange;
                    if (timeRange != null && getRange().getEndTime().toNanos() < timeRange.getEndTime().toNanos()) {
                        startRequest();
                    }
                }
            }
        }

        @Override
        public void handleCancel() {
            super.handleCancel();
            disposeProvider(true);
        }

        @Override
        public void handleFailure() {
            super.handleFailure();
            disposeProvider(true);
        }

    }

    // ------------------------------------------------------------------------
    // ITmfAnalysisModuleWithStateSystems
    // ------------------------------------------------------------------------

    @Override
    @Nullable
    public ITmfStateSystem getStateSystem(String id) {
        if (id.equals(getId())) {
            return fStateSystem;
        }
        return null;
    }

    @Override
    public @NonNull Iterable<@NonNull ITmfStateSystem> getStateSystems() {
        ITmfStateSystemBuilder stateSystem = fStateSystem;
        if (stateSystem == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(stateSystem);
    }

    /**
     * Signal handler for the TmfTraceRangeUpdatedSignal signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void traceRangeUpdated(final TmfTraceRangeUpdatedSignal signal) {
        fTimeRange = signal.getRange();
        ITmfStateProvider stateProvider = fStateProvider;
        synchronized (fRequestSyncObj) {
            if (signal.getTrace() == getTrace() && stateProvider != null && stateProvider.getAssignedStateSystem() != null) {
                ITmfEventRequest request = fRequest;
                if ((request == null) || request.isCompleted()) {
                    startRequest();
                }
            }
        }
    }

    private void startRequest() {
        ITmfStateProvider stateProvider = fStateProvider;
        TmfTimeRange timeRange = fTimeRange;
        if (stateProvider == null || timeRange == null) {
            return;
        }
        ITmfEventRequest request = createEventRequest(stateProvider, timeRange, fNbRead);
        stateProvider.getTrace().sendRequest(request);
        fRequest = request;
    }

    /**
     * Create a new event request
     *
     * @param stateProvider
     *            The state provider used to build the state system
     * @param timeRange
     *            The requested time range for the request
     * @param nbRead
     *            The event number at which to start the request
     * @return A new event request
     * @since 2.3
     */
    @VisibleForTesting
    protected ITmfEventRequest createEventRequest(ITmfStateProvider stateProvider, TmfTimeRange timeRange, int nbRead) {
        return new StateSystemEventRequest(stateProvider, timeRange, nbRead);
    }

    private static boolean isCompleteTrace(ITmfTrace trace) {
        return !(trace instanceof ITmfTraceCompleteness) || ((ITmfTraceCompleteness) trace).isComplete();
    }

    // ------------------------------------------------------------------------
    // ITmfPropertiesProvider
    // ------------------------------------------------------------------------

    /**
     * @since 2.0
     */
    @Override
    public @NonNull Map<@NonNull String, @NonNull String> getProperties() {
        Map<@NonNull String, @NonNull String> properties = super.getProperties();

        StateSystemBackendType backend = getBackendType();
        properties.put(NonNullUtils.checkNotNull(Messages.TmfStateSystemAnalysisModule_PropertiesBackend), backend.name());
        switch (backend) {
        case FULL:
        case PARTIAL:
            File htFile = getSsFile();
            if (htFile != null) {
                if (htFile.exists()) {
                    properties.put(NonNullUtils.checkNotNull(Messages.TmfStateSystemAnalysisModule_PropertiesFileSize), FileUtils.byteCountToDisplaySize(htFile.length()));
                } else {
                    properties.put(NonNullUtils.checkNotNull(Messages.TmfStateSystemAnalysisModule_PropertiesFileSize), NonNullUtils.checkNotNull(Messages.TmfStateSystemAnalysisModule_PropertiesAnalysisNotExecuted));
                }
            }
            break;
        case INMEM:
        case NULL:
        case CUSTOM:
        default:
            break;

        }
        return properties;
    }


    /**
     * @since 9.4
     */
    @Override
    public void clearPersistentData() {
        super.clearPersistentData();
        if (fStateSystem != null) {
            // State system is open
            fStateSystem.removeFiles();
        } else {
            // State system is closed... delete directly
            StateSystemBackendType backend = getBackendType();
            switch (backend) {
            case FULL:
            case PARTIAL:
                File htFile = getSsFile();
                if ((htFile != null) && (htFile.exists())) {
                    htFile.delete();
                }
                break;
                //$CASES-OMITTED$
            default:
                break;
            }
        }

        // Reset analysis so that it can be scheduled again
        resetAnalysis();
    }
}
