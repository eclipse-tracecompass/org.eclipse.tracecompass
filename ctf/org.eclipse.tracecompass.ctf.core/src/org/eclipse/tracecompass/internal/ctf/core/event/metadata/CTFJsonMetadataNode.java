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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * Nodes used to store one fragment of JSON Metadata for CTF2
 *
 * @author Sehr Moosabhoy
 */
public class CTFJsonMetadataNode implements ICTFMetadataNode {
    //deserialized attributes from Json
    @SerializedName("type")
    private String fType;
    @SerializedName("user-attributes")
    private Map<String, JsonObject> fUserAttributes;

    //other attributes added for convenience
    private ICTFMetadataNode fParent;
    private Map<String, ICTFMetadataNode> fChildren;
    private ArrayList<ICTFMetadataNode> fChildrenList;
    private final String fValue;


    /**
     * Constructor for a CTFJsonMetadataNode
     *
     * @param parent
     *            the parent node
     * @param type
     *            the fragment type of the node
     * @param value
     *            the value of the node
     */
    public CTFJsonMetadataNode(ICTFMetadataNode parent, String type, String value) {
        fParent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
        fType = Objects.requireNonNull(type);
        fValue = value;
        fChildren = new HashMap<>();
        fChildrenList = new ArrayList<>();
    }

    @Override
    public ICTFMetadataNode getParent() {
        return fParent;
    }

    @Override
    public ICTFMetadataNode getChild(String id) {
        return fChildren.get(id);
    }

    @Override
    public ICTFMetadataNode getChild(int index) {
        return fChildrenList.get(index);
    }

    @Override
    public String getText() {
        return fValue;
    }

    @Override
    public String getType() {
        return fType;
    }

    @Override
    public void setParent(ICTFMetadataNode node) {
        fParent = node;
    }

    @Override
    public void addChild(ICTFMetadataNode node) {
        fChildren.put(node.getType(), node);
        fChildrenList.add(node);
    }

    @Override
    public List<ICTFMetadataNode> getChildren() {
        return fChildrenList;
    }

    @Override
    public int getChildCount() {
        return fChildrenList.size();
    }

    @Override
    public ICTFMetadataNode getFirstChildWithType(String typeDeclaratorList) {
        for (ICTFMetadataNode child : fChildrenList) {
            if (child.getType().equals(typeDeclaratorList)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Get the user attributes of the node
     *
     * @return the userAttributes
     */
    public Map<String, JsonObject> getUserAttributes() {
        return fUserAttributes;
    }

    /**
     * Set the type of the node, specifically used for nodes that do not have a
     * type when created such as JsonStructureFieldMemberMetadataNode
     *
     * @param type
     *            the new type of the node
     */
    public void setType(String type) {
        fType = Objects.requireNonNull(type);
    }

    /**
     * Set the childrenList of the node
     *
     * @param childrenList
     *            the children of the node
     */
    public void setChildrenList(List<ICTFMetadataNode> childrenList) {
        fChildrenList = (ArrayList<ICTFMetadataNode>) childrenList;
    }

    /**
     * Helper method to initialize fields that aren't set by gson library
     *
     * @throws CTFException
     *             if type is null but node is created with gson
     */
    public void initialize() throws CTFException {
        if (fChildren == null) {
            fChildren = new HashMap<>();
        }
        if (fChildrenList == null) {
            fChildrenList = new ArrayList<>();
        }
        if (fType == null) {
            throw new CTFException("type of node cannot be null"); //$NON-NLS-1$
        }
    }
}
