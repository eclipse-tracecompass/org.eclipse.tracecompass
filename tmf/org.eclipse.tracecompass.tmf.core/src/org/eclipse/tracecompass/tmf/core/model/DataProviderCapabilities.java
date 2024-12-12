/*******************************************************************************
 * Copyright (c) 2024 Ericsson
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
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;

/**
 * A capabilities instance return all false.
 *
 * @since 9.5
 * @author Bernd Hufmann
 */
public class DataProviderCapabilities implements IDataProviderCapabilities {

    /** The NullCapabilities instance */
    public static final IDataProviderCapabilities NULL_INSTANCE = new DataProviderCapabilities.Builder().build();

    private final boolean canCreate;
    private final boolean canDelete;

    /**
     * Constructor
     *
     * @param builder
     *            a builder instance
     */
    public DataProviderCapabilities(Builder builder) {
        canCreate = builder.canCreate;
        canDelete = builder.canDelete;
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

        /**
         * Sets canCreate flag
         *
         * @param canCreate
         *            true if data provider can create a derived data provider
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
         *            true if data provider can create a derived data provider
         * @return the builder instance.
         */
        public Builder setCanDelete(boolean canDelete) {
            this.canDelete = canDelete;
            return this;
        }

        /**
         * The method to construct an instance of
         * {@link IDataProviderDescriptor}
         *
         * @return a {@link IDataProviderDescriptor} instance
         */

        public IDataProviderCapabilities build() {
            return new DataProviderCapabilities(this);
        }
    }
}
