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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.Definition;
import org.eclipse.tracecompass.ctf.core.event.types.DynamicBlobDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.DynamicBlobDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.Encoding;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDefinition;
import org.junit.Test;

/**
 * Test class for dynamic blob declaration in CTF2
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("javadoc")
public class DynamicBlobDeclarationTest {

    @Test
    public void testStandardBlob() throws CTFException {
        byte[] content = new byte[] {72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33};
        testDynamicBlobDeclaration(content, 12, 12, "lengthField", "application/test", 0);
    }

    @Test
    public void testAlignedBlob() throws CTFException {
        byte[] content = new byte[] {0, 0, 72, 101, 108, 108, 111};
        testDynamicBlobDeclaration(content, 5, 7, "lengthField", "application/test", 14);
    }

    @Test
    public void testEmptyBlob() throws CTFException {
        byte[] content = new byte[] {};
        testDynamicBlobDeclaration(content, 0, 0, "lengthField", "application/test", 0);
    }

    @Test
    public void testBigBlob() throws CTFException {
        byte[] content = new byte[] {72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33, 72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33, 72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33};
        testDynamicBlobDeclaration(content, 32, 36, "lengthField", "application/test", 32);
    }

    private static void testDynamicBlobDeclaration(byte[] content, int blobSize, int bufferSize, @NonNull String lengthFieldName, @NonNull String metadataType, int startingPosition) throws CTFException {
        IntegerDeclaration id = IntegerDeclaration.createDeclaration(8, false, 8,
                ByteOrder.LITTLE_ENDIAN, Encoding.UTF8, "", 32, null);
        DynamicBlobDeclaration declaration = new DynamicBlobDeclaration(lengthFieldName, metadataType);
        StructDeclaration structDec = new StructDeclaration(0);
        structDec.addField(lengthFieldName, id);

        StructDefinition structDef = new StructDefinition(
                structDec,
                null,
                "x",
                new Definition[] {
                        new IntegerDefinition(
                                id,
                                null,
                                lengthFieldName,
                                blobSize)
                });

        ByteBuffer byb = ByteBuffer.allocate(bufferSize);
        byb.order(ByteOrder.nativeOrder());
        byb.mark();
        byb.put(content);
        byb.reset();
        BitBuffer bb = new BitBuffer(byb);
        bb.position(startingPosition);
        DynamicBlobDefinition definition = (DynamicBlobDefinition) declaration.createDefinition(structDef, lengthFieldName, bb);
        assertNotNull(definition);
        byte[] expected = Arrays.copyOfRange(content, (int) Math.ceil((float) startingPosition / 8), content.length);
        assertTrue(Arrays.equals(expected, definition.getBytes()));
        assertEquals(metadataType, definition.getType());
    }
}
