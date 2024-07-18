/**********************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.statistics;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Interface for classes implementing a tree of statistics. It extends the
 * {@link IStatistics} to add children statistics and to provide the name of the
 * tree node.
 *
 * @author Siwei Zhang
 * @param <E>
 *            The type of object to calculate statistics on
 * @since 6.1
 */
public interface ITreeStatistics<@NonNull E> extends IStatistics<E> {

    /**
     * Get the name of the statistic tree node.
     *
     * @return The name of this statistic tree node
     */
    String getName();

    /**
     * Add a child statistic to this statistic.
     *
     * @param treeStatistic
     *            the child statistic to add.
     * @return Whether the add was successful.
     */
    boolean addChild(ITreeStatistics<E> treeStatistic);

    /**
     * @return The list of children statistics.
     */
    List<ITreeStatistics<E>> getChildren();
}
