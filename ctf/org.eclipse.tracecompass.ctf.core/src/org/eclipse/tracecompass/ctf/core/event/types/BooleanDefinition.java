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
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF2 boolean definition.
 *
 * The definition of a boolean field class in ctf2
 *
 * @author Arnaud Fiorini
 * @since 5.2
 */
public class BooleanDefinition extends SimpleDatatypeDefinition {

    private final boolean fValue;

    /**
     * Constructor
     *
     * @param declaration
     *            the parent declaration
     * @param definitionScope
     *            the parent scope
     * @param fieldName
     *            the field name
     * @param value
     *            the field value
     */
    public BooleanDefinition(@NonNull IDeclaration declaration, IDefinitionScope definitionScope, @NonNull String fieldName, boolean value) {
        super(declaration, definitionScope, fieldName);
        fValue = value;
    }

    /**
     * @return boolean value of the field
     */
    public boolean getValue() {
        return fValue;
    }

    @Override
    public BooleanDeclaration getDeclaration() {
        return (BooleanDeclaration) super.getDeclaration();
    }

    @Override
    public String toString() {
        return String.valueOf(fValue);
    }
}
