/**********************************************************************
 * Copyright (c) 2020, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.tmf.core.model;

import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;

/**
 * Interface to implement to describe a table column.
 *
 * @author Bernd Hufmann
 * @since 6.1
 *
 */
public interface ITableColumnDescriptor {

    /**
     * Gets the id of the column. If the column id is not needed, the default -1
     * is returned.
     *
     * @return the id of the column, or -1 if unused
     * @since 10.2
     */
    default long getId() {
        return -1L;
    }

    /**
     * Gets the header text of the column.
     *
     * @return the text of the header
     */
    String getText();

    /**
     * Gets the header tooltip text of the column.
     *
     * @return the tooltip text of the column
     */
    String getTooltip();

    /**
     * Gets the data type of the column
     *
     * @return {@link DataType}.
     * @since 9.0
     */
    default DataType getDataType() {
        return DataType.STRING;
    }

    /**
     * Returns {@code true} if the column should be hidden by default
     *
     * @return {@code true} if the column should be hidden by default
     * @since 10.2
     */
    default boolean isHiddenByDefault() {
        return false;
    }
}
