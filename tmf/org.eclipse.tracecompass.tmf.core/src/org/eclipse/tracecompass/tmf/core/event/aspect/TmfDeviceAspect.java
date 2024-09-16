/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.event.aspect;

import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;

/**
 * Device aspect, a category that could be an ASIC, CPU, DSP, GPU. Basically any
 * hardware context. Typically an event will only resolve by one device aspect.
 *
 * @author Matthew Khouzam
 * @since 5.2
 */
public abstract class TmfDeviceAspect implements ITmfEventAspect<Integer> {

    /**
     * Get the machine language category for a given aspect. It is a unique
     * string for a given type. Recommended values are "cpu", "gpu", "fpga",
     * "dsp", "net".
     *
     * @return the machine readable (non-externalized) category of the device.
     */
    public abstract String getDeviceType();

    @Override
    public DataType getDataType() {
        return DataType.NUMBER;
    }
}
