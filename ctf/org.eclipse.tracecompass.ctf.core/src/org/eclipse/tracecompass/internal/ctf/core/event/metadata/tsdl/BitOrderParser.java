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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl;

import org.eclipse.tracecompass.ctf.core.event.types.BitOrder;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.CTFJsonMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.MetadataStrings;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * The bit order of any field class that uses the bit order property is either
 * "first-to-last" or "last-to-first". It is an optional property: when it is
 * not specified, it defaults to "first-to-last" for little-endian fields and
 * "last-to-first" for big-endian fields.
 *
 * @author Arnaud Fiorini
 */
public final class BitOrderParser implements ICommonTreeParser {

    /**
     * Instance
     */
    public static final BitOrderParser INSTANCE = new BitOrderParser();

    private static final String INVALID_VALUE_FOR_BIT_ORDER = "Invalid value for bit order"; //$NON-NLS-1$

    private BitOrderParser() {
    }

    /**
     * Gets the value of a "bit-order" property.
     *
     * @param bitOrderTree
     *            A json node containing the bit order string.
     *
     * @return The "bit-order" value.
     * @throws ParseException
     *             if the value is invalid
     */
    @Override
    public final BitOrder parse(ICTFMetadataNode bitOrderTree, ICommonTreeParserParameter param) throws ParseException {
        if (!(bitOrderTree instanceof CTFJsonMetadataNode)) {
            throw new ParseException("The bit order property is not supported in CTF 1.8."); //$NON-NLS-1$
        }
        String stringValue = bitOrderTree.getText();
        if (MetadataStrings.FIRST_TO_LAST.equals(stringValue)) {
            return BitOrder.FIRST_TO_LAST;
        } else if (MetadataStrings.LAST_TO_FIRST.equals(stringValue)) {
            return BitOrder.LAST_TO_FIRST;
        }
        throw new ParseException(INVALID_VALUE_FOR_BIT_ORDER + " : " + stringValue); //$NON-NLS-1$
    }
}
