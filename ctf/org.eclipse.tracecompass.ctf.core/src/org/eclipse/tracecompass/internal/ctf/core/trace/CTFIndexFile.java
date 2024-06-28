/*******************************************************************************
 * Copyright (c) 2016, 2024 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Matthew Khouzam - Initial implementation
 *   Arnaud Fiorini - Update to make it work with later LTTng versions
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.trace;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFIOException;

/**
 * Ctf index file reader
 *
 * @author Matthew Khouzam
 */
public class CTFIndexFile {

    private static final int CTF_INDEX_MAGIC = 0xC1F1DCC1;
    private static final int CTF_INDEX_MAJOR = 1;
    private static final int CTF_INDEX_MINOR = 1;
    private static final int PACKET_SIZE = 9 * Long.SIZE / 8;

    private final @NonNull StreamInputPacketIndex fIndex;

    /**
     * Ctf index file reader
     *
     * @param indexFile
     *            The {@Link File} input
     * @param tracePacketHeaderDecl
     *            The struct information for the trace packet header
     * @param packetContextDecl
     *            The struct information for the packet context
     * @throws CTFIOException
     *             an error such as an {@link IOException}
     */
    public CTFIndexFile(File indexFile, StructDeclaration tracePacketHeaderDecl, StructDeclaration packetContextDecl) throws CTFIOException {
        try (DataInputStream dataIn = new DataInputStream(new FileInputStream(indexFile))) {
            CtfPacketIndexFileHeader header;

            header = new CtfPacketIndexFileHeader(dataIn);
            fIndex = new StreamInputPacketIndex();
            long packetHeaderBits = (long) tracePacketHeaderDecl.getMaximumSize() + packetContextDecl.getMaximumSize();
            while (dataIn.available() >= header.packetIndexLen) {
                StreamInputPacketIndexEntry element;
                if (header.oldMagic) {
                    element = new StreamInputPacketIndexEntry(dataIn, packetHeaderBits, indexFile.getName().replace("\\.idx", "")); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    element = new StreamInputPacketIndexEntry(dataIn, packetHeaderBits);
                }
                fIndex.append(element);
            }
        } catch (IOException e) {
            throw new CTFIOException(e);
        }
    }

    /**
     * Gets the packet entries of this file
     *
     * @return the packet entries of this file
     */
    public @NonNull StreamInputPacketIndex getStreamInputPacketIndex() {
        return fIndex;
    }

    /**
     * Header at the beginning of each index file. All integer fields are stored
     * in big endian.
     */
    class CtfPacketIndexFileHeader {
        private final int magic;
        private final int indexMajor;
        private final int indexMinor;
        /* CtfPacketIndexEntry, in bytes */
        private final int packetIndexLen;
        private final boolean oldMagic;

        public CtfPacketIndexFileHeader(DataInputStream dataIn) throws IOException, CTFIOException {
            magic = dataIn.readInt();
            if (magic != CTF_INDEX_MAGIC) {
                if (magic == 0x43544649) {
                    oldMagic = (dataIn.readShort() == 0x4458);
                } else {
                    oldMagic = false;
                }
                if (!oldMagic) {
                    throw new CTFIOException("Magic mismatch in index"); //$NON-NLS-1$
                }
            } else {
                oldMagic = false;
            }
            indexMajor = dataIn.readInt();
            if (indexMajor != CTF_INDEX_MAJOR) {
                throw new CTFIOException("Major version mismatch in index"); //$NON-NLS-1$
            }
            indexMinor = dataIn.readInt();
            if (indexMinor != CTF_INDEX_MINOR) {
                throw new CTFIOException("Minor version mismatch in index"); //$NON-NLS-1$
            }
            if (!oldMagic) {
                packetIndexLen = dataIn.readInt();
            } else {
                packetIndexLen = PACKET_SIZE;
            }
            if (packetIndexLen != PACKET_SIZE) {
                throw new CTFIOException("Packet size wrong in index"); //$NON-NLS-1$
            }
        }
    }
}
