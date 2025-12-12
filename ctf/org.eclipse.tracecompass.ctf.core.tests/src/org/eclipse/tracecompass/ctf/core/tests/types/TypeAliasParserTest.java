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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.FloatDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeAliasParser;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Test class for TypeAliasParser CTF2 field type support
 */
public class TypeAliasParserTest {

    /**
     * Test parsing fixed-length floating point field class
     *
     * @throws Exception if parsing fails
     */
    @Test
    public void testFixedLengthFloatingPointParsing() throws Exception {
        CTFTrace trace = new CTFTrace();
        DeclarationScope scope = new DeclarationScope(null, "test");

        JsonObject fieldClass = new JsonObject();
        fieldClass.add("type", new JsonPrimitive("fixed-length-floating-point-number"));
        fieldClass.add("length", new JsonPrimitive(32));
        fieldClass.add("byte-order", new JsonPrimitive("le"));

        JsonStructureFieldMemberMetadataNode node = new JsonStructureFieldMemberMetadataNode(null, "fixed-length-floating-point-number", "test", "float_field", fieldClass);

        IDeclaration result = TypeAliasParser.INSTANCE.parse(node,
            new TypeAliasParser.Param(trace, scope));

        assertNotNull(result);
        assertTrue(result instanceof FloatDeclaration);

        FloatDeclaration floatDecl = (FloatDeclaration) result;
        assertTrue(floatDecl.getExponent() > 0);
        assertTrue(floatDecl.getMantissa() > 0);
    }

    /**
     * Test parsing static-length string field class
     *
     * @throws Exception if parsing fails
     */
    @Test
    public void testStaticLengthStringParsing() throws Exception {
        CTFTrace trace = new CTFTrace();
        DeclarationScope scope = new DeclarationScope(null, "test");

        JsonObject fieldClass = new JsonObject();
        fieldClass.add("type", new JsonPrimitive("static-length-string"));
        fieldClass.add("length", new JsonPrimitive(16));
        fieldClass.add("encoding", new JsonPrimitive("utf-8"));

        JsonStructureFieldMemberMetadataNode node = new JsonStructureFieldMemberMetadataNode(null, "static-length-string", "test", "string_field", fieldClass);

        IDeclaration result = TypeAliasParser.INSTANCE.parse(node,
            new TypeAliasParser.Param(trace, scope));

        assertNotNull(result);
    }

    /**
     * Test parsing dynamic-length string field class
     *
     * @throws Exception if parsing fails
     */
    @Test
    public void testDynamicLengthStringParsing() throws Exception {
        // Test that the parser doesn't throw an exception for dynamic length strings
        // The actual parsing logic may not be fully implemented yet
        CTFTrace trace = new CTFTrace();
        DeclarationScope scope = new DeclarationScope(null, "test");

        // Register a length field in the scope
        IntegerDeclaration lengthDecl = IntegerDeclaration.UINT_5L_DECL;
        scope.registerIdentifier("length", lengthDecl);

        JsonObject fieldClass = new JsonObject();
        fieldClass.add("type", new JsonPrimitive("dynamic-length-string"));
        fieldClass.add("encoding", new JsonPrimitive("utf-8"));
        JsonObject lengthFieldLocation = new JsonObject();
        lengthFieldLocation.add("path", new JsonPrimitive("length"));
        fieldClass.add("length-field-location", lengthFieldLocation);

        JsonStructureFieldMemberMetadataNode node = new JsonStructureFieldMemberMetadataNode(null, "dynamic-length-string", "test", "dyn_string_field", fieldClass);

        IDeclaration result = TypeAliasParser.INSTANCE.parse(node,
                new TypeAliasParser.Param(trace, scope));
        // If we get here without exception, the basic parsing works
        assertNotNull(result);

    }
}
