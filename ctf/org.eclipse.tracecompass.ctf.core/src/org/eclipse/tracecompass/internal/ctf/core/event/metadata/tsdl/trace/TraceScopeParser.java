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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.trace;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.concatenateUnaryStrings;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.stream.StreamScopeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * TSDL uses three different types of scoping: a lexical scope is used for
 * declarations and type definitions, and static and dynamic scopes are used for
 * variants references to tag fields (with relative and absolute path lookups)
 * and for sequence references to length fields.
 *
 * @author Matthew Khouzam
 * @author Efficios - Description
 *
 */
public final class TraceScopeParser implements ICommonTreeParser {

    /**
     * Parameter object
     *
     * @author Matthew Khouzam
     *
     */
    @NonNullByDefault
    public static final class Param implements ICommonTreeParserParameter {
        private final List<ICTFMetadataNode> fList;

        /**
         * Parameter object constructor
         *
         * @param list
         *            the list of subtrees
         */
        public Param(List<ICTFMetadataNode> list) {
            fList = list;

        }

    }

    /**
     * Instance
     */
    public static final TraceScopeParser INSTANCE = new TraceScopeParser();

    private TraceScopeParser() {
    }

    /**
     * Parse a trace scope to get a concatenated scope name. Like
     * "trace.version.minor"
     *
     * @param unused
     *            unused
     * @param param
     *            a {@link Param} containing the list of ASTs to make the scope
     * @return a {@link String} of the scope
     * @throws ParseException
     *             an AST was malformed
     *
     */
    @Override
    public String parse(ICTFMetadataNode unused, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        List<@NonNull ICTFMetadataNode> lengthChildren = ((Param) param).fList;
        ICTFMetadataNode nextElem = lengthChildren.get(1).getChild(0);
        String type = nextElem.getType();
        if (CTFParser.tokenNames[CTFParser.IDENTIFIER].equals(type)) {
            return concatenateUnaryStrings(lengthChildren.subList(1, lengthChildren.size()));
        } else if (CTFParser.tokenNames[CTFParser.STREAM].equals(type)) {
            return StreamScopeParser.INSTANCE.parse(null, new StreamScopeParser.Param(lengthChildren.subList(1, lengthChildren.size())));
        } else {
            throw new ParseException("Unsupported scope trace." + nextElem); //$NON-NLS-1$
        }
    }

}
