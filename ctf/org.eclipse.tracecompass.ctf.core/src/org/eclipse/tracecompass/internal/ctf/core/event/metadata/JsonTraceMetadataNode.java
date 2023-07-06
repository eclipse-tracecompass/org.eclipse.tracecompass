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

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.json.JSONObject;

import com.google.gson.annotations.SerializedName;

/**
 * Node to store the trace fragment of JSON Metadata for CTF2 traces
 *
 * @author Sehr Moosabhoy
 *
 */
public class JsonTraceMetadataNode extends CTFJsonMetadataNode {

    @SerializedName("uid")
    private String fUid;
    @SerializedName("environment")
    private JSONObject fEnvironment;
    @SerializedName("packet-header-field-class")
    private JsonStructureFieldMetadataNode fPacketHeader;

    /**
     * Constructor for a JsonTraceMetadataNode
     *
     * @param parent
     *            the parent of the new node
     * @param type
     *            the type of the new node
     * @param value
     *            the value of the new node
     */
    public JsonTraceMetadataNode(ICTFMetadataNode parent, String type, String value) {
        super(parent, type, value);
    }

    /**
     * Get the uid of the trace
     *
     * @return the uid
     */
    public String getUid() {
        return fUid;
    }

    /**
     * Get the environment of the trace
     *
     * @return the environment
     */
    public JSONObject getEnvironment() {
        return fEnvironment;
    }

    /**
     * Get the packet header of the trace
     *
     * @return the packet header
     */
    public JsonStructureFieldMetadataNode getPacketHeader() {
        return fPacketHeader;
    }

    @Override
    public void initialize() throws CTFException {
        super.initialize();
        if (fPacketHeader != null) {
            fPacketHeader.initialize();
            addChild(fPacketHeader);
            fPacketHeader.setParent(this);
        }
    }
}
