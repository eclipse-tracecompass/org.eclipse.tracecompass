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

package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.enumeration;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.childTypeError;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.isAnyUnaryString;

import java.math.BigInteger;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.event.types.EnumDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.CtfCoreLoggerUtil;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.UnaryIntegerParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.UnaryStringParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The parser for individual enumerators within an enum body
 *
 * @author Matthew Khouzam - Initial API and implementation
 *
 */
public final class EnumeratorParser implements ICommonTreeParser {

    private static final String MAPPINGS = "mappings"; //$NON-NLS-1$

    /**
     * A parameter containing an enum declaration
     *
     * @author Matthew Khouzam
     *
     */
    @NonNullByDefault
    public static final class Param implements ICommonTreeParserParameter {
        private final EnumDeclaration fEnumDeclaration;

        /**
         * Constructor
         *
         * @param enumDeclaration
         *            the enum declaration to populate
         */
        public Param(EnumDeclaration enumDeclaration) {
            fEnumDeclaration = enumDeclaration;
        }
    }

    /**
     * Instance
     */
    public static final EnumeratorParser INSTANCE = new EnumeratorParser();

    private EnumeratorParser() {
    }

    /**
     * Parses an enumerator node and adds an enumerator declaration to an
     * enumeration declaration.
     *
     * The high value of the range of the last enumerator is needed in case the
     * current enumerator does not specify its value.
     *
     * @param enumerator
     *            An ENUM_ENUMERATOR node.
     * @param param
     *            an enumeration declaration to which will be added the
     *            enumerator.
     * @return The high value of the value range of the current enumerator.
     * @throws ParseException
     *             if the element failed to add
     */
    @Override
    public Long parse(ICTFMetadataNode enumerator, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        //use this to parse numbers
        EnumDeclaration enumDeclaration = ((Param) param).fEnumDeclaration;

        long low = 0;
        long high = 0;
        boolean valueSpecified = false;
        String label = null;

        if (enumerator instanceof JsonStructureFieldMemberMetadataNode) {
            JsonStructureFieldMemberMetadataNode jsonEnum = (JsonStructureFieldMemberMetadataNode) enumerator;
            if (jsonEnum.getFieldClass().isJsonObject()) {
                JsonObject fieldClass = jsonEnum.getFieldClass().getAsJsonObject();
                if (fieldClass.has(MAPPINGS)) {
                    JsonObject mappings = fieldClass.get(MAPPINGS).getAsJsonObject();
                    Set<Entry<String, JsonElement>> enumerations = mappings.entrySet();
                    for (Entry<String, JsonElement> enumeration : enumerations) {
                        label = enumeration.getKey();
                        JsonArray range = enumeration.getValue().getAsJsonArray().get(0).getAsJsonArray();
                        low = range.get(0).getAsLong();
                        high = range.get(1).getAsLong();
                        setEnumeration(enumDeclaration, low, high, true, label);
                    }
                }
            }
        } else {
            List<ICTFMetadataNode> children = enumerator.getChildren();
            for (ICTFMetadataNode child : children) {
                if (isAnyUnaryString(child)) {
                    label = UnaryStringParser.INSTANCE.parse(child, null);
                } else if (CTFParser.tokenNames[CTFParser.ENUM_VALUE].equals(child.getType())) {

                    valueSpecified = true;

                    low = UnaryIntegerParser.INSTANCE.parse(child.getChild(0), null);
                    high = low;
                } else if (CTFParser.tokenNames[CTFParser.ENUM_VALUE_RANGE].equals(child.getType())) {

                    valueSpecified = true;

                    low = UnaryIntegerParser.INSTANCE.parse(child.getChild(0), null);
                    high = UnaryIntegerParser.INSTANCE.parse(child.getChild(1), null);
                } else {
                    throw childTypeError(child);
                }
            }
            setEnumeration(enumDeclaration, low, high, valueSpecified, label);
        }
        return high;
    }

    private static void setEnumeration(EnumDeclaration enumDeclaration, long low, long high, boolean valueSpecified, String label) throws ParseException {
        if (low > high) {
            throw new ParseException("enum low value greater than high value"); //$NON-NLS-1$
        }
        if (valueSpecified && !enumDeclaration.add(low, high, label)) {
            CtfCoreLoggerUtil.logWarning("enum declarator values overlap. " + enumDeclaration.getLabels() + " and " + label); //$NON-NLS-1$ //$NON-NLS-2$
        } else if (!valueSpecified && !enumDeclaration.add(label)) {
            throw new ParseException("enum cannot add element " + label); //$NON-NLS-1$
        }
        IntegerDeclaration enumContainerType = enumDeclaration.getContainerType();
        if (valueSpecified && (BigInteger.valueOf(low).compareTo(enumContainerType.getMinValue()) < 0 ||
                BigInteger.valueOf(high).compareTo(enumContainerType.getMaxValue()) > 0)) {
            throw new ParseException(String.format("enum value ( %d - %d ) is not in range ( %d - %d )", low, high, enumContainerType.getMinValue(), enumContainerType.getMinValue())); //$NON-NLS-1$
        }
    }

}
