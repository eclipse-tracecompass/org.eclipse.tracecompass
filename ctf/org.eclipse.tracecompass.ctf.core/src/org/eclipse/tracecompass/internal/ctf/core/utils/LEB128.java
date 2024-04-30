/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.utils;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * LEB128 decoding utility functions. 
 * Original implementation: https://git.eclipse.org/r/c/tracecompass.incubator/org.eclipse.tracecompass.incubator/+/200680
 *
 * @author Matthew Khouzam
 * @author Vlad Arama
 *
 */
public class LEB128 {
    /**
     * Reads an unsigned LEB128 encoded integer from a ByteBuffer. The decoding
     * stops if the shift exceeds 64 bits to prevent overflow, as a long can
     * only support up to 64 bits.
     *
     * @param in
     *            ByteBuffer containing the LEB128 encoded integer.
     * @return The decoded integer as a long.
     */
    public static long readUnsignedLeb(ByteBuffer in) {
        long result = 0;
        long shift = 0;
        byte current = 0;
        do {
            current = in.get();
            result |= ((long) (current & 0x7f)) << shift;
            shift += 7;
        } while ((current & 0x80) != 0 && shift < 64);
        return result;
    }

    /**
     * Reads an unsigned LEB128 encoded integer from a DataInput. The decoding
     * stops if the shift exceeds 64 bits to prevent overflow, as a long can
     * only support up to 64 bits.
     *
     * @param in
     *            DataInput containing the LEB128 encoded integer.
     * @return The decoded integer as a long.
     * @throws IOException
     *             if the file ends
     */
    public static long readUnsignedLeb(DataInput in) throws IOException {
        long result = 0;
        long shift = 0;
        byte current = 0;
        do {
            current = in.readByte();
            result |= ((long) (current & 0x7f)) << shift;
            shift += 7;
        } while ((current & 0x80) != 0 && shift < 64);

        return result;
    }

    /**
     * Reads a signed LEB128 encoded integer from a ByteBuffer. The decoding
     * stops if the shift exceeds 64 bits to prevent overflow, as a long can
     * only support up to 64 bits.
     *
     * @param in
     *            ByteBuffer containing the LEB128 encoded integer.
     * @return The decoded signed integer as a long.
     */
    public static long readSignedLeb(ByteBuffer in) {
        long result = 0;
        int shift = 0;
        byte current;
        do {
            current = in.get();
            result |= ((long) (current & 0x7f)) << shift;
            shift += 7;
        } while ((current & 0x80) != 0 && shift < 64);

        if ((current & 0x40) != 0 && shift < 64) {
            result |= -1L << shift; // Sign extend
        }

        return result;
    }

    /**
     * Reads a signed LEB128 encoded integer from a DataInput. The decoding
     * stops if the shift exceeds 64 bits to prevent overflow, as a long can
     * only support up to 64 bits.
     *
     * @param in
     *            DataInput containing the LEB128 encoded integer.
     * @return The decoded signed integer as a long.
     * @throws IOException
     *             if the file ends
     */
    public static long readSignedLeb(DataInput in) throws IOException {
        long result = 0;
        int shift = 0;
        byte current;
        do {
            current = in.readByte();
            result |= ((long) (current & 0x7f)) << shift;
            shift += 7;
        } while ((current & 0x80) != 0 && shift < 64);

        if ((current & 0x40) != 0 && shift < 64) {
            result |= -1L << shift; // Sign extend
        }

        return result;
    }
}
