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

package org.eclipse.tracecompass.tmf.core.model.object;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a generic object with optional navigation parameters.
 * <p>
 * If the object is a partial subdivision of the full object, then the optional
 * next and previous navigation parameter objects can be set, and later returned
 * as query parameters to get the following or preceding subdivision.
 *
 * @since 10.2
 */
public class ObjectModel {

    private final Object fObject;
    private @Nullable Object fNext;
    private @Nullable Object fPrevious;

    /**
     * Constructor
     *
     * @param object
     *            the generic object represented by this model
     */
    public ObjectModel(Object object) {
        fObject = object;
    }

    /**
     * Get the generic object represented by this model
     *
     * @return the object
     */
    public Object getObject() {
        return fObject;
    }

    /**
     * Get the next navigation parameter object
     *
     * @return the next navigation parameter object
     */
    public @Nullable Object getNext() {
        return fNext;
    }

    /**
     * Get the previous navigation parameter object
     *
     * @return the previous navigation parameter object
     */
    public @Nullable Object getPrevious() {
        return fPrevious;
    }

    /**
     * Set the next navigation parameter object
     *
     * @param next
     *            the next navigation parameter object
     */
    public void setNext(Object next) {
        fNext = next;
    }

    /**
     * Set the previous navigation parameter object
     *
     * @param previous
     *            the previous navigation parameter object
     */
    public void setPrevious(Object previous) {
        fPrevious = previous;
    }
}
