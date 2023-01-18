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

package org.eclipse.tracecompass.internal.analysis.callstack.core.base;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderUtils;

/**
 * A symbol composed of an address for a pid. This symbol can be resolved with
 * symbol providers
 *
 * @author Geneviève Bastien
 */
public class ResolvableSymbol implements ICallStackSymbol {

    private final long fAddr;
    private final int fPid;
    private final long fTime;

    /**
     * Constructor
     *
     * @param addr
     *            The address of the symbol to resolve
     * @param pid
     *            The pid of the process containing this symbol
     * @param timestamp
     *            The timestamp at which this symbol is valid
     */
    public ResolvableSymbol(long addr, int pid, long timestamp) {
        fAddr = addr;
        fPid = pid;
        fTime = timestamp;
    }

    @Override
    public String resolve(@NonNull Collection<@NonNull ISymbolProvider> providers) {
        return SymbolProviderUtils.getSymbolText(providers, fPid, fTime, fAddr);
    }

    @Override
    public int hashCode() {
        // Just hash on the address as it should match the flame chart's hash
        // code
        // FIXME: The same symbol for flame chart and flame graph should be the
        // same. They are, but only because we made sure to convert to Long here
        // before...
        return Long.valueOf(fAddr).hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ResolvableSymbol)) {
            return false;
        }
        ResolvableSymbol other = (ResolvableSymbol) obj;
        return (fAddr == other.fAddr) && (fPid == other.fPid);
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(fAddr) + " in " + fPid; //$NON-NLS-1$//$NON-NLS-2$
    }
}
