/**********************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.tmf.core.model.tree;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;

/**
 * This interface represents a tree data provider. It returns a computed model
 * that will be used by tree viewers.
 *
 * @author Yonni Chen
 * @param <T>
 *            Tree model extending {@link ITmfTreeDataModel}
 * @since 4.0
 */
public interface ITmfTreeDataProvider<T extends ITmfTreeDataModel> extends ITmfDataProvider{

    /**
     * This methods computes a tree model. Then, it returns a
     * {@link TmfModelResponse} that contains the model. Tree model will be used
     * by tree viewer to show entries as a tree or flat hierarchy
     *
     * @param fetchParameters
     *            A query filter that contains an array of time. Times are used
     *            for requesting data.
     * @param monitor
     *            A ProgressMonitor to cancel task
     * @return A {@link TmfModelResponse} instance
     * @since 5.0
     */
    TmfModelResponse<TmfTreeModel<T>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor);
}


