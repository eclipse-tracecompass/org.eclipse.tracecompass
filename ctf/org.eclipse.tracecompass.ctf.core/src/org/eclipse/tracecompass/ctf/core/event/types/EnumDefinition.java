/*******************************************************************************
 * Copyright (c) 2011, 2014 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Matthew Khouzam - Initial API and implementation
 * Contributors: Simon Marchi - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.ctf.core.event.types;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF enum definition.
 *
 * The definition of a enum point basic data type. It will take the data from a
 * trace and store it (and make it fit) as an integer and a string.
 *
 * @version 1.0
 * @author Matthew Khouzam
 * @author Simon Marchi
 */
public final class EnumDefinition extends SimpleDatatypeDefinition {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private static final String UNKNOWN_ENUM = "<unknown> (%s)"; //$NON-NLS-1$

    private static final String UNINITIALIZED = "UNINITIALIZED"; //$NON-NLS-1$

    private final IntegerDefinition fInteger;

    private @Nullable String fValue;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param declaration
     *            the parent declaration
     * @param definitionScope
     *            the parent scope
     * @param fieldName
     *            the field name
     * @param intValue
     *            the value of the enum
     */
    public EnumDefinition(@NonNull EnumDeclaration declaration,
            IDefinitionScope definitionScope, @NonNull String fieldName, IntegerDefinition intValue) {
        super(declaration, definitionScope, fieldName);
        fInteger = intValue;
        fValue = UNINITIALIZED;

    }

    // ------------------------------------------------------------------------
    // Getters/Setters/Predicates
    // ------------------------------------------------------------------------

    /**
     * Gets the value of the enum in string format so
     * "Enum a{DAY="0", NIGHT="1"}; will return "DAY"
     *
     * @return the value of the enum.
     */
    public String getValue() {
        if (fValue == UNINITIALIZED) {
            fValue = getDeclaration().query(fInteger.getValue());
        }
        return fValue != null ? fValue : String.format(UNKNOWN_ENUM, getIntegerValue());
    }

    @Override
    public String getStringValue() {
        return getValue();
    }

    /**
     * Gets the value of the enum in string format so
     * "Enum a{DAY="0", NIGHT="1"}; will return 0
     *
     * @return the value of the enum.
     */
    @Override
    public Long getIntegerValue() {
        return fInteger.getValue();
    }

    @Override
    public EnumDeclaration getDeclaration() {
        return (EnumDeclaration) super.getDeclaration();
    }

    @Override
    public long size() {
        return fInteger.size();
    }

    @Override
    public byte[] getBytes() {
        return fInteger.getBytes();
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    public String toString() {
        return "{ value = " + getValue() + //$NON-NLS-1$
                ", container = " + fInteger.getValue()+ //$NON-NLS-1$
                " }"; //$NON-NLS-1$
    }
}
