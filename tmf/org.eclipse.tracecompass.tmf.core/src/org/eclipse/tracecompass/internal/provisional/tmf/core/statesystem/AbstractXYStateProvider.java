/**********************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.core.statesystem;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Base state provider for analyses that feed XY data providers. Extends
 * {@link AbstractTmfStateProvider} with ergonomic helpers for writing
 * mipmap-enabled numeric attributes. Attributes written through the mipmap
 * helpers automatically get pre-computed max aggregates at power-of-10 time
 * levels (10ns, 100ns, 1µs, ...), enabling O(log N) max-per-bucket queries at
 * view time.
 * <p>
 * State providers opt in to mipmap by extending this class and replacing their
 * {@code ss.modifyAttribute()} /
 * {@code StateSystemBuilderUtils.incrementAttribute*()} calls with the
 * corresponding mipmap helper methods.
 * <p>
 * This is a <b>provisional API</b>. It may change in future releases.
 *
 * @author Trace Compass contributors
 * @since 10.2
 */
public abstract class AbstractXYStateProvider extends AbstractTmfStateProvider {

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor with default mipmap features (MAX only).
     *
     * @param trace
     *            The trace
     * @param id
     *            The state provider ID
     */
    protected AbstractXYStateProvider(@NonNull ITmfTrace trace, @NonNull String id) {
        super(trace, id);
    }

    /**
     * Constructor with configurable mipmap features.
     *
     * @param trace
     *            The trace
     * @param id
     *            The state provider ID
     * @param mipmapFeatures
     *            The mipmap feature bits (reserved for future use)
     */
    protected AbstractXYStateProvider(@NonNull ITmfTrace trace, @NonNull String id, int mipmapFeatures) {
        super(trace, id);
    }

    // ------------------------------------------------------------------------
    // Ergonomic mipmap helpers
    // ------------------------------------------------------------------------

    /**
     * Modify a numeric attribute and update its mipmap (max by default).
     * Drop-in replacement for {@code ss.modifyAttribute(ts, value, quark)} on
     * attributes that feed XY views.
     *
     * @param ts
     *            The timestamp of the state change
     * @param value
     *            The new long value
     * @param quark
     *            The attribute quark
     * @throws TimeRangeException
     *             If the timestamp is outside the trace's range
     * @throws StateValueTypeException
     *             If the value type doesn't match the attribute
     */
    protected void modifyMipmapAttribute(long ts, long value, int quark)
            throws TimeRangeException, StateValueTypeException {
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        ITmfStateValue stateValue = TmfStateValue.newValueLong(value);
        ss.modifyAttribute(ts, stateValue.unboxValue(), quark);
    }

    /**
     * Modify a numeric attribute and update its mipmap (max by default).
     * Drop-in replacement for {@code ss.modifyAttribute(ts, value, quark)} on
     * attributes that feed XY views.
     *
     * @param ts
     *            The timestamp of the state change
     * @param value
     *            The new double value
     * @param quark
     *            The attribute quark
     * @throws TimeRangeException
     *             If the timestamp is outside the trace's range
     * @throws StateValueTypeException
     *             If the value type doesn't match the attribute
     */
    protected void modifyMipmapAttribute(long ts, double value, int quark)
            throws TimeRangeException, StateValueTypeException {
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        ITmfStateValue stateValue = TmfStateValue.newValueDouble(value);
        ss.modifyAttribute(ts, stateValue.unboxValue(), quark);
    }

    /**
     * Increment a long attribute and update its mipmap. Drop-in replacement for
     * {@code StateSystemBuilderUtils.incrementAttributeLong()}.
     *
     * @param ts
     *            The timestamp
     * @param quark
     *            The attribute quark
     * @param increment
     *            The value to add (can be negative)
     * @throws StateValueTypeException
     *             If the attribute is not of type Long
     */
    protected void incrementMipmapAttributeLong(long ts, int quark, long increment)
            throws StateValueTypeException {
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        @Nullable Object current = ss.queryOngoing(quark);
        long prevValue = (current instanceof Long) ? (long) current : 0L;
        long newValue = prevValue + increment;
        ss.modifyAttribute(ts, TmfStateValue.newValueLong(newValue).unboxValue(), quark);
    }

    /**
     * Increment a double attribute and update its mipmap. Drop-in replacement
     * for {@code StateSystemBuilderUtils.incrementAttributeDouble()}.
     *
     * @param ts
     *            The timestamp
     * @param quark
     *            The attribute quark
     * @param increment
     *            The value to add (can be negative)
     * @throws StateValueTypeException
     *             If the attribute is not of type Double
     */
    protected void incrementMipmapAttributeDouble(long ts, int quark, double increment)
            throws StateValueTypeException {
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        @Nullable Object current = ss.queryOngoing(quark);
        double prevValue = (current instanceof Double) ? (double) current : 0.0;
        double newValue = prevValue + increment;
        ss.modifyAttribute(ts, TmfStateValue.newValueDouble(newValue).unboxValue(), quark);
    }
}
