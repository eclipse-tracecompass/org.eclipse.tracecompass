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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.VariantDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonFieldClassAliasMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.enumeration.EnumParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.integer.IntegerDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.string.StringDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.variant.VariantParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * The "typealias" declaration can be used to give a name (including pointer
 * declarator specifier) to a type. It should also be used to map basic C types
 * (float, int, unsigned long, ...) to a CTF type. Typealias is a superset of
 * "typedef": it also allows assignment of a simple variable identifier to a
 * type.
 *
 * @author Matthew Khouzam - Inital API and implementation
 * @author Efficios - Documentation
 *
 */
public final class TypeAliasParser extends AbstractScopedCommonTreeParser {

    private static final String SIGNED = "signed"; //$NON-NLS-1$
    private static final String VARINT = "varint"; //$NON-NLS-1$

    /**
     * Parameters for the typealias parser
     *
     * @author Matthew Khouzam
     *
     */
    @NonNullByDefault
    public static final class Param implements ICommonTreeParserParameter {
        private final DeclarationScope fDeclarationScope;
        private final CTFTrace fTrace;

        /**
         * Parameter constructor
         *
         * @param trace
         *            the trace
         * @param scope
         *            the scope
         */
        public Param(CTFTrace trace, DeclarationScope scope) {
            fTrace = trace;
            fDeclarationScope = scope;
        }
    }

    /**
     * Instance
     */
    public static final TypeAliasParser INSTANCE = new TypeAliasParser();

    private TypeAliasParser() {
    }

    @Override
    public IDeclaration parse(ICTFMetadataNode typealias, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        DeclarationScope scope = ((Param) param).fDeclarationScope;

        List<ICTFMetadataNode> children = typealias.getChildren();

        ICTFMetadataNode target = null;
        ICTFMetadataNode alias = null;
        IDeclaration targetDeclaration = null;
        CTFTrace trace = ((Param) param).fTrace;

        String aliasString = null;
        if (typealias instanceof JsonStructureFieldMemberMetadataNode) {
            JsonStructureFieldMemberMetadataNode member = ((JsonStructureFieldMemberMetadataNode) typealias);
            String type = typealias.getType();
            JsonObject fieldClass = null;
            if (member.getFieldClass().isJsonObject()) {
                fieldClass = member.getFieldClass().getAsJsonObject();
                aliasString = member.getName();
                type = member.getType();
            } else if (member.getFieldClass().isJsonPrimitive()) {
                JsonPrimitive jPrimitive = member.getFieldClass().getAsJsonPrimitive();
                if (jPrimitive.isString()) {
                    String fieldClassAlias = jPrimitive.getAsString();
                    ICTFMetadataNode root = member;
                    while (root.getParent() != null) {
                        root = root.getParent();
                    }
                    for (ICTFMetadataNode node : root.getChildren()) {
                        if (node instanceof JsonFieldClassAliasMetadataNode) {
                            JsonFieldClassAliasMetadataNode aliasMetadataNode = (JsonFieldClassAliasMetadataNode) node;
                            if (aliasMetadataNode.getName().equals(fieldClassAlias)) {
                                fieldClass = aliasMetadataNode.getFieldClass();
                                aliasString = aliasMetadataNode.getName();
                                JsonElement typeMember = fieldClass.get(JsonMetadataStrings.TYPE);
                                if (typeMember != null && typeMember.isJsonPrimitive()) {
                                    type = typeMember.getAsString();
                                }
                                break;
                            }
                        }
                    }
                    if (fieldClass == null) {
                        throw new ParseException("no previously occurring field class alias named '" + fieldClassAlias + '\''); //$NON-NLS-1$
                    }
                }
            }
            if (fieldClass != null) {
                if (JsonMetadataStrings.FIXED_UNSIGNED_INTEGER_FIELD.equals(type)) {
                    fieldClass.addProperty(SIGNED, false);
                    fieldClass.addProperty(VARINT, false);
                    targetDeclaration = IntegerDeclarationParser.INSTANCE.parse(typealias, new IntegerDeclarationParser.Param(trace));
                } else if (JsonMetadataStrings.FIXED_SIGNED_INTEGER_FIELD.equals(type)) {
                    fieldClass.addProperty(SIGNED, true);
                    fieldClass.addProperty(VARINT, false);
                    targetDeclaration = IntegerDeclarationParser.INSTANCE.parse(typealias, new IntegerDeclarationParser.Param(trace));
                } else if (JsonMetadataStrings.VARIABLE_UNSIGNED_INTEGER_FIELD.equals(type)) {
                    fieldClass.addProperty(SIGNED, false);
                    fieldClass.addProperty(VARINT, true);
                    targetDeclaration = IntegerDeclarationParser.INSTANCE.parse(typealias, new IntegerDeclarationParser.Param(trace));
                } else if (JsonMetadataStrings.VARIABLE_SIGNED_INTEGER_FIELD.equals(type)) {
                    fieldClass.addProperty(SIGNED, true);
                    fieldClass.addProperty(VARINT, true);
                    targetDeclaration = IntegerDeclarationParser.INSTANCE.parse(typealias, new IntegerDeclarationParser.Param(trace));
                } else if (JsonMetadataStrings.STATIC_LENGTH_BLOB.equals(type)) {
                    targetDeclaration = BlobDeclarationParser.INSTANCE.parse(typealias, null);
                } else if (JsonMetadataStrings.NULL_TERMINATED_STRING.equals(type)) {
                    targetDeclaration = StringDeclarationParser.INSTANCE.parse(typealias, null);
                } else if (JsonMetadataStrings.VARIANT.equals(type)) {
                    targetDeclaration = VariantParser.INSTANCE.parse(typealias, new VariantParser.Param(trace, scope));
                } else if (JsonMetadataStrings.FIXED_UNSIGNED_ENUMERATION.equals(type)) {
                    targetDeclaration = EnumParser.INSTANCE.parse(typealias, new EnumParser.Param(trace, scope));
                } else {
                    throw new ParseException("Invalid field class: " + type); //$NON-NLS-1$
                }
            } else {
                throw new ParseException("field-class property is not a JSON object or JSON string"); //$NON-NLS-1$
            }
        } else {
            for (ICTFMetadataNode child : children) {
                String type = child.getType();
                if (CTFParser.tokenNames[CTFParser.TYPEALIAS_TARGET].equals(type)) {
                    target = child;
                } else if (CTFParser.tokenNames[CTFParser.TYPEALIAS_ALIAS].equals(type)) {
                    alias = child;
                } else {
                    throw childTypeError(child);
                }
            }

            targetDeclaration = TypeAliasTargetParser.INSTANCE.parse(target, new TypeAliasTargetParser.Param(trace, scope));

            if ((targetDeclaration instanceof VariantDeclaration)
                    && ((VariantDeclaration) targetDeclaration).isTagged()) {
                throw new ParseException("Typealias of untagged variant is not permitted"); //$NON-NLS-1$
            }

            aliasString = TypeAliasAliasParser.INSTANCE.parse(alias, null);
        }

        scope.registerType(aliasString, targetDeclaration);
        return targetDeclaration;
    }

}
