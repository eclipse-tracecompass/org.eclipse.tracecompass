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
 * Node to store the structure field member of JSON Metadata for CTF2 traces
 *
 * @author Sehr Moosabhoy
 *
 */
public class JsonStructureFieldMemberMetadataNode extends CTFJsonMetadataNode {

    @SerializedName("name")
    private final String fName;
    @SerializedName("field-class")
    private final JsonObject fFieldClass;

    /**
     * Constructor for a JsonStructureFieldMemberMetadataNode
     *
     * @param parent
     *            the parent of the new node
     * @param type
     *            the type of the new node
     * @param value
     *            the value of the new node
     * @param name
     *            the name of the new structure field member
     * @param fieldClass
     *            the field class of the new structure field member
     */
    public JsonStructureFieldMemberMetadataNode(ICTFMetadataNode parent, String type, String value, String name, JsonObject fieldClass) {
        super(parent, type, value);
        this.fName = name;
        this.fFieldClass = fieldClass;
    }

    /**
     * Get the name of the structure field member
     *
     * @return the name
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the field class of the structure field member
     *
     * @return the field class
     */
    public JsonObject getFieldClass() {
        return fFieldClass;
    }

}
