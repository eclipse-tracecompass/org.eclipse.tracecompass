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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Interface for event declaration metadata nodes. Abstracts the differences
 * between CTF1 (TSDL/ANTLR) and CTF2 (JSON) event declaration
 * representations, allowing the
 * {@link org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.event.EventDeclarationParser}
 * to work without instanceof checks.
 *
 * For JSON events, all fields are available from the node in one call. For
 * ANTLR events, individual key-value assignment nodes are processed one at a
 * time and do not implement this interface.
 *
 * @author Eclipse Trace Compass
 */
public interface IEventDeclarationMetadataNode extends ICTFMetadataNode {

    /**
     * Get the event name.
     *
     * @return the event name, or null if not set
     */
    @Nullable String getEventName();

    /**
     * Get the event id.
     *
     * @return the event id (default 0)
     */
    long getEventId();

    /**
     * Get the data stream class id (stream id) this event belongs to.
     *
     * @return the data stream class id (default 0)
     */
    long getDataStreamClassId();

    /**
     * Get the specific context field class node, if present.
     *
     * @return the context field class node, or null if not defined
     */
    @Nullable ICTFMetadataNode getSpecificContextNode();

    /**
     * Get the payload field class node, if present.
     *
     * @return the payload field class node, or null if not defined
     */
    @Nullable ICTFMetadataNode getPayloadNode();
}
