/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.instrumented;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackGroupDescriptor;

/**
 * A basic group descriptor implementation.
 *
 * @author Geneviève Bastien
 * @since 2.5
 */
public class CallStackGroupDescriptor implements ICallStackGroupDescriptor {

    private final String fName;
    private final @Nullable ICallStackGroupDescriptor fNextGroup;
    private final boolean fSymbolKeyGroup;

    /**
     * Constructor
     *
     * @param name
     *            The name of this group
     * @param nextGroup
     *            The next group of the hierarchy, i.e., the child of the group
     *            being constructed or <code>null</code> if this group is the
     *            leaf
     * @param isSymbolKeyGroup
     *            Whether this level contains the symbol key
     */
    public CallStackGroupDescriptor(String name, @Nullable ICallStackGroupDescriptor nextGroup, boolean isSymbolKeyGroup) {
        fName = name;
        fNextGroup = nextGroup;
        fSymbolKeyGroup = isSymbolKeyGroup;
    }

    @Override
    public @Nullable ICallStackGroupDescriptor getNextGroup() {
        return fNextGroup;
    }

    @Override
    public boolean isSymbolKeyGroup() {
        return fSymbolKeyGroup;
    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public String toString() {
        return "CallStack Descriptor: " + getName(); //$NON-NLS-1$
    }
}
