/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.tmf.core.model.tree;

import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;

/**
 * Represents an ITmfTreeDataModel entry model that can provide detail about
 * default selection. These objects are typically returned by
 * {@link ITmfTreeXYDataProvider#fetchTree}.
 *
 * @author Bernd Hufmann
 * @since 8.3
 */
public interface ITmfSelectionTreeDataModel extends ITmfTreeDataModel {

    /**
     * @return whether or not the entry is a default entry and its xy data
     *         should be fetched by default.
     */
    default boolean isDefault() {
        return false;
    }
}
