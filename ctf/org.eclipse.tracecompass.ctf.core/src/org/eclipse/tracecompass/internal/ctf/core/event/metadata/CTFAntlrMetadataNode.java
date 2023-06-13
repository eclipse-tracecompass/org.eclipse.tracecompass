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

import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Created to decouple ANTLR from CTF metadata parsing This node provides
 * definitions for previously used CommonTree methods
 *
 * @author Sehr Moosabhoy
 *
 */
public class CTFAntlrMetadataNode implements ICTFMetadataNode {
    private ICTFMetadataNode fParent;
    private final Map<String, ICTFMetadataNode> fChildren;
    private final String fValue;
    private final String fType;
    private final ArrayList<ICTFMetadataNode> fChildrenList;

    /**
     * @param parent
     *            the parent node of the node being created
     * @param type
     *            the int type of the node
     * @param value
     *            the information contained by the node
     */
    public CTFAntlrMetadataNode(ICTFMetadataNode parent, String type, String value) {
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
    public void addChild(ICTFMetadataNode child) {
        fChildren.put(child.getType(), child);
        fChildrenList.add(child);
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
    public String getText() {
        return fValue;
    }

    @Override
    public ICTFMetadataNode getChild(int index) {
        return fChildrenList.get(index);
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
    public List<ICTFMetadataNode> getChildren() {
        return fChildrenList;
    }

    @Override
    public int getChildCount() {
        return fChildrenList.size();
    }

    @Override
    public ICTFMetadataNode getFirstChildWithType(String type) {
        if (type != null) {
            for (ICTFMetadataNode child : this.getChildren()) {
                if (type.equals(child.getType())) {
                    return child;
                }
            }
        }
        return null;
    }

}
