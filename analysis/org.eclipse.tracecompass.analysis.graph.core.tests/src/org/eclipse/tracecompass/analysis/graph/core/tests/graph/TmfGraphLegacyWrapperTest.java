/*******************************************************************************
 * Copyright (c) 2022, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.tests.graph;

import java.io.IOException;

import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.TmfGraphLegacyWrapper;

/**
 * @author gbastien
 *
 */
public class TmfGraphLegacyWrapperTest extends ITmfGraphTest {

    @Override
    protected ITmfGraph createNewGraph() {
        return new TmfGraphLegacyWrapper(new TmfGraph());
    }

    @Override
    protected void deleteGraph(ITmfGraph graph) throws IOException {
        // Nothing to do
    }

}
