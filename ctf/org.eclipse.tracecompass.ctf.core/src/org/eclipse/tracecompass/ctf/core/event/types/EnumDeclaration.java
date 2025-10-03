/*******************************************************************************
 * Copyright (c) 2011, 2023 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Matthew Khouzam - Initial API and implementation
 * Contributors: Simon Marchi - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.ctf.core.event.types;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;

import com.google.common.collect.ImmutableSet;

/**
 * A CTF enum declaration.
 *
 * The definition of a enum point basic data type. It will take the data from a
 * trace and store it (and make it fit) as an integer and a string.
 *
 * @version 1.0
 * @author Matthew Khouzam
 * @author Simon Marchi
 */
public final class EnumDeclaration extends Declaration implements ISimpleDatatypeDeclaration {

    private static final int CACHE_SIZE = 4096;

    /**
     * A pair of longs class
     *
     * @since 1.1
     */
    public static class Pair {
        private final long fFirst;
        private final long fSecond;

        private Pair(long first, long second) {
            fFirst = first;
            fSecond = second;
        }

        /**
         * @return the first element
         */
        public long getFirst() {
            return fFirst;
        }

        /**
         * @return the second element
         */
        public long getSecond() {
            return fSecond;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Pair other = (Pair) obj;
            return fFirst == other.fFirst && fSecond == other.fSecond;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fFirst, fSecond);
        }

        @Override
        public String toString() {
            return Arrays.toString(new long[] { fFirst, fSecond });
        }
    }

    /**
     * Interval tree node for efficient range queries
     */
    private static class IntervalNode {
        final Pair interval;
        final List<String> labels;
        long maxEnd;
        IntervalNode left, right;

        IntervalNode(Pair interval, String label) {
            this.interval = interval;
            this.labels = new ArrayList<>();
            this.labels.add(label);
            this.maxEnd = interval.getSecond();
        }
    }

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private IntervalNode fEnumRoot;
    private final IntegerDeclaration fContainerType;
    private Pair fLastAdded = new Pair(-1, -1);
    private @Nullable String[] fCache = new String[CACHE_SIZE];

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * constructor
     *
     * @param containerType
     *            the enum is an int, this is the type that the data is
     *            contained in. If you have 1000 possible values, you need at
     *            least a 10 bit enum. If you store 2 values in a 128 bit int,
     *            you are wasting space.
     */
    public EnumDeclaration(IntegerDeclaration containerType) {
        fContainerType = containerType;
    }

    /**
     * Constructor
     *
     * @param containerType
     *            the enum is an int, this is the type that the data is
     *            contained in. If you have 1000 possible values, you need at
     *            least a 10 bit enum. If you store 2 values in a 128 bit int,
     *            you are wasting space.
     * @param enumTree
     *            Existing enum declaration table
     * @since 2.3
     */
    public EnumDeclaration(IntegerDeclaration containerType, Map<Pair, String> enumTree) {
        fContainerType = containerType;
        enumTree.entrySet().forEach(entry -> insert(entry.getKey(), entry.getValue()));
    }

    // ------------------------------------------------------------------------
    // Getters/Setters/Predicates
    // ------------------------------------------------------------------------

    /**
     *
     * @return The container type
     */
    public IntegerDeclaration getContainerType() {
        return fContainerType;
    }

    @Override
    public long getAlignment() {
        return getContainerType().getAlignment();
    }

    @Override
    public int getMaximumSize() {
        return fContainerType.getMaximumSize();
    }

    /**
     * @since 2.0
     */
    @Override
    public boolean isByteOrderSet() {
        return fContainerType.isByteOrderSet();
    }

    /**
     * @since 2.0
     */
    @Override
    public ByteOrder getByteOrder() {
        return fContainerType.getByteOrder();
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    public EnumDefinition createDefinition(@Nullable IDefinitionScope definitionScope, String fieldName, BitBuffer input) throws CTFException {
        alignRead(input);
        IntegerDefinition value = getContainerType().createDefinition(definitionScope, fieldName, input);
        return new EnumDefinition(this, definitionScope, fieldName, value);
    }

    /**
     * Add a value.
     *
     * @param low
     *            lowest value that this int can be to have label as a return
     *            string
     * @param high
     *            highest value that this int can be to have label as a return
     *            string
     * @param label
     *            the name of the value.
     * @return was the value be added? true == success
     */
    public boolean add(long low, long high, @Nullable String label) {
        if (high < low) {
            return false;
        }
        if (low < 0 || low >= CACHE_SIZE - 1 || high < 0 || high >= CACHE_SIZE - 1) {
            fCache = null;
        }
        for (int i = (int) low; i <= high; i++) { // high is inclusive
            if (fCache != null) {
                if (fCache[i] == null) {
                    fCache[i] = label;
                } else {
                    fCache = null;
                    break;
                }
            } else {
                break;
            }
        }
        Pair key = new Pair(low, high);
        insert(key, label);
        fLastAdded = key;
        return true;
    }

    private void insert(Pair interval, String label) {
        fEnumRoot = insertNode(fEnumRoot, interval, label);
    }

    private IntervalNode insertNode(IntervalNode node, Pair interval, String label) {
        if (node == null) {
            return new IntervalNode(interval, label);
        }

        if (interval.equals(node.interval)) {
            node.labels.add(label);
            return node;
        }

        if (interval.getFirst() < node.interval.getFirst()) {
            node.left = insertNode(node.left, interval, label);
        } else {
            node.right = insertNode(node.right, interval, label);
        }

        node.maxEnd = Math.max(node.maxEnd, interval.getSecond());
        return node;
    }

    /**
     * Add a value following the last previously added value.
     *
     * @param label
     *            the name of the value.
     * @return was the value be added? true == success
     * @since 2.0
     */
    public boolean add(@Nullable String label) {
        // add the item at last range end + 1, according to specification
        return add(fLastAdded.fSecond + 1, fLastAdded.fSecond + 1, label);
    }

    /**
     * Query the label for a value. If overlapping labels are found, they are
     * returned in the format "[l1, l2, ...]". If the enum is a bit flag, the
     * matching flags are returned in the format "flag1 | flag2 | ...".
     *
     * @param value
     *            the value to lookup
     * @return the label of that value, can be null
     */
    public @Nullable String query(long value) {
        if (fCache != null) {
            if (value < 0 || value >= CACHE_SIZE) {
                return null;
            }
            return fCache[(int) value];
        }
        List<String> strValues = new ArrayList<>();
        queryIntersecting(fEnumRoot, value, strValues);

        if (!strValues.isEmpty()) {
            return strValues.size() == 1 ? strValues.get(0) : strValues.toString();
        }

        /*
         * Divide the positive value in bits and see if there is a value for all
         * those bits
         */
        List<String> flagsSet = new ArrayList<>();
        for (int i = 0; i < Long.SIZE; i++) {
            Long bitValue = 1L << i;
            if ((bitValue & value) != 0) {
                List<String> bitFlags = new ArrayList<>();
                queryExact(fEnumRoot, bitValue, bitFlags);
                if (bitFlags.isEmpty()) {
                    return null;
                }
                flagsSet.add(bitFlags.size() == 1 ? bitFlags.get(0) : bitFlags.toString());
            }
        }
        return flagsSet.isEmpty() ? null : String.join(" | ", flagsSet); //$NON-NLS-1$
    }

    private void queryIntersecting(IntervalNode node, long value, List<String> result) {
        if (node == null || node.maxEnd < value) {
            return;
        }

        if (value >= node.interval.getFirst() && value <= node.interval.getSecond()) {
            result.addAll(node.labels);
        }

        if (node.left != null && node.left.maxEnd >= value) {
            queryIntersecting(node.left, value, result);
        }

        queryIntersecting(node.right, value, result);
    }

    private void queryExact(IntervalNode node, long value, List<String> result) {
        if (node == null) {
            return;
        }

        if (node.interval.getFirst() == value && node.interval.getSecond() == value) {
            result.addAll(node.labels);
        }

        if (value < node.interval.getFirst()) {
            queryExact(node.left, value, result);
        } else if (value > node.interval.getFirst()) {
            queryExact(node.right, value, result);
        } else {
            queryExact(node.left, value, result);
            queryExact(node.right, value, result);
        }
    }

    /**
     * Get a copy of the lookup table.
     *
     * @return a copy of the Enum declaration entry map.
     *
     * @since 2.3
     */
    public Map<Pair, String> getLookupTable() {
        Map<Pair, String> table = new LinkedHashMap<>();
        collectEntries(fEnumRoot, table);
        return table;
    }

    private void collectEntries(IntervalNode node, Map<Pair, String> table) {
        if (node == null) {
            return;
        }

        String value = node.labels.size() == 1 ? node.labels.get(0) : node.labels.toString();
        table.put(node.interval, value);

        collectEntries(node.left, table);
        collectEntries(node.right, table);
    }

    /**
     * Gets a set of labels of the enum
     *
     * @return A set of labels of the enum, can be empty but not null
     */
    public Set<String> getLabels() {
        List<String> labels = new ArrayList<>();
        collectLabels(fEnumRoot, labels);
        return ImmutableSet.copyOf(labels);
    }

    private void collectLabels(IntervalNode node, List<String> labels) {
        if (node == null) {
            return;
        }

        labels.addAll(node.labels);
        collectLabels(node.left, labels);
        collectLabels(node.right, labels);
    }

    @Override
    public String toString() {
        /* Only used for debugging */
        StringBuilder sb = new StringBuilder();
        sb.append("[declaration] enum["); //$NON-NLS-1$
        for (String label : getLabels()) {
            sb.append("label:").append(label).append(' '); //$NON-NLS-1$
        }
        sb.append("type:").append(fContainerType.toString()); //$NON-NLS-1$
        sb.append(']');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(fContainerType, getLookupTable());
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
        EnumDeclaration other = (EnumDeclaration) obj;
        if (!fContainerType.equals(other.fContainerType)) {
            return false;
        }
        return Objects.equals(getLookupTable(), other.getLookupTable());
    }

    @Override
    public boolean isBinaryEquivalent(@Nullable IDeclaration obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EnumDeclaration other = (EnumDeclaration) obj;
        if (!fContainerType.isBinaryEquivalent(other.fContainerType)) {
            return false;
        }
        return Objects.equals(getLookupTable(), other.getLookupTable());
    }

}
