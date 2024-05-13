/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Matthew Khouzam - Initial API and implementation
 *     Marc-Andre Laperle - Add min/maximum for validation
 *******************************************************************************/

package org.eclipse.tracecompass.ctf.core.tests.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.nio.ByteOrder;

import org.eclipse.tracecompass.ctf.core.event.types.Encoding;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>IntegerDeclarationTest</code> contains tests for the class
 * <code>{@link IntegerDeclaration}</code>.
 *
 * @author ematkho
 * @version $Revision: 1.0 $
 */
public class IntegerDeclarationTest {

    private IntegerDeclaration fixture;

    /**
     * Perform pre-test initialization.
     */
    @Before
    public void setUp() {
        fixture = IntegerDeclaration.createDeclaration(1, false, 1, ByteOrder.BIG_ENDIAN,
                Encoding.ASCII, "", 32, null);
    }

    /**
     * Run the IntegerDeclaration(int,boolean,int,ByteOrder,Encoding)
     * constructor test.
     */
    @Test
    public void testIntegerDeclaration() {
        int len = 1;
        boolean signed = false;
        int base = 1;
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        Encoding encoding = Encoding.ASCII;

        IntegerDeclaration result = IntegerDeclaration.createDeclaration(len, signed, base,
                byteOrder, encoding, "", 16, null);

        assertNotNull(result);
        assertEquals(1, result.getBase());
        assertEquals(false, result.isCharacter());
        String outputValue = "[declaration] integer[";
        assertEquals(outputValue,
                result.toString().substring(0, outputValue.length()));
        assertEquals(1, result.getLength());
        assertEquals(false, result.isSigned());
    }

    /**
     * Run the createVarintDeclaration method test (boolean, int, String,
     * boolean)
     */
    @Test
    public void testVarintDeclaration() {
        boolean signed = false;
        boolean varint = true;
        int base = 1;
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        IntegerDeclaration result = IntegerDeclaration.createVarintDeclaration(signed, base, null, varint);

        assertNotNull(result);
        assertEquals(1, result.getBase());
        assertEquals(true, result.isVarint());
        String outputValue = "[declaration] integer[";
        assertEquals(outputValue,
                result.toString().substring(0, outputValue.length()));
        assertEquals(byteOrder, result.getByteOrder());
        assertEquals(false, result.isSigned());
    }

    /**
     * Test the factory part more rigorously to make sure there are no
     * regressions
     */
    @Test
    public void testIntegerDeclarationBruteForce() {
        ByteOrder[] bos = { ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN };
        Encoding[] encodings = { Encoding.ASCII, Encoding.NONE, Encoding.UTF8 };
        boolean[] signeds = { true, false }; // not a real word
        String[] clocks = { "something", "" };
        int[] bases = { 2, 4, 6, 8, 10, 12, 16 };
        for (int len = 2; len < 65; len++) {
            for (ByteOrder bo : bos) {
                for (boolean signed : signeds) {
                    for (int base : bases) {
                        for (Encoding enc : encodings) {
                            for (String clock : clocks) {
                                assertNotNull(enc);
                                assertNotNull(clock);
                                IntegerDeclaration intDec = IntegerDeclaration.createDeclaration(len, signed, base, bo, enc, clock, 8, null);
                                String title = Integer.toString(len) + " " + bo + " " + signed + " " + base + " " + enc;
                                assertEquals(title, signed, intDec.isSigned());
                                assertEquals(title, base, intDec.getBase());
                                // at len 8 le and be are the same
                                if (len != 8) {
                                    assertEquals(title, bo, intDec.getByteOrder());
                                }
                                assertEquals(title, len, intDec.getLength());
                                assertEquals(title, len, intDec.getMaximumSize());
                                assertEquals(title, clock, intDec.getClock());
                                assertEquals(title, !signed && len == 8, intDec.isUnsignedByte());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Run the int getBase() method test.
     */
    @Test
    public void testGetBase() {
        int result = fixture.getBase();
        assertEquals(1, result);
    }

    /**
     * Run the ByteOrder getByteOrder() method test.
     */
    @Test
    public void testGetByteOrder() {
        ByteOrder result = fixture.getByteOrder();
        assertNotNull(result);
        assertEquals("BIG_ENDIAN", result.toString());
    }

    /**
     * Run the Encoding getEncoding() method test.
     */
    @Test
    public void testGetEncoding() {
        Encoding result = fixture.getEncoding();
        assertNotNull(result);
        assertEquals("ASCII", result.name());
        assertEquals("ASCII", result.toString());
        assertEquals(1, result.ordinal());
    }

    /**
     * Run the int getLength() method test.
     */
    @Test
    public void testGetLength() {
        int result = fixture.getLength();
        assertEquals(1, result);
    }

    /**
     * Run the boolean isCharacter() method test.
     */
    @Test
    public void testIsCharacter() {
        boolean result = fixture.isCharacter();
        assertEquals(false, result);
    }

    /**
     * Run the boolean isVarint() method test.
     */
    @Test
    public void testIsVarint() {
        boolean result = fixture.isVarint();
        assertEquals(false, result);
    }

    /**
     * Run the boolean isCharacter() method test.
     */
    @Test
    public void testIsCharacter_8bytes() {
        IntegerDeclaration fixture8 = IntegerDeclaration.createDeclaration(8, true, 1,
                ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 8, null);

        boolean result = fixture8.isCharacter();
        assertEquals(true, result);
    }

    /**
     * Run the boolean isSigned() method test.
     */
    @Test
    public void testIsSigned_signed() {
        IntegerDeclaration fixtureSigned = IntegerDeclaration.createDeclaration(2, true,
                1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 8, null);
        boolean result = fixtureSigned.isSigned();
        assertEquals(true, result);
    }

    /**
     * Run the boolean isSigned() method test.
     */
    @Test
    public void testIsSigned_unsigned() {
        boolean result = fixture.isSigned();
        assertEquals(false, result);
    }

    /**
     * Run the String toString() method test.
     */
    @Test
    public void testToString() {
        String result = fixture.toString();
        String trunc = result.substring(0, 22);
        assertEquals("[declaration] integer[", trunc);
    }

    /**
     * Run the long getMaxValue() method test.
     */
    @Test
    public void testMaxValue() {
        assertEquals(BigInteger.ONE, fixture.getMaxValue());

        IntegerDeclaration signed8bit = IntegerDeclaration.createDeclaration(8, true, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.valueOf(127), signed8bit.getMaxValue());

        IntegerDeclaration unsigned8bit = IntegerDeclaration.createDeclaration(8, false, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.valueOf(255), unsigned8bit.getMaxValue());

        IntegerDeclaration signed32bit = IntegerDeclaration.createDeclaration(32, true, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.valueOf(2147483647), signed32bit.getMaxValue());

        IntegerDeclaration unsigned32bit = IntegerDeclaration.createDeclaration(32, false, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.valueOf(4294967295l), unsigned32bit.getMaxValue());

        IntegerDeclaration signed64bit = IntegerDeclaration.createDeclaration(64, true, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.valueOf(9223372036854775807L), signed64bit.getMaxValue());

        IntegerDeclaration unsigned64bit = IntegerDeclaration.createDeclaration(64, false, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.valueOf(2).pow(64).subtract(BigInteger.ONE), unsigned64bit.getMaxValue());
    }

    /**
     * Run the long getMinValue() method test.
     */
    @Test
    public void testMinValue() {
        assertEquals(BigInteger.ZERO, fixture.getMinValue());

        IntegerDeclaration signed8bit = IntegerDeclaration.createDeclaration(8, true, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.valueOf(-128), signed8bit.getMinValue());

        IntegerDeclaration unsigned8bit = IntegerDeclaration.createDeclaration(8, false, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.ZERO, unsigned8bit.getMinValue());

        IntegerDeclaration signed32bit = IntegerDeclaration.createDeclaration(32, true, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.valueOf(-2147483648), signed32bit.getMinValue());

        IntegerDeclaration unsigned32bit = IntegerDeclaration.createDeclaration(32, false, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.ZERO, unsigned32bit.getMinValue());

        IntegerDeclaration signed64bit = IntegerDeclaration.createDeclaration(64, true, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.valueOf(-9223372036854775808L), signed64bit.getMinValue());

        IntegerDeclaration unsigned64bit = IntegerDeclaration.createDeclaration(64, false, 1, ByteOrder.BIG_ENDIAN, Encoding.ASCII, "", 32, null);
        assertEquals(BigInteger.ZERO, unsigned64bit.getMinValue());
    }

    /**
     * Test the hashcode
     */
    @Test
    public void hashcodeTest() {
        IntegerDeclaration a = IntegerDeclaration.createDeclaration(32, false, 10, ByteOrder.BIG_ENDIAN, Encoding.NONE, "", 32, null);
        IntegerDeclaration i = IntegerDeclaration.createDeclaration(32, false, 10, ByteOrder.BIG_ENDIAN, Encoding.NONE, "", 32, null);
        assertEquals(a.hashCode(), i.hashCode());
        assertEquals(a.hashCode(), a.hashCode());
    }

    /**
     * Test the equals
     */
    @Test
    public void equalsTest() {
        IntegerDeclaration a = IntegerDeclaration.createDeclaration(32, false, 10, ByteOrder.BIG_ENDIAN, Encoding.NONE, "", 32, null);
        IntegerDeclaration b = IntegerDeclaration.createDeclaration(8, false, 10, ByteOrder.BIG_ENDIAN, Encoding.NONE, "", 32, null);
        IntegerDeclaration c = IntegerDeclaration.createDeclaration(32, true, 10, ByteOrder.BIG_ENDIAN, Encoding.NONE, "", 32, null);
        IntegerDeclaration d = IntegerDeclaration.createDeclaration(32, false, 16, ByteOrder.BIG_ENDIAN, Encoding.NONE, "", 32, null);
        IntegerDeclaration e = IntegerDeclaration.createDeclaration(32, false, 10, ByteOrder.LITTLE_ENDIAN, Encoding.NONE, "", 32, null);
        IntegerDeclaration f = IntegerDeclaration.createDeclaration(32, false, 10, ByteOrder.BIG_ENDIAN, Encoding.UTF8, "", 32, null);
        IntegerDeclaration g = IntegerDeclaration.createDeclaration(32, false, 10, ByteOrder.BIG_ENDIAN, Encoding.NONE, "hi", 32, null);
        IntegerDeclaration h = IntegerDeclaration.createDeclaration(32, false, 10, ByteOrder.BIG_ENDIAN, Encoding.NONE, "", 16, null);
        IntegerDeclaration i = IntegerDeclaration.createDeclaration(32, false, 10, ByteOrder.BIG_ENDIAN, Encoding.NONE, "", 32, null);
        assertNotEquals(a, null);
        assertNotEquals(a, new Object());
        assertNotEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(a, e);
        assertNotEquals(a, f);
        assertNotEquals(a, g);
        assertNotEquals(a, h);
        assertEquals(a, i);
        assertNotEquals(b, a);
        assertNotEquals(c, a);
        assertNotEquals(d, a);
        assertNotEquals(e, a);
        assertNotEquals(f, a);
        assertNotEquals(g, a);
        assertNotEquals(h, a);
        assertEquals(i, a);
        assertEquals(a, a);
    }

}