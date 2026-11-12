/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.isAnyUnaryString;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Adapter that wraps a CTF1 (TSDL/ANTLR) trace declaration node and implements
 * {@link ITraceDeclarationMetadataNode}. This allows the
 * TraceDeclarationParser to work uniformly with both ANTLR and JSON metadata
 * nodes without instanceof checks.
 *
 * An ANTLR trace declaration node has a left child (key) and a right child
 * (value), representing a single key-value assignment within the trace block.
 *
 * @author Eclipse Trace Compass
 */
public class AntlrTraceDeclarationMetadataNode implements ITraceDeclarationMetadataNode {

    private final ICTFMetadataNode fDelegate;
    private @Nullable String fKey;
    private @Nullable ICTFMetadataNode fValueNode;

    /**
     * Constructor. Wraps the given ANTLR trace declaration node and extracts
     * the key string and value node from its children.
     *
     * @param delegate
     *            the original ANTLR trace declaration metadata node to wrap
     */
    public AntlrTraceDeclarationMetadataNode(ICTFMetadataNode delegate) {
        fDelegate = delegate;
        parseKeyValue();
    }

    private void parseKeyValue() {
        ICTFMetadataNode leftNode = fDelegate.getChild(0);
        fValueNode = fDelegate.getChild(1);

        if (leftNode != null) {
            List<ICTFMetadataNode> leftStrings = leftNode.getChildren();
            if (leftStrings != null && !leftStrings.isEmpty() && isAnyUnaryString(leftStrings.get(0))) {
                try {
                    fKey = concatenateUnaryStrings(leftStrings);
                } catch (Exception e) {
                    fKey = null;
                }
            }
        }
    }

    @Override
    public boolean isPreamble() {
        return false;
    }

    @Override
    public boolean isKeyValueDeclaration() {
        return true;
    }

    @Override
    public int getTraceVersion() {
        return -1;
    }

    @Override
    public @Nullable UUID getTraceUUID() {
        return null;
    }

    @Override
    public @Nullable String getDeclarationKey() {
        return fKey;
    }

    @Override
    public @Nullable ICTFMetadataNode getDeclarationValue() {
        return fValueNode;
    }

    // --- ICTFMetadataNode delegation ---

    @Override
    public ICTFMetadataNode getParent() {
        return fDelegate.getParent();
    }

    @Override
    public ICTFMetadataNode getChild(String id) {
        return fDelegate.getChild(id);
    }

    @Override
    public ICTFMetadataNode getChild(int index) {
        return fDelegate.getChild(index);
    }

    @Override
    public String getText() {
        return fDelegate.getText();
    }

    @Override
    public String getType() {
        return fDelegate.getType();
    }

    @Override
    public void setParent(ICTFMetadataNode node) {
        fDelegate.setParent(node);
    }

    @Override
    public void addChild(ICTFMetadataNode node) {
        fDelegate.addChild(node);
    }

    @Override
    public List<ICTFMetadataNode> getChildren() {
        return fDelegate.getChildren();
    }

    @Override
    public int getChildCount() {
        return fDelegate.getChildCount();
    }

    @Override
    public ICTFMetadataNode getFirstChildWithType(String typeDeclaratorList) {
        return fDelegate.getFirstChildWithType(typeDeclaratorList);
    }
}
