/**********************************************************************
 * Copyright (c) 2020, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsModel;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.AbstractSegmentStoreStatisticsDataProvider;

/**
 * Abstract class for segment store statistics data provider tests. This class provides methods to verify
 * the entries in segment store statistics and ensure their correctness.
 *
 * @author Bernd Hufmann
 * @author Siwei Zhang
 */
public abstract class AbstractSegmentStoreStatisticsDataProviderTest {

    /**
     * Verifies the entries in the segment store statistics against the expected
     * labels and entries.
     *
     * @param expectedLabels
     *            The expected labels for each entry.
     * @param expectedEntries
     *            The expected statistics holders.
     * @param entries
     *            The actual segment store statistics entries.
     * @param startIndex
     *            The start index for verification.
     * @param nbEntries
     *            The number of entries to verify.
     */
    protected static void verifyEntries(List<@NonNull List<@NonNull String>> expectedLabels,
            @NonNull List<@NonNull StatisticsHolder> expectedEntries, List<@NonNull SegmentStoreStatisticsModel> entries, int startIndex, int nbEntries) {
        assertEquals("Number of entries", nbEntries, entries.size());
        for (int i = 0; i < expectedLabels.size(); i++) {
            int index = startIndex + i;
            SegmentStoreStatisticsModel entry = entries.get(index);
            assertEquals("Entry (index " + index + ")", expectedLabels.get(i), entry.getLabels());
            assertEquals("name (index " + index + ")", expectedEntries.get(i).fName, entry.getName());
            assertEquals("id (index " + index + ")", expectedEntries.get(i).fId, entry.getId());
            assertEquals("parentId (index " + index + ")", expectedEntries.get(i).fParentId, entry.getParentId());

            assertEquals("min (index " + index + ")", expectedEntries.get(i).fMin, entry.getMin());
            assertEquals("max (index " + index + ")", expectedEntries.get(i).fMax, entry.getMax());
            assertEquals("Average (index " + index + ")", expectedEntries.get(i).fAverage, entry.getMean(), 0.02);
            assertEquals("StdDev (index " + index + ")", expectedEntries.get(i).fStdDev, entry.getStdDev(), 0.02);
            assertEquals("Count (index " + index + ")", expectedEntries.get(i).fNbElements, entry.getNbElements());
            assertEquals("Total (index " + index + ")", expectedEntries.get(i).fTotal, entry.getTotal(), 0.02);

            assertEquals("Min start (index " + index + ")", expectedEntries.get(i).fMinStart, entry.getMinStart());
            assertEquals("Min end (index " + index + ")", expectedEntries.get(i).fMinEnd, entry.getMinEnd());
            assertEquals("Max start (index " + index + ")", expectedEntries.get(i).fMaxStart, entry.getMaxStart());
            assertEquals("Max end (index " + index + ")", expectedEntries.get(i).fMaxEnd, entry.getMaxEnd());
        }
    }

    /**
     * Verifies the entries with user-defined aspects in the segment store
     * statistics against the expected labels and entries.
     *
     * @param expectedLabels
     *            The expected labels for each entry.
     * @param expectedEntriesUserDefined
     *            The expected statistics holders with user-defined fields.
     * @param entries
     *            The actual segment store statistics entries.
     * @param startIndex
     *            The start index for verification.
     * @param nbEntries
     *            The number of entries to verify.
     */
    protected static void verifyEntriesWithUserDefinedAspect(List<@NonNull List<@NonNull String>> expectedLabels,
            @NonNull List<@NonNull StatisticsHolderUserDefined> expectedEntriesUserDefined, List<@NonNull SegmentStoreStatisticsModel> entries, int startIndex, int nbEntries) {
        @NonNull List<@NonNull StatisticsHolder> expectedEntries = expectedEntriesUserDefined.stream().collect(Collectors.toList());
        verifyEntries(expectedLabels, expectedEntries, entries, startIndex, nbEntries);
        for (int i = 0; i < expectedLabels.size(); i++) {
            String expectedUserDefinedValue = expectedEntriesUserDefined.get(i).fUserDefinedField;
            int index = startIndex + i;
            SegmentStoreStatisticsModel entry = entries.get(index);
            assertTrue("User defined aspect not found (index " + index + ")", entry.getLabels().stream().anyMatch(s -> s.equals(expectedUserDefinedValue)));
        }
    }

    /**
     * Resets static ENTRY_ID of AbstractSegmentStoreStatisticsDataProvider, so
     * that each test class extending
     * AbstractSegmentStoreStatisticsDataProviderTest has known IDs, independent
     * of the order of execution.
     */
    protected static void resetIds() {
        try {
            Field entryIdField = AbstractSegmentStoreStatisticsDataProvider.class.getDeclaredField("ENTRY_ID");
            entryIdField.setAccessible(true);
            Object entryIdObject = entryIdField.get(null);
            Class<?> entryIdClass = entryIdObject.getClass();
            Method setMethod = entryIdClass.getDeclaredMethod("set", long.class);
            setMethod.invoke(entryIdObject, 0L);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            fail("Error resetting entry ID: " + e.getMessage());
        }
    }

    /**
     * Holder class for statistics data. It contains various fields to hold
     * statistical values.
     */
    protected static class StatisticsHolder {
        String fName;
        long fId;
        long fParentId;
        long fMin;
        long fMax;
        long fNbElements;
        double fAverage;
        double fStdDev;
        double fTotal;
        long fMinStart;
        long fMinEnd;
        long fMaxStart;
        long fMaxEnd;

        /**
         * Constructor for StatisticsHolder.
         *
         * @param name
         *            The name of the statistic.
         * @param id
         *            The id of the statistic.
         * @param parentId
         *            The parent id of the statistic.
         * @param min
         *            The minimum value.
         * @param max
         *            The maximum value.
         * @param average
         *            The average value.
         * @param stdDev
         *            The standard deviation.
         * @param nbElements
         *            The number of elements.
         * @param total
         *            The total value.
         * @param minStart
         *            The minimum start value.
         * @param minEnd
         *            The minimum end value.
         * @param maxStart
         *            The maximum start value.
         * @param maxEnd
         *            The maximum end value.
         */
        public StatisticsHolder(String name, long id, long parentId, long min, long max, double average,
                double stdDev, long nbElements, double total, long minStart, long minEnd, long maxStart, long maxEnd) {
            fName = name;
            fId = id;
            fParentId = parentId;
            fMin = min;
            fMax = max;
            fNbElements = nbElements;
            fAverage = average;
            fStdDev = stdDev;
            fTotal = total;
            fMinStart = minStart;
            fMinEnd = minEnd;
            fMaxStart = maxStart;
            fMaxEnd = maxEnd;
        }
    }

    /**
     * Holder class for statistics data with an additional user-defined field.
     */
    protected static class StatisticsHolderUserDefined extends StatisticsHolder {
        String fUserDefinedField;

        /**
         * Constructor for StatisticsHolderUserDefined.
         *
         * @param name
         *            The name of the statistic.
         * @param id
         *            The id of the statistic.
         * @param parentId
         *            The parent id of the statistic.
         * @param min
         *            The minimum value.
         * @param max
         *            The maximum value.
         * @param average
         *            The average value.
         * @param stdDev
         *            The standard deviation.
         * @param nbElements
         *            The number of elements.
         * @param total
         *            The total value.
         * @param minStart
         *            The minimum start value.
         * @param minEnd
         *            The minimum end value.
         * @param maxStart
         *            The maximum start value.
         * @param maxEnd
         *            The maximum end value.
         * @param userDefinedField
         *            The user-defined field value.
         */
        public StatisticsHolderUserDefined(String name, long id, long parentId, long min, long max, double average,
                double stdDev, long nbElements, double total, long minStart, long minEnd, long maxStart, long maxEnd, String userDefinedField) {
            super(name, id, parentId, min, max, average, stdDev, nbElements, total, minStart, minEnd, maxStart, maxEnd);
            fUserDefinedField = userDefinedField;
        }
    }
}
