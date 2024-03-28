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

package org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2;

import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICalledFunction;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.SymbolAspect;
import org.eclipse.tracecompass.analysis.profiling.core.model.IHostModel;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.SegmentComparators;

import com.google.common.collect.Ordering;

/**
 * Called Function common class, defines the start, end, depth, parent and
 * children. Does not define the symbol
 *
 * @author Matthew Khouzam
 * @author Sonia Farrah
 */
abstract class AbstractCalledFunction implements ICalledFunction {

    private static final Comparator<ISegment> COMPARATOR;
    static {
        /*
         * requireNonNull() has to be called separately, or else it breaks the
         * type inference.
         */
        Comparator<ISegment> comp = Ordering.from(SegmentComparators.INTERVAL_START_COMPARATOR).compound(SegmentComparators.INTERVAL_END_COMPARATOR);
        COMPARATOR = Objects.requireNonNull(comp);
    }

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = -7585356855392065628L;

    protected final long fStart;
    protected final long fEnd;
    protected long fSelfTime = 0;

    private final @Nullable ICalledFunction fParent;
    private final int fProcessId;
    private final int fThreadId;

    private final transient IHostModel fModel;
    private transient long fCpuTime = Long.MIN_VALUE;

    protected AbstractCalledFunction(long start, long end, int processId, int threadId, @Nullable ICalledFunction parent, IHostModel model) {
        if (start > end) {
            throw new IllegalArgumentException(Messages.TimeError + "[" + start + "," + end + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        fStart = start;
        fEnd = end;
        fParent = parent;
        // It'll be modified once we add a child to it
        fSelfTime = fEnd - fStart;
        fProcessId = processId;
        fThreadId = threadId;
        if (parent instanceof AbstractCalledFunction) {
            ((AbstractCalledFunction) parent).addChild(this);
        }
        fModel = model;
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
    public @Nullable ICalledFunction getParent() {
        return fParent;
    }

    @Override
    public String getName() {
        return NonNullUtils.nullToEmptyString(SymbolAspect.SYMBOL_ASPECT.resolve(this));
    }

    /**
     * Add the child to the segment's children, and subtract the child's
     * duration to the duration of the segment so we can calculate its self
     * time.
     *
     * @param child
     *            The child to add to the segment's children
     */
    protected void addChild(ICalledFunction child) {
        if (child.getParent() != this) {
            throw new IllegalArgumentException("Child parent not the same as child being added to."); //$NON-NLS-1$
        }
        substractChildDuration(child.getEnd() - child.getStart());
    }

    /**
     * Subtract the child's duration to the duration of the segment.
     *
     * @param childDuration
     *            The child's duration
     */
    private void substractChildDuration(long childDuration) {
        fSelfTime -= childDuration;
    }

    @Override
    public long getSelfTime() {
        return fSelfTime;
    }

    @Override
    public long getCpuTime() {
        long cpuTime = fCpuTime;
        if (cpuTime == Long.MIN_VALUE) {
            cpuTime = fModel.getCpuTime(fThreadId, fStart, fEnd);
            fCpuTime = cpuTime;
        }
        return cpuTime;
    }

    @Override
    public int getProcessId() {
        return fProcessId;
    }

    @Override
    public int getThreadId() {
        return fThreadId;
    }

    @Override
    public int compareTo(@Nullable ISegment o) {
        if (o == null) {
            throw new IllegalArgumentException();
        }
        return COMPARATOR.compare(this, o);
    }

    @Override
    public String toString() {
        return '[' + String.valueOf(fStart) + ", " + String.valueOf(fEnd) + ']' + " Duration: " + getLength() + ", Self Time: " + fSelfTime; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public int hashCode() {
        return Objects.hash(fEnd, fParent, fSelfTime, fStart, getSymbol());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractCalledFunction other = (AbstractCalledFunction) obj;
        return (fEnd == other.fEnd &&
                fSelfTime == other.fSelfTime &&
                fStart == other.fStart &&
                Objects.equals(fParent, other.getParent()) &&
                Objects.equals(getSymbol(), other.getSymbol()));
    }
}
