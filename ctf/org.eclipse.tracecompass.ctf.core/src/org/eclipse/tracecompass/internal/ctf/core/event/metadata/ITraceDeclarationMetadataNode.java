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

import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Interface for trace declaration metadata nodes. Abstracts the differences
 * between CTF1 (TSDL/ANTLR) and CTF2 (JSON preamble) trace declaration
 * representations, allowing the
 * {@link org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.trace.TraceDeclarationParser}
 * to work without instanceof checks.
 *
 * @author Eclipse Trace Compass
 */
public interface ITraceDeclarationMetadataNode extends ICTFMetadataNode {

    /**
     * Check if this is a preamble node (CTF2 JSON) that contains version and
     * UUID information.
     *
     * @return true if this node contains preamble information (version/UUID)
     */
    boolean isPreamble();

    /**
     * Check if this is a key-value declaration node (CTF1 ANTLR) that contains
     * a left-side key and a right-side value.
     *
     * @return true if this node contains a key-value assignment
     */
    boolean isKeyValueDeclaration();

    /**
     * Get the trace version (only valid if {@link #isPreamble()} returns true).
     *
     * @return the trace version, or -1 if not available
     */
    int getTraceVersion();

    /**
     * Get the trace UUID (only valid if {@link #isPreamble()} returns true).
     *
     * @return the UUID, or null if not set
     */
    @Nullable UUID getTraceUUID();

    /**
     * Get the left-side key of the declaration (only valid if
     * {@link #isKeyValueDeclaration()} returns true).
     *
     * @return the key string, or null if not a key-value node
     */
    @Nullable String getDeclarationKey();

    /**
     * Get the right-side value node of the declaration (only valid if
     * {@link #isKeyValueDeclaration()} returns true).
     *
     * @return the value node, or null if not a key-value node
     */
    @Nullable ICTFMetadataNode getDeclarationValue();
}
