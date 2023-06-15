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

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * Node to store the field class alias fragment of JSON Metadata for CTF2 traces
 *
 * @author Sehr Moosabhoy
 *
 */
public class JsonFieldClassAliasMetadataNode extends CTFJsonMetadataNode {

    @SerializedName("name")
    private final String fName;
    @SerializedName("field-class")
    private final JsonObject fFieldClass;

    /**
     * Constructor for a JsonFieldClassAliasMetadataNode
     *
     * @param parent
     *            the parent of the new node
     * @param type
     *            the type of the new node
     * @param value
     *            the value of the new node
     * @param name
     *            the name of the field class alias
     * @param fieldClass
     *            the field class described by this new node
     */
    public JsonFieldClassAliasMetadataNode(ICTFMetadataNode parent, String type, String value, String name, JsonObject fieldClass) {
        super(parent, type, value);
        this.fName = name;
        this.fFieldClass = fieldClass;
    }

    /**
     * Get the name of the field class alias
     *
     * @return the name
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the field class of the field class alias
     *
     * @return the field class
     */
    public JsonObject getFieldClass() {
        return fFieldClass;
    }

}
