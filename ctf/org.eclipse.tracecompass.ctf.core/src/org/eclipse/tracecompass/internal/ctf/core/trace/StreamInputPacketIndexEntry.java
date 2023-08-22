/*******************************************************************************
 * Copyright (c) 2011, 2015 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Matthew Khouzam - Initial API and implementation
 * Contributors: Simon Marchi - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.trace;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.ctf.core.CTFStrings;
import org.eclipse.tracecompass.ctf.core.event.types.Declaration;
import org.eclipse.tracecompass.ctf.core.event.types.EnumDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.FloatDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.SimpleDatatypeDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.StringDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDefinition;
import org.eclipse.tracecompass.ctf.core.trace.ICTFPacketDescriptor;
import org.eclipse.tracecompass.ctf.core.trace.IPacketReader;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * <b><u>StreamInputPacketIndexEntry</u></b>
 * <p>
 * Represents an entry in the index of event packets.
 */
public class StreamInputPacketIndexEntry implements ICTFPacketDescriptor {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\D*(\\d+)"); //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * Position of the start of the packet header in the file, in bits
     */
    private final long fOffsetBits;

    /**
     * Position of the start of the packet header in the file, in bytes
     */
    private final long fOffsetBytes;

    /**
     * Packet size, in bits
     */
    private final long fPacketSizeBits;

    /**
     * Content size, in bits
     */
    private final long fContentSizeBits;

    /**
     * Begin timestamp
     */
    private final long fTimestampBegin;

    /**
     * End timestamp
     */
    private final long fTimestampEnd;

    /**
     * How many lost events are there?
     */
    private final long fLostEvents;

    /**
     * Which target is being traced
     */
    private final String fTarget;
    private final long fTargetID;

    /**
     * Attributes of this index entry
     */
    private final @NonNull Map<String, Object> fAttributes;

    private final long fEndPacketHeaderBits;

    private final StructDefinition fStreamPacketContextDef;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructs an index entry.
     *
     * @param dataOffsetBits
     *            offset in the file for the start of data in bits
     * @param fileSizeBytes
     *            number of bytes in a file
     *
     *            TODO: Remove
     */

    public StreamInputPacketIndexEntry(long dataOffsetBits, long fileSizeBytes) {
        fAttributes = Collections.emptyMap();
        fContentSizeBits = (fileSizeBytes * Byte.SIZE);
        fPacketSizeBits = (fileSizeBytes * Byte.SIZE);
        fOffsetBits = dataOffsetBits;
        fOffsetBytes = bitsToBytes(dataOffsetBits);
        fLostEvents = 0;
        fTarget = ""; //$NON-NLS-1$
        fTargetID = 0;
        fTimestampBegin = 0;
        fTimestampEnd = Long.MAX_VALUE;
        fEndPacketHeaderBits = dataOffsetBits;
        fStreamPacketContextDef = null;
    }

    private static long bitsToBytes(long value) {
        return (long) Math.ceil(value / (double) Byte.SIZE);
    }

    /**
     * full Constructor
     *
     * @param dataOffsetBits
     *            offset in the file for the start of data in bits
     * @param streamPacketContextDef
     *            packet context
     * @param fileSizeBytes
     *            number of bytes in a file
     * @param lostSoFar
     *            number of lost events so far
     *
     *            TODO: Remove
     */
    public StreamInputPacketIndexEntry(long dataOffsetBits, StructDefinition streamPacketContextDef, long fileSizeBytes, long lostSoFar) {
        this(dataOffsetBits, streamPacketContextDef, fileSizeBytes, lostSoFar, dataOffsetBits);
    }

    /**
     * full Constructor
     *
     * @param dataOffsetBits
     *            offset in the file for the start of data in bits
     * @param streamPacketContextDef
     *            packet context
     * @param fileSizeBytes
     *            number of bytes in a file
     * @param lostSoFar
     *            number of lost events so far
     * @param endPacketHeaderBits
     *            end of packet headers
     */
    public StreamInputPacketIndexEntry(long dataOffsetBits, StructDefinition streamPacketContextDef, long fileSizeBytes, long lostSoFar, long endPacketHeaderBits) {
        fStreamPacketContextDef = streamPacketContextDef;
        fEndPacketHeaderBits = endPacketHeaderBits;
        fAttributes = computeAttributeMap(streamPacketContextDef);
        fContentSizeBits = computeContentSize(fileSizeBytes);
        fPacketSizeBits = computePacketSize(fileSizeBytes);
        fTimestampBegin = computeTsBegin();
        fTimestampEnd = computeTsEnd();
        fOffsetBits = dataOffsetBits;
        fOffsetBytes = bitsToBytes(dataOffsetBits);

        // LTTng Specific
        Target target = lookupTarget(streamPacketContextDef);
        fTarget = target.getLabel();
        fTargetID = target.getNumber();
        fLostEvents = computeLostEvents(lostSoFar);
    }

    /**
     * Copy constructor that updates the timestamp end
     *
     * @param entryToAdd
     *            the original {@link StreamInputPacketIndexEntry}
     * @param newTimestampEnd
     *            the new timestamp end
     */
    public StreamInputPacketIndexEntry(ICTFPacketDescriptor entryToAdd, long newTimestampEnd) {
        if (entryToAdd instanceof StreamInputPacketIndexEntry) {
            fStreamPacketContextDef = ((StreamInputPacketIndexEntry) entryToAdd).getStreamPacketContextDef();
        } else {
            fStreamPacketContextDef = null;
        }

        fEndPacketHeaderBits = entryToAdd.getPayloadStartBits();
        fAttributes = entryToAdd.getAttributes();
        fContentSizeBits = entryToAdd.getContentSizeBits();
        fPacketSizeBits = entryToAdd.getPacketSizeBits();
        fTimestampBegin = entryToAdd.getTimestampBegin();
        fTimestampEnd = newTimestampEnd;
        fOffsetBits = entryToAdd.getOffsetBits();
        fOffsetBytes = entryToAdd.getOffsetBits();

        // LTTng Specific
        fTarget = entryToAdd.getTarget();
        fTargetID = entryToAdd.getTargetId();
        fLostEvents = entryToAdd.getLostEvents();
    }

    private static @NonNull Map<String, Object> computeAttributeMap(StructDefinition streamPacketContextDef) {
        Builder<String, Object> attributeBuilder = ImmutableMap.<String, Object> builder();
        StructDeclaration decl = streamPacketContextDef.getDeclaration();
        for (String field : decl.getFieldsList()) {
            IDefinition id = streamPacketContextDef.lookupDefinition(field);
            String fieldName;
            if (id.getDeclaration() instanceof Declaration && ((Declaration) id.getDeclaration()).getRole() != null) {
                fieldName = ((Declaration) id.getDeclaration()).getRole();
            } else {
                fieldName = field.startsWith("_") ? field.substring(1) : field; //$NON-NLS-1$
            }
            if (id instanceof IntegerDefinition) {
                attributeBuilder.put(fieldName, ((IntegerDefinition) id).getValue());
            } else if (id instanceof FloatDefinition) {
                attributeBuilder.put(fieldName, ((FloatDefinition) id).getValue());
            } else if (id instanceof EnumDefinition) {
                final EnumDefinition enumDec = (EnumDefinition) id;
                attributeBuilder.put(fieldName, new AbstractMap.SimpleImmutableEntry<>(
                        NonNullUtils.checkNotNull(enumDec.getStringValue()),
                        NonNullUtils.checkNotNull(enumDec.getIntegerValue())));
            } else if (id instanceof StringDefinition) {
                attributeBuilder.put(fieldName, ((StringDefinition) id).getValue());
            }
        }
        return attributeBuilder.build();
    }

    private Long getPacketSize() {
        Long size = (Long) fAttributes.get(CTFStrings.PACKET_SIZE);
        if (size == null) {
            size = (Long) fAttributes.get(JsonMetadataStrings.PACKET_TOTAL_LENGTH);
        }
        return size;
    }

    private Long getContentSize() {
        Long size = (Long) fAttributes.get(CTFStrings.CONTENT_SIZE);
        if (size == null) {
            size = (Long) fAttributes.get(JsonMetadataStrings.PACKET_CONTENT_LENGTH);
        }
        return size;
    }

    private long computeContentSize(long fileSizeBytes) {
        Long contentSize = getContentSize();
        /* Read the content size in bits */
        if (contentSize != null) {
            return contentSize.longValue();
        }
        Long packetSize = getPacketSize();
        if (packetSize != null) {
            return packetSize.longValue();
        }
        return fileSizeBytes * Byte.SIZE;
    }

    private long computePacketSize(long fileSizeBytes) {
        Long packetSize = getPacketSize();
        /* Read the packet size in bits */
        if (packetSize != null) {
            return packetSize.longValue();
        }
        long contentSizeBits = computeContentSize(fileSizeBytes);
        if (contentSizeBits != 0) {
            return contentSizeBits;
        }
        return fileSizeBytes * Byte.SIZE;
    }

    private long computeTsBegin() {
        Long tsBegin = readTimestampBeginAttribute();
        /* Read the begin timestamp */
        if (tsBegin != null) {
            return tsBegin.longValue();
        }
        return 0;
    }

    private Long readTimestampBeginAttribute() {
        Long begin = (Long) fAttributes.get(CTFStrings.TIMESTAMP_BEGIN);
        if (begin == null) {
            begin = (Long) fAttributes.get(JsonMetadataStrings.DEFAULT_CLOCK_TIMESTAMP);
        }
        return begin;
    }

    private long computeTsEnd() {
        Long tsEnd = readTimestampEndAttribute();
        /* Read the end timestamp */
        if (tsEnd != null) {
            // check if tsEnd == unsigned long max value
            if (tsEnd == -1) {
                return Long.MAX_VALUE;
            }
            return tsEnd.longValue();
        }
        return Long.MAX_VALUE;
    }

    private Long readTimestampEndAttribute() {
        Long end = (Long) fAttributes.get(CTFStrings.TIMESTAMP_END);
        if (end == null) {
            end = (Long) fAttributes.get(JsonMetadataStrings.PACKET_END_TIMESTAMP);
        }
        return end;
    }

    private long computeLostEvents(long lostSoFar) {
        Long lostEvents = readLostAttribute();
        if (lostEvents != null) {
            return lostEvents - lostSoFar;
        }
        return 0;
    }

    private Long readLostAttribute() {
        Long lost = (Long) fAttributes.get(CTFStrings.EVENTS_DISCARDED);
        if (lost == null) {
            lost = (Long) fAttributes.get(JsonMetadataStrings.CURRENT_DISCARDED_EVENT_COUNT);
        }
        return lost;
    }

    private static class Target {
        private String fLabel;
        private long fNumber;

        public Target(String target, long targetID) {
            fLabel = target;
            fNumber = targetID;
        }

        public String getLabel() {
            return fLabel;
        }

        public long getNumber() {
            return fNumber;
        }
    }

    private Target lookupTarget(StructDefinition streamPacketContextDef) {
        Target ret = new Target(null, IPacketReader.UNKNOWN_CPU);
        boolean hasDevice = fAttributes.containsKey(CTFStrings.DEVICE);
        if (hasDevice) {
            IDefinition def = streamPacketContextDef.lookupDefinition(CTFStrings.DEVICE);
            if (def instanceof SimpleDatatypeDefinition) {
                SimpleDatatypeDefinition simpleDefinition = (SimpleDatatypeDefinition) def;
                ret = new Target(simpleDefinition.getStringValue(), simpleDefinition.getIntegerValue());
            } else if (def instanceof StringDefinition) {
                StringDefinition stringDefinition = (StringDefinition) def;
                ret = new Target(stringDefinition.getValue(), IPacketReader.UNKNOWN_CPU);
                final Matcher matcher = NUMBER_PATTERN.matcher(ret.getLabel());
                if (matcher.matches()) {
                    String number = matcher.group(1);
                    ret = new Target(ret.getLabel(), Integer.parseInt(number));
                }
            }
        } else {
            Long cpuId = (Long) fAttributes.get(CTFStrings.CPU_ID);
            if (cpuId != null) {
                ret = new Target("CPU" + cpuId.toString(), cpuId); //$NON-NLS-1$
            }
        }
        return ret;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    public boolean includes(long ts) {
        return (ts >= fTimestampBegin) && (ts <= fTimestampEnd);
    }

    @Override
    public String toString() {
        return "StreamInputPacketIndexEntry [offsetBits=" + fOffsetBits //$NON-NLS-1$
                + ", timestampBegin=" + fTimestampBegin + ", timestampEnd=" //$NON-NLS-1$ //$NON-NLS-2$
                + fTimestampEnd + "]"; //$NON-NLS-1$
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    @Override
    public long getOffsetBits() {
        return fOffsetBits;
    }

    @Override
    public long getPacketSizeBits() {
        return fPacketSizeBits;
    }

    @Override
    public long getContentSizeBits() {
        return fContentSizeBits;
    }

    @Override
    public long getTimestampBegin() {
        return fTimestampBegin;
    }

    @Override
    public long getTimestampEnd() {
        return fTimestampEnd;
    }

    @Override
    public long getLostEvents() {
        return fLostEvents;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return fAttributes;
    }

    @Override
    public String getTarget() {
        return fTarget;
    }

    @Override
    public long getTargetId() {
        return fTargetID;
    }

    @Override
    public long getOffsetBytes() {
        return fOffsetBytes;
    }

    @Override
    public long getPayloadStartBits() {
        return fEndPacketHeaderBits;
    }

    /**
     * Get the backing stream packet context
     *
     * @return the backing context definition
     */
    @Override
    public StructDefinition getStreamPacketContextDef() {
        return fStreamPacketContextDef;
    }
}
