/*******************************************************************************
 * Copyright (c) 2014, 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.tests.project.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.TmfExperimentStub;
import org.eclipse.tracecompass.tmf.ui.editors.TmfEventsEditor;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfExperimentElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.tracecompass.tmf.ui.tests.experiment.type.TmfEventsEditorStub;
import org.eclipse.tracecompass.tmf.ui.tests.shared.ProjectModelTestData;
import org.eclipse.tracecompass.tmf.ui.viewers.events.TmfEventsTable;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Some unit tests for trace types and experiment types
 *
 * @author Geneviève Bastien
 */
public class TraceAndExperimentTypeTest {

    /** Test experiment type id */
    public static final String TEST_EXPERIMENT_TYPE = "org.eclipse.linuxtools.tmf.core.tests.experimenttype";

    private List<ITmfTrace> fTraces;
    private TmfProjectElement fixture;
    private TmfExperimentElement fExperiment;
    private static final String EXPERIMENT_NAME = "exp_test";

    /**
     * Perform pre-test initialization.
     */
    @Before
    public void setUp() {
        try {
            fixture = ProjectModelTestData.getFilledProject();
            fExperiment = ProjectModelTestData.addExperiment(fixture, EXPERIMENT_NAME);
            assertNotNull(fExperiment);
            fTraces = new ArrayList<>();
        } catch (CoreException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Cleans up the project after tests have been executed
     */
    @After
    public void cleanUp() {
        ProjectModelTestData.deleteProject(fixture);
        for (ITmfTrace trace : fTraces) {
            trace.dispose();
        }
    }

    /**
     * Test whether a newly created experiment has the default experiment type,
     * even though none was specified
     */
    @Test
    public void testDefaultExperimentType() {
        TmfExperimentElement experimentElement = ProjectModelTestData.addExperiment(fixture, "testDefaultExpType");
        assertNotNull(experimentElement);
        TmfExperiment experiment = experimentElement.instantiateTrace();
        assertNotNull(experiment);
        assertEquals(TmfTraceType.DEFAULT_EXPERIMENT_TYPE, experimentElement.getTraceType());
        experiment.dispose();
    }

    /**
     * Test selecting experiment type based on valid hint (default)
     */
    @Test
    public void testSelectExperimentTypeWithHint() {
        fTraces.add(TmfTestTrace.A_TEST_10K.getTrace());
        fTraces.add(TmfTestTrace.A_TEST_10K2.getTrace());

        String expected = TmfTraceType.DEFAULT_EXPERIMENT_TYPE;
        List<TraceTypeHelper> types = TmfTraceType.selectExperimentType(fTraces, expected);
        assertEquals(1, types.size());
        assertEquals(expected, types.get(0).getTraceTypeId());
    }

    /**
     * Test selecting experiment type based on no hint (null) => main stub
     */
    @Test
    public void testSelectExperimentTypeWithoutHint() {
        fTraces.add(TmfTestTrace.A_TEST_10K.getTrace());
        fTraces.add(TmfTestTrace.A_TEST_10K2.getTrace());

        List<TraceTypeHelper> types = TmfTraceType.selectExperimentType(fTraces, null);
        assertEquals(1, types.size());
        assertEquals(TEST_EXPERIMENT_TYPE, types.get(0).getTraceTypeId());
    }

    /**
     * Test selecting experiment type based on no hint, SysLog traces
     */
    @Test
    public void testSelectExperimentTypeSyslog() {
        fTraces.add(TmfTestTrace.SYSLOG_1.getTrace());
        fTraces.add(TmfTestTrace.SYSLOG_2.getTrace());

        List<TraceTypeHelper> types = TmfTraceType.selectExperimentType(fTraces, null);
        assertEquals(1, types.size());
        assertEquals(TEST_EXPERIMENT_TYPE + ".syslog", types.get(0).getTraceTypeId());
    }

    /**
     * Test selecting experiment type based on no hint, mixed traces
     */
    @Test
    public void testSelectExperimentTypeMixed() {
        fTraces.add(TmfTestTrace.A_TEST_10K.getTrace());
        fTraces.add(TmfTestTrace.SYSLOG_1.getTrace());

        List<TraceTypeHelper> types = TmfTraceType.selectExperimentType(fTraces, null);
        assertEquals(2, types.size());
    }

    /**
     * Test selecting experiment type based on no hint, unknown traces
     */
    @Test
    public void testSelectExperimentTypeUnknown() {
        fTraces.add(TmfTestTrace.O_TEST_10K.getTrace());
        fTraces.add(TmfTestTrace.R_TEST_10K.getTrace());

        List<TraceTypeHelper> types = TmfTraceType.selectExperimentType(fTraces, null);
        assertTrue(types.isEmpty());
    }

    /**
     * Test selecting experiment type based on no hint, error trace => default
     */
    @Test
    public void testSelectExperimentTypeError() {
        fTraces.add(TmfTestTrace.A_TEST_10K.getTrace());
        fTraces.add(TmfTestTrace.E_TEST_10K.getTrace()); // stubbed error case

        List<TraceTypeHelper> types = TmfTraceType.selectExperimentType(fTraces, null);
        assertEquals(1, types.size());
        assertEquals(TmfTraceType.DEFAULT_EXPERIMENT_TYPE, types.get(0).getTraceTypeId());
    }

    /**
     * Test that the experiment opened is of the right class
     */
    @Test
    public void testExperimentType() {

        IResource resource = fExperiment.getResource();
        try {
            resource.setPersistentProperty(TmfCommonConstants.TRACETYPE, TEST_EXPERIMENT_TYPE);
            fExperiment.refreshTraceType();
        } catch (CoreException e) {
            fail(e.getMessage());
        }

        TmfOpenTraceHelper.openFromElement(fExperiment);
        ProjectModelTestData.delayUntilTraceOpened(fExperiment);

        ITmfTrace trace = fExperiment.getTrace();
        assertTrue(trace instanceof TmfExperimentStub);

        fExperiment.closeEditors();
    }

    /**
     * Test that event editor, event table and statistics viewer are the default
     * ones for a generic experiment
     */
    @Test
    public void testNoExperimentTypeChildren() {
        TmfOpenTraceHelper.openFromElement(fExperiment);

        ProjectModelTestData.delayUntilTraceOpened(fExperiment);

        final IWorkbench wb = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();
        IEditorPart editor = activePage.getActiveEditor();

        /*
         * Test the editor class. Cannot test table class since it is unexposed
         */
        assertNotNull(editor);
        assertTrue(editor.getClass().equals(TmfEventsEditor.class));

        fExperiment.closeEditors();
    }

    /**
     * Test that event editor, event table and statistics viewer are built
     * correctly when specified
     */
    @Test
    public void testExperimentTypeChildren() {

        /* Set the trace type of the experiment */
        IResource resource = fExperiment.getResource();
        try {
            resource.setPersistentProperty(TmfCommonConstants.TRACETYPE, TEST_EXPERIMENT_TYPE);
            fExperiment.refreshTraceType();
        } catch (CoreException e) {
            fail(e.getMessage());
        }

        TmfOpenTraceHelper.openFromElement(fExperiment);

        ProjectModelTestData.delayThread(500);

        /* Test the editor class */
        final IWorkbench wb = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();
        IEditorPart editor = activePage.getActiveEditor();

        assertNotNull(editor);
        assertTrue(editor.getClass().equals(TmfEventsEditorStub.class));

        /* Test the event table class */
        TmfEventsEditorStub editorStub = (TmfEventsEditorStub) editor;
        TmfEventsTable table = editorStub.getNewEventsTable();

        assertNotNull(table);
        assertTrue(table.getClass().equals(TmfEventsTable.class));

        fExperiment.closeEditors();
    }

    /**
     * Test that the analysis get populated under an experiment of the proper
     * type
     */
    @Test
    public void testExperimentTypeAnalysis() {

        /* Set the trace type of the experiment */
        IResource resource = fExperiment.getResource();
        try {
            resource.setPersistentProperty(TmfCommonConstants.TRACETYPE, TEST_EXPERIMENT_TYPE);
            fExperiment.refreshTraceType();
        } catch (CoreException e) {
            fail(e.getMessage());
        }

        /* Force the refresh of the experiment */
        fExperiment.getParent().refresh();
        assertFalse(fExperiment.getAvailableAnalysis().isEmpty());
    }

}
