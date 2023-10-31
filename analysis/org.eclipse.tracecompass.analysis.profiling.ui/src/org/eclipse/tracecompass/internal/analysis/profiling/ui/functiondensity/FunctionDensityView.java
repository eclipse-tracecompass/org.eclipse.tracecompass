/******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.ui.functiondensity;

import java.util.Objects;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density2.AbstractSegmentStoreDensityView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density2.AbstractSegmentStoreDensityViewer;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.AbstractSegmentStoreTableViewer;
import org.eclipse.tracecompass.tmf.core.segment.SegmentDurationAspect;
import org.eclipse.tracecompass.tmf.core.segment.SegmentEndTimeAspect;
import org.eclipse.tracecompass.tmf.core.segment.SegmentStartTimeAspect;

/**
 * Call stack Density view displaying the call stack segments tree.
 *
 * @author Sonia Farrah
 */
public class FunctionDensityView extends AbstractSegmentStoreDensityView {

    /** The view's ID */
    public static final String ID = "org.eclipse.tracecompass.analysis.profiling.ui.functiondensity"; //$NON-NLS-1$

    /**
     * Constructs a new density view.
     */
    public FunctionDensityView() {
        super(ID);
    }

    @SuppressWarnings("null")
    @Override
    protected AbstractSegmentStoreTableViewer createSegmentStoreTableViewer(Composite parent) {
        return new FunctionTableViewer(new TableViewer(parent, SWT.FULL_SELECTION | SWT.VIRTUAL), getViewSite().getSecondaryId()) {
            @Override
            protected void createProviderColumns() {
                super.createProviderColumns();
                Table t = (Table) getControl();

                moveColumnTo(t, SegmentDurationAspect.SEGMENT_DURATION_ASPECT.getName(), 0);
                moveColumnTo(t, SegmentStartTimeAspect.SEGMENT_START_TIME_ASPECT.getName(), 1);
                moveColumnTo(t, SegmentEndTimeAspect.SEGMENT_END_TIME_ASPECT.getName(), 2);
            }

            private void moveColumnTo(Table t, String aspectName, int desiredIndex) {
                int[] order = t.getColumnOrder();
                int foundIndex = -1;
                for (int i = 0; i < t.getColumnCount(); i++) {
                    TableColumn col = t.getColumn(i);
                    if (col.getText().equals(aspectName)) {
                        foundIndex = i;
                    }
                }
                if (foundIndex == -1) {
                    // At least we tried
                    return;
                }
                int tmp = order[desiredIndex];
                order[desiredIndex] = order[foundIndex];
                order[foundIndex] = tmp;
                t.setColumnOrder(order);
            }
        };
    }

    @SuppressWarnings("null")
    @Override
    protected AbstractSegmentStoreDensityViewer createSegmentStoreDensityViewer(Composite parent) {
        return new FunctionDensityViewer(Objects.requireNonNull(parent), getViewSite().getSecondaryId());
    }
}
