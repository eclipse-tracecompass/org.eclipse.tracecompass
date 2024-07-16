/**********************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.tests.segmentstore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule;
import org.eclipse.tracecompass.analysis.timing.core.tests.stubs.segmentstore.StubSegmentStoreOnDiskProvider;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link AbstractSegmentStoreAnalysisModule}
 *
 * @author Bernd Hufmann
 */
public class SegmentStoreAnalysisTest {
    private static TmfXmlTraceStub fTrace;

    /**
     * Set-up resources
     */
    @BeforeClass
    public static void init() {
        TmfXmlTraceStubNs trace = new TmfXmlTraceStubNs();
        assertNotNull(trace);
        fTrace = trace;
    }

    /**
     * Disposes resources
     */
    @AfterClass
    public static void tearDown() {
        fTrace.dispose();
    }

    // --------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    /**
     * Test clearing persistent data
     *
     * @throws TmfAnalysisException
     *             An exception when setting the trace
     */
    @Test
    public void testClearPersistentData() throws TmfAnalysisException {
        StubSegmentStoreOnDiskProvider module = new StubSegmentStoreOnDiskProvider();
        StubSegmentStoreOnDiskProvider module2 = new StubSegmentStoreOnDiskProvider();
        try {
            ITmfTrace trace = fTrace;
            assertNotNull(trace);
            module.setTrace(trace);
            module2.setTrace(trace);

            // Execute the first module
            module.schedule();
            assertTrue(module.waitForCompletion());

            // Check if data file exists
            assertTrue(Files.exists(module.getDataFilePath()));

            // Delete data file while open
            module.clearPersistentData();

            // The data file should be deleted
            Path dataFile = module.getDataFilePath();
            assertNotNull(dataFile);
            assertFalse(Files.exists(dataFile));

            // Re-schedule
            module.schedule();
            assertTrue(module.waitForCompletion());
            // Dispose module (close file)
            module.dispose();

            module2.clearPersistentData();

            // Delete state system file while closed
            dataFile = module.getDataFilePath();
            assertNotNull(dataFile);
            assertFalse(Files.exists(dataFile));
        } finally {
            module.dispose();
            module2.dispose();
        }
    }

}
