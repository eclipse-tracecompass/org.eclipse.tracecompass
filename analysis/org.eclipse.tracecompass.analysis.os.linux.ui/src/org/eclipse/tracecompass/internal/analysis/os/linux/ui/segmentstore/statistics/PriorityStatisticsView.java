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

package org.eclipse.tracecompass.internal.analysis.os.linux.ui.segmentstore.statistics;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentsStatisticsView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentsStatisticsViewer;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.IViewDescriptor;

/**
 * A segment store statistics view for showing statistics based on the priority of segments
 * The analysis specified by the secondaryId must create segments that implement IPrioritySegment.
 *
 * @author Hoang Thuan Pham
 */
public class PriorityStatisticsView extends AbstractSegmentsStatisticsView {
    /**
     * ID of this view
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.ui.segmentstore.statistics.priority"; //$NON-NLS-1$

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);
        // Set the title of the view from the actual view ID
        IViewDescriptor desc = PlatformUI.getWorkbench().getViewRegistry().find(getViewId());
        if (desc != null) {
            setPartName(desc.getLabel());
        }
    }

    @Override
    protected @NonNull AbstractSegmentsStatisticsViewer createSegmentStoreStatisticsViewer(@NonNull Composite parent) {
        // The analysis ID is the secondary ID of the view
        String analysisId = NonNullUtils.nullToEmptyString(getViewSite().getSecondaryId());
        return new PriorityStatisticsViewer(parent, analysisId);
    }
}
