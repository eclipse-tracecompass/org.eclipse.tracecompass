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

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

/**
 * A CTF2 declaration of an optional field.
 *
 * An optional field is, depending on the value of a previously decoded
 * selector field, either an instance of a given field class (enabled) or a
 * zero-bit field (disabled).
 * <ul>
 * <li>If the selector field is a boolean field, the optional field is enabled
 * when the selector is true.</li>
 * <li>If the selector field is an integer field, the optional field is enabled
 * when the selector value is within the selector ranges.</li>
 * </ul>
 *
 * @since 5.3
 * @author Arnaud Fiorini
 */
public class OptionalDeclaration extends Declaration {

    private final String fSelectorName;
    private final IDeclaration fFieldDeclaration;
    private final @Nullable List<IntegerRange> fSelectorRanges;

    /**
     * Constructor
     *
     * @param selectorName
     *            the name of the selector field
     * @param fieldDeclaration
     *            the declaration of the selector field
     * @param selectorRanges
     *            the ranges of the selector values enabling the optional
     *            field, required for an integer selector, null for a boolean
     *            selector
     */
    public OptionalDeclaration(String selectorName, IDeclaration fieldDeclaration, @Nullable List<IntegerRange> selectorRanges) {
        fSelectorName = selectorName;
        fFieldDeclaration = fieldDeclaration;
        fSelectorRanges = selectorRanges == null ? null : List.copyOf(selectorRanges);
    }

    @Override
    public @NonNull OptionalDefinition createDefinition(IDefinitionScope definitionScope, @NonNull String fieldName, @NonNull BitBuffer input) throws CTFException {
        if (definitionScope == null) {
            throw new CTFException("Optional field " + fieldName + " has no scope to look up its selector " + fSelectorName); //$NON-NLS-1$ //$NON-NLS-2$
        }
        IDefinition selector = definitionScope.lookupDefinition(fSelectorName);
        Definition field = null;
        if (isEnabled(selector)) {
            field = fFieldDeclaration.createDefinition(definitionScope, fieldName, input);
        }
        return new OptionalDefinition(this, definitionScope, fieldName, field);
    }

    private boolean isEnabled(@Nullable IDefinition definitionScope) throws CTFException {
        if (definitionScope == null) {
            throw new CTFException("Optional selector field not found: " + fSelectorName); //$NON-NLS-1$
        }
        if (definitionScope instanceof BooleanDefinition booleanDefinition) {
            return booleanDefinition.getValue();
        }
        if (!(definitionScope instanceof IntegerDefinition)) {
            throw new CTFException("Optional selector field must be a boolean or an integer: " + fSelectorName); //$NON-NLS-1$
        }
        IntegerDefinition integerDefinition = (IntegerDefinition) definitionScope;
        Long value = integerDefinition.getIntegerValue();
        List<IntegerRange> ranges = fSelectorRanges;
        if (ranges == null) {
            throw new CTFException("Optional field with an integer selector requires selector ranges: " + fSelectorName); //$NON-NLS-1$
        }
        if (value < 0 && !integerDefinition.getDeclaration().isSigned()) {
            /* Unsigned value above Long.MAX_VALUE, outside of any range */
            return false;
        }
        for (IntegerRange range : ranges) {
            if (range.getStart() <= value && value <= range.getEnd()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the name of the selector field
     */
    public String getSelectorName() {
        return fSelectorName;
    }

    /**
     * @return the declaration of the field when the optional is enabled
     */
    public IDeclaration getFieldDeclaration() {
        return fFieldDeclaration;
    }

    /**
     * @return the selector ranges enabling the optional field, null for a
     *         boolean selector
     */
    public @Nullable List<IntegerRange> getSelectorRanges() {
        return fSelectorRanges;
    }

    @Override
    public long getAlignment() {
        return 1;
    }

    @Override
    public int getMaximumSize() {
        return fFieldDeclaration.getMaximumSize();
    }

    @Override
    public String toString() {
        /* Only used for debugging */
        return "[declaration] optional[selector:" + fSelectorName + ", field:" + fFieldDeclaration + ']'; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public boolean isBinaryEquivalent(IDeclaration other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        OptionalDeclaration o = (OptionalDeclaration) other;
        return fSelectorName.equals(o.fSelectorName)
                && rangesEqual(fSelectorRanges, o.fSelectorRanges)
                && fFieldDeclaration.isBinaryEquivalent(o.fFieldDeclaration);
    }

    private static boolean rangesEqual(@Nullable List<IntegerRange> a, @Nullable List<IntegerRange> b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).getStart() != b.get(i).getStart() || a.get(i).getEnd() != b.get(i).getEnd()) {
                return false;
            }
        }
        return true;
    }
}
