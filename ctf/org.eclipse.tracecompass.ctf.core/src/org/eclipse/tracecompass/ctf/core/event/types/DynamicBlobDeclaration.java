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
package org.eclipse.tracecompass.ctf.core.event.types;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF blob class declaration.
 *
 * The declaration of a blob data type that can be used to define sequence of
 * zero or more contiguous bytes with an associated IANA media type
 *
 * @since 5.2
 * @author Arnaud Fiorini
 */
public class DynamicBlobDeclaration extends Declaration {

    private String fLengthName;
    private String fMediaType;

    /**
     * Constructor
     *
     * @param lengthName
     *            the length field location
     * @param mediaType
     *            the IANA media type
     */
    public DynamicBlobDeclaration(@NonNull String lengthName, @NonNull String mediaType) {
        fLengthName = lengthName;
        fMediaType = mediaType;
    }

    @Override
    public @NonNull Definition createDefinition(IDefinitionScope definitionScope, @NonNull String fieldName, @NonNull BitBuffer input) throws CTFException {
        IDefinition lenDef = null;

        if (definitionScope != null) {
            lenDef = definitionScope.lookupDefinition(fLengthName);
        }

        if (lenDef == null) {
            throw new CTFException("Dynamic blob length field not found"); //$NON-NLS-1$
        }

        if (!(lenDef instanceof IntegerDefinition)) {
            throw new CTFException("Dynamic blob length field not integer"); //$NON-NLS-1$
        }

        IntegerDefinition lengthDefinition = (IntegerDefinition) lenDef;

        if (lengthDefinition.getDeclaration().isSigned()) {
            throw new CTFException("Dynamic blob length must not be signed"); //$NON-NLS-1$
        }

        long length = lengthDefinition.getValue();
        if ((length > Integer.MAX_VALUE) || (!input.canRead((int) length * 8))) {
            throw new CTFException("Blob is too large " + length); //$NON-NLS-1$
        }

        byte[] array = new byte[(int) length];
        if (input.getByteBuffer().remaining() < length) {
            throw new CTFException("There is not enough data provided. Length asked: " + length + " Remaining buffer size: " + input.getByteBuffer().remaining()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        /* Offset the buffer position wrt the current alignment */
        alignRead(input);
        input.get(array);

        return new DynamicBlobDefinition(this, definitionScope, fieldName, array, fMediaType);
    }

    @Override
    public int getMaximumSize() {
        return Integer.MAX_VALUE;
    }

    /**
     * From the documentation:
     * https://diamon.org/ctf/files/CTF2-SPECRC-7.0rA.html#align-dec
     *
     * Alignment of a blob will always be 8 bits
     */
    @Override
    public long getAlignment() {
        return 8;
    }

    /**
     * @return a string describing the contents of the blob
     */
    public String getMediaType() {
        return fMediaType;
    }

    /**
     * @return the name of the field which defines the size of the blob
     */
    public String getLengthFieldName() {
        return fLengthName;
    }

    @Override
    public String toString() {
        /* Only used for debugging */
        return "[declaration] dynamicblob[length-field-location=" + fLengthName + ", media-type=" + fMediaType + ']'; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public boolean isBinaryEquivalent(IDeclaration other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        DynamicBlobDeclaration otherBlob = (DynamicBlobDeclaration) other;
        if (!fMediaType.equals(otherBlob.fMediaType)) {
            return false;
        }
        return (fLengthName.equals(otherBlob.fLengthName));
    }
}
