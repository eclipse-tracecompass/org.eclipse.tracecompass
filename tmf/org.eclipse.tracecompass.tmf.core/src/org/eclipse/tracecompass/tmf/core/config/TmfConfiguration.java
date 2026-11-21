/*******************************************************************************
 * Copyright (c) 2023, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.tmf.core.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
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

    /**
     * String to use for unknown name, description or typeId
     * @since 9.5
     */
    public static final String UNKNOWN = "---unknown---"; //$NON-NLS-1$

    /**
     * The json file extension
     *
     * @since 9.5
     * @deprecated use {@link AbstractTmfDataProviderConfigurator#JSON_EXTENSION} instead
     */
    @Deprecated
    public static final String JSON_EXTENSION = "json"; //$NON-NLS-1$

    @Expose
    @SerializedName(value = "id")
    @Nullable
    private String fId;
    @Expose
    @SerializedName(value = "name")
    @Nullable
    private final String fName;
    @Expose
    @SerializedName(value = "description")
    @Nullable
    private final String fDescription;
    @Expose
    @SerializedName(value = "sourceTypeId")
    @Nullable
    private String fSourceTypeId;
    @Expose
    @SerializedName(value = "parameters")
    @Nullable
    private final Map<String, Object> fParameters;

    /**
     * Default constructor. Needed for deserialization from file.
     *
     * @since 9.5
     */
    public TmfConfiguration() {
        fId = UNKNOWN;
        fName = UNKNOWN;
        fDescription = UNKNOWN;
        fSourceTypeId = UNKNOWN;
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

    /**
     * Gets the name of the configuration
     *
     * @return the name of the configuration
     */
    @Override
    public String getName() {
        String name = fName;
        if (name == null) {
            return UNKNOWN;
        }
        return name;
    }

    /**
     * Gets the id of the configuration
     *
     * @return the id of the configuration
     */
    @Override
    public String getId() {
        String id = fId;
        if (id == null || id.isBlank()) {
            id = toUuidString(getName());
            fId = id;
        }
        return id;
    }

    /**
     * Gets the sourceTypeId of the configuration
     *
     * @return the sourceTypeId
     */
    @Override
    public String getSourceTypeId() {
        String sourceTypeId = fSourceTypeId;
        if (sourceTypeId == null) {
            return UNKNOWN;
        }
        return sourceTypeId;
    }

    /**
     * Gets the description of the configuration
     *
     * @return the description of the configuration
     */
    @Override
    public String getDescription() {
        String desc = fDescription;
        if (desc == null) {
            return UNKNOWN;
        }
        return desc;
    }

    /**
     * Get Parameter map
     *
     * @return the parameters map
     */
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = fParameters;
        if (parameters == null) {
            return new HashMap<>();
        }
        return parameters;
    }

    private static String toUuidString(String name) {
        return UUID.nameUUIDFromBytes(Objects.requireNonNull((name).getBytes(Charset.defaultCharset()))).toString();
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
                && Objects.equals(fSourceTypeId, other.fSourceTypeId) && Objects.equals(fDescription, other.fDescription) && Objects.equals(fParameters, other.fParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fName, fId, fSourceTypeId, fDescription, fParameters);
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
         * The method to construct an instance of {@link ITmfConfiguration}
         *
         * @return a {@link ITmfConfiguration} instance
         */
        public ITmfConfiguration build() {
            if (fId.isBlank()) {
                fId = toUuidString(fName.isBlank() ? UNKNOWN : fName);
            }
            return new TmfConfiguration(this);
        }
    }

    /**
     * Converts JSON parameters from file to {@link ITmfConfiguration}
     *
     * @param jsonFile
     *            the parameters as JSON string description and sourceTypeId
     * @return {@link ITmfConfiguration}
     * @throws TmfConfigurationException
     *             if an error occurred
     * @since 9.5
     */
    @SuppressWarnings("null")
    public static ITmfConfiguration fromJsonFile(File jsonFile) throws TmfConfigurationException {
        try (Reader reader = new FileReader(jsonFile)) {
            return new Gson().fromJson(reader, TmfConfiguration.class);
        } catch (IOException | JsonParseException e) {
            Activator.logError(e.getMessage(), e);
            throw new TmfConfigurationException("Can't parse JSON file. " + jsonFile.getName(), e); //$NON-NLS-1$
        }
    }

    /**
     * Serialize {@link ITmfConfiguration} to JSON file with name configId.json
     *
     * @param configuration
     *            the configuration to serialize
     * @param rootPath
     *            the root path to store the configuration
     * @throws TmfConfigurationException
     *             if an error occurs
     * @since 9.5
     * @deprecated use
     *             {@link AbstractTmfDataProviderConfigurator#writeConfiguration(ITmfConfiguration, IPath)}
     *             instead
     */
    @Deprecated
    public static void writeConfiguration(ITmfConfiguration configuration, IPath rootPath) throws TmfConfigurationException {
        IPath supplPath = rootPath;
        File folder = supplPath.toFile();
        if (!folder.exists()) {
            folder.mkdir();
        }
        supplPath = supplPath.addTrailingSeparator().append(configuration.getId()).addFileExtension(JSON_EXTENSION);
        File file = supplPath.toFile();
        try (Writer writer = new FileWriter(file)) {
            writer.append(new Gson().toJson(configuration));
        } catch (IOException | JsonParseException e) {
            Activator.logError(e.getMessage(), e);
            throw new TmfConfigurationException("Error writing configuration.", e); //$NON-NLS-1$
        }
    }

}
