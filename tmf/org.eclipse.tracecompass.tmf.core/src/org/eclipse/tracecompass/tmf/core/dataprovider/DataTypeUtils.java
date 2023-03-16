/**********************************************************************
 * Copyright (c) 2020, 2023 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.tmf.core.dataprovider;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.format.DataSizeWithUnitFormat;
import org.eclipse.tracecompass.common.core.format.DataSpeedWithUnitFormat;
import org.eclipse.tracecompass.common.core.format.DecimalWithUnitPrefixFormat;
import org.eclipse.tracecompass.common.core.format.SubSecondTimeWithUnitFormat;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;

/**
 * Utility class for data provider's data types
 *
 * @author Geneviève Bastien
 * @since 6.1
 */
public final class DataTypeUtils {

    private static final Format DECIMAL_NO_UNIT_FORMAT = new DecimalWithUnitPrefixFormat(""); //$NON-NLS-1$

    private static final Format OTHER_FORMAT = new Format() {
        private static final long serialVersionUID = -2824035517014261121L;

        @Override
        public StringBuffer format(@Nullable Object obj, @Nullable StringBuffer toAppendTo, @Nullable FieldPosition pos) {
            if (toAppendTo == null) {
                return new StringBuffer(String.valueOf(obj));
            }
            return Objects.requireNonNull(toAppendTo.append(String.valueOf(obj)));
        }

        @Override
        public @Nullable Object parseObject(@Nullable String source, @Nullable ParsePosition pos) {
            return null;
        }
    };

    private static final Format BINARY_SPEED_FORMAT = new DataSpeedWithUnitFormat() {
        private static final long serialVersionUID = 6571473479261285111L;

        @Override
        public Number parseObject(String source, ParsePosition pos) {
            return null;
        }
    };

    private static final Format BINARY_SIZE_FORMAT = new DataSizeWithUnitFormat() {
        private static final long serialVersionUID = 391619032553653610L;

        @Override
        public Number parseObject(String source, ParsePosition pos) {
            return null;
        }
    };

    private DataTypeUtils() {
        // Nothing to do
    }

    /**
     * Get the formatter
     *
     * @param type
     *            The data type
     * @param units
     *            The units to add to the formatted value
     * @return The formatter for this data type
     */
    public static Format getFormat(DataType type, String units) {
        switch (type) {
        case NUMBER:
            return units.isEmpty() ? DECIMAL_NO_UNIT_FORMAT : new DecimalWithUnitPrefixFormat(units);
        case TIMESTAMP:
            return Objects.requireNonNull(TmfTimestampFormat.getDefaulTimeFormat());
        case BINARY_NUMBER:
            // Check if it's a speed
            if (units.endsWith("/s")) { //$NON-NLS-1$
                return BINARY_SPEED_FORMAT;
            }
            return BINARY_SIZE_FORMAT;
        case DURATION:
            return SubSecondTimeWithUnitFormat.getInstance();
        case STRING:
            return OTHER_FORMAT;
        case TIME_RANGE:
            return OTHER_FORMAT;
        default:
            // Return the default format
            return OTHER_FORMAT;
        }
    }

    /**
     * Converts a time range (start, end) in a DataType string.
     *
     * @param start
     *            The start time
     * @param end
     *            The end time
     * @return A standard Time Range string
     * @since 8.4
     */
    public static String toRangeString(long start, long end) {
        return "[" + start + "," + end + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
