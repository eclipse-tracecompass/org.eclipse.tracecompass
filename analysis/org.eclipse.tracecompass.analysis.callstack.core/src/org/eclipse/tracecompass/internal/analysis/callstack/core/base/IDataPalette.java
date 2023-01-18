/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core.base;

import java.util.Map;

import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;

/**
 * An interface for data palette that describe return the list of styles and get
 * the specific style for an object
 *
 * @author Geneviève Bastien
 */
public interface IDataPalette {

    /**
     * Get the style for an object. The returned style should not be one of the
     * original style returned by the {@link #getStyles()} method, but a style
     * object with the name of the base style as parent and a possibly empty
     * map.
     *
     * @param object
     *            The object for which to get the style
     * @return The style for the object
     */
    OutputElementStyle getStyleFor(Object object);

    /**
     * Get the map of all styles provided by this palette. These are the base
     * styles, mapping to the key for each style. Styles for object can then
     * refer to those base styles as parents.
     *
     * @return The map of style name to full style description.
     */
    Map<String, OutputElementStyle> getStyles();
}
