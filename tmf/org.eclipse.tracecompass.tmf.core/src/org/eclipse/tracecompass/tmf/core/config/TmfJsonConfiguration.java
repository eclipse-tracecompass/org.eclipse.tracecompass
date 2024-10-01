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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Implementation of {@link ITmfConfiguration} interface. It provides a builder
 * class to create instances of that interface.
 *
 * @author Bernd Hufmann
 * @since 9.5
 */
public class TmfJsonConfiguration {

    /**
     * The json file extension
     */
    public static final String JSON_EXTENSION = "json"; //$NON-NLS-1$

    @Expose
    @SerializedName(value = "id")
    @Nullable private String fId;
    @Expose
    @SerializedName(value = "name")
    @Nullable private final String fName;
    @Expose
    @SerializedName(value = "description")
    @Nullable private final String fDescription;
    @Expose
    @SerializedName(value = "sourceTypeId")
    @Nullable private String fSourceTypeId;
    @Expose
    @SerializedName(value = "parameters")
    @Nullable private final JsonElement fParameters;

    /**
     * Default constructor. Needed for deserialization from file.
     * @since 9.5
     */
    public TmfJsonConfiguration() {
        fId = ""; //$NON-NLS-1$
        fName = ""; //$NON-NLS-1$
        fDescription = ""; //$NON-NLS-1$
        fSourceTypeId = ""; //$NON-NLS-1$
        fParameters = JsonNull.INSTANCE;
    }

    /**
     * Constructor
     * @param id
     *          the ID
     * @param name
     *          the name
     * @param description
     *          the description
     * @param sourceTypeId
     *          the sourceTypeId
     * @param jsonString
     *          the parameter JSON string
     */
    public TmfJsonConfiguration(String id, String name, String description, String sourceTypeId, String jsonString) {
        fId = id;
        fName = name;
        fDescription = description;
        fSourceTypeId = sourceTypeId;
        fParameters = new Gson().fromJson(jsonString, JsonElement.class);
    }

    /**
     * Gets the name of the configuration
     *
     * @return the name of the configuration
     */
    public @Nullable String getName() {
        return fName;
    }

    /**
     * Gets the id of the configuration
     *
     * @return the id of the configuration
     */
    public String getId() {
        String id = fId;
        if (id == null || id.isBlank()) {
            id = toUuidString();
            fId = id;
        }
        return id;
    }

    /**
     * sets the sourceTypeId of the configuration
     *
     * @param sourceTypeId
     *            the sourceTypeId of the configuration
     */
    public void setSourceTypeId(String sourceTypeId) {
        fSourceTypeId = sourceTypeId;
    }

    /**
     * Gets the sourceTypeId of the configuration
     *
     * @return the sourceTypeId of the configuration
     */

    public @Nullable String getSourceTypeId() {
        return fSourceTypeId;
    }

    /**
     * Gets the description of the configuration
     *
     * @return the description of the configuration
     */
    public @Nullable String getDescription() {
        return fDescription;
    }

    /**
     * Gets the parameters as JSON string
     *
     * @return the parameters as JSON string
     */
    @SuppressWarnings("null")
    public String getJsonParameters() {
        JsonElement parameters = fParameters;
        if (parameters != null) {
            return parameters.toString();
        }
        return "{}"; //$NON-NLS-1$
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
            .append(", fJsonParameters=").append(getJsonParameters())
            .append("]").toString();
    }

    @Override
    public boolean equals(@Nullable Object arg0) {
        if (!(arg0 instanceof TmfJsonConfiguration)) {
            return false;
        }
        TmfJsonConfiguration other = (TmfJsonConfiguration) arg0;
        return Objects.equals(fName, other.fName) && Objects.equals(fId, other.fId)
                && Objects.equals(fSourceTypeId, other.fSourceTypeId) && Objects.equals(fDescription, other.fDescription) && Objects.equals(fParameters, other.fParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fName, fId, fSourceTypeId, fDescription, fParameters);
    }

    private String toUuidString() {
        String inputStr = new StringBuilder()
                .append("fName=").append(fName) //$NON-NLS-1$
                .append("fParameters=").append(getJsonParameters()).toString(); //$NON-NLS-1$
        return UUID.nameUUIDFromBytes(Objects.requireNonNull(inputStr.getBytes(Charset.defaultCharset()))).toString();
    }

    /**
     * Converts a JSON parameters string to {@link ITmfConfiguration}
     *
     * @param json
     *            the parameters as JSON string
     * @param defaultValues
     *            {@link ITmfConfiguration} with default values for name,
     *            description and sourceTypeId
     * @return {@link ITmfConfiguration}
     * @throws TmfConfigurationException
     *             if an error occurred
     * @since 9.5
     */
    public static ITmfConfiguration fromJsonString(String json, ITmfConfiguration defaultValues) throws TmfConfigurationException {
       try {
           @SuppressWarnings("null")
           TmfJsonConfiguration config = new Gson().fromJson(json, TmfJsonConfiguration.class);
           TmfConfiguration.Builder builder = new TmfConfiguration.Builder();
           String name = config.getName();
           name = name == null ? defaultValues.getName() : name;
           String description = config.getDescription();
           description = description == null ? defaultValues.getDescription() : description;
           return builder.setId(config.getId())
                  .setDescription(description)
                  .setName(name)
                  .setSourceTypeId(defaultValues.getSourceTypeId())
                  .setJsonParameters(json).build();
       } catch (JsonSyntaxException e) {
           Activator.logError(e.getMessage(), e);
           throw new TmfConfigurationException("Can't parse json string. ", e); //$NON-NLS-1$
       }
   }

    /**
     * Converts a JSON parameters from file to {@link ITmfConfiguration}
     *
     * @param jsonFile
     *            the parameters as JSON string
     *            description and sourceTypeId
     * @return {@link ITmfConfiguration}
     * @throws TmfConfigurationException
     *             if an error occurred
     * @since 9.5
     */
    public static ITmfConfiguration fromJsonFile(File jsonFile) throws TmfConfigurationException {
        try (Reader reader = new FileReader(jsonFile)) {
            @SuppressWarnings("null")
            TmfJsonConfiguration config = new Gson().fromJson(reader, TmfJsonConfiguration.class);
            TmfConfiguration.Builder builder = new TmfConfiguration.Builder();

            String name = config.getName();
            name = name == null ? "No Name" : name; //$NON-NLS-1$
            String description = config.getDescription();
            description = description == null ? "" : description; //$NON-NLS-1$
            String sourceTypeId = config.getSourceTypeId();
            if (sourceTypeId == null) {
                throw new TmfConfigurationException("No SourceTypeId in config file " + jsonFile.getName()); //$NON-NLS-1$
            }
            return builder.setId(config.getId())
                    .setDescription(description)
                    .setName(name)
                    .setSourceTypeId(sourceTypeId)
                    .setJsonParameters(config.getJsonParameters()).build();
        } catch (IOException e) {
            Activator.logError(e.getMessage(), e);
            throw new TmfConfigurationException("Can't parse json string. ", e); //$NON-NLS-1$
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
     */
   public static void writeConfiguration(ITmfConfiguration configuration, IPath rootPath) throws TmfConfigurationException {
       IPath supplPath = rootPath;
       File folder = supplPath.toFile();
       if (!folder.exists()) {
           folder.mkdir();
       }
       supplPath = supplPath.addTrailingSeparator().append(configuration.getId()).addFileExtension(JSON_EXTENSION);
       TmfJsonConfiguration jsonConfig = new TmfJsonConfiguration(configuration.getId(), configuration.getName(), configuration.getDescription(), configuration.getSourceTypeId(), configuration.getJsonParameters());
       File file = supplPath.toFile();
       try (Writer writer = new FileWriter(file)) {
           writer.append(new Gson().toJson(jsonConfig));
       } catch (IOException e) {
           throw new TmfConfigurationException("Error writing configuration.", e); //$NON-NLS-1$
       }
   }
}
