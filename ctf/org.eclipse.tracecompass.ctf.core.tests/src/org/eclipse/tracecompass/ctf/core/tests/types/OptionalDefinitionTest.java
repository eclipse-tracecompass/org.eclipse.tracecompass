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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.BooleanDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.Definition;
import org.eclipse.tracecompass.ctf.core.event.types.Encoding;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerRange;
import org.eclipse.tracecompass.ctf.core.event.types.OptionalDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.OptionalDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.StringDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StringDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDefinition;
import org.junit.Test;

/**
 * Tests for the optional definition class in CTF2, following the
 * specification: https://diamon.org/ctf/#opt-fc
 *
 * Each test decodes a structure made of a selector field, an optional field
 * and a trailing 8-bit field which validates that a disabled optional field
 * is a zero-bit field.
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("javadoc")
public class OptionalDefinitionTest {

    private static final @NonNull String SELECTOR = "has-ip";
    private static final @NonNull String OPTIONAL = "ip";
    private static final @NonNull String TAIL = "tail";

    private static final @NonNull IntegerDeclaration UINT_8 = IntegerDeclaration.createDeclaration(8, false, 10, ByteOrder.LITTLE_ENDIAN, Encoding.NONE, "", 8, null);
    private static final @NonNull IntegerDeclaration INT_8 = IntegerDeclaration.createDeclaration(8, true, 10, ByteOrder.LITTLE_ENDIAN, Encoding.NONE, "", 8, null);
    private static final @NonNull IntegerDeclaration UINT_16 = IntegerDeclaration.createDeclaration(16, false, 10, ByteOrder.LITTLE_ENDIAN, Encoding.NONE, "", 8, null);
    private static final @NonNull IntegerDeclaration UINT_64 = IntegerDeclaration.createDeclaration(64, false, 10, ByteOrder.LITTLE_ENDIAN, Encoding.NONE, "", 8, null);
    private static final @NonNull BooleanDeclaration BOOL_8 = new BooleanDeclaration(8, ByteOrder.LITTLE_ENDIAN, 8);
    private static final @NonNull StringDeclaration STRING = StringDeclaration.getStringDeclaration(Encoding.UTF8);

    private static final List<IntegerRange> RANGES = List.of(new IntegerRange(-12, -12), new IntegerRange(-5, 0), new IntegerRange(15, 35));

    private static StructDefinition getStructDefinition(@NonNull IDeclaration selector, @NonNull OptionalDeclaration optional, byte... data) throws CTFException {
        StructDeclaration struct = new StructDeclaration(8);
        struct.addField(SELECTOR, selector);
        struct.addField(OPTIONAL, optional);
        struct.addField(TAIL, UINT_8);
        ByteBuffer byb = ByteBuffer.wrap(data);
        byb.order(ByteOrder.LITTLE_ENDIAN);
        return struct.createDefinition(null, "payload", new BitBuffer(byb));
    }

    private static OptionalDefinition getOptionalDefinition(StructDefinition struct) {
        Definition definition = struct.getDefinition(OPTIONAL);
        assertTrue(definition instanceof OptionalDefinition);
        return (OptionalDefinition) definition;
    }

    private static long getTail(StructDefinition struct) {
        Definition definition = struct.getDefinition(TAIL);
        assertTrue(definition instanceof IntegerDefinition);
        return ((IntegerDefinition) definition).getValue();
    }

    private static void assertEnabled(StructDefinition struct, long expectedValue) {
        OptionalDefinition optional = getOptionalDefinition(struct);
        assertTrue(optional.isEnabled());
        Definition field = optional.getField();
        assertNotNull(field);
        assertTrue(field instanceof IntegerDefinition);
        assertEquals(expectedValue, ((IntegerDefinition) field).getValue());
        assertEquals(String.valueOf(expectedValue), optional.toString());
    }

    private static void assertDisabled(StructDefinition struct) {
        OptionalDefinition optional = getOptionalDefinition(struct);
        assertFalse(optional.isEnabled());
        assertNull(optional.getField());
        assertEquals(0, optional.size());
        assertEquals("null", optional.toString());
    }

    /**
     * An instance is enabled when a boolean selector is true
     */
    @Test
    public void testBooleanSelectorTrue() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_16, null);
        StructDefinition struct = getStructDefinition(BOOL_8, declaration, (byte) 1, (byte) 0x34, (byte) 0x12, (byte) 7);
        assertEnabled(struct, 0x1234);
        assertEquals(7, getTail(struct));
    }

    /**
     * An instance is disabled when a boolean selector is false, and it is a
     * zero-bit field
     */
    @Test
    public void testBooleanSelectorFalse() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_16, null);
        StructDefinition struct = getStructDefinition(BOOL_8, declaration, (byte) 0, (byte) 7, (byte) 0x12, (byte) 0x34);
        assertDisabled(struct);
        assertEquals(7, getTail(struct));
    }

    /**
     * An instance is enabled when an unsigned integer selector is in the
     * selector field ranges
     */
    @Test
    public void testUnsignedIntegerSelectorInRange() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_8, List.of(new IntegerRange(1, 3), new IntegerRange(10, 10)));
        for (int selector : new int[] { 1, 2, 3, 10 }) {
            StructDefinition struct = getStructDefinition(UINT_8, declaration, (byte) selector, (byte) 42, (byte) 7);
            assertEnabled(struct, 42);
            assertEquals(7, getTail(struct));
        }
    }

    /**
     * An instance is disabled when an unsigned integer selector is not in the
     * selector field ranges
     */
    @Test
    public void testUnsignedIntegerSelectorOutOfRange() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_8, List.of(new IntegerRange(1, 3), new IntegerRange(10, 10)));
        for (int selector : new int[] { 0, 4, 9, 11, 255 }) {
            StructDefinition struct = getStructDefinition(UINT_8, declaration, (byte) selector, (byte) 7, (byte) 42);
            assertDisabled(struct);
            assertEquals(7, getTail(struct));
        }
    }

    /**
     * Test a signed integer selector with the ranges
     */
    @Test
    public void testSignedIntegerSelector() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_8, RANGES);
        for (int selector : new int[] { -12, -5, -1, 0, 15, 20, 35 }) {
            StructDefinition struct = getStructDefinition(INT_8, declaration, (byte) selector, (byte) 42, (byte) 7);
            assertEnabled(struct, 42);
            assertEquals(7, getTail(struct));
        }
        for (int selector : new int[] { -128, -13, -11, -6, 1, 14, 36, 127 }) {
            StructDefinition struct = getStructDefinition(INT_8, declaration, (byte) selector, (byte) 7, (byte) 42);
            assertDisabled(struct);
            assertEquals(7, getTail(struct));
        }
    }

    /**
     * Test a variable-length unsigned integer selector
     */
    @Test
    public void testVariableLengthIntegerSelector() throws CTFException {
        IntegerDeclaration varint = IntegerDeclaration.createVarintDeclaration(false, 10, null, true);
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_8, List.of(new IntegerRange(128, 300)));

        /* 200 is encoded as 0xc8 0x01 in LEB128 */
        StructDefinition struct = getStructDefinition(varint, declaration, (byte) 0xc8, (byte) 0x01, (byte) 42, (byte) 7);
        assertEnabled(struct, 42);
        assertEquals(7, getTail(struct));

        /* 100 is encoded as 0x64 in LEB128 */
        struct = getStructDefinition(varint, declaration, (byte) 0x64, (byte) 7, (byte) 42);
        assertDisabled(struct);
        assertEquals(7, getTail(struct));
    }

    /**
     * An unsigned 64-bit selector value greater than Long.MAX_VALUE is not in
     * any selector field range
     */
    @Test
    public void testLargeUnsignedIntegerSelector() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_8, List.of(new IntegerRange(-1, -1), new IntegerRange(0, Long.MAX_VALUE)));
        byte[] data = { -1, -1, -1, -1, -1, -1, -1, -1, 7, 42 };
        StructDefinition struct = getStructDefinition(UINT_64, declaration, data);
        assertDisabled(struct);
        assertEquals(7, getTail(struct));
    }

    /**
     * Test an optional null-terminated string
     */
    @Test
    public void testOptionalString() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, STRING, RANGES);
        StructDefinition struct = getStructDefinition(INT_8, declaration, (byte) 20, (byte) 'i', (byte) 'p', (byte) 0, (byte) 7);
        OptionalDefinition optional = getOptionalDefinition(struct);
        assertTrue(optional.isEnabled());
        Definition optionalField = optional.getField();
        assertNotNull(optionalField);
        assertTrue(optionalField instanceof StringDefinition);
        assertEquals("ip", ((StringDefinition) optionalField).getValue());
        assertEquals(7, getTail(struct));
    }

    /**
     * Test the getters of the definition
     */
    @Test
    public void testGetDeclaration() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_8, null);
        StructDefinition struct = getStructDefinition(BOOL_8, declaration, (byte) 1, (byte) 42, (byte) 7);
        OptionalDefinition optional = getOptionalDefinition(struct);
        assertSame(declaration, optional.getDeclaration());
        assertEquals(8, optional.size());
    }

    /**
     * The selector field must exist
     */
    @Test(expected = CTFException.class)
    public void testMissingSelector() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration("unknown", UINT_8, null);
        getStructDefinition(BOOL_8, declaration, (byte) 1, (byte) 42, (byte) 7);
    }

    /**
     * The selector field must be a boolean or an integer field
     */
    @Test(expected = CTFException.class)
    public void testInvalidSelectorType() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_8, RANGES);
        getStructDefinition(STRING, declaration, (byte) 'a', (byte) 0, (byte) 42, (byte) 7);
    }

    /**
     * The selector field ranges are required with an integer selector
     */
    @Test(expected = CTFException.class)
    public void testIntegerSelectorWithoutRanges() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_8, null);
        getStructDefinition(UINT_8, declaration, (byte) 1, (byte) 42, (byte) 7);
    }

    /**
     * The selector field must be looked up in a scope
     */
    @Test(expected = CTFException.class)
    public void testNoScope() throws CTFException {
        OptionalDeclaration declaration = new OptionalDeclaration(SELECTOR, UINT_8, null);
        declaration.createDefinition(null, OPTIONAL, new BitBuffer(ByteBuffer.wrap(new byte[] { 42 })));
    }
}
