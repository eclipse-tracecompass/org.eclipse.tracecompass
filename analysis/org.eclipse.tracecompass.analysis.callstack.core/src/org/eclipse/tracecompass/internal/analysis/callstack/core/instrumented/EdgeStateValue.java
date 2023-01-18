/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * {@link CustomStateValue} to serialize edges from one {@link HostThread} to
 * another. Also contains the scope id, start and duration must come from the
 * encapsulating interval.
 *
 * @author Loic Prieur-Drevon
 */
public class EdgeStateValue extends CustomStateValue {

    /**
     * The Custom Type value ID for {@link EdgeStateValue}s.
     */
    public static final byte CUSTOM_TYPE_ID = 22;
    /**
     * {@link org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue.CustomStateValueFactory}
     * for {@link EdgeStateValue}s
     */
    public static final CustomStateValueFactory EDGE_STATE_VALUE_FACTORY = b -> new EdgeStateValue(b.getInt(), readHostThread(b), readHostThread(b));

    @SuppressWarnings("null")
    private static final Comparator<HostThread> HOST_COMPARATOR = Comparator.comparing(HostThread::getHost)
            .thenComparingInt(HostThread::getTid);

    @SuppressWarnings("null")
    private static final Comparator<EdgeStateValue> COMPARATOR = Comparator
            .comparing(EdgeStateValue::getSource, HOST_COMPARATOR)
            .thenComparing(EdgeStateValue::getDestination, HOST_COMPARATOR);

    private static final HostThread readHostThread(ISafeByteBufferReader buffer) {
        String host = buffer.getString();
        int tid = buffer.getInt();
        return new HostThread(host, tid);
    }

    private final int fId;
    private final HostThread fSrc;
    private final HostThread fDst;

    /**
     * Constructor
     *
     * @param id
     *            edge ID
     * @param src
     *            source {@link HostThread}
     * @param dst
     *            destination {@link HostThread}
     */
    public EdgeStateValue(int id, HostThread src, HostThread dst) {
        fId = id;
        fSrc = src;
        fDst = dst;
    }

    /**
     * Get the edge ID
     *
     * @return the edge ID
     */
    public int getId() {
        return fId;
    }

    /**
     * Getter for the source thread
     *
     * @return the source {@link HostThread} object
     */
    public HostThread getSource() {
        return fSrc;
    }

    /**
     * Getter for the destination thread
     *
     * @return the destination {@link HostThread} object.
     */
    public HostThread getDestination() {
        return fDst;
    }

    @Override
    public int compareTo(@NonNull ITmfStateValue o) {
        if (o instanceof EdgeStateValue) {
            return COMPARATOR.compare(this, (EdgeStateValue) o);
        }

        return 1;
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        buffer.putInt(fId);

        // TODO deduplicate host name when applicable.
        serializeHostThread(fSrc, buffer);
        serializeHostThread(fDst, buffer);
    }

    private static void serializeHostThread(HostThread hostThread, ISafeByteBufferWriter buffer) {
        buffer.putString(hostThread.getHost());
        buffer.putInt(hostThread.getTid());
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 3 * Integer.BYTES; // fId and TIDs
        /*
         * String's length + 3 (2 bytes for size, 1 byte for \0 at the end)
         */
        size += fSrc.getHost().getBytes(StandardCharsets.UTF_8).length + 3;
        size += fDst.getHost().getBytes(StandardCharsets.UTF_8).length + 3;
        return size;
    }

    @Override
    public String toString() {
        return "Edge: id=" + fId + ", src=" + fSrc + ", dst=" + fDst; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public int hashCode() {
        return Objects.hash(fId, fSrc, fDst);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof EdgeStateValue)) {
            return false;
        }
        EdgeStateValue other = (EdgeStateValue) obj;
        return fId == other.fId && Objects.equals(fSrc, other.fSrc) && Objects.equals(fDst, other.fDst);
    }
}
