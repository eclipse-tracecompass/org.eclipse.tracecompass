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

package org.eclipse.tracecompass.analysis.graph.core.graph;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree.HistoryTreeTmfGraph;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.TmfGraphLegacyWrapper;

/**
 * The factory to create execution graphs
 *
 * @author Geneviève Bastien
 * @since 3.1
 */
public class TmfGraphFactory {

    /**
     * Create a graph object that will save the data to disk
     *
     * @param htFile
     *            The file where to save the graph
     * @param workerSerializer
     *            The worker serializer object that will [de-]serialize the
     *            workers for the graph
     * @param startTime
     *            The start time of the graph
     * @param version
     *            The version of this history tree
     * @return A graph object that will save the data to disk, or
     *         <code>null</code> if the graph cannot be created.
     */
    public static @Nullable ITmfGraph createOnDiskGraph(Path htFile, WorkerSerializer workerSerializer, long startTime, int version) {
        HistoryTreeTmfGraph graph;
        try {
            graph = new HistoryTreeTmfGraph(htFile, version, workerSerializer, startTime);
        } catch (IOException e) {
            return null;
        }

        return graph;
    }

    /**
     * Create an in-memory graph. If the trace is too large, building it may
     * cause OutOfMemoryExceptions.
     *
     * @return A new in-memory graph
     */
    public static ITmfGraph createSimpleGraph() {
        return new TmfGraphLegacyWrapper();
    }

}
