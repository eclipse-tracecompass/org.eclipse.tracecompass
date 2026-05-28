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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF declaration of a boolean field
 *
 * It declares a certain amount of bits as a boolean value.
 *
 * @since 5.2
 * @author Arnaud Fiorini
 */
public class BooleanDeclaration extends Declaration {

    private int fLength;
    private long fAlignment;
    private ByteOrder fByteOrder;

    /**
     * @param length
     *            the size of the boolean field
     * @param byteOrder
     *            the order in which the bytes should be read
     * @param alignment
     *            alignment of the first bit of field relative to the beginning
     *            of the packet
     */
    public BooleanDeclaration(int length, ByteOrder byteOrder, long alignment) {
        fLength = length;
        fByteOrder = byteOrder;
        fAlignment = alignment;
    }

    @Override
    public @NonNull Definition createDefinition(IDefinitionScope definitionScope, @NonNull String fieldName, @NonNull BitBuffer input) throws CTFException {
        ByteOrder byteOrder = input.getByteOrder();
        input.setByteOrder(fByteOrder);
        boolean value = false;
        try {
            value = read(input);
        } finally {
            input.setByteOrder(byteOrder);
        }
        return new BooleanDefinition(this, definitionScope, fieldName, value);
    }

    private boolean read(BitBuffer input) throws CTFException {
        alignRead(input);
        for (int i = 0; i * 64 < fLength; i++) {
            if (input.get(Math.min(64, fLength - i * 64), false) != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getAlignment() {
        return fAlignment;
    }

    @Override
    public int getMaximumSize() {
        return fLength;
    }

    /**
     * @return the byte order enum value
     */
    public ByteOrder getByteOrder() {
        return fByteOrder;
    }

    @Override
    public boolean isBinaryEquivalent(IDeclaration other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        return fLength == other.getMaximumSize() && fAlignment == other.getAlignment();
    }
}
