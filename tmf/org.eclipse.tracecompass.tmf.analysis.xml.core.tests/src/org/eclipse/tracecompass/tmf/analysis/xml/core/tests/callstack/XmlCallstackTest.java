/*******************************************************************************
 * Copyright (c) 2017, 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.analysis.xml.core.tests.callstack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.base.FlameWithKernelPalette;
import org.eclipse.tracecompass.analysis.profiling.core.base.IDataPalette;
import org.eclipse.tracecompass.analysis.profiling.core.tests.CallStackTestBase2;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeProvider.DataType;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeProvider.MetricType;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.callstack.CallstackXmlAnalysis;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlAnalysisModuleSource;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.tests.Activator;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.junit.Test;

/**
 * Test the XML callstack analysis
 *
 * @author Geneviève Bastien
 */
public class XmlCallstackTest extends CallStackTestBase2 {

    private static final @NonNull String XML_MODULE_ID = "callstack.analysis";
    private static final String XML_FILE_NAME = "test_callstack.xml";
    private static final Path CALLSTACK_XML_PATH = new Path("test_xml_files/test_valid/test_callstack.xml");


    private CallstackXmlAnalysis fXmlModule;

    @Override
    public void setUp() {
        // Add the XML file
        IPath absoluteFilePath = Activator.getAbsolutePath(CALLSTACK_XML_PATH);
        File file = absoluteFilePath.toFile();
        IStatus xmlFile = XmlUtils.addXmlFile(file);
        assertTrue(xmlFile.isOK());
        XmlAnalysisModuleSource.notifyModuleChange();
        super.setUp();

        ITmfTrace trace = getTrace();
        CallstackXmlAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, CallstackXmlAnalysis.class, XML_MODULE_ID);
        assertNotNull(module);

        module.schedule();
        assertTrue(module.waitForCompletion());
        fXmlModule = module;
    }

    @Override
    public void tearDown() {
        super.tearDown();
        // Remote the XML file
        XmlUtils.deleteFiles(Collections.singleton(XML_FILE_NAME));

    }

    /**
     * Test the weighted tree configuration of the XML callstack
     */
    @Test
    public void testCallstackWeightedTree() {
        CallstackXmlAnalysis xmlModule = fXmlModule;
        assertNotNull(xmlModule);

        MetricType weightType = xmlModule.getWeightType();
        assertEquals(DataType.NANOSECONDS, weightType.getDataType());

        List<@NonNull MetricType> additionalMetrics = xmlModule.getAdditionalMetrics();
        assertFalse(additionalMetrics.isEmpty());

        IDataPalette palette = xmlModule.getPalette();
        assertEquals(FlameWithKernelPalette.getInstance(), palette);
    }

}
