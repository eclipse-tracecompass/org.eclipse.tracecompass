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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.string;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.childTypeError;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.isAnyUnaryString;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.types.Encoding;
import org.eclipse.tracecompass.ctf.core.event.types.StringDeclaration;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Strings are an array of bytes of variable size and are terminated by a '\0'
 * “NULL” character. Their encoding is described in the TSDL metadata. In
 * absence of encoding attribute information, the default encoding is UTF-8.
 *
 * TSDL metadata representation of a named string type:
 *
 * <pre>
 * typealias string {
 *   encoding = /* UTF8 OR ASCII * /;
 * } := name;
 * </pre>
 *
 * A nameless string type can be declared as a field type:
 *
 * <pre>
 * string field_name; /* use default UTF8 encoding * /
 * </pre>
 *
 * Strings are always aligned on byte size.
 *
 * @author Matthew Khouzam
 * @author Efficios - Javadoc Preable
 *
 */
public final class StringDeclarationParser implements ICommonTreeParser {

    /**
     * Instance
     */
    public static final StringDeclarationParser INSTANCE = new StringDeclarationParser();

    private static final @NonNull String ENCODING = "encoding"; //$NON-NLS-1$

    private StringDeclarationParser() {
    }

    /**
     * Parse a string declaration node and return a {@link StringDeclaration}
     *
     * @param string
     *            the string declaration AST node
     * @param unused
     *            unused
     * @return a {@link StringDeclaration} describing the string layout
     * @throws ParseException
     *             the AST is malformed
     */
    @Override
    public StringDeclaration parse(ICTFMetadataNode string, ICommonTreeParserParameter unused) throws ParseException {
        List<ICTFMetadataNode> children = string.getChildren();
        StringDeclaration stringDeclaration = null;

        if (string instanceof JsonStructureFieldMemberMetadataNode || children == null) {
            stringDeclaration = StringDeclaration.getStringDeclaration(Encoding.UTF8);
        } else {
            Encoding encoding = Encoding.UTF8;
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
                    if (left.equals(ENCODING)) {
                        encoding = EncodingParser.INSTANCE.parse(rightNode, null);
                    } else {
                        throw new ParseException("String: unknown attribute " //$NON-NLS-1$
                                + left);
                    }
                } else {
                    throw childTypeError(child);
                }
            }

            stringDeclaration = StringDeclaration.getStringDeclaration(encoding);
        }

        return stringDeclaration;
    }

}
