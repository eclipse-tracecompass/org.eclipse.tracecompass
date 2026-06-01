/**********************************************************************
 * Copyright (c) 2012, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.tracecompass.internal.lttng2.control.ui.views.handlers;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceSessionState;
import org.eclipse.tracecompass.internal.lttng2.control.ui.Activator;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.model.impl.TraceSessionComponent;
import org.eclipse.tracecompass.lttng2.control.ui.views.signals.ExternalTraceStartSignal;
import org.eclipse.tracecompass.lttng2.control.ui.views.signals.LTTngSessionStartSignal;
import org.eclipse.tracecompass.lttng2.control.ui.views.signals.LTTngSessionStopSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;

/**
 * <p>
 * Command handler implementation to start one or more trace sessions.
 * </p>
 *
 * @author Bernd Hufmann
 */
public class StartHandler extends ChangeSessionStateHandler {

    /**
     * Constructor
     */
    public StartHandler() {
        super();
        TmfSignalManager.register(this);
    }

    @Override
    public void dispose() {
        TmfSignalManager.deregister(this);
        super.dispose();
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    @Override
    public TraceSessionState getNewState() {
        return TraceSessionState.ACTIVE;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    public void changeState(TraceSessionComponent session, IProgressMonitor monitor) throws ExecutionException {
        session.startSession(monitor);
        TmfSignalManager.dispatchSignal(
                new LTTngSessionStartSignal(session));
    }

    /**
     * Handle the external trace start signal
     * @param signal contains the information of the start external trace
     */
    @TmfSignalHandler
    public void handle(ExternalTraceStartSignal signal) {
        Display.getDefault().asyncExec(() -> {
            try {
                execute(null);
            } catch (ExecutionException e) {
                Activator.getDefault().logError("Failed to synchronize with the external trace start signal", e); //$NON-NLS-1$
            }
        });
    }

    /**
     * Handle the LTTng session stop signal
     * @param signal contains the information of the stop LTTng session
     */
    @TmfSignalHandler
    public void handle(LTTngSessionStopSignal signal) {
        Display.getDefault().asyncExec(() -> {
            if (signal.getSource() instanceof TraceSessionComponent session && !this.fSessions.contains(session)) {
                this.fSessions.add(session);
            }
        });
    }
}
