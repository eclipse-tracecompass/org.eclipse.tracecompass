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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.density2.AbstractSegmentStoreDensityViewer;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Call stack segments density viewer
 *
 * @author Sonia Farrah
 */
public class FunctionDensityViewer extends AbstractSegmentStoreDensityViewer {
    private final String fAnalysisId;

    /**
     * Constructs a new density viewer.
     *
     * @param parent
     *            the parent of the viewer
     * @param analysisId
     *            The ID of the analysis for this view
     */
    public FunctionDensityViewer(Composite parent, String analysisId) {
        super(parent);
        fAnalysisId = analysisId;
    }

    @Override
    protected @Nullable ISegmentStoreProvider getSegmentStoreProvider(ITmfTrace trace) {
        IAnalysisModule modules = trace.getAnalysisModule(fAnalysisId);
        if (!(modules instanceof ISegmentStoreProvider)) {
            return null;
        }
        return (ISegmentStoreProvider) modules;
    }
}
