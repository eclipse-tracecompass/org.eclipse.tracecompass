/*******************************************************************************
 * Copyright (c) 2018, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.timing.core.segmentstore;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.segment.interfaces.INamedSegment;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * A generic abstraction of a {@link AbstractSegmentStatisticsAnalysis}. Entries
 * in a trace that have the same key will be aggregated into one entry in the
 * statistics table. The key is specified by the return value of the
 * getSegmentType function. By default, the entries are grouped by the segment
 * name, but the class can be extended to overwrite the default implementation.
 *
 * @author Loic Prieur-Drevon
 * @since 5.5
 */
public class GenericSegmentStatisticsAnalysis extends AbstractSegmentStatisticsAnalysis {
    private final String fSecondaryId;

    /**
     * Constructor
     *
     * @param secondaryId
     *            The secondary analysis id, which this statistics analysis will
     *            be based on.
     */
    public GenericSegmentStatisticsAnalysis(String secondaryId) {
        fSecondaryId = secondaryId;
    }

    @Override
    protected @Nullable String getSegmentType(ISegment segment) {
        if (segment instanceof INamedSegment) {
            return ((INamedSegment) segment).getName();
        }
        return null;
    }

    @Override
    protected @Nullable ISegmentStoreProvider getSegmentStoreProvider(ITmfTrace trace) {
        IAnalysisModule segmentStoreModule = trace.getAnalysisModule(fSecondaryId);
        if (segmentStoreModule instanceof ISegmentStoreProvider) {
            return (ISegmentStoreProvider) segmentStoreModule;
        }
        return null;
    }
}