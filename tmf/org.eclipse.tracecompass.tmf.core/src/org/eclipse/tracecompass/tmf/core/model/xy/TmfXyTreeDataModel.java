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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;

/**
 * Implementation of {@link ITmfXyEntryModel}.
 *
 * @author Bernd Hufmann
 * @since 8.3
 */
public class TmfXyTreeDataModel extends TmfTreeDataModel implements ITmfXyEntryModel {

    private final boolean fIsDefault;

    /**
     * Constructor
     *
     * @param id
     *            The id of the model
     * @param parentId
     *            The parent id of this model. If it has none, give
     *            <code>-1</code>.
     * @param name
     *            The name of this model
     */
    public TmfXyTreeDataModel(long id, long parentId, String name) {
        this(id, parentId, Collections.singletonList(name), true, null);
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
     */
    public TmfXyTreeDataModel(long id, long parentId, List<String> labels) {
        this(id, parentId, labels, true, null);
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
     */
    public TmfXyTreeDataModel(long id, long parentId, List<String> labels, boolean hasRowModel, @Nullable OutputElementStyle style) {
        this(id, parentId, labels, hasRowModel, style, false);
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
     * @param isDefault
     *            hint if this entry should be shown by default or not. For
     *            example if true, clients can decide to show that entry with
     *            data when opening the view.
     */
    public TmfXyTreeDataModel(long id, long parentId, List<String> labels, boolean hasRowModel, @Nullable OutputElementStyle style, boolean isDefault) {
        super(id, parentId, labels, hasRowModel, style);
        fIsDefault = isDefault;
    }

    @Override
    public boolean isDefault() {
        return fIsDefault;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (obj == null) {
            return false;
        }
        TmfXyTreeDataModel other = (TmfXyTreeDataModel) obj;
        return fIsDefault == other.fIsDefault;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fIsDefault);
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("<");
        builder.append(super.toString())
                .append(" isDefault=")
                .append(fIsDefault)
                .append(">");
        return builder.toString();
    }
}
