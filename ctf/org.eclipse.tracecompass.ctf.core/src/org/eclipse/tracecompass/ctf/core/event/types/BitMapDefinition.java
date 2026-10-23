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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF2 fixed-length bit map definition.
 *
 * @since 5.3
 * @author Arnaud Fiorini
 */
public class BitMapDefinition extends BitArrayDefinition {

    private final List<String> fActiveFlags;

    /**
     * Constructor
     *
     * @param declaration
     *            the parent declaration
     * @param definitionScope
     *            the parent scope
     * @param fieldName
     *            the field name
     * @param value
     *            the bits of the field, element i of the bit array being bit
     *            (i % 8) of byte (i / 8)
     */
    public BitMapDefinition(@NonNull BitMapDeclaration declaration, IDefinitionScope definitionScope, @NonNull String fieldName, byte[] value) {
        super(declaration, definitionScope, fieldName, value);
        List<String> activeFlags = new ArrayList<>();
        for (Entry<String, List<IntegerRange>> flag : declaration.getFlags().entrySet()) {
            if (isAnyBitSet(flag.getValue())) {
                activeFlags.add(flag.getKey());
            }
        }
        fActiveFlags = List.copyOf(activeFlags);
    }

    private boolean isAnyBitSet(List<IntegerRange> ranges) {
        for (IntegerRange range : ranges) {
            long end = Math.min(range.getEnd(), getDeclaration().getLength() - 1L);
            for (long i = Math.max(range.getStart(), 0); i <= end; i++) {
                if (isBitSet(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public BitMapDeclaration getDeclaration() {
        return (BitMapDeclaration) super.getDeclaration();
    }

    /**
     * Get the names of the active flags, in declaration order. A flag is active
     * when at least one of its bits is set.
     *
     * @return the active flag names
     */
    public List<String> getActiveFlags() {
        return fActiveFlags;
    }

    /**
     * Check if a flag is active
     *
     * @param flagName
     *            the flag name
     * @return true if the flag is active
     */
    public boolean isFlagActive(String flagName) {
        return fActiveFlags.contains(flagName);
    }

    @Override
    public String toString() {
        return super.toString() + " (" + String.join(" | ", fActiveFlags) + ')'; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
