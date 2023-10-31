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

package org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.model;

import java.util.Collection;

/**
 * Interface that analyses should implement if they provide information on the
 * amount of time a certain TID spends on the CPU. This will cause this analysis
 * to be picked up at creation and added to the model for the host.
 *
 * NOTE to developers: this interface is used with the composite host model but
 * won't be necessary anymore once the analyses populate the model directly.
 */
public interface ICpuTimeProvider {

    /**
     * Get the amount of time a thread was active on the CPU (any CPU) during a
     * period.
     *
     * @param tid
     *            The ID of the thread
     * @param start
     *            The start of the period for which to get the time on CPU
     * @param end
     *            The end of the period for which to get the time on CPU
     * @return The time spent on the CPU by the thread in that duration or
     *         {@link IHostModel#TIME_UNKNOWN} if it is not available
     */
    long getCpuTime(int tid, long start, long end);

    /**
     * Get the list of host IDs this provider is for
     *
     * @return The list of host IDs
     */
    Collection<String> getHostIds();
}
