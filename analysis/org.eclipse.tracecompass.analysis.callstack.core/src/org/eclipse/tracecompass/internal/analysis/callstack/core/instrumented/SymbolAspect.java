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

package org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented;

import java.util.Collection;
import java.util.Comparator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.callstack.core.ICalledFunction;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderUtils;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/**
 * An aspect used to get the function name of a call stack event or to compare
 * the duration of two events
 *
 * @author Sonia Farrah
 */
public final class SymbolAspect implements ISegmentAspect {
    /**
     * A symbol aspect
     */
    public static final ISegmentAspect SYMBOL_ASPECT = new SymbolAspect();

    /**
     * Constructor
     */
    public SymbolAspect() {
        // Empty
    }

    @Override
    public @NonNull String getName() {
        return String.valueOf("Function name"); //$NON-NLS-1$
    }

    @Override
    public @NonNull String getHelpText() {
        return String.valueOf("Function name"); //$NON-NLS-1$
    }

    @Override
    public @Nullable Comparator<?> getComparator() {
        return new Comparator<ISegment>() {
            @Override
            public int compare(@Nullable ISegment o1, @Nullable ISegment o2) {
                if (o1 == null || o2 == null) {
                    throw new IllegalArgumentException();
                }
                return Long.compare(o1.getLength(), o2.getLength());
            }
        };
    }

    @Override
    public @Nullable Object resolve(@NonNull ISegment segment) {
        if (segment instanceof ICalledFunction) {
            ICalledFunction calledFunction = (ICalledFunction) segment;
            ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
            if (trace != null) {
                Object symbol = calledFunction.getSymbol();
                if (symbol instanceof Long) {
                    Long longAddress = (Long) symbol;
                    Collection<ISymbolProvider> providers = SymbolProviderManager.getInstance().getSymbolProviders(trace);

                    // look for a symbol for a given process
                    long time = segment.getStart();
                    int pid = calledFunction.getProcessId();
                    if (pid > 0) {
                        return SymbolProviderUtils.getSymbolText(providers, pid, time, longAddress);
                    }

                    return SymbolProviderUtils.getSymbolText(providers, longAddress);
                }
                return String.valueOf(symbol);
            }
        }
        return null;
    }
}
