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

package org.eclipse.tracecompass.internal.analysis.callstack.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to manage the models for the hosts
 *
 * @author Geneviève Bastien
 */
public final class ModelManager {

    private static final Map<String, IHostModel> MODELS_FOR_HOST = new HashMap<>();

    private ModelManager() {
    }

    /**
     * Get the model for a given host ID.
     *
     * @param hostId
     *            The ID of the host for which to retrieve the model
     * @return The model for the host
     */
    public static synchronized IHostModel getModelFor(String hostId) {
        IHostModel model = MODELS_FOR_HOST.get(hostId);
        if (model == null) {
            model = new CompositeHostModel(hostId);
            MODELS_FOR_HOST.put(hostId, model);
        }
        return model;
    }

    /**
     * Dispose all the models
     */
    public static synchronized void disposeModels() {
        MODELS_FOR_HOST.values().forEach(IHostModel::dispose);
        MODELS_FOR_HOST.clear();
    }
}
