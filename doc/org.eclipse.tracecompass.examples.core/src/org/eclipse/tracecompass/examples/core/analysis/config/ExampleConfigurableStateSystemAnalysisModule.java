/*******************************************************************************
 * Copyright (c) 2020, 2025 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.examples.core.analysis.config;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

/**
 * An example of a simple state system analysis module.
 *
 * This module is also in the developer documentation of Trace Compass. If it is
 * modified here, the doc should also be updated.
 *
 * @author Geneviève Bastien
 * @author Bernd Hufmann
 */
public class ExampleConfigurableStateSystemAnalysisModule extends TmfStateSystemAnalysisModule {

    /**
     * Module ID
     */
    public static final String ID = "org.eclipse.tracecompass.examples.state.system.module.config"; //$NON-NLS-1$

    /**
     * The configuration to apply.
     */
    private ITmfConfiguration fConfiguration = null;

    /**
     * Default constructor
     */
    public ExampleConfigurableStateSystemAnalysisModule() {
        super();
    }

    /**
     * Constructor
     * 
     * @param configuration
     *          the configuration
     */
    public ExampleConfigurableStateSystemAnalysisModule(ITmfConfiguration configuration) {
        fConfiguration = configuration;
        if (configuration != null) {
            setId(ID + ":" + fConfiguration.getId()); //$NON-NLS-1$
        }
    }
    
    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new ExampleConfigurableStateProvider(Objects.requireNonNull(getTrace()), fConfiguration);
    }

    @Override
    public @Nullable ITmfConfiguration getConfiguration() {
        return fConfiguration;
    }
}
