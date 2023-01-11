/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Objects;

/**
 * A class that associates a callstack with a depth, to abstract the state
 * system accesses.
 *
 * @author Geneviève Bastien
 */
public class CallStackDepth {

    private final CallStack fCallstack;
    private final int fDepth;

    /**
     * Constructor. The caller must make sure that the callstack has the
     * requested depth.
     *
     * @param callstack
     *            The callstack
     * @param depth
     *            The depth of the callstack
     */
    public CallStackDepth(CallStack callstack, int depth) {
        fCallstack = callstack;
        fDepth = depth;
    }

    /**
     * Get the quark corresponding to this callstack depth
     *
     * @return The quark at this depth
     */
    public int getQuark() {
        return fCallstack.getQuarkAtDepth(fDepth);
    }

    /**
     * Get the callstack corresponding to this callstack depth
     *
     * @return The callstack
     */
    public CallStack getCallStack() {
        return fCallstack;
    }

    /**
     * Get the depth in the callstack this object represents
     *
     * @return The depth in the callstack
     */
    public int getDepth() {
        return fDepth;
    }

    @Override
    public int hashCode() {
        // Compare with the actual callstack object, as the callstack's own hash
        // may change as the callstack is built. Here, we are looking for the
        // same object, no matter its content
        return Objects.hashCode(System.identityHashCode(fCallstack), fDepth);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof CallStackDepth)) {
            return false;
        }
        CallStackDepth csd = (CallStackDepth) obj;
        // Compare with the actual callstack object, as the callstack's own hash
        // may change as the callstack is built. Here, we are looking for the
        // same object, no matter its content
        return Objects.equal(System.identityHashCode(fCallstack), System.identityHashCode(csd.fCallstack)) && (fDepth == csd.fDepth);
    }

    @Override
    public String toString() {
        return fCallstack + " at depth " + fDepth; //$NON-NLS-1$
    }
}
