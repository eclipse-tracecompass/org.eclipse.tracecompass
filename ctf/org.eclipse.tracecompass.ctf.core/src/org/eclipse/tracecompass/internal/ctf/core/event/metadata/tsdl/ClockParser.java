/*******************************************************************************
 * Copyright (c) 2015, 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl;

import org.eclipse.tracecompass.ctf.core.event.CTFClock;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AntlrClockMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.IClockMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Clock metadata allows to describe the clock topology of the system, as well
 * as to detail each clock parameter. In absence of clock description, it is
 * assumed that all fields named timestamp use the same clock source, which
 * increments once per nanosecond.
 * <p>
 * Describing a clock and how it is used by streams is threefold: first, the
 * clock and clock topology should be described in a clock description block,
 * e.g.:
 *
 * <pre>
clock {
    name = cycle_counter_sync;
    uuid = "62189bee-96dc-11e0-91a8-cfa3d89f3923";
    description = "Cycle counter synchronized across CPUs";
    freq = 1000000000;           // frequency, in Hz
    // precision in seconds is: 1000 * (1/freq)
    precision = 1000;

     // clock value offset from Epoch is:
     // offset_s + (offset * (1/freq))

    offset_s = 1326476837;
    offset = 897235420;
    absolute = FALSE;
};
 * </pre>
 *
 * The mandatory name field specifies the name of the clock identifier, which
 * can later be used as a reference. The optional field uuid is the unique
 * identifier of the clock. It can be used to correlate different traces that
 * use the same clock. An optional textual description string can be added with
 * the description field. The freq field is the initial frequency of the clock,
 * in Hz. If the freq field is not present, the frequency is assumed to be
 * 1000000000 (providing clock increment of 1 ns). The optional precision field
 * details the uncertainty on the clock measurements, in (1/freq) units. The
 * offset_s and offset fields indicate the offset from POSIX.1 Epoch, 1970-01-01
 * 00:00:00 +0000 (UTC), to the zero of value of the clock. The offset_s field
 * is in seconds. The offset field is in (1/freq) units. If any of the offset_s
 * or offset field is not present, it is assigned the 0 value. The field
 * absolute is TRUE if the clock is a global reference across different clock
 * UUID (e.g. NTP time). Otherwise, absolute is FALSE, and the clock can be
 * considered as synchronized only with other clocks that have the same UUID.
 * <p>
 * Secondly, a reference to this clock should be added within an integer type:
 *
 * <pre>
typealias integer {
    size = 64; align = 1; signed = false;
    map = clock.cycle_counter_sync.value;
} := uint64_ccnt_t;
 * </pre>
 *
 * Thirdly, stream declarations can reference the clock they use as a timestamp
 * source:
 *
 * <pre>
struct packet_context {
    uint64_ccnt_t ccnt_begin;
    uint64_ccnt_t ccnt_end;
    // ...
};

stream {
    // ...
    event.header := struct {
        uint64_ccnt_t timestamp;
        // ...
    };
    packet.context := struct packet_context;
};
 * </pre>
 *
 * For a N-bit integer type referring to a clock, if the integer overflows
 * compared to the N low order bits of the clock prior value found in the same
 * stream, then it is assumed that one, and only one, overflow occurred. It is
 * therefore important that events encoding time on a small number of bits
 * happen frequently enough to detect when more than one N-bit overflow occurs.
 * <p>
 * In a packet context, clock field names ending with _begin and _end have a
 * special meaning: this refers to the timestamps at, respectively, the
 * beginning and the end of each packet.
 *
 * @author Matthew Khouzam - Initial API and implementation
 * @author Efficios (documentation)
 *
 */
public final class ClockParser implements ICommonTreeParser {

    private static final String NAME = "name"; //$NON-NLS-1$
    private static final String FREQUENCY = "freq"; //$NON-NLS-1$
    private static final String ORIGIN = "origin"; //$NON-NLS-1$
    private static final String OFFSET = "offset"; //$NON-NLS-1$
    private static final String OFFSET_S = "offset_s"; //$NON-NLS-1$
    private static final String PRECISION = "precision"; //$NON-NLS-1$
    private static final String DESCRIPTION = "description"; //$NON-NLS-1$

    /**
     * Instance
     */
    public static final ClockParser INSTANCE = new ClockParser();

    private ClockParser() {
    }

    @Override
    public CTFClock parse(ICTFMetadataNode clock, ICommonTreeParserParameter unused) throws ParseException {
        IClockMetadataNode clockNode;
        if (clock instanceof IClockMetadataNode) {
            clockNode = (IClockMetadataNode) clock;
        } else {
            // Wrap raw ANTLR node into the typed interface
            clockNode = new AntlrClockMetadataNode(clock);
        }

        CTFClock ctfClock = new CTFClock();
        ctfClock.addAttribute(NAME, clockNode.getClockName());
        ctfClock.addAttribute(FREQUENCY, clockNode.getClockFrequency());
        ctfClock.addAttribute(PRECISION, clockNode.getClockPrecision());

        String description = clockNode.getClockDescription();
        if (description != null) {
            ctfClock.addAttribute(DESCRIPTION, description);
        }

        long offsetSeconds = clockNode.getClockOffsetSeconds();
        long offsetCycles = clockNode.getClockOffsetCycles();
        if (offsetSeconds != 0 || offsetCycles != 0) {
            ctfClock.addAttribute(OFFSET, offsetCycles);
            ctfClock.addAttribute(OFFSET_S, offsetSeconds);
        }

        String origin = clockNode.getClockOrigin();
        if (origin != null) {
            ctfClock.addAttribute(ORIGIN, origin);
        }

        String uuid = clockNode.getClockUUID();
        if (uuid != null) {
            ctfClock.addAttribute("uuid", uuid); //$NON-NLS-1$
        }

        if (clockNode.isClockAbsolute()) {
            ctfClock.addAttribute("absolute", "TRUE"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return ctfClock;
    }
}
