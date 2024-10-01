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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Implementation of {@link ITmfConfiguration} interface. It provides a builder
 * class to create instances of that interface.
 *
 * @author Bernd Hufmann
 * @since 9.2
 */
public class TmfConfiguration implements ITmfConfiguration {

    private final String fId;
    private final String fName;
    private final String fDescription;
    private final String fSourceTypeId;
    private final Map<String, Object> fParameters;
    private final @Nullable String fJsonParameters;

    /**
     * Constructor
     *
     * @param builder
     *            the builder object to create the descriptor
     */
    private TmfConfiguration(Builder builder) {
        fId = Objects.requireNonNull(builder.fId);
        fName = builder.fName;
        fDescription = builder.fDescription;
        fSourceTypeId = Objects.requireNonNull(builder.fSourceTypeId);
        fParameters = builder.fParameters;
        fJsonParameters = builder.fJsonParameters;
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
    public String getSourceTypeId() {
        return fSourceTypeId;
    }

    @Override
    public String getDescription() {
        return fDescription;
    }

    @Override
    public Map<String, Object> getParameters() {
        return fParameters;
    }

    @Override
    public String getJsonParameters() {
        String parameters = fJsonParameters;
        if (parameters == null) {
            return "{}"; //$NON-NLS-1$
        }
        return parameters;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
            .append("[")
            .append("fName=").append(getName())
            .append(", fDescription=").append(getDescription())
            .append(", fType=").append(getSourceTypeId())
            .append(", fId=").append(getId())
            .append(", fParameters=").append(getParameters())
            .append(", fJsonParameters=").append(getJsonParameters())
            .append("]").toString();
    }

    @Override
    public boolean equals(@Nullable Object arg0) {
        if (!(arg0 instanceof TmfConfiguration)) {
            return false;
        }
        TmfConfiguration other = (TmfConfiguration) arg0;
        return Objects.equals(fName, other.fName) && Objects.equals(fId, other.fId)
                && Objects.equals(fSourceTypeId, other.fSourceTypeId) && Objects.equals(fDescription, other.fDescription)
                && Objects.equals(fParameters, other.fParameters) && Objects.equals(fJsonParameters, other.fJsonParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fName, fId, fSourceTypeId, fDescription, fParameters, fJsonParameters);
    }

    /**
     * A builder class to build instances implementing interface
     * {@link ITmfConfiguration}
     */
    public static class Builder {
        private String fId = ""; //$NON-NLS-1$
        private String fName = ""; //$NON-NLS-1$
        private String fDescription = ""; //$NON-NLS-1$
        private String fSourceTypeId = ""; //$NON-NLS-1$
        private Map<String, Object> fParameters = new HashMap<>();
        private String fJsonParameters = ""; //$NON-NLS-1$

        /**
         * Constructor
         */
        public Builder() {
            // Empty constructor
        }

        /**
         * Sets the ID of the configuration instance.
         *
         * @param id
         *            the ID to set
         * @return the builder instance.
         */
        public Builder setId(String id) {
            fId = id;
            return this;
        }

        /**
         * Sets the name of the configuration instance.
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
         * Sets the description of the configuration instance.
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
         * Sets the ID of the configuration source type {@link ITmfConfigurationSourceType}.
         *
         * @param sourceTypeId
         *            the ID of configuration source type.
         * @return the builder instance.
         */
        public Builder setSourceTypeId(String sourceTypeId) {
            fSourceTypeId = sourceTypeId;
            return this;
        }

        /**
         * Sets the optional parameters of the {@link ITmfConfiguration}
         * instance
         *
         * @param parameters
         *            the optional parameters of the {@link ITmfConfiguration}
         *            instance
         * @return the builder instance
         */
        public Builder setParameters(Map<String, Object> parameters) {
            fParameters = parameters;
            return this;
        }

        /**
         * Sets the optional JSON parameters of the {@link ITmfConfiguration}
         * instance
         *
         * @param jsonParameters
         *            the optional JSON parameters of the {@link ITmfConfiguration}
         *            instance
         * @return the builder instance
         * @since 9.5
         */
        public Builder setJsonParameters(String jsonParameters) {
            fJsonParameters = jsonParameters;
            return this;
        }

        /**
         * The method to construct an instance of {@link ITmfConfiguration}
         *
         * @return a {@link ITmfConfiguration} instance
         */
        public ITmfConfiguration build() {
            String id = fId;
            if (id.isBlank()) {
                throw new IllegalStateException("Configuration ID not set"); //$NON-NLS-1$
            }
            return new TmfConfiguration(this);
        }
    }
}
