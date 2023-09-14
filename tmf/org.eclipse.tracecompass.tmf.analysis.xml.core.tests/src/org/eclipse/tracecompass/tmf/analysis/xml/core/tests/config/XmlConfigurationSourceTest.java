/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.analysis.xml.core.tests.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.config.XmlConfigurationSource;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.tests.Activator;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSource;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceManager;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Tests for the {@link XmlConfigurationSource} class
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("null")
public class XmlConfigurationSourceTest {

    private static final String UNKNOWN_TYPE = "test-test-test";
    private static final String XML_ANALYSIS_TYPE_ID = "org.eclipse.tracecompass.tmf.core.config.xmlsourcetype"; //$NON-NLS-1$
    private static final Path PATH_INVALID = new Path("test_xml_files/test_invalid");
    private static final Path PATH_VALID = new Path("test_xml_files/test_valid");

    private static String VALID_NAME = "kvm_exits";
    private static String VALID_XML_FILE = VALID_NAME + ".xml";
    private static String INVALID_XML_FILE = "invalid_condition_operators.xml";
    private static String PATH_TO_INVALID_FILE = getPath(PATH_INVALID, INVALID_XML_FILE);
    private static String PATH_TO_VALID_FILE = getPath(PATH_VALID, VALID_XML_FILE);
    private static String PATH_TO_VALID_FILE2 = getPath(PATH_VALID, "state_provider_placement.xml"); //$NON-NLS-1$
    private static final String EXPECTED_TYPE_NAME = "XML Data-driven analyses"; //$NON-NLS-1$
    private static final String EXPECTED_TYPE_DESCRIPTION = "Data-driven analyses described in XML"; //$NON-NLS-1$
    private static final String EXPECTED_KEY_NAME = "path";
    private static final String EXPECTED_PARAM_DESCRIPTION = "URI to XML analysis file";
    private static final String EXPECTED_DATA_TYPE = "STRING";
    private static final String EXPECTED_CONFIG_NAME = VALID_NAME;
    private static final String EXPECTED_CONFIG_ID = VALID_XML_FILE;
    private static final String EXPECTED_CONFIG_DESCRIPTION = "XML Data-driven analysis: " + VALID_NAME;
    private static Map<String, Object> VALID_PARAM = ImmutableMap.of(EXPECTED_KEY_NAME, PATH_TO_VALID_FILE);
    private static Map<String, Object> VALID_PARAM2 = ImmutableMap.of(EXPECTED_KEY_NAME, PATH_TO_VALID_FILE2);
    private static Map<String, Object> INVALID_PARAM = ImmutableMap.of(EXPECTED_KEY_NAME, PATH_TO_INVALID_FILE);
    private static final String EXPECTED_MESSAGE_NO_PATH = "Missing path"; //$NON-NLS-1$
    private static final String EXPECTED_MESSAGE_FILE_NOT_FOUND = "An error occurred while validating the XML file."; //$NON-NLS-1$
    private static final String EXPECTED_MESSAGE_INVALID_CONFIG = "No such configuration with ID: " + UNKNOWN_TYPE; //$NON-NLS-1$
    private static final String EXPECTED_MESSAGE_FILE_MISMATCH = "File mismatch"; //$NON-NLS-1$

    private static ITmfConfigurationSource sfXmlConfigSource;
    private static TmfConfigurationSourceManager sfInstance;
    /**
     * Test setup
     */
    @BeforeClass
    public static void init() {
        sfInstance = TmfConfigurationSourceManager.getInstance();
        assertNotNull(sfInstance);
        sfXmlConfigSource = sfInstance.getConfigurationSource(XML_ANALYSIS_TYPE_ID);
        assertNotNull(sfXmlConfigSource);
    }

    /**
     * Empty the XML directory after the test
     */
    @After
    public void emptyXmlFolder() {
        // Clean-up config source
        Iterator<ITmfConfiguration> iter = sfXmlConfigSource.getConfigurations().iterator();
        while (iter.hasNext()) {
            sfXmlConfigSource.remove(iter.next().getId());
        }

        // Delete any remaining xml files, if any
        File fFolder = XmlUtils.getXmlFilesPath().toFile();
        if (!(fFolder.isDirectory() && fFolder.exists())) {
            return;
        }
        for (File xmlFile : fFolder.listFiles()) {
            xmlFile.delete();
        }
    }

    /**
     * Test the constructor of the XML configuration source
     *
     * @throws TmfConfigurationException
     *             If an error occurred
     */
    @Test
    public void testConstructor() throws TmfConfigurationException {
        // Make sure that an xml file is loaded
        ITmfConfiguration config =  sfXmlConfigSource.create(VALID_PARAM);

        XmlConfigurationSource instance = new XmlConfigurationSource();
        List<ITmfConfiguration> configurations = instance.getConfigurations();
        assertTrue(configurations.contains(config));
    }

    /**
     * Test the ITmfConfigurationSourceType for this XML configuration source
     */
    @Test
    public void testSourceType() {
        ITmfConfigurationSourceType type = sfXmlConfigSource.getConfigurationSourceType();
        assertEquals(XML_ANALYSIS_TYPE_ID, type.getId());
        assertEquals(EXPECTED_TYPE_NAME, type.getName());
        assertEquals(EXPECTED_TYPE_DESCRIPTION, type.getDescription());
        List<ITmfConfigParamDescriptor> descriptors = type.getConfigParamDescriptors();
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        ITmfConfigParamDescriptor desc = descriptors.get(0);
        assertEquals(EXPECTED_KEY_NAME, desc.getKeyName());
        assertEquals(EXPECTED_DATA_TYPE, desc.getDataType());
        assertEquals(EXPECTED_PARAM_DESCRIPTION, desc.getDescription());
        assertTrue(desc.isRequired());
    }

    /**
     * Test the creation of a configuration instance
     */
    @Test
    public void testCreation() {
        // Test missing path
        Map<String, Object> param = Collections.emptyMap();
        try {
            sfXmlConfigSource.create(param);
        } catch (TmfConfigurationException e) {
            // success
            assertEquals(EXPECTED_MESSAGE_NO_PATH, e.getMessage());
        }

        // Test XML file doesn't exist
        param = ImmutableMap.of(EXPECTED_KEY_NAME, XmlUtils.getXmlFilesPath().append(UNKNOWN_TYPE).toOSString());
        try {
            sfXmlConfigSource.create(param);
        } catch (TmfConfigurationException e) {
            // success
            assertEquals(EXPECTED_MESSAGE_FILE_NOT_FOUND, e.getMessage());
        }

        // Test invalid XML file
        try {
            sfXmlConfigSource.create(INVALID_PARAM);
        } catch (TmfConfigurationException e) {
            // success
        }

        try {
            ITmfConfiguration config = sfXmlConfigSource.create(VALID_PARAM);
            validateConfig(PATH_TO_VALID_FILE, config);
        } catch (TmfConfigurationException e) {
            fail();
        }
    }

    /**
     * Test the update of a configuration instance
     */
    @Test
    public void testUpdate() {
        ITmfConfiguration config = createConfig(PATH_TO_VALID_FILE);
        assertNotNull(config);

        // Test missing path
        Map<String, Object> param = Collections.emptyMap();
        try {
            sfXmlConfigSource.update(config.getId(), param);
        } catch (TmfConfigurationException e) {
            // success
            assertEquals(EXPECTED_MESSAGE_NO_PATH, e.getMessage());
        }

        // Test config doesn't exist
        try {
            sfXmlConfigSource.update(UNKNOWN_TYPE, param);
        } catch (TmfConfigurationException e) {
            // success
            assertEquals(EXPECTED_MESSAGE_INVALID_CONFIG, e.getMessage());
        }

        // Test XML file doesn't exist
        param = ImmutableMap.of(EXPECTED_KEY_NAME, XmlUtils.getXmlFilesPath().append(UNKNOWN_TYPE).toOSString());
        try {
            sfXmlConfigSource.update(config.getId(), param);
        } catch (TmfConfigurationException e) {
            // success
            assertEquals(EXPECTED_MESSAGE_FILE_NOT_FOUND, e.getMessage());
        }

        // Test invalid XML file
        try {
            sfXmlConfigSource.update(EXPECTED_CONFIG_ID, INVALID_PARAM);
        } catch (TmfConfigurationException e) {
            // success

        }

        // Test file name mismatch... not allowed to use different xmlfile name
        try {
            sfXmlConfigSource.update(config.getId(), VALID_PARAM2);
        } catch (TmfConfigurationException e) {
            assertEquals(EXPECTED_MESSAGE_FILE_MISMATCH, e.getMessage());
        }

        try {
            ITmfConfiguration configUpdated = sfXmlConfigSource.update(config.getId(), VALID_PARAM);
            validateConfig(PATH_TO_VALID_FILE, configUpdated);
        } catch (TmfConfigurationException e) {
            fail();
        }
    }

    /**
     * Test contains
     */
    @Test
    public void testContains() {
        createConfig(PATH_TO_VALID_FILE);
        assertTrue(sfXmlConfigSource.contains(EXPECTED_CONFIG_ID));
        assertFalse(sfXmlConfigSource.contains(UNKNOWN_TYPE));
    }

    /**
     * Test the removal
     */
    @Test
    public void testRemove() {
        createConfig(PATH_TO_VALID_FILE);
        ITmfConfiguration config = sfXmlConfigSource.remove(UNKNOWN_TYPE);
        assertNull(config);
        config = sfXmlConfigSource.remove(EXPECTED_CONFIG_ID);
        validateConfig(PATH_TO_VALID_FILE, config);
    }

    /**
     * Test list configuration
     */
    @Test
    public void testGetConfiguration() {
        List<ITmfConfiguration> configurations = sfXmlConfigSource.getConfigurations();
        assertTrue(configurations.isEmpty());
        ITmfConfiguration config = createConfig(PATH_TO_VALID_FILE);
        assertNotNull(config);
        configurations = sfXmlConfigSource.getConfigurations();
        assertFalse(configurations.isEmpty());
        assertTrue(configurations.contains(config));
    }

    private static ITmfConfiguration createConfig(String path) {
        Map<String, Object> param = ImmutableMap.of("path", path);
        try {
            return sfXmlConfigSource.create(param);
        } catch (TmfConfigurationException e) {
            // Nothing to do
        }
        return null;
    }

    private static void validateConfig(String path, ITmfConfiguration config) {
        assertNotNull(config);
        assertEquals(EXPECTED_CONFIG_NAME, config.getName());
        assertEquals(EXPECTED_CONFIG_ID, config.getId());
        assertEquals(XML_ANALYSIS_TYPE_ID, config.getSourceTypeId());
        assertEquals(EXPECTED_CONFIG_DESCRIPTION, config.getDescription());
        assertTrue(config.getParameters().isEmpty());
    }

    private static String getPath(Path folder, String name) {
        IPath path = Activator.getAbsolutePath(folder).append(name);
        File file = path.toFile();
        assertTrue(file.exists());
        return Objects.requireNonNull(file.getAbsolutePath());
    }
}
