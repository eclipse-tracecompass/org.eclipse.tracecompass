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

package org.eclipse.tracecompass.analysis.graph.core.tests.analysis.criticalpath;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.eclipse.tracecompass.analysis.graph.core.criticalpath.CriticalPathAlgorithmException;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.ICriticalPathAlgorithm;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.GraphBuilder;
import org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath.CriticalPathAlgorithmBounded;
import org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath.OSCriticalPathAlgorithm;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.TmfGraphLegacyWrapper;

/**
 * Test the {@link CriticalPathAlgorithmBounded} critical path algorithm
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 */
public class TmfCriticalPathAlgoBoundedTest extends TmfCriticalPathAlgorithmTest {

    @Override
    protected ITmfGraph computeCriticalPath(ITmfGraph graph, ITmfVertex start) {
        assertNotNull(graph);
        ICriticalPathAlgorithm cp = new OSCriticalPathAlgorithm(graph);
        try {
            return cp.computeCriticalPath(new TmfGraphLegacyWrapper(), start, null);
        } catch (CriticalPathAlgorithmException e) {
            fail(e.getMessage());
        }
        return null;
    }

    @Override
    protected ITmfGraph getExpectedCriticalPath(GraphBuilder builder) {
        return builder.criticalPathBounded();
    }

}
