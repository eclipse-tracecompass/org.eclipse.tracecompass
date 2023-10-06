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
 *     Patrick Tasse - Initial implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.lang.reflect.Type;

import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

/**
 * Class that stores a field class property, which can be a JSON object or a
 * string referring to a field class alias.
 */
public class FieldClass {

    private final JsonObject fFieldClass;

    /**
     * Field class deserializer
     */
    public static class FieldClassDeserializer implements JsonDeserializer<FieldClass> {
        private final ICTFMetadataNode fRoot;

        /**
         * Constructor
         *
         * @param root
         *            the CTF metadata root node
         */
        public FieldClassDeserializer(ICTFMetadataNode root) {
            fRoot = root;
        }

        @Override
        public FieldClass deserialize(JsonElement jElement, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (jElement.isJsonPrimitive()) {
                JsonPrimitive jPrimitive = jElement.getAsJsonPrimitive();
                if (jPrimitive.isString()) {
                    String fieldClassAlias = jPrimitive.getAsString();
                    for (ICTFMetadataNode node : fRoot.getChildren()) {
                        if (node instanceof JsonFieldClassAliasMetadataNode) {
                            JsonFieldClassAliasMetadataNode fieldClassAliasNode = (JsonFieldClassAliasMetadataNode) node;
                            if (fieldClassAliasNode.getName().equals(fieldClassAlias)) {
                                return new FieldClass(fieldClassAliasNode.getFieldClass());
                            }
                        }
                    }
                    throw new JsonParseException("no previously occurring field class alias named '" + fieldClassAlias + '\''); //$NON-NLS-1$
                }
            } else if (jElement.isJsonObject()) {
                JsonObject jObject = jElement.getAsJsonObject();
                return new FieldClass(jObject);
            }
            throw new JsonParseException("field-class property is not a JSON object or JSON string"); //$NON-NLS-1$
        }
    }

    /**
     * Constructor
     *
     * @param fieldClass
     *            the field class JSON object
     */
    public FieldClass(JsonObject fieldClass) {
        fFieldClass = fieldClass;
    }

    /**
     * Get the field class as a JSON object
     *
     * @return the field class
     */
    public JsonObject getFieldClass() {
        return fFieldClass;
    }
}
