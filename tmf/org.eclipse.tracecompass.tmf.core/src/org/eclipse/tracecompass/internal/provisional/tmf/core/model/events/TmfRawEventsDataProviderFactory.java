/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.core.model.events;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * {@link TmfRawEventsDataProvider} factory, uses the data provider extension
 * point.
 *
 * @author Patrick Tasse
 */
public class TmfRawEventsDataProviderFactory implements IDataProviderFactory {

    private static final IDataProviderDescriptor DESCRIPTOR =
            new DataProviderDescriptor.Builder()
                        .setId(TmfRawEventsDataProvider.ID)
                        .setName(NonNullUtils.nullToEmptyString(Messages.RawEventsDataProvider_Title))
                        .setDescription(NonNullUtils.nullToEmptyString(Messages.RawEventsDataProviderFactory_DescriptionText))
                        .setProviderType(ProviderType.DATA)
                        .setCapabilities(new DataProviderCapabilities.Builder().setSelectionRange(true).build())
                        .build();

    @Override
    public @Nullable ITmfDataProvider createDataProvider(ITmfTrace trace) {
        return new TmfRawEventsDataProvider(trace);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        return Collections.singleton(DESCRIPTOR);
    }
}
