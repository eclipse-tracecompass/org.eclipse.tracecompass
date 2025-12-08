/*******************************************************************************
 * Copyright (c) 2025 Hong Anh
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.dialogs;


/**
 * A filter that extends {@link MultiTreePatternFilter} to allow filtering
 * based on a specific column index.
 *
 * This implementation enables users to apply multiple patterns (separated by '/')
 * while specifying which column of the tree should be used for matching.
 *
 * @author Hong Anh
 * @since 9.2
 */

public class SpecificColumnPatternFilter extends MultiTreePatternFilter {
    private int index = 0;

    /**
     * Creates a new filter with the specified column index.
     * @param indexColumnFilter the index of the column to apply the filter on
     */

    public SpecificColumnPatternFilter(int indexColumnFilter) {
        this.index = indexColumnFilter;
    }

    /**
     * Returns the index of the column used for filtering.
     * @return the column index specified during construction
     */
    @Override
    protected int getIndexColumnFilter() {
        return this.index;
    }
}
