/**********************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Generalized {@link SegmentStoreDensityDataProvider} factory using secondary
 * ID to identify which segment store provider to build it from.
 *
 * @author Puru Jaiswal
 */
public class SegmentStoreDensityDataProviderFactory implements IDataProviderFactory {

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        return null;
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace, String secondaryId) {
        IAnalysisModule m = new SegmentStoreAnalysisModule(trace, secondaryId);
        try {
            m.setTrace(trace);
            String composedId = SegmentStoreDensityDataProvider.ID + ":" + secondaryId; //$NON-NLS-1$
            m.schedule();
            return new SegmentStoreDensityDataProvider(trace, (ISegmentStoreProvider) m, composedId);
        } catch (TmfAnalysisException ex) {
            m.dispose();
            return null;
        }
    }
}
