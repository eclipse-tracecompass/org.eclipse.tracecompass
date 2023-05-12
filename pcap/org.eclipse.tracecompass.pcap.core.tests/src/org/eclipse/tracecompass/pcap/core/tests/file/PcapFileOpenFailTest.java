/*******************************************************************************
 * Copyright (c) 2014, 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Vincent Perot - Initial API and implementation
 *   Viet-Hung Phan - Support pcapNg
 *******************************************************************************/

package org.eclipse.tracecompass.pcap.core.tests.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;

import org.eclipse.tracecompass.internal.pcap.core.trace.BadPcapFileException;
import org.eclipse.tracecompass.internal.pcap.core.util.PcapHelper;
import org.eclipse.tracecompass.pcap.core.tests.shared.PcapTestTrace;
import org.junit.Test;

/**
 * JUnit Class that tests the opening of non-valid pcap files.
 *
 * @author Vincent Perot
 */

public class PcapFileOpenFailTest {

    /**
     * Test that tries to open a pcap with a bad magic number
     *
     * @throws IOException
     *             Thrown when an IO error occurs. Fails the test.
     * @throws BadPcapFileException
     *             Won't happen
     */
    @Test
    public void fileOpenBadPcapTest() throws IOException, BadPcapFileException {
        PcapTestTrace trace = PcapTestTrace.BAD_PCAPFILE;
        assumeTrue(trace.exists());
        assertNull(PcapHelper.getPcapFile(trace.getPath()));
    }

    /**
     * Test that tries to open a non-pcap binary file
     *
     * @throws IOException
     *             Thrown when an IO error occurs. Fails the test.
     * @throws BadPcapFileException
     *             Won't happen
     */
    @Test
    public void fileOpenBinaryFile() throws IOException, BadPcapFileException {
        PcapTestTrace trace = PcapTestTrace.KERNEL_TRACE;
        assumeTrue(trace.exists());
        assertNull(PcapHelper.getPcapFile(trace.getPath()));
    }

    /**
     * Test that tries to open a directory
     *
     * @throws IOException
     *             Thrown when an IO error occurs. Fails the test.
     */
    @Test
    public void FileOpenDirectory() throws IOException {
        PcapTestTrace trace = PcapTestTrace.KERNEL_DIRECTORY;
        assumeTrue(trace.exists());
        try {
            PcapHelper.getPcapFile(trace.getPath());
            fail("The file was accepted even though it is not a pcapNg file!");
        } catch (IOException | BadPcapFileException e) {
            assertEquals("Bad Pcap File.", e.getMessage());
        }
    }
}
