/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.tmf.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Implementation of {@link ITmfConfigurationSourceType} interface. It provides
 * a builder class to create instances of that interface.
 *
 * @author Bernd Hufmann
 * @since 9.2
 */
public class TmfConfigurationSourceType implements ITmfConfigurationSourceType {

    private final String fId;
    private final String fName;
    private final String fDescription;
    private final List<ITmfConfigParamDescriptor> fParamDescriptors;

    /**
     * Constructor
     *
     * @param bulider
     *            the builder object to create the descriptor
     */
    private TmfConfigurationSourceType(Builder builder) {
        fId = builder.fId;
        fName = builder.fName;
        fDescription = builder.fDescription;
        fParamDescriptors = builder.fDescriptors;
    }

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public String getId() {
        return fId;
    }

    @Override
    public String getDescription() {
        return fDescription;
    }

    @Override
    public List<ITmfConfigParamDescriptor> getConfigParamDescriptors() {
        return fParamDescriptors;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[")
                .append("fName=").append(getName())
                .append(", fDescription=").append(getDescription())
                .append(", fId=").append(getId())
                .append(", fKeys=").append(getConfigParamDescriptors())
                .append("]").toString();
    }

    @Override
    public boolean equals(@Nullable Object arg0) {
        if (!(arg0 instanceof TmfConfigurationSourceType)) {
            return false;
        }
        TmfConfigurationSourceType other = (TmfConfigurationSourceType) arg0;
        return Objects.equals(fName, other.fName) && Objects.equals(fId, other.fId) && Objects.equals(fDescription, other.fDescription)
                && Objects.equals(fParamDescriptors, other.fParamDescriptors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fName, fId, fParamDescriptors, fDescription);
    }

    /**
     * A builder class to build instances implementing interface
     * {@link ITmfConfigurationSourceType}
     */
    public static class Builder {
        private String fId = ""; //$NON-NLS-1$
        private String fName = ""; //$NON-NLS-1$
        private String fDescription = ""; //$NON-NLS-1$
        private List<ITmfConfigParamDescriptor> fDescriptors = new ArrayList<>();

        /**
         * Constructor
         */
        public Builder() {
            // Empty constructor
        }

        /**
         * Sets the ID of the configuration source type
         *
         * @param id
         *            the ID of the data provider
         * @return the builder instance.
         */
        public Builder setId(String id) {
            fId = id;
            return this;
        }

        /**
         * Sets the name of the configuration source type
         *
         * @param name
         *            the name to set
         * @return the builder instance.
         */
        public Builder setName(String name) {
            fName = name;
            return this;
        }

        /**
         * Sets the description of the configuration source type
         *
         * @param description
         *            the description text to set
         * @return the builder instance.
         */
        public Builder setDescription(String description) {
            fDescription = description;
            return this;
        }

        /**
         * Sets the configuration parameter descriptors of the configuration
         * source type
         *
         * @param descriptors
         *            the query parameter keys to set
         * @return the builder instance.
         */
        public Builder setConfigParamDescriptors(List<ITmfConfigParamDescriptor> descriptors) {
            fDescriptors = descriptors;
            return this;
        }

        /**
         * The method to construct an instance of
         * {@link ITmfConfigurationSourceType}
         *
         * @return a {@link ITmfConfigurationSourceType} instance
         */
        public ITmfConfigurationSourceType build() {
            if (fId.isBlank()) {
                throw new IllegalStateException("Configuration source type ID not set"); //$NON-NLS-1$
            }

            if (fName.isBlank()) {
                throw new IllegalStateException("Configuration source type name not set"); //$NON-NLS-1$
            }
            return new TmfConfigurationSourceType(this);
        }
    }
}
