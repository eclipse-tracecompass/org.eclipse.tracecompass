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

/**
 * Interface that lists constants related to Internet Protocol v6.
 *
 * See http://en.wikipedia.org/wiki/IPv6_packet.
 */
public interface IPv6Values {

    /** Version */
    int VERSION = 6;

    /** Size in bytes of an IP address */
    int IP_ADDRESS_SIZE = 16;

    /** Size in bytes of a default IPv6 packet header */
    int DEFAULT_HEADER_LENGTH = 40;

}
