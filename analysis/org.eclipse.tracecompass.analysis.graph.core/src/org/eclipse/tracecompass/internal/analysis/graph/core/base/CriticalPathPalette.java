/*******************************************************************************
 * Copyright (c) 2020, 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.base;

import java.util.Map;

import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;

@Deprecated
public class CriticalPathPalette {

    /**
     * Get the map of all styles provided by this palette. These are the base
     * styles, mapping to the key for each style. Styles for object can then
     * refer to those base styles as parents.
     *
     * @return The map of style name to full style description.
     */
    public static Map<String, OutputElementStyle> getStyles() {
        return OSCriticalPathPalette.getStyles();
    }
}
