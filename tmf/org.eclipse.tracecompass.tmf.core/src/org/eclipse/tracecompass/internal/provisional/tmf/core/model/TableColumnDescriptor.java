/**********************************************************************
 * Copyright (c) 2020, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.core.model;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.ITableColumnDescriptor;

import com.google.common.base.Objects;

/**
 * Data table column descriptor implementation.
 *
 * @since 6.1
 */
public class TableColumnDescriptor implements ITableColumnDescriptor {

    private final long fId;
    private final String fText;
    private final String fTooltipText;
    private final DataType fDataType;
    private final boolean fHiddenByDefault;

    /**
     * Constructor
     *
     * @param header
     *            Column header
     */
    private TableColumnDescriptor(Builder builder) {
        fId = builder.fId;
        fText = builder.fText;
        fTooltipText = builder.fTooltipText;
        fDataType = builder.fDataType;
        fHiddenByDefault = builder.fHiddenByDefault;
    }

    @Override
    public long getId() {
        return fId;
    }

    @Override
    public String getText() {
        return fText;
    }

    @Override
    public String getTooltip() {
        return fTooltipText;
    }

    @Override
    public boolean isHiddenByDefault() {
        return fHiddenByDefault;
    }

    @Override
    public DataType getDataType() {
        return fDataType;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ITableColumnDescriptor)) {
            return false;
        }
        ITableColumnDescriptor other = (ITableColumnDescriptor) obj;
        return Objects.equal(fId, other.getId()) &&
                Objects.equal(fText, other.getText()) &&
                Objects.equal(fTooltipText, other.getTooltip()) &&
                Objects.equal(fDataType, other.getDataType()) &&
                Objects.equal(fHiddenByDefault, other.isHiddenByDefault());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fId, fText, fTooltipText, fDataType, fHiddenByDefault);
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        if (fId != -1) {
            builder.append("id=").append(fId).append(" ");
        }
        builder.append("text=").append(fText)
                .append(" tooltip=").append(fTooltipText)
                .append(" dataType=").append(fDataType.toString());
        if (fHiddenByDefault) {
            builder.append(" hiddenByDefault=").append(fHiddenByDefault);
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     *
     * A builder class to build instances implementing interface
     * {@link TableColumnDescriptor}
     *
     * @author Bernd Hufmann
     */
    public static class Builder {
        private long fId = -1;
        private String fText = ""; //$NON-NLS-1$
        private String fTooltipText = ""; //$NON-NLS-1$
        private DataType fDataType = DataType.STRING;
        private boolean fHiddenByDefault = false;

        /**
         * Constructor
         */
        public Builder() {
            // Empty constructor
        }

        /**
         * Sets the id of the header
         *
         * @param id
         *            the header id to set
         * @return this {@link Builder} object
         * @since 10.1
         */
        public Builder setId(long id) {
            fId = id;
            return this;
        }

        /**
         * Sets the text of the header
         *
         * @param text
         *            the header text to set
         * @return this {@link Builder} object
         */
        public Builder setText(String text) {
            fText = text;
            return this;
        }

        /**
         * Sets the tooltip text of the header
         *
         * @param tooltip
         *            the tooltip text to set
         * @return this {@link Builder} object
         */
        public Builder setTooltip(String tooltip) {
            fTooltipText = tooltip;
            return this;
        }

        /**
         * Sets the data type of the column
         *
         * @param dataType
         *            the dataType to set
         * @return this {@link Builder} object
         * @since 9.0
         */
        public Builder setDataType(DataType dataType) {
            fDataType = dataType;
            return this;
        }

        /**
         * Sets whether the column should be hidden by default
         *
         * @param hiddenByDefault
         *            {@code true} if the column should be hidden by default
         * @return this {@link Builder} object
         * @since 10.1
         */
        public Builder setHiddenByDefault(boolean hiddenByDefault) {
            fHiddenByDefault = hiddenByDefault;
            return this;
        }

        /**
         * The method to construct an instance of {@link ITableColumnDescriptor}
         *
         * @return a {@link ITableColumnDescriptor} instance
         */
        public TableColumnDescriptor build() {
            return new TableColumnDescriptor(this);
        }
    }
}
