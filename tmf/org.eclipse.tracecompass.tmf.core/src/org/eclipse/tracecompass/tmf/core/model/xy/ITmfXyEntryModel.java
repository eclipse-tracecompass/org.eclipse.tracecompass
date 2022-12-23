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

package org.eclipse.tracecompass.tmf.core.model.xy;

import org.eclipse.tracecompass.tmf.core.model.tree.ITmfSelectionTreeDataModel;

/**
 * Represents an XY entry model. These objects are typically returned by
 * {@link ITmfTreeXYDataProvider#fetchTree}.
 *
 * @author Bernd Hufmann
 * @since 8.3
 */
public interface ITmfXyEntryModel extends ITmfSelectionTreeDataModel {
    // no additional interfaces for now
}
