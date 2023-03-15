/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.tmf.core.tests.dataprovider;

import static org.junit.Assert.assertEquals;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataTypeUtils;
import org.junit.Test;

/**
 * Test {@link DataTypeUtils}
 *
 * @author Bernd Hufmann
 */
@NonNullByDefault
public class DataTypeUtilsTest {
    private static final long START = 1234L;
    private static final long END = 5678L;
    private static final String EXPECTED_TIME_RANGE_A_STRING = "[1234,5678]";

    /**
     * Test {@link DataTypeUtils#toRangeString(long, long)}
     */
    @Test
    public void testTimeQuery() {
        String timeRangeString = DataTypeUtils.toRangeString(START, END);
        assertEquals(EXPECTED_TIME_RANGE_A_STRING, timeRangeString);
    }
}
