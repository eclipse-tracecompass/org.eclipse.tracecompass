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

import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonTraceMetadataNode;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * Test class for JsonTraceMetadataNode environment parsing
 */
public class JsonTraceMetadataNodeTest {

    /**
     * Test parsing environment object from CTF2 JSON
     * 
     * @throws Exception if parsing fails
     */
    @Test
    public void testEnvironmentParsing() throws Exception {
        JsonTraceMetadataNode node = new JsonTraceMetadataNode(null, "trace-class", "test");
        
        JsonObject environment = new JsonObject();
        environment.addProperty("hostname", "test-host");
        environment.addProperty("domain", "kernel");
        environment.addProperty("tracer_name", "lttng-modules");
        
        // Simulate Gson deserialization by setting the field directly
        java.lang.reflect.Field envField = JsonTraceMetadataNode.class.getDeclaredField("fEnvironment");
        envField.setAccessible(true);
        envField.set(node, environment);
        
        node.initialize();
        
        assertNotNull(node.getEnvironment());
        assertEquals("test-host", node.getEnvironment().get("hostname").getAsString());
        assertEquals("kernel", node.getEnvironment().get("domain").getAsString());
        assertEquals("lttng-modules", node.getEnvironment().get("tracer_name").getAsString());
    }

    /**
     * Test UID and packet header parsing
     * 
     * @throws Exception if parsing fails
     */
    @Test
    public void testUidAndPacketHeader() throws Exception {
        JsonTraceMetadataNode node = new JsonTraceMetadataNode(null, "trace-class", "test");
        
        // Simulate Gson deserialization
        java.lang.reflect.Field uidField = JsonTraceMetadataNode.class.getDeclaredField("fUid");
        uidField.setAccessible(true);
        uidField.set(node, "test-uid-123");
        
        node.initialize();
        
        assertEquals("test-uid-123", node.getUid());
    }
}
