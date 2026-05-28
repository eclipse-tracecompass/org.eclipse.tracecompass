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
import static org.junit.Assert.assertTrue;

import java.nio.ByteOrder;

import org.eclipse.tracecompass.ctf.core.event.types.BooleanDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.FloatDeclaration;
import org.junit.Test;

/**
 * Test for the boolean declaration class in CTF2
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("javadoc")
public class BooleanDeclarationTest {

    @Test
    public void constructorTest() {
        for (int i = 1; i < 20; i++) {
            BooleanDeclaration booleanDeclaration = new BooleanDeclaration(i, ByteOrder.nativeOrder(), 1);
            assertNotNull(booleanDeclaration);
        }
    }

    @Test
    public void getterTest() {
        BooleanDeclaration booleanDeclaration = new BooleanDeclaration(8, ByteOrder.nativeOrder(), 1);
        assertEquals(booleanDeclaration.getAlignment(), 1);
        assertEquals(booleanDeclaration.getByteOrder(), ByteOrder.nativeOrder());
        assertEquals(booleanDeclaration.getMaximumSize(), 8);
    }

    @Test
    public void binaryEquivalentTest() {
        BooleanDeclaration a = new BooleanDeclaration(8, ByteOrder.BIG_ENDIAN, 0);
        BooleanDeclaration b = new BooleanDeclaration(8, ByteOrder.LITTLE_ENDIAN, 0);
        BooleanDeclaration c = new BooleanDeclaration(8, ByteOrder.BIG_ENDIAN, 0);
        BooleanDeclaration d = new BooleanDeclaration(24, ByteOrder.BIG_ENDIAN, 0);
        FloatDeclaration e = new FloatDeclaration(8, 8, ByteOrder.BIG_ENDIAN, 0);

        assertTrue(a.isBinaryEquivalent(a));
        assertTrue(a.isBinaryEquivalent(b));
        assertTrue(a.isBinaryEquivalent(c));
        assertTrue(!a.isBinaryEquivalent(d));
        assertTrue(!a.isBinaryEquivalent(e));

        assertTrue(b.isBinaryEquivalent(a));
        assertTrue(b.isBinaryEquivalent(b));
        assertTrue(b.isBinaryEquivalent(c));
        assertTrue(!b.isBinaryEquivalent(d));
        assertTrue(!b.isBinaryEquivalent(e));

        assertTrue(!c.isBinaryEquivalent(d));
        assertTrue(!c.isBinaryEquivalent(e));
        assertTrue(!d.isBinaryEquivalent(e));
    }
}
