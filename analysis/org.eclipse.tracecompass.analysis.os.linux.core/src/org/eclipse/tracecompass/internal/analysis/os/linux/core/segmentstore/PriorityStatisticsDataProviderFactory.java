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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.GenericSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.AbstractSegmentStoreStatisticsDataProviderFactory;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor.Builder;

/**
 * A data provider factory for Priority Statistics analysis.
 *
 * @author Hoang Thuan Pham
 */
public class PriorityStatisticsDataProviderFactory extends AbstractSegmentStoreStatisticsDataProviderFactory {
    /**
     * The data provider ID
     */
    public static final String DATA_PROVIDER_ID = "org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityStatisticsDataProvider"; //$NON-NLS-1$

    /**
     * A statistics analysis that groups entries based on the priority of each segment.
     */
    private final class PriorityStatisticsAnalysis extends GenericSegmentStatisticsAnalysis {
        /**
         * Constructor
         *
         * @param secondaryId The ID of the analysis which generates the segment store
         */
        private PriorityStatisticsAnalysis(String secondaryId) {
            super(secondaryId);
        }

        @Override
        protected @Nullable String getSegmentType(@NonNull ISegment segment) {
            if (segment instanceof IPrioritySegment) {
                // The segment type is the priority of the segment
                return NLS.bind(Messages.PriorityStatisticsAnalysis_segmentType, ((IPrioritySegment) segment).getPriority());
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
        return new PriorityStatisticsAnalysis(analysisId);
    }

    @Override
    protected String getDataProviderId() {
        return DATA_PROVIDER_ID;
    }

    @Override
    protected void setStatisticsAnalysisModuleName(AbstractSegmentStatisticsAnalysis statisticsAnalysisModule, IAnalysisModule baseAnalysisModule) {
        statisticsAnalysisModule.setName(Objects.requireNonNull(NLS.bind(Messages.PriorityStatisticsDataProviderFactory_AnalysisName, baseAnalysisModule.getName())));
    }

    @Override
    protected Builder getDataProviderDescriptor(IAnalysisModule analysis) {
        DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
        builder.setId(DATA_PROVIDER_ID + DataProviderConstants.ID_SEPARATOR + analysis.getId())
                .setName(Objects.requireNonNull(NLS.bind(Messages.PriorityStatisticsDataProviderFactory_title, analysis.getName())))
                .setDescription(Objects.requireNonNull(NLS.bind(Messages.PriorityStatisticsDataProviderFactory_description, analysis.getHelpText())))
                .setProviderType(ProviderType.DATA_TREE);
        return builder;
    }
}
