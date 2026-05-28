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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.bool;

import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.event.types.BooleanDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.CTFJsonMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.AlignmentParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.ByteOrderParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * According to the documentation:
 * https://diamon.org/ctf/files/CTF2-SPECRC-7.0rA.html#fl-bool-fc
 *
 * A fixed length boolean field class is a fixed length bit array where if all
 * the bits are cleared (zero) then the value is false else it is true.
 *
 * The length, or number of bits, of a boolean is specified as a property.
 *
 * @author Arnaud Fiorini
 */
public class BooleanDeclarationParser implements ICommonTreeParser {

    /**
     * Parameter object with only a trace in it
     */
    @NonNullByDefault
    public static final class Param implements ICommonTreeParserParameter {
        private final CTFTrace fTrace;

        /**
         * Constructor
         *
         * @param trace
         *            the trace
         */
        public Param(CTFTrace trace) {
            fTrace = trace;
        }
    }

    /**
     * Instance
     */
    public static final BooleanDeclarationParser INSTANCE = new BooleanDeclarationParser();

    private BooleanDeclarationParser() {
    }

    @Override
    public @NonNull BooleanDeclaration parse(ICTFMetadataNode bool, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        CTFTrace trace = ((Param) param).fTrace;

        /* The return value */
        ByteOrder byteOrder = trace.getByteOrder();
        long alignment = 0;
        int size = 0;

        if (bool instanceof JsonStructureFieldMemberMetadataNode member) {
            JsonElement fieldClassElement = member.getFieldClass();
            if (fieldClassElement == null || !fieldClassElement.isJsonObject()) {
                throw new ParseException(getClass().getName() + " fieldclass must be a json object."); //$NON-NLS-1$
            }
            JsonObject fieldclass = member.getFieldClass().getAsJsonObject();
            if (fieldclass.has(JsonMetadataStrings.LENGTH)) {
                size = fieldclass.get(JsonMetadataStrings.LENGTH).getAsInt();

                if (size <= 0) {
                    throw new ParseException("Unsupported boolean size: " + size); //$NON-NLS-1$
                }
            }

            if (fieldclass.has(JsonMetadataStrings.BYTE_ORDER)) {
                CTFJsonMetadataNode bo = new CTFJsonMetadataNode(bool, CTFParser.tokenNames[CTFParser.UNARY_EXPRESSION_STRING], fieldclass.get(JsonMetadataStrings.BYTE_ORDER).getAsString());
                byteOrder = ByteOrderParser.INSTANCE.parse(bo, new ByteOrderParser.Param(trace));
            }

            if (fieldclass.has(JsonMetadataStrings.ALIGNMENT)) {
                alignment = AlignmentParser.INSTANCE.parse(bool, null);
            }
        } else {
            throw new ParseException("The fixed-length boolean field class is not supported in CTF 1.8."); //$NON-NLS-1$
        }

        if (size == 0) {
            throw new ParseException("The size is a required property for the fixed-length boolean field class."); //$NON-NLS-1$
        }

        if (alignment == 0) {
            alignment = 1;
        }

        return new BooleanDeclaration(size, byteOrder, alignment);
    }

}
