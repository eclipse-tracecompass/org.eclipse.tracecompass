/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sehr Moosabhoy - Initial implementation
 *******************************************************************************/
package org.eclipse.tracecompass.ctf.core.event.types;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF blob class definition.
 *
 * The definition of a blob data type that can be used to define sequence of
 * zero or more contiguous bytes with an associated IANA media type
 *
 * @author Sehr Moosabhoy
 * @since 4.3
 */
public class BlobDeclaration extends Declaration {

    private final int fLength;
    private final String fMediaType;

    /**
     * Constructor
     *
     * @param len
     *            The length in bits
     * @param mediaType
     *            The media type of the data
     * @param role
     *            The role of the blob
     */
    public BlobDeclaration(int len, String mediaType, String role) {
        fLength = len;
        fMediaType = mediaType;
        setRole(role);
    }

    @Override
    public @NonNull BlobDefinition createDefinition(IDefinitionScope definitionScope, @NonNull String fieldName, @NonNull BitBuffer input) throws CTFException {
        if (fLength > getMaximumSize()) {
            throw new CTFException("Length asked: " + fLength + " is larger than the maximum blob size " + getMaximumSize()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        byte[] array = new byte[fLength];
        if (input.getByteBuffer().remaining() < fLength) {
            throw new CTFException("There is not enough data provided. Length asked: " + fLength + " Remaining buffer size: " + input.getByteBuffer().remaining()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        input.get(array);

        return new BlobDefinition(this, definitionScope, fieldName, array, fMediaType);
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
     * Arbitrary decision to have maximum size as 1MB
     */
    @Override
    public int getMaximumSize() {
        return 1000000;
    }

    @Override
    public boolean isBinaryEquivalent(IDeclaration other) {
        return false;
    }
}
