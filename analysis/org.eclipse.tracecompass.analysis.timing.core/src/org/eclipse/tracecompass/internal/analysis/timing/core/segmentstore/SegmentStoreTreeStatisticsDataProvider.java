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
package org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsModel;
import org.eclipse.tracecompass.analysis.timing.core.statistics.ITreeStatistics;
import org.eclipse.tracecompass.analysis.timing.core.statistics.ITreeStatisticsAnalysis;
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
 * This data provider supplies a statistics tree for analysis modules
 * implementing {@link ITreeStatisticsAnalysis}, offering a hierarchical
 * structure of statistics across multiple levels. By passing a
 * {@link FilterTimeQueryFilter}, it also returns statistics for the
 * specified range.
 *
 * @author Siwei Zhang
 * @since 6.1
 */
public class SegmentStoreTreeStatisticsDataProvider extends AbstractSegmentStoreStatisticsDataProvider {

    /**
     * Base prefix for {@link SegmentStoreTreeStatisticsDataProvider}
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreTreeStatisticsDataProvider"; //$NON-NLS-1$

    private final ITreeStatisticsAnalysis fProvider;
    private final @Nullable IAnalysisModule fModule;

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
    public SegmentStoreTreeStatisticsDataProvider(ITmfTrace trace, ITreeStatisticsAnalysis provider, String id) {
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
     *            a list of user-defined aspects that will be added to the
     *            default ones
     */
    public SegmentStoreTreeStatisticsDataProvider(ITmfTrace trace, ITreeStatisticsAnalysis provider, String id, List<IDataAspect<NamedStatistics>> userDefinedAspects) {
        super(trace, id, userDefinedAspects);
        fProvider = provider;
        fModule = provider instanceof IAnalysisModule ? (IAnalysisModule) provider : null;
    }

    @Override
    public TmfModelResponse<TmfTreeModel<SegmentStoreStatisticsModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        IAnalysisModule module = fModule;
        if (module != null) {
            if (monitor != null) {
                module.waitForCompletion(monitor);
                if (monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
            } else {
                module.waitForCompletion();
            }
        }

        ITreeStatistics<ISegment> statsRoot = fProvider.getStatsRoot();
        if (statsRoot == null) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        List<SegmentStoreStatisticsModel> list = new ArrayList<>();
        String rootName = getRootEntryName();
        if (rootName == null) {
            rootName = getTrace().getName();
        }
        /*
         * Add statistics for the full duration, the total would be the first layer.
         */
        list.add(new SegmentStoreStatisticsModel(fTraceId, -1, getCellLabels(NonNullUtils.nullToEmptyString(rootName), statsRoot), statsRoot));
        /*
         * Add entries for the remaining layers.
         */
        addModelsRecursively(statsRoot, list, getUniqueId(TOTAL_PREFIX), fTraceId, TOTAL_PREFIX);


        /*
         * Add statistics for selection, if any.
         */
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        Boolean isFiltered = DataProviderParameterUtils.extractIsFiltered(fetchParameters);
        if (filter != null && isFiltered != null && isFiltered) {
            long start = filter.getStart();
            long end = filter.getEnd();

            IProgressMonitor nonNullMonitor = monitor != null ? monitor : new NullProgressMonitor();
            ITreeStatistics<ISegment> statsRootForRange = fProvider.getStatsRootForRange(start, end, nonNullMonitor);
            if (statsRootForRange == null) {
                return new TmfModelResponse<>(null, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            /*
             * Add entries for the remaining layers for selection.
             */
            addModelsRecursively(statsRootForRange, list, getUniqueId(SELECTION_PREFIX), fTraceId, SELECTION_PREFIX);
        }
        TmfTreeModel.Builder<SegmentStoreStatisticsModel> treeModelBuilder = new TmfTreeModel.Builder();
        treeModelBuilder.setColumnDescriptors(getColumnDescriptors());
        treeModelBuilder.setEntries(Collections.unmodifiableList(list));
        return new TmfModelResponse<>(treeModelBuilder.build(), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    /*
     * Add entries recursively from the root.
     */
    private void addModelsRecursively(ITreeStatistics<ISegment> rootStats, List<SegmentStoreStatisticsModel> list, long currentId, long parentId, String prefix) {
        list.add(new SegmentStoreStatisticsModel(currentId, parentId, getCellLabels(rootStats.getName(), rootStats), rootStats));
        for (ITreeStatistics<ISegment> childStats : rootStats.getChildren()) {
            /*
             * Use both parent's and child's name to generate the unique id, as
             * different children might have the same name.
             */
            long uniqueIdForChild = getUniqueId(prefix + rootStats.getName() + childStats.getName());
            addModelsRecursively(childStats, list, uniqueIdForChild, currentId, prefix);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fModule != null) {
            fModule.dispose();
        }
    }
}
