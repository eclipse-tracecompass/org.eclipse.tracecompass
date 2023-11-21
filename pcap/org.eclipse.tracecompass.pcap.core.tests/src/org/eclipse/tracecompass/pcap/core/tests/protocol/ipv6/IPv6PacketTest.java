/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.pcap.core.tests.protocol.ipv6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.tracecompass.internal.pcap.core.packet.BadPacketException;
import org.eclipse.tracecompass.internal.pcap.core.protocol.PcapProtocol;
import org.eclipse.tracecompass.internal.pcap.core.protocol.ipv6.IPv6Endpoint;
import org.eclipse.tracecompass.internal.pcap.core.protocol.ipv6.IPv6Packet;
import org.eclipse.tracecompass.internal.pcap.core.trace.BadPcapFileException;
import org.eclipse.tracecompass.internal.pcap.core.trace.PcapFile;
import org.eclipse.tracecompass.pcap.core.tests.shared.PcapTestTrace;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit Class that tests the IPv6Packet class and its method.
 */
public class IPv6PacketTest {

    private static final Map<String, String> EXPECTED_FIELDS;
    static {
        EXPECTED_FIELDS = new LinkedHashMap<>();
        EXPECTED_FIELDS.put("Version", "6");
        EXPECTED_FIELDS.put("Traffic Class", "0x12");
        EXPECTED_FIELDS.put("Flow Label", "0xabcde");
        EXPECTED_FIELDS.put("Payload Length", "1 bytes");
        EXPECTED_FIELDS.put("Next Header", "0xfe");
        EXPECTED_FIELDS.put("Hop Limit", "63");
        EXPECTED_FIELDS.put("Source IP Address", "fedc:ba98:7654:3210:fedc:ba98:7654:3210");
        EXPECTED_FIELDS.put("Destination IP Address", "123:4567:89ab:cdef:123:4567:89ab:cdef");
    }

    private static final String EXPECTED_TOSTRING;
    static {
        StringBuilder sb = new StringBuilder();
        sb.append("Internet Protocol Version 6, Source: fedc:ba98:7654:3210:fedc:ba98:7654:3210, Destination: 123:4567:89ab:cdef:123:4567:89ab:cdef\n");
        sb.append("Version: 6, Header Length: 40 bytes, Traffic Class: 0x12, Flow Label: 0xabcde, Payload Length: 1 bytes\n");
        sb.append("Next Header: 0xfe, Hop Limit: 63\n");
        sb.append("Payload: a6");

        EXPECTED_TOSTRING = sb.toString();
    }

    private ByteBuffer fPacket;

    /**
     * Initialize the packet.
     */
    @Before
    public void initialize() {
        fPacket = ByteBuffer.allocate(41);
        fPacket.order(ByteOrder.BIG_ENDIAN);

        // Version (4 bits) + Traffic Class MSB (4 bits)
        fPacket.put((byte) 0x61);

        // Traffic Class LSB (4 bits) + Flow Label MSB (4 bits)
        fPacket.put((byte) 0x2A);

        // Flow Label LSB (20 bits)
        fPacket.put((byte) 0xBC);
        fPacket.put((byte) 0xDE);

        // Payload Length (2 bytes)
        fPacket.put((byte) 0x00);
        fPacket.put((byte) 0x01);

        // Next Header (1 byte)
        fPacket.put((byte) 0xFE);

        // Hop Limit (1 byte)
        fPacket.put((byte) 0x3F);

        // Source Address (16 bytes)
        fPacket.put((byte) 0xFE);
        fPacket.put((byte) 0xDC);
        fPacket.put((byte) 0xBA);
        fPacket.put((byte) 0x98);
        fPacket.put((byte) 0x76);
        fPacket.put((byte) 0x54);
        fPacket.put((byte) 0x32);
        fPacket.put((byte) 0x10);
        fPacket.put((byte) 0xFE);
        fPacket.put((byte) 0xDC);
        fPacket.put((byte) 0xBA);
        fPacket.put((byte) 0x98);
        fPacket.put((byte) 0x76);
        fPacket.put((byte) 0x54);
        fPacket.put((byte) 0x32);
        fPacket.put((byte) 0x10);

        // Destination Address (16 bytes)
        fPacket.put((byte) 0x01);
        fPacket.put((byte) 0x23);
        fPacket.put((byte) 0x45);
        fPacket.put((byte) 0x67);
        fPacket.put((byte) 0x89);
        fPacket.put((byte) 0xAB);
        fPacket.put((byte) 0xCD);
        fPacket.put((byte) 0xEF);
        fPacket.put((byte) 0x01);
        fPacket.put((byte) 0x23);
        fPacket.put((byte) 0x45);
        fPacket.put((byte) 0x67);
        fPacket.put((byte) 0x89);
        fPacket.put((byte) 0xAB);
        fPacket.put((byte) 0xCD);
        fPacket.put((byte) 0xEF);

        // Payload - 1 byte
        fPacket.put((byte) 0xA6);

        fPacket.flip();
    }

    /**
     * Test that verify the correctness of the IPv6Packet's methods.
     *
     * @throws BadPcapFileException
     *             Thrown when the file is erroneous. Fails the test.
     * @throws IOException
     *             Thrown when an IO error occurs. Fails the test.
     * @throws BadPacketException
     *             Thrown when a packet is erroneous. Fails the test.
     */
    @Test
    public void CompleteIPv6PacketTest() throws IOException, BadPcapFileException, BadPacketException {
        PcapTestTrace trace = PcapTestTrace.EMPTY_PCAP;
        assumeTrue(trace.exists());
        try (PcapFile file = trace.getTrace()) {
            ByteBuffer byteBuffer = fPacket;
            if (byteBuffer == null) {
                fail("CompleteIPv6PacketTest has failed!");
                return;
            }
            IPv6Packet packet = new IPv6Packet(file, null, byteBuffer);

            // Protocol Testing
            assertEquals(PcapProtocol.IPV6, packet.getProtocol());
            assertTrue(packet.hasProtocol(PcapProtocol.IPV6));
            assertTrue(packet.hasProtocol(PcapProtocol.UNKNOWN));
            assertFalse(packet.hasProtocol(PcapProtocol.TCP));

            // Abstract methods Testing
            assertTrue(packet.validate());
            IPv6Packet expected = new IPv6Packet(file, null, byteBuffer);
            assertEquals(expected.hashCode(), packet.hashCode());
            assertEquals(expected, packet);

            assertEquals(EXPECTED_FIELDS, packet.getFields());
            assertEquals(EXPECTED_TOSTRING, packet.toString());
            assertEquals("Src: fedc:ba98:7654:3210:fedc:ba98:7654:3210 , Dst: 123:4567:89ab:cdef:123:4567:89ab:cdef", packet.getLocalSummaryString());
            assertEquals("fedc:ba98:7654:3210:fedc:ba98:7654:3210 > 123:4567:89ab:cdef:123:4567:89ab:cdef Len=1", packet.getGlobalSummaryString());

            assertEquals(new IPv6Endpoint(packet, true), packet.getSourceEndpoint());
            assertEquals(new IPv6Endpoint(packet, false), packet.getDestinationEndpoint());

            fPacket.position(40);
            byte[] payload = new byte[1];
            fPacket.get(payload);
            assertEquals(ByteBuffer.wrap(payload), packet.getPayload());

            // Packet-specific methods Testing
            assertEquals(InetAddress.getByAddress(Arrays.copyOfRange(fPacket.array(), 8, 24)), packet.getSourceIpAddress());
            assertEquals(InetAddress.getByAddress(Arrays.copyOfRange(fPacket.array(), 24, 40)), packet.getDestinationIpAddress());
            assertEquals(6, packet.getVersion());
            assertEquals(40, packet.getHeaderLength());
            assertEquals(0x12, packet.getTrafficClass());
            assertEquals(0xABCDE, packet.getFlowLabel());
            assertEquals(1, packet.getPayloadLength());
            assertEquals(0xFE, packet.getNextHeader());
            assertEquals(0x3F, packet.getHopLimit());
        }
    }
}
