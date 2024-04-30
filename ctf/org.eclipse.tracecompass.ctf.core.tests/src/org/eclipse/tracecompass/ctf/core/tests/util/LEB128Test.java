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
package org.eclipse.tracecompass.ctf.core.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.DataInput;
import java.io.IOException;
import org.eclipse.tracecompass.internal.ctf.core.utils.LEB128;
import org.junit.Test;

/**
 * Test the LEB128 class 
 * Original implementation: https://git.eclipse.org/r/c/tracecompass.incubator/org.eclipse.tracecompass.incubator/+/200680
 *
 * @author Matthew Khouzam
 * @author Vlad Arama
 */
public class LEB128Test {
    private static class ByteStream implements DataInput {
        public ByteStream(byte[] input) {
            data = input;
        }

        private final byte[] data;
        private int pos = 0;

        @Override
        public void readFully(byte[] b) throws IOException {
            fail();
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            fail();
        }

        @Override
        public int skipBytes(int n) throws IOException {
            fail();
            return 0;
        }

        @Override
        public boolean readBoolean() throws IOException {
            fail();
            return false;
        }

        @Override
        public byte readByte() throws IOException {
            try {
                byte value = data[pos++];
                return value;
            } catch (IndexOutOfBoundsException e) {
                // OK we're in a test
            }
            return 0;
        }

        @Override
        public int readUnsignedByte() throws IOException {
            fail();
            return 0;
        }

        @Override
        public short readShort() throws IOException {
            fail();
            return 0;
        }

        @Override
        public int readUnsignedShort() throws IOException {
            fail();
            return 0;
        }

        @Override
        public char readChar() throws IOException {
            fail();
            return 0;
        }

        @Override
        public int readInt() throws IOException {
            fail();
            return 0;
        }

        @Override
        public long readLong() throws IOException {
            fail();
            return 0;
        }

        @Override
        public float readFloat() throws IOException {
            fail();
            return 0;
        }

        @Override
        public double readDouble() throws IOException {
            fail();
            return 0;
        }

        @Override
        public String readLine() throws IOException {
            fail();
            return null;
        }

        @Override
        public String readUTF() throws IOException {
            fail();
            return null;
        }
    }

    /**
     * Test the value 0
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testZeroUnsigned() throws IOException {
        byte[] test = new byte[1];
        test[0] = 0;
        long expected = 0;
        assertEquals(expected, LEB128.readUnsignedLeb(new ByteStream(test)));
    }

    /**
     * Test the value 0
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testZeroSigned() throws IOException {
        byte[] test = new byte[1];
        test[0] = 0;
        long expected = 0;
        assertEquals(expected, LEB128.readSignedLeb(new ByteStream(test)));
    }

    /**
     * Test the value 1
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testOneUnsigned() throws IOException {
        byte[] test = new byte[1];
        test[0] = 1;
        long expected = 1;
        assertEquals(expected, LEB128.readUnsignedLeb(new ByteStream(test)));
    }

    /**
     * Test the value 1
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testOneSigned() throws IOException {
        byte[] test = new byte[1];
        test[0] = 1;
        long expected = 1;
        assertEquals(expected, LEB128.readSignedLeb(new ByteStream(test)));
    }

    /**
     * Test the value 624485
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testBigUnsigned() throws IOException {
        byte[] test = new byte[3];
        test[0] = (byte) 0xE5;
        test[1] = (byte) 0x8E;
        test[2] = 0x26;
        long expected = 624485;
        assertEquals(expected, LEB128.readUnsignedLeb(new ByteStream(test)));
    }

    /**
     * Test the value 123456
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testBigSignedPositive() throws IOException {
        byte[] test = new byte[3];
        test[0] = (byte) 0xC0;
        test[1] = (byte) 0xC4;
        test[2] = 0x07;
        long expected = 123456;
        assertEquals(expected, LEB128.readSignedLeb(new ByteStream(test)));
    }

    /**
     * Test the value -123456
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testBigSignedNegative() throws IOException {
        byte[] test = new byte[3];
        test[0] = (byte) 0xC0;
        test[1] = (byte) 0xBB;
        test[2] = 0x78;
        long expected = -123456;
        assertEquals(expected, LEB128.readSignedLeb(new ByteStream(test)));
    }

    /**
     * Test the value 167815081739229L
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testVeryBigUnsigned() throws IOException {
        byte[] test = new byte[7];
        test[0] = (byte) 0xDD;
        test[1] = (byte) 0x87;
        test[2] = (byte) 0xD7;
        test[3] = (byte) 0xF2;
        test[4] = (byte) 0x87;
        test[5] = (byte) 0x94;
        test[6] = 0x26;
        long expected = 167815081739229L;
        assertEquals(expected, LEB128.readUnsignedLeb(new ByteStream(test)));
    }

    /**
     * Test the value 167815081739229L
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testVeryBigSignedPositive() throws IOException {
        byte[] test = new byte[7];
        test[0] = (byte) 0xDD;
        test[1] = (byte) 0x87;
        test[2] = (byte) 0xD7;
        test[3] = (byte) 0xF2;
        test[4] = (byte) 0x87;
        test[5] = (byte) 0x94;
        test[6] = 0x26;
        long expected = 167815081739229L;
        assertEquals(expected, LEB128.readSignedLeb(new ByteStream(test)));
    }

    /**
     * Test the value -167815081739229L
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testVeryBigSignedNegative() throws IOException {
        byte[] test = new byte[7];
        test[0] = (byte) 0xA3;
        test[1] = (byte) 0xF8;
        test[2] = (byte) 0xA8;
        test[3] = (byte) 0x8D;
        test[4] = (byte) 0xF8;
        test[5] = (byte) 0xEB;
        test[6] = 0x59;
        long expected = -167815081739229L;
        assertEquals(expected, LEB128.readSignedLeb(new ByteStream(test)));
    }
}
