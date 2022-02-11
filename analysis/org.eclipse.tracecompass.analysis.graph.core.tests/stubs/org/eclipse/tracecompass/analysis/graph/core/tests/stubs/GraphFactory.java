/*******************************************************************************
 * Copyright (c) 2015, 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.tests.stubs;

import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState.OSEdgeContextEnum;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.TmfGraphLegacyWrapper;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;

/**
 * Factory generating various scenarios of graphs to test critical path
 * algorithms on
 *
 * @author Geneviève Bastien
 * @author Francis Giraldeau
 */
public class GraphFactory {

    /**
     * First default actor of a graph
     */
    public static final IGraphWorker Actor0 = new TestGraphWorker(0);

    /**
     * Second default actor of the graph
     */
    public static final TestGraphWorker Actor1 = new TestGraphWorker(1);

    /** An optional link qualifier on some edges */
    private static final String LINK_QUALIFIER = "testLinkQualifier";

    /**
     * Simple RUNNING edge involving one object
     */
    public static final GraphBuilder GRAPH_BASIC =
            new GraphBuilder("basic") {

                @Override
                public ITmfGraph build() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0 = graph.createVertex(Actor0, 0);
                    graph.add(v0);
                    ITmfVertex v1 = graph.createVertex(Actor0, 1);
                    graph.append(v1, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    return build();
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    return build();
                }

            };

    /**
     * Single object, timer starts at t2 and wakes up at t4. Blocked at t3
     *
     * <pre>
     *        /   -T-   \
     * * -R- * -R- * -B- * -R- *
     * </pre>
     */
    public static final GraphBuilder GRAPH_WAKEUP_SELF =
            new GraphBuilder("wakeup_self") {
                @Override
                public ITmfGraph build() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    ITmfVertex vStart = graph.createVertex(Actor0, 1);
                    graph.append(vStart, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 2), new OSEdgeContextState(OSEdgeContextEnum.RUNNING), LINK_QUALIFIER);
                    ITmfVertex vEnd = graph.createVertex(Actor0, 3);
                    graph.append(vEnd, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor0, 4), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.edgeVertical(vStart, vEnd, new OSEdgeContextState(OSEdgeContextEnum.TIMER), null);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 1), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 2), new OSEdgeContextState(OSEdgeContextEnum.RUNNING), LINK_QUALIFIER);
                    graph.append(graph.createVertex(Actor0, 3), new OSEdgeContextState(OSEdgeContextEnum.TIMER));
                    graph.append(graph.createVertex(Actor0, 4), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 1), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 3), new OSEdgeContextState(OSEdgeContextEnum.TIMER));
                    graph.append(graph.createVertex(Actor0, 4), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    return graph;
                }

            };

    /**
     * Single object, 4 vertices, blocked between 2 and 3, but nothing wakes up
     *
     * <pre>
     * * -R- * -B- * -R- *
     * </pre>
     */
    public static final GraphBuilder GRAPH_WAKEUP_MISSING =
            new GraphBuilder("wakeup_missing") {
                @Override
                public ITmfGraph build() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 2), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 4), new OSEdgeContextState(OSEdgeContextEnum.BLOCKED), LINK_QUALIFIER);
                    graph.append(graph.createVertex(Actor0, 6), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 2), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 4), new OSEdgeContextState(OSEdgeContextEnum.BLOCKED), LINK_QUALIFIER);
                    graph.append(graph.createVertex(Actor0, 6), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    throw new UnsupportedOperationException();
                }
            };

    /**
     * Object woken from blockage by another network object
     *
     * <pre>
     * * - R - * - B - * - R - *
     *               /N
     *             *
     * </pre>
     */
    public static final GraphBuilder GRAPH_WAKEUP_UNKNOWN =
            new GraphBuilder("wakeup_unknown") {
                @Override
                public ITmfGraph build() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex vIn = graph.createVertex(Actor0, 4);
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 2), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append( vIn, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor0, 6), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    ITmfVertex vNet = graph.createVertex(Actor1, 3);
                    graph.add( vNet);
                    graph.edge(vNet, vIn, new OSEdgeContextState(OSEdgeContextEnum.NETWORK));
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    ITmfVertex vStartBlock = graph.createVertex(Actor0, 2);
                    ITmfVertex vEndBlock = graph.createVertex(Actor0, 4);
                    graph.append(vStartBlock, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(vEndBlock);
                    graph.append(graph.createVertex(Actor0, 6), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    ITmfVertex vStartOther = graph.createVertex(Actor1, 2);
                    ITmfVertex vEndOther = graph.createVertex(Actor1, 3);
                    graph.add(vStartOther);
                    graph.appendUnknown(vEndOther);

                    graph.edge(vStartBlock, vStartOther);
                    graph.edge(vEndOther, vEndBlock, new OSEdgeContextState(OSEdgeContextEnum.NETWORK));
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    ITmfVertex vStartBlock = graph.createVertex(Actor0, 3);
                    ITmfVertex vEndBlock = graph.createVertex(Actor0, 4);
                    graph.append(graph.createVertex(Actor0, 2), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.appendUnknown(vStartBlock);
                    graph.add(vEndBlock);
                    graph.append(graph.createVertex(Actor0, 6), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.edgeVertical(vStartBlock, vEndBlock, new OSEdgeContextState(OSEdgeContextEnum.NETWORK), null);

                    return graph;
                }
            };

    /**
     * Object woken from blockage by another running object that was created by
     * first object
     *
     * <pre>
     * * -R- * -R- * -B- * -R- *
     *         \         |
     *          *  -R-   *
     * </pre>
     */
    public static GraphBuilder GRAPH_WAKEUP_NEW =
            new GraphBuilder("wakeup_new") {
                @Override
                public ITmfGraph build() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex vSrcLink = graph.createVertex(Actor0, 2);
                    ITmfVertex vBlockEnd = graph.createVertex(Actor0, 6);
                    ITmfVertex vDstLink = graph.createVertex(Actor1, 3);
                    ITmfVertex vWakeup = graph.createVertex(Actor1, 6);
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(vSrcLink, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 4), new OSEdgeContextState(OSEdgeContextEnum.RUNNING), LINK_QUALIFIER);
                    graph.append(vBlockEnd, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor0, 8), new OSEdgeContextState(OSEdgeContextEnum.RUNNING), LINK_QUALIFIER);

                    graph.add(vDstLink);
                    graph.append(vWakeup, new OSEdgeContextState(OSEdgeContextEnum.RUNNING), LINK_QUALIFIER);

                    graph.edge(vSrcLink, vDstLink);
                    graph.edge(vWakeup, vBlockEnd);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex vBlockStart = graph.createVertex(Actor0, 4);
                    ITmfVertex vBlockEnd = graph.createVertex(Actor0, 6);
                    ITmfVertex vDstLink = graph.createVertex(Actor1, 4);
                    ITmfVertex vWakeup = graph.createVertex(Actor1, 6);
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 2), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(vBlockStart, new OSEdgeContextState(OSEdgeContextEnum.RUNNING), LINK_QUALIFIER);
                    graph.add(vBlockEnd);
                    graph.append(graph.createVertex(Actor0, 8), new OSEdgeContextState(OSEdgeContextEnum.RUNNING), LINK_QUALIFIER);

                    graph.add(vDstLink);
                    graph.append(vWakeup, new OSEdgeContextState(OSEdgeContextEnum.RUNNING), LINK_QUALIFIER);

                    graph.edge(vBlockStart, vDstLink);
                    graph.edge(vWakeup, vBlockEnd);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex vSrcLink = graph.createVertex(Actor0, 2);
                    ITmfVertex vBlockEnd = graph.createVertex(Actor0, 6);
                    ITmfVertex vDstLink = graph.createVertex(Actor1, 3);
                    ITmfVertex vWakeup = graph.createVertex(Actor1, 6);
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(vSrcLink, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(vBlockEnd);
                    graph.append(graph.createVertex(Actor0, 8), new OSEdgeContextState(OSEdgeContextEnum.RUNNING), LINK_QUALIFIER);

                    graph.add(vDstLink);
                    graph.append(vWakeup, new OSEdgeContextState(OSEdgeContextEnum.RUNNING), LINK_QUALIFIER);

                    graph.edge(vSrcLink, vDstLink);
                    graph.edge(vWakeup, vBlockEnd);
                    return graph;
                }
            };

    /**
     * Two objects join to unblock the first but with delay
     *
     * <pre>
     * 0: * --R-- * --B-- * --R-- *
     *              /
     * 1: * -R- * --R-- *
     * </pre>
     */
    public static GraphBuilder GRAPH_OPENED_DELAY =
            new GraphBuilder("opened") {
                @Override
                public ITmfGraph build() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 3), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    ITmfVertex v1 = graph.createVertex(Actor0, 6);
                    graph.append(v1, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor0, 9), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(graph.createVertex(Actor1, 0));
                    ITmfVertex v2 = graph.createVertex(Actor1, 2);
                    graph.append(v2, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor1, 5), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.edge(v2, v1);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    ITmfVertex v1 = graph.createVertex(Actor0, 3);
                    graph.append(v1, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    ITmfVertex v2 = graph.createVertex(Actor1, 3);
                    ITmfVertex v3 = graph.createVertex(Actor0, 6);
                    graph.add(v2);
                    graph.add(v3);
                    graph.edge(v1, v2);
                    graph.edge(v2, v3);
                    graph.append(graph.createVertex(Actor0, 9), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    throw new UnsupportedOperationException();
                }
            };

    /**
     * Two objects join to unblock the first without delay
     *
     * <pre>
     * * --R-- * --B-- * --R-- *
     *                 |
     * * -------R----- * --R-- *
     * </pre>
     */
    public static GraphBuilder GRAPH_OPENED =
            new GraphBuilder("opened") {
                @Override
                public ITmfGraph build() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 3), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    ITmfVertex v1 = graph.createVertex(Actor0, 6);
                    graph.append(v1, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor0, 9), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(graph.createVertex(Actor1, 0));
                    ITmfVertex v2 = graph.createVertex(Actor1, 6);
                    graph.append(v2, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor1, 9), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.edge(v2, v1);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    graph.add(graph.createVertex(Actor0, 0));
                    ITmfVertex v1 = graph.createVertex(Actor0, 3);
                    graph.append(v1, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    ITmfVertex v2 = graph.createVertex(Actor1, 3);
                    ITmfVertex v3 = graph.createVertex(Actor1, 6);
                    graph.add(v2);
                    graph.append(v3, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    ITmfVertex v4 = graph.createVertex(Actor0, 6);
                    graph.add(v4);
                    graph.edge(v1, v2);
                    graph.edge(v3, v4);
                    graph.append(graph.createVertex(Actor0, 9), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    throw new UnsupportedOperationException();
                }
            };

    /**
     * Two objects are blocked and mutually unblock at different times
     *
     * <pre>
     * 0: * -R- * -R- * -R- * -B- * -R- *
     *                |           |
     * 1: * -R- * -B- * -R- * -R- * -R- *
     * </pre>
     */
    public static GraphBuilder GRAPH_WAKEUP_MUTUAL =
            new GraphBuilder("wakeup_mutual") {
                @Override
                public ITmfGraph build() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0Wakeup = graph.createVertex(Actor0, 2);
                    ITmfVertex v0Unblock = graph.createVertex(Actor0, 4);
                    ITmfVertex v1Unblock = graph.createVertex(Actor1, 2);
                    ITmfVertex v1Wakeup = graph.createVertex(Actor1, 4);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 1), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0Wakeup, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 3), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0Unblock, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor0, 5), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(graph.createVertex(Actor1, 0));
                    graph.append(graph.createVertex(Actor1, 1), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v1Unblock, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor1, 3), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v1Wakeup, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor1, 5), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v0Wakeup, v1Unblock);
                    graph.edge(v1Wakeup, v0Unblock);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0StartBlock = graph.createVertex(Actor0, 3);
                    ITmfVertex v0EndBlock = graph.createVertex(Actor0, 4);
                    ITmfVertex v1StartBlock = graph.createVertex(Actor1, 3);
                    ITmfVertex v1EndBlock = graph.createVertex(Actor1, 4);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 1), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 2), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0StartBlock, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(v0EndBlock);
                    graph.append(graph.createVertex(Actor0, 5), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(v1StartBlock);
                    graph.append(v1EndBlock, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v0StartBlock, v1StartBlock);
                    graph.edge(v1EndBlock, v0EndBlock);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0Wakeup = graph.createVertex(Actor0, 2);
                    ITmfVertex v0Unblock = graph.createVertex(Actor0, 4);
                    ITmfVertex v1Unblock = graph.createVertex(Actor1, 2);
                    ITmfVertex v1Wakeup = graph.createVertex(Actor1, 4);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 1), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0Wakeup, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(v0Unblock);
                    graph.append(graph.createVertex(Actor0, 5), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(v1Unblock);
                    graph.append(graph.createVertex(Actor1, 3), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v1Wakeup, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v0Wakeup, v1Unblock);
                    graph.edge(v1Wakeup, v0Unblock);
                    return graph;
                }
            };

    /**
     * Many objects wakeup the first object, the calls are embedded
     *
     * <pre>
     * 0: * -R- * -R- * -R- * -B- * -B- * -R- *
     *          |     |           |     |
     * 1:       |     * --- R --- *     |
     *          |                       |
     * 2:       *    ------ R ------    *
     * ...
     * </pre>
     */
    public static GraphBuilder GRAPH_WAKEUP_EMBEDDED =
            new GraphBuilder("wakeup_embeded") {
                private TestGraphWorker fActor2 = new TestGraphWorker(2);

                @Override
                public ITmfGraph build() {
                    /* Initialize some vertices */
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0FirstFork = graph.createVertex(Actor0, 2);
                    ITmfVertex v0SecondFork = graph.createVertex(Actor0, 4);
                    ITmfVertex v0FirstUnblock = graph.createVertex(Actor0, 8);
                    ITmfVertex v0SecondUnblock = graph.createVertex(Actor0, 10);
                    ITmfVertex v1In = graph.createVertex(Actor1, 4);
                    ITmfVertex v1Out = graph.createVertex(Actor1, 8);
                    ITmfVertex v2In = graph.createVertex(fActor2, 2);
                    ITmfVertex v2Out = graph.createVertex(fActor2, 10);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(v0FirstFork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0SecondFork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 6), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0FirstUnblock, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(v0SecondUnblock, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor0, 12), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(v1In);
                    graph.append(v1Out, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 2's vertices and edges */
                    graph.add(v2In);
                    graph.append(v2Out, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v0FirstFork, v2In);
                    graph.edge(v0SecondFork, v1In);
                    graph.edge(v1Out, v0FirstUnblock);
                    graph.edge(v2Out, v0SecondUnblock);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    /* Initialize some vertices */
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0StartBlock = graph.createVertex(Actor0, 6);
                    ITmfVertex v0FirstUnblock = graph.createVertex(Actor0, 8);
                    ITmfVertex v0SecondUnblock = graph.createVertex(Actor0, 10);
                    ITmfVertex v1In = graph.createVertex(Actor1, 6);
                    ITmfVertex v1Out = graph.createVertex(Actor1, 8);
                    ITmfVertex v2In = graph.createVertex(fActor2, 8);
                    ITmfVertex v2Out = graph.createVertex(fActor2, 10);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 2), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 4), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0StartBlock, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(v0FirstUnblock);
                    graph.add(v0SecondUnblock);
                    graph.append(graph.createVertex(Actor0, 12), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(v1In);
                    graph.append(v1Out, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 2's vertices and edges */
                    graph.add(v2In);
                    graph.append(v2Out, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v0StartBlock, v1In);
                    graph.edge(v1Out, v0FirstUnblock);
                    graph.edge(v0FirstUnblock, v2In);
                    graph.edge(v2Out, v0SecondUnblock);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    throw new UnsupportedOperationException();
                }
            };

    /**
     * Many objects wakeup the first object, the calls interleave
     *
     * <pre>
     * 0: * -R- * -R- * -R- * -B- * -B- * -R- *
     *          |     |           |     |
     * 1:       * ------ R ------ *     |
     *                |                 |
     * 2:             * ------ R ------ *
     * </pre>
     */
    public static GraphBuilder GRAPH_WAKEUP_INTERLEAVE =
            new GraphBuilder("wakeup_interleave") {
                private TestGraphWorker fActor2 = new TestGraphWorker(2);

                @Override
                public ITmfGraph build() {
                    /* Initialize some vertices */
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0FirstFork = graph.createVertex(Actor0, 2);
                    ITmfVertex v0SecondFork = graph.createVertex(Actor0, 4);
                    ITmfVertex v0FirstUnblock = graph.createVertex(Actor0, 8);
                    ITmfVertex v0SecondUnblock = graph.createVertex(Actor0, 10);
                    ITmfVertex v1In = graph.createVertex(Actor1, 2);
                    ITmfVertex v1Out = graph.createVertex(Actor1, 8);
                    ITmfVertex v2In = graph.createVertex(fActor2, 4);
                    ITmfVertex v2Out = graph.createVertex(fActor2, 10);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(v0FirstFork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0SecondFork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 6), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0FirstUnblock, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(v0SecondUnblock, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor0, 12), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(v1In);
                    graph.append(v1Out, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 2's vertices and edges */
                    graph.add(v2In);
                    graph.append(v2Out, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v0FirstFork, v1In);
                    graph.edge(v0SecondFork, v2In);
                    graph.edge(v1Out, v0FirstUnblock);
                    graph.edge(v2Out, v0SecondUnblock);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    /* Initialize some vertices */
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0StartBlock = graph.createVertex(Actor0, 6);
                    ITmfVertex v0FirstUnblock = graph.createVertex(Actor0, 8);
                    ITmfVertex v0SecondUnblock = graph.createVertex(Actor0, 10);
                    ITmfVertex v1In = graph.createVertex(Actor1, 6);
                    ITmfVertex v1Out = graph.createVertex(Actor1, 8);
                    ITmfVertex v2In = graph.createVertex(fActor2, 8);
                    ITmfVertex v2Out = graph.createVertex(fActor2, 10);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 2), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor0, 4), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0StartBlock, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(v0FirstUnblock);
                    graph.add(v0SecondUnblock);
                    graph.append(graph.createVertex(Actor0, 12), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(v1In);
                    graph.append(v1Out, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 2's vertices and edges */
                    graph.add(v2In);
                    graph.append(v2Out, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v0StartBlock, v1In);
                    graph.edge(v1Out, v0FirstUnblock);
                    graph.edge(v0FirstUnblock, v2In);
                    graph.edge(v2Out, v0SecondUnblock);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    throw new UnsupportedOperationException();
                }
            };

    /**
     * Objects block when creating new ones, nesting the blocks
     *
     * <pre>
     * ...
     * 0: * -R- * --------------B-------------- * -R- *
     *          |                               |
     * 1:       * -R- * --------B-------- * -R- *
     *                |                   |
     * 2:             * -R- * --B-- * -R- *
     *                      |       |
     * 3:                   * --R-- *
     * </pre>
     */
    public static GraphBuilder GRAPH_NESTED =
            new GraphBuilder("wakeup_nested") {
                private final TestGraphWorker fActor2 = new TestGraphWorker(2);
                private final TestGraphWorker fActor3 = new TestGraphWorker(3);

                @Override
                public ITmfGraph build() {
                    /* Initialize some vertices */
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0Fork = graph.createVertex(Actor0, 1);
                    ITmfVertex v0Return = graph.createVertex(Actor0, 6);
                    ITmfVertex v1In = graph.createVertex(Actor1, 1);
                    ITmfVertex v1Fork = graph.createVertex(Actor1, 2);
                    ITmfVertex v1Return = graph.createVertex(Actor1, 5);
                    ITmfVertex v1End = graph.createVertex(Actor1, 6);
                    ITmfVertex v2In = graph.createVertex(fActor2, 2);
                    ITmfVertex v2Fork = graph.createVertex(fActor2, 3);
                    ITmfVertex v2Return = graph.createVertex(fActor2, 4);
                    ITmfVertex v2End = graph.createVertex(fActor2, 5);
                    ITmfVertex v3In = graph.createVertex(fActor3, 3);
                    ITmfVertex v3End = graph.createVertex(fActor3, 4);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(v0Fork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0Return, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor0, 7), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(v1In);
                    graph.append(v1Fork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v1Return, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(v1End, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 2's vertices and edges */
                    graph.add(v2In);
                    graph.append(v2Fork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v2Return, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(v2End, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 3's vertices and edges */
                    graph.add(v3In);
                    graph.append(v3End, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v0Fork, v1In);
                    graph.edge(v1Fork, v2In);
                    graph.edge(v2Fork, v3In);
                    graph.edge(v3End, v2Return);
                    graph.edge(v2End, v1Return);
                    graph.edge(v1End, v0Return);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    /* Initialize some vertices */
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0Fork = graph.createVertex(Actor0, 1);
                    ITmfVertex v0Return = graph.createVertex(Actor0, 6);
                    ITmfVertex v1In = graph.createVertex(Actor1, 1);
                    ITmfVertex v1Fork = graph.createVertex(Actor1, 2);
                    ITmfVertex v1Return = graph.createVertex(Actor1, 5);
                    ITmfVertex v1End = graph.createVertex(Actor1, 6);
                    ITmfVertex v2In = graph.createVertex(fActor2, 2);
                    ITmfVertex v2Fork = graph.createVertex(fActor2, 3);
                    ITmfVertex v2Return = graph.createVertex(fActor2, 4);
                    ITmfVertex v2End = graph.createVertex(fActor2, 5);
                    ITmfVertex v3In = graph.createVertex(fActor3, 3);
                    ITmfVertex v3End = graph.createVertex(fActor3, 4);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(v0Fork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(v0Return);
                    graph.append(graph.createVertex(Actor0, 7), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(v1In);
                    graph.append(v1Fork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(v1Return);
                    graph.append(v1End, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 2's vertices and edges */
                    graph.add(v2In);
                    graph.append(v2Fork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(v2Return);
                    graph.append(v2End, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 3's vertices and edges */
                    graph.add(v3In);
                    graph.append(v3End, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v0Fork, v1In);
                    graph.edge(v1Fork, v2In);
                    graph.edge(v2Fork, v3In);
                    graph.edge(v3End, v2Return);
                    graph.edge(v2End, v1Return);
                    graph.edge(v1End, v0Return);
                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    return criticalPathBounded();
                }
            };

    /**
     * An object is blocked until a few other objects exchange network messages
     *
     * <pre>
     * 0: * -R- * ----------------------- B ---------------------- * -R- *
     *                                                             |
     * 1:              * -R- * -R- *                               |
     *                        \  ----N----  \                      |
     * 2:                              * -R- * -R- *               |
     *                                        \  ----N----  \      |
     * 3:                                              * -R- * -R- *
     * </pre>
     */
    public static final GraphBuilder GRAPH_NET1 =
            new GraphBuilder("wakeup_net1") {
                private TestGraphWorker fActor2 = new TestGraphWorker(2);
                private TestGraphWorker fActor3 = new TestGraphWorker(3);

                @Override
                public ITmfGraph build() {
                    /* Initialize some vertices */
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0Unblock = graph.createVertex(Actor0, 11);
                    ITmfVertex v1Send = graph.createVertex(Actor1, 4);
                    ITmfVertex v2Rcv = graph.createVertex(fActor2, 7);
                    ITmfVertex v2Send = graph.createVertex(fActor2, 8);
                    ITmfVertex v3Rcv = graph.createVertex(fActor3, 10);
                    ITmfVertex v3End = graph.createVertex(fActor3, 11);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(graph.createVertex(Actor0, 1), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v0Unblock, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
                    graph.append(graph.createVertex(Actor0, 12), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(graph.createVertex(Actor1, 3));
                    graph.append(v1Send, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(graph.createVertex(Actor1, 5), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 2's vertices and edges */
                    graph.add(graph.createVertex(fActor2, 6));
                    graph.append(v2Rcv, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v2Send, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 3's vertices and edges */
                    graph.add(graph.createVertex(fActor3, 9));
                    graph.append(v3Rcv, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.append(v3End, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v1Send, v2Rcv, new OSEdgeContextState(OSEdgeContextEnum.NETWORK));
                    graph.edge(v2Send, v3Rcv, new OSEdgeContextState(OSEdgeContextEnum.NETWORK));
                    graph.edge(v3End, v0Unblock);

                    return graph;
                }

                @Override
                public ITmfGraph criticalPathBounded() {
                    /* Initialize some vertices */
                    ITmfGraph graph = new TmfGraphLegacyWrapper();
                    ITmfVertex v0Fork = graph.createVertex(Actor0, 1);
                    ITmfVertex v0Unblock = graph.createVertex(Actor0, 11);
                    ITmfVertex v1Start = graph.createVertex(Actor1, 1);
                    ITmfVertex v1Send = graph.createVertex(Actor1, 4);
                    ITmfVertex v2Rcv = graph.createVertex(fActor2, 7);
                    ITmfVertex v2Send = graph.createVertex(fActor2, 8);
                    ITmfVertex v3Rcv = graph.createVertex(fActor3, 10);
                    ITmfVertex v3End = graph.createVertex(fActor3, 11);

                    /* Add actor 0's vertices and edges */
                    graph.add(graph.createVertex(Actor0, 0));
                    graph.append(v0Fork, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
                    graph.add(v0Unblock);
                    graph.append(graph.createVertex(Actor0, 12), new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 1's vertices and edges */
                    graph.add(v1Start);
                    graph.append(graph.createVertex(Actor1, 3), new OSEdgeContextState(OSEdgeContextEnum.UNKNOWN));
                    graph.append(v1Send, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 2's vertices and edges */
                    graph.add(v2Rcv);
                    graph.append(v2Send, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add actor 3's vertices and edges */
                    graph.add(v3Rcv);
                    graph.append(v3End, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));

                    /* Add vertical links */
                    graph.edge(v0Fork, v1Start);
                    graph.edge(v1Send, v2Rcv, new OSEdgeContextState(OSEdgeContextEnum.NETWORK));
                    graph.edge(v2Send, v3Rcv, new OSEdgeContextState(OSEdgeContextEnum.NETWORK));
                    graph.edge(v3End, v0Unblock);

                    return graph;
                }

                @Override
                public ITmfGraph criticalPathUnbounded() {
                    throw new UnsupportedOperationException();
                }
            };

}
