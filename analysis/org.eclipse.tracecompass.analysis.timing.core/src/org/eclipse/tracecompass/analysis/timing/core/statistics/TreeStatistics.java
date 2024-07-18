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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Extend {@link Statistics} to support a tree of statistics.
 *
 * @author Siwei Zhang
 * @param <E>
 *            The type of object to calculate statistics on
 * @since 6.1
 */
public class TreeStatistics<@NonNull E> extends Statistics<E> implements ITreeStatistics<E> {

    private String fName = ""; //$NON-NLS-1$
    private List<ITreeStatistics<E>> fChildStatistics = new ArrayList<>();

    /**
     * Constructor
     *
     * @param name
     *            The name of this statistic
     */
    public TreeStatistics(String name) {
        super();
        fName = name;
    }

    /**
     * Constructor
     *
     * @param mapper
     *            A mapper function that takes an object to compute statistics
     *            for and returns the value to use for the statistics
     * @param name
     *            The name of this statistic
     */
    public TreeStatistics(Function<E, @Nullable ? extends @Nullable Number> mapper, String name) {
        super(mapper);
        fName = name;
    }

    @Override
    public List<ITreeStatistics<E>> getChildren() {
        return fChildStatistics;
    }

    @Override
    public boolean addChild(ITreeStatistics<E> treeStatistic) {
        return fChildStatistics.add(treeStatistic);
    }

    @Override
    public String getName() {
        return fName;
    }
}
