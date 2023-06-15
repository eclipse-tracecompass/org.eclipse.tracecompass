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
 * Node to store the structure field class of JSON Metadata for CTF2 traces
 *
 * @author Sehr Moosabhoy
 *
 */
public class JsonStructureFieldMetadataNode extends CTFJsonMetadataNode {

    @SerializedName("member-classes")
    private JsonStructureFieldMemberMetadataNode[] fMemberClasses;
    @SerializedName("minimum-alignment")
    private int fMinimumAlignment;

    /**
     * Constructor for a JsonStructureFieldMetadataNode
     *
     * @param parent
     *            the parent of the new node
     * @param type
     *            the type of the new node
     * @param value
     *            the value of the new node
     */
    public JsonStructureFieldMetadataNode(ICTFMetadataNode parent, String type, String value) {
        super(parent, type, value);
    }

    /**
     * Get the member classes of the structure field class
     *
     * @return the member classes
     */
    public JsonStructureFieldMemberMetadataNode[] getMemberClasses() {
        return fMemberClasses;
    }

    /**
     * Get the minimum alignment of the structure field class
     *
     * @return the minimum alignment
     */
    public int getMinimumAlignment() {
        return fMinimumAlignment;
    }

}
