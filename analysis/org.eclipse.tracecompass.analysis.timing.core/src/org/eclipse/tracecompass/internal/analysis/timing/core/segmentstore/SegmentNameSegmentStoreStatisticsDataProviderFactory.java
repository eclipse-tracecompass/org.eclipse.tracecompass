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

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.GenericSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;

/**
 * Generalized {@link SegmentStoreStatisticsDataProvider} factory using
 * secondary ID to identify which segment store provider to build it from.
 * Entries in this data set are grouped by segment name.
 *
 * @author Loic Prieur-Drevon
 * @author Hoang Thuan Pham
 * @since 4.0
 */
public class SegmentNameSegmentStoreStatisticsDataProviderFactory extends AbstractSegmentStoreStatisticsDataProviderFactory {

    @Override
    protected AbstractSegmentStatisticsAnalysis getAnalysis(String secondaryId) {
        return new GenericSegmentStatisticsAnalysis(secondaryId);
    }

    @Override
    protected String getDataProviderId() {
        return SegmentStoreStatisticsDataProvider.ID;
    }

    @Override
    protected void setStatisticsAnalysisModuleName(AbstractSegmentStatisticsAnalysis statisticsAnalysisModule, IAnalysisModule baseAnalysisModule) {
        statisticsAnalysisModule.setName(Objects.requireNonNull(NLS.bind(Messages.SegmentStoreStatisticsDataProviderFactory_AnalysisName, baseAnalysisModule.getName())));
    }

    @Override
    protected @Nullable IDataProviderDescriptor getDataProviderDescriptor(IAnalysisModule analysis) {
        DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
        builder.setId(SegmentStoreStatisticsDataProvider.ID + DataProviderConstants.ID_SEPARATOR + analysis.getId())
                .setName(Objects.requireNonNull(NLS.bind(Messages.SegmentStoreStatisticsDataProvider_title, analysis.getName())))
                .setDescription(Objects.requireNonNull(NLS.bind(Messages.SegmentStoreStatisticsDataProvider_description, analysis.getHelpText())))
                .setProviderType(ProviderType.DATA_TREE)
                .setCreationConfiguration(analysis.getConfiguration());
        return builder.build();
    }
}
