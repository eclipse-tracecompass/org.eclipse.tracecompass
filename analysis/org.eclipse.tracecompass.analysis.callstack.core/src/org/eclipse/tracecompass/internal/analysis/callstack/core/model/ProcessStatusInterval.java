/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.model;

import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * A segment representing the status of a process for a certain duration
 *
 * @author Geneviève Bastien
 */
public class ProcessStatusInterval implements ISegment {

    /**
     * Generated serial version
     */
    private static final long serialVersionUID = -1304201837498841077L;

    private final long fStartTime;
    private final long fEndTime;
    private final ProcessStatus fStatus;

    /**
     * Constructor
     *
     * @param start
     *            The start time of this interval
     * @param end
     *            The end time of this interval
     * @param status
     *            The status of this interval
     */
    public ProcessStatusInterval(long start, long end, ProcessStatus status) {
        fStartTime = start;
        fEndTime = end;
        fStatus = status;
    }

    @Override
    public long getStart() {
        return fStartTime;
    }

    @Override
    public long getEnd() {
        return fEndTime;
    }

    /**
     * Get the process status represented by this interval
     *
     * @return The status of this interval
     */
    public ProcessStatus getProcessStatus() {
        return fStatus;
    }
}
