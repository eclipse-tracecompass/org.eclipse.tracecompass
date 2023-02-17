/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore;

import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;

/**
 * A typed data where the {@link DataType} determines what the resolved object
 * represents.
 *
 * Note:
 *  - For the data types representing a number, the resolved object is expected
 *    to be either a Number object or String that can be parsed to a Number object.
 *  - For {@link DataType#TIME_RANGE} the resolved object is expected to be a
 *    string of format "[start,end]", where start/end are string representations
 *    of a timestamp as Long value.
 *  - For {@link DataType#STRING}, the resolved object is expected to be a String
 *
 * @author Bernd Hufmann
 *
 * @param <E>
 *            The type of object as input for resolving
 *
 */
public interface ITypedDataAspect<E> extends IDataAspect<E> {
    /**
     * @return data type hint that is returned by apply() call
     */
    DataType getDataType();
}
