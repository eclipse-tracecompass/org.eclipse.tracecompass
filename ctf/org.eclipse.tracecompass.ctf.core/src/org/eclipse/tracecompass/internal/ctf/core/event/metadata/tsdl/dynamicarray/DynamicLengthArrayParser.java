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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.dynamicarray;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeAliasParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.types.SequenceDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A dynamic-length array field class describes dynamic-length array fields.
 *
 * @author Matthew Khouzam
 */
public final class DynamicLengthArrayParser extends AbstractScopedCommonTreeParser {

    /**
     * Instance
     */
    public static final DynamicLengthArrayParser INSTANCE = new DynamicLengthArrayParser();

    private DynamicLengthArrayParser() {
    }

    @Override
    public IDeclaration parse(ICTFMetadataNode node, ICommonTreeParserParameter param) throws ParseException {
        if (!(node instanceof JsonStructureFieldMemberMetadataNode)) {
            throw new ParseException("Dynamic-length array only supported in JSON metadata"); //$NON-NLS-1$
        }
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }

        JsonStructureFieldMemberMetadataNode member = (JsonStructureFieldMemberMetadataNode) node;
        JsonObject fieldClass = member.getFieldClass().getAsJsonObject();

        JsonElement lengthFieldLocation = fieldClass.get(JsonMetadataStrings.LENGTH_FIELD_LOCATION);
        if (lengthFieldLocation == null) {
            throw new ParseException("Dynamic-length array requires length-field-location property"); //$NON-NLS-1$
        }

        JsonElement elementFieldClass = fieldClass.get(JsonMetadataStrings.ELEMENT_FIELD_CLASS);
        if (elementFieldClass == null) {
            throw new ParseException("Dynamic-length array requires element-field-class property"); //$NON-NLS-1$
        }

        JsonElement pathElement = lengthFieldLocation.getAsJsonObject().get(JsonMetadataStrings.PATH);
        String lengthName = pathElement.isJsonArray() ? 
            pathElement.getAsJsonArray().get(pathElement.getAsJsonArray().size() - 1).getAsString() :
            pathElement.getAsString();
        CTFTrace trace = ((Param) param).getTrace();
        DeclarationScope scope = ((Param) param).getScope();

        JsonStructureFieldMemberMetadataNode elementNode = new JsonStructureFieldMemberMetadataNode(node, "", "", "", elementFieldClass.getAsJsonObject()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        IDeclaration elementDeclaration = TypeAliasParser.INSTANCE.parse(elementNode, new TypeAliasParser.Param(trace, scope));

        return new SequenceDeclaration(lengthName, elementDeclaration);
    }

    /**
     * Parameters for the dynamic-length array parser
     */
    @NonNullByDefault
    public static final class Param implements ICommonTreeParserParameter {
        private final CTFTrace fTrace;
        private final DeclarationScope fScope;

        /**
         * Parameter constructor
         *
         * @param trace the trace
         * @param scope the declaration scope
         */
        public Param(CTFTrace trace, DeclarationScope scope) {
            fTrace = trace;
            fScope = scope;
        }

        /**
         * Get the trace
         *
         * @return the trace
         */
        public CTFTrace getTrace() {
            return fTrace;
        }

        /**
         * Get the scope
         *
         * @return the scope
         */
        public DeclarationScope getScope() {
            return fScope;
        }
    }
}
