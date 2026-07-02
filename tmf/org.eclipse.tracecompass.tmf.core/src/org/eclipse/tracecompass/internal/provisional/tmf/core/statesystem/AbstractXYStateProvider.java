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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.statesystem.mipmap.AbstractTmfMipmapStateProvider;
import org.eclipse.tracecompass.internal.tmf.core.statesystem.mipmap.AvgMipmapFeature;
import org.eclipse.tracecompass.internal.tmf.core.statesystem.mipmap.ITmfMipmapFeature;
import org.eclipse.tracecompass.internal.tmf.core.statesystem.mipmap.MinMipmapFeature;
import org.eclipse.tracecompass.internal.tmf.core.statesystem.mipmap.TimeMipmapFeature;
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

    /**
     * Feature bit for the maximum mipmap feature.
     */
    public static final int MAX = AbstractTmfMipmapStateProvider.MAX;

    /**
     * Feature bit for the minimum mipmap feature.
     */
    public static final int MIN = AbstractTmfMipmapStateProvider.MIN;

    /**
     * Feature bit for the average mipmap feature.
     */
    public static final int AVG = AbstractTmfMipmapStateProvider.AVG;

    /**
     * Default mipmap feature bits: MAX only. Sufficient for spike-preserving
     * downsampling in XY views.
     */
    private static final int DEFAULT_MIPMAP_FEATURES = MAX;

    /**
     * The mipmap resolution. Using 10 gives power-of-10 time-based levels when
     * combined with the {@code TimeMipmapFeature}. With the count-based
     * {@code TmfMipmapFeature}, this means every 10 intervals at level N are
     * aggregated into one interval at level N+1.
     */
    private static final int MIPMAP_RESOLUTION = 10;

    private final int fMipmapFeatures;
    private final Map<Integer, Set<ITmfMipmapFeature>> fFeatureMap = new HashMap<>();

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
        fMipmapFeatures = DEFAULT_MIPMAP_FEATURES;
    }

    /**
     * Constructor with configurable mipmap features.
     *
     * @param trace
     *            The trace
     * @param id
     *            The state provider ID
     * @param mipmapFeatures
     *            The mipmap feature bits (e.g., {@code MAX | MIN})
     */
    protected AbstractXYStateProvider(@NonNull ITmfTrace trace, @NonNull String id, int mipmapFeatures) {
        super(trace, id);
        fMipmapFeatures = mipmapFeatures;
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
        updateMipmapFeatures(ts, stateValue, quark);
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
        updateMipmapFeatures(ts, stateValue, quark);
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
        ITmfStateValue stateValue = TmfStateValue.newValueLong(newValue);
        ss.modifyAttribute(ts, stateValue.unboxValue(), quark);
        updateMipmapFeatures(ts, stateValue, quark);
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
        ITmfStateValue stateValue = TmfStateValue.newValueDouble(newValue);
        ss.modifyAttribute(ts, stateValue.unboxValue(), quark);
        updateMipmapFeatures(ts, stateValue, quark);
    }

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    @Override
    public void dispose() {
        waitForEmptyQueue();
        for (Set<ITmfMipmapFeature> features : fFeatureMap.values()) {
            for (ITmfMipmapFeature feature : features) {
                feature.updateAndCloseMipmap();
            }
        }
        super.dispose();
    }

    // ------------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------------

    private void updateMipmapFeatures(long ts, ITmfStateValue value, int quark) {
        Set<ITmfMipmapFeature> features = fFeatureMap.get(quark);
        if (features == null) {
            features = createFeatures(quark, ts, value);
            if (features.isEmpty()) {
                return;
            }
            fFeatureMap.put(quark, features);
        }
        for (ITmfMipmapFeature feature : features) {
            feature.updateMipmap(value, ts);
        }
    }

    private Set<ITmfMipmapFeature> createFeatures(int quark, long ts, ITmfStateValue value) {
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        Set<ITmfMipmapFeature> features = new LinkedHashSet<>();

        if (value.isNull()) {
            return features;
        }

        try {
            if ((fMipmapFeatures & MAX) != 0) {
                int featureQuark = ss.getQuarkRelativeAndAdd(quark, AbstractTmfMipmapStateProvider.MAX_STRING);
                ss.modifyAttribute(ts, 0, featureQuark);
                features.add(new TimeMipmapFeature(quark, featureQuark, ss));
            }
            if ((fMipmapFeatures & MIN) != 0) {
                int featureQuark = ss.getQuarkRelativeAndAdd(quark, AbstractTmfMipmapStateProvider.MIN_STRING);
                ss.modifyAttribute(ts, 0, featureQuark);
                features.add(new MinMipmapFeature(quark, featureQuark, MIPMAP_RESOLUTION, ss));
            }
            if ((fMipmapFeatures & AVG) != 0) {
                int featureQuark = ss.getQuarkRelativeAndAdd(quark, AbstractTmfMipmapStateProvider.AVG_STRING);
                ss.modifyAttribute(ts, 0, featureQuark);
                features.add(new AvgMipmapFeature(quark, featureQuark, MIPMAP_RESOLUTION, ss));
            }
        } catch (TimeRangeException | StateValueTypeException e) {
            Activator.logError("Failed to create mipmap features for quark " + quark, e); //$NON-NLS-1$
        }

        return features;
    }
}
