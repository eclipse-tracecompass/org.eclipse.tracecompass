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
package org.eclipse.tracecompass.internal.ctf.core.event.types;

import java.nio.charset.Charset;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.tracecompass.ctf.core.event.types.Declaration;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.StringDefinition2;

/**
 * Dynamic-length string declaration with encoding support
 *
 * @author Matthew Khouzam
 */
public class DynamicLengthStringDeclaration extends Declaration {

    private final Charset fEncoding;
    private final String fLengthName;

    /**
     * Constructor
     *
     * @param lengthName the name of the length field
     * @param encoding the character encoding
     */
    public DynamicLengthStringDeclaration(@Nullable String lengthName, Charset encoding) {
        fLengthName = lengthName;
        fEncoding = encoding;
    }

    /**
     * Get the encoding
     *
     * @return the character encoding
     */
    public Charset getEncoding() {
        return fEncoding;
    }

    /**
     * Get the length field name
     *
     * @return the length field name
     */
    public String getLengthName() {
        return fLengthName;
    }

    @Override
    public StringDefinition2 createDefinition(@Nullable IDefinitionScope definitionScope, String fieldName, BitBuffer input) throws CTFException {
        IDefinition lenDef = null;
        if (definitionScope != null) {
            lenDef = definitionScope.lookupDefinition(fLengthName);
        }
        if (lenDef == null) {
            throw new CTFException("Length field not found: " + fLengthName); //$NON-NLS-1$
        }
        if (!(lenDef instanceof IntegerDefinition)) {
            throw new CTFException("Length field must be an integer"); //$NON-NLS-1$
        }
        long rawLength = ((IntegerDefinition) lenDef).getValue();
        if (rawLength < 0) {
        	throw new CTFException("Cannot have a length < 0, declared = " + rawLength); //$NON-NLS-1$
        }
        if (rawLength > 1e6) {
        	throw new CTFException("Cannot have a length > 1000000, declared = " + rawLength); //$NON-NLS-1$
        }
        int length = (int)rawLength;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) input.get(Byte.SIZE, false);
        }
        String value = new String(bytes, fEncoding);
        int nullIndex = value.indexOf('\0');
        if (nullIndex >= 0) {
            value = value.substring(0, nullIndex);
        }
        return new StringDefinition2(this, definitionScope, fieldName, value);
    }

    @Override
    public long getAlignment() {
        return Byte.SIZE;
    }

    @Override
    public int getMaximumSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return "dynamic_string[" + fLengthName + "]<" + fEncoding.name() + ">"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public boolean isBinaryEquivalent(IDeclaration other) {
        // TODO Auto-generated method stub
        return false;
    }
}
