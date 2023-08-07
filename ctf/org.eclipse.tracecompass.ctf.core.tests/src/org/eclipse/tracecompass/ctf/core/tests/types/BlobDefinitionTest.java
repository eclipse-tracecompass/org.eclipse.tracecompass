/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sehr Moosabhoy - Initial implementation
 *******************************************************************************/

package org.eclipse.tracecompass.ctf.core.tests.types;

import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.tracecompass.ctf.core.event.types.BlobDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.BlobDefinition;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>BlobDefinitionTest</code> contains tests for the class
 * <code>{@link BlobDefinition}</code>.
 *
 * @author Sehr Moosabhoy
 */
public class BlobDefinitionTest {

    private BlobDefinition fixture;
    private static final int LENGTH = 16;
    @NonNull private static final String MEDIA_TYPE = "\"application/octet-stream\"";
    private static final byte[] UUID_ARRAY = new byte[] { 0x2a, 0x64, 0x22, (byte) 0xd0, 0x6c, (byte) 0xee, 0x11, (byte) 0xe0, (byte) 0x8c, 0x08, (byte) 0xcb, 0x07, (byte) 0xd7, (byte) 0xb3, (byte) 0xa5, 0x64 };

    /**
     * Perform pre-test initialization.
     *
     * @throws CTFException
     *             won't happen
     */
    @Before
    public void setUp() throws CTFException {
        String name = "testBlob";
        BlobDeclaration blobDec = new BlobDeclaration(LENGTH, MEDIA_TYPE);
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        BitBuffer bb = new BitBuffer(byteBuffer);
        byteBuffer.mark();
        byteBuffer.put(UUID_ARRAY);
        byteBuffer.reset();
        fixture = blobDec.createDefinition(null, name, bb);
    }

    /**
     * Run the BlobDefinition constructor test.
     */
    @Test
    public void testBlobDefinition() {
        BlobDeclaration declaration = new BlobDeclaration(LENGTH, MEDIA_TYPE);
        IDefinitionScope definitionScope = null;
        String fieldName = "";

        BlobDefinition result = new BlobDefinition(declaration,
                definitionScope, fieldName, UUID_ARRAY, MEDIA_TYPE);

        assertNotNull(result);
    }

    /**
     * Run the Blob getDeclaration() method test.
     */
    @Test
    public void testGetDeclaration() {
        BlobDeclaration result = fixture.getDeclaration();
        assertNotNull(result);
    }

    /**
     * Run the Blob getBytes() method test.
     */
    @Test
    public void testGetBytes() {
        byte[] result = fixture.getBytes();
        assertNotNull(result);
    }

    /**
     * Run the Blob getStringValue() method test.
     */
    @Test
    public void testGetStringValue() {
        String result = fixture.getStringValue();
        assertNotNull(result);
    }

    /**
     * Run the Blob toString() method test.
     */
    @Test
    public void testToString() {
        String result = fixture.toString();
        assertNotNull(result);
    }
}
