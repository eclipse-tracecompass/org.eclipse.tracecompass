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

import com.google.gson.JsonElement;

/**
 * Interface for field member metadata nodes. Abstracts the CTF2 (JSON)
 * structure field member representation, allowing parsers to work without
 * instanceof checks for JsonStructureFieldMemberMetadataNode.
 *
 * A field member has a name, a field class (describing the type), and
 * optionally a role and field ranges (for variant members).
 *
 * @author Eclipse Trace Compass
 */
public interface IFieldMemberMetadataNode extends ICTFMetadataNode {

    /**
     * Get the name of the structure field member.
     *
     * @return the member name
     */
    @Nullable String getMemberName();

    /**
     * Get the field class of the structure field member.
     *
     * @return the field class as a JsonElement
     */
    @Nullable JsonElement getFieldClass();

    /**
     * Get the role of the field class in this member (e.g.,
     * "packet-magic-number").
     *
     * @return the role, or null if not set
     */
    @Nullable String getRole();

    /**
     * Get the field ranges for this member class (applicable to variant field
     * classes).
     *
     * @return the field ranges as a JsonElement, or null if not applicable
     */
    @Nullable JsonElement getFieldRanges();
}
