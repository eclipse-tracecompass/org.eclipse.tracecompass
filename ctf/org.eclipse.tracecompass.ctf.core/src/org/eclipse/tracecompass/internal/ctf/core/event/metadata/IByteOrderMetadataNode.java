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
 * Interface for byte order metadata nodes. Abstracts the differences between
 * CTF1 (TSDL/ANTLR) and CTF2 (JSON) byte order representations, allowing the
 * {@link org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.ByteOrderParser}
 * to work without instanceof checks.
 *
 * @author Eclipse Trace Compass
 */
public interface IByteOrderMetadataNode extends ICTFMetadataNode {

    /**
     * Get the byte order string value (e.g., "le", "be", "native", "network",
     * "little-endian", "big-endian")
     *
     * @return the byte order string, or null if not available
     */
    @Nullable String getByteOrderString();
}
