/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.tests.stubs.trace;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfTraceIndexer;

/**
 * @see TmfExperimentStub
 */
public class TmfExperimentSyslog extends TmfExperiment {

    /**
     * Default constructor. Should not be called directly by the code, but
     * needed for the extension point.
     *
     * Do not call this directly (but do not remove it either!)
     */
    public TmfExperimentSyslog() {
        super();
    }

    @Override
    protected ITmfTraceIndexer createIndexer(int interval) {
        return new TmfIndexerStub(this, interval);
    }

    @Override
    public TmfIndexerStub getIndexer() {
        return (TmfIndexerStub) super.getIndexer();
    }

    /**
     * Make this specific stub meant to support traces with at least one
     * prefixed with "syslog".
     */
    @Override
    public IStatus validateWithTraces(List<ITmfTrace> traces) {
        if (getClass() == TmfExperimentSyslog.class) {
            int confidence = 0;
            for (ITmfTrace trace : traces) {
                if (trace.getName().startsWith("syslog")) {
                    confidence = DEFAULT_GENERIC_EXPERIMENT_CONFIDENCE;
                } else if (trace.getName().startsWith("E-")) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "stubbed error case"); //$NON-NLS-1$
                }
            }
            return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "class extends TmfExperimentSyslog"); //$NON-NLS-1$
    }
}
