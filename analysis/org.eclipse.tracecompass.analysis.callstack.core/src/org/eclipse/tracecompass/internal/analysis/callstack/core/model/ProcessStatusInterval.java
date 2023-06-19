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

import org.eclipse.jdt.annotation.Nullable;
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
    private final int fThreadId;
    private final ProcessStatus fStatus;
    private final @Nullable String fSyscallName;

    /**
     * Constructor
     *
     * @param start
     *            The start time of this interval
     * @param end
     *            The end time of this interval
     * @param threadId
     *            The thread id of this interval
     * @param status
     *            The status of this interval
     * @param syscallName
     *            The syscall name if necessary
     */
    public ProcessStatusInterval(long start, long end, int threadId, ProcessStatus status, @Nullable String syscallName) {
        fStartTime = start;
        fEndTime = end;
        fThreadId = threadId;
        fStatus = status;
        fSyscallName = syscallName;
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
     * Gets the thread id represented by this interval
     *
     * @return The thread id
     */
    public int getThreadId() {
        return fThreadId;
    }

    /**
     * Get the process status represented by this interval
     *
     * @return The status of this interval
     */
    public ProcessStatus getProcessStatus() {
        return fStatus;
    }

    /**
     * Get the syscall name represented by this interval
     *
     * @return The syscall name for this interval
     */
    public @Nullable String getSyscallName() {
        return fSyscallName;
    }
}
