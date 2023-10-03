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

package org.eclipse.tracecompass.analysis.os.linux.ui.segmentstore.statistics;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentsStatisticsViewer;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityStatisticsDataProviderFactory;

/**
 * A segment store statistics viewer for showing statistics based on the priority
 * of segments.
 *
 * @author Hoang Thuan Pham
 */
public class PriorityStatisticsViewer extends AbstractSegmentsStatisticsViewer {
    /**
     * Constructor
     *
     * @param parent
     *            The parent composite
     * @param analysisId
     *            The ID of the segment store provider to do statistics on
     */
    public PriorityStatisticsViewer(Composite parent, String analysisId) {
        super(parent, PriorityStatisticsDataProviderFactory.DATA_PROVIDER_ID + ':' + analysisId);
    }
}
