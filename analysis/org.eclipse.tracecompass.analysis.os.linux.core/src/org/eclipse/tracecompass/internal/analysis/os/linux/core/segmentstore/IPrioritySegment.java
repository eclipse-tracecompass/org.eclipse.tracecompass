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

package org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore;

import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * Interface to be implemented by segments that have a priority to provide. This
 * priority can be used in analyses and outputs to identify segments.
 *
 * This interface is a qualifier interface for segments. A concrete segment type
 * can implement many such qualifier interfaces.
 *
 * @author Hoang Thuan Pham
 */
public interface IPrioritySegment extends ISegment {
    /**
     * Get the priority of this segment
     *
     * @return The priority of this segment
     */
    int getPriority();
}
