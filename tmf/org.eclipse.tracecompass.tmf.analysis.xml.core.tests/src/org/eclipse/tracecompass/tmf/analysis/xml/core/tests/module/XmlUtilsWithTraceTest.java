/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.analysis.xml.core.tests.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlAnalysisModuleSource;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.tests.Activator;
import org.eclipse.tracecompass.tmf.analysis.xml.core.tests.common.TmfXmlTestFiles;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.TmfProjectNature;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.ResourceUtil;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test the XML Utils delete supplementary directory
 *
 * @author Geneviève Bastien
 */
public class XmlUtilsWithTraceTest {

    private static final @NonNull String TEST_TRACE_NAME = "testTrace4.xml";

    private static final @NonNull String TEST_TRACE = "test_traces/" + TEST_TRACE_NAME;

    private static final @NonNull String ANALYSIS_ID = "xml.core.tests.simple.pattern";

    private static IProject sfProject;

    /**
     * Class setup
     *
     * @throws CoreException
     *             if an error occurs
     */
    @BeforeClass
    public static void startUp() throws CoreException {
        IProject project = ResourcesPlugin.getWorkspace().getRoot()
                .getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        if (!project.exists()) {
            project.create(null);
            if (!project.isOpen()) {
                project.open(null);
            }
            IProjectDescription description = project.getDescription();
            description.setNatureIds(new String[] { TmfProjectNature.ID });
            project.setDescription(description, null);
        }
        if (!project.isOpen()) {
            project.open(null);
        }

        IFolder tracesFolder = project.getFolder("Traces"); //$NON-NLS-1$
        if (!tracesFolder.exists()) {
            tracesFolder.create(true, true, null);
        }

        IFolder supplRootFolder = project.getFolder(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER_NAME);
        if (!supplRootFolder.exists()) {
            supplRootFolder.create(true, true, null);
        }
        sfProject = project;
    }

    /**
     * Class tear down
     *
     * @throws CoreException
     *             if an error occurs
     */
    @AfterClass
    public static void tearDown() throws CoreException {
        if (sfProject != null) {
            sfProject.delete(true, null);
        }
    }

    /**
     * Load the XML files for the current test
     */
    @Before
    public void setUp() {
        XmlUtils.addXmlFile(TmfXmlTestFiles.VALID_PATTERN_SIMPLE_FILE.getFile());
        XmlAnalysisModuleSource.notifyModuleChange();
    }

    /**
     * Clean
     */
    @After
    public void cleanUp() {
        XmlUtils.deleteFiles(ImmutableList.of(
                TmfXmlTestFiles.VALID_PATTERN_SIMPLE_FILE.getFile().getName()));
        XmlAnalysisModuleSource.notifyModuleChange();
    }

    private ITmfTrace getTrace() throws CoreException {
        TmfXmlTraceStubNs trace = new TmfXmlTraceStubNs();
        String absolutePath = Activator.getAbsolutePath(new Path(TEST_TRACE)).toOSString();
        IStatus status = trace.validate(null, absolutePath);
        if (!status.isOK()) {
            fail(status.getException().getMessage());
        }

        IResource resource = createResource(absolutePath);
        try {
            trace.initTrace(resource, absolutePath, TmfEvent.class);
        } catch (TmfTraceException e) {
            trace.dispose();
            fail(e.getMessage());
        }

        // Initialize the trace and module
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, trace, null);
        trace.traceOpened(signal);
        // The data provider manager uses opened traces from the manager
        TmfTraceManager.getInstance().traceOpened(signal);
        return trace;
    }

    @SuppressWarnings("null")
    private static IResource createResource(String path) throws CoreException {
        IPath targetLocation = new Path(path);
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        IFolder tracesFolder = project.getFolder("Traces");
        IResource resource = tracesFolder.getFile(TEST_TRACE_NAME);
        ResourceUtil.createSymbolicLink(resource, targetLocation, true, null);

        IFolder supplRootFolder = project.getFolder(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER_NAME);
        IFolder supplFolder = supplRootFolder.getFolder(resource.getProjectRelativePath().removeFirstSegments(1));
        if (!supplFolder.exists()) {
            supplFolder.create(true, true, null);
        }
        resource.setPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, supplFolder.getLocation().toOSString());

        return resource;
    }

    private static void runModule(ITmfTrace trace) {
        IAnalysisModule module = trace.getAnalysisModule(ANALYSIS_ID);
        assertNotNull(module);
        module.schedule();
        assertTrue(module.waitForCompletion());
    }

    /**
     * Test delete supplementary dir when trace is open
     *
     * @throws CoreException
     *             if an error occurs
     */
    @Test
    public void testDeleteSupplDirWithTraceOpen() throws CoreException {
        ITmfTrace trace = getTrace();
        assertNotNull(trace);
        try {
            runModule(trace);
            String supplPathString = TmfTraceManager.getSupplementaryFileDir(trace);
            IPath path = new Path(supplPathString);
            IPath ssFile = path.addTrailingSeparator().append(ANALYSIS_ID).addFileExtension("ht");
            IPath segFile = path.addTrailingSeparator().append(ANALYSIS_ID).addFileExtension("dat");
            assertTrue(ssFile.toFile().exists());
            assertTrue(segFile.toFile().exists());
            XmlUtils.deleteSupplementaryResources(TmfXmlTestFiles.VALID_PATTERN_SIMPLE_FILE.getFile().getName(), null);
            assertFalse(ssFile.toFile().exists());
            assertFalse(segFile.toFile().exists());
        } finally {
            trace.dispose();
            TmfTraceManager.getInstance().traceClosed(new TmfTraceClosedSignal(this, trace));
        }
    }

    /**
     * Test delete supplementary dir when trace is open
     *
     * @throws CoreException
     *             Exception thrown by analyses
     */
    @Test
    public void testDeleteSupplDirWithTraceClosed() throws CoreException {
        ITmfTrace trace = getTrace();
        assertNotNull(trace);
        try {
            runModule(trace);
            String supplPathString = TmfTraceManager.getSupplementaryFileDir(trace);
            IPath path = new Path(supplPathString);
            IPath ssFile = path.addTrailingSeparator().append(ANALYSIS_ID).addFileExtension("ht");
            IPath segFile = path.addTrailingSeparator().append(ANALYSIS_ID).addFileExtension("dat");
            assertTrue(ssFile.toFile().exists());
            assertTrue(segFile.toFile().exists());

            TmfTraceClosedSignal signal = new TmfTraceClosedSignal(this, trace);
            TmfTraceManager.getInstance().traceClosed(signal);
            trace.dispose();

            XmlUtils.deleteSupplementaryResources(TmfXmlTestFiles.VALID_PATTERN_SIMPLE_FILE.getFile().getName(), null);
            assertFalse(ssFile.toFile().exists());
            assertFalse(segFile.toFile().exists());
        } finally {
            trace.dispose();
            TmfTraceManager.getInstance().traceClosed(new TmfTraceClosedSignal(this, trace));
        }
    }

}
