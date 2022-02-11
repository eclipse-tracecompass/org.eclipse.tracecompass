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

package org.eclipse.tracecompass.internal.analysis.graph.core.graph.historytree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.graph.IEdgeContextStateFactory;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdgeContextState;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.graph.WorkerSerializer;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState.OSEdgeContextEnum;

/**
 * HistoryTreeGraph implementation to store the linux execution graph on disk.
 *
 * This graph extends the basic history tree graph to implement
 * functions that specific to the OsEdgeContextState.
 *
 * @author Arnaud Fiorini
 */
public class OsHistoryTreeGraph extends HistoryTreeTmfGraph {

    private class OsEdgeContextStateFactory implements IEdgeContextStateFactory {
        @Override
        public ITmfEdgeContextState createContextState(int code) {
            return new OSEdgeContextState(code);
        }
    }

    private class Reader implements IHTIntervalReader<TmfEdgeInterval> {
        @Override
        public TmfEdgeInterval readInterval(ISafeByteBufferReader buffer) {
            return TmfEdgeInterval.readBuffer(buffer, new OsEdgeContextStateFactory());
        }

    }

    /**
     * Instantiate the {@link OsHistoryTreeGraph}.
     *
     * @param newStateFile The filename/location where to store the state history (Should end in .ht)
     * @param version The version number of the reader/writer
     * @param workerSerializer The worker serializer object for this graph
     * @param startTime The start time of this graph
     * @throws IOException Thrown if we can't create the file for some reason
     */
    public OsHistoryTreeGraph(Path newStateFile, int version, WorkerSerializer workerSerializer, long startTime) throws IOException {
        super(newStateFile, version, workerSerializer, startTime);
    }

    @Override
    protected GraphHistoryTree createHistoryTree(Path treeFile, int version, long startTime) throws IOException {
        try {
            if (Files.exists(treeFile)) {
                GraphHistoryTree sht = new GraphHistoryTree(NonNullUtils.checkNotNull(treeFile.toFile()), version, new Reader());
                readWorkers(sht);
                setFinishedBuilding(true);
                return sht;
            }
        } catch (IOException e) {
            /**
             * Couldn't create the history tree with this file, just fall back
             * to a new history tree and clean worker attribs
             */
            getWorkerAttrib().clear();
        }

        return new GraphHistoryTree(NonNullUtils.checkNotNull(treeFile.toFile()),
                BLOCK_SIZE,
                MAX_CHILDREN,
                version,
                startTime,
                new Reader());
    }

    @Override
    public @Nullable ITmfEdge appendUnknown(ITmfVertex vertex) {
        return append(vertex, new OSEdgeContextState(OSEdgeContextEnum.UNKNOWN), StringUtils.EMPTY);
    }

    @Override
    public @Nullable ITmfEdge append(ITmfVertex vertex) {
        return append(vertex, new OSEdgeContextState(OSEdgeContextEnum.DEFAULT), StringUtils.EMPTY);
    }

    @Override
    public @Nullable ITmfEdge edgeUnknown(ITmfVertex from, ITmfVertex to) {
        return edge(from, to, new OSEdgeContextState(OSEdgeContextEnum.UNKNOWN));
    }

    @Override
    public @Nullable ITmfEdge edge(ITmfVertex from, ITmfVertex to) {
        return edge(from, to, new OSEdgeContextState(OSEdgeContextEnum.DEFAULT));
    }

}
