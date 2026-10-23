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
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF2 optional field definition.
 *
 * It wraps the field definition when the optional field is enabled, and holds
 * nothing when it is disabled.
 *
 * @since 5.3
 * @author Arnaud Fiorini
 */
public class OptionalDefinition extends Definition {

    private final @Nullable Definition fField;

    /**
     * Constructor
     *
     * @param declaration
     *            the parent declaration
     * @param definitionScope
     *            the parent scope
     * @param fieldName
     *            the field name
     * @param field
     *            the field definition if the optional is enabled, null
     *            otherwise
     */
    public OptionalDefinition(@NonNull OptionalDeclaration declaration, IDefinitionScope definitionScope, @NonNull String fieldName, @Nullable Definition field) {
        super(declaration, definitionScope, fieldName);
        fField = field;
    }

    @Override
    public OptionalDeclaration getDeclaration() {
        return (OptionalDeclaration) super.getDeclaration();
    }

    /**
     * @return true if the optional field is enabled
     */
    public boolean isEnabled() {
        return fField != null;
    }

    /**
     * @return the field definition if the optional is enabled, null otherwise
     */
    public @Nullable Definition getField() {
        return fField;
    }

    @Override
    public long size() {
        Definition field = fField;
        return field == null ? 0 : field.size();
    }

    @Override
    public String toString() {
        Definition field = fField;
        return field == null ? "null" : field.toString(); //$NON-NLS-1$
    }
}
