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
 * Node to store the data stream fragment of JSON Metadata for CTF2 traces
 *
 * @author Sehr Moosabhoy
 *
 */
public class JsonDataStreamMetadataNode extends CTFJsonMetadataNode {

    @SerializedName("id")
    private int fId;
    @SerializedName("name")
    private String fName;
    @SerializedName("namespace")
    private String fNamespace;
    @SerializedName("default-clock-class-name")
    private String fDefaultClockName;
    @SerializedName("packet-context-field-class")
    private JsonStructureFieldMetadataNode fPacketContextFieldClass;
    @SerializedName("event-record-header-field-class")
    private JsonStructureFieldMetadataNode fEventRecordHeaderClass;
    @SerializedName("event-record-common-context-field-class")
    private JsonStructureFieldMetadataNode fEventRecordCommonContextClass;

    /**
     * Constructor for a JsonDataStreamMetadataNode
     *
     * @param parent
     *            the parent of the new node
     * @param type
     *            the type of the new node
     * @param value
     *            the value of the new node
     */
    public JsonDataStreamMetadataNode(ICTFMetadataNode parent, String type, String value) {
        super(parent, type, value);
    }

    /**
     * Get the namespace of the data stream
     *
     * @return the namespace
     */
    public String getNamespace() {
        return fNamespace;
    }

    /**
     * Get the name of the data stream
     *
     * @return the name
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the id of the data stream
     *
     * @return the id
     */
    public int getId() {
        return fId;
    }

    /**
     * Get the default clock name of the data stream
     *
     * @return the default clock name
     */
    public String getDefaultClockName() {
        return fDefaultClockName;
    }

    /**
     * Get the packet context field class of the data stream
     *
     * @return the packet context field class
     */
    public JsonStructureFieldMetadataNode getPacketContextFieldClass() {
        return fPacketContextFieldClass;
    }

    /**
     * Get the event record header class of the data stream
     *
     * @return the event record header class
     */
    public JsonStructureFieldMetadataNode getEventRecordHeaderClass() {
        return fEventRecordHeaderClass;
    }

    /**
     * Get the event record common context class of the data stream
     *
     * @return the event record common context class
     */
    public JsonStructureFieldMetadataNode getEventRecordCommonContextClass() {
        return fEventRecordCommonContextClass;
    }

    @Override
    public void initialize() throws CTFException {
        super.initialize();
        if (fPacketContextFieldClass != null) {
            fPacketContextFieldClass.initialize();
        }
        if (fEventRecordCommonContextClass != null) {
            fEventRecordCommonContextClass.initialize();
        }
        if (fEventRecordHeaderClass != null) {
            fEventRecordHeaderClass.initialize();
        }
    }
}
