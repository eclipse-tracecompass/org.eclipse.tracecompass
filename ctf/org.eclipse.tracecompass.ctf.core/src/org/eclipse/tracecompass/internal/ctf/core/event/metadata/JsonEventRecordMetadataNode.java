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

import com.google.gson.annotations.SerializedName;

/**
 * Node to store the event record fragment of JSON Metadata for CTF2 traces
 *
 * @author Sehr Moosabhoy
 *
 */
public class JsonEventRecordMetadataNode extends CTFJsonMetadataNode {

    @SerializedName("id")
    private int fId;
    @SerializedName("data-stream-class-id")
    private int fDataStreamClassId;
    @SerializedName("name")
    private String fName;
    @SerializedName("namespace")
    private String fNamespace;
    @SerializedName("specific-context-field-class")
    private JsonStructureFieldMetadataNode fSpecificContextClass;
    @SerializedName("payload-field-class")
    private JsonStructureFieldMetadataNode fPayloadFieldClass;

    /**
     * Constructor for a JsonEventRecordMetadataNode
     *
     * @param parent
     *            the parent of the new node
     * @param type
     *            the type of the new node
     * @param value
     *            the value of the new node
     */
    public JsonEventRecordMetadataNode(ICTFMetadataNode parent, String type, String value) {
        super(parent, type, value);
    }

    /**
     * Get the namespace of the event record
     *
     * @return the namespace
     */
    public String getNamespace() {
        return fNamespace;
    }

    /**
     * Get the name of the event record
     *
     * @return the name
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the id of the event record
     *
     * @return the id
     */
    public int getId() {
        return fId;
    }

    /**
     * Get the data stream class id of the event record
     *
     * @return the data stream class id
     */
    public int getDataStreamClassId() {
        return fDataStreamClassId;
    }

    /**
     * Get the specific context class of the event record
     *
     * @return the specific context class
     */
    public JsonStructureFieldMetadataNode getSpecificContextClass() {
        return fSpecificContextClass;
    }

    /**
     * Get the payload field class of the event record
     *
     * @return the payload field class
     */
    public JsonStructureFieldMetadataNode getPayloadFieldClass() {
        return fPayloadFieldClass;
    }

    @Override
    public void initialize() throws CTFException {
        super.initialize();
        if (fSpecificContextClass != null) {
            fSpecificContextClass.initialize();
            addChild(fSpecificContextClass);
            fSpecificContextClass.setParent(this);
        }
        if (fPayloadFieldClass != null) {
            fPayloadFieldClass.initialize();
            addChild(fPayloadFieldClass);
            fPayloadFieldClass.setParent(this);
        }
    }
}
