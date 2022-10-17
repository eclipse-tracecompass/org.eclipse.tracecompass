/*******************************************************************************
 * Copyright (c) 2017, 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.counters.core.aspects;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.counters.core.CounterType;
import org.eclipse.tracecompass.internal.analysis.counters.core.Messages;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Numerical aspect, useful for graphs. Not public since it is a trivial
 * implementation of {@link ITmfCounterAspect}
 *
 * @author Matthew Khouzam
 * @since 3.1
 */
public abstract class AbstractCounterAspect implements ITmfCounterAspect {
    private final String fFieldName;
    private final String fLabel;
    private final CounterType fType;

    /**
     * Abstract Counter aspect constructor
     *
     * @param fieldName
     *            The name of the counter field in an event
     * @param label
     *            The label to display in "help"
     */
    public AbstractCounterAspect(String fieldName, String label) {
        fFieldName = fieldName;
        fLabel = label;
        fType = CounterType.LONG;
    }

    /**
     * Abstract Counter aspect constructor with type
     *
     * @param fieldName
     *            The name of the counter field in an event
     * @param label
     *            The label to display in "help"
     * @param type
     *            The type of the counter
     * @since 2.1
     */
    public AbstractCounterAspect(String fieldName, String label, CounterType type) {
        fFieldName = fieldName;
        fLabel = label;
        fType = type;
    }

    @Override
    public @NonNull String getName() {
        return fLabel;
    }

    @Override
    public @NonNull String getHelpText() {
        return Messages.CounterAspect_HelpPrefix + ' ' + getName();
    }

    /**
     * Resolve the value of the counter recorded in this event. The returned
     * class type depends on which type was defined for this counter aspect
     *
     * @param event
     *            The event to process
     * @return The counter value based on its type, either {@link Double} or
     *         {@link Long}; or {@code null} if event is missing the counter
     *         field.
     * @since 2.1
     */
    @Override
    public @Nullable Number resolve(@NonNull ITmfEvent event) {
        switch (fType) {
        case DOUBLE:
            return event.getContent().getFieldValue(Double.class, fFieldName);
        case LONG:
        default:
            return event.getContent().getFieldValue(Long.class, fFieldName);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ' ' + fFieldName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fFieldName, fLabel);
    }

    /**
     * {@inheritDoc}
     *
     * This is a conservative equals. It only works on very identical aspects.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractCounterAspect other = (AbstractCounterAspect) obj;
        return Objects.equals(fFieldName, other.fFieldName) && Objects.equals(fLabel, other.fLabel);
    }

    /**
     * Gets the type of this counter
     *
     * @return the type of this counter
     * @since 2.1
     */
    public CounterType getType() {
        return fType;
    }
}
