/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.ctf.core.event.types;

/**
 * A simple range class
 *
 * @author Vlad Arama
 * @since 4.5
 */
public class IntegerRange {
    private final long startRange;
    private final long endRange;

    /**
     * Constructor
     *
     * @param start
     *            The start of the range
     * @param end
     *            The end of the range
     */
    public IntegerRange(long start, long end) {
        startRange = start;
        endRange = end;
    }

    /**
     * Getter for the start of the range
     *
     * @return The start of the range
     */
    public long getStart() {
        if (startRange > endRange) {
            return endRange;
        }
        return startRange;
    }

    /**
     * Getter for the end of the range
     *
     * @return The end of the range
     */
    public long getEnd() {
        if (startRange > endRange) {
            return startRange;
        }
        return endRange;
    }
}