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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSource;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceManager;
import org.eclipse.tracecompass.tmf.tests.stubs.model.config.TestConfigurationSource;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit Test class to test {@link TmfConfigurationSourceManager}
 */
public class TmfConfigurationSourceManagerTest {

    // ------------------------------------------------------------------------
    // Test data
    // ------------------------------------------------------------------------
    private static final String UNKNOWN_TYPE = "test-test-test";

    private static TmfConfigurationSourceManager sfInstance;

    // ------------------------------------------------------------------------
    // Test setup
    // ------------------------------------------------------------------------
    /**
     * Test initialization.
     */
    @Before
    public void setUp() {
        sfInstance = TmfConfigurationSourceManager.getInstance();
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------
    /**
     * Test {@link TmfConfigurationSourceManager#getConfigurationSourceTypes()}
     */
    @Test
    public void testConfigurationSourceTypes() {
        List<@NonNull ITmfConfigurationSourceType> sources = sfInstance.getConfigurationSourceTypes();
        assertFalse(sources.isEmpty());
        assertFalse(sources.stream().anyMatch(config -> config.getId().equals(UNKNOWN_TYPE)));
        assertTrue(sources.stream().anyMatch(config -> config.getId().equals(TestConfigurationSource.STUB_ANALYSIS_TYPE_ID)));
    }

    /**
     * Test {@link TmfConfigurationSourceManager#getConfigurationSource(String)}
     */
    @Test
    public void testConfigurationSource() {
        ITmfConfigurationSource source = sfInstance.getConfigurationSource(UNKNOWN_TYPE);
        assertNull(source);
        source = sfInstance.getConfigurationSource(TestConfigurationSource.STUB_ANALYSIS_TYPE_ID);
        assertNotNull(source);
        assertTrue(source instanceof TestConfigurationSource);
    }
}
