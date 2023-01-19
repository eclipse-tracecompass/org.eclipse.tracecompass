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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.IHostModel;

/**
 * Factory to create {@link ICalledFunction}s.
 *
 * @author Matthew Khouzam
 */
public final class CalledFunctionFactory {

    private CalledFunctionFactory() {
        // do nothing
    }

    /**
     * Factory Method for a state value mapped called function
     *
     * @param start
     *            the start time
     * @param end
     *            the end time
     * @param symbolValue
     *            the symbol
     * @param processId
     *            The process ID of the traced application
     * @param threadId
     *            The thread ID of the called function or
     *            {@link IHostModel#UNKNOWN_TID} if not available
     * @param parent
     *            the parent node
     * @param model
     *            The operating system model this function is a part of
     * @return an ICalledFunction with the specified properties
     */
    public static AbstractCalledFunction create(long start, long end, @Nullable Object symbolValue, int processId, int threadId, @Nullable ICalledFunction parent, IHostModel model) {
        if (symbolValue == null) {
            throw new IllegalArgumentException("Symbol value is null"); //$NON-NLS-1$
        }
        if (symbolValue instanceof Number) {
            return create(start, end, ((Number) symbolValue).longValue(), processId, threadId, parent, model);
        }
        return create(start, end, String.valueOf(symbolValue), processId, threadId, parent, model);
    }

    /**
     * Factory method to create a called function with a symbol that is a long
     * integer
     *
     * @param start
     *            the start time
     * @param end
     *            the end time
     * @param value
     *            the symbol
     * @param processId
     *            The process ID of the traced application
     * @param parent
     *            the parent node
     * @return an ICalledFunction with the specified propertiess
     */
    private static CalledFunction create(long start, long end, long value, int processId, int threadId, @Nullable ICalledFunction parent, IHostModel model) {
        if (start > end) {
            throw new IllegalArgumentException(Messages.TimeError + '[' + start + ',' + end + ']');
        }
        return new CalledFunction(start, end, value, processId, threadId, parent, model);
    }

    /**
     * Factory method to create a called function with a symbol that is a
     * {@link String}
     *
     * @param start
     *            the start time
     * @param end
     *            the end time
     * @param value
     *            the symbol
     * @param processId
     *            The process ID of the traced application
     * @param threadId
     *            The thread ID of the called function or
     *            {@link IHostModel#UNKNOWN_TID} if not available
     * @param parent
     *            the parent node
     * @param model
     *            The operating system model this function is a part of
     * @return an ICalledFunction with the specified properties
     */
    public static CalledStringFunction create(long start, long end, String value, int processId, int threadId, @Nullable ICalledFunction parent, IHostModel model) {
        if (start > end) {
            throw new IllegalArgumentException(Messages.TimeError + '[' + start + ',' + end + ']');
        }
        return new CalledStringFunction(start, end, value, processId, threadId, parent, model);
    }
}
