/**********************************************************************
 * Copyright (c) 2018, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsModel;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreStatisticsAspects.NamedStatistics;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.TableColumnDescriptor;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.ITableColumnDescriptor;
import org.eclipse.tracecompass.tmf.core.model.filters.FilterTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * An abstract data provider for segment store statistics. This data provider
 * will supply a statistics tree using {@link SegmentStoreStatisticsModel}. By
 * passing a {@link FilterTimeQueryFilter}, it also returns statistics for the
 * specified range.
 *
 * @author Loic Prieur-Drevon
 * @author Siwei Zhang
 * @since 6.1
 */
public abstract class AbstractSegmentStoreStatisticsDataProvider extends AbstractTmfTraceDataProvider
        implements ITmfTreeDataProvider<SegmentStoreStatisticsModel> {

    /**
     * The prefix for statistics for full duration.
     */
    protected static final String TOTAL_PREFIX = "Total_"; //$NON-NLS-1$
    /**
     * The prefix for statistics for selection.
     */
    protected static final String SELECTION_PREFIX = "Selection_"; //$NON-NLS-1$
    private static final AtomicLong ENTRY_ID = new AtomicLong();

    private final String fId;
    private @Nullable String fRootEntryName;

    private final Map<String, Long> fIdToType = new HashMap<>();
    /**
     * The entry id for the trace.
     */
    protected final long fTraceId = ENTRY_ID.getAndIncrement();

    private SegmentStoreStatisticsAspects fAspects = new SegmentStoreStatisticsAspects();

    /**
     * Constructor
     *
     * @param trace
     *            the trace for which this provider will supply info
     * @param id
     *            the extension point ID
     */
    protected AbstractSegmentStoreStatisticsDataProvider(ITmfTrace trace, String id) {
        super(trace);
        fId = id;
    }

    /**
     * Constructor
     *
     * @param trace
     *            the trace for which this provider will supply info
     * @param id
     *            the extension point ID
     * @param userDefinedAspects
     *            a list of user-defined aspects that will be added to the
     *            default ones
     */
    protected AbstractSegmentStoreStatisticsDataProvider(ITmfTrace trace, String id, List<IDataAspect<NamedStatistics>> userDefinedAspects) {
        this(trace, id);
        fAspects = new SegmentStoreStatisticsAspects(userDefinedAspects);
    }

    /**
     * Gets the list of column descriptors.
     *
     * @return list of column descriptors
     */
    protected List<ITableColumnDescriptor> getColumnDescriptors() {
        ImmutableList.Builder<ITableColumnDescriptor> headers = new ImmutableList.Builder<>();
        for (IDataAspect<NamedStatistics> aspect : fAspects.getAspects()) {
            TableColumnDescriptor.Builder builder = new TableColumnDescriptor.Builder();
            builder.setText(Objects.requireNonNull(aspect.getName()));
            builder.setTooltip(Objects.requireNonNull(aspect.getHelpText()));
            if (aspect instanceof ITypedDataAspect) {
                builder.setDataType(((ITypedDataAspect<?>) aspect).getDataType());
            }
            headers.add(builder.build());
        }
        return headers.build();
    }

    /**
     * Returns a list of cell labels.
     *
     * @param name
     *            the name value of the label column per row. Used as is.
     * @param statistics
     *            the {@link IStatistics} implementation to get the cell statistics labels from
     * @return the list of cell label
     */
    protected List<String> getCellLabels(String name, IStatistics<ISegment> statistics) {
        NamedStatistics namedStatistics = new NamedStatistics(name, statistics);
        ImmutableList.Builder<String> labels = new ImmutableList.Builder<>();
        for (IDataAspect<NamedStatistics> aspect : fAspects.getAspects()) {
            labels.add(NonNullUtils.nullToEmptyString(aspect.apply(namedStatistics)));
        }
        return labels.build();
    }

    /**
     * Returns the unique id for the name of the entry.
     *
     * @param name
     *            the name of the entry
     * @return an id that is unique for each name
     */
    protected long getUniqueId(String name) {
        synchronized (fIdToType) {
            return fIdToType.computeIfAbsent(name, n -> ENTRY_ID.getAndIncrement());
        }
    }

    @Override
    public String getId() {
        return fId;
    }

    /**
     * Sets the root entry name to apply. If null, then the trace name will be
     * used (default).
     *
     * @param rootName
     *            the root entry name to apply.
     */
    public void setRootEntryName(@Nullable String rootName) {
        fRootEntryName = rootName;
    }

    /**
     * Gets the root entry name to apply.
     *
     * @return the root entry name to apply.
     */
    public final @Nullable String getRootEntryName() {
        return fRootEntryName;
    }

    /**
     * Set a mapper function to convert a statistics Number to String.
     * Used for minimum, maximum, average, standard deviation and total.
     *
     * @param mapper
     *              function to convert a Number to String
     * @since 5.2
     */
    public void setMapper(Function<Number, String> mapper) {
        fAspects.setMapper(mapper);
    }

    /**
     * Sets a mapper function to format a label string to an output string.
     *
     * @param mapper
     *                function to map input string to output string. This can
     *                be used, for example, to change a symbol address to a
     *                symbol name.
     * @since 5.2
     */
    public void setLabelMapper(UnaryOperator<String> mapper) {
        fAspects.setLabelMapper(mapper);
    }

    @Override
    public void dispose() {
        synchronized (fIdToType) {
            fIdToType.clear();
        }
    }
}
