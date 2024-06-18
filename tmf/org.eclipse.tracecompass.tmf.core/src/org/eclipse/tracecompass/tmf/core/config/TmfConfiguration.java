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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Implementation of {@link ITmfConfiguration} interface. It provides a builder
 * class to create instances of that interface.
 *
 * @author Bernd Hufmann
 * @since 9.2
 */
public class TmfConfiguration implements ITmfConfiguration {

    @Expose
    @SerializedName(value = "id")
    private final String fId;
    @Expose
    @SerializedName(value = "name")
    private final String fName;
    @Expose
    @SerializedName(value = "description")
    private final String fDescription;
    @Expose
    @SerializedName(value = "sourceTypeId")
    private final String fSourceTypeId;
    @Expose
    @SerializedName(value = "parameters")
    private final Map<String, String> fParameters;

    /**
     * Default constructor. Needed for deserialization from file.
     * @since 9.4
     */
    public TmfConfiguration() {
        fId = ""; //$NON-NLS-1$
        fName = ""; //$NON-NLS-1$
        fDescription = ""; //$NON-NLS-1$
        fSourceTypeId = ""; //$NON-NLS-1$
        fParameters = new HashMap<>();
    }

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
    public Map<String, String> getParameters() {
        return fParameters;
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
                && Objects.equals(fParameters, other.fParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fName, fId, fSourceTypeId, fDescription, fParameters);
    }

    static String toUuidString(String name, Map<String, String> params) {
        StringBuilder paramBuilder = new StringBuilder();
        for (Entry<String, String> entry : params.entrySet()) {
            paramBuilder.append(entry.getKey())
            .append("=") //$NON-NLS-1$
            .append(entry.getValue());
        }
        String inputStr = new StringBuilder()
                .append("fName=").append(name) //$NON-NLS-1$
                .append("fParameters=").append(paramBuilder.toString()).toString(); //$NON-NLS-1$
        return UUID.nameUUIDFromBytes(Objects.requireNonNull(inputStr.getBytes(Charset.defaultCharset()))).toString();
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
        private Map<String, String> fParameters = new HashMap<>();

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
        public Builder setParameters(Map<String, String> parameters) {
            fParameters = parameters;
            return this;
        }

        /**
         * The method to construct an instance of {@link ITmfConfiguration}
         *
         * @return a {@link ITmfConfiguration} instance
         */
        public ITmfConfiguration build() {
            String typeId = fSourceTypeId;
            if (typeId.isBlank()) {
                throw new IllegalStateException("Configuration source type ID not set"); //$NON-NLS-1$
            }
            String id = fId;
            if (id.isBlank()) {
                fId = toUuidString(fName, fParameters);
            }
            return new TmfConfiguration(this);
        }
    }
}
