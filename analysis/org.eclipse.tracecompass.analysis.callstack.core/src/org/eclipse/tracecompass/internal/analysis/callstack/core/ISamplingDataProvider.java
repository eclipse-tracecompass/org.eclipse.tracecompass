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

/**
 * Interfaces that classes providing sampling data for threads can implement
 *
 * @author Geneviève Bastien
 */
public interface ISamplingDataProvider {

    /**
     * Get the aggregated sample data for a thread in a time range.
     *
     * @param tid
     *            The ID of the thread
     * @param start
     *            The start of the period for which to get the time on CPU
     * @param end
     *            The end of the period for which to get the time on CPU
     * @return The collection of aggregated sampling data for the time range
     */
    Collection<AggregatedCallSite> getSamplingData(int tid, long start, long end);

    /**
     * The list of host IDs for which this object providers information on
     * thread on the CPU
     *
     * @return The list of host IDs
     */
    Collection<String> getHostIds();
}
