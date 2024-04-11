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

package org.eclipse.tracecompass.internal.tmf.analysis.xml.core.callstack;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.Activator;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfAnalysisModuleHelperXml;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlUtils;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModuleHelper;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAnalysisManager;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.w3c.dom.Element;

import com.google.common.collect.Multimap;

/**
 * An XML module helper for the callstack modules. It overrides some methods to
 * use the trace type that applies as well as labels from the module that
 * creates the state system.
 *
 * @author Geneviève Bastien
 */
public class CallstackXmlModuleHelper extends TmfAnalysisModuleHelperXml {

    static interface ISubModuleHelper {
        public Collection<IAnalysisModuleHelper> getHelpers();

        public @Nullable IAnalysisModule getAnalysis(ITmfTrace trace);
    }

    static class HiddenModuleHelper implements ISubModuleHelper {
        private final IAnalysisModuleHelper fHelper;

        public HiddenModuleHelper(IAnalysisModuleHelper helper) {
            fHelper = helper;
        }

        @Override
        public Collection<IAnalysisModuleHelper> getHelpers() {
            return Collections.singleton(fHelper);
        }

        @Override
        public @Nullable IAnalysisModule getAnalysis(ITmfTrace trace) {
            try {
                return fHelper.newModule(trace);
            } catch (TmfAnalysisException e) {
                Activator.logError(e.getMessage());
            }
            return null;
        }
    }

    static class RefModuleHelper implements ISubModuleHelper {
        private final String fAnalysisId;

        public RefModuleHelper(String id) {
            fAnalysisId = id;
        }

        @Override
        public @NonNull Collection<IAnalysisModuleHelper> getHelpers() {
            Multimap<String, IAnalysisModuleHelper> analysisModules = TmfAnalysisManager.getAnalysisModules();
            Collection<IAnalysisModuleHelper> collection = analysisModules.get(fAnalysisId);
            if (collection.isEmpty()) {
                Activator.logWarning("Callstack XML analysis: no analysis called " + fAnalysisId); //$NON-NLS-1$
            }
            return collection;
        }

        @Override
        public @Nullable IAnalysisModule getAnalysis(ITmfTrace trace) {
            return trace.getAnalysisModule(fAnalysisId);
        }
    }

    private final ISubModuleHelper fHelper;

    /**
     * Constructor
     *
     * @param xmlFile
     *            The XML file this element comes from
     * @param node
     *            The XML element for this callstack
     */
    public CallstackXmlModuleHelper(File xmlFile, Element node) {
        super(xmlFile, node, XmlAnalysisModuleType.OTHER);
        /* Create the helper for the underlying module or set its analysis ID */
        List<Element> childElements = TmfXmlUtils.getChildElements(node, TmfXmlStrings.PATTERN);
        if (!childElements.isEmpty()) {
            // Create a helper for this module
            fHelper = new HiddenModuleHelper(new TmfAnalysisModuleHelperXml(xmlFile, childElements.get(0), XmlAnalysisModuleType.PATTERN));
            return;
        }
        childElements = TmfXmlUtils.getChildElements(node, TmfXmlStrings.STATE_PROVIDER);
        if (!childElements.isEmpty()) {
            // Create a helper for this module
            fHelper = new HiddenModuleHelper(new TmfAnalysisModuleHelperXml(xmlFile, childElements.get(0), XmlAnalysisModuleType.STATE_SYSTEM));
            return;
        }
        childElements = TmfXmlUtils.getChildElements(node, TmfXmlStrings.ANALYSIS);
        if (childElements.isEmpty()) {
            throw new IllegalStateException("XML callstack element: there should be one of {pattern, stateProvider, analysis}. none found"); //$NON-NLS-1$
        }
        // Create a helper for this module
        fHelper = new RefModuleHelper(String.valueOf(childElements.get(0).getAttribute(TmfXmlStrings.ID)));
        return;
    }

    @Override
    protected IAnalysisModule createOtherModule(String analysisid, String name) {
        IAnalysisModule module = new CallstackXmlAnalysis(Objects.requireNonNull(getSourceFile()), fHelper);
        module.setId(analysisid);
        module.setName(name);
        return module;
    }

    @Override
    public String getName() {
        return fHelper.getHelpers().stream()
                .map(h -> h.getName())
                .findFirst()
                .get();
    }

    @Override
    @NonNullByDefault({})
    public boolean appliesToTraceType(Class<? extends ITmfTrace> traceClass) {
        List<IAnalysisModuleHelper> collect = fHelper.getHelpers().stream()
                .filter(h -> h.appliesToTraceType(traceClass))
                .collect(Collectors.toList());
        return !collect.isEmpty();
    }

}
