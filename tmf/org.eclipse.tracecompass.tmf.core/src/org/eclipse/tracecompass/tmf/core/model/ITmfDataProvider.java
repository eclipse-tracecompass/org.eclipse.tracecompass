/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.tmf.core.model;

/**
 * Base interface that each data provider has to implement.
 *
 * Each data provider needs to provide at least one fetch method, that is
 * typical for this data provider type and which returns @link TmfModelResponse}
 * with a defined serializable model.
 *
 * <p>
 * Example interface:
 * <pre>{@code
 * public ICustomDataProvider implements ITmfDataProvider {
 *   TmfModelResponse<CustomModel> fetchCustomData(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor);
 * }
 * }</pre>
 * </p>
 * Example implementation:
 * <pre>{@code
 *  public class CustomModel {
 *    private final String fValue = value;
 *    public CustomModel(String value) {
 *      fValue = value;
 *    }
 *    String getValue() {
 *      return fValue;
 *    }
 *  }
 *
 *  public class CustomDataProvider implements ICustomDataProvider {
 *    // ITmfDataProvider
 *    public String getId() {
 *      return "customId";
 *    }
 *    public void dispose() {}
 *
 *    // ICustomDataProvider
 *    TmfModelResponse<CustomModel> fetchCustomData(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
 *        CustomModel model = new CustomModel("my data");
 *        return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
 *    }
 *  }
 * }</pre>
 *
 *
 * @since 9.7
 * @author Matthew Khouzam
 * @auther Bernd Hufmann
 *
 */
public interface ITmfDataProvider {
    /**
     * This method return the extension point ID of this provider
     *
     * @return The ID
     */
    String getId();

    /**
     * Dispose of the provider to avoid resource leakage.
     */
    public default void dispose() {
        // Do nothing for now
    }
}
