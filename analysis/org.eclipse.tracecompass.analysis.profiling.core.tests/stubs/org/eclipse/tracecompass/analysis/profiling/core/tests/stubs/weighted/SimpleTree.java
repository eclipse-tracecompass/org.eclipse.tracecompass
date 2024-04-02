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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.tree.ITree;

/**
 * A simple {@link ITree} implementation for testing purposes
 *
 * @author Geneviève Bastien
 */
public class SimpleTree implements ITree {

    private final String fName;
    private @Nullable ITree fParent;
    private List<ITree> fChildren = new ArrayList<>();

    /**
     * Constructor
     *
     * @param name
     *            The object name
     */
    public SimpleTree(String name) {
        fName = name;
    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public @Nullable ITree getParent() {
        return fParent;
    }

    @Override
    public Collection<ITree> getChildren() {
        return new ArrayList<>(fChildren);
    }

    /**
     * Add a child to this object
     *
     * @param child
     *            The child to add
     */
    @Override
    public void addChild(ITree child) {
        fChildren.add(child);
        child.setParent(this);
    }

    @Override
    public String toString() {
        return fName;
    }

    @Override
    public ITree copyElement() {
        return new SimpleTree(getName());
    }

    @Override
    public void setParent(@Nullable ITree parent) {
        fParent = parent;
    }

}
