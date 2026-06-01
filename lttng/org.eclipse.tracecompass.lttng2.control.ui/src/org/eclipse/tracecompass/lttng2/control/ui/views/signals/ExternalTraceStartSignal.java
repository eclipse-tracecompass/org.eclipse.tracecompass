/*******************************************************************************
 * Copyright (c) 2026 Renesas Electronics Corp.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Tuan Can - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.control.ui.views.signals;

import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;

/**
 * Signal notify that the External trace have been started
 * @since 1.5
 */
public class ExternalTraceStartSignal extends TmfSignal{

    private final String sessionName;

    /**
     * Constructor
     * @param source input source
     * @param sessionName external trace
     */
    public ExternalTraceStartSignal(Object source, String sessionName) {
        super(source);
        this.sessionName = sessionName;
    }

    /**
     * Get the name of the external trace
     * @return External trace name
     */
    public String getSessionName() {
        return sessionName;
    }
}
