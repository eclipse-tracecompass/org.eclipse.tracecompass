/*******************************************************************************
 * Copyright (c) 2017, 2025 Ericsson, Draeger Auriga
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.viewers.tree;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.dialogs.SpecificColumnPatternFilter;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.dialogs.TriStateFilteredCheckboxTree;

/**
 * An abstract tree viewer that supports selection with filtering capabilities.
 *
 * This viewer extends
 *     {@link AbstractSelectTreeViewer2}
 * to allow filtering based on a specific column index.
 *
 * @author Hong Anh
 * @since 9.2
 */
public abstract class AbstractSelectTreeViewer3 extends AbstractSelectTreeViewer2 {
    /**
     * Constructor
     *
     * @param parent
     *            Parent composite
     * @param legendIndex
     *            index of the legend column (-1 if none)
     * @param id
     *            {@link ITmfTreeDataProvider} ID
     * @param indexColumnFilter
     *            the index of the column to apply the filter on
     */
    public AbstractSelectTreeViewer3(Composite parent, int legendIndex, String id, int indexColumnFilter) {
        // Initialize the tree viewer with a filtered checkbox tree and column-based filtering
        super(parent, new TriStateFilteredCheckboxTree(parent,
                SWT.MULTI | SWT.H_SCROLL | SWT.FULL_SELECTION,
                new SpecificColumnPatternFilter(indexColumnFilter), true, false), legendIndex, id);
    }
}
