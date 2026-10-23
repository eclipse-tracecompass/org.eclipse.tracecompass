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
import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.BitMapDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.BitMapDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerRange;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.CTFJsonMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.bitmap.BitMapDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Test class for the fixed-length bit map declaration parser in CTF2,
 * following the specification: https://diamon.org/ctf/#fl-bm-fc
 *
 * @author Arnaud Fiorini
 */
public class BitMapDeclarationParserTest {

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static BitMapDeclaration parse(JsonObject fieldClass) throws ParseException {
        return BitMapDeclarationParser.INSTANCE.parse(createNode(fieldClass), new BitMapDeclarationParser.Param(new CTFTrace()));
    }

    private static JsonStructureFieldMemberMetadataNode createNode(JsonObject fieldClass) {
        return new JsonStructureFieldMemberMetadataNode(null, JsonMetadataStrings.FIXED_LENGTH_BIT_MAP, "test", "flags", fieldClass);
    }

    /**
     * Create an integer range set from pairs of lower and upper bit indexes
     */
    private static JsonArray ranges(long... bounds) {
        JsonArray rangeSet = new JsonArray();
        for (int i = 0; i < bounds.length; i += 2) {
            JsonArray range = new JsonArray();
            range.add(bounds[i]);
            range.add(bounds[i + 1]);
            rangeSet.add(range);
        }
        return rangeSet;
    }

    /**
     * Create bit map flags with a single flag
     */
    private static JsonObject flags(String name, JsonArray ranges) {
        JsonObject flags = new JsonObject();
        flags.add(name, ranges);
        return flags;
    }

    /**
     * Create a fixed-length bit map field class, the null properties are
     * omitted
     */
    private static JsonObject bitMap(Integer length, String byteOrder, JsonObject flags) {
        JsonObject bitMap = new JsonObject();
        bitMap.addProperty("type", "fixed-length-bit-map");
        if (length != null) {
            bitMap.addProperty("length", length);
        }
        if (byteOrder != null) {
            bitMap.addProperty("byte-order", byteOrder);
        }
        if (flags != null) {
            bitMap.add("flags", flags);
        }
        return bitMap;
    }

    /**
     * Create a valid fixed-length bit map field class with a single flag on
     * bit 0
     */
    private static JsonObject bitMap(Integer length, String byteOrder) {
        return bitMap(length, byteOrder, flags("first", ranges(0, 0)));
    }

    private static void assertRange(long start, long end, IntegerRange range) {
        assertEquals(start, range.getStart());
        assertEquals(end, range.getEnd());
    }

    // ------------------------------------------------------------------------
    // Inherited bit array properties
    // ------------------------------------------------------------------------

    /**
     * Test parsing a fixed-length little-endian bit map field class with only
     * the required properties
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testLittleEndian() throws ParseException {
        BitMapDeclaration result = parse(bitMap(16, "little-endian"));

        assertNotNull(result);
        assertEquals(16, result.getLength());
        assertEquals(ByteOrder.LITTLE_ENDIAN, result.getByteOrder());
        /* The default alignment is 1 */
        assertEquals(1, result.getAlignment());
        assertEquals(List.of("first"), List.copyOf(result.getFlags().keySet()));
    }

    /**
     * Test parsing a fixed-length big-endian bit map field class
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testBigEndian() throws ParseException {
        BitMapDeclaration result = parse(bitMap(5, "big-endian"));

        assertNotNull(result);
        assertEquals(5, result.getLength());
        assertEquals(ByteOrder.BIG_ENDIAN, result.getByteOrder());
        assertEquals(1, result.getAlignment());
    }

    /**
     * Test parsing a fixed-length bit map field class with an alignment
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testAlignment() throws ParseException {
        JsonObject fieldClass = bitMap(32, "little-endian");
        fieldClass.addProperty("alignment", 32);

        assertEquals(32, parse(fieldClass).getAlignment());
    }

    /**
     * Test parsing a fixed-length bit map field class of one bit, the smallest
     * valid length
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testOneBit() throws ParseException {
        assertEquals(1, parse(bitMap(1, "little-endian")).getLength());
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
        JsonObject fieldClass = bitMap(8, "little-endian");
        fieldClass.addProperty("bit-order", "first-to-last");
        assertEquals(8, parse(fieldClass).getLength());

        fieldClass = bitMap(8, "big-endian");
        fieldClass.addProperty("bit-order", "last-to-first");
        assertEquals(8, parse(fieldClass).getLength());
    }

    /**
     * Parse a 4-bit field class with a flag on each bit, with the given byte
     * order and bit order, and decode one byte with it
     */
    private static BitMapDefinition decodeNibble(String byteOrder, String bitOrder, byte data) throws ParseException, CTFException {
        JsonObject flags = new JsonObject();
        flags.add("bit0", ranges(0, 0));
        flags.add("bit1", ranges(1, 1));
        flags.add("bit2", ranges(2, 2));
        flags.add("bit3", ranges(3, 3));
        JsonObject fieldClass = bitMap(4, byteOrder, flags);
        if (bitOrder != null) {
            fieldClass.addProperty("bit-order", bitOrder);
        }
        BitMapDeclaration declaration = parse(fieldClass);
        return (BitMapDefinition) declaration.createDefinition(null, "flags", new BitBuffer(ByteBuffer.wrap(new byte[] { data })));
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
        assertEquals(List.of("bit0"), decodeNibble("little-endian", null, (byte) 0x01).getActiveFlags());
        assertEquals(List.of("bit0"), decodeNibble("little-endian", "first-to-last", (byte) 0x01).getActiveFlags());
        /* The first decoded bit is the last element */
        assertEquals(List.of("bit3"), decodeNibble("little-endian", "last-to-first", (byte) 0x01).getActiveFlags());
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
        assertEquals(List.of("bit3"), decodeNibble("big-endian", null, (byte) 0x80).getActiveFlags());
        assertEquals(List.of("bit3"), decodeNibble("big-endian", "last-to-first", (byte) 0x80).getActiveFlags());
        /* The first decoded bit is element 0 */
        assertEquals(List.of("bit0"), decodeNibble("big-endian", "first-to-last", (byte) 0x80).getActiveFlags());
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
        namespace.addProperty("info", "flags");
        attributes.add("my.tracer", namespace);

        JsonObject fieldClass = bitMap(8, "little-endian");
        fieldClass.add("attributes", attributes);
        fieldClass.add("extensions", new JsonObject());

        BitMapDeclaration result = parse(fieldClass);
        assertEquals(8, result.getLength());
        assertEquals(ByteOrder.LITTLE_ENDIAN, result.getByteOrder());
        assertEquals(1, result.getFlags().size());
    }

    /**
     * Test that the length property is required
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testMissingLength() throws ParseException {
        parse(bitMap(null, "little-endian"));
    }

    /**
     * Test that the length must be greater than zero
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testZeroLength() throws ParseException {
        parse(bitMap(0, "little-endian"));
    }

    /**
     * Test that a negative length is rejected
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testNegativeLength() throws ParseException {
        parse(bitMap(-1, "little-endian"));
    }

    /**
     * Test that the byte-order property is required
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testMissingByteOrder() throws ParseException {
        parse(bitMap(8, null));
    }

    /**
     * Test that an invalid byte order is rejected
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testInvalidByteOrder() throws ParseException {
        parse(bitMap(8, "middle-endian"));
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
        JsonObject fieldClass = bitMap(8, "little-endian");
        fieldClass.addProperty("bit-order", "middle-out");
        parse(fieldClass);
    }

    /**
     * Test that the alignment must be a power of two
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testNonPowerOfTwoAlignment() throws ParseException {
        JsonObject fieldClass = bitMap(8, "little-endian");
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
        JsonObject fieldClass = bitMap(8, "little-endian");
        fieldClass.addProperty("alignment", 0);
        parse(fieldClass);
    }

    // ------------------------------------------------------------------------
    // Flags
    // ------------------------------------------------------------------------

    /**
     * Test parsing several flags, each with one or more bit ranges. The flags
     * keep their declaration order.
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testFlags() throws ParseException {
        JsonObject flags = new JsonObject();
        flags.add("active", ranges(0, 0));
        flags.add("mode", ranges(1, 3, 6, 6));
        flags.add("error", ranges(7, 7));

        BitMapDeclaration result = parse(bitMap(8, "little-endian", flags));
        Map<String, List<IntegerRange>> parsedFlags = result.getFlags();

        assertEquals(List.of("active", "mode", "error"), List.copyOf(parsedFlags.keySet()));

        List<IntegerRange> active = parsedFlags.get("active");
        assertNotNull(active);
        assertEquals(1, active.size());
        assertRange(0, 0, active.get(0));

        List<IntegerRange> mode = parsedFlags.get("mode");
        assertNotNull(mode);
        assertEquals(2, mode.size());
        assertRange(1, 3, mode.get(0));
        assertRange(6, 6, mode.get(1));

        List<IntegerRange> error = parsedFlags.get("error");
        assertNotNull(error);
        assertEquals(1, error.size());
        assertRange(7, 7, error.get(0));
    }

    /**
     * Test that a bit may have more than one associated name
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testOverlappingFlags() throws ParseException {
        JsonObject flags = new JsonObject();
        flags.add("low", ranges(0, 3));
        flags.add("first", ranges(0, 0));

        BitMapDeclaration result = parse(bitMap(4, "little-endian", flags));
        assertEquals(2, result.getFlags().size());
        List<IntegerRange> low = result.getFlags().get("low");
        List<IntegerRange> first = result.getFlags().get("first");
        assertNotNull(low);
        assertRange(0, 3, low.get(0));
        assertNotNull(first);
        assertRange(0, 0, first.get(0));
    }

    /**
     * Test that a flag can cover the last bit of the bit map
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testFlagOnLastBit() throws ParseException {
        BitMapDeclaration result = parse(bitMap(8, "little-endian", flags("last", ranges(7, 7))));
        List<IntegerRange> last = result.getFlags().get("last");
        assertNotNull(last);
        assertRange(7, 7, last.get(0));
    }

    /**
     * Test that a flag can cover all the bits of the bit map
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testFlagOnAllBits() throws ParseException {
        BitMapDeclaration result = parse(bitMap(8, "little-endian", flags("any", ranges(0, 7))));
        List<IntegerRange> any = result.getFlags().get("any");
        assertNotNull(any);
        assertRange(0, 7, any.get(0));
    }

    /**
     * Test that a flag can name a bit beyond the 64th bit of a long bit map
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testFlagBeyond64Bits() throws ParseException {
        BitMapDeclaration result = parse(bitMap(72, "little-endian", flags("high", ranges(70, 71))));
        assertEquals(72, result.getLength());
        List<IntegerRange> high = result.getFlags().get("high");
        assertNotNull(high);
        assertRange(70, 71, high.get(0));
    }

    /**
     * Test that the flags property is required
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testMissingFlags() throws ParseException {
        parse(bitMap(8, "little-endian", null));
    }

    /**
     * Test that the flags property must contain one or more properties
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testEmptyFlags() throws ParseException {
        parse(bitMap(8, "little-endian", new JsonObject()));
    }

    /**
     * Test that the flags property must be a JSON object
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testFlagsNotAnObject() throws ParseException {
        JsonObject fieldClass = bitMap(8, "little-endian", null);
        fieldClass.add("flags", ranges(0, 0));
        parse(fieldClass);
    }

    /**
     * Test that the bit ranges of a flag must be an integer range set
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testFlagRangesNotAnArray() throws ParseException {
        JsonObject flags = new JsonObject();
        flags.addProperty("first", 0);
        parse(bitMap(8, "little-endian", flags));
    }

    /**
     * Test that an integer range must contain exactly a lower and an upper
     * value
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testRangeWithOneValue() throws ParseException {
        JsonArray range = new JsonArray();
        range.add(0);
        JsonArray rangeSet = new JsonArray();
        rangeSet.add(range);
        parse(bitMap(8, "little-endian", flags("first", rangeSet)));
    }

    /**
     * Test that an integer range must contain exactly a lower and an upper
     * value
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testRangeWithThreeValues() throws ParseException {
        JsonArray range = new JsonArray();
        range.add(0);
        range.add(1);
        range.add(2);
        JsonArray rangeSet = new JsonArray();
        rangeSet.add(range);
        parse(bitMap(8, "little-endian", flags("first", rangeSet)));
    }

    /**
     * Test that an integer range must be a JSON array
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testRangeNotAnArray() throws ParseException {
        JsonArray rangeSet = new JsonArray();
        rangeSet.add(3);
        parse(bitMap(8, "little-endian", flags("first", rangeSet)));
    }

    /**
     * Test that the lower value of a range cannot be greater than its upper
     * value
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testRangeLowerGreaterThanUpper() throws ParseException {
        parse(bitMap(8, "little-endian", flags("first", ranges(3, 1))));
    }

    /**
     * Test that a bit index cannot be negative
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testNegativeBitIndex() throws ParseException {
        parse(bitMap(8, "little-endian", flags("first", ranges(-1, 0))));
    }

    /**
     * Test that a bit index must be less than the length of the bit map
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testBitIndexEqualToLength() throws ParseException {
        parse(bitMap(8, "little-endian", flags("first", ranges(0, 8))));
    }

    /**
     * Test that a bit index must be less than the length of the bit map, in
     * any range of the flag
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testBitIndexOutOfRangeInSecondRange() throws ParseException {
        parse(bitMap(8, "little-endian", flags("first", ranges(0, 0, 9, 10))));
    }

    // ------------------------------------------------------------------------
    // Type and parameters
    // ------------------------------------------------------------------------

    /**
     * Test that the fixed-length bit map field class is not supported in
     * CTF 1.8
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testCTF1NotSupported() throws ParseException {
        BitMapDeclarationParser.INSTANCE.parse(new CTFJsonMetadataNode(null, "bit-map", ""),
                new BitMapDeclarationParser.Param(new CTFTrace()));
    }

    /**
     * Test that the parser requires its own parameter type
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidParam() throws ParseException {
        BitMapDeclarationParser.INSTANCE.parse(createNode(bitMap(8, "little-endian")), null);
    }
}
