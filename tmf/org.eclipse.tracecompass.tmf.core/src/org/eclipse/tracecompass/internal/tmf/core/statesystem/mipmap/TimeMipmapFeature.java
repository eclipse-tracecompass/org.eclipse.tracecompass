/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.core.statesystem.mipmap;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue.Type;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;

/**
 * A time-based mipmap feature that promotes levels based on power-of-10 time
 * window widths rather than interval count. Each level aggregates over a time
 * window that is 10× wider than the previous level:
 *
 * <pre>
 * Level 1: 10 ns windows
 * Level 2: 100 ns windows
 * Level 3: 1 µs windows
 * Level 4: 10 µs windows
 * ...
 * </pre>
 *
 * This ensures mipmap levels align with natural time units regardless of event
 * density. The maximum value within each time window is stored at the
 * corresponding level.
 *
 * @author Trace Compass contributors
 */
public class TimeMipmapFeature implements ITmfMipmapFeature {

    /** The base time window width for level 1 (10 ns) */
    private static final long BASE_WINDOW_NS = 10L;

    /** Maximum number of levels (10^18 ns = ~31 years, more than enough) */
    private static final int MAX_LEVELS = 18;

    private final int fBaseQuark;
    private final int fMipmapQuark;
    private final ITmfStateSystemBuilder fSs;
    private final List<Integer> fLevelQuarks = new ArrayList<>();

    /** Per-level current max value */
    private final ITmfStateValue[] fLevelMax;
    /** Per-level window start time */
    private final long[] fLevelWindowStart;
    /** Whether each level has been initialized */
    private final boolean[] fLevelActive;

    /** The current state value and timestamp */
    private @NonNull ITmfStateValue fCurrentValue = TmfStateValue.nullValue();
    private long fCurrentStartTime;
    private boolean fStarted;

    /**
     * Constructor
     *
     * @param baseQuark
     *            The quark of the attribute we want to mipmap
     * @param mipmapQuark
     *            The quark of the mipmap feature attribute (e.g., "max")
     * @param ss
     *            The state system in which to insert the state changes
     */
    public TimeMipmapFeature(int baseQuark, int mipmapQuark, ITmfStateSystemBuilder ss) {
        fBaseQuark = baseQuark;
        fMipmapQuark = mipmapQuark;
        fSs = ss;
        fLevelMax = new ITmfStateValue[MAX_LEVELS + 1];
        fLevelWindowStart = new long[MAX_LEVELS + 1];
        fLevelActive = new boolean[MAX_LEVELS + 1];
        fStarted = false;
    }

    @Override
    public void updateMipmap(@NonNull ITmfStateValue value, long ts) {
        if (value.isNull()) {
            return;
        }

        if (!fStarted) {
            fCurrentValue = value;
            fCurrentStartTime = ts;
            fStarted = true;
            initializeLevels(ts);
        }

        // Update max at each level whose window has not yet closed
        for (int level = 1; level <= MAX_LEVELS; level++) {
            long windowWidth = getWindowWidth(level);
            long windowStart = fLevelWindowStart[level];
            long windowEnd = windowStart + windowWidth;

            if (ts >= windowEnd) {
                // The current window at this level has closed — flush it
                flushLevel(level, windowEnd - 1);
                // Start a new window aligned to the time grid
                long newWindowStart = alignToGrid(ts, windowWidth);
                fLevelWindowStart[level] = newWindowStart;
                fLevelMax[level] = value;
                fLevelActive[level] = true;
            } else {
                // Still within the current window — update max
                if (!fLevelActive[level]) {
                    fLevelMax[level] = value;
                    fLevelActive[level] = true;
                } else {
                    fLevelMax[level] = maxOf(fLevelMax[level], value);
                }
            }
        }

        fCurrentValue = value;
        fCurrentStartTime = ts;
    }

    @Override
    public void updateAndCloseMipmap() {
        if (!fStarted) {
            return;
        }
        // Flush all active levels
        for (int level = 1; level <= MAX_LEVELS; level++) {
            if (fLevelActive[level]) {
                flushLevel(level, fCurrentStartTime);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------------

    private void initializeLevels(long ts) {
        for (int level = 1; level <= MAX_LEVELS; level++) {
            long windowWidth = getWindowWidth(level);
            fLevelWindowStart[level] = alignToGrid(ts, windowWidth);
            fLevelMax[level] = TmfStateValue.nullValue();
            fLevelActive[level] = false;
        }
    }

    private void flushLevel(int level, long endTime) {
        if (!fLevelActive[level] || fLevelMax[level] == null || fLevelMax[level].isNull()) {
            return;
        }

        try {
            int levelQuark = getOrCreateLevelQuark(level);
            long startTime = fLevelWindowStart[level];
            if (startTime > endTime) {
                return;
            }
            fSs.modifyAttribute(startTime, fLevelMax[level].unboxValue(), levelQuark);
        } catch (TimeRangeException | StateValueTypeException e) {
            // Silently ignore — can happen at trace boundaries
        }

        fLevelActive[level] = false;
        fLevelMax[level] = TmfStateValue.nullValue();
    }

    private int getOrCreateLevelQuark(int level) {
        while (fLevelQuarks.size() <= level) {
            fLevelQuarks.add(-1);
        }
        int quark = fLevelQuarks.get(level);
        if (quark == -1) {
            quark = fSs.getQuarkRelativeAndAdd(fMipmapQuark, String.valueOf(level));
            fLevelQuarks.set(level, quark);
            // Update the mipmap quark's value to track max level
            try {
                fSs.updateOngoingState(TmfStateValue.newValueInt(level), fMipmapQuark);
            } catch (Exception e) {
                // Best effort
            }
        }
        return quark;
    }

    /**
     * Get the time window width for a given level.
     * Level 1 = 10 ns, level 2 = 100 ns, level 3 = 1000 ns, etc.
     */
    private static long getWindowWidth(int level) {
        // 10^level nanoseconds
        long width = BASE_WINDOW_NS;
        for (int i = 1; i < level; i++) {
            width *= 10;
        }
        return width;
    }

    /**
     * Align a timestamp to the start of its grid-aligned window.
     */
    private static long alignToGrid(long ts, long windowWidth) {
        return (ts / windowWidth) * windowWidth;
    }

    /**
     * Return the maximum of two state values.
     */
    private static @NonNull ITmfStateValue maxOf(@NonNull ITmfStateValue a, @NonNull ITmfStateValue b) {
        if (a.isNull()) {
            return b;
        }
        if (b.isNull()) {
            return a;
        }
        if (a.getType() == Type.DOUBLE || b.getType() == Type.DOUBLE) {
            return a.unboxDouble() >= b.unboxDouble() ? a : b;
        }
        return a.unboxLong() >= b.unboxLong() ? a : b;
    }
}
