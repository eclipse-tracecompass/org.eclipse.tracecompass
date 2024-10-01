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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * JUnit Test class to test {@link TmfConfiguration}
 */
public class TmfConfigurationTest {

    // ------------------------------------------------------------------------
    // Test data
    // ------------------------------------------------------------------------
    private static final String PATH = "/tmp/my-test.xml";
    private static final String ID = "my-test.xml";
    private static final String DESC = "descriptor";
    private static final String SOURCE_ID = "my-source-id";
    private static final @NonNull Map<@NonNull String, @NonNull Object> PARAM = ImmutableMap.of("path", "/tmp/home/my-test.xml");
    private static final String EXPECTED_TO_STRING = "TmfConfiguration[fName=/tmp/my-test.xml, fDescription=descriptor, fType=my-source-id, fId=my-test.xml, fParameters={path=/tmp/home/my-test.xml}, fJsonParameters=]";
    private static final String EXPECTED2_TO_STRING = "TmfConfiguration[fName=/tmp/my-test.xml, fDescription=descriptor, fType=my-source-id, fId=my-test.xml, fParameters={}, fJsonParameters={\"path\":\"/tmp/my-test.xml\"}]";

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------
    /**
     * Test builder, constructor and getter/setters.
     */
    @Test
    public void testBuilder() {
        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setSourceTypeId(SOURCE_ID)
                .setParameters(PARAM);
            ITmfConfiguration config = builder.build();
            assertEquals(PATH, config.getName());
            assertEquals(ID, config.getId());
            assertEquals(DESC, config.getDescription());
            assertEquals(SOURCE_ID, config.getSourceTypeId());
            assertEquals(PARAM, config.getParameters());
    }

    /**
     * Test builder with missing params.
     */
    @Test
    public void testBuilderMissingParams() {
        // Test missing ID
        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setDescription(DESC)
                .setSourceTypeId(SOURCE_ID)
                .setParameters(PARAM);
        try {
            builder.build();
            fail("No exception created");
        } catch (IllegalStateException e) {
            // success
        }

        // Test blank ID
        builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setSourceTypeId(SOURCE_ID)
                .setId("\n") // blank
                .setDescription(DESC)
                .setParameters(PARAM);
        try {
            builder.build();
            fail("No exception created");
        } catch (IllegalStateException e) {
            // success
        }

        // Test successful builder
        builder = new TmfConfiguration.Builder()
            .setId(ID)
            .setSourceTypeId(SOURCE_ID);
        builder.build();
        // success - no exception created
    }

    /**
     * Test {@Link TmfConfiguration#equals()}
     */
    @Test
    public void testEquality() {
        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setSourceTypeId(SOURCE_ID)
                .setParameters(PARAM)
                .setJsonParameters("{\"path\":\"" + PATH +"\"}");
        ITmfConfiguration baseConfiguration = builder.build();

        // Make sure it is equal to itself
        ITmfConfiguration testConfig = builder.build();
        assertEquals(baseConfiguration, testConfig);
        assertEquals(testConfig, baseConfiguration);

        // Change each of the variable and make sure result is not equal
        builder.setName("Other path");
        testConfig = builder.build();
        assertNotEquals(baseConfiguration, testConfig);
        assertNotEquals(testConfig, baseConfiguration);

        builder.setName(PATH);
        builder.setId("Other Id");
        testConfig = builder.build();
        assertNotEquals(baseConfiguration, testConfig);
        assertNotEquals(testConfig, baseConfiguration);

        builder.setId(ID);
        builder.setDescription("Other desc");
        testConfig = builder.build();
        assertNotEquals(baseConfiguration, testConfig);
        assertNotEquals(testConfig, baseConfiguration);

        builder.setDescription(DESC);
        builder.setSourceTypeId("Other type id");
        testConfig = builder.build();
        assertNotEquals(baseConfiguration, testConfig);
        assertNotEquals(testConfig, baseConfiguration);

        builder.setSourceTypeId(SOURCE_ID);
        builder.setParameters(ImmutableMap.of("path", "/tmp/home/my-other.xml"));
        testConfig = builder.build();
        assertNotEquals(baseConfiguration, testConfig);
        assertNotEquals(testConfig, baseConfiguration);
    }

    /**
     * Test {@Link TmfConfiguration#toString()}
     **/
    @Test
    public void testToString() {
        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setSourceTypeId(SOURCE_ID)
                .setParameters(PARAM);
        assertEquals(EXPECTED_TO_STRING, builder.build().toString());

        builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setSourceTypeId(SOURCE_ID)
                .setParameters(Collections.emptyMap())
                .setJsonParameters("{\"path\":\"" + PATH +"\"}");
        assertEquals(EXPECTED2_TO_STRING, builder.build().toString());
    }

    /**
     * Test {@Link TmfConfiguration#hashCode()}
     */
    @Test
    public void testHashCode() {
        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setSourceTypeId(SOURCE_ID)
                .setParameters(PARAM);
        ITmfConfiguration config1 = builder.build();

        builder = new TmfConfiguration.Builder()
                .setName(PATH + "1")
                .setId(ID + "1")
                .setDescription(DESC + "1")
                .setSourceTypeId(SOURCE_ID + "1")
                .setParameters(ImmutableMap.of("path", "/tmp/home/my-other.xml"));

        ITmfConfiguration config2 = builder.build();

        builder = new TmfConfiguration.Builder()
                .setName(PATH + "1")
                .setId(ID + "1")
                .setDescription(DESC + "1")
                .setSourceTypeId(SOURCE_ID + "1")
                .setJsonParameters("{\"path\":\"" + PATH +"\"}");

        ITmfConfiguration config3 = builder.build();

        assertEquals(config1.hashCode(), config1.hashCode());
        assertEquals(config2.hashCode(), config2.hashCode());
        assertEquals(config3.hashCode(), config3.hashCode());
        assertNotEquals(config1.hashCode(), config2.hashCode());
        assertNotEquals(config2.hashCode(), config3.hashCode());
    }
}
