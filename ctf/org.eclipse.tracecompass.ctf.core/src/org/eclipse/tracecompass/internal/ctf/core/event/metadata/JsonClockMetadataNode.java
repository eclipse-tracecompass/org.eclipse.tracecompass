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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 *
 * Node to store the clock fragment of JSON Metadata for CTF2 traces
 *
 * @author Sehr Moosabhoy
 *
 */
public class JsonClockMetadataNode extends CTFJsonMetadataNode {

    @SerializedName("name")
    private final String fName;
    @SerializedName("frequency")
    private final Long fFrequency;
    @SerializedName("origin")
    private JsonElement fOrigin;
    @SerializedName("offset-from-origin")
    private JsonObject fOffset;
    @SerializedName("precision")
    private int fPrecision;
    @SerializedName("description")
    private String fDescription;

    /**
     * Constructor for a JsonClockMetadataNode
     *
     * @param parent
     *            the parent of this node
     * @param type
     *            the type of this node
     * @param value
     *            the value of this node
     * @param name
     *            the name of the clock described in this node
     * @param frequency
     *            the frequency of the clock described in this node
     */
    public JsonClockMetadataNode(ICTFMetadataNode parent, String type, String value, String name, Long frequency) {
        super(parent, type, value);
        this.fName = name;
        this.fFrequency = frequency;
    }

    /**
     * Get the frequency of the node
     *
     * @return the frequency
     */
    public Long getFrequency() {
        return fFrequency;
    }

    /**
     * Get the clock origin of the node
     *
     * @return the origin
     */
    public JsonElement getOrigin() {
        return fOrigin;
    }

    /**
     * Get the offset of the node
     *
     * @return the offset
     */
    public JsonObject getOffset() {
        return fOffset;
    }

    /**
     * Get the description of the node
     *
     * @return the description
     */
    public String getDescription() {
        return fDescription;
    }

    /**
     * Get the precision of the node
     *
     * @return the precision
     */
    public int getPrecision() {
        return fPrecision;
    }

    /**
     * Get the name of the node
     *
     * @return the name
     */
    public String getName() {
        return fName;
    }

}
