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

package org.eclipse.tracecompass.internal.pcap.core.protocol.ipv6;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;
import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.pcap.core.packet.BadPacketException;
import org.eclipse.tracecompass.internal.pcap.core.packet.Packet;
import org.eclipse.tracecompass.internal.pcap.core.protocol.PcapProtocol;
import org.eclipse.tracecompass.internal.pcap.core.protocol.tcp.TCPPacket;
import org.eclipse.tracecompass.internal.pcap.core.protocol.udp.UDPPacket;
import org.eclipse.tracecompass.internal.pcap.core.protocol.unknown.UnknownPacket;
import org.eclipse.tracecompass.internal.pcap.core.trace.PcapFile;
import org.eclipse.tracecompass.internal.pcap.core.util.ConversionHelper;
import org.eclipse.tracecompass.internal.pcap.core.util.IPProtocolNumberHelper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Class that represents an IPv6 packet.
 */
public class IPv6Packet extends Packet {

    private final @Nullable Packet fChildPacket;
    private final @Nullable ByteBuffer fPayload;

    private final int fVersion;
    private final int fTrafficClass;
    private final int fFlowLabel;
    private final int fPayloadLength;
    private final int fNextHeader;
    private final int fHopLimit;
    private final Inet6Address fSourceIpAddress;
    private final Inet6Address fDestinationIpAddress;

    private @Nullable IPv6Endpoint fSourceEndpoint;
    private @Nullable IPv6Endpoint fDestinationEndpoint;

    private @Nullable Map<String, String> fFields;

    // TODO Interpret options. See
    // http://www.iana.org/assignments/ip-parameters/ip-parameters.xhtml

    /**
     * Constructor of the IPv4 Packet class.
     *
     * @param file
     *            The file that contains this packet.
     * @param parent
     *            The parent packet of this packet (the encapsulating packet).
     * @param packet
     *            The entire packet (header and payload).
     * @throws BadPacketException
     *             Thrown when the packet is erroneous.
     */
    public IPv6Packet(PcapFile file, @Nullable Packet parent, ByteBuffer packet) throws BadPacketException {
        super(file, parent, PcapProtocol.IPV6);

        // The endpoints are lazy loaded. They are defined in the get*Endpoint()
        // methods.
        fSourceEndpoint = null;
        fDestinationEndpoint = null;

        fFields = null;

        packet.order(ByteOrder.BIG_ENDIAN);
        packet.position(0);

        short storage0 = packet.getShort();
        fVersion = ((storage0 & 0xF000) >> 12) & 0x0F;
        fTrafficClass = ((storage0 & 0x0FF0) >> 4) & 0xFF;
        fFlowLabel = (((storage0 & 0x000F) << 16) & 0xF0000) + ConversionHelper.unsignedShortToInt(packet.getShort());

        fPayloadLength = ConversionHelper.unsignedShortToInt(packet.getShort());
        fNextHeader = ConversionHelper.unsignedByteToInt(packet.get());
        fHopLimit = ConversionHelper.unsignedByteToInt(packet.get());

        byte[] source = new byte[IPv6Values.IP_ADDRESS_SIZE];
        byte[] destination = new byte[IPv6Values.IP_ADDRESS_SIZE];
        packet.get(source);
        packet.get(destination);

        try {
            fSourceIpAddress = (Inet6Address) checkNotNull(InetAddress.getByAddress(source));
            fDestinationIpAddress = (Inet6Address) checkNotNull(InetAddress.getByAddress(destination));
        } catch (UnknownHostException e) {
            throw new BadPacketException("The IP Address size is not valid!"); //$NON-NLS-1$
        }

        // Get payload if any.
        if (packet.remaining() > 0) {
            ByteBuffer payload = packet.slice();
            payload.order(ByteOrder.BIG_ENDIAN);
            fPayload = payload;
        } else {
            fPayload = null;
        }

        // Find child
        fChildPacket = findChildPacket();

    }

    @Override
    public @Nullable Packet getChildPacket() {
        return fChildPacket;
    }

    @Override
    public @Nullable ByteBuffer getPayload() {
        return fPayload;
    }

    /**
     * {@inheritDoc}
     *
     * See http://en.wikipedia.org/wiki/List_of_IP_protocol_numbers
     */
    @Override
    protected @Nullable Packet findChildPacket() throws BadPacketException {
        // TODO Implement more protocols
        ByteBuffer payload = fPayload;
        if (payload == null) {
            return null;
        }

        switch (fNextHeader) {
        case IPProtocolNumberHelper.PROTOCOL_NUMBER_TCP:
            return new TCPPacket(getPcapFile(), this, payload);
        case IPProtocolNumberHelper.PROTOCOL_NUMBER_UDP:
            return new UDPPacket(getPcapFile(), this, payload);
        default:
            return new UnknownPacket(getPcapFile(), this, payload);
        }

    }

    @Override
    public String toString() {
        String string = getProtocol().getName() + ", Source: " + fSourceIpAddress.getHostAddress() + ", Destination: " + fDestinationIpAddress.getHostAddress() + //$NON-NLS-1$ //$NON-NLS-2$
                "\nVersion: " + fVersion + ", Header Length: " + getHeaderLength() + " bytes, Traffic Class: " + String.format("%s%02x", "0x", fTrafficClass)   //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$ //$NON-NLS-5$
                + ", Flow Label: " + String.format("%s%02x", "0x", fFlowLabel) + ", Payload Length: " + getPayloadLength() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                " bytes\nNext Header: " + String.format("%s%02x", "0x", fNextHeader) + ", Hop Limit: " + fHopLimit + "\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        final Packet child = fChildPacket;
        if (child != null) {
            return string + child.toString();
        }
        return string;
    }

    /**
     * Getter method that returns the version of the IP protocol used. This
     * should always be set to 6.
     *
     * @return The version of the IP used.
     */
    public int getVersion() {
        return fVersion;
    }

    /**
     * Getter method that returns the header length in bytes.
     *
     * @return The header length in bytes.
     */
    public int getHeaderLength() {
        return IPv6Values.DEFAULT_HEADER_LENGTH;
    }

    /**
     * Getter method that returns the Traffic Class.
     *
     * @return The Traffic Class
     */
    public int getTrafficClass() {
        return fTrafficClass;
    }

    /**
     * Getter method that returns the Flow Label.
     *
     * @return The Flow Label
     */
    public int getFlowLabel() {
        return fFlowLabel;
    }

    /**
     * Getter method that returns the Payload Length.
     *
     * @return The Payload Length
     */
    public int getPayloadLength() {
        return fPayloadLength;
    }

    /**
     * Getter method that returns the Next Header.
     *
     * @return The Next Header
     */
    public int getNextHeader() {
        return fNextHeader;
    }

    /**
     * Getter method that returns the Hop Limit.
     *
     * @return The Hop Limit
     */
    public int getHopLimit() {
        return fHopLimit;
    }

    /**
     * Getter method that returns the source IP address.
     *
     * @return The source IP address, as a byte array in big-endian.
     */
    public Inet6Address getSourceIpAddress() {
        return fSourceIpAddress;
    }

    /**
     * Getter method that returns the destination IP address.
     *
     * @return The destination IP address, as a byte array in big-endian.
     */
    public Inet6Address getDestinationIpAddress() {
        return fDestinationIpAddress;
    }

    @Override
    public boolean validate() {
        // Not yet implemented. ATM, we consider that all packets are valid.
        // This is the case for all packets.
        // TODO Implement it.
        return true;
    }

    @Override
    public IPv6Endpoint getSourceEndpoint() {
        @Nullable
        IPv6Endpoint endpoint = fSourceEndpoint;
        if (endpoint == null) {
            endpoint = new IPv6Endpoint(this, true);
        }
        fSourceEndpoint = endpoint;
        return fSourceEndpoint;
    }

    @Override
    public IPv6Endpoint getDestinationEndpoint() {
        @Nullable
        IPv6Endpoint endpoint = fDestinationEndpoint;

        if (endpoint == null) {
            endpoint = new IPv6Endpoint(this, false);
        }
        fDestinationEndpoint = endpoint;
        return fDestinationEndpoint;
    }

    @Override
    public Map<String, String> getFields() {
        Map<String, String> map = fFields;
        if (map == null) {
            Builder<@NonNull String, @NonNull String> builder = ImmutableMap.<@NonNull String, @NonNull String> builder()
                    .put("Version", String.valueOf(fVersion)) //$NON-NLS-1$
                    .put("Traffic Class", String.format("%s%02x", "0x", fTrafficClass)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    .put("Flow Label", String.format("%s%05x", "0x", fFlowLabel)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    .put("Payload Length", String.valueOf(getPayloadLength()) + " bytes") //$NON-NLS-1$ //$NON-NLS-2$
                    .put("Next Header", String.format("%s%02x", "0x", fNextHeader)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    .put("Hop Limit", String.valueOf(fHopLimit)) //$NON-NLS-1$
                    .put("Source IP Address", nullToEmptyString(fSourceIpAddress.getHostAddress())) //$NON-NLS-1$
                    .put("Destination IP Address", nullToEmptyString(fDestinationIpAddress.getHostAddress())); //$NON-NLS-1$
            fFields = builder.build();
            return fFields;
        }
        return map;
    }

    @Override
    public String getLocalSummaryString() {
        return "Src: " + fSourceIpAddress.getHostAddress() + " , Dst: " + fDestinationIpAddress.getHostAddress(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected String getSignificationString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fSourceIpAddress.getHostAddress())
                .append(" > ") //$NON-NLS-1$
                .append(fDestinationIpAddress.getHostAddress());

        final ByteBuffer payload = fPayload;
        if (payload != null) {
            sb.append(" Len=") //$NON-NLS-1$
            .append(payload.limit());
        } else {
            sb.append(" Len=0"); //$NON-NLS-1$
        }
        return NonNullUtils.nullToEmptyString(sb);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final Packet child = fChildPacket;
        if (child != null) {
            result = prime * result + child.hashCode();
        } else {
            result = prime * result;
        }
        result = prime * result + fVersion;
        result = prime * result + fTrafficClass;
        result = prime * result + fFlowLabel;
        result = prime * result + fPayloadLength;
        result = prime * result + fNextHeader;
        result = prime * result + fHopLimit;
        result = prime * result + fSourceIpAddress.hashCode();
        result = prime * result + fDestinationIpAddress.hashCode();
        if (child == null) {
            result = prime * result + payloadHashCode(fPayload);
        }
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        IPv6Packet other = (IPv6Packet) obj;
        if (!Objects.equals(fChildPacket, other.fChildPacket)) {
            return false;
        }
        if (fVersion != other.fVersion) {
            return false;
        }
        if (fTrafficClass != other.fTrafficClass) {
            return false;
        }
        if (fFlowLabel != other.fFlowLabel) {
            return false;
        }
        if (fPayloadLength != other.fPayloadLength) {
            return false;
        }
        if (fNextHeader != other.fNextHeader) {
            return false;
        }
        if (fHopLimit != other.fHopLimit) {
            return false;
        }
        if (!(fSourceIpAddress.equals(other.fSourceIpAddress))) {
            return false;
        }
        if (!(fDestinationIpAddress.equals(other.fDestinationIpAddress))) {
            return false;
        }
        return (fChildPacket != null || payloadEquals(fPayload, other.fPayload));
    }
}
