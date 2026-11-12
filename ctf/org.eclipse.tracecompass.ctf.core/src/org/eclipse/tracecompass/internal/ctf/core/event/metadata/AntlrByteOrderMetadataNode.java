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

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Adapter that wraps a CTF1 (TSDL/ANTLR) byte order node and implements
 * {@link IByteOrderMetadataNode}. This allows the ByteOrderParser to work
 * uniformly with both ANTLR and JSON metadata nodes without instanceof checks.
 *
 * For ANTLR nodes, the byte order string is obtained by concatenating the
 * unary string children of the node.
 *
 * @author Eclipse Trace Compass
 */
public class AntlrByteOrderMetadataNode implements IByteOrderMetadataNode {

    private final ICTFMetadataNode fDelegate;
    private @Nullable String fByteOrderString;

    /**
     * Constructor. Wraps the given ANTLR byte order node and extracts the
     * byte order string from its children.
     *
     * @param delegate
     *            the original ANTLR byte order metadata node to wrap
     */
    public AntlrByteOrderMetadataNode(ICTFMetadataNode delegate) {
        fDelegate = delegate;
        try {
            fByteOrderString = concatenateUnaryStrings(delegate.getChildren());
        } catch (Exception e) {
            fByteOrderString = null;
        }
    }

    @Override
    public @Nullable String getByteOrderString() {
        return fByteOrderString;
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
