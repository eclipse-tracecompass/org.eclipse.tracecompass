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
 * Interface for stream declaration metadata nodes. Abstracts the differences
 * between CTF1 (TSDL/ANTLR) and CTF2 (JSON) stream declaration
 * representations, allowing the
 * {@link org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.stream.StreamDeclarationParser}
 * to work without instanceof checks.
 *
 * For JSON streams, all fields are available from the node in one call. For
 * ANTLR streams, individual key-value assignment nodes are processed one at a
 * time and do not implement this interface.
 *
 * @author Eclipse Trace Compass
 */
public interface IStreamDeclarationMetadataNode extends ICTFMetadataNode {

    /**
     * Get the stream id, or -1 if not set.
     *
     * @return the stream id, or -1 if not present
     */
    long getStreamId();

    /**
     * Get the event record header field class node, if present.
     *
     * @return the event record header node, or null if not defined
     */
    @Nullable ICTFMetadataNode getEventRecordHeaderNode();

    /**
     * Get the event record common context field class node, if present.
     *
     * @return the event record common context node, or null if not defined
     */
    @Nullable ICTFMetadataNode getEventRecordCommonContextNode();

    /**
     * Get the packet context field class node, if present.
     *
     * @return the packet context node, or null if not defined
     */
    @Nullable ICTFMetadataNode getPacketContextNode();
}
