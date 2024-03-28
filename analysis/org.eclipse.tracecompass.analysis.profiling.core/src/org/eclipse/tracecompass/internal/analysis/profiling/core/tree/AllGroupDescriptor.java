/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.core.tree;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;

/**
 * Group descriptor to represent all elements grouped together
 *
 * @author Geneviève Bastien
 */
public final class AllGroupDescriptor implements IWeightedTreeGroupDescriptor {

    private static final String ALL_NAME = "all"; //$NON-NLS-1$

    private static final IWeightedTreeGroupDescriptor INSTANCE = new AllGroupDescriptor();

    /**
     * Get the instance of the all group descriptor
     *
     * @return The instance of this group.
     */
    public static IWeightedTreeGroupDescriptor getInstance() {
        return INSTANCE;
    }

    private AllGroupDescriptor() {
    }

    @Override
    public @Nullable IWeightedTreeGroupDescriptor getNextGroup() {
        return null;
    }

    @Override
    public String getName() {
        return ALL_NAME;
    }
}
