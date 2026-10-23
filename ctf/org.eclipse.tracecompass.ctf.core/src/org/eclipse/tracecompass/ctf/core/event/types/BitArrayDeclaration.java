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
 * A CTF2 declaration of a fixed-length bit array field.
 *
 * A fixed-length bit array is a simple array of contiguous bits, without any
 * attached integer value meaning. The bits are numbered from 0 (least
 * significant) to length - 1 (most significant).
 *
 * @since 5.3
 * @author Arnaud Fiorini
 */
public class BitArrayDeclaration extends Declaration {

    private final int fLength;
    private final long fAlignment;
    private final ByteOrder fByteOrder;
    private final BitOrder fBitOrder;

    /**
     * Constructor
     *
     * @param length
     *            the number of bits of the bit array, between 1 and
     *            Integer.MAX_VALUE
     * @param byteOrder
     *            the order in which the bytes should be read
     * @param bitOrder
     *            the order in which the bits should be read
     * @param alignment
     *            alignment of the first bit of field relative to the beginning
     *            of the packet
     */
    public BitArrayDeclaration(int length, ByteOrder byteOrder, BitOrder bitOrder, long alignment) {
        if (length <= 0) {
            throw new IllegalArgumentException("Unsupported bit array length: " + length); //$NON-NLS-1$
        }
        fLength = length;
        fByteOrder = byteOrder;
        fBitOrder = bitOrder;
        fAlignment = alignment;
    }

    @Override
    public @NonNull BitArrayDefinition createDefinition(IDefinitionScope definitionScope, @NonNull String fieldName, @NonNull BitBuffer input) throws CTFException {
        ByteOrder byteOrder = input.getByteOrder();
        input.setByteOrder(fByteOrder);
        byte[] value = new byte[(int) ((fLength + 7L) / Byte.SIZE)];
        try {
            alignRead(input);
            /*
             * The buffer reads the bits in the natural order of the byte
             * order: the first decoded bit is the least significant bit of a
             * little-endian chunk and the most significant bit of a big-endian
             * chunk. Reverse the chunk when the bit order is not the natural
             * one.
             */
            boolean reverse = fBitOrder != BitOrder.fromByteOrder(fByteOrder);
            int read = 0;
            while (read < fLength) {
                int chunkLength = Math.min(Long.SIZE, fLength - read);
                long chunk = input.get(chunkLength, false);
                if (reverse) {
                    chunk = Long.reverse(chunk) >>> (Long.SIZE - chunkLength);
                }
                int offset = (fBitOrder == BitOrder.FIRST_TO_LAST) ? read : fLength - read - chunkLength;
                setBits(value, offset, chunk, chunkLength);
                read += chunkLength;
            }
        } finally {
            input.setByteOrder(byteOrder);
        }
        return createDefinition(definitionScope, fieldName, value);
    }

    private static void setBits(byte[] value, int offset, long bits, int length) {
        for (int i = 0; i < length; i++) {
            if (((bits >>> i) & 1L) != 0) {
                int index = offset + i;
                value[index / Byte.SIZE] |= (byte) (1 << (index % Byte.SIZE));
            }
        }
    }

    /**
     * Create the definition once the bits have been read
     *
     * @param definitionScope
     *            the definition scope
     * @param fieldName
     *            the field name
     * @param value
     *            the bits that were read, element i of the bit array being
     *            bit (i % 8) of byte (i / 8)
     * @return the definition
     */
    protected @NonNull BitArrayDefinition createDefinition(IDefinitionScope definitionScope, @NonNull String fieldName, byte[] value) {
        return new BitArrayDefinition(this, definitionScope, fieldName, value);
    }

    /**
     * @return the number of bits of the bit array
     */
    public int getLength() {
        return fLength;
    }

    /**
     * @return the byte order
     */
    public ByteOrder getByteOrder() {
        return fByteOrder;
    }

    /**
     * @return first to last or last to first
     */
    public BitOrder getBitOrder() {
        return fBitOrder;
    }

    @Override
    public long getAlignment() {
        return fAlignment;
    }

    @Override
    public int getMaximumSize() {
        return fLength;
    }

    @Override
    public String toString() {
        /* Only used for debugging */
        return "[declaration] bit array[length:" + fLength + ", byte order:" + fByteOrder + ", bit order:" + fBitOrder + ", alignment:" + fAlignment + ']'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    @Override
    public boolean isBinaryEquivalent(IDeclaration other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        BitArrayDeclaration o = (BitArrayDeclaration) other;
        return fLength == o.fLength && fAlignment == o.fAlignment && fByteOrder.equals(o.fByteOrder) && fBitOrder == o.fBitOrder;
    }
}
