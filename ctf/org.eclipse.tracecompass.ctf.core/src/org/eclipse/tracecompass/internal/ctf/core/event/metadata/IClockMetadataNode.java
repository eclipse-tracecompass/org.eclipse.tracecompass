/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Interface for clock metadata nodes. Abstracts the differences between CTF1
 * (TSDL/ANTLR) and CTF2 (JSON) clock representations, allowing the
 * {@link org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.ClockParser}
 * to work without instanceof checks.
 *
 * @author Eclipse Trace Compass
 */
public interface IClockMetadataNode extends ICTFMetadataNode {

    /**
     * Get the name of the clock
     *
     * @return the clock name
     */
    @Nullable String getClockName();

    /**
     * Get the frequency of the clock in Hz
     *
     * @return the frequency
     */
    long getClockFrequency();

    /**
     * Get the precision of the clock
     *
     * @return the precision
     */
    long getClockPrecision();

    /**
     * Get the description of the clock
     *
     * @return the description, or null if not set
     */
    @Nullable String getClockDescription();

    /**
     * Get the offset in seconds from Epoch
     *
     * @return the offset in seconds
     */
    long getClockOffsetSeconds();

    /**
     * Get the offset in cycles (1/freq units)
     *
     * @return the offset in cycles
     */
    long getClockOffsetCycles();

    /**
     * Get the origin of the clock (e.g., "unix-epoch" or a custom origin name)
     *
     * @return the origin string, or null if not set
     */
    @Nullable String getClockOrigin();

    /**
     * Check if the clock is absolute (true if clock is a global reference, e.g.
     * NTP time)
     *
     * @return true if the clock is absolute
     */
    boolean isClockAbsolute();

    /**
     * Get the UUID of the clock as a string
     *
     * @return the UUID string, or null if not set
     */
    @Nullable String getClockUUID();
}
