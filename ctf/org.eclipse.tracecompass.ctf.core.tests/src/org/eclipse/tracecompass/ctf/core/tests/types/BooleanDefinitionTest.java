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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.BooleanDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.BooleanDefinition;
import org.junit.Test;

/**
 * Tests for the boolean definition class in CTF2
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("javadoc")
public class BooleanDefinitionTest {

    @NonNull private static final String FIELD_NAME = "float";

    /**
     * Test a larger than long boolean field set to true
     * @throws CTFException
     */
    @Test
    public void testBiggerThanLongTrueBoolean() throws CTFException {
        byte[] data = new byte[9];
        data[1] = (byte) (1 << 3);
        testBooleanCreateDefinition(9, 67, data, 0, true);
    }

    /**
     * Test a large boolean field set to true
     * @throws CTFException
     */
    @Test
    public void testLargeTrueBoolean() throws CTFException {
        byte[] data = new byte[17];
        data[1] = (byte) (1 << 7);
        testBooleanCreateDefinition(17, 129, data, 0, true);
    }

    /**
     * Test a small boolean field set to true
     * @throws CTFException
     */
    @Test
    public void testSmallTrueBoolean() throws CTFException {
        byte[] data = new byte[2];
        data[1] = (byte) (1 << 5);
        testBooleanCreateDefinition(2, 14, data, 0, true);
    }

    /**
     * Test a large boolean field set to false
     * @throws CTFException
     */
    @Test
    public void testLargeFalseBoolean() throws CTFException {
        byte[] data = new byte[17];
        data[16] = (byte) (1 << 7);
        testBooleanCreateDefinition(17, 135, data, 0, false);
    }

    /**
     * Test a small boolean field set to false
     * @throws CTFException
     */
    @Test
    public void testSmallFalseBoolean() throws CTFException {
        byte[] data = new byte[2];
        data[1] = (byte) (1 << 7);
        testBooleanCreateDefinition(2, 12, data, 0, false);
    }
    /**
     * Test a large boolean field with alignment set to false
     * @throws CTFException
     */
    @Test
    public void testLargeFalseBooleanWithAlignment() throws CTFException {
        byte[] data = new byte[20];
        data[0] = 1;
        testBooleanCreateDefinition(20, 145, data, 8, false);
    }

    /**
     * Test a small boolean field with alignment set to false
     * @throws CTFException
     */
    @Test
    public void testSmallFalseBooleanWithAlignment() throws CTFException {
        byte[] data = new byte[3];
        data[0] = (byte) (1 << 1);
        testBooleanCreateDefinition(3, 16, data, 8, false);
    }

    private static void testBooleanCreateDefinition(int bufferSize, int fieldSize, byte[] content, int alignment, boolean expectedValue) throws CTFException {
        BooleanDeclaration declaration = new BooleanDeclaration(fieldSize, ByteOrder.nativeOrder(), alignment);
        ByteBuffer byb = ByteBuffer.allocate(bufferSize);
        byb.order(ByteOrder.nativeOrder());
        byb.mark();
        byb.put(content);
        byb.reset();
        BitBuffer bb = new BitBuffer(byb);
        if (alignment > 1) {
            bb.position(1);
        }
        BooleanDefinition definition = (BooleanDefinition) declaration.createDefinition(null, FIELD_NAME, bb);
        assertNotNull(definition);
        if (expectedValue) {
            assertTrue(definition.getValue());
        } else {
            assertTrue(!definition.getValue());
        }
    }
}
