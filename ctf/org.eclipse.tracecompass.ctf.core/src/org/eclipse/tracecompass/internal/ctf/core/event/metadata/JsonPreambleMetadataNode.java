/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sehr Moosabhoy - Initial implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

import com.google.gson.annotations.SerializedName;

/**
 * Node to store the preamble fragment of JSON Metadata for CTF2 traces
 *
 * @author Sehr Moosabhoy
 *
 */
public class JsonPreambleMetadataNode extends CTFJsonMetadataNode {
    /**
     * the uuid of a trace as a byte array
     */
    @SerializedName("uuid")
    private byte[] fUuid;

    /**
     * the version of the trace
     */
    @SerializedName("version")
    private int fVersion;

    /**
     * Constructor for a JsonPreambleMetadataNode
     *
     * @param parent
     *            the parent node
     * @param type
     *            the type of the node (Must be "preamble")
     * @param value
     *            value of the node (not used for a preamble node)
     */
    public JsonPreambleMetadataNode(ICTFMetadataNode parent, String type, String value) {
        super(parent, type, value);
    }

    /**
     * Get the version of the trace
     *
     * @return the version
     */
    public int getVersion() {
        return fVersion;
    }

    /**
     * Get the uuid of the trace as a UUID rather than a byte array
     *
     * @return the uuid
     */
    public UUID getUuid() {
        if (fUuid == null) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(fUuid);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

}
