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
import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableList;

/**
 * JUnit Test class to test {@link TmfConfigurationSourceType}
 */
public class TmfConfigurationSourceTypeTest {

    // ------------------------------------------------------------------------
    // Test data
    // ------------------------------------------------------------------------
    private static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();
    private static final String PATH = "/tmp/my-test.xml";
    private static final String ID = "my-test.xml";
    private static final String DESC = "descriptor";
    private static File fsSchemaFile;
    private static final @NonNull List<@NonNull ITmfConfigParamDescriptor> PARAM = ImmutableList.of(new TmfConfigParamDescriptor.Builder().setKeyName("path").build());
    private static final String EXPECTED_TO_STRING = "TmfConfigurationSourceType[fName=/tmp/my-test.xml, fDescription=descriptor, fId=my-test.xml, fKeys=[TmfConfigParamDescriptor[fKeyName=path, fDataType=STRING, fIsRequired=true, fDescription=]], fSchemaFile=null]";
    private static final String EXPECTED_TO_STRING_WITH_SCHEMA = "TmfConfigurationSourceType[fName=/tmp/my-test.xml, fDescription=descriptor, fId=my-test.xml, fKeys=[], fSchemaFile=schema.json]";

    private static final String SCHEMA_FILE_NAME = "schema.json";

    // ------------------------------------------------------------------------
    // Class setup and cleanup
    // ------------------------------------------------------------------------

    /**
     * Class setup
     *
     * @throws IOException
     *             if IO error happens
     */
    @BeforeClass
    public static void setupClass() throws IOException {
        TEMPORARY_FOLDER.create();
        fsSchemaFile = TEMPORARY_FOLDER.newFile(SCHEMA_FILE_NAME);
    }

    /**
     * Class cleanup
     */
    @AfterClass
    public static void cleanupClass(){
        TEMPORARY_FOLDER.delete();
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------

    /**
     * Test builder, constructor and getter/setters.
     */
    @Test
    public void testBuilder() {
        TmfConfigurationSourceType.Builder builder = new TmfConfigurationSourceType.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setConfigParamDescriptors(PARAM);
            ITmfConfigurationSourceType config = builder.build();
            assertEquals(PATH, config.getName());
            assertEquals(ID, config.getId());
            assertEquals(DESC, config.getDescription());
            assertEquals(PARAM, config.getConfigParamDescriptors());


    }

    /**
     * Test builder with missing params.
     */
    @Test
    public void testBuilderMissingParams() {

        // Test missing name
        TmfConfigurationSourceType.Builder builder = new TmfConfigurationSourceType.Builder()
                .setId(ID)
                .setDescription(DESC)
                .setConfigParamDescriptors(PARAM);
        try {
            builder.build();
            fail("No exception created");
        } catch (IllegalStateException e) {
            // success
        }

       // Test missing blank name
        builder = new TmfConfigurationSourceType.Builder()
                .setName("  ") // blank)
                .setId(ID)
                .setDescription(DESC)
                .setConfigParamDescriptors(PARAM);
        try {
            builder.build();
            fail("No exception created");
        } catch (IllegalStateException e) {
            // success
        }

        // Test missing ID
        builder = new TmfConfigurationSourceType.Builder()
                .setName(PATH)
                .setDescription(DESC)
                .setConfigParamDescriptors(PARAM);
        try {
            builder.build();
            fail("No exception created");
        } catch (IllegalStateException e) {
            // success
        }

        // Test blank ID
        builder = new TmfConfigurationSourceType.Builder()
                .setName(PATH)
                .setId("\n") // blank
                .setDescription(DESC)
                .setConfigParamDescriptors(PARAM);
        try {
            builder.build();
            fail("No exception created");
        } catch (IllegalStateException e) {
            // success
        }

        // Test non-existing JSON schema file
        File schemaFile = new File("schema.json");
        builder = new TmfConfigurationSourceType.Builder()
                .setName(PATH)
                .setId("\n") // blank
                .setDescription(DESC)
                .setSchemaFile(schemaFile);
        try {
            builder.build();
            fail("No exception created");
        } catch (IllegalStateException e) {
            // success
        }

        // Test successful builder
        builder = new TmfConfigurationSourceType.Builder()
            .setId(ID)
            .setName(PATH);
        builder.build();
        // success - no exception created
    }

    /**
     * Test {@Link TmfConfigurationSourceType#equals()}
     */
    @Test
    public void testEquality() {
        TmfConfigurationSourceType.Builder builder = new TmfConfigurationSourceType.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setConfigParamDescriptors(PARAM);
        ITmfConfigurationSourceType baseConfiguration = builder.build();

        // Make sure it is equal to itself
        ITmfConfigurationSourceType testConfig = builder.build();
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
        builder.setConfigParamDescriptors(ImmutableList.of(new TmfConfigParamDescriptor.Builder().setKeyName("path2").build()));
        testConfig = builder.build();
        assertNotEquals(baseConfiguration, testConfig);
        assertNotEquals(testConfig, baseConfiguration);
    }

    /**
     * Test {@Link TmfConfigurationSourceType#toString()}
     **/
    @Test
    public void testToString() {
        TmfConfigurationSourceType.Builder builder = new TmfConfigurationSourceType.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setConfigParamDescriptors(PARAM);
        assertEquals(EXPECTED_TO_STRING, builder.build().toString());
        builder = new TmfConfigurationSourceType.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setSchemaFile(fsSchemaFile);
        assertEquals(EXPECTED_TO_STRING_WITH_SCHEMA, builder.build().toString());
    }

    /**
     * Test {@Link TmfConfigurationSourceType#hashCode()}
     */
    @Test
    public void testHashCode() {
        TmfConfigurationSourceType.Builder builder = new TmfConfigurationSourceType.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setConfigParamDescriptors(PARAM);
        ITmfConfigurationSourceType config1 = builder.build();

        builder = new TmfConfigurationSourceType.Builder()
                .setName(PATH + "1")
                .setId(ID + "1")
                .setDescription(DESC + "1")
                .setConfigParamDescriptors(ImmutableList.of(new TmfConfigParamDescriptor.Builder().setKeyName("path2").build()));

        ITmfConfigurationSourceType config2 = builder.build();

        assertEquals(config1.hashCode(), config1.hashCode());
        assertEquals(config2.hashCode(), config2.hashCode());
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }
}
