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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerRange;
import org.eclipse.tracecompass.ctf.core.event.types.OptionalDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StringDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.CTFJsonMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.optional.OptionalDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ArrayDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Test class for the optional declaration parser in CTF2, following the
 * specification: https://diamon.org/ctf/#opt-fc
 *
 * @author Arnaud Fiorini
 */
public class OptionalDeclarationParserTest {

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static OptionalDeclaration parse(JsonStructureFieldMemberMetadataNode node) throws ParseException {
        return OptionalDeclarationParser.INSTANCE.parse(node,
                new OptionalDeclarationParser.Param(new CTFTrace(), new DeclarationScope(null, "test")));
    }

    private static OptionalDeclaration parse(JsonObject fieldClass) throws ParseException {
        return parse(createNode(null, fieldClass));
    }

    private static JsonStructureFieldMemberMetadataNode createNode(CTFJsonMetadataNode parent, JsonObject fieldClass) {
        return new JsonStructureFieldMemberMetadataNode(parent, JsonMetadataStrings.OPTIONAL, "test", "ip", fieldClass);
    }

    /**
     * Create a selector field location in the event record payload
     */
    private static JsonObject selectorFieldLocation(String... path) {
        JsonArray pathArray = new JsonArray();
        for (String element : path) {
            pathArray.add(element);
        }
        JsonObject location = new JsonObject();
        location.addProperty("origin", "event-record-payload");
        location.add("path", pathArray);
        return location;
    }

    /**
     * Create an integer range set from pairs of lower and upper bounds
     */
    private static JsonArray ranges(long... bounds) {
        JsonArray ranges = new JsonArray();
        for (int i = 0; i < bounds.length; i += 2) {
            JsonArray range = new JsonArray();
            range.add(bounds[i]);
            range.add(bounds[i + 1]);
            ranges.add(range);
        }
        return ranges;
    }

    private static JsonObject nullTerminatedString() {
        JsonObject string = new JsonObject();
        string.addProperty("type", "null-terminated-string");
        return string;
    }

    /**
     * Create an optional field class, the null properties are omitted
     */
    private static JsonObject optional(JsonObject location, JsonArray selectorRanges, JsonElement fieldClass) {
        JsonObject optional = new JsonObject();
        optional.addProperty("type", "optional");
        if (location != null) {
            optional.add("selector-field-location", location);
        }
        if (selectorRanges != null) {
            optional.add("selector-field-ranges", selectorRanges);
        }
        if (fieldClass != null) {
            optional.add("field-class", fieldClass);
        }
        return optional;
    }

    /**
     * Optional null-terminated string with a fixed-length signed integer
     * selector field class (example 61 of the specification)
     */
    private static JsonObject integerSelectorExample() {
        return optional(selectorFieldLocation("has-ip"), ranges(-12, -12, -5, 0, 15, 35), nullTerminatedString());
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------

    /**
     * Test parsing an optional static-length array field class with a boolean
     * selector field class (example 60 of the specification)
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testBooleanSelector() throws ParseException {
        JsonObject element = new JsonObject();
        element.addProperty("type", "fixed-length-unsigned-integer");
        element.addProperty("length", 8);
        element.addProperty("byte-order", "little-endian");
        element.addProperty("alignment", 8);

        JsonObject array = new JsonObject();
        array.addProperty("type", "static-length-array");
        array.add("element-field-class", element);
        array.addProperty("length", 16);

        OptionalDeclaration result = parse(optional(selectorFieldLocation("has-ip"), null, array));

        assertNotNull(result);
        assertEquals("has-ip", result.getSelectorName());
        assertNull(result.getSelectorRanges());
        assertTrue(result.getFieldDeclaration() instanceof ArrayDeclaration);
        ArrayDeclaration arrayDeclaration = (ArrayDeclaration) result.getFieldDeclaration();
        assertEquals(16, arrayDeclaration.getLength());
        assertTrue(arrayDeclaration.getElementType() instanceof IntegerDeclaration);
    }

    /**
     * Test parsing an optional null-terminated string field class with a
     * fixed-length signed integer selector field class (example 61 of the
     * specification)
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testIntegerSelector() throws ParseException {
        OptionalDeclaration result = parse(integerSelectorExample());

        assertNotNull(result);
        assertEquals("has-ip", result.getSelectorName());
        assertTrue(result.getFieldDeclaration() instanceof StringDeclaration);

        List<IntegerRange> ranges = result.getSelectorRanges();
        assertNotNull(ranges);
        assertEquals(3, ranges.size());
        assertEquals(-12, ranges.get(0).getStart());
        assertEquals(-12, ranges.get(0).getEnd());
        assertEquals(-5, ranges.get(1).getStart());
        assertEquals(0, ranges.get(1).getEnd());
        assertEquals(15, ranges.get(2).getStart());
        assertEquals(35, ranges.get(2).getEnd());
    }

    /**
     * Test that the selector name is the last element of the selector field
     * location path
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test
    public void testSelectorLocationPath() throws ParseException {
        OptionalDeclaration result = parse(optional(selectorFieldLocation("header", "flags", "enabled"), null, nullTerminatedString()));

        assertEquals("enabled", result.getSelectorName());
    }

    /**
     * Test that an unknown field class alias is rejected
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testUnknownFieldClassAlias() throws ParseException {
        CTFJsonMetadataNode root = new CTFJsonMetadataNode(null, "root", "");
        parse(createNode(root, optional(selectorFieldLocation("has-value"), null, new JsonPrimitive("unknown"))));
    }

    /**
     * Test that the field-class property is required
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testMissingFieldClass() throws ParseException {
        parse(optional(selectorFieldLocation("has-ip"), null, null));
    }

    /**
     * Test that the field-class property must be a field class or a string
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testInvalidFieldClass() throws ParseException {
        parse(optional(selectorFieldLocation("has-ip"), null, new JsonPrimitive(42)));
    }

    /**
     * Test that the selector-field-location property is required
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testMissingSelectorFieldLocation() throws ParseException {
        parse(optional(null, null, nullTerminatedString()));
    }

    /**
     * Test that the selector-field-location path must not be empty
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testEmptySelectorFieldLocationPath() throws ParseException {
        parse(optional(selectorFieldLocation(), null, nullTerminatedString()));
    }

    /**
     * Test that the selector-field-ranges property must not be empty
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testEmptySelectorFieldRanges() throws ParseException {
        parse(optional(selectorFieldLocation("has-ip"), ranges(), nullTerminatedString()));
    }

    /**
     * Test that a selector field range with a lower bound greater than its
     * upper bound is rejected
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testInvalidSelectorFieldRange() throws ParseException {
        parse(optional(selectorFieldLocation("has-ip"), ranges(5, 2), nullTerminatedString()));
    }

    /**
     * Test that a malformed selector field range is rejected
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testMalformedSelectorFieldRange() throws ParseException {
        JsonArray range = new JsonArray();
        range.add(1);
        range.add(2);
        range.add(3);
        JsonArray selectorRanges = new JsonArray();
        selectorRanges.add(range);

        parse(optional(selectorFieldLocation("has-ip"), selectorRanges, nullTerminatedString()));
    }

    /**
     * Test that the optional field class is not supported in CTF 1.8
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = ParseException.class)
    public void testCTF1NotSupported() throws ParseException {
        OptionalDeclarationParser.INSTANCE.parse(new CTFJsonMetadataNode(null, "optional", ""),
                new OptionalDeclarationParser.Param(new CTFTrace(), new DeclarationScope(null, "test")));
    }

    /**
     * Test that the parser requires its own parameter type
     *
     * @throws ParseException
     *             if parsing fails
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidParam() throws ParseException {
        OptionalDeclarationParser.INSTANCE.parse(createNode(null, integerSelectorExample()), null);
    }
}
