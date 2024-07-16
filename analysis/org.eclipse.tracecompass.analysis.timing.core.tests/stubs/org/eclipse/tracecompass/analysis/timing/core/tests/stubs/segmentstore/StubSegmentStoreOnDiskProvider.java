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

import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.segmentstore.core.BasicSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentStoreFactory.SegmentStoreType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;

/**
 * Test stub for segment store on disk
 *
 * @author Bernd Hufmann
 */
public class StubSegmentStoreOnDiskProvider extends StubSegmentStoreProvider {

    /**
     * The reader for this segment class
     */
    public static final @NonNull IHTIntervalReader<@NonNull ISegment> READER = buffer -> new BasicSegment(buffer.getLong(), buffer.getLong());

    /**
     * The constructor
     */
    public StubSegmentStoreOnDiskProvider() {
        super();
    }

    /**
     * Constructor to initialize segments
     *
     * @param nullSegments
     *            : to decide on null or not null segments
     */
    public StubSegmentStoreOnDiskProvider(boolean nullSegments) {
        super(nullSegments);
    }

    @Override
    protected boolean buildAnalysisSegments(@NonNull ISegmentStore<@NonNull ISegment> segmentStore, @NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        return super.buildAnalysisSegments(segmentStore, monitor);
    }

    @Override
    protected @NonNull SegmentStoreType getSegmentStoreType() {
        return SegmentStoreType.OnDisk;
    }

    @Override
    protected IHTIntervalReader<@NonNull ISegment> getSegmentReader() {
        return READER;
    }

    /**
     * Get on-disk date file path
     *
     * @return on-disk data file path
     */
    @Override
    public Path getDataFilePath() {
        return super.getDataFilePath();
    }
}
