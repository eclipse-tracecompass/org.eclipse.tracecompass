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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.variant;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.VariantDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeAliasParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeDeclaratorParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeSpecifierListParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * This parses the (sub)declarations located IN a variant declaration.
 *
 * @author Matthew Khouzam
 */
public final class VariantDeclarationParser extends AbstractScopedCommonTreeParser {

    /**
     * Parameter Object
     *
     * @author Matthew Khouzam
     *
     */
    @NonNullByDefault
    public static final class Param implements ICommonTreeParserParameter {
        private final VariantDeclaration fVariant;
        private final DeclarationScope fDeclarationScope;
        private final CTFTrace fTrace;

        /**
         * Parameter Object Contructor
         *
         * @param variant
         *            variant declaration to populate
         * @param trace
         *            trace
         * @param scope
         *            current scope
         */
        public Param(VariantDeclaration variant, CTFTrace trace, DeclarationScope scope) {
            fVariant = variant;
            fTrace = trace;
            fDeclarationScope = scope;
        }
    }

    /**
     * Instance
     */
    public static final VariantDeclarationParser INSTANCE = new VariantDeclarationParser();

    private VariantDeclarationParser() {
    }

    /**
     * Parses the variant declaration and gets a {@link VariantDeclaration}
     * back.
     *
     * @param declaration
     *            the variant declaration AST node
     * @param param
     *            the {@link Param} parameter object
     * @return the {@link VariantDeclaration}
     * @throws ParseException
     *             if the AST is malformed
     */
    @Override
    public VariantDeclaration parse(ICTFMetadataNode declaration, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        VariantDeclaration variant = ((Param) param).fVariant;
        final DeclarationScope scope = ((Param) param).fDeclarationScope;
        CTFTrace trace = ((Param) param).fTrace;

        if (declaration instanceof JsonStructureFieldMetadataNode) {
            List<ICTFMetadataNode> children = declaration.getChildren();
            if (children.isEmpty()) {
                throw new ParseException("Cannot have a variant with no fields"); //$NON-NLS-1$
            }
            for (ICTFMetadataNode child : children) {
                JsonStructureFieldMemberMetadataNode member = (JsonStructureFieldMemberMetadataNode) child;
                JsonElement fieldClass = member.getFieldClass();
                String name = member.getName();
                IDeclaration decl;
                if (fieldClass.isJsonObject()) {
                    String type = fieldClass.getAsJsonObject().get(JsonMetadataStrings.TYPE).getAsString();
                    if (JsonMetadataStrings.STRUCTURE.equals(type)) {
                        JsonStructureFieldMetadataNode structureFieldClass = Objects.requireNonNull(new Gson().fromJson(fieldClass.getAsJsonObject(), JsonStructureFieldMetadataNode.class));
                        try {
                            structureFieldClass.initialize();
                        } catch (CTFException e) {
                            throw new ParseException("Variant declaration does not match CTF2 Json format"); //$NON-NLS-1$
                        }
                        decl = TypeSpecifierListParser.INSTANCE.parse(structureFieldClass, new TypeSpecifierListParser.Param(trace, null, null, scope));
                    } else {
                        decl = TypeAliasParser.INSTANCE.parse(member, new TypeAliasParser.Param(trace, scope));
                    }
                } else {
                    // TODO: Cover else-case where field class is JsonString for
                    // field class alias
                    throw new ParseException("Field class aliases are not yet supported by trace compass"); //$NON-NLS-1$
                }
                addVariantField(variant, scope, decl, name);
            }
        } else {
            /* Get the type specifier list node */
            ICTFMetadataNode typeSpecifierListNode = declaration.getFirstChildWithType(CTFParser.tokenNames[CTFParser.TYPE_SPECIFIER_LIST]);
            if (typeSpecifierListNode == null) {
                throw new ParseException("Variant need type specifiers"); //$NON-NLS-1$
            }

            /* Get the type declarator list node */
            ICTFMetadataNode typeDeclaratorListNode = declaration.getFirstChildWithType(CTFParser.tokenNames[CTFParser.TYPE_DECLARATOR_LIST]);
            if (typeDeclaratorListNode == null) {
                throw new ParseException("Cannot have empty variant"); //$NON-NLS-1$
            }
            /* Get the type declarator list */
            List<ICTFMetadataNode> typeDeclaratorList = typeDeclaratorListNode.getChildren();

            /*
             * For each type declarator, parse the declaration and add a field
             * to the variant
             */
            for (ICTFMetadataNode typeDeclaratorNode : typeDeclaratorList) {

                StringBuilder identifierSB = new StringBuilder();
                IDeclaration decl = TypeDeclaratorParser.INSTANCE.parse(typeDeclaratorNode,
                        new TypeDeclaratorParser.Param(trace, typeSpecifierListNode, scope, identifierSB));
                String name = identifierSB.toString();

                addVariantField(variant, scope, decl, name);
            }
        }
        return variant;
    }

    private static void addVariantField(VariantDeclaration variant, DeclarationScope scope, IDeclaration decl, String name) throws ParseException {
        if (variant.hasField(name)) {
            throw new ParseException("variant: duplicate field " //$NON-NLS-1$
                    + name);
        }
        scope.registerIdentifier(name, decl);
        variant.addField(name, decl);
    }
}
