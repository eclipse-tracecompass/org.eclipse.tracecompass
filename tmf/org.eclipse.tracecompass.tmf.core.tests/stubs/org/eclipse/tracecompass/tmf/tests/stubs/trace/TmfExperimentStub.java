/*******************************************************************************
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
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.tests.stubs.trace;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.request.TmfCoalescedEventRequest;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.TmfEventProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfTraceIndexer;

/**
 * <b><u>TmfExperimentStub</u></b>
 * <p>
 * Implement me. Please.
 * <p>
 */
@SuppressWarnings("javadoc")
public class TmfExperimentStub extends TmfExperiment {

    private final Collection<IAnalysisModule> fAdditionalModules = new HashSet<>();

    /**
     * Default constructor. Should not be called directly by the code, but
     * needed for the extension point.
     *
     * Do not call this directly (but do not remove it either!)
     */
    public TmfExperimentStub() {
        super();
    }

    public TmfExperimentStub(String name, ITmfTrace[] traces, int blockSize) {
        super(ITmfEvent.class, name, traces, blockSize, null);
    }

    @Override
    protected ITmfTraceIndexer createIndexer(int interval) {
        return new TmfIndexerStub(this, interval);
    }

    @Override
    public TmfIndexerStub getIndexer() {
        return (TmfIndexerStub) super.getIndexer();
    }

    /**
     * @return a copy of the pending request list
     * @throws Exception
     *             if java reflection failed
     */
    public List<TmfCoalescedEventRequest> getAllPendingRequests() throws Exception {
        Method m = TmfEventProvider.class.getDeclaredMethod("getPendingRequests");
        m.setAccessible(true);
        LinkedList<?> list = (LinkedList<?>) m.invoke(this);
        LinkedList<TmfCoalescedEventRequest> retList = new LinkedList<>();
        for (Object element : list) {
            retList.add((TmfCoalescedEventRequest) element);
        }

        return retList;
    }

    /**
     * Clears the pending request list
     *
     * @throws Exception
     *             if java reflection failed
     */
    public void clearAllPendingRequests() throws Exception {
        Method m = TmfEventProvider.class.getDeclaredMethod("clearPendingRequests");
        m.setAccessible(true);
        m.invoke(this);
    }

    /**
     * Sets the timer flag
     *
     * @param enabled
     *            flag to set
     * @throws Exception
     *             if java reflection failed
     */
    public void setTimerEnabledFlag(boolean enabled) throws Exception {
        Class<?>[] paramTypes = new Class[1];
        paramTypes[0] = Boolean.class;
        Method m = TmfEventProvider.class.getDeclaredMethod("setTimerEnabled", paramTypes);

        Object[] params = new Object[1];
        params[0] = Boolean.valueOf(enabled);
        m.setAccessible(true);
        m.invoke(this, params);
    }

    /**
     * Add an additional new module
     *
     * @param module
     *            The new module
     */
    public void addAnalysisModule(IAnalysisModule module) {
        fAdditionalModules.add(module);
    }

    /**
     * Make this specific stub meant to support traces with at least one
     * prefixed with "A-".
     */
    @Override
    public IStatus validateWithTraces(List<ITmfTrace> traces) {
        if (getClass() == TmfExperimentStub.class) {
            int confidence = 0;
            for (ITmfTrace trace : traces) {
                if (trace.getName().startsWith("A-")) {
                    confidence = DEFAULT_GENERIC_EXPERIMENT_CONFIDENCE;
                } else if (trace.getName().startsWith("E-")) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "stubbed error case"); //$NON-NLS-1$
                }
            }
            return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "class extends TmfExperimentStub"); //$NON-NLS-1$
    }
}
