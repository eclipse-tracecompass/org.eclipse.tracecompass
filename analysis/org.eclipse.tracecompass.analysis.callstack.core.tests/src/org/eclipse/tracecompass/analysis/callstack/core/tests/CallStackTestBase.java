/*******************************************************************************
 * Copyright (c) 2016, 2023 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.callstack.core.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.callstack.core.tests.stubs.CallStackAnalysisStub;
import org.eclipse.tracecompass.analysis.callstack.core.tests.stubs.KernelAnalysisModuleStub;
import org.eclipse.tracecompass.internal.analysis.callstack.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.internal.analysis.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.internal.analysis.callstack.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.internal.analysis.callstack.core.model.ModelManager;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for call stack tests. It sets up the trace and analysis module.
 *
 * @author Geneviève Bastien
 */
public class CallStackTestBase {
    private static final String CALLSTACK_FILE = "testfiles/traces/callstack.xml";

    private ITmfTrace fTrace;
    private CallStackAnalysisStub fModule;
    private KernelAnalysisModuleStub fKernelModule;

    /**
     * Setup the trace for the tests
     */
    @Before
    public void setUp() {
        TmfXmlTraceStub trace = new TmfXmlTraceStubNs();
        IPath filePath = ActivatorTest.getAbsoluteFilePath(CALLSTACK_FILE);
        IStatus status = trace.validate(null, filePath.toOSString());
        if (!status.isOK()) {
            fail(status.getException().getMessage());
        }
        try {
            trace.initTrace(null, filePath.toOSString(), TmfEvent.class);
        } catch (TmfTraceException e) {
            fail(e.getMessage());
        }
        fTrace = trace;
        TmfTraceManager traceManager = TmfTraceManager.getInstance();
        traceManager.traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));

        KernelAnalysisModuleStub kernelModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModuleStub.class, KernelAnalysisModuleStub.ID1);
        assertNotNull(kernelModule);
        kernelModule.schedule();
        assertTrue(kernelModule.waitForCompletion());
        fKernelModule = kernelModule;

        CallStackAnalysisStub module = TmfTraceUtils.getAnalysisModuleOfClass(trace, CallStackAnalysisStub.class, CallStackAnalysisStub.ID);
        assertNotNull(module);
        module.schedule();
        assertTrue(module.waitForCompletion());
        fModule = module;
    }

    /**
     * Dispose of the test data
     */
    @After
    public void tearDown() {
        ITmfTrace trace = fTrace;
        if (trace != null) {
            trace.dispose();
        }
        InstrumentedCallStackAnalysis module = fModule;
        if (module != null) {
            module.dispose();
        }
        ModelManager.disposeModels();
    }

    /**
     * Get the analysis module. Its execution is complete.
     *
     * The structure of the callstack provided by this module is the following:
     *
     * <pre>
     * where 1e2 means at timestamp 1, entry of function named op2
     *   and 10x means at timestamp 10, exit of the function
     *
     * pid1 --- tid2   1e1 ------------- 10x  12e4------------20x
     *      |             3e2-------7x
     *      |               4e3--5x
     *      |-- tid3      3e2 --------------------------------20x
     *                       5e3--6x  7e2--------13x
     *
     * pid5 --- tid6   1e1 ----------------------------------------20x
     *      |            2e3 ---------7x 8e2---11x 12e4------------20x
     *      |                4e1--6x      9e3-10x
     *      |-- tid7   1e5 -----------------------------------20x
     *                   2e2 +++ 6x  9e2 ++++ 13x 15e2 ++ 19x
     *                                10e3 + 11x
     * </pre>
     *
     * @return The analysis module
     */
    public CallStackAnalysisStub getModule() {
        return fModule;
    }

    /**
     * Get the kernel analysis module.
     *
     * It mimicks the kernel analysis by reproducing the thread states attribute
     * tree.
     *
     * @return The analysis module
     */
    public KernelAnalysisModuleStub getKernelModule() {
        return fKernelModule;
    }

    /**
     * Get the trace
     *
     * @return The trace used for this test
     */
    public @NonNull ITmfTrace getTrace() {
        ITmfTrace trace = fTrace;
        if (trace == null) {
            throw new IllegalArgumentException();
        }
        return trace;
    }

    /**
     * Get the callstack symbol from a callsite
     *
     * @param callsite
     *            The callsite to get the symbol for
     * @return The callstack symbol
     * @throws NullPointerException
     *             if the object associate with the callsite is not a callstack
     *             symbol
     */
    public static ICallStackSymbol getCallSiteSymbol(AggregatedCallSite callsite) {
        Object object = callsite.getObject();
        if (object instanceof ICallStackSymbol) {
            return (ICallStackSymbol) object;
        }
        throw new IllegalArgumentException("The object is not of the right type");
    }
}
