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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;

/**
 * Basic implementation of {@link ITmfTreeDataModel}.
 *
 * @author Yonni Chen
 * @since 4.0
 */
public class TmfTreeDataModel implements ITmfTreeDataModel {

    private final long fId;
    private final long fParentId;
    private final List<String> fLabels;
    private final boolean fHasRowModel;
    private final @Nullable OutputElementStyle fStyle;
    private final String fDataType;

    /**
     * Constructor
     *
     * @param id
     *            The id of the model
     * @param parentId
     *            The parent id of this model. If it has none, give <code>-1</code>.
     * @param name
     *            The name of this model
     */
    public TmfTreeDataModel(long id, long parentId, String name) {
        this(id, parentId, Collections.singletonList(name), true, null, null);
    }

    /**
     * Constructor
     *
     * @param id
     *            The id of the model
     * @param parentId
     *            The parent id of this model. If it has none, give <code>-1</code>.
     * @param labels
     *            The labels of this model
     * @since 5.0
     */
    public TmfTreeDataModel(long id, long parentId, List<String> labels) {
        this(id, parentId, labels, true, null, null);
    }

    /**
     * Constructor
     *
     * @param id
     *            The id of the model
     * @param parentId
     *            The parent id of this model. If it has none, give <code>-1</code>.
     * @param labels
     *            The labels of this model
     * @param dataType
     *            The data type of this model
     * @since 9.3
     */
    public TmfTreeDataModel(long id, long parentId, List<String> labels, String dataType) {
        this(id, parentId, labels, true, null, dataType);
    }

    /**
     * Constructor
     *
     * @param id
     *            The id of the model
     * @param parentId
     *            The parent id of this model. If it has none, give <code>-1</code>.
     * @param labels
     *            The labels of this model
     * @param hasRowModel
     *            Whether this entry has data or not
     * @since 9.3
     */
    public TmfTreeDataModel(long id, long parentId, List<String> labels, boolean hasRowModel) {
        this(id, parentId, labels, hasRowModel, null, null);
    }

    /**
     * Constructor
     *
     * @param id
     *            The id of the model
     * @param parentId
     *            The parent id of this model. If it has none, give
     *            <code>-1</code>.
     * @param labels
     *            The labels of this model
     * @param hasRowModel
     *            Whether this entry has data or not
     * @param style
     *            The style of this entry
     * @since 9.3
     */
    public TmfTreeDataModel(long id, long parentId, List<String> labels, boolean hasRowModel, @Nullable OutputElementStyle style) {
        this(id, parentId, labels, hasRowModel, style, null);
    }

    /**
     * Constructor
     *
     * @param id
     *            The id of the model
     * @param parentId
     *            The parent id of this model. If it has none, give
     *            <code>-1</code>.
     * @param labels
     *            The labels of this model
     * @param hasRowModel
     *            Whether this entry has data or not
     * @param style
     *            The style of this entry
     * @param dataType
     *            The data type of this entry
     * @since 9.3
     */
    public TmfTreeDataModel(long id, long parentId, List<String> labels, boolean hasRowModel, @Nullable OutputElementStyle style, @Nullable String dataType) {
        fId = id;
        fParentId = parentId;
        fLabels = labels;
        fHasRowModel = hasRowModel;
        fStyle = style;
        fDataType = dataType == null ? DataType.STRING.name() : dataType;
    }

    @Override
    public long getId() {
        return fId;
    }

    @Override
    public long getParentId() {
        return fParentId;
    }

    @Override
    public String getName() {
        return fLabels.isEmpty() ? "" : fLabels.get(0); //$NON-NLS-1$
    }

    @Override
    public List<String> getLabels() {
        return fLabels;
    }

    @Override
    public boolean hasRowModel() {
        return fHasRowModel;
    }

    @Override
    public @Nullable OutputElementStyle getStyle() {
        return fStyle;
    }

    /**
     * Returns the data type of the model
     *
     * @since 9.3
     */
    @Override
    public String getDataType() {
        return fDataType;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TmfTreeDataModel other = (TmfTreeDataModel) obj;

        if (!Objects.equals(fStyle, other.fStyle)) {
            return false;
        }

        return fId == other.fId
                && fParentId == other.fParentId
                && fLabels.equals(other.fLabels)
                && fHasRowModel == other.fHasRowModel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fId, fParentId, fLabels, fStyle, fHasRowModel);
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "<name=" + fLabels + " id=" + fId + " parentId=" + fParentId + " style=" + fStyle + " hasRowModel=" + fHasRowModel + ">";
    }
}
