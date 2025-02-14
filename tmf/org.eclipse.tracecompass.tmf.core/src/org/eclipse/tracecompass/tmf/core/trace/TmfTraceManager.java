/*******************************************************************************
 * Copyright (c) 2013, 2025 Ericsson and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *   Patrick Tasse - Support selection range
 *   Xavier Raynaud - Support filters tracking
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.trace;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.signal.TmfEventFilterAppliedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceModelSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.traceeventlogger.LogUtils;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;

/**
 * Central trace manager for TMF. It tracks the currently opened traces and
 * experiment, as well as the currently-selected time or time range and the
 * current window time range for each one of those. It also tracks filters
 * applied for each trace.
 *
 * It's a singleton class, so only one instance should exist (available via
 * {@link #getInstance()}).
 *
 * @author Alexandre Montplaisir
 */
@NonNullByDefault
public final class TmfTraceManager {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final Map<ITmfTrace, TmfTraceContext> fTraces;

    /** Count of trace instances by resource, used to track instance number */
    private final Multiset<IResource> fInstanceCounts;

    /** The currently-selected trace. Should always be part of the trace map */
    private @Nullable ITmfTrace fCurrentTrace = null;

    private static final String TEMP_DIR_NAME = ".tracecompass-temp"; //$NON-NLS-1$

    private static final Logger LOGGER = TraceCompassLog.getLogger(TmfTraceManager.class);

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    private TmfTraceManager() {
        fTraces = new LinkedHashMap<>();
        fInstanceCounts = checkNotNull(HashMultiset.create());

        TmfSignalManager.registerVIP(this);
    }

    /** Singleton instance */
    private static @Nullable TmfTraceManager tm = null;

    /**
     * Get an instance of the trace manager.
     *
     * @return The trace manager
     */
    public static synchronized TmfTraceManager getInstance() {
        TmfTraceManager mgr = tm;
        if (mgr == null) {
            mgr = new TmfTraceManager();
            tm = mgr;
        }
        return mgr;
    }

    /**
     * Disposes the trace manager
     *
     * @since 2.3
     */
    public synchronized void dispose() {
        TmfSignalManager.deregister(this);
        fTraces.clear();
        fInstanceCounts.clear();
        fCurrentTrace = null;
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Get the currently selected trace (normally, the focused editor).
     *
     * @return The active trace, or <code>null</code> if there is no active
     *         trace
     */
    public synchronized @Nullable ITmfTrace getActiveTrace() {
        return fCurrentTrace;
    }

    /**
     * Get the trace set of the currently active trace.
     *
     * @return The active trace set. Empty (but non-null) if there is no
     *         currently active trace.
     * @see #getTraceSet(ITmfTrace)
     */
    public synchronized Collection<ITmfTrace> getActiveTraceSet() {
        final ITmfTrace trace = fCurrentTrace;
        return getTraceSet(trace);
    }

    /**
     * Get the currently-opened traces, as an unmodifiable set.
     *
     * @return A set containing the opened traces
     */
    public synchronized Set<ITmfTrace> getOpenedTraces() {
        return Collections.unmodifiableSet(fTraces.keySet());
    }

    /**
     * Get the currently-opened traces that belong to a given host, as an
     * unmodifiable set.
     *
     * @param hostId
     *            The ID of the host
     * @return A set containing the opened traces for a host
     * @since 3.2
     */
    public synchronized Set<ITmfTrace> getTracesForHost(String hostId) {
        return Collections.unmodifiableSet(fTraces.keySet().stream()
                .flatMap(t -> getTraceSet(t).stream())
                .filter(t -> hostId.equals(t.getHostId()))
                .collect(Collectors.toSet()));
    }

    /**
     * Get the editor file for an opened trace.
     *
     * @param trace
     *            the trace
     * @return the editor file or null if the trace is not opened
     */
    public synchronized @Nullable IFile getTraceEditorFile(ITmfTrace trace) {
        TmfTraceContext ctx = fTraces.get(trace);
        if (ctx != null) {
            return ctx.getEditorFile();
        }
        return null;
    }

    /**
     * Get the {@link TmfTraceContext} of the current active trace. This can be
     * used to retrieve the current active/selected time ranges and such.
     *
     * @return The trace's context.
     * @since 1.0
     */
    public synchronized TmfTraceContext getCurrentTraceContext() {
        TmfTraceContext curCtx = fTraces.get(fCurrentTrace);
        if (curCtx == null) {
            /* There are no traces opened at the moment. */
            return TmfTraceContext.NULL_CONTEXT;
        }
        return curCtx;
    }

    /**
     * Get the {@link TmfTraceContext} of the given trace.
     *
     * @param trace
     *            The trace or experiment.
     * @return The trace's context.
     * @since 2.3
     */
    public synchronized TmfTraceContext getTraceContext(ITmfTrace trace) {
        TmfTraceContext curCtx = fTraces.get(trace);
        if (curCtx == null) {
            /* The trace is not opened. */
            return TmfTraceContext.NULL_CONTEXT;
        }
        return curCtx;
    }

    /**
     * Gets a unique name that differentiates multiple opened instances of the
     * same trace. The first instance has the trace name, subsequent instances
     * have a sequential suffix appended to the trace name. Closing all trace
     * instances resets the sequence.
     *
     * @param trace
     *            The trace or experiment
     *
     * @return The trace unique name
     * @since 3.2
     */
    public synchronized String getTraceUniqueName(@Nullable ITmfTrace trace) {
        if (trace == null) {
            return StringUtils.EMPTY;
        }
        TmfTraceContext ctx = getTraceContext(trace);
        int instanceNumber = ctx.getInstanceNumber();
        String name = NonNullUtils.nullToEmptyString(trace.getName());
        if (instanceNumber <= 1) {
            return name;
        }
        return name + " | " + instanceNumber; //$NON-NLS-1$
    }

    // ------------------------------------------------------------------------
    // Public utility methods
    // ------------------------------------------------------------------------

    /**
     * Get the trace set of a given trace or experiment. For a standard trace,
     * this is simply a collection containing only that trace. For experiments,
     * this is a collection of all the leaf traces contained in this experiment,
     * recursively.
     *
     * @param trace
     *            The trace or experiment. If it is null, an empty collection
     *            will be returned.
     * @return The corresponding trace set.
     */
    public static Collection<ITmfTrace> getTraceSet(@Nullable ITmfTrace trace) {
        if (trace == null) {
            return ImmutableSet.of();
        }
        List<@NonNull ITmfTrace> traces = trace.getChildren(ITmfTrace.class);
        if (!traces.isEmpty()) {
            Iterable<ITmfTrace> iterable = checkNotNull(Iterables.concat(Iterables.transform(traces, TmfTraceManager::getTraceSet)));
            return ImmutableSet.copyOf(iterable);
        } else if (trace instanceof TmfExperiment) {
            return ImmutableSet.of();
        }
        return ImmutableSet.of(trace);
    }

    /**
     * Get the trace set of a given trace or experiment, including the
     * experiments. For a standard trace, this is simply a collection containing
     * only that trace. For experiments, this is a collection of all the
     * experiments and leaf traces contained in the experiments, recursively.
     *
     * @param trace
     *            The trace or experiment. If it is null, an empty collection
     *            will be returned.
     * @return The corresponding trace set, including experiments.
     */
    public static Collection<ITmfTrace> getTraceSetWithExperiment(@Nullable ITmfTrace trace) {
        if (trace == null) {
            return ImmutableSet.of();
        }
        List<@NonNull ITmfTrace> traces = trace.getChildren(ITmfTrace.class);
        if (!traces.isEmpty()) {
            Iterable<ITmfTrace> iterable = checkNotNull(Iterables.concat(Iterables.transform(traces, t -> getTraceSetWithExperiment(t))));
            return ImmutableSet.<ITmfTrace>builder().add(trace).addAll(iterable).build();
        }
        return Collections.singleton(trace);
    }

    /**
     * Return the path (as a string) to the directory for supplementary files to
     * use with a given trace. If no supplementary file directory has been
     * configured, a temporary directory based on the trace's name will be
     * provided.
     *
     * @param trace
     *            The trace
     * @return The path to the supplementary file directory (trailing slash is
     *         INCLUDED!)
     */
    public static String getSupplementaryFileDir(ITmfTrace trace) {
        IResource resource = trace.getResource();
        if (resource == null) {
            return getTemporaryDir(trace);
        }

        String supplDir = null;
        try {
            supplDir = resource.getPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER);
        } catch (CoreException e) {
            return getTemporaryDir(trace);
        }
        return supplDir + File.separator;
    }

    /**
     * Refresh the supplementary files resources for a trace, so it can pick up
     * new files that got created.
     *
     * @param trace
     *            The trace for which to refresh the supplementary files
     */
    public static void refreshSupplementaryFiles(ITmfTrace trace) {
        IResource resource = trace.getResource();
        if (resource != null && resource.exists()) {
            String supplFolderPath = getSupplementaryFileDir(trace);
            IProject project = resource.getProject();
            /* Remove the project's path from the supplementary path dir */
            if (!supplFolderPath.startsWith(project.getLocation().toOSString())) {
                Activator.logWarning(String.format("Supplementary files folder for trace %s is not within the project.", trace.getName())); //$NON-NLS-1$
                return;
            }
            IFolder supplFolder = project.getFolder(supplFolderPath.substring(project.getLocationURI().getPath().length()));
            if (supplFolder.exists()) {
                try {
                    supplFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
                } catch (CoreException e) {
                    Activator.logError("Error refreshing resources", e); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Delete the supplementary files of a given trace.
     *
     * @param trace
     *            The trace for which the supplementary files are to be deleted
     * @since 2.2
     */
    public static void deleteSupplementaryFiles(ITmfTrace trace) {
        try {
            String supplementaryFileDir = TmfTraceManager.getSupplementaryFileDir(trace);
            FileUtils.cleanDirectory(new File(supplementaryFileDir));
            // Needed to audit for privacy concerns
            LogUtils.traceInstant(LOGGER, Level.CONFIG, "deleteSupplementaryFiles", supplementaryFileDir); //$NON-NLS-1$
        } catch (IOException e) {
            Activator.logError("Error deleting supplementary files for trace " + trace.getName(), e); //$NON-NLS-1$
        }
        refreshSupplementaryFiles(trace);
    }

    /**
     * Deletes the supplementary folder for the given trace
     *
     * @param trace
     *            trace to delete the folder for
     *
     * @since 3.1
     */
    public static void deleteSupplementaryFolder(ITmfTrace trace) {
        deleteSupplementaryFiles(trace);
        File parent = new File(TmfTraceManager.getSupplementaryFileDir(trace));
        try {
            String temporaryDirPath = getTemporaryDirPath();
            deleteFolder(parent, temporaryDirPath);
            // Needed to audit for privacy concerns
            LogUtils.traceInstant(LOGGER, Level.CONFIG, "deleteSupplementaryFolder", temporaryDirPath); //$NON-NLS-1$
        } catch (IOException e) {
            Activator.logError("Error deleting supplementary folder for trace " + trace.getName(), e); //$NON-NLS-1$
        }
    }

    /**
     * Returns whether a trace should be synchronized in selection and window range
     * with an other trace.
     *
     * @param trace
     *            The trace that may need to be synchronized
     * @param other
     *            The other trace that has a change of selection or window range
     * @return true if the trace should be synchronized with the other trace
     * @since 4.0
     */
    public synchronized boolean isSynchronized(ITmfTrace trace, ITmfTrace other) {
        /* other instance of the same trace should never synchronize */
        final boolean sameTrace = trace.getResource() == null ? false : trace.getResource().equals(other.getResource());
        return trace.equals(other) || (!sameTrace && getTraceContext(trace).isSynchronized());
    }

    /**
     * Update the trace context of a given trace.
     *
     * @param trace
     *            The trace
     * @param updater
     *            the function to apply to the trace context's builder
     * @since 2.3
     */
    public synchronized void updateTraceContext(ITmfTrace trace, UnaryOperator<TmfTraceContext.Builder> updater) {
        TmfTraceContext ctx = getTraceContext(trace);
        if (!ctx.equals(TmfTraceContext.NULL_CONTEXT)) {
            fTraces.put(trace, checkNotNull(updater.apply(ctx.builder())).build());
        }
    }

    // ------------------------------------------------------------------------
    // Signal handlers
    // ------------------------------------------------------------------------

    /**
     * Signal handler for the traceOpened signal.
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public synchronized void traceOpened(final TmfTraceOpenedSignal signal) {
        final ITmfTrace trace = signal.getTrace();
        final IFile editorFile = signal.getEditorFile();

        final TmfTimeRange windowRange = trace.getInitialTimeRange();
        final ITmfTimestamp startTs = windowRange.getStartTime();
        final TmfTimeRange selectionRange = new TmfTimeRange(startTs, startTs);

        final TmfTraceContext startCtx = trace.createTraceContext(selectionRange, windowRange, editorFile, null);

        fTraces.put(trace, startCtx);

        IResource resource = trace.getResource();
        if (resource != null) {
            fInstanceCounts.add(resource);
            updateTraceContext(trace, builder -> builder.setInstanceNumber(fInstanceCounts.count(resource)));
        }

        /* We also want to set the newly-opened trace as the active trace */
        fCurrentTrace = trace;
    }

    /**
     * Signal propagator
     *
     * @param signal
     *            any signal
     * @since 2.0
     */
    @TmfSignalHandler
    public synchronized void signalReceived(final TmfTraceModelSignal signal) {
        fTraces.forEach((t, u) -> u.receive(signal));
    }


    /**
     * Handler for the TmfTraceSelectedSignal.
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public synchronized void traceSelected(final TmfTraceSelectedSignal signal) {
        final ITmfTrace newTrace = signal.getTrace();
        if (!fTraces.containsKey(newTrace)) {
            throw new RuntimeException();
        }
        fCurrentTrace = newTrace;
    }

    /**
     * Signal handler for the filterApplied signal.
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public synchronized void filterApplied(TmfEventFilterAppliedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        if (trace == null) {
            return;
        }

        updateTraceContext(trace, builder ->
                builder.setFilter(signal.getEventFilter()));
    }

    /**
     * Signal handler for the traceClosed signal.
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public synchronized void traceClosed(final TmfTraceClosedSignal signal) {
        fTraces.remove(signal.getTrace());

        IResource resource = signal.getTrace().getResource();
        if (resource != null && fTraces.keySet().stream()
                .noneMatch(trace -> resource.equals(trace.getResource()))) {
            /* Reset the instance count only when no other instance remains */
            fInstanceCounts.setCount(resource, 0);
        }

        if (fTraces.size() == 0) {
            fCurrentTrace = null;
            /*
             * In other cases, we should receive a traceSelected signal that
             * will indicate which trace is the new one.
             */
        }
    }

    /**
     * Signal handler for the selection range signal.
     *
     * If the signal trace is null, the time selection of *all* traces whose
     * range contains the requested new selection time range will be updated. If
     * the signal contains a trace, the signal trace and time-synchronized
     * traces will be updated, but not other instances of the signal trace.
     *
     * @param signal
     *            The incoming signal
     * @since 1.0
     */
    @TmfSignalHandler
    public synchronized void selectionRangeUpdated(final TmfSelectionRangeUpdatedSignal signal) {
        final ITmfTimestamp beginTs = signal.getBeginTime();
        final ITmfTimestamp endTs = signal.getEndTime();
        final ITmfTrace signalTrace = signal.getTrace();

        for (ITmfTrace trace : fTraces.keySet()) {
            if ((beginTs.intersects(getValidTimeRange(trace)) || endTs.intersects(getValidTimeRange(trace)))
                    && (signalTrace == null || isSynchronized(trace, signalTrace))) {
                updateTraceContext(trace, builder ->
                        builder.setSelection(new TmfTimeRange(beginTs, endTs)));
            }
        }
    }

    /**
     * Signal handler for the window range signal.
     *
     * If the signal trace is null, the window range of *all* valid traces will
     * be updated to the new window range. If the signal contains a trace, the
     * signal trace and time-synchronized traces will be updated, but not other
     * instances of the signal trace.
     *
     * @param signal
     *            The incoming signal
     * @since 1.0
     */
    @TmfSignalHandler
    public synchronized void windowRangeUpdated(final TmfWindowRangeUpdatedSignal signal) {
        final ITmfTrace signalTrace = signal.getTrace();
        for (Map.Entry<ITmfTrace, TmfTraceContext> entry : fTraces.entrySet()) {
            ITmfTrace trace = checkNotNull(entry.getKey());
            if (signalTrace != null && !isSynchronized(trace, signalTrace)) {
                continue;
            }
            final TmfTimeRange validTr = getValidTimeRange(trace);
            if (validTr == null) {
                continue;
            }

            /* Determine the new time range */
            TmfTimeRange targetTr = signal.getCurrentRange().getIntersection(validTr);
            if (targetTr != null) {
                updateTraceContext(trace, builder ->
                        builder.setWindowRange(targetTr));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Private utility methods
    // ------------------------------------------------------------------------

    /**
     * Return the valid time range of a trace (not the current window time
     * range, but the range of all possible valid timestamps).
     *
     * For a real trace this is the whole range of the trace. For an experiment,
     * it goes from the start time of the earliest trace to the end time of the
     * latest one.
     *
     * @param trace
     *            The trace to check for
     * @return The valid time span, or 'null' if the trace is not valid
     */
    private @Nullable TmfTimeRange getValidTimeRange(ITmfTrace trace) {
        if (!fTraces.containsKey(trace)) {
            /* Trace is not part of the currently opened traces */
            return null;
        }

        List<ITmfTrace> traces = trace.getChildren(ITmfTrace.class);

        if (traces.isEmpty()) {
            /* "trace" is a single trace, return its time range directly */
            return trace.getTimeRange();
        }

        if (traces.size() == 1) {
            /* Trace is an experiment with only 1 trace */
            return traces.get(0).getTimeRange();
        }

        /*
         * Trace is an trace set with 2+ traces, so get the earliest start and
         * the latest end.
         */
        ITmfTimestamp start = traces.get(0).getStartTime();
        ITmfTimestamp end = traces.get(0).getEndTime();

        for (int i = 1; i < traces.size(); i++) {
            ITmfTrace curTrace = traces.get(i);
            if (curTrace.getStartTime().compareTo(start) < 0) {
                start = curTrace.getStartTime();
            }
            if (curTrace.getEndTime().compareTo(end) > 0) {
                end = curTrace.getEndTime();
            }
        }
        return new TmfTimeRange(start, end);
    }

    /**
     * Get the temporary directory path. If there is an instance of Eclipse
     * running, the temporary directory will reside under the workspace.
     *
     * @return the temporary directory path suitable to be passed to the
     *         java.io.File constructor without a trailing separator
     */
    public static String getTemporaryDirPath() {
        // Get the workspace path from the properties
        String property = System.getProperty("osgi.instance.area"); //$NON-NLS-1$
        if (property != null) {
            try {
                File dir = URIUtil.toFile(URIUtil.fromString(property));
                dir = new File(dir.getAbsolutePath() + File.separator + TEMP_DIR_NAME);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                return dir.getAbsolutePath();
            } catch (URISyntaxException e) {
                Activator.logError(e.getLocalizedMessage(), e);
            }
        }
        return System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
    }

    /**
     * Get a temporary directory based on a trace's path. We will create the
     * directory if it doesn't exist, so that it's ready to be used.
     */
    private static String getTemporaryDir(ITmfTrace trace) {
        String pathName = new Path(getTemporaryDirPath())
                .append(trace.getPath() != null ? trace.getPath() : trace.getName())
                .addTrailingSeparator()
                .toOSString();
        File dir = new File(pathName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return Objects.requireNonNull(pathName);
    }

    /*
     * Deletes a folder recursively and deletes the parent(s) until a non-empty
     * parent is found or till stopPath is reached.
     */
    private static void deleteFolder(File folder, String stopPath) throws IOException {
        if (folder.exists()) {
            FileUtils.deleteDirectory(folder);
        }
        File parent = folder.getParentFile();
        if (!parent.getAbsolutePath().equals(stopPath) && (parent.list().length == 0)) {
            deleteFolder(parent, stopPath);
        }
    }
}
