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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.childTypeError;

import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Type specifier list name parser (is it a bool? a string... )
 *
 * @author Matthew Khouzam
 *
 */
public final class TypeSpecifierListNameParser implements ICommonTreeParser {

    /**
     * Instance
     */
    public static final TypeSpecifierListNameParser INSTANCE = new TypeSpecifierListNameParser();

    private TypeSpecifierListNameParser() {
    }

    /**
     * Creates the string representation of a type specifier.
     *
     * @param typeSpecifier
     *            A TYPE_SPECIFIER node.
     *
     * @return param unused
     * @throws ParseException
     *             invalid node
     */
    @Override
    public StringBuilder parse(ICTFMetadataNode typeSpecifier, ICommonTreeParserParameter param) throws ParseException {
        StringBuilder sb = new StringBuilder();
        String type = typeSpecifier.getType();
        if (CTFParser.tokenNames[CTFParser.FLOATTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.INTTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.LONGTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.SHORTTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.SIGNEDTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.UNSIGNEDTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.CHARTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.DOUBLETOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.VOIDTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.BOOLTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.COMPLEXTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.IMAGINARYTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.CONSTTOK].equals(type) ||
                CTFParser.tokenNames[CTFParser.IDENTIFIER].equals(type)) {
            parseSimple(typeSpecifier, sb);

        } else if (CTFParser.tokenNames[CTFParser.STRUCT].equals(type)) {
            parseStruct(typeSpecifier, sb);

        } else if (CTFParser.tokenNames[CTFParser.VARIANT].equals(type)) {
            parseVariant(typeSpecifier, sb);

        } else if (CTFParser.tokenNames[CTFParser.ENUM].equals(type)) {
            parseEnum(typeSpecifier, sb);

        } else if (CTFParser.tokenNames[CTFParser.FLOATING_POINT].equals(type) || CTFParser.tokenNames[CTFParser.INTEGER].equals(type)
                || CTFParser.tokenNames[CTFParser.STRING].equals(type)) {
            throw new ParseException("CTF type found in createTypeSpecifierString"); //$NON-NLS-1$
        } else {
            throw childTypeError(typeSpecifier);
        }
        return sb;

    }

    private static void parseEnum(ICTFMetadataNode typeSpecifier, StringBuilder sb) throws ParseException {
        ICTFMetadataNode enumName = typeSpecifier.getFirstChildWithType(CTFParser.tokenNames[CTFParser.ENUM_NAME]);
        if (enumName == null) {
            throw new ParseException("nameless enum found in createTypeSpecifierString"); //$NON-NLS-1$
        }

        ICTFMetadataNode enumNameIdentifier = enumName.getChild(0);

        parseSimple(enumNameIdentifier, sb);
    }

    private static void parseVariant(ICTFMetadataNode typeSpecifier, StringBuilder sb) throws ParseException {
        ICTFMetadataNode variantName = typeSpecifier.getFirstChildWithType(CTFParser.tokenNames[CTFParser.VARIANT_NAME]);
        if (variantName == null) {
            throw new ParseException("nameless variant found in createTypeSpecifierString"); //$NON-NLS-1$
        }

        ICTFMetadataNode variantNameIdentifier = variantName.getChild(0);

        parseSimple(variantNameIdentifier, sb);
    }

    private static void parseSimple(ICTFMetadataNode typeSpecifier, StringBuilder sb) {
        sb.append(typeSpecifier.getText());
    }

    private static void parseStruct(ICTFMetadataNode typeSpecifier, StringBuilder sb) throws ParseException {
        ICTFMetadataNode structName = typeSpecifier.getFirstChildWithType(CTFParser.tokenNames[CTFParser.STRUCT_NAME]);
        if (structName == null) {
            throw new ParseException("nameless struct found in createTypeSpecifierString"); //$NON-NLS-1$
        }

        ICTFMetadataNode structNameIdentifier = structName.getChild(0);

        parseSimple(structNameIdentifier, sb);
    }

}
