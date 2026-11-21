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

import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Interface for event metadata nodes. Abstracts the differences between CTF1
 * (TSDL/ANTLR) and CTF2 (JSON) event representations, allowing the
 * {@link org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.event.EventParser}
 * to work without instanceof checks.
 *
 * For JSON events, the event data is self-contained in a single node. For ANTLR
 * events, the data is spread across children that must be iterated.
 *
 * @author Eclipse Trace Compass
 */
public interface IEventMetadataNode extends ICTFMetadataNode {

    /**
     * Check whether this event node is self-contained (i.e., it contains all
     * event declaration information in one node, as in CTF2 JSON format).
     *
     * When true, the node should be passed directly to
     * EventDeclarationParser. When false, the node's children should be
     * iterated and dispatched to appropriate sub-parsers.
     *
     * @return true if the event data is self-contained
     */
    boolean isSelfContained();
}
