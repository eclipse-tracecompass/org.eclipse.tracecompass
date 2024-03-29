/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.tests.stubs2;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICalledFunction;

/**
 * A Stub for the CalledFunction segment.
 *
 * @author Arnaud Fiorini
 */
public class CalledFunctionStub implements ICalledFunction {

    private static final long serialVersionUID = 1L;
    private final long fStart;
    private final long fEnd;

    /**
     * Constructor
     *
     * @param start
     *            The start of the segment
     * @param end
     *            The end of the segment
     */
    public CalledFunctionStub(long start, long end) {
        fStart = start;
        fEnd = end;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public long getStart() {
        return fStart;
    }

    @Override
    public long getEnd() {
        return fEnd;
    }

    @Override
    public Object getSymbol() {
        return "";
    }

    @Override
    public @Nullable ICalledFunction getParent() {
        return null;
    }

    @Override
    public long getSelfTime() {
        return fEnd - fStart;
    }

    @Override
    public long getCpuTime() {
        return fEnd - fStart;
    }

    @Override
    public int getProcessId() {
        return 0;
    }

    @Override
    public int getThreadId() {
        return 0;
    }
}
