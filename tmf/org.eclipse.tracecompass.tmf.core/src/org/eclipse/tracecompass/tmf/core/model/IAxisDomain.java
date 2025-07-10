/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.tmf.core.model;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;


/**
 * Shows the available values of X axis.
 *
 * @author Siwei Zhang
 * @since 10.1
 */
public sealed interface IAxisDomain permits IAxisDomain.Categorical, IAxisDomain.Range {

    /**
     * Categorical axis domain (e.g., names or labels).
     *
     * @param categories
     *            the category labels for the X axis
     */
    record Categorical(List<String> categories) implements IAxisDomain {
        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Categorical other)) {
                return false;
            }
            return Objects.equals(categories, other.categories);
        }

        @Override
        public int hashCode() {
            return Objects.hash(categories);
        }

        @Override
        public String toString() {
            return "AxisDomain.Categorical{categories=" + categories + "}"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Represents a range-based axis domain, such as one used for execution
     * durations.
     *
     * @param start
     *            the start value of the range
     * @param end
     *            the end value of the range
     */
    record Range(long start, long end) implements IAxisDomain {
        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Range other)) {
                return false;
            }
            return start == other.start && end == other.end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }

        @Override
        public String toString() {
            return "AxisDomain.TimeRange{start=" + start + ", end=" + end + "}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
