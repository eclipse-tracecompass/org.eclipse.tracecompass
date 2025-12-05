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
import org.eclipse.tracecompass.ctf.core.event.types.StringDefinition2;

import com.google.common.base.Objects;

/**
 * Dynamic-length string declaration with encoding support
 *
 * @author Matthew Khouzam
 */
public class StaticLengthStringDeclaration extends Declaration {

    private final Charset fEncoding;
    private final int fLength;

    /**
     * Constructor
     *
     * @param length
     *            the name of the length field
     * @param encoding
     *            the character encoding
     */
    public StaticLengthStringDeclaration(int length, Charset encoding) {
        fLength = length;
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
     * Get the length
     *
     * @return the length
     */
    public int getLength() {
        return fLength;
    }

    @Override
    public StringDefinition2 createDefinition(@Nullable IDefinitionScope definitionScope, String fieldName, BitBuffer input) throws CTFException {
        long rawLength = fLength;
        if (rawLength < 0) {
            throw new CTFException("Cannot have a length < 0, declared = " + rawLength); //$NON-NLS-1$
        }
        if (rawLength > 1e6) {
            throw new CTFException("Cannot have a length > 1000000, declared = " + rawLength); //$NON-NLS-1$
        }
        int length = (int) rawLength;
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
        return "dynamic_string[" + fLength + "]<" + fEncoding.name() + ">"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public boolean isBinaryEquivalent(IDeclaration other) {
        if (!(other instanceof StaticLengthStringDeclaration)) {
            return false;
        }
        StaticLengthStringDeclaration o = (StaticLengthStringDeclaration) other;
        return fLength == o.fLength && Objects.equal(fEncoding, o.fEncoding);
    }
}
