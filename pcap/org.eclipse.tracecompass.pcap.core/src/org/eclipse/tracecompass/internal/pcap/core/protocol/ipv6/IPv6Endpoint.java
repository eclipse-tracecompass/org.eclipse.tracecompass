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

import java.net.Inet6Address;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.pcap.core.endpoint.ProtocolEndpoint;

/**
 * Class that extends the {@link ProtocolEndpoint} class. It represents the
 * endpoint at an IPv6 level.
 */
public class IPv6Endpoint extends ProtocolEndpoint {

    private final Inet6Address fIPAddress;

    /**
     * Constructor of the {@link IPv6Endpoint} class. It takes a packet to get
     * its endpoint. Since every packet has two endpoints (source and
     * destination), the isSourceEndpoint parameter is used to specify which
     * endpoint to take.
     *
     * @param packet
     *            The packet that contains the endpoints.
     * @param isSourceEndpoint
     *            Whether to take the source or the destination endpoint of the
     *            packet.
     */
    public IPv6Endpoint(IPv6Packet packet, boolean isSourceEndpoint) {
        super(packet, isSourceEndpoint);
        fIPAddress = isSourceEndpoint ? packet.getSourceIpAddress() : packet.getDestinationIpAddress();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        ProtocolEndpoint endpoint = getParentEndpoint();
        if (endpoint == null) {
            result = 0;
        } else {
            result = endpoint.hashCode();
        }

        result = prime * result + fIPAddress.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IPv6Endpoint)) {
            return false;
        }

        IPv6Endpoint other = (IPv6Endpoint) obj;

        // Check on layer
        boolean localEquals = fIPAddress.equals(other.fIPAddress);
        if (!localEquals) {
            return false;
        }

        // Check above layers.
        ProtocolEndpoint endpoint = getParentEndpoint();
        if (endpoint != null) {
            return endpoint.equals(other.getParentEndpoint());
        }
        return true;
    }

    @Override
    public String toString() {
        ProtocolEndpoint endpoint = getParentEndpoint();
        if (endpoint == null) {
            return checkNotNull(fIPAddress.getHostAddress());
        }
        return endpoint.toString() + '/' + fIPAddress.getHostAddress();
    }

}
