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

package org.eclipse.tracecompass.analysis.callstack.core.tests.callgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.callstack.core.tests.CallStackTestBase;
import org.eclipse.tracecompass.analysis.callstack.core.tests.stubs.CallStackAnalysisStub;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.AggregatedCalledFunction;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.AllGroupDescriptor;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.ITree;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.WeightedTree;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.WeightedTreeGroupBy;
import org.eclipse.tracecompass.internal.analysis.callstack.core.tree.WeightedTreeSet;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

/**
 * Test the {@link WeightedTreeGroupBy} class with a callgraph
 *
 * @author Geneviève Bastien
 */
public class CallGraphGroupByInstrumentedTest extends CallStackTestBase {

    private static class CallGraphExpected {
        private final long duration;
        private final long selfTime;
        private final Map<String, CallGraphExpected> children;

        public CallGraphExpected(long dur, long self, Map<String, CallGraphExpected> childMap) {
            duration = dur;
            selfTime = self;
            children = childMap;
        }
    }

    private static Map<String, CallGraphExpected> getExpectedAll() {
        return ImmutableMap.of(
                "op1", new CallGraphExpected(28, 8, ImmutableMap.of(
                        "op2", new CallGraphExpected(7, 5, ImmutableMap.of(
                                "op3", new CallGraphExpected(2, 2, Collections.emptyMap()))),
                        "op3", new CallGraphExpected(5, 3, ImmutableMap.of(
                                "op1", new CallGraphExpected(2, 2, Collections.emptyMap()))),
                        "op4", new CallGraphExpected(8, 8, Collections.emptyMap()))),
                "op4", new CallGraphExpected(8, 8, Collections.emptyMap()),
                "op2", new CallGraphExpected(17, 10, ImmutableMap.of(
                        "op3", new CallGraphExpected(1, 1, Collections.emptyMap()),
                        "op2", new CallGraphExpected(6, 6, Collections.emptyMap()))),
                "op5", new CallGraphExpected(19, 7, ImmutableMap.of(
                        "op2", new CallGraphExpected(12, 11, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))))));
    }

    private static Map<String, CallGraphExpected> getExpectedProcess1() {
        return ImmutableMap.of(
                "op1", new CallGraphExpected(9, 5, ImmutableMap.of(
                        "op2", new CallGraphExpected(4, 3, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))))),
                "op4", new CallGraphExpected(8, 8, Collections.emptyMap()),
                "op2", new CallGraphExpected(17, 10, ImmutableMap.of(
                        "op3", new CallGraphExpected(1, 1, Collections.emptyMap()),
                        "op2", new CallGraphExpected(6, 6, Collections.emptyMap()))));
    }

    private static Map<String, CallGraphExpected> getExpectedProcess5() {
        return ImmutableMap.of(
                "op1", new CallGraphExpected(19, 3, ImmutableMap.of(
                        "op3", new CallGraphExpected(5, 3, ImmutableMap.of(
                                "op1", new CallGraphExpected(2, 2, Collections.emptyMap()))),
                        "op2", new CallGraphExpected(3, 2, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))),
                        "op4", new CallGraphExpected(8, 8, Collections.emptyMap()))),
                "op5", new CallGraphExpected(19, 7, ImmutableMap.of(
                        "op2", new CallGraphExpected(12, 11, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))))));
    }

    private static Map<String, CallGraphExpected> getExpectedThread2() {
        return ImmutableMap.of(
                "op1", new CallGraphExpected(9, 5, ImmutableMap.of(
                        "op2", new CallGraphExpected(4, 3, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))))),
                "op4", new CallGraphExpected(8, 8, Collections.emptyMap()));
    }

    private static Map<String, CallGraphExpected> getExpectedThread3() {
        return ImmutableMap.of(
                "op2", new CallGraphExpected(17, 10, ImmutableMap.of(
                        "op3", new CallGraphExpected(1, 1, Collections.emptyMap()),
                        "op2", new CallGraphExpected(6, 6, Collections.emptyMap()))));
    }

    private static Map<String, CallGraphExpected> getExpectedThread6() {
        return ImmutableMap.of(
                "op1", new CallGraphExpected(19, 3, ImmutableMap.of(
                        "op2", new CallGraphExpected(3, 2, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))),
                        "op3", new CallGraphExpected(5, 3, ImmutableMap.of(
                                "op1", new CallGraphExpected(2, 2, Collections.emptyMap()))),
                        "op4", new CallGraphExpected(8, 8, Collections.emptyMap()))));
    }

    private static Map<String, CallGraphExpected> getExpectedThread7() {
        return ImmutableMap.of(
                "op5", new CallGraphExpected(19, 7, ImmutableMap.of(
                        "op2", new CallGraphExpected(12, 11, ImmutableMap.of(
                                "op3", new CallGraphExpected(1, 1, Collections.emptyMap()))))));
    }

    /**
     * Test the group by all level for a call graph
     */
    @Test
    public void testGroupByAllInstrumented() {
        CallStackAnalysisStub cga = getModule();
        CallGraph baseCallGraph = cga.getCallGraph();

        WeightedTreeSet<@NonNull ICallStackSymbol, @NonNull Object> callGraph = WeightedTreeGroupBy.groupWeightedTreeBy(AllGroupDescriptor.getInstance(), baseCallGraph, cga);
        @SuppressWarnings("null")
        Collection<Object> elements = callGraph.getElements();
        assertEquals(1, elements.size());

        Object element = Iterables.getFirst(elements, null);
        assertNotNull(element);

        Collection<@NonNull WeightedTree<@NonNull ICallStackSymbol>> callingContextTree = callGraph.getTreesFor(element);
        compareCcts("", getExpectedAll(), callingContextTree);
    }

    /**
     * Test the group by intermediate level for a call graph
     */
    @SuppressWarnings("null")
    @Test
    public void testGroupByProcessInstrumented() {
        CallStackAnalysisStub cga = getModule();
        CallGraph baseCallGraph = cga.getCallGraph();

        // The first group descriptor is the process
        Collection<IWeightedTreeGroupDescriptor> groupDescriptors = cga.getGroupDescriptors();
        IWeightedTreeGroupDescriptor processGroup = Iterables.getFirst(groupDescriptors, null);
        assertNotNull(processGroup);

        WeightedTreeSet<@NonNull ICallStackSymbol, @NonNull Object> callGraph = WeightedTreeGroupBy.groupWeightedTreeBy(processGroup, baseCallGraph, cga);
        Collection<Object> elements = callGraph.getElements();
        assertEquals(2, elements.size());

        for (Object element : elements) {
            assertTrue(element instanceof ICallStackElement);
            switch (String.valueOf(element)) {
            case "1": {
                Collection<@NonNull ITree> children = ((ICallStackElement) element).getChildren();
                assertEquals(0, children.size());
                // Make sure the children have no tree with them
                for (ITree child : children) {
                    assertTrue(callGraph.getTreesFor(child).isEmpty());
                }
                Collection<@NonNull WeightedTree<@NonNull ICallStackSymbol>> callingContextTree = callGraph.getTreesFor(element);
                compareCcts("", getExpectedProcess1(), callingContextTree);
            }
                break;
            case "5": {
                Collection<@NonNull ITree> children = ((ICallStackElement) element).getChildren();
                assertEquals(0, children.size());
                for (ITree child : children) {
                    assertTrue(callGraph.getTreesFor(child).isEmpty());
                }
                Collection<@NonNull WeightedTree<@NonNull ICallStackSymbol>> callingContextTree = callGraph.getTreesFor(element);
                compareCcts("", getExpectedProcess5(), callingContextTree);
            }
                break;
            default:
                fail("Unexpected element: " + element);
            }
        }
    }

    /**
     * Test the group by leaf level of the call graph
     */
    @SuppressWarnings("null")
    @Test
    public void testGroupByThreadInstrumented() {
        CallStackAnalysisStub cga = getModule();
        CallGraph baseCallGraph = cga.getCallGraph();

        // The first group descriptor is the process
        Collection<IWeightedTreeGroupDescriptor> groupDescriptors = cga.getGroupDescriptors();
        IWeightedTreeGroupDescriptor group = Iterables.getFirst(groupDescriptors, null);
        assertNotNull(group);
        while (group.getNextGroup() != null) {
            group = group.getNextGroup();
            assertNotNull(group);
        }

        // Group by thread
        WeightedTreeSet<@NonNull ICallStackSymbol, @NonNull Object> callGraph = WeightedTreeGroupBy.groupWeightedTreeBy(group, baseCallGraph, cga);
        Collection<Object> elements = callGraph.getElements();
        assertEquals(2, elements.size());

        for (Object element : elements) {
            assertTrue(element instanceof ICallStackElement);
            switch (String.valueOf(element)) {
            case "1": {
                Collection<@NonNull ITree> children = ((ICallStackElement) element).getChildren();
                assertEquals(2, children.size());
                for (ITree thread : children) {
                    switch (String.valueOf(thread)) {
                    case "2": {
                        Collection<@NonNull WeightedTree<@NonNull ICallStackSymbol>> callingContextTree = callGraph.getTreesFor(thread);
                        compareCcts("", getExpectedThread2(), callingContextTree);
                    }
                        break;
                    case "3": {
                        Collection<@NonNull WeightedTree<@NonNull ICallStackSymbol>> callingContextTree = callGraph.getTreesFor(thread);
                        compareCcts("", getExpectedThread3(), callingContextTree);
                    }
                        break;
                    default:
                        fail("Unexpected thread element: " + thread);
                    }
                }
            }
                break;
            case "5": {
                Collection<@NonNull ITree> children = ((ICallStackElement) element).getChildren();
                assertEquals(2, children.size());
                for (ITree thread : children) {
                    switch (String.valueOf(thread)) {
                    case "6": {
                        Collection<@NonNull WeightedTree<@NonNull ICallStackSymbol>> callingContextTree = callGraph.getTreesFor(thread);
                        compareCcts("", getExpectedThread6(), callingContextTree);
                    }
                        break;
                    case "7": {
                        Collection<@NonNull WeightedTree<@NonNull ICallStackSymbol>> callingContextTree = callGraph.getTreesFor(thread);
                        compareCcts("", getExpectedThread7(), callingContextTree);
                    }
                        break;
                    default:
                        fail("Unexpected thread element: " + thread);
                    }
                }
            }
                break;
            default:
                fail("Unexpected element: " + element);
            }
        }
    }

    /**
     * Test changing the grouping for an analysis
     */
    @Test
    public void testMultiGroupBys() {
        // First, group by process
        testGroupByProcessInstrumented();

        // Then, regroup by thread
        testGroupByThreadInstrumented();

        // Then, group by all
        testGroupByAllInstrumented();

        // Group by process again
        testGroupByProcessInstrumented();

        // Group by all
        testGroupByAllInstrumented();

        // Finally by thread
        testGroupByThreadInstrumented();
    }

    private static void compareCcts(String prefix, Map<String, CallGraphExpected> expected, Collection<@NonNull WeightedTree<@NonNull ICallStackSymbol>> callingContextTree) {
        assertEquals(expected.size(), callingContextTree.size());
        for (WeightedTree<@NonNull ICallStackSymbol> callsite : callingContextTree) {
            assertTrue(callsite instanceof AggregatedCalledFunction);
            AggregatedCalledFunction function = (AggregatedCalledFunction) callsite;
            ICallStackSymbol callSiteSymbol = getCallSiteSymbol(function);
            CallGraphExpected cgExpected = expected.get(callSiteSymbol.resolve(Collections.emptySet()));
            assertNotNull(cgExpected);
            assertEquals("Callsite " + callSiteSymbol, cgExpected.duration, function.getDuration());
            assertEquals("Callsite " + callSiteSymbol, cgExpected.selfTime, function.getSelfTime());
            compareCcts(prefix + callSiteSymbol + ", ", cgExpected.children, callsite.getChildren());
        }
    }
}
