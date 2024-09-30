/*******************************************************************************
 * Copyright (c) 2012, 2024 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *   Patrick Tasse - Updated for removal of context clone
 *   Geneviève Bastien - Added the createTimestamp function
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ctf.core.trace;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.CTFCallsite;
import org.eclipse.tracecompass.ctf.core.event.CTFClock;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFStreamInput;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.core.trace.CTFTraceReader;
import org.eclipse.tracecompass.ctf.core.trace.CTFTraceWriter;
import org.eclipse.tracecompass.ctf.core.trace.ICTFStream;
import org.eclipse.tracecompass.ctf.core.trace.Metadata;
import org.eclipse.tracecompass.internal.tmf.ctf.core.Activator;
import org.eclipse.tracecompass.internal.tmf.ctf.core.event.aspect.CtfEventContextAspect;
import org.eclipse.tracecompass.internal.tmf.ctf.core.event.aspect.CtfPacketContextAspect;
import org.eclipse.tracecompass.internal.tmf.ctf.core.event.aspect.CtfPacketHeaderAspect;
import org.eclipse.tracecompass.internal.tmf.ctf.core.event.aspect.CtfStreamContextAspect;
import org.eclipse.tracecompass.internal.tmf.ctf.core.trace.iterator.CtfIterator;
import org.eclipse.tracecompass.internal.tmf.ctf.core.trace.iterator.CtfIteratorManager;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ICyclesConverter;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceKnownSize;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceWithPreDefinedEvents;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfPersistentlyIndexable;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfTraceIndexer;
import org.eclipse.tracecompass.tmf.core.trace.indexer.TmfBTreeTraceIndexer;
import org.eclipse.tracecompass.tmf.core.trace.indexer.checkpoint.ITmfCheckpoint;
import org.eclipse.tracecompass.tmf.core.trace.indexer.checkpoint.TmfCheckpoint;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.trim.ITmfTrimmableTrace;
import org.eclipse.tracecompass.tmf.ctf.core.CtfConstants;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfLocation;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfLocationInfo;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfTmfContext;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventFactory;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventType;
import org.eclipse.tracecompass.tmf.ctf.core.event.aspect.CtfChannelAspect;
import org.eclipse.tracecompass.tmf.ctf.core.event.aspect.CtfCpuAspect;
import org.eclipse.tracecompass.tmf.ctf.core.event.lookup.CtfTmfCallsite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * The CTf trace handler
 *
 * @version 1.0
 * @author Matthew khouzam
 */
public class CtfTmfTrace extends TmfTrace
        implements ITmfPropertiesProvider, ITmfPersistentlyIndexable,
        ITmfTraceWithPreDefinedEvents, ITmfTraceKnownSize, ICyclesConverter, ITmfTrimmableTrace {

    // -------------------------------------------
    // Constants
    // -------------------------------------------

    /**
     * Clock offset property
     *
     * @since 1.2
     * @deprecated use {@link CTFTrace#CLOCK_OFFSET} instead
     */
    @Deprecated
    public static final String CLOCK_OFFSET = "clock_offset"; //$NON-NLS-1$

    /**
     * Default cache size for CTF traces
     */
    protected static final int DEFAULT_CACHE_SIZE = 50000;

    /**
     * Event aspects available for all CTF traces
     *
     * @since 1.0
     */
    protected static final @NonNull Collection<@NonNull ITmfEventAspect<?>> CTF_ASPECTS = ImmutableList.of(
            TmfBaseAspects.getTimestampAspect(),
            new CtfChannelAspect(),
            new CtfCpuAspect(),
            TmfBaseAspects.getEventTypeAspect(),
            TmfBaseAspects.getContentsAspect(),
            CtfPacketHeaderAspect.getInstance(),
            CtfPacketContextAspect.getInstance(),
            CtfStreamContextAspect.getInstance(),
            CtfEventContextAspect.getInstance());

    /**
     * The Ctf clock unique identifier field
     */
    private static final String CLOCK_HOST_PROPERTY = "uuid"; //$NON-NLS-1$
    private static final int CONFIDENCE = 10;
    private static final int MIN_CONFIDENCE = 1;

    /**
     * This is a reduction factor to avoid overflows.
     */
    private static final long REDUCTION_FACTOR = 4096;

    /**
     * Average CTF event size, used to estimate the trace size. (Inspired by
     * empirical observations with LTTng kernel traces, to avoid hanging at 100% for
     * too long)
     *
     * TODO: Find a more suitable approximation, perhaps per concrete trace type or
     * per trace directly with the metadata
     */
    private static final int CTF_AVG_EVENT_SIZE = 16;

    // -------------------------------------------
    // Fields
    // -------------------------------------------

    private final Map<@NonNull String, @NonNull CtfTmfEventType> fContainedEventTypes = Collections.synchronizedMap(new HashMap<>());

    private final CtfIteratorManager fIteratorManager = new CtfIteratorManager(this);

    private final @NonNull CtfTmfEventFactory fEventFactory;

    /** Reference to the CTF Trace */
    private CTFTrace fTrace;

    private UUID fUUID;

    // -------------------------------------------
    // Constructor
    // -------------------------------------------

    /**
     * Default constructor
     */
    public CtfTmfTrace() {
        this(CtfTmfEventFactory.instance());
    }

    /**
     * Constructor for sub-classes to specify their own event factory.
     *
     * @param eventFactory
     *            The event factory to use to generate trace events
     * @since 2.0
     */
    protected CtfTmfTrace(@NonNull CtfTmfEventFactory eventFactory) {
        super();
        fEventFactory = eventFactory;
    }

    // -------------------------------------------
    // TmfTrace Overrides
    // -------------------------------------------
    /**
     * Method initTrace.
     *
     * @param resource
     *            The resource associated with this trace
     * @param path
     *            The path to the trace file
     * @param eventType
     *            The type of events that will be read from this trace
     * @throws TmfTraceException
     *             If something went wrong while reading the trace
     */
    @Override
    public void initTrace(final IResource resource, final String path, final Class<? extends ITmfEvent> eventType)
            throws TmfTraceException {
        /*
         * Set the cache size. This has to be done before the call to super() because
         * the super needs to know the cache size.
         */
        setCacheSize();

        super.initTrace(resource, path, eventType);

        try {
            this.fTrace = new CTFTrace(path);
            CtfTmfContext ctx;
            /* Set the start and (current) end times for this trace */
            ctx = (CtfTmfContext) seekEvent(0L);
            CtfTmfEvent event = getNext(ctx);
            if ((ctx.getLocation().equals(CtfIterator.NULL_LOCATION)) || (ctx.getCurrentEvent() == null)) {
                /* Handle the case where the trace is empty */
                this.setStartTime(TmfTimestamp.BIG_BANG);
            } else {
                final ITmfTimestamp curTime = event.getTimestamp();
                this.setStartTime(curTime);
                long endTime = Math.max(curTime.getValue(), fTrace.getCurrentEndTime());
                this.setEndTime(TmfTimestamp.fromNanos(endTime));
            }
            /*
             * Register every event type. When you call getType, it will register a trace to
             * that type in the TmfEventTypeManager
             */
            try (CtfIterator iter = fIteratorManager.getIterator(ctx)) {
                if (iter == null) {
                    throw new TmfTraceException("Failed to get CTF Iterator for path " + path); //$NON-NLS-1$
                }
                Set<@NonNull ITmfEventField> streamContextNames = new HashSet<>();
                for (IEventDeclaration ied : iter.getEventDeclarations()) {
                    CtfTmfEventType ctfTmfEventType = fContainedEventTypes.get(ied.getName());
                    if (ctfTmfEventType == null) {
                        List<ITmfEventField> content = new ArrayList<>();

                        /* Should only return null the first time */
                        final StructDeclaration fields = ied.getFields();
                        if (fields != null) {
                            for (String fieldName : fields.getFieldsList()) {
                                content.add(new TmfEventField(checkNotNull(fieldName), null, null));
                            }
                        }

                        /* Add stream contexts */
                        final StructDeclaration streamContexts = ied.getStream().getEventContextDecl();
                        if (streamContextNames.isEmpty()) {
                            if (streamContexts != null) {
                                for (String fieldName : streamContexts.getFieldsList()) {
                                    streamContextNames.add(new TmfEventField(checkNotNull(CtfConstants.CONTEXT_FIELD_PREFIX + fieldName), null, null));
                                }
                            }
                        }
                        content.addAll(streamContextNames);

                        if (!content.isEmpty()) {
                            ITmfEventField contentTree = new TmfEventField(
                                    ITmfEventField.ROOT_FIELD_ID,
                                    null,
                                    content.toArray(new ITmfEventField[content.size()]));

                            ctfTmfEventType = new CtfTmfEventType(checkNotNull(ied.getName()), contentTree);
                            fContainedEventTypes.put(ctfTmfEventType.getName(), ctfTmfEventType);
                        }
                    }
                }
            }
            ctx.dispose();
            fUUID = fTrace.getUUID();
        } catch (final CTFException e) {
            /*
             * If it failed at the init(), we can assume it's because the file was not found
             * or was not recognized as a CTF trace. Throw into the new type of exception
             * expected by the rest of TMF.
             */
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    @Override
    public synchronized void dispose() {
        fIteratorManager.dispose();
        fContainedEventTypes.clear();
        if (fTrace != null) {
            fTrace = null;
        }
        super.dispose();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation of a CTF trace.
     *
     * Firstly a weak validation of the metadata is done to determine if the path is
     * actually for a CTF trace. After that a full validation is done.
     *
     * If the weak and full validation are successful the confidence is set to 10.
     *
     * If the weak validation was successful, but the full validation fails a
     * TraceValidationStatus with severity warning and confidence of 1 is returned.
     *
     * If both weak and full validation fails an error status is returned.
     */
    @Override
    public IStatus validate(final IProject project, final String path) {
        boolean isMetadataFile = false;
        String pluginId = Activator.PLUGIN_ID;
        try {
            isMetadataFile = Metadata.preValidate(path);
        } catch (final CTFException e) {
            return new Status(IStatus.ERROR, pluginId, Messages.CtfTmfTrace_ReadingError + ": " + e.toString(), e); //$NON-NLS-1$
        }

        if (isMetadataFile) {
            // Trace is pre-validated, continue will full validation
            try {
                final CTFTrace trace = new CTFTrace(path);
                if (!trace.majorIsSet()) {
                    if (isMetadataFile) {
                        return new TraceValidationStatus(MIN_CONFIDENCE, IStatus.WARNING, pluginId, Messages.CtfTmfTrace_MajorNotSet, null);
                    }
                    return new Status(IStatus.ERROR, pluginId, Messages.CtfTmfTrace_MajorNotSet);
                }

                // Validate using reader initialization
                try (CTFTraceReader ctfTraceReader = new CTFTraceReader(trace)) {
                    // do nothing
                }

                // Get contained event names
                Collection<String> eventNames = new HashSet<>();
                for (ICTFStream stream : trace.getStreams()) {
                    for (IEventDeclaration ed : trace.getEventDeclarations(stream.getId())) {
                        if (ed != null) {
                            eventNames.add(ed.getName());
                        }
                    }
                }

                // Trace is validated, return with confidence
                return new CtfTraceValidationStatus(CONFIDENCE, pluginId, trace.getEnvironment(), eventNames);

            } catch (final CTFException | BufferOverflowException e) {
                // return warning since it's a CTF trace but with errors in it
                return new TraceValidationStatus(MIN_CONFIDENCE, IStatus.WARNING, pluginId, Messages.CtfTmfTrace_ReadingError + ": " + e.toString(), e); //$NON-NLS-1$
            }
        }
        return new Status(IStatus.ERROR, pluginId, Messages.CtfTmfTrace_ReadingError);
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return CTF_ASPECTS;
    }

    /**
     * Method getCurrentLocation. This is not applicable in CTF
     *
     * @return null, since the trace has no knowledge of the current location
     * @see org.eclipse.tracecompass.tmf.core.trace.ITmfTrace#getCurrentLocation()
     */
    @Override
    public ITmfLocation getCurrentLocation() {
        return null;
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        final CtfLocation curLocation = (CtfLocation) location;
        final long startTime = getStartTime().getValue();
        final double diff = curLocation.getLocationInfo().getTimestamp() - startTime;
        final double total = getEndTime().getValue() - startTime;
        return Math.max(0.0, Math.min(1.0, diff / total));
    }

    /**
     * Method seekEvent.
     *
     * @param location
     *            ITmfLocation<?>
     * @return ITmfContext
     */
    @Override
    public synchronized ITmfContext seekEvent(final ITmfLocation location) {
        CtfLocation currentLocation = (CtfLocation) location;
        CtfTmfContext context = new CtfTmfContext(this);
        if (fTrace == null) {
            context.setLocation(null);
            context.setRank(ITmfContext.UNKNOWN_RANK);
            return context;
        }
        /*
         * The rank is set to 0 if the iterator seeks the beginning. If not, it will be
         * set to UNKNOWN_RANK, since CTF traces don't support seeking by rank for now.
         */
        if (currentLocation == null) {
            currentLocation = new CtfLocation(new CtfLocationInfo(0L, 0L));
            context.setRank(0);
        } else {
            context.setRank(ITmfContext.UNKNOWN_RANK);
        }
        /* This will seek and update the location after the seek */
        context.setLocation(currentLocation);
        return context;
    }

    @Override
    public synchronized ITmfContext seekEvent(double ratio) {
        CtfTmfContext context = new CtfTmfContext(this);
        if (fTrace == null) {
            context.setLocation(null);
            context.setRank(ITmfContext.UNKNOWN_RANK);
            return context;
        }
        final long end = getEndTime().getValue();
        final long start = getStartTime().getValue();
        final long diff = end - start;
        final long ratioTs = Math.round(diff * ratio) + start;
        context.seek(ratioTs);
        context.setRank(ITmfContext.UNKNOWN_RANK);
        return context;
    }

    /**
     * Method readNextEvent.
     *
     * @param context
     *            ITmfContext
     * @return CtfTmfEvent
     * @see org.eclipse.tracecompass.tmf.core.trace.ITmfTrace#getNext(ITmfContext)
     */
    @Override
    public synchronized CtfTmfEvent getNext(final ITmfContext context) {
        if (fTrace == null) {
            return null;
        }
        CtfTmfEvent event = null;
        if (context instanceof CtfTmfContext) {
            if (context.getLocation() == null || CtfLocation.INVALID_LOCATION.equals(context.getLocation().getLocationInfo())) {
                return null;
            }
            CtfTmfContext ctfContext = (CtfTmfContext) context;
            event = ctfContext.getCurrentEvent();

            if (event != null) {
                updateAttributes(context, event);
                ctfContext.advance();
                ctfContext.increaseRank();
            }
        }

        return event;
    }

    /**
     * Ctf traces have a clock with a unique uuid that will be used to identify the
     * host. Traces with the same clock uuid will be known to have been made on the
     * same machine.
     *
     * Note: uuid is an optional field, it may not be there for a clock.
     */
    @Override
    public String getHostId() {
        CTFTrace trace = fTrace;
        if (trace != null) {
            CTFClock clock = trace.getClock();
            if (clock != null) {
                String clockHost = (String) clock.getProperty(CLOCK_HOST_PROPERTY);
                if (clockHost != null) {
                    return clockHost;
                }
            }
        }
        return super.getHostId();
    }

    /**
     * Get the first callsite that matches the event name
     *
     * @param eventName
     *            The event name to look for
     * @return The best callsite candidate
     * @since 3.0
     */
    public @Nullable CtfTmfCallsite getCallsite(String eventName) {
        CTFTrace trace = fTrace;
        if (trace != null) {
            for (ICTFStream stream : fTrace.getStreams()) {
                for (IEventDeclaration eventDeclaration : stream.getEventDeclarations()) {
                    if (eventDeclaration != null && Objects.equals(eventDeclaration.getName(), eventName) &&
                            !eventDeclaration.getCallsites().isEmpty()) {
                        return new CtfTmfCallsite(eventDeclaration.getCallsites().get(0));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the closest matching callsite for given event name and instruction
     * pointer
     *
     * @param eventName
     *            The event name
     * @param ip
     *            The instruction pointer
     * @return The closest matching callsite
     * @since 3.0
     */
    public @Nullable CtfTmfCallsite getCallsite(String eventName, long ip) {
        CtfTmfCallsite callsite = null;
        CTFTrace trace = fTrace;
        if (trace != null) {
            for (ICTFStream stream : trace.getStreams()) {
                for (IEventDeclaration eventDeclaration : stream.getEventDeclarations()) {
                    if (eventDeclaration != null && Objects.equals(eventDeclaration.getName(), eventName)) {
                        for (CTFCallsite cs : eventDeclaration.getCallsites()) {
                            if (cs.getIp() >= ip && (callsite == null || cs.getIp() < callsite.getIntructionPointer())) {
                                callsite = new CtfTmfCallsite(cs);
                            }
                        }
                    }
                }
            }
        }
        return callsite;
    }

    /**
     * Get the CTF environment variables defined in this CTF trace, in <name, value>
     * form. This comes from the trace's CTF metadata.
     *
     * @return The CTF environment
     */
    public Map<String, String> getEnvironment() {
        CTFTrace trace = fTrace;
        if (trace == null) {
            return Collections.emptyMap();
        }
        return trace.getEnvironment();
    }

    /**
     * @since 3.0
     */
    @Override
    public UUID getUUID() {
        UUID uuid = fUUID;
        return uuid == null ? super.getUUID() : uuid;
    }

    // -------------------------------------------
    // ITmfPropertiesProvider
    // -------------------------------------------

    /**
     * @since 2.0
     */
    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();
        CTFTrace trace = fTrace;
        if (trace == null) {
            return properties;
        }
        properties.putAll(trace.getEnvironment());
        properties.put(Messages.CtfTmfTrace_HostID, getHostId());
        return properties;
    }

    // -------------------------------------------
    // Clocks
    // -------------------------------------------

    /**
     * Gets the clock offset with respect to POSIX.1 Epoch in cycles
     *
     * @return the clock offset with respect to POSIX.1 Epoch in cycles
     */
    public long getOffset() {
        CTFTrace trace = fTrace;
        if (trace != null) {
            return trace.getOffset();
        }
        return 0;
    }

    /**
     * Convert a CTF timestamp in CPU cycles to its equivalent in nanoseconds for
     * this trace.
     *
     * @param cycles
     *            The timestamp in cycles, relative to the clock offset
     * @return The timestamp in nanoseconds, relative to POSIX.1 Epoch
     */
    public long timestampCyclesToNanos(long cycles) {
        CTFTrace trace = fTrace;
        if (trace != null) {
            return trace.timestampCyclesToNanos(cycles);
        }
        return 0;
    }

    /**
     * Convert a CTF timestamp in nanoseconds to its equivalent in CPU cycles for
     * this trace.
     *
     * @param nanos
     *            The timestamp in nanoseconds, relative to POSIX.1 Epoch
     * @return The timestamp in cycles, relative to the clock offset
     */
    public long timestampNanoToCycles(long nanos) {
        CTFTrace trace = fTrace;
        if (trace != null) {
            return trace.timestampNanoToCycles(nanos);
        }
        return 0;
    }

    /**
     * Gets the list of declared events
     */
    @Override
    public Set<@NonNull CtfTmfEventType> getContainedEventTypes() {
        return ImmutableSet.copyOf(fContainedEventTypes.values());
    }

    /**
     * Register an event type to this trace.
     *
     * Public visibility so that {@link CtfTmfEvent#getType} can call it.
     *
     * FIXME This could probably be made cleaner?
     *
     * @param eventType
     *            The event type to register
     */
    public void registerEventType(CtfTmfEventType eventType) {
        fContainedEventTypes.put(eventType.getName(), eventType);
    }

    // -------------------------------------------
    // Parser
    // -------------------------------------------

    @Override
    public CtfTmfEvent parseEvent(ITmfContext context) {
        CtfTmfEvent event = null;
        if (context instanceof CtfTmfContext) {
            final ITmfContext tmpContext = seekEvent(context.getLocation());
            event = getNext(tmpContext);
        }
        return event;
    }

    /**
     * Sets the cache size for a CtfTmfTrace.
     */
    protected void setCacheSize() {
        setCacheSize(DEFAULT_CACHE_SIZE);
    }

    // -------------------------------------------
    // CtfIterator factory methods
    // -------------------------------------------

    /**
     * Get the event factory for this trace to generate new events for it.
     *
     * @return The event factory
     * @since 2.0
     */
    public @NonNull CtfTmfEventFactory getEventFactory() {
        return fEventFactory;
    }

    /**
     * Get an iterator to the trace
     *
     * @return an iterator to the trace
     */
    public ITmfContext createIterator() {
        try {
            return new CtfIterator(fTrace, this);
        } catch (CTFException e) {
            Activator.getDefault().logError(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Get an iterator to the trace, , which will initially point to the given
     * location/rank.
     *
     * @param ctfLocationData
     *            The initial timestamp the iterator will be pointing to
     * @param rank
     *            The initial rank
     * @return The new iterator
     */
    public ITmfContext createIterator(CtfLocationInfo ctfLocationData, long rank) {
        try {
            return new CtfIterator(fTrace, this, ctfLocationData, rank);
        } catch (CTFException e) {
            Activator.getDefault().logError(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Create the 'CtfIterator' object from a CtfTmfContext.
     *
     * @param context
     *            The iterator will initially be pointing to this context
     * @return A new CtfIterator object
     * @since 1.0
     */
    public ITmfContext createIteratorFromContext(CtfTmfContext context) {
        return fIteratorManager.getIterator(context);
    }

    /**
     * Dispose an iterator that was create with {@link #createIteratorFromContext}
     *
     * @param context
     *            The last context that was pointed to by the iterator (this is the
     *            'key' to find the correct iterator to dispose).
     * @since 1.0
     */
    public void disposeContext(CtfTmfContext context) {
        fIteratorManager.removeIterator(context);
    }

    // ------------------------------------------------------------------------
    // Timestamp transformation functions
    // ------------------------------------------------------------------------

    /**
     * @since 1.0
     */
    @Override
    public @NonNull ITmfTimestamp createTimestamp(long ts) {
        return TmfTimestamp.fromNanos(getTimestampTransform().transform(ts));
    }

    private static int fCheckpointSize = -1;

    @Override
    public synchronized int getCheckpointSize() {
        if (fCheckpointSize == -1) {
            TmfCheckpoint c = new TmfCheckpoint(TmfTimestamp.fromNanos(0), new CtfLocation(0, 0), 0);
            ByteBuffer b = ByteBuffer.allocate(ITmfCheckpoint.MAX_SERIALIZE_SIZE);
            b.clear();
            c.serialize(b);
            fCheckpointSize = b.position();
        }

        return fCheckpointSize;
    }

    @Override
    protected ITmfTraceIndexer createIndexer(int interval) {
        return new TmfBTreeTraceIndexer(this, interval);
    }

    @Override
    public ITmfLocation restoreLocation(ByteBuffer bufferIn) {
        return new CtfLocation(bufferIn);
    }

    @Override
    public boolean isComplete() {
        if (getResource() == null) {
            return true;
        }

        String host = null;
        String port = null;
        String sessionName = null;
        try {
            host = getResource().getPersistentProperty(CtfConstants.LIVE_HOST);
            port = getResource().getPersistentProperty(CtfConstants.LIVE_PORT);
            sessionName = getResource().getPersistentProperty(CtfConstants.LIVE_SESSION_NAME);
        } catch (CoreException e) {
            Activator.getDefault().logError(e.getMessage(), e);
            // Something happened to the resource, assume we won't get any more
            // data from it
            return true;
        }
        return host == null || port == null || sessionName == null;
    }

    @Override
    public void setComplete(final boolean isComplete) {
        super.setComplete(isComplete);
        try {
            if (isComplete) {
                getResource().setPersistentProperty(CtfConstants.LIVE_HOST, null);
                getResource().setPersistentProperty(CtfConstants.LIVE_PORT, null);
                getResource().setPersistentProperty(CtfConstants.LIVE_SESSION_NAME, null);
            }
        } catch (CoreException e) {
            Activator.getDefault().logError(e.getMessage(), e);
        }
    }

    /**
     * @return the number of estimated chunks of events read. This reads the file
     *         size of the trace and divides it by a factor and the average event
     *         size, this is not accurate but can give a ball park figure of how
     *         much is done.
     * @since 2.1
     */
    @Override
    public int size() {
        long size = 0;
        CTFTrace trace = fTrace;
        if (trace != null) {
            Iterable<ICTFStream> streams = trace.getStreams();
            for (ICTFStream stream : streams) {
                for (CTFStreamInput si : stream.getStreamInputs()) {
                    size += si.getFile().length();
                }
            }
        }
        return (int) (size / REDUCTION_FACTOR / CTF_AVG_EVENT_SIZE);
    }

    /**
     * @return the number of events divided a reduction factor. Is monotonic.
     * @since 2.1
     */
    @Override
    public int progress() {
        return (int) (getNbEvents() / REDUCTION_FACTOR);
    }

    /**
     * @since 3.0
     */
    @Override
    public long cyclesToNanos(long cycles) {
        // CTFTrace adds the clock offset in cycles to the input
        CTFTrace trace = fTrace;
        if (trace != null) {
            return fTrace.timestampCyclesToNanos(cycles - trace.getOffset());
        }
        return 0;
    }

    /**
     * @since 3.0
     */
    @Override
    public long nanosToCycles(long nanos) {
        // CTFTrace subtracts the clock offset in cycles from the output
        CTFTrace trace = fTrace;
        if (trace != null) {
            return trace.timestampNanoToCycles(nanos) + fTrace.getOffset();
        }
        return 0;
    }

    /**
     * @since 3.0
     */
    @Override
    public ITmfTimestamp readStart() {
        return getStartTime();
    }

    /**
     * @since 3.0
     */
    @Override
    public ITmfTimestamp readEnd() {
        try (CTFTraceReader reader = new CTFTraceReader(fTrace)) {
            reader.goToLastEvent();
            long end = reader.getEndTime();
            return createTimestamp(end);
        } catch (CTFException e) {
            return null;
        }
    }

    @Override
    public Path trim(@NonNull TmfTimeRange range, @NonNull Path destinationPath, @NonNull IProgressMonitor monitor) throws CoreException {
        CTFTrace trace = fTrace;
        if (trace == null) {
            return null;
        }
        try {
            CTFTraceWriter ctfWriter = new CTFTraceWriter(trace);
            String tracePath = destinationPath.toAbsolutePath().toString();
            ctfWriter.copyPackets(range.getStartTime().toNanos(), range.getEndTime().toNanos(), tracePath);
            return destinationPath;
        } catch (CTFException e) {
            Activator.getDefault().logError("Error writing trace : " + destinationPath, e); //$NON-NLS-1$
        }
        return null;
    }

}
