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

package org.eclipse.tracecompass.analysis.profiling.core.callgraph;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.FlameDefaultPalette2;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.analysis.profiling.core.base.IDataPalette;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTreeGroupBy;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

/**
 * Interface that analyses who provide callgraph
 *
 * @author Geneviève Bastien
 * @since 2.5
 */
public interface ICallGraphProvider2 extends IWeightedTreeProvider<ICallStackSymbol, ICallStackElement, AggregatedCallSite> {

    /**
     * Get the group descriptors that describe how the elements are grouped in
     * this call graph hierarchy. This method will return the root group
     * descriptor. Children groups can be retrieved by the parent group. For
     * call graph providers who have only one series, this will be a singleton.
     *
     * @return The collection of group descriptors for this call graph
     */
    Collection<IWeightedTreeGroupDescriptor> getGroupDescriptors();

    @Override
    default @Nullable IWeightedTreeGroupDescriptor getGroupDescriptor() {
        // Return the first group descriptor
        Collection<IWeightedTreeGroupDescriptor> groupDescriptors = getGroupDescriptors();
        if (groupDescriptors.isEmpty()) {
            return null;
        }
        return groupDescriptors.iterator().next();
    }

    /**
     * Get the call graph for a given time range. This callgraph is for all the
     * elements. The caller can then group the result by calling
     * {@link WeightedTreeGroupBy#groupWeightedTreeBy(IWeightedTreeGroupDescriptor, IWeightedTreeSet, IWeightedTreeProvider)}
     * method
     *
     * @param start
     *            The start of the range
     * @param end
     *            The end of the range
     * @return The call graph object containing the CCTs for each element in the
     *         range.
     */
    CallGraph getCallGraph(ITmfTimestamp start, ITmfTimestamp end);

    /**
     * Get the call graph for the full range of the trace. This callgraph is for
     * all the elements. The caller can then group the result by calling
     * {@link WeightedTreeGroupBy#groupWeightedTreeBy(IWeightedTreeGroupDescriptor, IWeightedTreeSet, IWeightedTreeProvider)}
     *
     * @return The call graph object containing the CCTs for each element in the
     *         range.
     */
    CallGraph getCallGraph();

    @Override
    default @Nullable IWeightedTreeSet<ICallStackSymbol, ICallStackElement, AggregatedCallSite> getSelection(ITmfTimestamp start, ITmfTimestamp end) {
        return getCallGraph(start, end);
    }

    @Override
    default IWeightedTreeSet<ICallStackSymbol, ICallStackElement, AggregatedCallSite> getTreeSet() {
        return getCallGraph();
    }

    /**
     * Factory method to create an aggregated callsite for a symbol
     *
     * @param object
     *            The symbol
     * @return A new aggregated callsite
     */
    AggregatedCallSite createCallSite(Object object);

    @Override
    default IDataPalette getPalette() {
        return FlameDefaultPalette2.getInstance();
    }
}
