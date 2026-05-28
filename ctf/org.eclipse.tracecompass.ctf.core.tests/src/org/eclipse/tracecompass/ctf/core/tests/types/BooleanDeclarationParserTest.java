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

import java.nio.ByteOrder;

import org.eclipse.tracecompass.ctf.core.event.types.BooleanDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.bool.BooleanDeclarationParser;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Test class for boolean declaration parser in CTF2
 *
 * @author Arnaud Fiorini
 */
public class BooleanDeclarationParserTest {

    /**
     * Test parsing 14-bit boolean field from CTF2 JSON
     *
     * @throws Exception if parsing fails
     */
    @Test
    public void testCTF2BooleanParsingWithoutAlignment() throws Exception {
        CTFTrace trace = new CTFTrace();

        JsonObject fieldClass = new JsonObject();
        fieldClass.add("length", new JsonPrimitive(14));
        fieldClass.add("byte-order", new JsonPrimitive("little-endian"));

        JsonStructureFieldMemberMetadataNode node = new JsonStructureFieldMemberMetadataNode(null, "test", "test", "fixed-length-boolean", fieldClass);

        BooleanDeclaration result = BooleanDeclarationParser.INSTANCE.parse(node,
            new BooleanDeclarationParser.Param(trace));

        assertNotNull(result);
        assertEquals(1, result.getAlignment());
        assertEquals(14, result.getMaximumSize());
        assertEquals(ByteOrder.LITTLE_ENDIAN, result.getByteOrder());
    }

    /**
     * Test parsing 37-bit boolean field from CTF2 JSON
     *
     * @throws Exception if parsing fails
     */
    @Test
    public void testCTF2BooleanParsingWithAlignment() throws Exception {
        CTFTrace trace = new CTFTrace();

        JsonObject fieldClass = new JsonObject();
        fieldClass.add("length", new JsonPrimitive(37));
        fieldClass.add("byte-order", new JsonPrimitive("big-endian"));
        fieldClass.add("alignment", new JsonPrimitive(8));

        JsonStructureFieldMemberMetadataNode node = new JsonStructureFieldMemberMetadataNode(null, "test", "test", "fixed-length-boolean", fieldClass);

        BooleanDeclaration result = BooleanDeclarationParser.INSTANCE.parse(node,
            new BooleanDeclarationParser.Param(trace));

        assertNotNull(result);
        assertEquals(8, result.getAlignment());
        assertEquals(37, result.getMaximumSize());
        assertEquals(ByteOrder.BIG_ENDIAN, result.getByteOrder());
    }
}
