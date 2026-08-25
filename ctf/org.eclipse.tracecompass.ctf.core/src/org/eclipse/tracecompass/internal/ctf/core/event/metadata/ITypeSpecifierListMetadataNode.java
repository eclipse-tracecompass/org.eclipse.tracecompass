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
 * Interface for type specifier list metadata nodes. Abstracts the differences
 * between CTF1 (TSDL/ANTLR) and CTF2 (JSON) type specifier list
 * representations, allowing the
 * {@link org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeSpecifierListParser}
 * to work without instanceof checks.
 *
 * For ANTLR, the type specifier list's first child determines the type. For
 * JSON, the node itself describes the type.
 *
 * @author Eclipse Trace Compass
 */
public interface ITypeSpecifierListMetadataNode extends ICTFMetadataNode {

    /**
     * Get the first child node that determines the type, or the node itself if
     * it is self-describing (as in JSON).
     *
     * @return the type-determining node
     */
    @Nullable ICTFMetadataNode getTypeSpecifierNode();

    /**
     * Get the type string of the first child (e.g., "INTEGER", "STRUCT",
     * "VARIANT"), or null if the type is determined by the node itself.
     *
     * @return the type string, or null if not applicable
     */
    @Nullable String getTypeSpecifierType();
}
