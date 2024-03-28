/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.callstack;

import java.util.Collection;
import java.util.Map;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * An interface that analyses can implement if they can provide a stack of
 * called function for a single event.
 *
 * @author Geneviève Bastien
 * @since 2.5
 */
public interface IEventCallStackProvider {

    /**
     * Get the callstack from an event
     *
     * @param event
     *            The event for which to get the stack
     * @return The callstack for the event, grouped by some domain, where the
     *         first element of each collection is the root.
     */
    Map<String, Collection<Object>> getCallStack(ITmfEvent event);

}
