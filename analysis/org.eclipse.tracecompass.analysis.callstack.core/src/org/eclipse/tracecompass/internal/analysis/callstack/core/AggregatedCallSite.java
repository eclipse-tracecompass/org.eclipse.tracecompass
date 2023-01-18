/*******************************************************************************
 * Copyright (c) 2017-2019 École Polytechnique de Montréal
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
import java.util.Map;

import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.WeightedTree;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

/**
 * Base class for aggregating call site data from either sampled or instrumented
 * call stacks.
 *
 * @author Geneviève Bastien
 */
public class AggregatedCallSite extends WeightedTree<ICallStackSymbol> {

    /**
     * Constructor
     *
     * @param symbol
     *            The symbol of the call site. It can eventually be resolved to
     *            a string using the symbol providers
     * @param initialLength
     *            The initial length of this object
     */
    public AggregatedCallSite(ICallStackSymbol symbol, long initialLength) {
        super(symbol, initialLength);
    }

    /**
     * Copy constructor
     *
     * @param copy
     *            The call site to copy
     */
    protected AggregatedCallSite(AggregatedCallSite copy) {
        super(copy);
    }

    /**
     * Return the children as a collection of aggregatedCallSite
     *
     * @return The children as callees
     */
    @VisibleForTesting
    public Collection<AggregatedCallSite> getCallees() {
        List<AggregatedCallSite> list = new ArrayList<>();
        for (WeightedTree<ICallStackSymbol> child : getChildren()) {
            if (child instanceof AggregatedCallSite) {
                list.add((AggregatedCallSite) child);
            }
        }
        return list;
    }

    /**
     * Make a copy of this callsite, with its statistics. Implementing classes
     * should make sure they copy all fields of the callsite, including the
     * statistics.
     *
     * @return A copy of this aggregated call site
     */
    @Override
    public AggregatedCallSite copyOf() {
        return new AggregatedCallSite(this);
    }

    /**
     * Get additional statistics for this call site
     *
     * @return A map of statistics title with statistics
     */
    public Map<String, IStatistics<?>> getStatistics() {
        return ImmutableMap.of();
    }

    @Override
    public String toString() {
        return "CallSite: " + getObject(); //$NON-NLS-1$
    }
}
