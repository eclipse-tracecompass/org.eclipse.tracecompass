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

package org.eclipse.tracecompass.internal.tmf.core.statesystem.mipmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;

import com.google.common.collect.BiMap;

/**
 * Internal utility for auto-detecting mipmap sub-attributes on state system
 * quarks and replacing trivially point-sampled Y values with max-per-bucket
 * aggregated values.
 * <p>
 * This class is called by the common data provider base classes
 * ({@code AbstractTreeCommonXDataProvider} and
 * {@code AbstractTreeGenericXYCommonXDataProvider}) to transparently enhance
 * query results when mipmap attributes are present.
 *
 * @author Trace Compass contributors
 */
public final class MipmapXYQueryHelper {

    private MipmapXYQueryHelper() {
        // Static utility class
    }

    /**
     * Enhance a collection of Y models with mipmap max-per-bucket values where
     * available. For each model, if its corresponding quark in the state system
     * has a "max" sub-attribute (indicating mipmap data is present), the Y
     * values are replaced with the maximum value in each bucket defined by
     * adjacent timestamps in {@code bucketBoundaries}.
     * <p>
     * Models whose quarks lack mipmap attributes are returned unchanged.
     *
     * @param ss
     *            The state system to query
     * @param idToQuark
     *            Mapping from entry IDs to state system quarks
     * @param models
     *            The Y models to enhance
     * @param bucketBoundaries
     *            Sorted array of timestamps defining bucket edges. Bucket i
     *            spans [bucketBoundaries[i], bucketBoundaries[i+1])
     * @return Enhanced collection of Y models (same size as input)
     */
    public static Collection<IYModel> enhanceWithMipmap(
            ITmfStateSystem ss,
            BiMap<Long, Integer> idToQuark,
            Collection<IYModel> models,
            long[] bucketBoundaries) {

        if (bucketBoundaries.length < 2) {
            return models;
        }

        List<IYModel> enhanced = new ArrayList<>(models.size());
        for (IYModel model : models) {
            IYModel result = tryEnhanceModel(ss, idToQuark, model, bucketBoundaries);
            enhanced.add(result != null ? result : model);
        }
        return enhanced;
    }

    /**
     * Try to enhance a single Y model with mipmap max values.
     *
     * @return Enhanced model, or null if mipmap is not available for this model
     */
    private static @Nullable IYModel tryEnhanceModel(
            ITmfStateSystem ss,
            BiMap<Long, Integer> idToQuark,
            IYModel model,
            long[] bucketBoundaries) {

        Integer quark = idToQuark.get(model.getId());
        if (quark == null) {
            return null;
        }

        // Check if mipmap "max" sub-attribute exists
        int maxQuark = ss.optQuarkRelative(quark, AbstractTmfMipmapStateProvider.MAX_STRING);
        if (maxQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return null;
        }

        // Mipmap is available — query max per bucket
        double[] maxValues = queryMaxPerBucket(ss, quark, bucketBoundaries);
        if (maxValues == null) {
            return null;
        }

        return new YModel(model.getId(), model.getName(), maxValues, model.getYAxisDescription());
    }

    /**
     * Query the maximum value per bucket using mipmap-accelerated range
     * queries.
     *
     * @param ss
     *            The state system
     * @param quark
     *            The base attribute quark (must have "max" sub-attribute)
     * @param bucketBoundaries
     *            Sorted timestamps defining bucket edges
     * @return Array of max values (one per bucket), or null on error
     */
    private static double @Nullable [] queryMaxPerBucket(
            ITmfStateSystem ss, int quark, long[] bucketBoundaries) {

        int numBuckets = bucketBoundaries.length - 1;
        double[] result = new double[numBuckets];

        long ssStart = ss.getStartTime();
        long ssEnd = ss.getCurrentEndTime();

        for (int i = 0; i < numBuckets; i++) {
            long t1 = Math.max(bucketBoundaries[i], ssStart);
            long t2 = Math.min(bucketBoundaries[i + 1], ssEnd);

            if (t1 > t2 || t1 > ssEnd || t2 < ssStart) {
                result[i] = 0.0;
                continue;
            }

            try {
                ITmfStateValue maxVal = TmfStateSystemOperations.queryRangeMax(ss, t1, t2, quark);
                if (maxVal.isNull()) {
                    result[i] = 0.0;
                } else if (maxVal.getType() == ITmfStateValue.Type.DOUBLE) {
                    result[i] = maxVal.unboxDouble();
                } else {
                    result[i] = maxVal.unboxLong();
                }
            } catch (AttributeNotFoundException | TimeRangeException | StateValueTypeException e) {
                result[i] = 0.0;
            }
        }

        return result;
    }
}
