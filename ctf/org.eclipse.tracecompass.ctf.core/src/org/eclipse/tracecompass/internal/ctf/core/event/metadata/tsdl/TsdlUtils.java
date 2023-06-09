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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * TSDL utils, this class provides some simple verifications for a common tree.
 * These are useful before parsing.
 *
 * @author Matthew Khouzam
 *
 */
public final class TsdlUtils {

    private static final UnaryStringParser STRING_PARSER = UnaryStringParser.INSTANCE;

    private TsdlUtils() {
    }

    /**
     * Is the tree a unary string
     *
     * @param node
     *            The node to check.
     * @return True if the given node is an unary string.
     */
    public static boolean isUnaryString(ICTFMetadataNode node) {
        return ((node.getType() == CTFParser.UNARY_EXPRESSION_STRING));
    }

    /**
     * Is the tree a unary string or a quoted string
     *
     * @param node
     *            The node to check.
     * @return True if the given node is any type of unary string (no quotes,
     *         quotes, etc).
     */
    public static boolean isAnyUnaryString(ICTFMetadataNode node) {
        return (isUnaryString(node) || (node.getType() == CTFParser.UNARY_EXPRESSION_STRING_QUOTES));
    }

    /**
     * Is the tree a unary integer
     *
     * @param node
     *            The node to check.
     * @return True if the given node is an unary integer.
     */
    public static boolean isUnaryInteger(ICTFMetadataNode node) {
        return ((node.getType() == CTFParser.UNARY_EXPRESSION_DEC) ||
                (node.getType() == CTFParser.UNARY_EXPRESSION_HEX) || (node.getType() == CTFParser.UNARY_EXPRESSION_OCT));
    }

    /**
     * Concatenates a list of unary strings separated by arrows (->) or dots.
     *
     * @param strings
     *            A list, first element being an unary string, subsequent
     *            elements being ARROW or DOT nodes with unary strings as child.
     * @return The string representation of the unary string chain.
     * @throws ParseException
     *             If the strings list contains a non-string element
     */
    public static @NonNull String concatenateUnaryStrings(List<ICTFMetadataNode> strings) throws ParseException {

        StringBuilder sb = new StringBuilder();

        ICTFMetadataNode first = strings.get(0);
        sb.append(STRING_PARSER.parse(first, null));

        boolean isFirst = true;

        for (ICTFMetadataNode ref : strings) {
            if (isFirst) {
                isFirst = false;
                continue;
            }

            ICTFMetadataNode id = ref.getChild(0);

            if (ref.getType() == CTFParser.ARROW) {
                sb.append("->"); //$NON-NLS-1$
            } else { /* DOT */
                sb.append('.');
            }

            sb.append(STRING_PARSER.parse(id, null));
        }

        return sb.toString();
    }

    /**
     * Throws a ParseException stating that the parent-child relation between
     * the given node and its parent is not valid. It means that the shape of
     * the AST is unexpected.
     *
     * @param child
     *            The invalid child node.
     * @return ParseException with details
     */
    public static ParseException childTypeError(ICTFMetadataNode child) {
        ICTFMetadataNode parent = child.getParent();
        String error = "Parent " + CTFParser.tokenNames[parent.getType()] //$NON-NLS-1$
                + " can't have a child of type " //$NON-NLS-1$
                + CTFParser.tokenNames[child.getType()] + "."; //$NON-NLS-1$

        return new ParseException(error);
    }

}
