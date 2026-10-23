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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.OptionalDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.VariantDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.core.trace.ICTFStream;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ArrayDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.types.SequenceDeclaration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Test the CTF2 field class aliases, following the specification:
 * https://diamon.org/ctf/#field-class-alias-frag
 *
 * The following JSON object properties may refer to the effective field class
 * of a previously occurring field class alias using its name:
 * <ul>
 * <li>field class alias fragment: field-class</li>
 * <li>trace class fragment: packet-header-field-class</li>
 * <li>data stream class fragment: packet-context-field-class,
 * event-record-header-field-class and
 * event-record-common-context-field-class</li>
 * <li>event record class fragment: specific-context-field-class and
 * payload-field-class</li>
 * <li>structure field member class: field-class</li>
 * <li>abstract array field class: element-field-class</li>
 * <li>optional field class: field-class</li>
 * <li>variant field class option: field-class</li>
 * </ul>
 *
 * Each test writes the metadata stream of a trace without data streams and
 * checks the declarations created when opening the trace.
 *
 * @author Arnaud Fiorini
 */
public class FieldClassAliasTest {

    private static final char RECORD_SEPARATOR = '\u001e';

    /**
     * Temporary folder holding the trace
     */
    @Rule
    public TemporaryFolder fFolder = new TemporaryFolder();

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    /**
     * Write the metadata stream made of the given fragments in a new trace
     * directory and open the trace
     *
     * @param fragments
     *            the fragments of the metadata stream, in order
     * @return the opened trace
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    private CTFTrace openTrace(JsonObject... fragments) throws IOException, CTFException {
        File traceDirectory = fFolder.newFolder();
        StringBuilder metadata = new StringBuilder();
        for (JsonObject fragment : fragments) {
            metadata.append(RECORD_SEPARATOR).append(fragment.toString());
        }
        Files.writeString(new File(traceDirectory, "metadata").toPath(), metadata.toString(), StandardCharsets.UTF_8);
        return new CTFTrace(traceDirectory);
    }

    /**
     * Create a CTF2 preamble fragment
     */
    private static JsonObject preamble() {
        JsonObject preamble = new JsonObject();
        preamble.addProperty("type", "preamble");
        preamble.addProperty("version", 2);
        return preamble;
    }

    /**
     * Create a trace class fragment without packet header
     */
    private static JsonObject traceClass() {
        JsonObject traceClass = new JsonObject();
        traceClass.addProperty("type", "trace-class");
        return traceClass;
    }

    /**
     * Create the data stream class fragment of ID 0 without field classes
     */
    private static JsonObject dataStreamClass() {
        JsonObject dataStreamClass = new JsonObject();
        dataStreamClass.addProperty("type", "data-stream-class");
        dataStreamClass.addProperty("id", 0);
        return dataStreamClass;
    }

    /**
     * Create an event record class fragment of data stream class 0 without
     * payload
     */
    private static JsonObject eventRecordClass() {
        JsonObject eventRecordClass = new JsonObject();
        eventRecordClass.addProperty("type", "event-record-class");
        eventRecordClass.addProperty("id", 0);
        eventRecordClass.addProperty("data-stream-class-id", 0);
        eventRecordClass.addProperty("name", "event");
        return eventRecordClass;
    }

    /**
     * Create an event record class fragment of data stream class 0 with the
     * given payload member classes
     */
    private static JsonObject eventRecordClass(JsonObject... payloadMembers) {
        JsonObject eventRecordClass = eventRecordClass();
        eventRecordClass.add("payload-field-class", structure(payloadMembers));
        return eventRecordClass;
    }

    /**
     * Create a field class alias fragment
     *
     * @param name
     *            the name of the alias
     * @param fieldClass
     *            the aliased field class, or a reference to another alias
     */
    private static JsonObject fieldClassAlias(String name, JsonElement fieldClass) {
        JsonObject alias = new JsonObject();
        alias.addProperty("type", "field-class-alias");
        alias.addProperty("name", name);
        alias.add("field-class", fieldClass);
        return alias;
    }

    /**
     * Create a little-endian, byte-aligned fixed-length unsigned integer field
     * class
     *
     * @param length
     *            the length in bits
     */
    private static JsonObject unsignedInteger(int length) {
        JsonObject integer = new JsonObject();
        integer.addProperty("type", "fixed-length-unsigned-integer");
        integer.addProperty("length", length);
        integer.addProperty("byte-order", "little-endian");
        integer.addProperty("alignment", 8);
        return integer;
    }

    /**
     * Create a structure field class
     *
     * @param members
     *            the member classes, in order
     */
    private static JsonObject structure(JsonObject... members) {
        JsonArray memberClasses = new JsonArray();
        for (JsonObject member : members) {
            memberClasses.add(member);
        }
        JsonObject structure = new JsonObject();
        structure.addProperty("type", "structure");
        structure.add("member-classes", memberClasses);
        return structure;
    }

    /**
     * Create a structure field member class
     *
     * @param name
     *            the name of the member
     * @param fieldClass
     *            the field class of the member, or a reference to an alias
     */
    private static JsonObject member(String name, JsonElement fieldClass) {
        JsonObject member = new JsonObject();
        member.addProperty("name", name);
        member.add("field-class", fieldClass);
        return member;
    }

    /**
     * Create a reference to a field class alias
     */
    private static JsonPrimitive aliasRef(String name) {
        return new JsonPrimitive(name);
    }

    /**
     * Create a field location of a field at the root of the event record
     * payload
     *
     * @param fieldName
     *            the name of the field
     */
    private static JsonObject payloadLocation(String fieldName) {
        JsonArray path = new JsonArray();
        path.add(fieldName);
        JsonObject location = new JsonObject();
        location.addProperty("origin", "event-record-payload");
        location.add("path", path);
        return location;
    }

    /**
     * Create an integer range set containing a single value
     *
     * @param value
     *            the value
     */
    private static JsonArray singleValueRange(long value) {
        JsonArray range = new JsonArray();
        range.add(value);
        range.add(value);
        JsonArray ranges = new JsonArray();
        ranges.add(range);
        return ranges;
    }

    /**
     * Field class alias named "uint8" of an 8-bit unsigned integer
     */
    private static JsonObject uint8Alias() {
        return fieldClassAlias("uint8", unsignedInteger(8));
    }

    /**
     * Field class alias named "uint16" of a 16-bit unsigned integer
     */
    private static JsonObject uint16Alias() {
        return fieldClassAlias("uint16", unsignedInteger(16));
    }

    /**
     * Structure alias named "value-struct" whose single member is named
     * "value" and refers to the "uint16" alias
     */
    private static JsonObject structAlias() {
        return fieldClassAlias("value-struct", structure(member("value", aliasRef("uint16"))));
    }

    /**
     * Get the single event record class of data stream class 0
     */
    private static IEventDeclaration getEvent(CTFTrace trace) {
        ICTFStream stream = trace.getStream(0L);
        assertNotNull(stream);
        IEventDeclaration event = stream.getEventDeclarations().get(0);
        assertNotNull(event);
        return event;
    }

    /**
     * Get the declaration of a field of the payload of the single event record
     * class
     */
    private static IDeclaration getPayloadField(CTFTrace trace, String fieldName) {
        StructDeclaration payload = getEvent(trace).getFields();
        assertNotNull(payload);
        IDeclaration field = payload.getField(fieldName);
        assertNotNull("Missing payload field " + fieldName, field);
        return field;
    }

    /**
     * Assert that a declaration is an integer of the expected length
     */
    private static void assertInteger(IDeclaration declaration, int expectedLength) {
        assertTrue(declaration instanceof IntegerDeclaration);
        assertEquals(expectedLength, ((IntegerDeclaration) declaration).getLength());
    }

    /**
     * Assert that a structure is the one of {@link #structAlias()}
     */
    private static void assertValueStruct(IDeclaration declaration) {
        assertTrue(declaration instanceof StructDeclaration);
        IDeclaration value = ((StructDeclaration) declaration).getField("value");
        assertNotNull(value);
        assertInteger(value, 16);
    }

    // ------------------------------------------------------------------------
    // Field class alias fragment
    // ------------------------------------------------------------------------

    /**
     * The field-class property of a field class alias fragment may refer to a
     * previously occurring field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testFieldClassAliasOfAlias() throws IOException, CTFException {
        JsonObject byteAlias = fieldClassAlias("byte", aliasRef("uint8"));
        CTFTrace trace = openTrace(preamble(), uint8Alias(), byteAlias, traceClass(), dataStreamClass(),
                eventRecordClass(member("field", aliasRef("byte"))));

        assertInteger(getPayloadField(trace, "field"), 8);
    }

    /**
     * A field class alias may be a structure using other aliases. Structures
     * nested in the payload are flattened, the member of the aliased structure
     * is then named "field.value".
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testStructureFieldClassAlias() throws IOException, CTFException {
        CTFTrace trace = openTrace(preamble(), uint16Alias(), structAlias(), traceClass(), dataStreamClass(),
                eventRecordClass(member("field", aliasRef("value-struct"))));

        assertInteger(getPayloadField(trace, "field.value"), 16);
    }

    // ------------------------------------------------------------------------
    // Trace class fragment
    // ------------------------------------------------------------------------

    /**
     * The packet-header-field-class property of a trace class fragment may
     * refer to a field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testPacketHeaderFieldClass() throws IOException, CTFException {
        JsonObject traceClass = traceClass();
        traceClass.add("packet-header-field-class", aliasRef("value-struct"));
        CTFTrace trace = openTrace(preamble(), uint16Alias(), structAlias(), traceClass, dataStreamClass(),
                eventRecordClass());

        assertValueStruct(trace.getPacketHeader());
    }

    // ------------------------------------------------------------------------
    // Data stream class fragment
    // ------------------------------------------------------------------------

    /**
     * The packet-context-field-class property of a data stream class fragment
     * may refer to a field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testPacketContextFieldClass() throws IOException, CTFException {
        JsonObject dataStreamClass = dataStreamClass();
        dataStreamClass.add("packet-context-field-class", aliasRef("value-struct"));
        CTFTrace trace = openTrace(preamble(), uint16Alias(), structAlias(), traceClass(), dataStreamClass,
                eventRecordClass());

        assertValueStruct(trace.getStream(0L).getPacketContextDecl());
    }

    /**
     * The event-record-header-field-class property of a data stream class
     * fragment may refer to a field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testEventRecordHeaderFieldClass() throws IOException, CTFException {
        JsonObject dataStreamClass = dataStreamClass();
        dataStreamClass.add("event-record-header-field-class", aliasRef("value-struct"));
        CTFTrace trace = openTrace(preamble(), uint16Alias(), structAlias(), traceClass(), dataStreamClass,
                eventRecordClass());

        assertValueStruct(trace.getStream(0L).getEventHeaderDeclaration());
    }

    /**
     * The event-record-common-context-field-class property of a data stream
     * class fragment may refer to a field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testEventRecordCommonContextFieldClass() throws IOException, CTFException {
        JsonObject dataStreamClass = dataStreamClass();
        dataStreamClass.add("event-record-common-context-field-class", aliasRef("value-struct"));
        CTFTrace trace = openTrace(preamble(), uint16Alias(), structAlias(), traceClass(), dataStreamClass,
                eventRecordClass());

        assertValueStruct(trace.getStream(0L).getEventContextDecl());
    }

    // ------------------------------------------------------------------------
    // Event record class fragment
    // ------------------------------------------------------------------------

    /**
     * The specific-context-field-class property of an event record class
     * fragment may refer to a field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testSpecificContextFieldClass() throws IOException, CTFException {
        JsonObject eventRecordClass = eventRecordClass();
        eventRecordClass.add("specific-context-field-class", aliasRef("value-struct"));
        CTFTrace trace = openTrace(preamble(), uint16Alias(), structAlias(), traceClass(), dataStreamClass(), eventRecordClass);

        assertValueStruct(getEvent(trace).getContext());
    }

    /**
     * The payload-field-class property of an event record class fragment may
     * refer to a field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testPayloadFieldClass() throws IOException, CTFException {
        JsonObject eventRecordClass = eventRecordClass();
        eventRecordClass.add("payload-field-class", aliasRef("value-struct"));
        CTFTrace trace = openTrace(preamble(), uint16Alias(), structAlias(), traceClass(), dataStreamClass(), eventRecordClass);

        assertValueStruct(getEvent(trace).getFields());
    }

    // ------------------------------------------------------------------------
    // Field classes
    // ------------------------------------------------------------------------

    /**
     * The field-class property of a structure field member class may refer to
     * a field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testStructureMemberFieldClass() throws IOException, CTFException {
        CTFTrace trace = openTrace(preamble(), uint8Alias(), uint16Alias(), traceClass(), dataStreamClass(),
                eventRecordClass(member("small", aliasRef("uint8")), member("big", aliasRef("uint16"))));

        assertInteger(getPayloadField(trace, "small"), 8);
        assertInteger(getPayloadField(trace, "big"), 16);
    }

    /**
     * The element-field-class property of a static-length array field class
     * may refer to a field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testStaticLengthArrayElementFieldClass() throws IOException, CTFException {
        JsonObject array = new JsonObject();
        array.addProperty("type", "static-length-array");
        array.addProperty("length", 4);
        array.add("element-field-class", aliasRef("uint16"));
        CTFTrace trace = openTrace(preamble(), uint16Alias(), traceClass(), dataStreamClass(),
                eventRecordClass(member("array", array)));

        IDeclaration declaration = getPayloadField(trace, "array");
        assertTrue(declaration instanceof ArrayDeclaration);
        assertEquals(4, ((ArrayDeclaration) declaration).getLength());
        assertInteger(((ArrayDeclaration) declaration).getElementType(), 16);
    }

    /**
     * The element-field-class property of a dynamic-length array field class
     * may refer to a field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testDynamicLengthArrayElementFieldClass() throws IOException, CTFException {
        JsonObject array = new JsonObject();
        array.addProperty("type", "dynamic-length-array");
        array.add("length-field-location", payloadLocation("length"));
        array.add("element-field-class", aliasRef("uint16"));
        CTFTrace trace = openTrace(preamble(), uint8Alias(), uint16Alias(), traceClass(), dataStreamClass(),
                eventRecordClass(member("length", aliasRef("uint8")), member("array", array)));

        IDeclaration declaration = getPayloadField(trace, "array");
        assertTrue(declaration instanceof SequenceDeclaration);
        assertEquals("length", ((SequenceDeclaration) declaration).getLengthName());
        assertInteger(((SequenceDeclaration) declaration).getElementType(), 16);
    }

    /**
     * The field-class property of an optional field class may refer to a field
     * class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testOptionalFieldClass() throws IOException, CTFException {
        JsonObject optional = new JsonObject();
        optional.addProperty("type", "optional");
        optional.add("selector-field-location", payloadLocation("selector"));
        optional.add("selector-field-ranges", singleValueRange(1));
        optional.add("field-class", aliasRef("uint16"));
        CTFTrace trace = openTrace(preamble(), uint8Alias(), uint16Alias(), traceClass(), dataStreamClass(),
                eventRecordClass(member("selector", aliasRef("uint8")), member("optional", optional)));

        IDeclaration declaration = getPayloadField(trace, "optional");
        assertTrue(declaration instanceof OptionalDeclaration);
        assertInteger(((OptionalDeclaration) declaration).getFieldDeclaration(), 16);
    }

    /**
     * The field-class property of a variant field class option may refer to a
     * field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testVariantOptionFieldClass() throws IOException, CTFException {
        JsonObject smallOption = member("small", aliasRef("uint8"));
        smallOption.add("selector-field-ranges", singleValueRange(0));
        JsonObject bigOption = member("big", aliasRef("uint16"));
        bigOption.add("selector-field-ranges", singleValueRange(1));
        JsonArray options = new JsonArray();
        options.add(smallOption);
        options.add(bigOption);

        JsonObject variant = new JsonObject();
        variant.addProperty("type", "variant");
        variant.add("selector-field-location", payloadLocation("selector"));
        variant.add("options", options);
        CTFTrace trace = openTrace(preamble(), uint8Alias(), uint16Alias(), traceClass(), dataStreamClass(),
                eventRecordClass(member("selector", aliasRef("uint8")), member("variant", variant)));

        IDeclaration declaration = getPayloadField(trace, "variant");
        assertTrue(declaration instanceof VariantDeclaration);
        VariantDeclaration variantDeclaration = (VariantDeclaration) declaration;
        assertInteger(variantDeclaration.getFields().get("small"), 8);
        assertInteger(variantDeclaration.getFields().get("big"), 16);
    }

    // ------------------------------------------------------------------------
    // Alias resolution rules
    // ------------------------------------------------------------------------

    /**
     * A field class may only refer to a previously occurring field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     */
    @Test
    public void testAliasMustPreviouslyOccur() throws IOException {
        try {
            openTrace(preamble(), traceClass(), dataStreamClass(),
                    eventRecordClass(member("field", aliasRef("uint8"))),
                    uint8Alias());
            fail("A field class cannot refer to an alias defined after it");
        } catch (CTFException e) {
            // Expected
        }
    }

    /**
     * A string which is not one of the properties listed by the specification
     * is not replaced, even if it is the name of a field class alias
     *
     * @throws IOException
     *             if the metadata stream cannot be written
     * @throws CTFException
     *             if the trace cannot be opened
     */
    @Test
    public void testOnlyFieldClassPropertiesAreResolved() throws IOException, CTFException {
        CTFTrace trace = openTrace(preamble(), uint8Alias(), traceClass(), dataStreamClass(),
                eventRecordClass(member("uint8", aliasRef("uint8"))));

        assertInteger(getPayloadField(trace, "uint8"), 8);
    }
}
