/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.ctf.core.tests.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.ByteOrder;

import org.eclipse.tracecompass.ctf.core.event.types.FloatDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.floatingpoint.FloatDeclarationParser;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Test class for FloatDeclarationParser CTF2 support
 */
public class FloatDeclarationParserTest {

    /**
     * Test parsing 32-bit floating point from CTF2 JSON
     * 
     * @throws Exception if parsing fails
     */
    @Test
    public void testCTF2Float32Parsing() throws Exception {
        CTFTrace trace = new CTFTrace();
        
        JsonObject fieldClass = new JsonObject();
        fieldClass.add("length", new JsonPrimitive(32));
        fieldClass.add("byte-order", new JsonPrimitive("le"));
        
        JsonStructureFieldMemberMetadataNode node = new JsonStructureFieldMemberMetadataNode(null, "test", "test", "float_field", fieldClass);
        
        FloatDeclaration result = FloatDeclarationParser.INSTANCE.parse(node, 
            new FloatDeclarationParser.Param(trace));
        
        assertNotNull(result);
        assertEquals(8, result.getExponent());
        assertEquals(24, result.getMantissa());
        assertEquals(ByteOrder.LITTLE_ENDIAN, result.getByteOrder());
    }

    /**
     * Test parsing 64-bit floating point from CTF2 JSON
     * 
     * @throws Exception if parsing fails
     */
    @Test
    public void testCTF2Float64Parsing() throws Exception {
        CTFTrace trace = new CTFTrace();
        
        JsonObject fieldClass = new JsonObject();
        fieldClass.add("length", new JsonPrimitive(64));
        fieldClass.add("byte-order", new JsonPrimitive("be"));
        fieldClass.add("alignment", new JsonPrimitive(8));
        
        JsonStructureFieldMemberMetadataNode node = new JsonStructureFieldMemberMetadataNode(null, "test", "test", "double_field", fieldClass);
        
        FloatDeclaration result = FloatDeclarationParser.INSTANCE.parse(node, 
            new FloatDeclarationParser.Param(trace));
        
        assertNotNull(result);
        assertEquals(11, result.getExponent());
        assertEquals(53, result.getMantissa());
        assertEquals(ByteOrder.BIG_ENDIAN, result.getByteOrder());
        assertEquals(8, result.getAlignment());
    }
}
