/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.dataprovider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.OSCriticalPathModule;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdgeContextState;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.OSCriticalPathPalette;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState.OSEdgeContextEnum;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableMap;

/**
 * @author Arnaud Fiorini
 *
 */
public class OSCriticalPathDataProvider extends CriticalPathDataProvider {

    private static final @NonNull String ARROW_SUFFIX = "arrow"; //$NON-NLS-1$
    private static final @NonNull Map<@NonNull String, @NonNull OutputElementStyle> STATE_MAP;
    private static final @NonNull Map<@NonNull String, @NonNull OutputElementStyle> STYLE_MAP = Collections.synchronizedMap(new HashMap<>());

    static {
        ImmutableMap.Builder<@NonNull String, @NonNull OutputElementStyle> builder = new ImmutableMap.Builder<>();
        builder.putAll(OSCriticalPathPalette.getStyles());

        // Add the arrow types
        builder.put(OSEdgeContextEnum.DEFAULT.name() + ARROW_SUFFIX, new OutputElementStyle(OSEdgeContextEnum.UNKNOWN.name(), ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.CriticalPathDataProvider_UnknownArrow), StyleProperties.STYLE_GROUP, String.valueOf(Messages.CriticalPathDataProvider_GroupArrows))));
        builder.put(OSEdgeContextEnum.NETWORK.name() + ARROW_SUFFIX, new OutputElementStyle(OSEdgeContextEnum.NETWORK.name(), ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(Messages.CriticalPathDataProvider_NetworkArrow), StyleProperties.STYLE_GROUP, String.valueOf(Messages.CriticalPathDataProvider_GroupArrows))));
        STATE_MAP = builder.build();
    }


    /**
     * @param trace the trace from which the critical path is extracted from
     * @param criticalPathProvider critical path module
     */
    public OSCriticalPathDataProvider(@NonNull ITmfTrace trace, @NonNull OSCriticalPathModule criticalPathProvider) {
        super(trace, criticalPathProvider);
    }

    @Override
    protected @NonNull OutputElementStyle getMatchingState(ITmfEdgeContextState contextState, boolean arrow) {
        String parentStyleName = contextState.getContextEnum().name();
        parentStyleName = STATE_MAP.containsKey(parentStyleName) ? parentStyleName : OSEdgeContextEnum.UNKNOWN.name();
        parentStyleName = arrow ? parentStyleName + ARROW_SUFFIX : parentStyleName;
        return STYLE_MAP.computeIfAbsent(contextState.getContextEnum().name(), style -> new OutputElementStyle(style));
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull OutputStyleModel> fetchStyle(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STATE_MAP), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

}
