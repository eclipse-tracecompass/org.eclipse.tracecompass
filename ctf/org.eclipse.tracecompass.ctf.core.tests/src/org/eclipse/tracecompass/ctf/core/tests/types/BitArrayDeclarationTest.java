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
package org.eclipse.tracecompass.ctf.core.tests.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteOrder;

import org.eclipse.tracecompass.ctf.core.event.types.BitArrayDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.BitOrder;
import org.eclipse.tracecompass.ctf.core.event.types.BooleanDeclaration;
import org.junit.Test;

/**
 * Test for the fixed-length bit array declaration class in CTF2, following the
 * specification: https://diamon.org/ctf/#fl-ba-fc
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("javadoc")
public class BitArrayDeclarationTest {

    @Test
    public void constructorTest() {
        assertNotNull(new BitArrayDeclaration(1, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1));
        assertNotNull(new BitArrayDeclaration(64, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 8));
        assertNotNull(new BitArrayDeclaration(Integer.MAX_VALUE, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 8));
    }

    /**
     * The length must be greater than zero
     */
    @Test(expected = IllegalArgumentException.class)
    public void zeroLengthTest() {
        new BitArrayDeclaration(0, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1);
    }

    /**
     * The length must be greater than zero
     */
    @Test(expected = IllegalArgumentException.class)
    public void negativeLengthTest() {
        new BitArrayDeclaration(-8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1);
    }

    @Test
    public void getterTest() {
        BitArrayDeclaration declaration = new BitArrayDeclaration(12, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 16);
        assertEquals(12, declaration.getLength());
        assertEquals(ByteOrder.BIG_ENDIAN, declaration.getByteOrder());
        assertEquals(BitOrder.LAST_TO_FIRST, declaration.getBitOrder());
        assertEquals(16, declaration.getAlignment());
        assertEquals(12, declaration.getMaximumSize());
    }

    @Test
    public void toStringTest() {
        BitArrayDeclaration declaration = new BitArrayDeclaration(12, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1);
        String result = declaration.toString();
        assertTrue(result.contains("bit array"));
        assertTrue(result.contains("length:12"));
        assertTrue(result.contains("byte order:" + ByteOrder.LITTLE_ENDIAN));
        assertTrue(result.contains("bit order:" + BitOrder.FIRST_TO_LAST));
        assertTrue(result.contains("alignment:1"));
    }

    @Test
    public void binaryEquivalentTest() {
        BitArrayDeclaration a = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        BitArrayDeclaration b = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        BitArrayDeclaration c = new BitArrayDeclaration(16, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        BitArrayDeclaration d = new BitArrayDeclaration(8, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 8);
        BitArrayDeclaration e = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1);
        BooleanDeclaration f = new BooleanDeclaration(8, ByteOrder.LITTLE_ENDIAN, 8);
        BitArrayDeclaration h = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.LAST_TO_FIRST, 8);
        BitArrayDeclaration i = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);

        assertTrue(a.isBinaryEquivalent(a));
        assertTrue(a.isBinaryEquivalent(b));
        assertTrue(b.isBinaryEquivalent(a));
        assertFalse(a.isBinaryEquivalent(c));
        assertFalse(a.isBinaryEquivalent(d));
        assertFalse(a.isBinaryEquivalent(e));
        assertFalse(a.isBinaryEquivalent(f));
        assertFalse(a.isBinaryEquivalent(h));
        assertTrue(a.isBinaryEquivalent(i));
        assertFalse(a.isBinaryEquivalent(null));
    }
}
