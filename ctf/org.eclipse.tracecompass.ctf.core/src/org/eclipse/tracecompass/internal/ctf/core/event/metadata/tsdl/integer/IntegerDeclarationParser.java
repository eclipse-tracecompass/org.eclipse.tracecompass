/*******************************************************************************
 * Copyright (c) 2015, 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.integer;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.childTypeError;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.isAnyUnaryString;

import java.nio.ByteOrder;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.event.types.Encoding;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.CtfCoreLoggerUtil;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.CTFAntlrMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.CTFJsonMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.MetadataStrings;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.AlignmentParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.ByteOrderParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.SizeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.string.EncodingParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

import com.google.gson.JsonObject;

/**
 * Signed integers are represented in two-complement. Integer alignment, size,
 * signedness and byte ordering are defined in the TSDL metadata. Integers
 * aligned on byte size (8-bit) and with length multiple of byte size (8-bit)
 * correspond to the C99 standard integers. In addition, integers with alignment
 * and/or size that are not a multiple of the byte size are permitted; these
 * correspond to the C99 standard bitfields, with the added specification that
 * the CTF integer bitfields have a fixed binary representation. Integer size
 * needs to be a positive integer. Integers of size 0 are forbidden. An
 * MIT-licensed reference implementation of the CTF portable bitfields is
 * available here.
 *
 * Binary representation of integers:
 * <ul>
 * <li>On little and big endian: Within a byte, high bits correspond to an
 * integer high bits, and low bits correspond to low bits</li>
 * <li>On little endian: Integer across multiple bytes are placed from the less
 * significant to the most significant Consecutive integers are placed from
 * lower bits to higher bits (even within a byte)</li>
 * <li>On big endian: Integer across multiple bytes are placed from the most
 * significant to the less significant Consecutive integers are placed from
 * higher bits to lower bits (even within a byte)</li>
 * </ul>
 *
 * This binary representation is derived from the bitfield implementation in GCC
 * for little and big endian. However, contrary to what GCC does, integers can
 * cross units boundaries (no padding is required). Padding can be explicitly
 * added to follow the GCC layout if needed.
 *
 * @author Matthew Khouzam
 * @author Efficios - javadoc preamble
 *
 */
public final class IntegerDeclarationParser implements ICommonTreeParser {

    /**
     * Parameter Object with a trace
     *
     * @author Matthew Khouzam
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
    public static final IntegerDeclarationParser INSTANCE = new IntegerDeclarationParser();

    private static final @NonNull String ENCODING = "encoding"; //$NON-NLS-1$
    private static final @NonNull String EMPTY_STRING = ""; //$NON-NLS-1$
    private static final int DEFAULT_INT_BASE = 10;
    private static final @NonNull String MAP = "map"; //$NON-NLS-1$
    private static final @NonNull String BASE = "base"; //$NON-NLS-1$
    private static final @NonNull String PREFERRED_BASE = "preferred-display-base"; //$NON-NLS-1$
    private static final @NonNull String BYTE_ORDER = "byte-order"; //$NON-NLS-1$
    private static final @NonNull String SIZE = "size"; //$NON-NLS-1$
    private static final @NonNull String LENGTH = "length"; //$NON-NLS-1$
    private static final @NonNull String SIGNED = "signed"; //$NON-NLS-1$
    private static final @NonNull String ALIGNMENT = "alignment"; //$NON-NLS-1$
    private static final @NonNull String VARINT = "varint"; //$NON-NLS-1$

    private IntegerDeclarationParser() {
    }

    /**
     * Parses an integer declaration node.
     *
     * @param parameter
     *            parent trace, for byte orders
     *
     * @return The corresponding integer declaration.
     */
    @Override
    public IntegerDeclaration parse(ICTFMetadataNode integer, ICommonTreeParserParameter parameter) throws ParseException {
        if (!(parameter instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        CTFTrace trace = ((Param) parameter).fTrace;

        /* The return value */
        boolean signed = false;
        boolean varint = false;
        ByteOrder byteOrder = trace.getByteOrder();
        long size = 0;
        long alignment = 0;
        int base = DEFAULT_INT_BASE;
        String role = null;
        @NonNull
        String clock = EMPTY_STRING;

        Encoding encoding = Encoding.NONE;

        if (integer instanceof JsonStructureFieldMemberMetadataNode) {
            JsonStructureFieldMemberMetadataNode member = (JsonStructureFieldMemberMetadataNode) integer;
            JsonObject fieldclass = member.getFieldClass().getAsJsonObject();
            role = member.getRole();
            // by default fieldclass is unsigned
            if (fieldclass.has(SIGNED)) {
                signed = fieldclass.get(SIGNED).getAsBoolean();
            }
            if (fieldclass.has(PREFERRED_BASE)) {
                base = fieldclass.get(PREFERRED_BASE).getAsInt();
            }
            if (fieldclass.has(VARINT)) {
                varint = fieldclass.get(VARINT).getAsBoolean();
            }
            if (varint) {
                return IntegerDeclaration.createVarintDeclaration(signed, base, role, true);
            }
            if (fieldclass.has(ALIGNMENT)) {
                alignment = fieldclass.get(ALIGNMENT).getAsInt();
            }
            size = fieldclass.get(LENGTH).getAsInt();

            CTFJsonMetadataNode bo = new CTFJsonMetadataNode(integer, CTFParser.tokenNames[CTFParser.UNARY_EXPRESSION_STRING], fieldclass.get(BYTE_ORDER).getAsString());
            byteOrder = ByteOrderParser.INSTANCE.parse(bo, new ByteOrderParser.Param(trace));

        } else if (integer instanceof CTFAntlrMetadataNode) {
            List<ICTFMetadataNode> children = integer.getChildren();
            /*
             * If the integer has no attributes, then it is missing the size
             * attribute which is required
             */
            if (children == null) {
                throw new ParseException("integer: missing size attribute"); //$NON-NLS-1$
            }

            /* Iterate on all integer children */
            for (ICTFMetadataNode child : children) {
                String type = child.getType();
                if (CTFParser.tokenNames[CTFParser.CTF_EXPRESSION_VAL].equals(type)) {
                    ICTFMetadataNode leftNode = child.getChild(0);
                    ICTFMetadataNode rightNode = child.getChild(1);
                    List<ICTFMetadataNode> leftStrings = leftNode.getChildren();
                    if (!isAnyUnaryString(leftStrings.get(0))) {
                        throw new ParseException("Left side of ctf expression must be a string"); //$NON-NLS-1$
                    }
                    String left = concatenateUnaryStrings(leftStrings);
                    switch (left) {
                    case SIGNED:
                        signed = SignedParser.INSTANCE.parse(rightNode, null);
                        break;
                    case MetadataStrings.BYTE_ORDER:
                        byteOrder = ByteOrderParser.INSTANCE.parse(rightNode, new ByteOrderParser.Param(trace));
                        break;
                    case SIZE:
                        size = SizeParser.INSTANCE.parse(rightNode, null);
                        break;
                    case MetadataStrings.ALIGN:
                        alignment = AlignmentParser.INSTANCE.parse(rightNode, null);
                        break;
                    case BASE:
                        base = BaseParser.INSTANCE.parse(rightNode, null);
                        break;
                    case ENCODING:
                        encoding = EncodingParser.INSTANCE.parse(rightNode, null);
                        break;
                    case MAP:
                        clock = ClockMapParser.INSTANCE.parse(rightNode, null);
                        break;
                    default:
                        CtfCoreLoggerUtil.logWarning("Unknown integer attribute: " + left); //$NON-NLS-1$
                        break;
                    }
                } else {
                    throw childTypeError(child);
                }
            }
        }

        if (size <= 0) {
            throw new ParseException("Invalid size attribute in Integer: " + size); //$NON-NLS-1$
        }

        if (alignment == 0) {
            alignment = 1;
        }

        return IntegerDeclaration.createDeclaration((int) size, signed, base,
                byteOrder, encoding, clock, alignment, role);
    }
}
