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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.BitArrayDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.BitArrayDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.BitMapDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.BitMapDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.BitOrder;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerRange;
import org.junit.Test;

/**
 * Test for the fixed-length bit map declaration class in CTF2, following the
 * specification: https://diamon.org/ctf/#fl-bm-fc
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("javadoc")
public class BitMapDeclarationTest {

    private static Map<String, List<IntegerRange>> flags() {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("active", List.of(new IntegerRange(0, 0)));
        flags.put("mode", List.of(new IntegerRange(1, 3), new IntegerRange(6, 6)));
        flags.put("error", List.of(new IntegerRange(7, 7)));
        return flags;
    }

    @Test
    public void constructorTest() {
        assertNotNull(new BitMapDeclaration(1, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, Map.of("flag", List.of(new IntegerRange(0, 0)))));
        assertNotNull(new BitMapDeclaration(8, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 8, flags()));
    }

    /**
     * The length must be greater than zero
     */
    @Test(expected = IllegalArgumentException.class)
    public void zeroLengthTest() {
        new BitMapDeclaration(0, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags());
    }

    /**
     * The length must be greater than zero
     */
    @Test(expected = IllegalArgumentException.class)
    public void negativeLengthTest() {
        new BitMapDeclaration(-8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags());
    }

    /**
     * The flags must contain one or more flags
     */
    @Test(expected = IllegalArgumentException.class)
    public void emptyFlagsTest() {
        new BitMapDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, Map.of());
    }

    @Test
    public void getterTest() {
        BitMapDeclaration declaration = new BitMapDeclaration(12, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 16, flags());
        assertEquals(12, declaration.getLength());
        assertEquals(ByteOrder.BIG_ENDIAN, declaration.getByteOrder());
        assertEquals(16, declaration.getAlignment());
        assertEquals(12, declaration.getMaximumSize());
    }

    /**
     * The flags keep their declaration order and their ranges
     */
    @Test
    public void getFlagsTest() {
        BitMapDeclaration declaration = new BitMapDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags());
        Map<String, List<IntegerRange>> result = declaration.getFlags();
        assertEquals(List.of("active", "mode", "error"), List.copyOf(result.keySet()));
        List<IntegerRange> mode = result.get("mode");
        assertNotNull(mode);
        assertEquals(2, mode.size());
        assertEquals(1, mode.get(0).getStart());
        assertEquals(3, mode.get(0).getEnd());
        assertEquals(6, mode.get(1).getStart());
        assertEquals(6, mode.get(1).getEnd());
    }

    /**
     * A modification of the map given to the constructor does not change the
     * declaration
     */
    @Test
    public void flagsCopiedTest() {
        Map<String, List<IntegerRange>> flags = flags();
        BitMapDeclaration declaration = new BitMapDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags);
        flags.put("other", List.of(new IntegerRange(5, 5)));
        flags.remove("active");
        assertEquals(List.of("active", "mode", "error"), List.copyOf(declaration.getFlags().keySet()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void flagsUnmodifiableTest() {
        BitMapDeclaration declaration = new BitMapDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags());
        declaration.getFlags().put("other", List.of(new IntegerRange(5, 5)));
    }

    /**
     * A bit may have more than one associated name
     */
    @Test
    public void overlappingFlagsTest() {
        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        flags.put("low", List.of(new IntegerRange(0, 3)));
        flags.put("first", List.of(new IntegerRange(0, 0)));
        BitMapDeclaration declaration = new BitMapDeclaration(4, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags);
        assertEquals(2, declaration.getFlags().size());
    }

    /**
     * Reading a bit map creates a bit map definition
     */
    @Test
    public void createDefinitionTest() throws CTFException {
        BitMapDeclaration declaration = new BitMapDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8, flags());
        BitArrayDefinition definition = declaration.createDefinition(null, "flags", new BitBuffer(ByteBuffer.wrap(new byte[] { 1 })));
        assertTrue(definition instanceof BitMapDefinition);
    }

    @Test
    public void toStringTest() {
        BitMapDeclaration declaration = new BitMapDeclaration(12, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags());
        String result = declaration.toString();
        assertTrue(result.contains("bit map"));
        assertTrue(result.contains("length:12"));
        assertTrue(result.contains("active"));
        assertTrue(result.contains("mode"));
        assertTrue(result.contains("error"));
    }

    @Test
    public void binaryEquivalentTest() {
        BitMapDeclaration a = new BitMapDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8, flags());
        BitMapDeclaration b = new BitMapDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8, flags());
        BitMapDeclaration c = new BitMapDeclaration(16, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8, flags());
        BitMapDeclaration d = new BitMapDeclaration(8, ByteOrder.BIG_ENDIAN, BitOrder.LAST_TO_FIRST, 8, flags());
        BitMapDeclaration e = new BitMapDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 1, flags());
        BitArrayDeclaration f = new BitArrayDeclaration(8, ByteOrder.LITTLE_ENDIAN, BitOrder.FIRST_TO_LAST, 8);

        assertTrue(a.isBinaryEquivalent(a));
        assertTrue(a.isBinaryEquivalent(b));
        assertTrue(b.isBinaryEquivalent(a));
        assertFalse(a.isBinaryEquivalent(c));
        assertFalse(a.isBinaryEquivalent(d));
        assertFalse(a.isBinaryEquivalent(e));
        assertFalse(a.isBinaryEquivalent(f));
        assertFalse(f.isBinaryEquivalent(a));
        assertFalse(a.isBinaryEquivalent(null));
    }
}
