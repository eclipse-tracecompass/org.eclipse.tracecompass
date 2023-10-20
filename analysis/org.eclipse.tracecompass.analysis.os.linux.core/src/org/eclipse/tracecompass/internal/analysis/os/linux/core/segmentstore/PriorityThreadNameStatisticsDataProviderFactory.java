/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.GenericSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.swslatency.SWSLatencyAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.AbstractSegmentStoreStatisticsDataProviderFactory;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.segment.interfaces.INamedSegment;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;

/**
 * A data provider factory for Priority/Thread name statistics analysis.
 *
 * @author Hoang Thuan Pham
 */
public class PriorityThreadNameStatisticsDataProviderFactory extends AbstractSegmentStoreStatisticsDataProviderFactory {

    /**
     * The data provider ID
     */
    public static final String DATA_PROVIDER_ID = "org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityThreadNameStatisticsDataProvider"; //$NON-NLS-1$

    /**
     * To extend another data provider, create an analysis that extends
     * {@link GenericSegmentStatisticsAnalysis} and override the
     * getSegmentType method again to specify how entries should be grouped for the
     * statistics.
     */
    private final class PriorityThreadNameStatisticsAnalysis extends GenericSegmentStatisticsAnalysis {
        /**
         * Constructor
         *
         * @param secondaryId The ID of the analysis which generates the segment store
         */
        private PriorityThreadNameStatisticsAnalysis(String secondaryId) {
            super(secondaryId);
        }

        @Override
        protected @Nullable String getSegmentType(ISegment segment) {
            if ((segment instanceof INamedSegment) && (segment instanceof IPrioritySegment)) {
                return NLS.bind(Messages.PriorityThreadNameStatisticsAnalysis_segmentType, ((IPrioritySegment) segment).getPriority(), ((INamedSegment) segment).getName());
            }
            return null;
        }
    }

    @Override
    protected AbstractSegmentStatisticsAnalysis getAnalysis(String analysisId) {
        /**
         * Return the {@link AbstractSegmentStatisticsAnalysis} that has the new
         * getSegmentType logic to bind it to the data provider.
         */
        return new PriorityThreadNameStatisticsAnalysis(analysisId);
    }

    @Override
    protected String getDataProviderId() {
        return DATA_PROVIDER_ID;
    }

    @Override
    protected void setStatisticsAnalysisModuleName(AbstractSegmentStatisticsAnalysis statisticsAnalysisModule, IAnalysisModule baseAnalysisModule) {
        statisticsAnalysisModule.setName(Objects.requireNonNull(NLS.bind(Messages.PriorityThreadNameStatisticsDataProviderFactory_AnalysisName, baseAnalysisModule.getName())));
    }

    @Override
    protected @Nullable IDataProviderDescriptor getDataProviderDescriptor(IAnalysisModule analysis) {
        if (analysis instanceof SWSLatencyAnalysis) {
            DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
            builder.setId(DATA_PROVIDER_ID + DataProviderConstants.ID_SEPARATOR + analysis.getId())
                   .setName(Objects.requireNonNull(NLS.bind(Messages.PriorityThreadNameStatisticsDataProviderFactory_title, analysis.getName())))
                   .setDescription(Objects.requireNonNull(NLS.bind(Messages.PriorityThreadNameStatisticsDataProviderFactory_description, analysis.getHelpText())))
                   .setProviderType(ProviderType.DATA_TREE);
            return builder.build();
        }
        return null;
    }
}
