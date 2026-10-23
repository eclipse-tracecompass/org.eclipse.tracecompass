/**********************************************************************
 * Copyright (c) 2026 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.ctf.core.event.types;

import java.nio.ByteOrder;

/**
 * The bit order of a CTF2 fixed-length bit array field, as described in
 * https://diamon.org/ctf/#fl-ba-fc
 *
 * @since 5.3
 * @author Arnaud Fiorini
 */
public enum BitOrder {
    /**
     * The first bit of the field is the least significant bit
     */
    FIRST_TO_LAST,
    /**
     * The first bit of the field is the most significant bit
     */
    LAST_TO_FIRST;

    /**
     * Gets the default bit order for a given byte order: first-to-last for
     * little-endian and last-to-first for big-endian.
     *
     * @param byteOrder
     *            the byte order of the field
     * @return the default bit order
     */
    public static BitOrder fromByteOrder(ByteOrder byteOrder) {
        return ByteOrder.BIG_ENDIAN.equals(byteOrder) ? LAST_TO_FIRST : FIRST_TO_LAST;
    }
}
