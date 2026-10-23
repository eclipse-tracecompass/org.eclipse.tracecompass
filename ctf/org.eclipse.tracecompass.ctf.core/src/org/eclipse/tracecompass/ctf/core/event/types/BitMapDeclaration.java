/**********************************************************************
 * Copyright (c) 2026 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.ctf.core.event.types;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF2 declaration of a fixed-length bit map field.
 *
 * A fixed-length bit map is a fixed-length bit array with named flags. Each
 * flag is associated with a set of bit index ranges: a flag is active when at
 * least one of the bits of its ranges is set.
 *
 * @since 5.3
 * @author Arnaud Fiorini
 */
public class BitMapDeclaration extends BitArrayDeclaration {

    private final Map<String, List<IntegerRange>> fFlags;

    /**
     * Constructor
     *
     * @param length
     *            the number of bits of the bit map
     * @param byteOrder
     *            the order in which the bytes should be read
     * @param bitOrder
     *            the order in which the bits should be read
     * @param alignment
     *            alignment of the first bit of field relative to the beginning
     *            of the packet
     * @param flags
     *            the flags, mapping a flag name to the ranges of bit indexes
     *            of this flag
     */
    public BitMapDeclaration(int length, ByteOrder byteOrder, BitOrder bitOrder, long alignment, Map<String, List<IntegerRange>> flags) {
        if (flags.isEmpty()) {
            throw new IllegalArgumentException("Flags must not be empty."); //$NON-NLS-1$
        }
        super(length, byteOrder, bitOrder, alignment);
        fFlags = Collections.unmodifiableMap(new LinkedHashMap<>(flags));
    }

    @Override
    protected @NonNull BitMapDefinition createDefinition(IDefinitionScope definitionScope, @NonNull String fieldName, byte[] value) {
        return new BitMapDefinition(this, definitionScope, fieldName, value);
    }

    /**
     * @return the flags, mapping a flag name to the ranges of bit indexes of
     *         this flag
     */
    public Map<String, List<IntegerRange>> getFlags() {
        return fFlags;
    }

    @Override
    public String toString() {
        /* Only used for debugging */
        return "[declaration] bit map[length:" + getLength() + ", flags:" + fFlags.keySet() + ']'; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
