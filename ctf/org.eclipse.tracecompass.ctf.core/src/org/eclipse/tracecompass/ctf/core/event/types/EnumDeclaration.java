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
import java.util.Collection;
import java.util.HashMap;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

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

    private static final int CACHE_SIZE = 256;
    private Map<Long, String> fCache = new HashMap<>();

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

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * fEnumMap key is the Pair of low and high, value is the label. Overlap of
     * keys in the map is allowed.
     */
    private final Multimap<Pair, String> fEnumMap = LinkedHashMultimap.create();
    private final IntegerDeclaration fContainerType;
    private Pair fLastAdded = new Pair(-1, -1);

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
        enumTree.entrySet().forEach(entry -> fEnumMap.put(entry.getKey(), entry.getValue()));
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
        Pair key = new Pair(low, high);
        fEnumMap.put(key, label);
        fLastAdded = key;
        return true;
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
        String retVal = fCache.get(value);
        if (retVal == null) {
            List<String> strValues = new ArrayList<>();
            fEnumMap.forEach((k, v) -> {
                if (value >= k.getFirst() && value <= k.getSecond()) {
                    strValues.add(v);
                }
            });
            if (!strValues.isEmpty()) {
                retVal = strValues.size() == 1 ? strValues.get(0) : strValues.toString();
                fCache.put(value, retVal);
                if (fCache.size() > CACHE_SIZE) {
                    fCache.remove(fCache.keySet().toArray()[0]);
                }
                return retVal;
            }
            /*
             * Divide the positive value in bits and see if there is a value for
             * all those bits
             */
            List<String> flagsSet = new ArrayList<>();
            for (int i = 0; i < Long.SIZE; i++) {
                Long bitValue = 1L << i;
                if ((bitValue & value) != 0) {
                    /*
                     * See if there is a value for this bit where lower ==
                     * upper, no range accepted here
                     */
                    Pair bitPair = new Pair(bitValue, bitValue);
                    Collection<String> flagValues = fEnumMap.get(bitPair);
                    if (flagValues.isEmpty()) {
                        // No value for this bit, not an enum flag
                        return null;
                    }
                    flagsSet.add(flagValues.size() == 1 ? flagValues.iterator().next() : flagValues.toString());
                }
            }
            retVal = flagsSet.isEmpty() ? null : String.join(" | ", flagsSet); //$NON-NLS-1$
            fCache.put(value, retVal);
            if (fCache.size() > CACHE_SIZE) {
                fCache.remove(fCache.keySet().toArray()[0]);
            }
        }
        return retVal;
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
        fEnumMap.asMap().forEach((k, v) -> {
            table.put(k, v.size() == 1 ? v.iterator().next() : v.toString());
        });
        return table;
    }

    /**
     * Gets a set of labels of the enum
     *
     * @return A set of labels of the enum, can be empty but not null
     */
    public Set<String> getLabels() {
        return ImmutableSet.copyOf(fEnumMap.values());
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
        return Objects.hash(fContainerType, fEnumMap);
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
        /*
         * Must iterate through the entry sets as the comparator used in the
         * enum tree does not respect the contract
         */
        return Iterables.elementsEqual(fEnumMap.entries(), other.fEnumMap.entries());
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
        /*
         * Must iterate through the entry sets as the comparator used in the
         * enum tree does not respect the contract
         */
        return Iterables.elementsEqual(fEnumMap.entries(), other.fEnumMap.entries());
    }

}
