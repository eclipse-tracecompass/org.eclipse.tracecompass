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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsModel;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatisticsAnalysis;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreStatisticsAspects.NamedStatistics;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.FilterTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * This data provider delivers a statistics tree for an
 * {@link AbstractSegmentStatisticsAnalysis} or any analysis module implementing
 * {@link IStatisticsAnalysis}, featuring a fixed two-level structure. By
 * passing a {@link FilterTimeQueryFilter}, it also returns statistics for the
 * specified range.
 *
 * @author Loic Prieur-Drevon
 * @author Siwei Zhang
 * @since 6.1
 */
public class SegmentStoreStatisticsDataProvider extends AbstractSegmentStoreStatisticsDataProvider {

    /**
     * Base {@link SegmentStoreStatisticsDataProvider} prefix
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider"; //$NON-NLS-1$

    private static final String STATISTICS_SUFFIX = ".statistics"; //$NON-NLS-1$
    private static final Map<IStatisticsAnalysis, SegmentStoreStatisticsDataProvider> PROVIDER_MAP = new WeakHashMap<>();

    private final IStatisticsAnalysis fProvider;
    private final @Nullable IAnalysisModule fModule;

    /**
     * Get an instance of {@link SegmentStoreStatisticsDataProvider} for a trace and
     * provider. Returns a null instance if the ISegmentStoreProvider is null. If
     * the provider is an instance of {@link IAnalysisModule}, the analysis is also
     * scheduled.
     *
     * @param trace
     *            A trace on which we are interested to fetch a model
     * @param module
     *            the ID of the analysis to generate this provider from.
     * @return An instance of SegmentStoreDataProvider. Returns a null if the
     *         ISegmentStoreProvider is null.
     */
    public static synchronized @Nullable SegmentStoreStatisticsDataProvider getOrCreate(ITmfTrace trace, AbstractSegmentStatisticsAnalysis module) {
        // TODO experiment support.
        return PROVIDER_MAP.computeIfAbsent(module, p -> new SegmentStoreStatisticsDataProvider(trace, p, module.getId() + STATISTICS_SUFFIX));
    }

    /**
     * Constructor
     *
     * @param trace
     *            the trace for which this provider will supply info
     * @param provider
     *            the segment statistics module from which to get data
     * @param id
     *            the extension point ID
     */
    public SegmentStoreStatisticsDataProvider(ITmfTrace trace, IStatisticsAnalysis provider, String id) {
        super(trace, id);
        fProvider = provider;
        fModule = provider instanceof IAnalysisModule ? (IAnalysisModule) provider : null;
    }

    /**
     * Constructor
     *
     * @param trace
     *            the trace for which this provider will supply info
     * @param provider
     *            the segment statistics module from which to get data
     * @param id
     *            the extension point ID
     * @param userDefinedAspects
     *            a list of user defined aspects that will be added to the
     *            default ones
     */
    public SegmentStoreStatisticsDataProvider(ITmfTrace trace, IStatisticsAnalysis provider, String id, List<IDataAspect<NamedStatistics>> userDefinedAspects) {
        super(trace, id, userDefinedAspects);
        fProvider = provider;
        fModule = provider instanceof IAnalysisModule ? (IAnalysisModule) provider : null;
    }

    @SuppressWarnings("null")
    @Override
    public TmfModelResponse<TmfTreeModel<SegmentStoreStatisticsModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        IAnalysisModule module = fModule;
        boolean success = true;
        if (module != null) {
            if (monitor != null) {
                success = module.waitForCompletion(monitor);
                if (monitor.isCanceled() && module.getFailureCause() == null) {
                    return new TmfModelResponse<>(null, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
            } else {
                success = module.waitForCompletion();
                if (!success && module.getFailureCause() == null) {
                    return new TmfModelResponse<>(null, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
            }

            if (!success && module.getFailureCause() != null) {
                return new TmfModelResponse<>(null, Status.FAILED, NLS.bind(CommonStatusMessage.ANALYSIS_EXECUTION_FAILED, module.getFailureCause()));
            }
        }

        IStatistics<ISegment> statsTotal = fProvider.getStatsTotal();
        if (statsTotal == null) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        List<SegmentStoreStatisticsModel> list = new ArrayList<>();
        String rootName = getRootEntryName();
        if (rootName == null) {
            rootName = getTrace().getName();
        }
        list.add(new SegmentStoreStatisticsModel(fTraceId, -1, getCellLabels(NonNullUtils.nullToEmptyString(rootName), statsTotal), statsTotal));

        /*
         * Add statistics for the full duration.
         */
        long totalId = getUniqueId(TOTAL_PREFIX);
        list.add(new SegmentStoreStatisticsModel(totalId, fTraceId, getCellLabels(Objects.requireNonNull(Messages.SegmentStoreStatisticsDataProvider_Total), statsTotal), statsTotal));
        Map<String, IStatistics<ISegment>> totalStats = fProvider.getStatsPerType();
        for (Entry<String, IStatistics<ISegment>> entry : totalStats.entrySet()) {
            IStatistics<ISegment> statistics = entry.getValue();
            list.add(new SegmentStoreStatisticsModel(getUniqueId(TOTAL_PREFIX + entry.getKey()), totalId, getCellLabels(entry.getKey(), statistics), statistics));
        }

        /*
         * Add statistics for selection, if any.
         */
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        Boolean isFiltered = DataProviderParameterUtils.extractIsFiltered(fetchParameters);
        if (filter != null && isFiltered != null && isFiltered) {
            long start = filter.getStart();
            long end = filter.getEnd();

            IProgressMonitor nonNullMonitor = monitor != null ? monitor : new NullProgressMonitor();
            IStatistics<ISegment> statsForRange = fProvider.getStatsForRange(start, end, nonNullMonitor);
            if (statsForRange == null) {
                return new TmfModelResponse<>(null, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }

            long selectionId = getUniqueId(SELECTION_PREFIX);
            if (statsForRange.getNbElements() > 0) {
                list.add(new SegmentStoreStatisticsModel(selectionId, fTraceId, getCellLabels(Objects.requireNonNull(Messages.SegmentStoreStatisticsDataProvider_Selection), statsForRange), statsForRange));
                Map<String, IStatistics<ISegment>> selectionStats = fProvider.getStatsPerTypeForRange(start, end, nonNullMonitor);
                for (Entry<String, IStatistics<ISegment>> entry : selectionStats.entrySet()) {
                    IStatistics<ISegment> statistics = entry.getValue();
                    list.add(new SegmentStoreStatisticsModel(getUniqueId(SELECTION_PREFIX + entry.getKey()), selectionId, getCellLabels(entry.getKey(), statistics), statistics));
                }
            }
        }
        TmfTreeModel.Builder<SegmentStoreStatisticsModel> treeModelBuilder = new TmfTreeModel.Builder();
        treeModelBuilder.setColumnDescriptors(getColumnDescriptors());
        treeModelBuilder.setEntries(Collections.unmodifiableList(list));
        return new TmfModelResponse<>(treeModelBuilder.build(), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fModule != null) {
            fModule.dispose();
        }
    }
}
