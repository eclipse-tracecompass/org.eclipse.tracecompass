/**********************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.analysis.timing.core.tests.stubs.segmentstore;

import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule;
import org.eclipse.tracecompass.segmentstore.core.BasicSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Test stub for segment store table
 *
 * @author Kyrollos Bekhet
 */
public class StubSegmentStoreProvider extends AbstractSegmentStoreAnalysisModule {

    /** Stub column name */
    public static final String STUB_COLUMN_NAME = "Stub Column";

    /** Stub column content */
    public static final String STUB_COLUMN_CONTENT = "Stub Content";

    /** Search direction values to reuse */
    public static final String NEXT_DIR_UNDER_TEST = "NEXT";
    public static final String PREV_DIR_UNDER_TEST = "PREVIOUS";

    private static final int SIZE = 65535;

    private final List<@NonNull ISegment> fPreFixture;

    private static final @NonNull ISegmentAspect STUB_CUSTOM_ASPECT = new ISegmentAspect() {
        @Override
        public @Nullable Object resolve(@NonNull ISegment segment) {
            return STUB_COLUMN_CONTENT;
        }

        @Override
        public @NonNull String getName() {
            return STUB_COLUMN_NAME;
        }

        @Override
        public @NonNull String getHelpText() {
            return "Stub segment column information";
        }

        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }
    };

    /**
     * The constructor
     */
    public StubSegmentStoreProvider() {
        ImmutableList.Builder<@NonNull ISegment> builder = new Builder<>();
        int previousStartTime = 0;
        for (int i = 0; i < SIZE; i++) {
            if (i % 7 == 0) {
                previousStartTime = i;
            }
            ISegment segment = new BasicSegment(previousStartTime, i);
            builder.add(segment);
        }
        fPreFixture = builder.build();
    }

    /**
     * Constructor to initialize segments
     *
     * @param nullSegments
     *            : to decide on null or not null segments
     */
    public StubSegmentStoreProvider(boolean nullSegments) {
        ImmutableList.Builder<@NonNull ISegment> builder = new Builder<>();
        if (nullSegments) {
            fPreFixture = builder.build();
        } else {
            int previousStartTime = 0;
            for (int i = 0; i < SIZE; i++) {
                if (i % 7 == 0) {
                    previousStartTime = i;
                }
                ISegment segment = new BasicSegment(previousStartTime, i);
                builder.add(segment);
            }
            fPreFixture = builder.build();
        }
    }

    @Override
    protected boolean buildAnalysisSegments(@NonNull ISegmentStore<@NonNull ISegment> segmentStore, @NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        boolean retVal = segmentStore.addAll(fPreFixture);
        segmentStore.close(false);
        return retVal;
    }

    @Override
    protected void canceling() {
        // Empty
    }

    @Override
    public boolean setTrace(@NonNull ITmfTrace trace) throws TmfAnalysisException {
        if (trace instanceof TmfXmlTraceStub) {
            TmfXmlTraceStub tmfXmlTraceStub = (TmfXmlTraceStub) trace;
            try {
                tmfXmlTraceStub.addAnalysisModule(this);
            } catch (TmfTraceException e) {
                throw new TmfAnalysisException(e.getMessage());
            }
        }
        return super.setTrace(trace);
    }

    @Override
    public @NonNull Iterable<@NonNull ISegmentAspect> getSegmentAspects() {
        return ImmutableList.of(STUB_CUSTOM_ASPECT);
    }

    @Override
    public boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        return super.executeAnalysis(monitor);
    }
}
