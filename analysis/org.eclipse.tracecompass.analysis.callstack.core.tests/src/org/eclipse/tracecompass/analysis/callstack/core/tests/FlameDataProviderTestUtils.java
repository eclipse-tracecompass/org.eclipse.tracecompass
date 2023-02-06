/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.callstack.core.tests;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.FlameChartEntryModel;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.FlameChartEntryModel.EntryType;

/**
 * Utility methods to manipulate flame chart/graph data provider data
 *
 * @author Geneviève Bastien
 */
public class FlameDataProviderTestUtils {

    /**
     * Find an entry by its ID
     *
     * @param list
     *            The list of flame chart entries
     * @param id
     *            The ID to search for
     * @return The entry, or <code>null</code> if the entry is unavailable
     */
    public static @Nullable FlameChartEntryModel findEntryById(Collection<FlameChartEntryModel> list, long id) {
        return list.stream()
                .filter(entry -> entry.getId() == id)
                .findFirst().orElse(null);
    }

    /**
     * Find an entry by its name and type
     *
     * @param list
     *            The list of flame chart entries
     * @param name
     *            The name of the entry
     * @param type
     *            The type of the entry
     * @return The entry, or <code>null</code> if the entry is unavailable
     */
    public static @Nullable FlameChartEntryModel findEntryByNameAndType(Collection<FlameChartEntryModel> list, String name, EntryType type) {
        return list.stream()
                .filter(entry -> entry.getEntryType() == type && entry.getName().equals(name))
                .findFirst().orElse(null);
    }

    /**
     * Find an entry by its depth and type
     *
     * @param list
     *            The list of flame chart entries
     * @param depth
     *            The depth of the entry
     * @param type
     *            The type of the entry
     * @return The entry, or <code>null</code> if the entry is unavailable
     */
    public static @Nullable FlameChartEntryModel findEntryByDepthAndType(Collection<FlameChartEntryModel> list, int depth, EntryType type) {
        return list.stream()
                .filter(entry -> entry.getEntryType() == type && entry.getDepth() == depth)
                .findFirst().orElse(null);
    }

    /**
     * @param list
     *            The list of flame chart entries
     * @param parentId
     *            The parent ID
     * @return The entries
     */
    public static List<FlameChartEntryModel> findEntriesByParent(Collection<FlameChartEntryModel> list, long parentId) {
        return list.stream()
                .filter(entry -> entry.getParentId() == parentId)
                .collect(Collectors.toList());
    }
}
