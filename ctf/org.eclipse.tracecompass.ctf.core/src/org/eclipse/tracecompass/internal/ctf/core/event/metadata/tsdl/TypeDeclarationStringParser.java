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

import java.util.List;

import org.eclipse.tracecompass.internal.ctf.core.event.metadata.CTFAntlrMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Type declaration String parser
 *
 * @author Matthew Khouzam
 *
 */
public final class TypeDeclarationStringParser implements ICommonTreeParser {

    /**
     * Parameter Object with a list of common trees
     *
     * @author Matthew Khouzam
     *
     */
    public static final class Param implements ICommonTreeParserParameter {
        private final List<ICTFMetadataNode> fList;

        /**
         * Constructor
         *
         * @param list
         *            List of trees
         */
        public Param(List<ICTFMetadataNode> list) {
            fList = list;
        }
    }

    /**
     * Instance
     */
    public static final TypeDeclarationStringParser INSTANCE = new TypeDeclarationStringParser();

    private TypeDeclarationStringParser() {
    }

    /**
     * Creates the string representation of a type specifier.
     *
     * @param typeSpecifierList
     *            A TYPE_SPECIFIER node.
     *
     * @return A StringBuilder to which will be appended the string.
     * @throws ParseException
     *             invalid node
     */
    @Override
    public String parse(ICTFMetadataNode typeSpecifierList, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        List<ICTFMetadataNode> pointers = ((Param) param).fList;
        StringBuilder sb = new StringBuilder();
        sb.append(TypeSpecifierListStringParser.INSTANCE.parse(typeSpecifierList, null));
        if (pointers != null) {
            ICTFMetadataNode temp = new CTFAntlrMetadataNode(null, 0, null);
            for (ICTFMetadataNode pointer : pointers) {
                temp.addChild(pointer);
            }
            sb.append(PointerListStringParser.INSTANCE.parse(temp, null));
        }
        return sb.toString();
    }

}
