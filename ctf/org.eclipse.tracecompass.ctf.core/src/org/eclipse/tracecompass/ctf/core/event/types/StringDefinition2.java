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
package org.eclipse.tracecompass.ctf.core.event.types;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF string definition that supports both static and dynamic-length strings.
 *
 * @author Matthew Khouzam
 * @since 5.1
 */
public final class StringDefinition2 extends Definition {

    private final String fString;

    /**
     * Constructor for StringDeclaration
     *
     * @param declaration the parent declaration
     * @param definitionScope the parent scope
     * @param fieldName the field name
     * @param value the String value
     */
    public StringDefinition2(@NonNull StringDeclaration declaration,
            IDefinitionScope definitionScope, @NonNull String fieldName, String value) {
        super(declaration, definitionScope, fieldName);
        fString = value;
    }

    /**
     * Constructor for DynamicLengthStringDeclaration
     *
     * @param declaration the parent declaration
     * @param definitionScope the parent scope
     * @param fieldName the field name
     * @param value the String value
     */
    public StringDefinition2(@NonNull Declaration declaration,
            IDefinitionScope definitionScope, @NonNull String fieldName, String value) {
        super(declaration, definitionScope, fieldName);
        fString = value;
    }

    /**
     * Gets the string value
     *
     * @return the string
     */
    public String getValue() {
        return fString;
    }

    @Override
    public long size() {
        return fString.length();
    }

    @Override
    public String toString() {
        return '\"' + getValue() + '\"';
    }
}
