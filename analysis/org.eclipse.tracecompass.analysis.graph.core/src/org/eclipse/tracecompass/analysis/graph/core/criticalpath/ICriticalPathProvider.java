/*******************************************************************************
 * Copyright (c) 2017, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.criticalpath;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;

/**
 * @author Geneviève Bastien
 * @since 1.1
 */
public interface ICriticalPathProvider {

    /**
     * Get the critical path
     *
     * @return The critical path
     * @since 4.0
     */
   default @Nullable ITmfGraph getCriticalPathGraph() {
       return null;
   }

}
