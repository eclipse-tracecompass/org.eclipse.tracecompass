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

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Interface for structure field metadata nodes. Abstracts the CTF2 (JSON)
 * structure field class representation, allowing parsers to work without
 * instanceof checks for JsonStructureFieldMetadataNode.
 *
 * A structure field has member classes and an optional minimum alignment.
 *
 * @author Eclipse Trace Compass
 */
public interface IStructureFieldMetadataNode extends ICTFMetadataNode {

    /**
     * Get the member classes of the structure field class.
     *
     * @return the list of member class nodes, or null if not set
     */
    @Nullable List<? extends ICTFMetadataNode> getStructureMemberClasses();

    /**
     * Get the minimum alignment of the structure field class.
     *
     * @return the minimum alignment (0 if not specified)
     */
    long getStructureMinimumAlignment();
}
