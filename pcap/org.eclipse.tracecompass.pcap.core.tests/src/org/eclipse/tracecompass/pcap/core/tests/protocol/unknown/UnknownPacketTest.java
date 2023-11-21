/*******************************************************************************
 * Copyright (c) 2014, 2023 Ericsson
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

package org.eclipse.tracecompass.pcap.core.tests.protocol.unknown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import org.eclipse.tracecompass.internal.pcap.core.protocol.PcapProtocol;
import org.eclipse.tracecompass.internal.pcap.core.protocol.unknown.UnknownEndpoint;
import org.eclipse.tracecompass.internal.pcap.core.protocol.unknown.UnknownPacket;
import org.eclipse.tracecompass.internal.pcap.core.trace.BadPcapFileException;
import org.eclipse.tracecompass.internal.pcap.core.trace.PcapFile;
import org.eclipse.tracecompass.pcap.core.tests.shared.PcapTestTrace;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * JUnit Class that tests the UnknownPacket class and its method.
 *
 * @author Vincent Perot
 */
public class UnknownPacketTest {

    private static final Map<String, String> EXPECTED_FIELDS = ImmutableMap.of(
            "Binary", "00 01 02 31 32 33 41 42 43 61 62 63 fd fe ff",
            "Character", "···123ABCabc···"
            );

    private static final String fToString = "Payload: 00 01 02 31 32 33 41 42 43 61 62 63 fd fe ff";

    private ByteBuffer fPacket;

    /**
     * Initialize the packet.
     */
    @Before
    public void initialize() {
        fPacket = ByteBuffer.allocate(15);
        fPacket.order(ByteOrder.BIG_ENDIAN);

        // Payload - 15 bytes
        fPacket.put((byte) 0x00);
        fPacket.put((byte) 0x01);
        fPacket.put((byte) 0x02);
        fPacket.put((byte) 0x31);
        fPacket.put((byte) 0x32);
        fPacket.put((byte) 0x33);
        fPacket.put((byte) 0x41);
        fPacket.put((byte) 0x42);
        fPacket.put((byte) 0x43);
        fPacket.put((byte) 0x61);
        fPacket.put((byte) 0x62);
        fPacket.put((byte) 0x63);
        fPacket.put((byte) 0xfd);
        fPacket.put((byte) 0xfe);
        fPacket.put((byte) 0xff);

        fPacket.flip();
    }

    /**
     * Test that verify the correctness of the UnknownPacket's methods.
     * @throws BadPcapFileException
     *             Thrown when the file is erroneous. Fails the test.
     * @throws IOException
     *             Thrown when an IO error occurs. Fails the test.
     */
    @Test
    public void CompleteUnknownPacketTest() throws IOException, BadPcapFileException {
        PcapTestTrace trace = PcapTestTrace.MOSTLY_TCP;
        assumeTrue(trace.exists());
        // Get a right pcap/pcapNg trace
        try (PcapFile dummy = trace.getTrace();) {
            ByteBuffer byteBuffer = fPacket;
            if (byteBuffer == null) {
                fail("CompleteUnknownPacketTest has failed!");
                return;
            }
            UnknownPacket packet = new UnknownPacket(dummy, null, byteBuffer);

            // Protocol Testing
            assertEquals(PcapProtocol.UNKNOWN, packet.getProtocol());
            assertTrue(packet.hasProtocol(PcapProtocol.UNKNOWN));
            assertFalse(packet.hasProtocol(PcapProtocol.UDP));

            // Abstract methods Testing
            assertTrue(packet.validate());
            assertFalse(packet.equals(null));
            assertEquals(new UnknownPacket(dummy, null, byteBuffer), packet);
            assertEquals(new UnknownPacket(dummy, null, byteBuffer).hashCode(), packet.hashCode());

            assertEquals(EXPECTED_FIELDS, packet.getFields());
            assertEquals(fToString, packet.toString());
            assertEquals("Len: 15 bytes", packet.getLocalSummaryString());
            assertEquals("Data: 15 bytes", packet.getGlobalSummaryString());
            // TODO take care of plural form.

            // Unknown Endpoints are never equal!
            assertFalse(packet.getSourceEndpoint().equals(new UnknownEndpoint(packet, true)));
            assertFalse(packet.getDestinationEndpoint().equals(new UnknownEndpoint(packet, false)));

            fPacket.position(0);
            byte[] payload = new byte[15];
            fPacket.get(payload);
            ByteBuffer payloadBB = ByteBuffer.wrap(payload);
            payloadBB.flip();

            assertEquals(payloadBB, packet.getPayload());

            // Packet-specific methods Testing
            // None
        }
    }
}
