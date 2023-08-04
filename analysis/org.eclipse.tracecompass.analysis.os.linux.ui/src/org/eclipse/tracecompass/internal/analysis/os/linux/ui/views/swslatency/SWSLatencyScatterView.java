/*******************************************************************************
 * Copyright (c) 2015, 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.swslatency;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.os.linux.core.swslatency.SWSLatencyAnalysis;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.scatter.AbstractSegmentStoreScatterChartTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;

/**
 * Shows the sched_wakeup / sched_switch latencies in time
 *
 * @author Abdellah Rahmani
 */
public class SWSLatencyScatterView extends TmfChartView {

    /** The view's ID */
    public static final String ID = "org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.swslatency.scatter"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public SWSLatencyScatterView() {
        super(ID);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(@Nullable Composite parent) {
        return new SWSLatencyScatterGraphViewer(Objects.requireNonNull(parent), nullToEmptyString(Messages.SWSLatencyScatterView_title), nullToEmptyString(Messages.SWSLatencyScatterView_xAxis),
                nullToEmptyString(Messages.SWSLatencyScatterView_yAxis));
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new AbstractSegmentStoreScatterChartTreeViewer2(Objects.requireNonNull(parent), SWSLatencyAnalysis.ID);
    }
}
