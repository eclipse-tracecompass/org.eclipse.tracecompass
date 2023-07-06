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

import java.util.Base64;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF2 blob definition.
 *
 * The definition of a blob data type that can be used to define sequence of
 * zero or more contiguous bytes with an associated IANA media type
 *
 * @author Sehr Moosabhoy
 * @since 4.3
 */
public class BlobDefinition extends SimpleDatatypeDefinition {

    private final byte[] fArray;
    private final String fType;

    /**
     * Constructor
     *
     * @param declaration
     *            the parent declaration
     * @param definitionScope
     *            the parent scope
     * @param fieldName
     *            the field name
     * @param array
     *            the blob byte array
     * @param mediaType
     *            the IANA media type of the byte array
     */
    public BlobDefinition(@NonNull IDeclaration declaration, IDefinitionScope definitionScope, @NonNull String fieldName, byte[] array, String mediaType) {
        super(declaration, definitionScope, fieldName);
        fArray = array;
        fType = mediaType;
    }

    @Override
    public String getStringValue() {
        return toString();
    }

    @Override
    public byte[] getBytes() {
        return fArray;
    }

    @Override
    public @NonNull BlobDeclaration getDeclaration() {
        return (BlobDeclaration) super.getDeclaration();
    }

    @Override
    public String toString() {
        if (fArray.length < getDeclaration().getMaximumSize()) {
            String encoded = Base64.getEncoder().encodeToString(fArray);
            return "[ " + fType + " ]" + encoded; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return ""; //$NON-NLS-1$
    }
}
