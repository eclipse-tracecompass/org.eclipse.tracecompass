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

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.ctf.core.event.CTFClock;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.Activator;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonClockMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

import com.google.gson.JsonObject;

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
    private static final String SECONDS = "seconds"; //$NON-NLS-1$
    private static final String CYCLES = "cycles"; //$NON-NLS-1$
    private static final String OFFSET = "offset"; //$NON-NLS-1$
    private static final String OFFSET_S = "offset_s"; //$NON-NLS-1$
    private static final String PRECISION = "precision"; //$NON-NLS-1$
    private static final String DESCRIPTION = "description"; //$NON-NLS-1$
    private static final String UNIX_EPOCH = "unix-epoch"; //$NON-NLS-1$

    /**
     * Instance
     */
    public static final ClockParser INSTANCE = new ClockParser();

    private ClockParser() {
    }

    @Override
    public CTFClock parse(ICTFMetadataNode clock, ICommonTreeParserParameter unused) throws ParseException {
        CTFClock ctfClock = new CTFClock();
        if (clock instanceof JsonClockMetadataNode) {
            JsonClockMetadataNode jsonClock = (JsonClockMetadataNode) clock;

            ctfClock.addAttribute(FREQUENCY, jsonClock.getFrequency());
            ctfClock.addAttribute(NAME, jsonClock.getName());
            ctfClock.addAttribute(PRECISION, jsonClock.getPrecision());
            if (jsonClock.getDescription() != null) {
                ctfClock.addAttribute(DESCRIPTION, jsonClock.getPrecision());
            }
            JsonObject offset = jsonClock.getOffset();
            if (offset.has(SECONDS) && offset.has(CYCLES)) {
                Long seconds = offset.get(SECONDS).getAsLong();
                Long cycles = offset.get(CYCLES).getAsLong();
                ctfClock.addAttribute(OFFSET, cycles);
                ctfClock.addAttribute(OFFSET_S, seconds);
            }
            if (jsonClock.getOrigin() != null) {
                if (jsonClock.getOrigin().isJsonObject()) {
                    ctfClock.addAttribute(ORIGIN, jsonClock.getOrigin().getAsJsonObject().get(NAME).getAsString());
                } else if (jsonClock.getOrigin().getAsString().equals(UNIX_EPOCH)) {
                    ctfClock.addAttribute(ORIGIN, UNIX_EPOCH);
                }
            }
        } else {
            List<ICTFMetadataNode> children = clock.getChildren();
            for (ICTFMetadataNode child : children) {
                String key = child.getChild(0).getChild(0).getChild(0).getText();
                ICTFMetadataNode value = child.getChild(1).getChild(0).getChild(0);
                String type = value.getType();
                String text = value.getText();

                if (CTFParser.tokenNames[CTFParser.INTEGER].equals(type) || CTFParser.tokenNames[CTFParser.DECIMAL_LITERAL].equals(type)) {
                    /*
                     * Not a pretty hack, this is to make sure that there is no
                     * number overflow due to 63 bit integers. The offset should
                     * only really be an issue in the year 2262. The tracer in
                     * C/ASM can write an offset in an unsigned 64 bit long. In
                     * java, the last bit, being set to 1, will be read as a
                     * negative number, but since it is too large as a positive
                     * number it will throw an exception. This will happen in
                     * 2^63 ns from 1970. Therefore 293 years from 1970.
                     */
                    Long numValue;
                    try {
                        numValue = Long.parseLong(text);
                    } catch (NumberFormatException e) {
                        Activator.log(IStatus.WARNING, "Number conversion issue with " + text + ". Assigning " + key + " = 0."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        numValue = Long.valueOf(0L);
                    }
                    ctfClock.addAttribute(key, numValue);
                } else {
                    ctfClock.addAttribute(key, text);
                }
            }
        }
        return ctfClock;
    }
}
