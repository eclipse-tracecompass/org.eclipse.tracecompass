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
package org.eclipse.tracecompass.internal.ctf.core.event.types;

import java.util.List;

/**
 * Interface provides common metadata node methods that will be implemented by
 * metadata nodes that handle TSDL and JSON for CTF1 and CTF2 traces
 *
 * @author Sehr Moosabhoy
 *
 */
public interface ICTFMetadataNode {
    /**
     * Get the parent node
     *
     * @return the parent node
     */
    ICTFMetadataNode getParent();

    /**
     * Get a child with a given id
     *
     * @param id
     *            the id
     * @return child of the node matching the ID, or null if none is found
     */
    ICTFMetadataNode getChild(String id);

    /**
     * Get a child with a given index
     *
     * @param index
     *            the index
     * @return child of the node matching the index, or null if none is found
     */
    ICTFMetadataNode getChild(int index);

    /**
     * Get the text of the node
     *
     * @return the value of the node or null if it has not been set
     */
    String getText();

    /**
     * Get the type of the node
     *
     * @return the type of the node or null if it has not been set
     */
    int getType();

    /**
     * Set the parent of the node
     *
     * @param node
     *            the new parent of the node
     */
    void setParent(ICTFMetadataNode node);

    /**
     * Add a child to the node
     *
     * @param node
     *            the new child node to be added
     */
    void addChild(ICTFMetadataNode node);

    /**
     * Get all children of the node
     *
     * @return a list of all of the child nodes, or null if there aren't any
     */
    List<ICTFMetadataNode> getChildren();

    /**
     * Get the number of children of this node
     *
     * @return the number of children
     */
    int getChildCount();

    /**
     * Get the first child of this node which has the specified type
     *
     * @param typeDeclaratorList
     *            the specified child type
     * @return first child of the node matching the type, or null if none is
     *         found
     */
    ICTFMetadataNode getFirstChildWithType(int typeDeclaratorList);
}
