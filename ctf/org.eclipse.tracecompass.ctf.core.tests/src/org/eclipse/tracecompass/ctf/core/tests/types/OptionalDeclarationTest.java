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

import java.nio.ByteOrder;
import java.util.List;

import org.eclipse.tracecompass.ctf.core.event.types.BooleanDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.Encoding;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerRange;
import org.eclipse.tracecompass.ctf.core.event.types.OptionalDeclaration;
import org.junit.Test;

/**
 * Test for the optional declaration class in CTF2
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("javadoc")
public class OptionalDeclarationTest {

    private static final IntegerDeclaration UINT_32 = IntegerDeclaration.createDeclaration(32, false, 10, ByteOrder.LITTLE_ENDIAN, Encoding.NONE, "", 8, null);
    private static final List<IntegerRange> RANGES = List.of(new IntegerRange(-12, -12), new IntegerRange(-5, 0), new IntegerRange(15, 35));

    @Test
    public void constructorTest() {
        assertNotNull(new OptionalDeclaration("has-ip", UINT_32, null));
        assertNotNull(new OptionalDeclaration("has-ip", UINT_32, RANGES));
    }

    @Test
    public void getterTest() {
        OptionalDeclaration declaration = new OptionalDeclaration("has-ip", UINT_32, RANGES);
        assertEquals("has-ip", declaration.getSelectorName());
        assertSame(UINT_32, declaration.getFieldDeclaration());
        List<IntegerRange> selectorRanges = declaration.getSelectorRanges();
        assertNotNull(selectorRanges);
        assertEquals(3, selectorRanges.size());
        assertEquals(15, selectorRanges.get(2).getStart());
        assertEquals(35, selectorRanges.get(2).getEnd());
        /* The enabled field aligns itself */
        assertEquals(1, declaration.getAlignment());
        assertEquals(32, declaration.getMaximumSize());

        assertNull(new OptionalDeclaration("has-ip", UINT_32, null).getSelectorRanges());
    }

    @Test
    public void binaryEquivalentTest() {
        OptionalDeclaration a = new OptionalDeclaration("has-ip", UINT_32, RANGES);
        OptionalDeclaration b = new OptionalDeclaration("has-ip", UINT_32, List.copyOf(RANGES));
        OptionalDeclaration c = new OptionalDeclaration("other", UINT_32, RANGES);
        OptionalDeclaration d = new OptionalDeclaration("has-ip", IntegerDeclaration.UINT_8_DECL, RANGES);
        OptionalDeclaration e = new OptionalDeclaration("has-ip", UINT_32, null);
        BooleanDeclaration f = new BooleanDeclaration(32, ByteOrder.LITTLE_ENDIAN, 8);

        assertTrue(a.isBinaryEquivalent(a));
        assertTrue(a.isBinaryEquivalent(b));
        assertTrue(b.isBinaryEquivalent(a));
        assertFalse(a.isBinaryEquivalent(c));
        assertFalse(a.isBinaryEquivalent(d));
        assertFalse(a.isBinaryEquivalent(e));
        assertFalse(a.isBinaryEquivalent(f));
        assertFalse(a.isBinaryEquivalent(null));
    }
}
