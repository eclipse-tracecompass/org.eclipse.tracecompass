/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.ITree;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.IWeightedTreeGroupDescriptor;

/**
 * A basic callstack element implementing the methods of the interface.
 *
 * @author Geneviève Bastien
 */
public class CallStackElement implements ICallStackElement {

    /**
     * The default key to use for symbol resolution if none is available
     */
    public static final int DEFAULT_SYMBOL_KEY = -1;

    private final String fName;
    private final IWeightedTreeGroupDescriptor fDescriptor;
    private final @Nullable IWeightedTreeGroupDescriptor fNextDescriptor;
    private final Collection<ICallStackElement> fChildren = new ArrayList<>();
    private @Nullable ICallStackElement fParent;
    private @Nullable ICallStackElement fSymbolKeyElement = null;

    /**
     * Constructor
     *
     * @param name
     *            The name of this element
     * @param descriptor
     *            The corresponding group descriptor
     */
    public CallStackElement(String name, IWeightedTreeGroupDescriptor descriptor) {
        this(name, descriptor, null, null);
    }

    /**
     * Constructor
     *
     * @param name
     *            The name of this element
     * @param descriptor
     *            The corresponding group descriptor
     * @param nextGroup
     *            The next group descriptor
     * @param parent
     *            The parent element
     */
    public CallStackElement(String name, IWeightedTreeGroupDescriptor descriptor, @Nullable IWeightedTreeGroupDescriptor nextGroup, @Nullable ICallStackElement parent) {
        fName = name;
        fDescriptor = descriptor;
        fParent = parent;
        fNextDescriptor = nextGroup;
        if (parent instanceof CallStackElement) {
            fSymbolKeyElement = ((CallStackElement) parent).fSymbolKeyElement;
        }
    }

    @Override
    public Collection<ICallStackElement> getChildrenElements() {
        return fChildren;
    }

    @Override
    public void addChild(ITree child) {
        if (!(child instanceof ICallStackElement)) {
            throw new IllegalArgumentException("The CallStackElement hierarchy does not support children of type " + child.getClass().getName()); //$NON-NLS-1$
        }
        fChildren.add((ICallStackElement) child);
        child.setParent(this);
    }

    @Override
    public void setParent(@Nullable ITree parent) {
        if (parent != null && !(parent instanceof ICallStackElement)) {
            throw new IllegalArgumentException("The CallStackElement hierarchy does not support parent of type " + parent.getClass().getName()); //$NON-NLS-1$
        }
        fParent = (ICallStackElement) parent;
    }

    @Override
    public IWeightedTreeGroupDescriptor getGroup() {
        return fDescriptor;
    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public boolean isLeaf() {
        return fNextDescriptor == null;
    }

    @Override
    public @Nullable IWeightedTreeGroupDescriptor getNextGroup() {
        return fNextDescriptor;
    }

    @Override
    public void setSymbolKeyElement(ICallStackElement element) {
        fSymbolKeyElement = element;
    }

    @Override
    public boolean isSymbolKeyElement() {
        return fSymbolKeyElement == this;
    }

    @Override
    public final int getSymbolKeyAt(long startTime) {
        int processId = DEFAULT_SYMBOL_KEY;
        if (isSymbolKeyElement()) {
            return retrieveSymbolKeyAt(startTime);
        }
        ICallStackElement symbolKeyElement = fSymbolKeyElement;
        // if there is no symbol key element, return the default value
        if (symbolKeyElement == null) {
            return processId;
        }
        return symbolKeyElement.getSymbolKeyAt(startTime);
    }

    /**
     * Retrieve the symbol key for this element. This method is called by
     * {@link #getSymbolKeyAt(long)} when the current element is the symbol key.
     * So this method should assume the current is the symbol key provider and
     * use its own values to retrieve what the key to resolve symbols should be
     * at the time of the query.
     *
     * @param time
     *            The time at which to resolve the symbol
     * @return The symbol key at the requested time
     */
    protected int retrieveSymbolKeyAt(long time) {
        return DEFAULT_SYMBOL_KEY;
    }

    @Override
    public @Nullable ICallStackElement getParentElement() {
        return fParent;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public @Nullable ITree getParent() {
        return getParentElement();
    }

    @Override
    public Collection<ITree> getChildren() {
        List<ITree> list = new ArrayList<>();
        list.addAll(fChildren);
        return list;
    }

    @Override
    public CallStackElement copyElement() {
        return new CallStackElement(fName, fDescriptor, fNextDescriptor, null);
    }
}
