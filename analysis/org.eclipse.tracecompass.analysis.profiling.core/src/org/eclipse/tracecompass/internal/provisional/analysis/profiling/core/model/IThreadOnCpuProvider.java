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

import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface to be implemented by analyses who has information about the thread
 * running on a CPU at a given time.
 *
 * NOTE to developers: this interface is used with the composite host model but
 * won't be necessary anymore once the analyses populate the model directly.
 */
public interface IThreadOnCpuProvider {

    /**
     * Gets the current thread ID on a given CPU for a given time
     *
     * @param cpu
     *            the CPU
     * @param time
     *            the time in nanoseconds
     * @return the current TID at the time on the CPU or {@code null} if not
     *         known
     */
    default @Nullable Integer getThreadOnCpuAtTime(int cpu, long time) {
        return getThreadOnCpuAtTime(cpu, time, false);
    }

    /**
     * Gets the current thread ID on a given CPU for a given time, but may
     * blocks if the answer is not available yet but may come later
     *
     * @param cpu
     *            the CPU
     * @param time
     *            the time in nanoseconds
     * @param block
     *            If <code>true</code>, the method will block until the
     *            providers have the data available.
     * @return the current TID at the time on the CPU or {@code null} if not
     *         known
     */
    @Nullable Integer getThreadOnCpuAtTime(int cpu, long time, boolean block);

    /**
     * The list of host IDs for which this object providers information on
     * thread on the CPU
     *
     * @return The list of host IDs
     */
    Collection<String> getHostIds();
}
