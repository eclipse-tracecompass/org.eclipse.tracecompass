/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.tests.callgraph.sampled;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.CallGraph;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.sampled.callgraph.ProfilingCallGraphAnalysisModule;
import org.eclipse.tracecompass.analysis.profiling.core.tests.CallStackTestBase2;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.internal.analysis.profiling.core.tree.AllGroupDescriptor;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.junit.Test;

/**
 * Test profiling data where the input are stack traces from events
 *
 * @author Geneviève Bastien
 */
public class SampledCallGraphTest {

    private static final long @NonNull [] CALLSITE_1 = { 1, 2, 3, 4 };
    private static final long @NonNull [] CALLSITE_2 = { 1, 2, 3 };
    private static final long @NonNull [] CALLSITE_3 = { 1, 2, 3, 4 };
    private static final long @NonNull [] CALLSITE_4 = { 1, 3, 4 };
    private static final long @NonNull [] CALLSITE_5 = { 1, 2, 5 };
    private static final long @NonNull [] CALLSITE_6 = { 1, 2, 5, 4 };
    private static final long @NonNull [] CALLSITE_7 = { 10, 11, 12 };
    private static final long @NonNull [] CALLSITE_8 = { 10, 11 };
    private static final long @NonNull [] CALLSITE_9 = { 1, 2, 3, 4 };
    private static final long @NonNull [] CALLSITE_10 = { 1, 2, 4, 5 };

    /**
     * A default implementation of the profiling call graph analysis for test
     * purposes
     */
    private static class TestProfilingAnalysis extends ProfilingCallGraphAnalysisModule {

        private final @NonNull ICallStackElement fOneElement;

        public TestProfilingAnalysis() {
            ICallStackElement element = new CallStackElement("test", AllGroupDescriptor.getInstance());
            addRootElement(element);
            fOneElement = element;
        }

        public @NonNull ICallStackElement getElement() {
            return fOneElement;
        }

        @Override
        public Collection<IWeightedTreeGroupDescriptor> getGroupDescriptors() {
            return Collections.singleton(AllGroupDescriptor.getInstance());
        }

        @Override
        public Map<String, Collection<Object>> getCallStack(@NonNull ITmfEvent event) {
            return Collections.emptyMap();
        }

        @Override
        protected @Nullable Pair<@NonNull ICallStackElement, @NonNull AggregatedCallSite> getProfiledStackTrace(@NonNull ITmfEvent event) {
            return null;
        }

    }

    /**
     * Test a full sampling for one group
     */
    @Test
    public void testStackTraces() {
        TestProfilingAnalysis pg = new TestProfilingAnalysis();
        try {
            ICallStackElement element = pg.getElement();

            CallGraph cg = pg.getCallGraph();
            cg.addAggregatedCallSite(element, pg.getCallSite(element, CALLSITE_1, 1));
            cg.addAggregatedCallSite(element, pg.getCallSite(element, CALLSITE_2, 2));
            cg.addAggregatedCallSite(element, pg.getCallSite(element, CALLSITE_3, 3));
            cg.addAggregatedCallSite(element, pg.getCallSite(element, CALLSITE_4, 4));
            cg.addAggregatedCallSite(element, pg.getCallSite(element, CALLSITE_5, 5));
            cg.addAggregatedCallSite(element, pg.getCallSite(element, CALLSITE_6, 6));
            cg.addAggregatedCallSite(element, pg.getCallSite(element, CALLSITE_7, 7));
            cg.addAggregatedCallSite(element, pg.getCallSite(element, CALLSITE_8, 8));
            cg.addAggregatedCallSite(element, pg.getCallSite(element, CALLSITE_9, 9));
            cg.addAggregatedCallSite(element, pg.getCallSite(element, CALLSITE_10, 10));

            Collection<AggregatedCallSite> aggregatedData = cg.getCallingContextTree(element);

            assertNotNull(aggregatedData);
            assertEquals(2, aggregatedData.size());

            for (AggregatedCallSite callsite : aggregatedData) {
                switch (CallStackTestBase2.getCallSiteSymbol(callsite).resolve(Collections.emptySet())) {
                case "0x1": {
                    assertEquals(8, callsite.getWeight());
                    assertEquals(2, callsite.getCallees().size());
                    for (AggregatedCallSite childCallsite : callsite.getCallees()) {
                        switch (CallStackTestBase2.getCallSiteSymbol(childCallsite).resolve(Collections.emptySet())) {
                        case "0x2":
                            assertEquals(7, childCallsite.getWeight());
                            assertEquals(3, childCallsite.getCallees().size());
                            break;
                        case "0x3":
                            assertEquals(1, childCallsite.getWeight());
                            assertEquals(1, childCallsite.getCallees().size());
                            break;
                        default:
                            throw new IllegalStateException("Unknown callsite: " + CallStackTestBase2.getCallSiteSymbol(childCallsite));
                        }
                    }
                }
                    break;
                case "0xa": {
                    assertEquals(2, callsite.getWeight());
                    assertEquals(1, callsite.getCallees().size());
                    AggregatedCallSite childCallsite = callsite.getCallees().iterator().next();
                    assertEquals(2, childCallsite.getWeight());
                    assertEquals(1, callsite.getCallees().size());
                }
                    break;
                default:
                    throw new IllegalStateException("Unknown callsite: " + CallStackTestBase2.getCallSiteSymbol(callsite));
                }
            }
        } finally {
            pg.dispose();
        }

    }

}
