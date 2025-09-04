/**********************************************************************
 * Copyright (c) 2018, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.core.model.table;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.TableColumnDescriptor;
import org.eclipse.tracecompass.tmf.core.model.ITableColumnDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;

/**
 * This interface represents a virtual table data provider. It returns a
 * response that will be used by viewers. Response encapsulates a status and a
 * virtual table model
 *
 * @author Yonni Chen
 * @param <M>
 *            Unused model that represents the entries returned by fetchTree
 * @param <L>
 *            Model that represents a line in a table, returned by fetchLines
 * @since 4.0
 */
public interface ITmfVirtualTableDataProvider<M extends ITmfTreeDataModel, L extends IVirtualTableLine> extends ITmfTreeDataProvider<M> {

    /**
     * @deprecated Use {@link #fetchColumns} instead
     */
    @Deprecated
    @Override
    default TmfModelResponse<TmfTreeModel<M>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        throw new UnsupportedOperationException();
    }

    /**
     * This method computes the column descriptors of the virtual table. Then, it returns a {@link TmfModelResponse} that contains the
     * list of columns.
     *
     * @param fetchParameters
     *            Query parameters
     * @param monitor
     *            A ProgressMonitor to cancel task
     * @return A {@link TmfModelResponse} instance that encapsulates a
     *         list of {@link ITableColumnDescriptor}
     */
    default TmfModelResponse<List<ITableColumnDescriptor>> fetchColumns(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        TmfModelResponse<TmfTreeModel<M>> response = fetchTree(fetchParameters, monitor);
        TmfTreeModel<M> model = response.getModel();
        List<ITableColumnDescriptor> columns = model == null ? Collections.emptyList() :
            model.getEntries().stream().map(e -> new TableColumnDescriptor.Builder()
                    .setId(e.getId())
                    .setText(e.getName())
                    .setTooltip(e.getLabels().size() > 1 ? e.getLabels().get(1) : "") //$NON-NLS-1$
                    .setDataType(e.getDataType()).build())
            .collect(Collectors.toList());
        return new TmfModelResponse<>(columns, response.getStatus(), response.getStatusMessage());
    }

    /**
     * This method computes a virtual table model. Then, it returns a
     * {@link TmfModelResponse} that contains the model.
     *
     * @param fetchParameters
     *            Query parameters that contains a list of desired columns, a starting
     *            index and a number of requested lines
     * @param monitor
     *            A ProgressMonitor to cancel task
     *
     * @return A {@link TmfModelResponse} instance that encapsulate an
     *         {@link ITmfVirtualTableModel}
     */
    TmfModelResponse<ITmfVirtualTableModel<L>> fetchLines(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor);
}