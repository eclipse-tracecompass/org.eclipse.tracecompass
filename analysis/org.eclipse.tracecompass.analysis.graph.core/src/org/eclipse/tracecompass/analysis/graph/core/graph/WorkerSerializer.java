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

import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;

/**
 * An interface for worker serialization to save workers to disk. The serializer
 * should be specific to a worker implementation.
 *
 * @author Geneviève Bastien
 * @since 4.0
 */
public interface WorkerSerializer {
    /**
     * Serializes the worker to string
     *
     * @param worker
     *            The worker to serialize
     * @return A string representing the worker that can then be de-serialized
     *         to a worker object
     */
    String serialize(IGraphWorker worker);

    /**
     * Converts a worker string to a worker object
     *
     * @param serializedWorker
     *            The serialized worker string
     * @return The deserialized worker object
     */
    IGraphWorker deserialize(String serializedWorker);
}
