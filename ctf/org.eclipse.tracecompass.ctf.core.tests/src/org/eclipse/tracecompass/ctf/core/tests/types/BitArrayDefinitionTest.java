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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.BitArrayDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.BitArrayDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.BitOrder;
import org.junit.Test;

/**
 * Tests for the fixed-length bit array definition class in CTF2, following the
 * specification: https://diamon.org/ctf/#fl-ba-fc
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("javadoc")
public class BitArrayDefinitionTest {

    private static final @NonNull String FIELD_NAME = "bits";

    private static BitBuffer createBuffer(byte... data) {
        ByteBuffer byb = ByteBuffer.wrap(data);
        return new BitBuffer(byb);
    }

    private static BitArrayDefinition read(BitArrayDeclaration declaration, BitBuffer input) throws CTFException {
        return declaration.createDefinition(null, FIELD_NAME, Objects.requireNonNull(input));
    }

    private static BitArrayDefinition read(BitArrayDeclaration declaration, byte... data) throws CTFException {
        return read(declaration, createBuffer(data));
    }

    /**
     * Within a little-endian byte, the bits are decoded from the least
     * significant to the most significant bits
     */
    @Test
    public void testLittleEndianSubByte() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(4, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1);
        BitBuffer input = createBuffer((byte) 0xa5);
        assertArrayEquals(new byte[] { 0x05 }, read(declaration, input).getValue());
        assertArrayEquals(new byte[] { 0x0a }, read(declaration, input).getValue());
    }

    /**
     * Within a big-endian byte, the bits are decoded from the most significant
     * to the least significant bits
     */
    @Test
    public void testBigEndianSubByte() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(4, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 1);
        BitBuffer input = createBuffer((byte) 0xa5);
        assertArrayEquals(new byte[] { 0x0a }, read(declaration, input).getValue());
        assertArrayEquals(new byte[] { 0x05 }, read(declaration, input).getValue());
    }

    @Test
    public void testLittleEndianMultiByte() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(12, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1);
        assertArrayEquals(new byte[] { 0x34, 0x02 }, read(declaration, (byte) 0x34, (byte) 0x12).getValue());
    }

    @Test
    public void testBigEndianMultiByte() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(12, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 1);
        assertArrayEquals(new byte[] { 0x23, 0x01 }, read(declaration, (byte) 0x12, (byte) 0x34).getValue());
    }

    @Test
    public void testLittleEndian64Bits() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(64, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        BitArrayDefinition definition = read(declaration, (byte) 0x08, (byte) 0x07, (byte) 0x06, (byte) 0x05,
                (byte) 0x04, (byte) 0x03, (byte) 0x02, (byte) 0x81);
        assertArrayEquals(new byte[] { 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, (byte) 0x81 }, definition.getValue());
        assertTrue(definition.isBitSet(63));
        assertTrue(definition.isBitSet(3));
        assertFalse(definition.isBitSet(0));
    }

    @Test
    public void testBigEndian64Bits() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(64, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 8);
        BitArrayDefinition definition = read(declaration, (byte) 0x81, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08);
        assertArrayEquals(new byte[] { 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, (byte) 0x81 }, definition.getValue());
    }

    /**
     * The length of a bit array is only required to be greater than zero, so
     * a field longer than 64 bits must be decoded
     */
    @Test
    public void testLittleEndianLongerThan64Bits() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(72, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        BitBuffer input = createBuffer((byte) 0x01, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0x80);
        BitArrayDefinition definition = read(declaration, input);
        assertTrue(definition.isBitSet(0));
        assertFalse(definition.isBitSet(1));
        assertFalse(definition.isBitSet(64));
        assertTrue(definition.isBitSet(71));
        assertEquals(72, input.position());
        assertEquals("0b1" + "0".repeat(70) + "1", definition.toString());
    }

    /**
     * The length of a bit array is only required to be greater than zero, so
     * a field longer than 64 bits must be decoded
     */
    @Test
    public void testBigEndianLongerThan64Bits() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(72, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 8);
        BitBuffer input = createBuffer((byte) 0x80, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0x01);
        BitArrayDefinition definition = read(declaration, input);
        assertTrue(definition.isBitSet(0));
        assertFalse(definition.isBitSet(1));
        assertTrue(definition.isBitSet(71));
        assertEquals(72, input.position());
    }

    /**
     * With the last-to-first bit order, the first decoded bit of a
     * little-endian field is the last element of the bit array
     */
    @Test
    public void testLittleEndianLastToFirst() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(12, ByteOrder.LITTLE_ENDIAN, BitOrder.LAST_TO_FIRST, 1);
        /* Decoded bits, in order: 1100 0000 0010 */
        BitArrayDefinition definition = read(declaration, (byte) 0x03, (byte) 0x04);
        assertArrayEquals(new byte[] { 0x02, 0x0c }, definition.getValue());
    }

    /**
     * With the first-to-last bit order, the first decoded bit of a big-endian
     * field is the first element of the bit array
     */
    @Test
    public void testBigEndianFirstToLast() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(12, ByteOrder.BIG_ENDIAN, BitOrder.FIRST_TO_LAST, 1);
        /* Decoded bits, in order: 1100 0000 0010 */
        BitArrayDefinition definition = read(declaration, (byte) 0xc0, (byte) 0x20);
        assertArrayEquals(new byte[] { 0x03, 0x04 }, definition.getValue());
    }

    /**
     * The bit order applies to the whole field, not to each 64-bit chunk read
     * from the buffer
     */
    @Test
    public void testLittleEndianLastToFirstLongerThan64Bits() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(72, ByteOrder.LITTLE_ENDIAN, BitOrder.LAST_TO_FIRST, 8);
        /* Decoded bits 0, 1 and 64 are set */
        BitArrayDefinition definition = read(declaration, (byte) 0x03, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0x01);
        assertTrue(definition.isBitSet(71));
        assertTrue(definition.isBitSet(70));
        assertTrue(definition.isBitSet(7));
        assertFalse(definition.isBitSet(0));
        assertFalse(definition.isBitSet(64));
        assertEquals("0b11" + "0".repeat(62) + "10000000", definition.toString());
    }

    /**
     * The bit order applies to the whole field, not to each 64-bit chunk read
     * from the buffer
     */
    @Test
    public void testBigEndianFirstToLastLongerThan64Bits() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(72, ByteOrder.BIG_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        /* Decoded bits 0, 1 and 64 are set */
        BitArrayDefinition definition = read(declaration, (byte) 0xc0, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0x80);
        assertTrue(definition.isBitSet(0));
        assertTrue(definition.isBitSet(1));
        assertTrue(definition.isBitSet(64));
        assertFalse(definition.isBitSet(71));
        assertFalse(definition.isBitSet(7));
    }

    /**
     * A bit array has no integer type semantics, the most significant bit is
     * never a sign bit
     */
    @Test
    public void testNoSignExtension() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        BitArrayDefinition definition = read(declaration, (byte) 0xff);
        assertArrayEquals(new byte[] { (byte) 0xff }, definition.getValue());
    }

    /**
     * The first bit of an instance is aligned relative to the beginning of the
     * packet
     */
    @Test
    public void testAlignment() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 16);
        BitBuffer input = createBuffer((byte) 0xff, (byte) 0xff, (byte) 0x42);
        input.position(3);
        assertArrayEquals(new byte[] { 0x42 }, read(declaration, input).getValue());
        assertEquals(24, input.position());
    }

    /**
     * The default alignment of 1 does not skip any bit
     */
    @Test
    public void testDefaultAlignment() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(3, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1);
        BitBuffer input = createBuffer((byte) 0b0101_1010);
        input.position(1);
        assertArrayEquals(new byte[] { 0x05 }, read(declaration, input).getValue());
        assertEquals(4, input.position());
    }

    /**
     * The byte order of the input buffer is restored after reading
     */
    @Test
    public void testByteOrderRestored() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(8, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 8);
        BitBuffer input = new BitBuffer(ByteBuffer.wrap(new byte[] { 1 }), ByteOrder.LITTLE_ENDIAN);
        read(declaration, input);
        assertEquals(ByteOrder.LITTLE_ENDIAN, input.getByteOrder());
    }

    @Test
    public void testIsBitSet() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        BitArrayDefinition definition = read(declaration, (byte) 0b1000_0101);
        assertTrue(definition.isBitSet(0));
        assertFalse(definition.isBitSet(1));
        assertTrue(definition.isBitSet(2));
        assertFalse(definition.isBitSet(6));
        assertTrue(definition.isBitSet(7));
        /* Out of range */
        assertFalse(definition.isBitSet(-1));
        assertFalse(definition.isBitSet(8));
        assertFalse(definition.isBitSet(64));
    }

    /**
     * The string contains all the bits of the field, including the leading
     * zeros
     */
    @Test
    public void testToString() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        BitArrayDefinition definition = read(declaration, (byte) 0b0000_0101);
        assertEquals("0b00000101", definition.toString());

        declaration = new BitArrayDeclaration(3, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1);
        assertEquals("0b000", read(declaration, (byte) 0).toString());
    }

    @Test
    public void testGetDeclaration() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        BitArrayDefinition definition = read(declaration, (byte) 1);
        assertSame(declaration, definition.getDeclaration());
        assertEquals(8, definition.size());
    }

    /**
     * Reading past the end of the buffer fails
     */
    @Test(expected = CTFException.class)
    public void testBufferTooSmall() throws CTFException {
        BitArrayDeclaration declaration = new BitArrayDeclaration(16, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);
        read(declaration, (byte) 1);
    }
}
