/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.IHostModel;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * A Call stack function represented as an {@link ISegment}. It's used to build
 * a segments tree based on the state system. The parent represents the caller
 * of the function, and the children list represents its callees.
 *
 * @author Sonia Farrah
 */
public class CalledFunction extends AbstractCalledFunction {

    private static final long serialVersionUID = -6903907365458616473L;

    private final Long fSymbol;

    /**
     * Create a new segment.
     *
     * The end position should be equal to or greater than the start position.
     *
     * @param start
     *            Start position of the segment
     * @param end
     *            End position of the segment
     * @param symbol
     *            The symbol of the call stack function
     * @param processId
     *            The process ID of the traced application
     * @param threadId
     *            The ID of the thread that was running this function
     * @param parent
     *            The caller, can be null for root elements
     * @param model
     *            The operating system model to provide CPU times
     */
    protected CalledFunction(long start, long end, long symbol, int processId, int threadId, @Nullable ICalledFunction parent, IHostModel model) {
        super(start, end, processId, threadId, parent, model);
        fSymbol = symbol;
    }

    @Override
    public @NonNull Long getSymbol() {
        return fSymbol;
    }
}
