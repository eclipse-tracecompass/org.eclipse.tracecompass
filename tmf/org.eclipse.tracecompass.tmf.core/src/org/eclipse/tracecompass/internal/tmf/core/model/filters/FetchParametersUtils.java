/**********************************************************************
 * Copyright (c) 2019, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.tmf.core.model.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.VirtualTableQueryFilter;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.filters.FilterTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;

/**
 * Utility class to deal with data providers parameters. It can be use to create
 * parameters map from the old filter's API or create filters with a map.
 *
 * @author Simon Delisle
 */
public class FetchParametersUtils {

    private FetchParametersUtils() {
        // Default constructor
    }

    /**
     * Create a {@link TimeQueryFilter} with the given map of parameters
     *
     * @param parameters
     *            Map of parameters
     * @return A {@link TimeQueryFilter} or null if the parameters are invalid
     */
    public static @Nullable TimeQueryFilter createTimeQuery(Map<String, Object> parameters) {
        List<Long> timeRequested = DataProviderParameterUtils.extractTimeRequested(parameters);
        return timeRequested == null ? null : new TimeQueryFilter(timeRequested);
    }

    /**
     * Create a {@link SelectionTimeQueryFilter} with the given map of parameters
     *
     * @param parameters
     *            Map of parameters
     * @return A {@link SelectionTimeQueryFilter} or null if the parameters are invalid
     */
    public static @Nullable SelectionTimeQueryFilter createSelectionTimeQuery(Map<String, Object> parameters) {
        List<Long> timeRequested = DataProviderParameterUtils.extractTimeRequested(parameters);
        List<Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(parameters);
        return (timeRequested == null || selectedItems == null) ? null : new SelectionTimeQueryFilter(timeRequested, selectedItems);
    }

    /**
     * Create a {@link SelectionTimeQueryFilter} with the given map of
     * parameters with number of samples specified.
     *
     * @param parameters
     *            Map of parameters
     * @return A {@link SelectionTimeQueryFilter} or null if the parameters are
     *         invalid
     */
    public static @Nullable SelectionTimeQueryFilter createSelectionTimeQueryWithSamples(Map<String, Object> parameters) {
        DataProviderParameterUtils.TimeRangeWithSamples timeRange = DataProviderParameterUtils.extractTimeRangeWithSamples(parameters);
        List<Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(parameters);
        return (timeRange == null || selectedItems == null)
                ? null
                : new SelectionTimeQueryFilter(timeRange.start(), timeRange.end(), timeRange.nbSamples(), selectedItems);
    }

    /**
     * Create a {@link VirtualTableQueryFilter} with the given map of parameters
     *
     * @param parameters
     *            Map of parameters
     * @return A {@link VirtualTableQueryFilter} or null if the parameters are invalid
     */
    public static @Nullable VirtualTableQueryFilter createVirtualTableQueryFilter(Map<String, Object> parameters) {
        List<Long> columnRequested = DataProviderParameterUtils.extractLongList(parameters, DataProviderParameterUtils.REQUESTED_COLUMN_IDS_KEY);
        if (columnRequested == null) {
            return null;
        }

        Object indexObject = parameters.get(DataProviderParameterUtils.REQUESTED_TABLE_INDEX_KEY);
        if (!(indexObject instanceof Long) && !(indexObject instanceof Integer)) {
            return null;
        }

        long index = indexObject instanceof Long ? (long) indexObject : ((Integer) indexObject).longValue();

        Object countObject = parameters.get(DataProviderParameterUtils.REQUESTED_TABLE_COUNT_KEY);
        if (!(countObject instanceof Integer)) {
            return null;
        }

        return new VirtualTableQueryFilter(columnRequested, index, (int) countObject);
    }

    /**
     * Convert a given {@link TimeQueryFilter} into a map of parameters
     *
     * @param queryFilter
     *            The query filter
     * @return A map of parameters
     */
    public static Map<String, Object> timeQueryToMap(TimeQueryFilter queryFilter) {
        Map<String, Object> map = new HashMap<>();
        long[] timesRequested = queryFilter.getTimesRequested();
        List<Long> longList = new ArrayList<>();
        for (long time : timesRequested) {
            longList.add(time);
        }
        map.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, longList);
        return map;
    }

    /**
     * Convert a given {@link SelectionTimeQueryFilter} into a map of parameters
     *
     * @param queryFilter
     *            The query filter
     * @return A map of parameters
     */
    public static Map<String, Object> selectionTimeQueryToMap(SelectionTimeQueryFilter queryFilter) {
        Map<String, Object> map = new HashMap<>();
        long[] timesRequested = queryFilter.getTimesRequested();
        Collection<Long> selectedItems = queryFilter.getSelectedItems();
        List<Long> longList = new ArrayList<>();
        for (long time : timesRequested) {
            longList.add(time);
        }
        map.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, longList);
        map.put(DataProviderParameterUtils.REQUESTED_ITEMS_KEY, selectedItems);
        return map;
    }

    /**
     * Convert a given {@link VirtualTableQueryFilter} into a map of parameters
     *
     * @param queryFilter
     *            The query filter
     * @return A map of parameters
     */
    public static Map<String, Object> virtualTableQueryToMap(VirtualTableQueryFilter queryFilter) {
        Map<String, Object> map = new HashMap<>();
        List<Long> columnsId = queryFilter.getColumnsId();
        long index = queryFilter.getIndex();
        int count = queryFilter.getCount();
        map.put(DataProviderParameterUtils.REQUESTED_COLUMN_IDS_KEY, columnsId);
        map.put(DataProviderParameterUtils.REQUESTED_TABLE_INDEX_KEY, index);
        map.put(DataProviderParameterUtils.REQUESTED_TABLE_COUNT_KEY, count);
        return map;
    }

    /**
     * Convert a given {@link FilterTimeQueryFilter} into a map of parameters
     *
     * @param queryFilter
     *            The query filter
     * @return A map of parameters
     */
    public static Map<String, Object> filteredTimeQueryToMap(FilterTimeQueryFilter queryFilter) {
        Map<String, Object> map = new HashMap<>();
        long[] timesRequested = queryFilter.getTimesRequested();
        boolean filtered = queryFilter.isFiltered();
        List<Long> longList = new ArrayList<>();
        for (long time : timesRequested) {
            longList.add(time);
        }
        map.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, longList);
        map.put(DataProviderParameterUtils.FILTERED_KEY, filtered);
        return map;
    }
}
