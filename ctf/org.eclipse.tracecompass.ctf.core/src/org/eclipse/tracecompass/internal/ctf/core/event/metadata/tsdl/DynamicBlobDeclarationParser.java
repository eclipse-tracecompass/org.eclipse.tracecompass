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

import java.util.Objects;

import org.eclipse.tracecompass.ctf.core.event.types.DynamicBlobDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMemberMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * According to the documentation:
 * https://diamon.org/ctf/files/CTF2-SPECRC-7.0rA.html#dl-blob-fc
 *
 * A dynamic-length BLOB field is a sequence of zero or more contiguous bytes
 * with an associated IANA media type (given by the media-type property of its
 * class).
 *
 * The length, or number of bytes, of a dynamic-length BLOB field is specified
 * with a length-field-location
 *
 * @author Arnaud Fiorini
 */
public class DynamicBlobDeclarationParser implements ICommonTreeParser {

    /**
     * Blob declaration parser instance
     */
    public static final DynamicBlobDeclarationParser INSTANCE = new DynamicBlobDeclarationParser();

    private static final String DEFAULT_MEDIA_TYPE = "application/octet-stream"; //$NON-NLS-1$

    private DynamicBlobDeclarationParser() {
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
    public DynamicBlobDeclaration parse(ICTFMetadataNode blob, ICommonTreeParserParameter unused) throws ParseException {
        String mediaType = DEFAULT_MEDIA_TYPE;

        JsonStructureFieldMemberMetadataNode member = (JsonStructureFieldMemberMetadataNode) blob;
        JsonElement fieldClassElement = member.getFieldClass();
        if (fieldClassElement == null || !fieldClassElement.isJsonObject()) {
            throw new ParseException(getClass().getName() + " fieldclass must be a json object."); //$NON-NLS-1$
        }
        JsonObject fieldClass = member.getFieldClass().getAsJsonObject();

        JsonElement lengthFieldLocation = fieldClass.get(JsonMetadataStrings.LENGTH_FIELD_LOCATION);
        if (lengthFieldLocation == null) {
            throw new ParseException("Dynamic-length array requires length-field-location property"); //$NON-NLS-1$
        }

        JsonElement pathElement = lengthFieldLocation.getAsJsonObject().get(JsonMetadataStrings.PATH);
        String lengthName = pathElement.isJsonArray() ? pathElement.getAsJsonArray().get(pathElement.getAsJsonArray().size() - 1).getAsString() : pathElement.getAsString();

        if (fieldClass.has(JsonMetadataStrings.MEDIA_TYPE)) {
            mediaType = fieldClass.get(JsonMetadataStrings.MEDIA_TYPE).getAsString();
        }

        return new DynamicBlobDeclaration(Objects.requireNonNull(lengthName), Objects.requireNonNull(mediaType));
    }
}
