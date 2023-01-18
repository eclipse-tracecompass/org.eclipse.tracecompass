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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.IWeightedTreeSet;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Represents a callgraph, ie the aggregation of callsites per elements.
 *
 * TODO: Have an interface and keep the add* method internal
 *
 * @author Geneviève Bastien
 */
public class CallGraph implements IWeightedTreeSet<ICallStackSymbol, ICallStackElement, AggregatedCallSite> {

    /**
     * An empty graph that can be returned when there is no other call graph
     * available
     */
    public static final CallGraph EMPTY_GRAPH = new CallGraph();

    private Set<ICallStackElement> fRootElements = new HashSet<>();
    private final Multimap<ICallStackElement, AggregatedCallSite> fCcts = HashMultimap.create();

    /**
     * Constructor
     */
    public CallGraph() {
        // Empty
    }

    /**
     * Gets the calling context tree for an element.
     *
     * The calling context tree is the callgraph data aggregated by keeping the
     * context of each call.
     *
     * @param element
     *            The element for which to get the calling context tree
     * @return The aggregated data for the first level of the callgraph
     */
    @SuppressWarnings("null")
    public Collection<AggregatedCallSite> getCallingContextTree(ICallStackElement element) {
        return fCcts.get(element);
    }

    /**
     * Add an aggregated callsite to a callstack element.
     *
     * @param dstGroup
     *            the destination group
     * @param callsite
     *            the callsite to add
     */
    public void addAggregatedCallSite(ICallStackElement dstGroup, AggregatedCallSite callsite) {
        // Make sure the root element is present
        ICallStackElement root = dstGroup;
        ICallStackElement parent = dstGroup.getParentElement();
        while (parent != null) {
            root = parent;
            parent = parent.getParentElement();
        }
        fRootElements.add(root);
        // Add the callsite to the appropriate group
        Collection<AggregatedCallSite> callsites = fCcts.get(dstGroup);
        for (AggregatedCallSite site : callsites) {
            if (site.getObject().equals(callsite.getObject())) {
                site.merge(callsite);
                return;
            }
        }
        fCcts.put(dstGroup, callsite);
    }

    /**
     * Get the root elements containing the call graph data.
     *
     * @return The root elements of the call graph
     */
    @Override
    public Collection<ICallStackElement> getElements() {
        return ImmutableSet.copyOf(fRootElements);
    }

    @Override
    public Collection<AggregatedCallSite> getTreesFor(Object element) {
        if (element instanceof ICallStackElement) {
            return getCallingContextTree((ICallStackElement) element);
        }
        return Collections.emptyList();
    }
}
