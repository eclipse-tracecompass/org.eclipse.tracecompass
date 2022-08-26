/*******************************************************************************
 * Copyright (c) 2016 EfficiOS Inc. and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.ust.core.analysis.debuginfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo.BinaryCallsite;
import org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo.FunctionLocation;
import org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo.UstDebugInfoAnalysisModule;
import org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo.UstDebugInfoBinaryAspect;
import org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo.UstDebugInfoFunctionAspect;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.symbols.DefaultSymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.symbols.TmfResolvedSymbol;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Symbol provider for UST traces with debug information.
 *
 * @author Alexandre Montplaisir
 * @see UstDebugInfoAnalysisModule
 */
public class UstDebugInfoSymbolProvider extends DefaultSymbolProvider {

    private final List<org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider> fOtherProviders = new ArrayList<>();

    /**
     * Create a new {@link UstDebugInfoSymbolProvider} for the given trace
     *
     * @param trace
     *            A non-null trace
     */
    public UstDebugInfoSymbolProvider(LttngUstTrace trace) {
        super(trace);
    }

    /**
     * Sets the configured path prefix. Usually called from the preferences
     * page.
     *
     * @param newPathPrefix
     *            The new path prefix to use
     */
    void setConfiguredPathPrefix(LttngUstTrace.SymbolProviderConfig newConfig) {
        getTrace().setSymbolProviderConfig(newConfig);
    }

    @Override
    public void loadConfiguration(@Nullable IProgressMonitor monitor) {
        super.loadConfiguration(monitor);
        // Get all the symbol providers that are not of default class
        for (org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider provider : SymbolProviderManager.getInstance().getSymbolProviders(getTrace())) {
            if (!(provider instanceof DefaultSymbolProvider)) {
                fOtherProviders.add(provider);
            }
        }
    }

    @Override
    public @NonNull LttngUstTrace getTrace() {
        /* Type enforced at constructor */
        return (LttngUstTrace) super.getTrace();
    }

    @Override
    public @Nullable TmfResolvedSymbol getSymbol(int pid, long timestamp, long address) {
        LttngUstTrace trace = getTrace();
        BinaryCallsite bc = UstDebugInfoBinaryAspect.getBinaryCallsite(trace, pid, timestamp, address);
        if (bc == null) {
            return null;
        }

        String functionName = getFunctionNameFromSS(bc, trace);
        /*
         * Return function information only if it is non null, otherwise try to
         * resolve the symbol in another way (see code below).
         */
        if (functionName != null) {
            return new TmfResolvedSymbol(bc.getOffset(), functionName);
        }

        FunctionLocation loc = UstDebugInfoFunctionAspect.getFunctionFromBinaryLocation(bc);
        if (loc != null) {
            return new TmfResolvedSymbol(bc.getOffset(), loc.getFunctionName());
        }
        // Try to see if some other symbol provider has a symbol for this
        // relative binary callsite
        // FIXME: Ideally, it would be good to be able to specify the filename
        for (org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider provider : fOtherProviders) {
            TmfResolvedSymbol symbol = provider.getSymbol(pid, timestamp, bc.getOffset());
            if (symbol != null) {
                return symbol;
            }
        }
        return new TmfLibrarySymbol(bc.getOffset(), bc.getBinaryFilePath());
    }

    /**
     * Try to resolve the function name using information stored in the SS of
     * the {@link UstDebugInfoAnalysisModule}
     *
     * @param bc
     *            the {@link BinaryCallsite}, containing information
     *            corresponding to an instruction pointer
     * @param trace
     *            the lttng ust trace of the monitored application
     * @return the function name, or null if not found
     */
    public static @Nullable String getFunctionNameFromSS(BinaryCallsite bc, ITmfTrace trace) {
        Iterator<UstDebugInfoAnalysisModule> ustDebugModules = TmfTraceUtils.getAnalysisModulesOfClass(trace, UstDebugInfoAnalysisModule.class).iterator();
        if (ustDebugModules.hasNext()) {
            UstDebugInfoAnalysisModule ustDebugModule = ustDebugModules.next();
            ITmfStateSystem ustDebugSsb = ustDebugModule.getStateSystem();
            if (ustDebugSsb != null) {
                String binFilePath = bc.getBinaryFilePath();
                long offset = bc.getOffset() + ustDebugSsb.getStartTime();
                try {
                    int functionNameQuark = ustDebugSsb.getQuarkAbsolute(binFilePath, UstDebugInfoStateProvider.FUNCTION_NAME);
                    ITmfStateInterval functionInterval = ustDebugSsb.querySingleState(offset, functionNameQuark);
                    return functionInterval.getValueString();
                } catch (AttributeNotFoundException e) {
                    /*
                     * It's fine, sometimes a function could not be found with
                     * the info stored in the SS using nm. Try to resolve the
                     * symbol in another way.
                     */
                } catch (StateSystemDisposedException e) {
                    /*
                     * This can happen if user closes a trace during the
                     * analysis execution. Continue with what we have.
                     */
                }
            }
        }
        return null;
    }
}
