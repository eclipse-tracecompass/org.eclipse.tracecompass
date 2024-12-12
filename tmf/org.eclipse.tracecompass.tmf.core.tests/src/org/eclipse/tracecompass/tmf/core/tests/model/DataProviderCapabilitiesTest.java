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
package org.eclipse.tracecompass.tmf.core.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.junit.Test;

/**
 * JUnit Test class to test {@link DataProviderCapabilities}
 *
 * @author Bernd Hufmann
 */
public class DataProviderCapabilitiesTest {

    private static final String EXPECTED_TO_STRING = "DataProviderCapabilities[canCreate=true, canDelete=true]";

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------
    /**
     * Test builder, constructor and getter/setters.
     */
    @Test
    public void testBuilder() {
        DataProviderCapabilities.Builder builder = new DataProviderCapabilities.Builder()
                .setCanCreate(true)
                .setCanDelete(true);
        IDataProviderCapabilities capabilities = builder.build();
        assertTrue(capabilities.canCreate());
        assertTrue(capabilities.canDelete());
    }

    /**
     * Test {@Link DataProviderCapabilities#equals()}
     */
    @Test
    public void testEquality() {
        DataProviderCapabilities.Builder builder = new DataProviderCapabilities.Builder()
                .setCanCreate(true)
                .setCanDelete(true);
        IDataProviderCapabilities baseCapabilities = builder.build();

        // Make sure it is equal to itself
        IDataProviderCapabilities testCapabilities = builder.build();
        assertEquals(baseCapabilities, testCapabilities);
        assertEquals(testCapabilities, baseCapabilities);

        // Change each of the variable and make sure result is not equal
        builder.setCanCreate(false);
        testCapabilities = builder.build();
        assertNotEquals(baseCapabilities, testCapabilities);
        assertNotEquals(testCapabilities, baseCapabilities);

        builder.setCanCreate(true);
        builder.setCanDelete(false);
        testCapabilities = builder.build();
        assertNotEquals(baseCapabilities, testCapabilities);
        assertNotEquals(testCapabilities, baseCapabilities);

        builder.setCanCreate(false);
        builder.setCanDelete(false);
        testCapabilities = builder.build();
        assertNotEquals(baseCapabilities, testCapabilities);
        assertNotEquals(testCapabilities, baseCapabilities);

        // Different objects with same content
        assertFalse(testCapabilities == DataProviderCapabilities.NULL_INSTANCE);

        // Equal by content
        assertEquals(DataProviderCapabilities.NULL_INSTANCE, testCapabilities);
    }

    /**
     * Test {@Link TmfConfiguration#toString()}
     **/
    @Test
    public void testToString() {
        DataProviderCapabilities.Builder builder = new DataProviderCapabilities.Builder()
                .setCanCreate(true)
                .setCanDelete(true);
        assertEquals(EXPECTED_TO_STRING, builder.build().toString());
    }

    /**
     * Test {@Link TmfConfiguration#hashCode()}
     */
    @Test
    public void testHashCode() {
        DataProviderCapabilities.Builder builder = new DataProviderCapabilities.Builder()
                .setCanCreate(true)
                .setCanDelete(true);

        IDataProviderCapabilities capabilities1 = builder.build();
        IDataProviderCapabilities capabilities2 = DataProviderCapabilities.NULL_INSTANCE;

        assertEquals(capabilities1.hashCode(), capabilities1.hashCode());
        assertEquals(capabilities2.hashCode(), capabilities2.hashCode());
        assertNotEquals(capabilities1.hashCode(), capabilities2.hashCode());
    }
}
