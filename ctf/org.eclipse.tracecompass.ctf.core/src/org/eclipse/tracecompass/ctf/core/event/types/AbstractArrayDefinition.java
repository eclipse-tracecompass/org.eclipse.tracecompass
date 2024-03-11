/*******************************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.ctf.core.event.types;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.tracecompass.internal.ctf.core.utils.SaturatedArithmetic;

/**
 * Array definition, used for compound definitions and fixed length strings
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault
public abstract class AbstractArrayDefinition extends Definition {

    /**
     * Constructor
     *
     * @param declaration
     *            the event declaration
     *
     * @param definitionScope
     *            the definition is in a scope, (normally a struct) what is it?
     * @param fieldName
     *            the name of the definition. (it is a field in the parent scope)
     */
    public AbstractArrayDefinition(IDeclaration declaration, @Nullable IDefinitionScope definitionScope, String fieldName) {
        super(declaration, definitionScope, fieldName);
    }

    /**
     * Get the defintions, an array is a collection of definitions
     *
     * @return the definitions
     */
    public abstract List<@Nullable Definition> getDefinitions();

    /**
     * Get the the number of elements in the array
     *
     * @return how many elements in the array
     * @since 1.0
     */
    public abstract int getLength();

    @Override
    public long size() {
        List<@Nullable Definition> definitions = getDefinitions();
        if (definitions.isEmpty()) {
            return 0;
        }
        Definition definition = definitions.get(0);
        if (definition == null) {
            return 0;
        }
        return SaturatedArithmetic.multiply(getLength(), definition.size());
    }

}