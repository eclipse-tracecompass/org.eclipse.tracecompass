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
package org.eclipse.tracecompass.tmf.core.tests.model.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigParamDescriptor;
import org.junit.Test;

/**
 * JUnit Test class to test {@link TmfConfigParamDescriptor}
 */
public class TmfConfigParamDescriptorTest {

    // ------------------------------------------------------------------------
    // Test data
    // ------------------------------------------------------------------------
    private static final String PATH = "path";
    private static final String DESC = "descriptor";
    private static final String DATA_TYPE = "NUMBER";
    private static final String EXPECTED_TO_STRING = "TmfConfigParamDescriptor[fKeyName=path, fDataType=NUMBER, fIsRequired=true, fDescription=descriptor]";
    private static final String EXPECTED_DEFAULT_DATA_TYPE = "STRING";

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------
    /**
     * Test builder, constructor and getter/setters.
     */
    @Test
    public void testBuilder() {
        TmfConfigParamDescriptor.Builder builder = new TmfConfigParamDescriptor.Builder()
                .setKeyName(PATH)
                .setDescription(DESC)
                .setDataType(DATA_TYPE)
                .setIsRequired(false);
        ITmfConfigParamDescriptor config = builder.build();
            assertEquals(PATH, config.getKeyName());
            assertEquals(DESC, config.getDescription());
            assertEquals(DATA_TYPE, config.getDataType());
            assertFalse(config.isRequired());
    }

    /**
     * Test builder with missing params.
     */
    @Test
    public void testBuilderMissingParams() {
        TmfConfigParamDescriptor.Builder builder = new TmfConfigParamDescriptor.Builder()
                .setDescription(DESC)
                .setDataType(DATA_TYPE)
                .setIsRequired(false);
        // Test missing name
        try {
            builder.build();
            fail("No exception created");
        } catch (IllegalStateException e) {
            // success
        }

        // Test successful builder
        builder = new TmfConfigParamDescriptor.Builder()
                .setKeyName(PATH);
        ITmfConfigParamDescriptor config = builder.build();
        assertEquals(PATH, config.getKeyName());
        assertTrue(config.getDescription().isEmpty());
        assertEquals(EXPECTED_DEFAULT_DATA_TYPE, config.getDataType());
        assertTrue(config.isRequired());
    }

    /**
     * Test {@Link TmfConfiguration#equals()}
     */
    @Test
    public void testEquality() {
        TmfConfigParamDescriptor.Builder builder = new TmfConfigParamDescriptor.Builder()
                .setKeyName(PATH)
                .setDescription(DESC)
                .setDataType(DATA_TYPE)
                .setIsRequired(false);
        ITmfConfigParamDescriptor baseConfiguration = builder.build();

        // Make sure it is equal to itself
        ITmfConfigParamDescriptor testConfig = builder.build();
        assertEquals(baseConfiguration, testConfig);
        assertEquals(testConfig, baseConfiguration);

        // Change each of the variable and make sure result is not equal
        builder.setKeyName("Other path");
        testConfig = builder.build();
        assertNotEquals(baseConfiguration, testConfig);
        assertNotEquals(testConfig, baseConfiguration);

        builder.setKeyName(PATH);
        builder.setDescription("Other desc");
        testConfig = builder.build();
        assertNotEquals(baseConfiguration, testConfig);
        assertNotEquals(testConfig, baseConfiguration);

        builder.setDescription(DESC);
        builder.setDataType(EXPECTED_DEFAULT_DATA_TYPE);
        testConfig = builder.build();
        assertNotEquals(baseConfiguration, testConfig);
        assertNotEquals(testConfig, baseConfiguration);

        builder.setDataType(DATA_TYPE);
        builder.setIsRequired(true);
        testConfig = builder.build();
        assertNotEquals(baseConfiguration, testConfig);
        assertNotEquals(testConfig, baseConfiguration);
    }

    /**
     * Test {@Link TmfConfiguration#toString()}
     **/
    @Test
    public void testToString() {
        TmfConfigParamDescriptor.Builder builder = new TmfConfigParamDescriptor.Builder()
                .setKeyName(PATH)
                .setDescription(DESC)
                .setDataType(DATA_TYPE);
        assertEquals(EXPECTED_TO_STRING, builder.build().toString());
    }

    /**
     * Test {@Link TmfConfiguration#hashCode()}
     */
    @Test
    public void testHashCode() {
        TmfConfigParamDescriptor.Builder builder = new TmfConfigParamDescriptor.Builder()
                .setKeyName(PATH)
                .setDescription(DESC)
                .setDataType(DATA_TYPE);

        ITmfConfigParamDescriptor config1 = builder.build();

        builder = new TmfConfigParamDescriptor.Builder()
                .setKeyName(PATH + "1")
                .setDescription(DESC + "1")
                .setDataType(DATA_TYPE + "1")
                .setIsRequired(false);

        ITmfConfigParamDescriptor config2 = builder.build();

        assertEquals(config1.hashCode(), config1.hashCode());
        assertEquals(config2.hashCode(), config2.hashCode());
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }
}
