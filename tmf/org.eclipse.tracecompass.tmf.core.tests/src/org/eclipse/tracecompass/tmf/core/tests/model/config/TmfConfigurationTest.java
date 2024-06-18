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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * JUnit Test class to test {@link TmfConfiguration}
 */
public class TmfConfigurationTest {

    // ------------------------------------------------------------------------
    // Test data
    // ------------------------------------------------------------------------
    /** Temporary folder */
    @ClassRule
    public static TemporaryFolder fTemporaryFolder = new TemporaryFolder();
    /** JSON file to write/read to/from */
    public static File fJsonFile;

    private static final String PATH = "/tmp/my-test.xml";
    private static final String ID = "my-test.xml";
    private static final String UUID = "c52356b5-c7c4-3b68-b834-6daf1d9f8a3b";
    private static final String DESC = "descriptor";
    private static final String SOURCE_ID = "my-source-id";
    private static final @NonNull Map<@NonNull String, @NonNull String> PARAM = ImmutableMap.of("path", "/tmp/home/my-test.xml");
    private static final String EXPECTED_TO_STRING = "TmfConfiguration[fName=/tmp/my-test.xml, fDescription=descriptor, fType=my-source-id, fId=my-test.xml, fParameters={path=/tmp/home/my-test.xml}]";

    /**
     * Test class setup
     *
     * @throws IOException
     *             if error occurs
     */
    @BeforeClass
    public static void setup() throws IOException {
        fJsonFile = fTemporaryFolder.newFile(UUID + ".json");
    }

    /**
     * Test class cleanup
     */
    @AfterClass
    public static void cleanup() {
        if (fJsonFile != null && fJsonFile.exists()) {
            fJsonFile.delete();
        }
    }

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

            // Don't set ID to trigger generation of UUID
            builder = new TmfConfiguration.Builder()
                    .setName(PATH)
                    .setDescription(DESC)
                    .setSourceTypeId(SOURCE_ID)
                    .setParameters(PARAM);
                config = builder.build();
                assertEquals(PATH, config.getName());
                assertEquals(UUID, config.getId());
                assertEquals(DESC, config.getDescription());
                assertEquals(SOURCE_ID, config.getSourceTypeId());
                assertEquals(PARAM, config.getParameters());
    }

    /**
     * Test builder with missing params.
     */
    @Test
    public void testBuilderMissingParams() {
        // Test missing source type ID
        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setParameters(PARAM);
        try {
            builder.build();
            fail("No exception created");
        } catch (IllegalStateException e) {
            // success
        }

        // Test blank source type ID
        builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setSourceTypeId("  ") // blank
                .setId(ID)
                .setDescription(DESC)
                .setParameters(PARAM);
        try {
            builder.build();
            fail("No exception created");
        } catch (IllegalStateException e) {
            // success
        }

        // Test missing ID, which will trigger UUID generation
        builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setDescription(DESC)
                .setSourceTypeId(SOURCE_ID)
                .setParameters(PARAM);
        ITmfConfiguration config = builder.build();
        assertEquals(UUID, config.getId());

        // Test blank ID which triggers UUID generation
        builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setSourceTypeId(SOURCE_ID)
                .setId("\n") // blank
                .setDescription(DESC)
                .setParameters(PARAM);
        config = builder.build();
        assertEquals(UUID, config.getId());

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
                .setParameters(PARAM);
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

        assertEquals(config1.hashCode(), config1.hashCode());
        assertEquals(config2.hashCode(), config2.hashCode());
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    /**
     * Test GSON serialization/deserialization}
     *
     * @throws IOException
     *             if error occurs
     */
    @Test
    public void testJson() throws IOException {
        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setDescription(DESC)
                .setSourceTypeId(SOURCE_ID)
                .setParameters(PARAM);
        ITmfConfiguration config = builder.build();
        writeConfiguration(config);
        ITmfConfiguration readConfig = readConfiguration();
        assertEquals(config, readConfig);
    }

    private static @Nullable ITmfConfiguration readConfiguration() throws IOException {
        ITmfConfiguration config = null;
        Type listType = new TypeToken<TmfConfiguration>() {
        }.getType();
        try (Reader reader = new FileReader(fJsonFile)) {
            config = new Gson().fromJson(reader, listType);
        }
        return config;
    }

    private static void writeConfiguration(ITmfConfiguration config) throws IOException {
        try (Writer writer = new FileWriter(fJsonFile)) {
            writer.append(new Gson().toJson(config));
        }
    }
}
