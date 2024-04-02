/*******************************************************************************
 * Copyright (c) 2019, 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.tests.stubs.weighted;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;

/**
 * Simple implementation of the {@link IWeightedTreeProvider} interface, for
 * testing purposes.
 *
 * @author Geneviève Bastien
 */
public class SimpleWeightedTreeProvider implements IWeightedTreeProvider<String, SimpleTree, WeightedTree<String>> {

    static final IWeightedTreeGroupDescriptor ROOT_DESCRIPTOR = new IWeightedTreeGroupDescriptor() {
        @Override
        public @Nullable IWeightedTreeGroupDescriptor getNextGroup() {
            return SECOND_DESCRIPTOR;
        }

        @Override
        public String getName() {
            return "first level";
        }
    };

    private static final IWeightedTreeGroupDescriptor SECOND_DESCRIPTOR = new IWeightedTreeGroupDescriptor() {
        @Override
        public @Nullable IWeightedTreeGroupDescriptor getNextGroup() {
            return null;
        }

        @Override
        public String getName() {
            return "second level";
        }
    };

    private boolean fWithGroupDescriptors;

    /**
     * Constructor
     */
    public SimpleWeightedTreeProvider() {

    }

    /**
     * Sets whether to use specific group descriptor or the default ones
     *
     * @param withGroupDescriptors
     *            If <code>true</code>, the specific group descriptors will be
     *            used to describe the levels, otherwise, the returned group
     *            descriptors will be the ones provided by the default
     *            implementation
     */
    public void setSpecificGroupDescriptor(boolean withGroupDescriptors) {
        fWithGroupDescriptors = withGroupDescriptors;
    }

    @Override
    public IWeightedTreeSet<String, SimpleTree, WeightedTree<String>> getTreeSet() {
        return WeightedTreeTestData.getStubData();
    }

    @Override
    public String getTitle() {
        return "Simple weighted tree provider for unit tests";
    }

    @Override
    public @Nullable IWeightedTreeGroupDescriptor getGroupDescriptor() {
        return fWithGroupDescriptors ? ROOT_DESCRIPTOR : IWeightedTreeProvider.super.getGroupDescriptor();
    }

}