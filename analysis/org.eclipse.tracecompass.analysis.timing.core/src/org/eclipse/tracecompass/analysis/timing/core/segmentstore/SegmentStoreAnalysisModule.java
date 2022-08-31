/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Kyrollos Bekhet - Initial API and implementation
 ******************************************************************************/

package org.eclipse.tracecompass.analysis.timing.core.segmentstore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

/**
 * @author Kyrollos Bekhet
 * @since 5.3
 */
public class SegmentStoreAnalysisModule extends AbstractSegmentStoreAnalysisModule {

    private static class SegmentAspectWrapper implements ISegmentAspect {

        private ISegmentAspect fAspect;

        public SegmentAspectWrapper(ISegmentAspect aspect) {
            fAspect = aspect;
        }

        @Override
        public String getName() {
            return fAspect.getName();
        }

        @Override
        public String getHelpText() {
            return fAspect.getHelpText();
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return fAspect.getComparator();
        }

        @Override
        public @Nullable Object resolve(ISegment segment) {
            if (segment instanceof TraceNameSegment) {
                return fAspect.resolve(((TraceNameSegment) segment).getSegment());
            }
            return fAspect.resolve(segment);
        }
    }

    private static class SegmentAspectTraceName implements ISegmentAspect {

        public static final ISegmentAspect INSTANCE = new SegmentAspectTraceName();

        @Override
        public @Nullable Object resolve(ISegment segment) {
            if (segment instanceof TraceNameSegment) {
                return ((TraceNameSegment) segment).getTraceName();
            }
            return null;
        }

        @Override
        public String getName() {
            return "Trace"; //$NON-NLS-1$
        }

        @Override
        public String getHelpText() {
            return ""; //$NON-NLS-1$
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return Comparator.comparing(TraceNameSegment::getTraceName);
        }
    }

    private static class TraceNameSegment implements ISegment {
        private static final long serialVersionUID = 5122084433695846745L;

        private String fTraceName;
        private ISegment fSegment;

        /**
         * @param traceName
         *            The trace name
         * @param segment
         *            The segment to encapsulate inside this class
         */
        public TraceNameSegment(String traceName, ISegment segment) {
            fSegment = segment;
            fTraceName = traceName;
        }

        public String getTraceName() {
            return fTraceName;
        }

        @Override
        public long getStart() {
            return fSegment.getStart();
        }

        @Override
        public long getEnd() {
            return fSegment.getEnd();
        }

        /**
         * @return the segment encapsulated in this wrapper class
         */
        public ISegment getSegment() {
            return fSegment;
        }
    }

    private static final Logger LOGGER = TraceCompassLog.getLogger(SegmentStoreAnalysisModule.class);
    private static final Map<ISegmentAspect, ISegmentAspect> fAspects = new HashMap<>();

    private Map<String, ISegmentStoreProvider> fProviders;
    private @Nullable String fName;

    /**
     * Constructor only used for test purposes
     *
     * @param trace
     *            The trace to analyse
     * @throws TmfAnalysisException
     *             Exception thrown if analysis fails
     */
    @VisibleForTesting
    public SegmentStoreAnalysisModule(ITmfTrace trace) throws TmfAnalysisException {
        fProviders = new HashMap<>();
        setTrace(trace);
        if (trace instanceof TmfExperiment) {
            for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                IAnalysisModule m = tr.getAnalysisModules().iterator().next();
                addAnalysis(m, String.valueOf(tr.getName()));
            }
        } else {
            IAnalysisModule m = trace.getAnalysisModules().iterator().next();
            addAnalysis(m, String.valueOf(trace.getName()));
        }
    }

    /**
     * @param trace
     *            The trace to analyse
     * @param id
     *            The ID of the analysis
     */
    public SegmentStoreAnalysisModule(ITmfTrace trace, String id) {
        fProviders = new HashMap<>();
        if (trace instanceof TmfExperiment) {
            for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                IAnalysisModule m = tr.getAnalysisModule(id);
                if (m != null) {
                    addAnalysis(m, String.valueOf(tr.getName()));
                }
            }
        } else {
            IAnalysisModule m = trace.getAnalysisModule(id);
            if (m != null) {
                addAnalysis(m, String.valueOf(trace.getName()));
            }
        }
    }

    private void addAnalysis(IAnalysisModule module, String traceName) {
        if (module instanceof ISegmentStoreProvider) {
            fName = module.getName();
            module.schedule();
            fProviders.putIfAbsent(traceName, (ISegmentStoreProvider) module);
        }
    }

    @Override
    public String getName() {
        return NonNullUtils.nullToEmptyString(fName);
    }

    @Override
    protected boolean buildAnalysisSegments(ISegmentStore<ISegment> segmentStore, IProgressMonitor monitor) throws TmfAnalysisException {
        try (ScopeLog scope = new ScopeLog(LOGGER, Level.FINER, "SegmentStoreAnalysisModule#buildAnalysisSegment")) { //$NON-NLS-1$
            if (fProviders.size() == 0) {
                return false;
            }
            if (fProviders.size() == 1) {
                segmentStore.addAll(Objects.requireNonNull(fProviders.get(fProviders.keySet().iterator().next())).getSegmentStore());
            } else {
                for (Entry<String, ISegmentStoreProvider> providerEntry : fProviders.entrySet()) {
                    ISegmentStore<ISegment> segments = Objects.requireNonNull(providerEntry.getValue()).getSegmentStore();
                    if (segments != null) {
                        for (ISegment segment : segments) {
                            segmentStore.add(new TraceNameSegment(providerEntry.getKey(), segment));
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void dispose() {
        for (Entry<String, ISegmentStoreProvider> providerEntry : fProviders.entrySet()) {
            ISegmentStoreProvider provider = Objects.requireNonNull(providerEntry.getValue());
            if (provider instanceof IAnalysisModule) {
                ((IAnalysisModule) provider).dispose();
            }
        }
        super.dispose();
    }

    @Override
    protected void canceling() {
        // Do nothing
    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        if (fProviders.size() > 1) {
            return getExperimentSegmentAspects();
        }
        if (fProviders.size() == 1) {
            ISegmentStoreProvider provider = fProviders.values().iterator().next();
            return provider.getSegmentAspects();
        }
        return Collections.emptyList();
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        for (Entry<String, ISegmentStoreProvider> providerEntry : fProviders.entrySet()) {
            ISegmentStoreProvider provider = Objects.requireNonNull(providerEntry.getValue());
            if (provider instanceof IAnalysisModule) {
                ((IAnalysisModule) provider).waitForCompletion(monitor);
            }
        }
        return super.executeAnalysis(monitor);
    }

    private Iterable<ISegmentAspect> getExperimentSegmentAspects() {
        ImmutableSet.Builder<ISegmentAspect> builder = new ImmutableSet.Builder<>();
        ISegmentStoreProvider provider = fProviders.get(fProviders.keySet().iterator().next());
        if (provider != null) {
            builder.addAll(getWrapperAspects(provider.getSegmentAspects()));
        }
        builder.add(SegmentAspectTraceName.INSTANCE);
        return builder.build();
    }

    private static List<ISegmentAspect> getWrapperAspects(Iterable<ISegmentAspect> aspects) {
        List<ISegmentAspect> wrappedAspects = new ArrayList<>();
        for (ISegmentAspect aspect : aspects) {
            synchronized (fAspects) {
                ISegmentAspect wrapperAspect = fAspects.computeIfAbsent(aspect, a -> new SegmentAspectWrapper(aspect));
                wrappedAspects.add(wrapperAspect);
            }
        }
        return wrappedAspects;
    }
}
