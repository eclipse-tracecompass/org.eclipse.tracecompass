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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.bitarray;

import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.event.types.BitArrayDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.BitOrder;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.CTFJsonMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.AlignmentParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.BitOrderParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.ByteOrderParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * According to the documentation:
 * https://diamon.org/ctf/#fl-ba-fc
 *
 * A fixed-length bit array field class describes fixed-length bit array
 * fields, which are simple arrays of contiguous bits without any integer value
 * meaning.
 *
 * The length, byte order are required properties. The bit order is optional
 * and defaults to the natural bit order of the byte order.
 *
 * @author Arnaud Fiorini
 */
public final class BitArrayDeclarationParser implements ICommonTreeParser {

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
    public static final BitArrayDeclarationParser INSTANCE = new BitArrayDeclarationParser();

    private BitArrayDeclarationParser() {
    }

    @Override
    public @NonNull BitArrayDeclaration parse(ICTFMetadataNode bitArray, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        CTFTrace trace = ((Param) param).fTrace;

        long alignment = 1;
        int length = 0;

        if (!(bitArray instanceof JsonStructureFieldMemberMetadataNode member)) {
            throw new ParseException("The fixed-length bit array field class is not supported in CTF 1.8."); //$NON-NLS-1$
        }
        JsonElement fieldClassElement = member.getFieldClass();
        if (fieldClassElement == null || !fieldClassElement.isJsonObject()) {
            throw new ParseException(getClass().getName() + " fieldclass must be a json object."); //$NON-NLS-1$
        }
        JsonObject fieldClass = fieldClassElement.getAsJsonObject();

        if (!fieldClass.has(JsonMetadataStrings.LENGTH)) {
            throw new ParseException("The length is a required property for the fixed-length bit array field class."); //$NON-NLS-1$
        }
        length = fieldClass.get(JsonMetadataStrings.LENGTH).getAsInt();
        if (length <= 0) {
            throw new ParseException("Unsupported fixed-length bit array length: " + length); //$NON-NLS-1$
        }
        if (!fieldClass.has(JsonMetadataStrings.BYTE_ORDER)) {
            throw new ParseException("The byte order is a required property for the fixed-length bit array field class."); //$NON-NLS-1$
        }
        CTFJsonMetadataNode bo = new CTFJsonMetadataNode(bitArray, CTFParser.tokenNames[CTFParser.UNARY_EXPRESSION_STRING], fieldClass.get(JsonMetadataStrings.BYTE_ORDER).getAsString());
        ByteOrder byteOrder = ByteOrderParser.INSTANCE.parse(bo, new ByteOrderParser.Param(trace));

        BitOrder bitOrder = BitOrder.fromByteOrder(byteOrder);
        if (fieldClass.has(JsonMetadataStrings.BIT_ORDER)) {
            CTFJsonMetadataNode bitOrderNode = new CTFJsonMetadataNode(bitArray, CTFParser.tokenNames[CTFParser.UNARY_EXPRESSION_STRING], fieldClass.get(JsonMetadataStrings.BIT_ORDER).getAsString());
            bitOrder = BitOrderParser.INSTANCE.parse(bitOrderNode, null);
        }

        if (fieldClass.has(JsonMetadataStrings.ALIGNMENT)) {
            alignment = AlignmentParser.INSTANCE.parse(bitArray, null);
        }

        return new BitArrayDeclaration(length, byteOrder, bitOrder, alignment);
    }
}
