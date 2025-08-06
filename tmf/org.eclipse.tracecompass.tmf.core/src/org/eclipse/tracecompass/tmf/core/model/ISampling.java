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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;

/**
 * Represents the sampling information of the X-axis.
 * This allows both time-series (numeric) and categorical (string or range-based) sampling.
 *
 * @author Siwei Zhang
 * @since 10.1
 */
public sealed interface ISampling permits ISampling.Timestamps, ISampling.Categories, ISampling.Ranges {

    /**
     * Returns the number of sampling points in this sampling definition.
     *
     * @return the number of sampling points
     */
    int size();

    /**
     * Time-based sampling points.
     *
     * @param timestamps
     *            the X-axis sampling points as an array of timestamps
     */
    record Timestamps(long[] timestamps) implements ISampling {
        @Override
        public int size() {
            return timestamps.length;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (this == obj) || (obj instanceof Timestamps other && Arrays.equals(this.timestamps, other.timestamps));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(timestamps);
        }

        @Override
        public String toString() {
            return NonNullUtils.nullToEmptyString(Arrays.toString(timestamps));
        }
    }

    /**
     * Categorical sampling points (e.g., labels like "Read", "Write", "Idle").
     *
     * @param categories
     *            the X-axis categories
     */
    record Categories(List<String> categories) implements ISampling {
        @Override
        public int size() {
            return categories.size();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (this == obj) || (obj instanceof Categories other && Objects.equals(this.categories, other.categories));
        }

        @Override
        public int hashCode() {
            return Objects.hash(categories);
        }

        @Override
        public String toString() {
            return NonNullUtils.nullToEmptyString(categories.toString());
        }
    }

    /**
     * Range sampling points, representing intervals per bucket.
     *
     * @param ranges
     *            the ranges for each bucket on the X-axis
     */
    record Ranges(List<Range<Long>> ranges) implements ISampling {
        @Override
        public int size() {
            return ranges.size();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (this == obj) || (obj instanceof Ranges other && Objects.equals(this.ranges, other.ranges));
        }

        @Override
        public int hashCode() {
            return Objects.hash(ranges);
        }

        @Override
        public String toString() {
            return NonNullUtils.nullToEmptyString(ranges.toString());
        }
    }

    /**
     * Represents a closed interval [start, end] on a comparable type.
     *
     * @param <T>
     *            the type of the range boundaries (must be comparable)
     * @param start
     *            the start of the range (inclusive)
     * @param end
     *            the end of the range (inclusive)
     */
    public record Range<T extends Comparable<T>>(T start, T end) {
        @Override
        public boolean equals(@Nullable Object obj) {
            return (this == obj) || (obj instanceof Range<?> other &&
                    Objects.equals(start, other.start) && Objects.equals(end, other.end));
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }

        @Override
        public String toString() {
            return "Range[" + start + ", " + end + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
