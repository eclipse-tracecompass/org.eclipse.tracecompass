/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.tmf.core.model;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderCapabilities;

/**
 * An instance of data provider capabilities, indicating what capabilities a
 * data provider has.
 *
 * @since 9.6
 * @author Bernd Hufmann
 */
public class DataProviderCapabilities implements IDataProviderCapabilities {

    /** The NullCapabilities instance */
    public static final IDataProviderCapabilities NULL_INSTANCE = new DataProviderCapabilities.Builder().build();

    private final boolean canCreate;
    private final boolean canDelete;
    private final boolean selectionRange;

    /**
     * Constructor
     *
     * @param builder
     *            a builder instance
     */
    public DataProviderCapabilities(Builder builder) {
        canCreate = builder.canCreate;
        canDelete = builder.canDelete;
        selectionRange = builder.selectionRange;
    }

    @Override
    public boolean canCreate() {
        return canCreate;
    }

    @Override
    public boolean canDelete() {
        return canDelete;
    }

    @Override
    public boolean selectionRange() {
        return selectionRange;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[")
                .append("canCreate=").append(canCreate())
                .append(", canDelete=").append(canDelete())
                .append("]").toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(canCreate, canDelete);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DataProviderCapabilities)) {
            return false;
        }
        DataProviderCapabilities other = (DataProviderCapabilities) obj;
        return canCreate == other.canCreate && canDelete == other.canDelete;
    }

    /**
     * Builder class to build a IDataProviderCapabilities instance
     */
    public static class Builder {
        private boolean canCreate = false;
        private boolean canDelete = false;
        private boolean selectionRange = false;

        /**
         * Sets canCreate flag
         *
         * @param canCreate
         *            {@code true} if this data provider can create a derived
         *            data provider, else {@code false}
         * @return the builder instance.
         */
        public Builder setCanCreate(boolean canCreate) {
            this.canCreate = canCreate;
            return this;
        }

        /**
         * Sets canDelete flag
         *
         * @param canDelete
         *            {@code true} if this data provider can be deleted, else
         *            {@code false}
         * @return the builder instance.
         */
        public Builder setCanDelete(boolean canDelete) {
            this.canDelete = canDelete;
            return this;
        }

        /**
         * Sets selectionRange flag
         *
         * @param selectionRange
         *            {@code true} if this data provider uses the selection
         *            range, else {@code false}
         * @return the builder instance.
         * @since 10.2
         */
        public Builder setSelectionRange(boolean selectionRange) {
            this.selectionRange = selectionRange;
            return this;
        }

        /**
         * The method to construct an instance of
         * {@link IDataProviderCapabilities}
         *
         * @return a {@link IDataProviderCapabilities} instance
         */
        public IDataProviderCapabilities build() {
            return new DataProviderCapabilities(this);
        }
    }
}
