/******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.integration.core.tests.dataproviders;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Hidden data provider factory
 */
public class HiddenDataProviderFactory implements IDataProviderFactory {

    private static final String ID = "org.eclipse.tracecompass.integration.core.tests.hidden.dataprovider";
    private static final String NAME = "Hidden";
    private static final String DESCRIPTION = "Hidden data provider for integration tests";
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName(NAME)
            .setDescription(DESCRIPTION)
            .setProviderType(ProviderType.NONE)
            .build();

    /**
     * Constructor
     */
    public HiddenDataProviderFactory() {
        System.out.println("HiddenDataProviderFactory()");
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        return null;
    }

    @Override
    public @NonNull Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        return Arrays.asList(DESCRIPTOR);
    }
}
