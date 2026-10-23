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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.optional;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerRange;
import org.eclipse.tracecompass.ctf.core.event.types.OptionalDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeAliasParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * According to the documentation:
 * https://diamon.org/ctf/#opt-fc
 *
 * An optional field class describes optional fields. An optional field is,
 * depending on the value of a previously decoded selector field, an instance
 * of the field class of the field-class property or a zero-bit field.
 *
 * The selector field is located with the required selector-field-location
 * property. If the selector field is an integer field, the
 * selector-field-ranges property is required and the optional field is enabled
 * when the value of the selector field is within those ranges. If the
 * selector field is a boolean field, the optional field is enabled when the
 * selector field is true.
 *
 * @author Arnaud Fiorini
 */
public final class OptionalDeclarationParser extends AbstractScopedCommonTreeParser {

    /**
     * Parameters for the optional parser
     */
    @NonNullByDefault
    public static final class Param implements ICommonTreeParserParameter {
        private final CTFTrace fTrace;
        private final DeclarationScope fScope;

        /**
         * Parameter constructor
         *
         * @param trace
         *            the trace
         * @param scope
         *            the declaration scope
         */
        public Param(CTFTrace trace, DeclarationScope scope) {
            fTrace = trace;
            fScope = scope;
        }
    }

    /**
     * Instance
     */
    public static final OptionalDeclarationParser INSTANCE = new OptionalDeclarationParser();

    private OptionalDeclarationParser() {
    }

    @Override
    public OptionalDeclaration parse(ICTFMetadataNode node, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        if (!(node instanceof JsonStructureFieldMemberMetadataNode member)) {
            throw new ParseException("The optional field class is not supported in CTF 1.8."); //$NON-NLS-1$
        }
        JsonElement fieldClassElement = member.getFieldClass();
        if (fieldClassElement == null || !fieldClassElement.isJsonObject()) {
            throw new ParseException(getClass().getName() + " fieldclass must be a json object."); //$NON-NLS-1$
        }
        JsonObject fieldClass = fieldClassElement.getAsJsonObject();

        JsonElement selectorLocation = fieldClass.get(JsonMetadataStrings.SELECTOR_FIELD_LOCATION);
        if (selectorLocation == null || !selectorLocation.isJsonObject()) {
            throw new ParseException("Optional field class requires a selector-field-location property"); //$NON-NLS-1$
        }
        JsonElement pathElement = selectorLocation.getAsJsonObject().get(JsonMetadataStrings.PATH);
        if (pathElement == null || !pathElement.isJsonArray() || pathElement.getAsJsonArray().isEmpty()) {
            throw new ParseException("Optional field class selector-field-location requires a non-empty path"); //$NON-NLS-1$
        }
        JsonArray path = pathElement.getAsJsonArray();
        String selectorName = path.get(path.size() - 1).getAsString();

        JsonElement optionalFieldClass = fieldClass.get(JsonMetadataStrings.FIELD_CLASS);
        if (optionalFieldClass == null || !optionalFieldClass.isJsonObject()) {
            throw new ParseException("Optional field class requires a field-class property"); //$NON-NLS-1$
        }

        List<IntegerRange> selectorRanges = null;
        JsonElement rangesElement = fieldClass.get(JsonMetadataStrings.SELECTOR_FIELD_RANGES);
        if (rangesElement != null) {
            try {
                selectorRanges = parseRanges(rangesElement);
            } catch (ArithmeticException exception) {
                throw new ParseException("The selector field range contained an invalid integer: " + exception.getMessage()); //$NON-NLS-1$
            }
        }

        CTFTrace trace = ((Param) param).fTrace;
        DeclarationScope scope = ((Param) param).fScope;
        JsonStructureFieldMemberMetadataNode fieldNode = new JsonStructureFieldMemberMetadataNode(node, "", "", "", optionalFieldClass.getAsJsonObject()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        IDeclaration fieldDeclaration = TypeAliasParser.INSTANCE.parse(fieldNode, new TypeAliasParser.Param(trace, scope));

        return new OptionalDeclaration(selectorName, fieldDeclaration, selectorRanges);
    }

    private static List<IntegerRange> parseRanges(JsonElement rangesElement) throws ParseException {
        if (!rangesElement.isJsonArray() || rangesElement.getAsJsonArray().isEmpty()) {
            throw new ParseException("Optional field class selector-field-ranges must be a non-empty JSON array"); //$NON-NLS-1$
        }
        List<IntegerRange> ranges = new ArrayList<>();
        for (JsonElement rangeElement : rangesElement.getAsJsonArray()) {
            if (!rangeElement.isJsonArray() || rangeElement.getAsJsonArray().size() != 2) {
                throw new ParseException("Invalid selector field range: " + rangeElement); //$NON-NLS-1$
            }
            JsonArray rangeArray = rangeElement.getAsJsonArray();
            long start = rangeArray.get(0).getAsBigInteger().longValueExact();
            long end = rangeArray.get(1).getAsBigInteger().longValueExact();
            if (start > end) {
                throw new ParseException("Invalid selector field range: " + rangeElement); //$NON-NLS-1$
            }
            ranges.add(new IntegerRange(start, end));
        }
        return ranges;
    }
}
