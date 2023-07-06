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
     * Type string for a field class that points to an alias
     */
    public static final String ALIAS = "alias"; //$NON-NLS-1$

    /**
     * Type string for an unsigned integer field class
     */
    public static final String FIXED_UNSIGNED_INTEGER_FIELD = "fixed-length-unsigned-integer"; //$NON-NLS-1$

    /**
     * Type string for a static length blob field class
     */
    public static final String STATIC_LENGTH_BLOB = "static-length-blob"; //$NON-NLS-1$
}
