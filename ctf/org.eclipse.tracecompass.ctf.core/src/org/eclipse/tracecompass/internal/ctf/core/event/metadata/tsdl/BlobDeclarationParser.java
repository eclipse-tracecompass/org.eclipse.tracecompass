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
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.types.BlobDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

import com.google.gson.JsonObject;

/**
 * According to the documentation:
 * https://diamon.org/ctf/files/CTF2-SPECRC-7.0rA.html#sl-blob-fc
 *
 * A static-length BLOB field is a sequence of zero or more contiguous bytes
 * with an associated IANA media type (given by the media-type property of its
 * class).
 *
 * The length, or number of bytes, of a static-length BLOB field is a property
 * (length) of its class.
 *
 * @author Sehr Moosabhoy
 */
public final class BlobDeclarationParser implements ICommonTreeParser {

    /**
     * Blob declaration parser instance
     */
    public static final BlobDeclarationParser INSTANCE = new BlobDeclarationParser();

    private static final @NonNull String LENGTH = "length"; //$NON-NLS-1$
    private static final @NonNull String MEDIA_TYPE = "media-type"; //$NON-NLS-1$
    private static final String DEFAULT_MEDIA_TYPE = "application/octet-stream"; //$NON-NLS-1$

    private BlobDeclarationParser() {
    }

    /**
     * Parses a blob declaration node and returns a BlobDeclaration
     *
     * @param blob
     *            the JsonStructureFieldMemberMetadataNode describing the blob
     * @param unused
     *            unused
     * @return the parsed BlobDeclaration
     * @throws ParseException
     *             if the length attribute is invalid
     */
    @Override
    public BlobDeclaration parse(ICTFMetadataNode blob, ICommonTreeParserParameter unused) throws ParseException {

        long length = 0;
        String mediaType = DEFAULT_MEDIA_TYPE;
        String role = null;

        JsonStructureFieldMemberMetadataNode member = (JsonStructureFieldMemberMetadataNode) blob;
        JsonObject fieldClass = member.getFieldClass().getAsJsonObject();
        length = fieldClass.get(LENGTH).getAsInt();
        if (length <= 0) {
            throw new ParseException("Invalid length attribute in Blob: " + length); //$NON-NLS-1$
        }

        if (fieldClass.has(mediaType)) {
            mediaType = fieldClass.get(MEDIA_TYPE).getAsString();
        }
        role = member.getRole();

        return new BlobDeclaration((int) length, mediaType, role);
    }
}
