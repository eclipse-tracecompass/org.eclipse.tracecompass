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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.BitMapDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.BitMapDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.BitOrder;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerRange;
import org.junit.Test;

/**
 * Tests for the fixed-length bit map definition class in CTF2, following the
 * specification: https://diamon.org/ctf/#fl-bm-fc
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("javadoc")
public class BitMapDefinitionTest {

    private static final @NonNull String FIELD_NAME = "flags";

    private static BitBuffer createBuffer(byte... data) {
        ByteBuffer byb = ByteBuffer.wrap(data);
        return new BitBuffer(byb);
    }

    private static BitMapDefinition read(BitMapDeclaration declaration, BitBuffer input) throws CTFException {
        return (BitMapDefinition) declaration.createDefinition(null, FIELD_NAME, input);
    }

    private static BitMapDefinition read(BitMapDeclaration declaration, byte... data) throws CTFException {
        return read(declaration, createBuffer(data));
    }

    /**
     * Flags of an 8-bit map: "active" on bit 0, "mode" on bits 1 to 3 and 6,
     * "error" on bit 7
     */
    private static Map<String, List<IntegerRange>> flags() {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("active", List.of(new IntegerRange(0, 0)));
        flags.put("mode", List.of(new IntegerRange(1, 3), new IntegerRange(6, 6)));
        flags.put("error", List.of(new IntegerRange(7, 7)));
        return flags;
    }

    private static BitMapDeclaration declaration(ByteOrder byteOrder) {
        return new BitMapDeclaration(8, byteOrder, BitOrder.fromByteOrder(byteOrder), 8, flags());
    }

    /**
     * A flag is active when its single bit is set
     */
    @Test
    public void testSingleBitFlag() throws CTFException {
        BitMapDefinition definition = read(declaration(ByteOrder.LITTLE_ENDIAN), (byte) 0b0000_0001);
        assertEquals(List.of("active"), definition.getActiveFlags());
        assertTrue(definition.isFlagActive("active"));
        assertFalse(definition.isFlagActive("mode"));
        assertFalse(definition.isFlagActive("error"));
    }

    /**
     * A flag with a range of bits is active when any of its bits is set
     */
    @Test
    public void testRangeFlag() throws CTFException {
        BitMapDeclaration declaration = declaration(ByteOrder.LITTLE_ENDIAN);
        assertEquals(List.of("mode"), read(declaration, (byte) 0b0000_0010).getActiveFlags());
        assertEquals(List.of("mode"), read(declaration, (byte) 0b0000_0100).getActiveFlags());
        assertEquals(List.of("mode"), read(declaration, (byte) 0b0000_1000).getActiveFlags());
        assertEquals(List.of("mode"), read(declaration, (byte) 0b0000_1110).getActiveFlags());
    }

    /**
     * A flag with several ranges is active when a bit of any of its ranges is
     * set
     */
    @Test
    public void testMultiRangeFlag() throws CTFException {
        BitMapDefinition definition = read(declaration(ByteOrder.LITTLE_ENDIAN), (byte) 0b0100_0000);
        assertEquals(List.of("mode"), definition.getActiveFlags());
    }

    /**
     * A bit which is not named by any flag does not activate any flag
     */
    @Test
    public void testUnnamedBit() throws CTFException {
        BitMapDefinition definition = read(declaration(ByteOrder.LITTLE_ENDIAN), (byte) 0b0011_0000);
        assertEquals(List.of(), definition.getActiveFlags());
        assertTrue(definition.isBitSet(4));
        assertTrue(definition.isBitSet(5));
    }

    @Test
    public void testNoActiveFlag() throws CTFException {
        BitMapDefinition definition = read(declaration(ByteOrder.LITTLE_ENDIAN), (byte) 0);
        assertEquals(List.of(), definition.getActiveFlags());
        assertFalse(definition.isFlagActive("active"));
        assertFalse(definition.isFlagActive("mode"));
        assertFalse(definition.isFlagActive("error"));
    }

    /**
     * The active flags are in declaration order
     */
    @Test
    public void testAllActiveFlags() throws CTFException {
        BitMapDefinition definition = read(declaration(ByteOrder.LITTLE_ENDIAN), (byte) 0xff);
        assertEquals(List.of("active", "mode", "error"), definition.getActiveFlags());
    }

    /**
     * The active flags are in declaration order, not in bit order
     */
    @Test
    public void testDeclarationOrder() throws CTFException {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("high", List.of(new IntegerRange(7, 7)));
        flags.put("low", List.of(new IntegerRange(0, 0)));
        BitMapDeclaration declaration = new BitMapDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8, flags);
        assertEquals(List.of("high", "low"), read(declaration, (byte) 0b1000_0001).getActiveFlags());
    }

    /**
     * A bit may have more than one associated name, all of them are active
     * when the bit is set
     */
    @Test
    public void testOverlappingFlags() throws CTFException {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("low", List.of(new IntegerRange(0, 3)));
        flags.put("first", List.of(new IntegerRange(0, 0)));
        flags.put("second", List.of(new IntegerRange(1, 1)));
        BitMapDeclaration declaration = new BitMapDeclaration(4, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags);
        assertEquals(List.of("low", "first"), read(declaration, (byte) 0b0001).getActiveFlags());
        assertEquals(List.of("low", "second"), read(declaration, (byte) 0b0010).getActiveFlags());
        assertEquals(List.of("low"), read(declaration, (byte) 0b1000).getActiveFlags());
    }

    /**
     * An unknown flag name is never active
     */
    @Test
    public void testUnknownFlag() throws CTFException {
        BitMapDefinition definition = read(declaration(ByteOrder.LITTLE_ENDIAN), (byte) 0xff);
        assertFalse(definition.isFlagActive("unknown"));
    }

    /**
     * Within a little-endian byte, the bits are decoded from the least
     * significant to the most significant bits
     */
    @Test
    public void testLittleEndianSubByte() throws CTFException {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("first", List.of(new IntegerRange(0, 0)));
        flags.put("last", List.of(new IntegerRange(3, 3)));
        BitMapDeclaration declaration = new BitMapDeclaration(4, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags);
        BitBuffer input = createBuffer((byte) 0b1000_0001);
        assertEquals(List.of("first"), read(declaration, input).getActiveFlags());
        assertEquals(List.of("last"), read(declaration, input).getActiveFlags());
    }

    /**
     * Within a big-endian byte, the bits are decoded from the most significant
     * to the least significant bits
     */
    @Test
    public void testBigEndianSubByte() throws CTFException {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("first", List.of(new IntegerRange(0, 0)));
        flags.put("last", List.of(new IntegerRange(3, 3)));
        BitMapDeclaration declaration = new BitMapDeclaration(4, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 1, flags);
        BitBuffer input = createBuffer((byte) 0b1000_0001);
        assertEquals(List.of("last"), read(declaration, input).getActiveFlags());
        assertEquals(List.of("first"), read(declaration, input).getActiveFlags());
    }

    @Test
    public void testLittleEndianMultiByte() throws CTFException {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("low", List.of(new IntegerRange(0, 7)));
        flags.put("high", List.of(new IntegerRange(8, 15)));
        BitMapDeclaration declaration = new BitMapDeclaration(16, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8, flags);
        BitMapDefinition definition = read(declaration, (byte) 0, (byte) 0x01);
        assertArrayEquals(new byte[] { 0x00, 0x01 }, definition.getValue());
        assertEquals(List.of("high"), definition.getActiveFlags());
    }

    @Test
    public void testBigEndianMultiByte() throws CTFException {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("low", List.of(new IntegerRange(0, 7)));
        flags.put("high", List.of(new IntegerRange(8, 15)));
        BitMapDeclaration declaration = new BitMapDeclaration(16, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 8, flags);
        BitMapDefinition definition = read(declaration, (byte) 0, (byte) 0x01);
        assertArrayEquals(new byte[] { 0x01, 0x00 }, definition.getValue());
        assertEquals(List.of("low"), definition.getActiveFlags());
    }

    /**
     * The most significant bit of a 64-bit map is a regular bit
     */
    @Test
    public void test64Bits() throws CTFException {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("first", List.of(new IntegerRange(0, 0)));
        flags.put("last", List.of(new IntegerRange(63, 63)));
        BitMapDeclaration declaration = new BitMapDeclaration(64, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8, flags);
        BitMapDefinition definition = read(declaration, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0x80);
        assertEquals(List.of("last"), definition.getActiveFlags());
    }

    /**
     * The length of a bit map is only required to be greater than zero, so
     * flags beyond the 64th bit must be decoded
     */
    @Test
    public void testLittleEndianLongerThan64Bits() throws CTFException {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("first", List.of(new IntegerRange(0, 0)));
        flags.put("middle", List.of(new IntegerRange(64, 64)));
        flags.put("last", List.of(new IntegerRange(71, 71)));
        BitMapDeclaration declaration = new BitMapDeclaration(72, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8, flags);
        BitBuffer input = createBuffer((byte) 0x01, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0x80);
        BitMapDefinition definition = read(declaration, input);
        assertEquals(List.of("first", "last"), definition.getActiveFlags());
        assertEquals(72, input.position());
    }

    /**
     * The length of a bit map is only required to be greater than zero, so
     * flags beyond the 64th bit must be decoded
     */
    @Test
    public void testBigEndianLongerThan64Bits() throws CTFException {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("first", List.of(new IntegerRange(0, 0)));
        flags.put("middle", List.of(new IntegerRange(64, 64)));
        flags.put("last", List.of(new IntegerRange(71, 71)));
        BitMapDeclaration declaration = new BitMapDeclaration(72, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 8, flags);
        BitBuffer input = createBuffer((byte) 0x80, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0x01);
        BitMapDefinition definition = read(declaration, input);
        assertEquals(List.of("first", "last"), definition.getActiveFlags());
        assertEquals(72, input.position());
    }

    /**
     * The first bit of an instance is aligned relative to the beginning of the
     * packet
     */
    @Test
    public void testAlignment() throws CTFException {
        BitMapDeclaration declaration = new BitMapDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 16, flags());
        BitBuffer input = createBuffer((byte) 0xff, (byte) 0xff, (byte) 0x80);
        input.position(3);
        assertEquals(List.of("error"), read(declaration, input).getActiveFlags());
        assertEquals(24, input.position());
    }

    /**
     * The default alignment of 1 does not skip any bit
     */
    @Test
    public void testDefaultAlignment() throws CTFException {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("first", List.of(new IntegerRange(0, 0)));
        flags.put("second", List.of(new IntegerRange(1, 1)));
        BitMapDeclaration declaration = new BitMapDeclaration(2, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags);
        BitBuffer input = createBuffer((byte) 0b0000_0100);
        input.position(1);
        assertEquals(List.of("second"), read(declaration, input).getActiveFlags());
        assertEquals(3, input.position());
    }

    /**
     * The byte order of the input buffer is restored after reading
     */
    @Test
    public void testByteOrderRestored() throws CTFException {
        BitBuffer input = new BitBuffer(ByteBuffer.wrap(new byte[] { 1 }), ByteOrder.LITTLE_ENDIAN);
        read(declaration(ByteOrder.BIG_ENDIAN), input);
        assertEquals(ByteOrder.LITTLE_ENDIAN, input.getByteOrder());
    }

    /**
     * A bit map field is a bit array field, its bits remain accessible
     */
    @Test
    public void testBitArrayValue() throws CTFException {
        BitMapDefinition definition = read(declaration(ByteOrder.LITTLE_ENDIAN), (byte) 0b1000_0101);
        assertArrayEquals(new byte[] { (byte) 0x85 }, definition.getValue());
        assertTrue(definition.isBitSet(0));
        assertFalse(definition.isBitSet(1));
        assertTrue(definition.isBitSet(2));
        assertTrue(definition.isBitSet(7));
        assertFalse(definition.isBitSet(8));
    }

    /**
     * The string contains the bits and the names of the active flags
     */
    @Test
    public void testToString() throws CTFException {
        BitMapDefinition definition = read(declaration(ByteOrder.LITTLE_ENDIAN), (byte) 0b1000_0001);
        assertEquals("0b10000001 (active | error)", definition.toString());
    }

    @Test
    public void testGetDeclaration() throws CTFException {
        BitMapDeclaration declaration = declaration(ByteOrder.LITTLE_ENDIAN);
        BitMapDefinition definition = read(declaration, (byte) 1);
        assertSame(declaration, definition.getDeclaration());
        assertEquals(8, definition.size());
    }

    /**
     * Reading past the end of the buffer fails
     */
    @Test(expected = CTFException.class)
    public void testBufferTooSmall() throws CTFException {
        Map<String, List<IntegerRange>> flags = Map.of("flag", List.of(new IntegerRange(0, 0)));
        read(new BitMapDeclaration(16, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8, flags), (byte) 1);
    }
}
