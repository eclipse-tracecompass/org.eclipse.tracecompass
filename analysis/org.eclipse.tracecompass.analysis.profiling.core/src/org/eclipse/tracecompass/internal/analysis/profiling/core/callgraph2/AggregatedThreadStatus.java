/*******************************************************************************
 * Copyright (c) 2016, 2017 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2;

import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.internal.analysis.profiling.core.base.StringSymbol;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ProcessStatusInterval;

/**
 * Class to calculate statistics for an aggregated function.
 *
 * @author Geneviève Bastien
 */
public class AggregatedThreadStatus extends AggregatedCallSite {

    private final ProcessStatus fStatus;

    /**
     * Constructor
     *
     * @param status
     *            the process status
     */
    public AggregatedThreadStatus(ProcessStatus status) {
        super(new StringSymbol(status), 0);
        fStatus = status;
    }

    /**
     * Constructor
     *
     * @param status
     *            the aggregated thread status
     */
    public AggregatedThreadStatus(AggregatedThreadStatus status) {
        super(status);
        fStatus = status.fStatus;
    }

    /**
     * Update length
     *
     * @param interval
     *            the interval
     */
    public void update(ProcessStatusInterval interval) {
        addToWeight(interval.getLength());
    }

    /**
     * Get the process status
     *
     * @return the process status
     */
    public ProcessStatus getProcessStatus() {
        return fStatus;
    }

    @Override
    public String toString() {
        return "Aggregated Thread status for " + fStatus + ": " + getWeight(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public AggregatedCallSite copyOf() {
        return new AggregatedThreadStatus(this);
    }
}
