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

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Implementation of {@link ITmfConfigParamDescriptor} interface. It provides a
 * builder class to create instances of that interface.
 *
 * @author Bernd Hufmann
 * @since 9.2
 */
public class TmfConfigParamDescriptor implements ITmfConfigParamDescriptor {

    private final String fKeyName;
    private final String fDescription;
    private final String fDataType;
    private final boolean fIsRequired;

    /**
     * Constructor
     *
     * @param bulider
     *            the builder object to create the descriptor
     */
    private TmfConfigParamDescriptor(Builder builder) {
        fKeyName = builder.fKeyName;
        fDescription = builder.fDescription;
        fDataType = builder.fDataType;
        fIsRequired = builder.fIsRequired;
    }

    @Override
    public String getKeyName() {
        return fKeyName;
    }

    @Override
    public String getDataType() {
        return fDataType;
    }

    @Override
    public boolean isRequired() {
        return fIsRequired;
    }

    @Override
    public String getDescription() {
        return fDescription;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[")
                .append("fKeyName=").append(getKeyName())
                .append(", fDataType=").append(getDataType())
                .append(", fIsRequired=").append(isRequired())
                .append(", fDescription=").append(getDescription())
                .append("]").toString();
    }

    @Override
    public boolean equals(@Nullable Object arg0) {
        if (!(arg0 instanceof TmfConfigParamDescriptor)) {
            return false;
        }
        TmfConfigParamDescriptor other = (TmfConfigParamDescriptor) arg0;
        return Objects.equals(fKeyName, other.fKeyName)
                && Objects.equals(fDataType, other.fDataType)
                && Objects.equals(fDescription, other.fDescription)
                && Objects.equals(fIsRequired, other.fIsRequired);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fKeyName, fIsRequired, fDataType, fDescription);
    }

    /**
     * A builder class to build instances implementing interface
     * {@link ITmfConfigParamDescriptor}
     */
    public static class Builder {
        private String fKeyName = ""; //$NON-NLS-1$
        private String fDescription = ""; //$NON-NLS-1$ ;
        private String fDataType = "STRING"; //$NON-NLS-1$
        private boolean fIsRequired = true;

        /**
         * Constructor
         */
        public Builder() {
            // Empty constructor
        }

        /**
         * Sets the data type string of the configuration parameter.
         *
         * @param dataType
         *            the ID to set
         * @return the builder instance.
         */
        public Builder setDataType(String dataType) {
            fDataType = dataType;
            return this;
        }

        /**
         * Sets the unique key name of the configuration parameter.
         *
         * @param keyName
         *            the name to set
         * @return the builder instance.
         */
        public Builder setKeyName(String keyName) {
            fKeyName = keyName;
            return this;
        }

        /**
         * Sets the description of the configuration parameter.
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
         * Sets the isRequired flag of the configuration parameter.
         *
         * @param isRequired
         *            the is required flag.
         * @return the builder instance.
         */
        public Builder setIsRequired(boolean isRequired) {
            fIsRequired = isRequired;
            return this;
        }

        /**
         * The method to construct an instance of {@link ITmfConfiguration}
         *
         * @return a {@link ITmfConfiguration} instance
         */
        public ITmfConfigParamDescriptor build() {
            String keyName = fKeyName;
            if (keyName.isBlank()) {
                throw new IllegalStateException("The key name of the configuration parameter is not set"); //$NON-NLS-1$
            }
            return new TmfConfigParamDescriptor(this);
        }
    }
}
