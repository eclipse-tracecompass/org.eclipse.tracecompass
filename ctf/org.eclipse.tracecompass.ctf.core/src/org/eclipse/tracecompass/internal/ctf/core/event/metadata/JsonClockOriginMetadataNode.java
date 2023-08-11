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

import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

import com.google.gson.annotations.SerializedName;

/**
 * Node to store the clock origin object for CTF2 traces
 *
 * @author Sehr Moosabhoy
 */
public class JsonClockOriginMetadataNode extends CTFJsonMetadataNode {

    @SerializedName("namespace")
    private String fNamespace;
    @SerializedName("name")
    private final String fName;
    @SerializedName("uid")
    private final String fUid;

    /**
     * Constructor for a JsonClockOriginMetadataNode
     *
     * @param parent
     *            the parent of this node
     * @param type
     *            the type of this node
     * @param value
     *            the value of this node
     * @param name
     *            the name of the clock origin described in this node
     * @param uid
     *            the uid of the clock origin described in this node
     */
    public JsonClockOriginMetadataNode(ICTFMetadataNode parent, String type, String value, String name, String uid) {
        super(parent, type, value);
        fName = name;
        fUid = uid;
    }

    /**
     * Get the namespace of the node
     *
     * @return the namespace
     */
    public String getNamespace() {
        return fNamespace;
    }

    /**
     * Get the name of the node
     *
     * @return the name
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the uid of the node
     *
     * @return the uid
     */
    public String getUid() {
        return fUid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JsonClockOriginMetadataNode) {
            JsonClockOriginMetadataNode clock = (JsonClockOriginMetadataNode) obj;
            if (fName.equals(clock.getName()) && fNamespace.equals(clock.getNamespace()) && fUid.equals(clock.getUid())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
