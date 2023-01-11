/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.callstack.core;

import java.util.Comparator;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.Messages;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;

/**
 * @author Geneviève Bastien
 */
public class FunctionTidAspect implements ISegmentAspect {

    /**
     * A symbol aspect
     */
    public static final ISegmentAspect TID_ASPECT = new FunctionTidAspect();

    @Override
    public String getName() {
        return String.valueOf(Messages.AspectName_Tid);
    }

    @Override
    public String getHelpText() {
        return String.valueOf(Messages.AspectHelpText_Tid);
    }

    @Override
    public @Nullable Comparator<?> getComparator() {
        return null;
    }

    @Override
    public @Nullable Object resolve(ISegment segment) {
        if (segment instanceof ICalledFunction) {
            return ((ICalledFunction) segment).getThreadId();
        }
        return null;
    }
}
