/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.tests.stubs.analysis;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;

/**
 * Simple analysis type for test
 */
public class TestConfigurableAnalysis extends TestAnalysis {

    private @Nullable ITmfConfiguration fConfiguration = null;

    /**
     * Constructor
     */
    public TestConfigurableAnalysis() {
        super();
    }

    @Override
    public void setConfiguration(ITmfConfiguration configuration) throws TmfConfigurationException {
        if (configuration == null) {
            throw new TmfConfigurationException("Configuration is null");
        }
        fConfiguration = configuration;
    }

    @Override
    public @Nullable ITmfConfiguration getConfiguration() {
        return fConfiguration;
    }
}
