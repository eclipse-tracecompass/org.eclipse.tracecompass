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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF2 fixed-length bit array definition.
 *
 * @since 5.3
 * @author Arnaud Fiorini
 */
public class BitArrayDefinition extends Definition {

    private final byte[] fValue;

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
    public BitArrayDefinition(@NonNull BitArrayDeclaration declaration, IDefinitionScope definitionScope, @NonNull String fieldName, byte[] value) {
        super(declaration, definitionScope, fieldName);
        fValue = value;
    }

    @Override
    public BitArrayDeclaration getDeclaration() {
        return (BitArrayDeclaration) super.getDeclaration();
    }

    /**
     * @return the bits of the field, element i of the bit array being bit
     *         (i % 8) of byte (i / 8)
     */
    public byte[] getValue() {
        return fValue;
    }

    /**
     * Check whether a bit is set
     *
     * @param index
     *            the element index in the bit array
     * @return true if the bit is set, false if it is cleared or out of range
     */
    public boolean isBitSet(long index) {
        if (index < 0 || index >= getDeclaration().getLength()) {
            return false;
        }
        return ((fValue[(int) (index / Byte.SIZE)] >>> (index % Byte.SIZE)) & 1) != 0;
    }

    @Override
    public String toString() {
        int length = getDeclaration().getLength();
        StringBuilder sb = new StringBuilder("0b"); //$NON-NLS-1$
        for (int i = length - 1; i >= 0; i--) {
            sb.append(isBitSet(i) ? '1' : '0');
        }
        return sb.toString();
    }
}
