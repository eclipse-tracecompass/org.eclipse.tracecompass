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
import static org.junit.Assert.assertTrue;

import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Integration test class for CTF2 parsing functionality
 */
public class CTF2IntegrationTest {

    /**
     * Test parsing integer with mappings for enumeration-like behavior
     *
     * @throws Exception if parsing fails
     */
    @Test
    public void testIntegerWithMappings() throws Exception {
        CTFTrace trace = new CTFTrace();

        JsonObject fieldClass = new JsonObject();
        fieldClass.add("type", new JsonPrimitive("fixed-length-unsigned-integer"));
        fieldClass.add("length", new JsonPrimitive(8));
        fieldClass.add("byte-order", new JsonPrimitive("le"));

        // Add mappings for enumeration-like behavior
        JsonObject mappings = new JsonObject();
        JsonArray range1 = new JsonArray();
        range1.add(new JsonPrimitive(0));
        range1.add(new JsonPrimitive(0));
        JsonArray ranges1 = new JsonArray();
        ranges1.add(range1);
        mappings.add("ZERO", ranges1);

        JsonArray range2 = new JsonArray();
        range2.add(new JsonPrimitive(1));
        range2.add(new JsonPrimitive(1));
        JsonArray ranges2 = new JsonArray();
        ranges2.add(range2);
        mappings.add("ONE", ranges2);

        fieldClass.add("mappings", mappings);

        JsonStructureFieldMemberMetadataNode node = new JsonStructureFieldMemberMetadataNode(null, "fixed-length-unsigned-integer", "test", "int_field", fieldClass);

        IntegerDeclaration result = org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.integer.IntegerDeclarationParser.INSTANCE.parse(node,
            new org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.integer.IntegerDeclarationParser.Param(trace));

        assertNotNull(result);
        assertEquals(8, result.getLength());
        assertNotNull(result.getMappings());
        assertEquals(2, result.getMappings().size());
        assertTrue(result.getMappings().containsKey("ZERO"));
        assertTrue(result.getMappings().containsKey("ONE"));
    }
}
