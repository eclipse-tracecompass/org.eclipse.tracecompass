/*******************************************************************************
 * Copyright (c) 2011, 2023 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Sehr Moosabhoy - Implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.utils;

/**
 * Various strings for CTF2 implementation.
 *
 * @author Sehr Moosabhoy
 */
public final class JsonMetadataStrings {

    private JsonMetadataStrings() {
        // Do nothing
    }

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * Type string for a CTF2 preamble fragment
     */
    public static final String FRAGMENT_PREAMBLE = "preamble"; //$NON-NLS-1$

    /**
     * Type string for a CTF2 trace class fragment
     */
    public static final String FRAGMENT_TRACE = "trace-class"; //$NON-NLS-1$

    /**
     * Type string for a CTF2 field class alias fragment
     */
    public static final String FRAGMENT_FIELD_ALIAS = "field-class-alias"; //$NON-NLS-1$

    /**
     * Type string for a CTF2 clock class fragment
     */
    public static final String FRAGMENT_CLOCK = "clock-class"; //$NON-NLS-1$

    /**
     * Type string for a CTF2 data stream class fragment
     */
    public static final String FRAGMENT_DATA_STREAM = "data-stream-class"; //$NON-NLS-1$

    /**
     * Type string for a CTF2 event record class fragment
     */
    public static final String FRAGMENT_EVENT_RECORD = "event-record-class"; //$NON-NLS-1$

    /**
     * Field string for the type of a CTF2 node
     */
    public static final String TYPE = "type"; //$NON-NLS-1$

    /**
     * String for the roles field of a field class
     */
    public static final String ROLES = "roles"; //$NON-NLS-1$

    /**
     * String for the packet header magic role of a field class
     */
    public static final String MAGIC_NUMBER = "packet-magic-number"; //$NON-NLS-1$

    /**
     * String for the packet header uuid role of a field class
     */
    public static final String UUID = "metadata-stream-uuid"; //$NON-NLS-1$

    /**
     * String for the packet header data stream class id role of a field class
     */
    public static final String DATA_STREAM_ID = "data-stream-class-id"; //$NON-NLS-1$

    /**
     * String for the packet content length role of a data stream packet context
     */
    public static final String PACKET_CONTENT_LENGTH = "packet-content-length"; //$NON-NLS-1$

    /**
     * String for the packet total length role of a data stream packet context
     */
    public static final String PACKET_TOTAL_LENGTH = "packet-total-length"; //$NON-NLS-1$

    /**
     * String for the default clock timestamp role of a data stream packet
     * context or data stream event record header
     */
    public static final String DEFAULT_CLOCK_TIMESTAMP = "default-clock-timestamp"; //$NON-NLS-1$

    /**
     * String for the discarded event record counter snapshot role of a data
     * stream packet context
     */
    public static final String CURRENT_DISCARDED_EVENT_COUNT = "discarded-event-record-counter-snapshot"; //$NON-NLS-1$

    /**
     * String for the packet end default clock timestamp role of a data stream
     * packet context
     */
    public static final String PACKET_END_TIMESTAMP = "packet-end-default-clock-timestamp"; //$NON-NLS-1$

    /**
     * String for the event record class id role of a data stream event record
     * header
     */
    public static final String EVENT_RECORD_CLASS_ID = "event-record-class-id"; //$NON-NLS-1$

    /**
     * Type string for a field class that points to an alias
     */
    public static final String ALIAS = "alias"; //$NON-NLS-1$

    /**
     * Type string for an unsigned fixed integer field class
     */
    public static final String FIXED_UNSIGNED_INTEGER_FIELD = "fixed-length-unsigned-integer"; //$NON-NLS-1$

    /**
     * Type string for a signed fixed integer field class
     */
    public static final String FIXED_SIGNED_INTEGER_FIELD = "fixed-length-signed-integer"; //$NON-NLS-1$

    /**
     * Type string for an unsigned variable integer field class
     */
    public static final String VARIABLE_UNSIGNED_INTEGER_FIELD = "variable-length-unsigned-integer"; //$NON-NLS-1$

    /**
     * Type string for a signed variable integer field class
     */
    public static final String VARIABLE_SIGNED_INTEGER_FIELD = "variable-length-signed-integer"; //$NON-NLS-1$

    /**
     * Type string for a static length blob field class
     */
    public static final String STATIC_LENGTH_BLOB = "static-length-blob"; //$NON-NLS-1$

    /**
     * Type string for a null terminated string field class
     */
    public static final String NULL_TERMINATED_STRING = "null-terminated-string"; //$NON-NLS-1$

    /**
     * Type string for a fixed length unsigned enumeration field class
     */
    public static final String FIXED_UNSIGNED_ENUMERATION = "fixed-length-unsigned-enumeration"; //$NON-NLS-1$

    /**
     * Type string for a variant field class
     */
    public static final String VARIANT = "variant"; //$NON-NLS-1$

    /**
     * Type string for a structure field class
     */
    public static final String STRUCTURE = "structure"; //$NON-NLS-1$
}
