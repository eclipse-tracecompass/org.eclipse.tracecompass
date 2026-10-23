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
import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.BitArrayDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.CTFJsonMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.bitarray.BitArrayDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * Test class for the fixed-length bit array declaration parser in CTF2,
 * following the specification: https://diamon.org/ctf/#fl-ba-fc
 *
 * @author Arnaud Fiorini
 */
public class BitArrayDeclarationParserTest {

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static BitArrayDeclaration parse(JsonObject fieldClass) throws ParseException {
        return BitArrayDeclarationParser.INSTANCE.parse(createNode(fieldClass), new BitArrayDeclarationParser.Param(new CTFTrace()));
    }

    private static JsonStructureFieldMemberMetadataNode createNode(JsonObject fieldClass) {
        return new JsonStructureFieldMemberMetadataNode(null, JsonMetadataStrings.FIXED_LENGTH_BIT_ARRAY, "test", "bits", fieldClass);
    }

    /**
     * Create a fixed-length bit array field class, the null properties are
     * omitted
     */
    private static JsonObject bitArray(Integer length, String byteOrder) {
        JsonObject bitArray = new JsonObject();
        bitArray.addProperty("type", "fixed-length-bit-array");
        if (length != null) {
            bitArray.addProperty("length", length);
        }
        if (byteOrder != null) {
            bitArray.addProperty("byte-order", byteOrder);
        }
        return bitArray;
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------

    /**
     * Test parsing a fixed-length little-endian bit array field class with
     * only the required properties
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testLittleEndian() throws ParseException {
        BitArrayDeclaration result = parse(bitArray(16, "little-endian"));

        assertNotNull(result);
        assertEquals(16, result.getLength());
        assertEquals(ByteOrder.LITTLE_ENDIAN, result.getByteOrder());
        /* The default alignment is 1 */
        assertEquals(1, result.getAlignment());
    }

    /**
     * Test parsing a fixed-length big-endian bit array field class
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testBigEndian() throws ParseException {
        BitArrayDeclaration result = parse(bitArray(5, "big-endian"));

        assertNotNull(result);
        assertEquals(5, result.getLength());
        assertEquals(ByteOrder.BIG_ENDIAN, result.getByteOrder());
        assertEquals(1, result.getAlignment());
    }

    /**
     * Test parsing a fixed-length bit array field class with an alignment
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testAlignment() throws ParseException {
        JsonObject fieldClass = bitArray(32, "little-endian");
        fieldClass.addProperty("alignment", 32);
        BitArrayDeclaration result = parse(fieldClass);

        assertEquals(32, result.getAlignment());
    }

    /**
     * Test parsing a fixed-length bit array field class of one bit, the
     * smallest valid length
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testOneBit() throws ParseException {
        assertEquals(1, parse(bitArray(1, "little-endian")).getLength());
    }

    /**
     * Test that the optional bit-order property is accepted with both of its
     * valid values
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testBitOrder() throws ParseException {
        JsonObject fieldClass = bitArray(8, "little-endian");
        fieldClass.addProperty("bit-order", "first-to-last");
        assertEquals(8, parse(fieldClass).getLength());

        fieldClass = bitArray(8, "big-endian");
        fieldClass.addProperty("bit-order", "last-to-first");
        assertEquals(8, parse(fieldClass).getLength());
    }

    /**
     * Parse a 4-bit field class with the given byte order and bit order, and
     * decode one byte with it
     */
    private static byte[] decodeNibble(String byteOrder, String bitOrder, byte data) throws ParseException, CTFException {
        JsonObject fieldClass = bitArray(4, byteOrder);
        if (bitOrder != null) {
            fieldClass.addProperty("bit-order", bitOrder);
        }
        BitArrayDeclaration declaration = parse(fieldClass);
        return declaration.createDefinition(null, "bits", new BitBuffer(ByteBuffer.wrap(new byte[] { data }))).getValue();
    }

    /**
     * Test the bit order with a little-endian byte order. The bits are decoded
     * from the least significant bit of the byte, so the first decoded bit of
     * 0x01 is 1 followed by three 0.
     *
     * @throws ParseException
     *             if parsing fails
     * @throws CTFException
     *             if decoding fails
     */
    @Test
    public void testBitOrderLittleEndian() throws ParseException, CTFException {
        /* Default is first-to-last: the first decoded bit is element 0 */
        assertArrayEquals(new byte[] { 0x01 }, decodeNibble("little-endian", null, (byte) 0x01));
        assertArrayEquals(new byte[] { 0x01 }, decodeNibble("little-endian", "first-to-last", (byte) 0x01));
        /* The first decoded bit is the last element */
        assertArrayEquals(new byte[] { 0x08 }, decodeNibble("little-endian", "last-to-first", (byte) 0x01));
    }

    /**
     * Test the bit order with a big-endian byte order. The bits are decoded
     * from the most significant bit of the byte, so the first decoded bit of
     * 0x80 is 1 followed by three 0.
     *
     * @throws ParseException
     *             if parsing fails
     * @throws CTFException
     *             if decoding fails
     */
    @Test
    public void testBitOrderBigEndian() throws ParseException, CTFException {
        /* Default is last-to-first: the first decoded bit is the last element */
        assertArrayEquals(new byte[] { 0x08 }, decodeNibble("big-endian", null, (byte) 0x80));
        assertArrayEquals(new byte[] { 0x08 }, decodeNibble("big-endian", "last-to-first", (byte) 0x80));
        /* The first decoded bit is element 0 */
        assertArrayEquals(new byte[] { 0x01 }, decodeNibble("big-endian", "first-to-last", (byte) 0x80));
    }

    /**
     * Test that the optional attributes and extensions properties are accepted
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testAttributesAndExtensions() throws ParseException {
        JsonObject attributes = new JsonObject();
        JsonObject namespace = new JsonObject();
        namespace.addProperty("info", "bits");
        attributes.add("my.tracer", namespace);

        JsonObject fieldClass = bitArray(8, "little-endian");
        fieldClass.add("attributes", attributes);
        fieldClass.add("extensions", new JsonObject());

        BitArrayDeclaration result = parse(fieldClass);
        assertEquals(8, result.getLength());
        assertEquals(ByteOrder.LITTLE_ENDIAN, result.getByteOrder());
    }

    /**
     * Test that the length property is required
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testMissingLength() throws ParseException {
        parse(bitArray(null, "little-endian"));
    }

    /**
     * Test that the length must be greater than zero
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testZeroLength() throws ParseException {
        parse(bitArray(0, "little-endian"));
    }

    /**
     * Test that a negative length is rejected
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testNegativeLength() throws ParseException {
        parse(bitArray(-1, "little-endian"));
    }

    /**
     * Test that the byte-order property is required
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testMissingByteOrder() throws ParseException {
        parse(bitArray(8, null));
    }

    /**
     * Test that an invalid byte order is rejected
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testInvalidByteOrder() throws ParseException {
        parse(bitArray(8, "middle-endian"));
    }

    /**
     * Test that the alignment must be a power of two
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testNonPowerOfTwoAlignment() throws ParseException {
        JsonObject fieldClass = bitArray(8, "little-endian");
        fieldClass.addProperty("alignment", 3);
        parse(fieldClass);
    }

    /**
     * Test that the alignment must be positive
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testZeroAlignment() throws ParseException {
        JsonObject fieldClass = bitArray(8, "little-endian");
        fieldClass.addProperty("alignment", 0);
        parse(fieldClass);
    }

    /**
     * Test that the bit-order property must be "first-to-last" or
     * "last-to-first"
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testInvalidBitOrder() throws ParseException {
        JsonObject fieldClass = bitArray(8, "little-endian");
        fieldClass.addProperty("bit-order", "middle-out");
        parse(fieldClass);
    }

    /**
     * Test that the fixed-length bit array field class is not supported in
     * CTF 1.8
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testCTF1NotSupported() throws ParseException {
        BitArrayDeclarationParser.INSTANCE.parse(new CTFJsonMetadataNode(null, "bit-array", ""),
                new BitArrayDeclarationParser.Param(new CTFTrace()));
    }

    /**
     * Test that the parser requires its own parameter type
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidParam() throws ParseException {
        BitArrayDeclarationParser.INSTANCE.parse(createNode(bitArray(8, "little-endian")), null);
    }
}
