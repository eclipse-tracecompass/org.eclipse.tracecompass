/*******************************************************************************
 * Copyright (c) 2019, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.dataprovider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;

/**
 * Data Provider description, used to list the available providers for a trace
 * without triggering the analysis or creating the providers. Supplies
 * information such as the extension point ID, type of provider and help text.
 *
 * @author Loic Prieur-Drevon
 * @author Bernd Hufmann
 * @since 5.0
 */
@NonNullByDefault
public interface IDataProviderDescriptor {

    /**
     * The type of the data provider. The purpose of the type to indicate
     * to the clients the type of viewer to visualize or whether they can
     * share a common x-axis (e.g. time).
     *
     * The following types share common x-axis/time axis:
     *
     * {@link #TREE_TIME_XY}
     * {@link #TIME_GRAPH}
     *
     * @author Loic Prieur-Drevon
     * @author Bernd Hufmann
     */
    public enum ProviderType {
        /**
         * A provider for a table data structure implemented as virtual table.
         */
        TABLE,
        /**
         * A provider for a tree, whose entries have XY series. The x-series is time.
         */
        TREE_TIME_XY,
        /**
         * A provider for a Time Graph model, which has entries with a start and end
         * time, each entry has a series of states, arrows link from one series to
         * another
         */
        TIME_GRAPH,
        /**
         * A provider for a data tree, which has entries (rows) and columns.
         * @since 6.1
         */
        DATA_TREE,
        /**
         * A provider with no data. Can be used for grouping purposes and/or as data provider configurator.
         * @since 9.5
         */
        NONE
    }

    /**
     * Gets the name of the data provide
     *
     * @return the name
     */
    String getName();

    /**
     * Getter for this data provider's ID.
     *
     * @return the ID for this data provider.
     */
    String getId();

    /**
     * Getter for this data provider's type
     *
     * @return this data provider's type
     */
    ProviderType getType();

    /**
     * Getter for the description of this data provider.
     *
     * @return a short description of this data provider.
     */
    String getDescription();

    /**
     * Gets the parent data provider ID for grouping purposes.
     *
     * @return parent ID or null if not grouped or derived data provider
     * @since 9.5
     */
    default @Nullable String getParentId() {
        return null;
    }

    /**
     * Gets the input configuration used to create this data provider.
     *
     * @return the {@link ITmfConfiguration} configuration use to create this
     * data provider, or null if not applicable
     * @since 9.5
     */
    default @Nullable ITmfConfiguration getConfiguration() {
        return null;
    }

    /**
     * @return The data provider capabilities instance
     * @since 9.6
     */
    default IDataProviderCapabilities getCapabilities() {
        return DataProviderCapabilities.NULL_INSTANCE;
    }
}
