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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.bitmap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.event.types.BitArrayDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.BitMapDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerRange;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.bitarray.BitArrayDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * According to the documentation:
 * https://diamon.org/ctf/#fl-bm-fc
 *
 * A fixed-length bit map field class is a fixed-length bit array field class
 * which associates names to bits with its required flags property. Each flag
 * maps a name to an integer range set of bit indexes
 *
 * @author Arnaud Fiorini
 */
public final class BitMapDeclarationParser implements ICommonTreeParser {

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
    public static final BitMapDeclarationParser INSTANCE = new BitMapDeclarationParser();

    private BitMapDeclarationParser() {
    }

    @Override
    public @NonNull BitMapDeclaration parse(ICTFMetadataNode bitMap, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        CTFTrace trace = ((Param) param).fTrace;

        if (!(bitMap instanceof JsonStructureFieldMemberMetadataNode member)) {
            throw new ParseException("The fixed-length bit map field class is not supported in CTF 1.8."); //$NON-NLS-1$
        }
        /* A bit map is a bit array with flags */
        BitArrayDeclaration bitArray = BitArrayDeclarationParser.INSTANCE.parse(bitMap, new BitArrayDeclarationParser.Param(trace));
        JsonObject fieldClass = member.getFieldClass().getAsJsonObject();

        JsonElement flagsElement = fieldClass.get(JsonMetadataStrings.FLAGS);
        if (flagsElement == null || !flagsElement.isJsonObject() || flagsElement.getAsJsonObject().isEmpty()) {
            throw new ParseException("The flags property of a fixed-length bit map field class must be a non-empty JSON object."); //$NON-NLS-1$
        }

        Map<String, List<IntegerRange>> flags = new LinkedHashMap<>();
        for (Entry<String, JsonElement> flag : flagsElement.getAsJsonObject().entrySet()) {
            flags.put(flag.getKey(), parseBitRanges(flag.getKey(), flag.getValue(), bitArray.getLength()));
        }
        if (flags.isEmpty()) {
            throw new ParseException("The flags of a bit map field class must contain one or more properties."); //$NON-NLS-1$
        }

        return new BitMapDeclaration(bitArray.getLength(), bitArray.getByteOrder(), bitArray.getBitOrder(), bitArray.getAlignment(), flags);
    }

    private static List<IntegerRange> parseBitRanges(String flagName, JsonElement rangesElement, int length) throws ParseException {
        if (!rangesElement.isJsonArray() || rangesElement.getAsJsonArray().isEmpty()) {
            throw new ParseException("The bit ranges of flag " + flagName + " must be a non-empty JSON array."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        List<IntegerRange> ranges = new ArrayList<>();
        for (JsonElement rangeElement : rangesElement.getAsJsonArray()) {
            if (!rangeElement.isJsonArray() || rangeElement.getAsJsonArray().size() != 2) {
                throw new ParseException("Invalid bit range for flag " + flagName + ": " + rangeElement); //$NON-NLS-1$ //$NON-NLS-2$
            }
            JsonArray rangeArray = rangeElement.getAsJsonArray();
            long start = rangeArray.get(0).getAsLong();
            long end = rangeArray.get(1).getAsLong();
            if (start < 0 || start > end || end >= length) {
                throw new ParseException("Invalid bit range for flag " + flagName + ": " + rangeElement); //$NON-NLS-1$ //$NON-NLS-2$
            }
            ranges.add(new IntegerRange(start, end));
        }
        return ranges;
    }
}
