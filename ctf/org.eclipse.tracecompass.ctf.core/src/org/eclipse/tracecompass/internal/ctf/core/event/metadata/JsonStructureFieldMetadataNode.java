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

import java.util.List;

import org.eclipse.tracecompass.ctf.core.CTFException;
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
    private List<JsonStructureFieldMemberMetadataNode> fMemberClasses;
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
    public List<JsonStructureFieldMemberMetadataNode> getMemberClasses() {
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

    /**
     * Set the member classes of the structure field class
     *
     * @param memberClasses
     *            the Member Classes
     */
    public void setMemberClasses(List<JsonStructureFieldMemberMetadataNode> memberClasses) {
        fMemberClasses = memberClasses;
    }

    @Override
    public void initialize() throws CTFException {
        super.initialize();
        if (fMemberClasses != null) {
            for (JsonStructureFieldMemberMetadataNode member : fMemberClasses) {
                member.initialize();
                addChild(member);
                member.setParent(this);
            }
        }
    }
}
