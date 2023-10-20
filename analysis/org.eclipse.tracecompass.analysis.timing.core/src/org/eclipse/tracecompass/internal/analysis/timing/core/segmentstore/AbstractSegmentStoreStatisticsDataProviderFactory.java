/**********************************************************************
 * Copyright (c) 2018, 2023 Ericsson
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.GenericSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * An abstract data provider factory for segment store statistics. To extend a
 * new data provider factory, create an
 * {@link GenericSegmentStatisticsAnalysis} and override the
 * getSegmentType function to specify how entries should be grouped for the
 * statistics. Then, bind the statistics analysis to the data provider by returning an
 * instance of the analysis using the getAnalysis function.
 *
 * @author Loic Prieur-Drevon
 */
public abstract class AbstractSegmentStoreStatisticsDataProviderFactory implements IDataProviderFactory {

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        return null;
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace, String secondaryId) {

        IAnalysisModule baseAnalysisModule = trace.getAnalysisModule(secondaryId);
        String composedId = getDataProviderId() + ':' + secondaryId;
        // check that this trace has the queried analysis.
        if (!(baseAnalysisModule instanceof ISegmentStoreProvider)) {
            if (!(trace instanceof TmfExperiment)) {
                return null;
            }
            return TmfTreeCompositeDataProvider.create(TmfTraceManager.getTraceSet(trace), composedId);
        }
        baseAnalysisModule.schedule();

        AbstractSegmentStatisticsAnalysis statisticsAnalysisModule = getAnalysis(secondaryId);
        try {
            setStatisticsAnalysisModuleName(statisticsAnalysisModule, baseAnalysisModule);
            statisticsAnalysisModule.setTrace(trace);
        } catch (TmfAnalysisException e) {
            statisticsAnalysisModule.dispose();
            return null;
        }
        statisticsAnalysisModule.schedule();
        return new SegmentStoreStatisticsDataProvider(trace, statisticsAnalysisModule, composedId);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        Iterable<ISegmentStoreProvider> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, ISegmentStoreProvider.class);
        List<IDataProviderDescriptor> descriptors = new ArrayList<>();
        Set<String> existingModules = new HashSet<>();
        for (ISegmentStoreProvider module : modules) {
            IAnalysisModule analysis = (IAnalysisModule) module;
            // Only add analysis once per trace (which could be an experiment)
            if (!existingModules.contains(analysis.getId())) {
                IDataProviderDescriptor descriptor = getDataProviderDescriptor(analysis);
                if (descriptor != null) {
                    descriptors.add(descriptor);
                    existingModules.add(analysis.getId());
                }
            }
        }
        return descriptors;
    }

    /**
     * Get a {@link AbstractSegmentStatisticsAnalysis} that generates the
     * statistics for another analysis which is identified by the secondaryId
     * parameter. The statistics analysis should implement the getSegmentType method
     * to specify how entries should be grouped for the statistics.
     *
     * @param secondaryId
     *            The ID of the analysis which the returned
     *            {@link AbstractSegmentStatisticsAnalysis} is based on.
     * @return An {@link AbstractSegmentStatisticsAnalysis} that is based on the
     *         analysis that is identified by the secondary id.
     */
    protected abstract AbstractSegmentStatisticsAnalysis getAnalysis(String secondaryId);

    /**
     * Get the ID of the data provider to create.
     *
     * @return the ID of the data provider to create
     */
    protected abstract String getDataProviderId();

    /**
     * Set the statistics analysis module name.
     *
     * @param statisticsAnalysisModule
     *            The statistics analysis module
     * @param baseAnalysisModule
     *            The base module, which the statistics analysis is based on
     */
    protected abstract void setStatisticsAnalysisModuleName(AbstractSegmentStatisticsAnalysis statisticsAnalysisModule, IAnalysisModule baseAnalysisModule);

    /**
     * Get the data provider descriptor. Only return a descriptor if the
     * analysis is applicable for that factory.
     *
     * @param analysis
     *            The base analysis on which the statistics are based on.
     * @return A {@link IDataProviderDescriptor} that contains the data provider
     *         information or null if not applicable for this analysis module
     */
    protected abstract @Nullable IDataProviderDescriptor getDataProviderDescriptor(IAnalysisModule analysis);
}
