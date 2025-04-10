/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.provisional.tmf.core.dataprovider;

/**
 * Data model for the data provider configuration data. This class is used to
 * hold the data that is returned by the data provider configuration.
 *
 * @author Kaveh Shahedi
 */
public class TmfDataProviderConfigurationDataModel {

    private Object fContent;
    private String fContentType;
    private String fContentName;

    /**
     * Constructor
     *
     * @param content
     *            The content of the data model
     * @param contentType
     *            The type of the content
     * @param contentName
     *            The name of the content
     */
    public TmfDataProviderConfigurationDataModel(Object content, String contentType, String contentName) {
        fContent = content;
        fContentType = contentType;
        fContentName = contentName;
    }

    /**
     * Get the content of the data model.
     * The content type is Object as it can be anything (e.g. image, report, etc.)
     *
     * @return The content of the data model
     */
    public Object getContent() {
        return fContent;
    }

    /**
     * Get the type of the content.
     * This is used to identify the type of content that is returned by,
     * for instance, MIME type (e.g., application/octet-stream for binary data).
     *
     * @return The type of the content
     */
    public String getContentType() {
        return fContentType;
    }

    /**
     * Get the name of the content.
     * This is used to identify the name the content if required,
     * as it could be saved to a file.
     *
     * @return The name of the content
     */
    public String getContentName() {
        return fContentName;
    }
}
