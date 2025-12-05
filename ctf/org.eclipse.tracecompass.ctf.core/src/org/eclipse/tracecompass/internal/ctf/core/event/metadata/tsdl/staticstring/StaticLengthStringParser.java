/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.staticstring;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.types.StaticLengthStringDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A static-length string field class is an abstract string field class which
 * describes static-length string fields.
 *
 * A static-length string field is a sequence of zero or more contiguous encoded
 * Unicode codepoints. All the encoded codepoints of a static-length string
 * field before the first "NULL" (U+0000) codepoint, if any, form the resulting
 * string value. The first U+0000 codepoint, if any, and all the following bytes
 * are considered padding (garbage data).
 *
 * The length, or number of bytes, of a static-length string field is a
 * specified (already encoded/decoded) length field. A consumer can locate this
 * length field thanks to the length-field-location property of the
 * static-length string field class.
 *
 * @author Matthew Khouzam
 */
public final class StaticLengthStringParser extends AbstractScopedCommonTreeParser {

    /**
     * Instance
     */
    public static final StaticLengthStringParser INSTANCE = new StaticLengthStringParser();

    private StaticLengthStringParser() {
    }

    @Override
    public IDeclaration parse(ICTFMetadataNode node, ICommonTreeParserParameter param) throws ParseException {
        if (!(node instanceof JsonStructureFieldMemberMetadataNode)) {
            throw new ParseException("Static-length string only supported in JSON metadata"); //$NON-NLS-1$
        }

        JsonStructureFieldMemberMetadataNode member = (JsonStructureFieldMemberMetadataNode) node;
        JsonElement fieldClassElement = member.getFieldClass();
        if (fieldClassElement == null || !fieldClassElement.isJsonObject()) {
            throw new ParseException(getClass().getName() + " fieldclass must be a json object."); //$NON-NLS-1$
        }
        JsonObject fieldClass = fieldClassElement.getAsJsonObject();
        JsonElement lengthField = fieldClass.get(JsonMetadataStrings.LENGTH);
        if (lengthField == null) {
            throw new ParseException("static-length string requires length-field-location property"); //$NON-NLS-1$
        }
        JsonElement encodingField = fieldClass.get(JsonMetadataStrings.ENCODING);
        int length = lengthField.getAsInt();
        if (length > 1e6) {
            throw new ParseException("static-length string is over 1 million characters long, assuming an error."); //$NON-NLS-1$
        }
        Charset encoding = encodingField != null ? JsonMetadataStrings.ENCODINGS.getOrDefault(encodingField.getAsString(), StandardCharsets.UTF_8) : StandardCharsets.UTF_8;
        return new StaticLengthStringDeclaration(length, encoding);
    }

    /**
     * Parameters for the static-length string parser
     */
    @NonNullByDefault
    public static final class Param implements ICommonTreeParserParameter {
        private final CTFTrace fTrace;

        /**
         * Parameter constructor
         *
         * @param trace
         *            the trace
         */
        public Param(CTFTrace trace) {
            fTrace = trace;
        }

        /**
         * Get the trace
         *
         * @return the trace
         */
        public CTFTrace getTrace() {
            return fTrace;
        }
    }
}
