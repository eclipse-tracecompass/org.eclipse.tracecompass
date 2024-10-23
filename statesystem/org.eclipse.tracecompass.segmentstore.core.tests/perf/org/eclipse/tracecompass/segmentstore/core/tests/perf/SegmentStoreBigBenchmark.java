/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.segmentstore.core.tests.perf;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.segmentstore.core.BasicSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.tests.historytree.HistoryTreeSegmentStoreStub;
import org.junit.runners.Parameterized.Parameters;

/**
 * Benchmark the segment store with datasets that are too big to fit in memory
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public class SegmentStoreBigBenchmark extends SegmentStoreBenchmark {

    /**
     * Constructor
     *
     * @param name
     *            name of the benchmark
     * @param segStore
     *            The segment store to use
     */
    public SegmentStoreBigBenchmark(String name, ISegmentStore<BasicSegment> segStore) {
        super(name, segStore);
    }

    /**
     * @return The arrays of parameters
     * @throws IOException
     *             Exceptions thrown when setting the on-disk backends
     */
    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> getParameters() throws IOException {
        return Arrays.asList(new Object[][] {
                { "HT store", new HistoryTreeSegmentStoreStub<>(Files.createTempFile("tmpSegStore", null), 1, BasicSegment.BASIC_SEGMENT_READ_FACTORY) },
        });
    }

    @Override
    protected long getSegmentStoreSize() {
        return 100000000;
    }
}
