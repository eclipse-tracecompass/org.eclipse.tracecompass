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
     * adjacent timestamps in {@code requestedTimes}.
     * <p>
     * Models whose quarks lack mipmap attributes are returned unchanged.
     *
     * @param ss
     *            The state system to query
     * @param idToQuark
     *            Mapping from entry IDs to state system quarks
     * @param models
     *            The Y models to enhance
     * @param requestedTimes
     *            Sorted array of the requested sample timestamps. The enhanced
     *            model keeps exactly one value per requested timestamp: value
     *            {@code i} is the maximum over the bucket
     *            {@code [requestedTimes[i], requestedTimes[i+1])}, and the last
     *            value corresponds to the single sample at
     *            {@code requestedTimes[length-1]}.
     * @return Enhanced collection of Y models (same size as input, and each
     *         model keeps the same number of Y values as the input model)
     */
    public static Collection<IYModel> enhanceWithMipmap(
            ITmfStateSystem ss,
            BiMap<Long, Integer> idToQuark,
            Collection<IYModel> models,
            long[] requestedTimes) {

        if (requestedTimes.length < 2) {
            return models;
        }

        List<IYModel> enhanced = new ArrayList<>(models.size());
        for (IYModel model : models) {
            IYModel result = tryEnhanceModel(ss, idToQuark, model, requestedTimes);
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
            long[] requestedTimes) {

        Integer quark = idToQuark.get(model.getId());
        if (quark == null || quark < 0) {
            return null;
        }

        // Check if mipmap "max" sub-attribute exists
        int maxQuark = ss.optQuarkRelative(quark, AbstractTmfMipmapStateProvider.MAX_STRING);
        if (maxQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return null;
        }

        // The enhanced series must have exactly the same number of points as
        // the original point-sampled series (one value per requested
        // timestamp). Anything else would misalign the Y values against the X
        // axis and produce broken or invisible lines.
        double[] original = model.getData();
        if (original.length != requestedTimes.length) {
            return null;
        }

        double[] maxValues = queryMaxPerBucket(ss, quark, requestedTimes, original);
        return new YModel(model.getId(), model.getName(), maxValues, model.getYAxisDescription());
    }

    /**
     * Query the maximum value per bucket using mipmap-accelerated range
     * queries. Produces one value per requested timestamp: value {@code i} is
     * the maximum over {@code [requestedTimes[i], requestedTimes[i+1])} (the
     * last value is the single sample at the final timestamp). When the bucket
     * falls outside the state system's range or has no data, the original
     * point-sampled value is preserved so the result is never worse than
     * trivial sampling.
     *
     * @param ss
     *            The state system
     * @param quark
     *            The base attribute quark (must have "max" sub-attribute)
     * @param requestedTimes
     *            Sorted requested sample timestamps
     * @param original
     *            The original point-sampled values (same length as
     *            {@code requestedTimes}), used as a fallback
     * @return Array of values, one per requested timestamp
     */
    private static double[] queryMaxPerBucket(
            ITmfStateSystem ss, int quark, long[] requestedTimes, double[] original) {

        int nbPoints = requestedTimes.length;
        double[] result = new double[nbPoints];

        long ssStart = ss.getStartTime();
        long ssEnd = ss.getCurrentEndTime();

        for (int i = 0; i < nbPoints; i++) {
            // Bucket i spans [requestedTimes[i], requestedTimes[i+1]); the last
            // point is a single-sample bucket at requestedTimes[i].
            long bucketStart = requestedTimes[i];
            long bucketEnd = (i < nbPoints - 1) ? requestedTimes[i + 1] - 1 : requestedTimes[i];
            if (bucketEnd < bucketStart) {
                bucketEnd = bucketStart;
            }

            long t1 = Math.max(bucketStart, ssStart);
            long t2 = Math.min(bucketEnd, ssEnd);

            if (t1 > t2) {
                // Bucket is entirely outside the state system's range: keep the
                // original point-sampled value.
                result[i] = original[i];
                continue;
            }

            try {
                ITmfStateValue maxVal = TmfStateSystemOperations.queryRangeMax(ss, t1, t2, quark);
                if (maxVal.isNull()) {
                    result[i] = original[i];
                } else if (maxVal.getType() == ITmfStateValue.Type.DOUBLE) {
                    result[i] = maxVal.unboxDouble();
                } else {
                    result[i] = maxVal.unboxLong();
                }
            } catch (AttributeNotFoundException | TimeRangeException | StateValueTypeException e) {
                result[i] = original[i];
            }
        }

        return result;
    }
}
