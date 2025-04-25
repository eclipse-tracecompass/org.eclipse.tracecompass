/*******************************************************************************
 * Copyright (c) 2017, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.dataprovider;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Implementation of this interface can be contributed using the
 * org.eclipse.tracecompass.tmf.core.dataprovider extension point and is used to
 * create {@link ITmfTreeDataProvider}
 *
 * @author Simon Delisle
 * @since 3.2
 */
public interface IDataProviderFactory extends IAdaptable {

    /**
     * Create a {@link ITmfTreeDataProvider} for the given trace. If this factory
     * does not know how to handle the given trace it will return null
     *
     * @param trace
     *            A trace
     * @return {@link ITmfTreeDataProvider} that can be use for the given trace
     * @since 4.0
     * @deprecated As of version 9.7, use {@link #createDataProvider(ITmfTrace)} instead
     */
    @Deprecated(since = "9.7", forRemoval = true)
    @Nullable default ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        return null;
    }

    /**
     * Create a {@link ITmfDataProvider} for the given trace. If this factory
     * does not know how to handle the given trace it will return null
     *
     * @param trace
     *            A trace
     * @return {@link ITmfDataProvider} that can be use for the given trace
     * @since 9.7
     */
    @Nullable default ITmfDataProvider createDataProvider(@NonNull ITmfTrace trace) {
        return createProvider(trace);
    }

    /**
D     * Create a {@link ITmfTreeDataProvider} for the given trace. If this factory
     * does not know how to handle the given trace it will return null. The
     * resulting provider should have an ID that is an aggregate of the provider's
     * own ID and the secondaryId as such: <provider ID>:<secondaryId>
     *
     * @param trace
     *            A trace
     * @param secondaryId
     *            Additional ID to identify different instances of the same
     *            provider, for instance, when the same provider can be used for
     *            different analysis modules
     * @return {@link ITmfTreeDataProvider} that can be use for the given trace with
     *         ID <provider ID>:<secondaryId>, or <code>null</code> if no provider
     *         is available for this trace and ID
     * @since 4.0
     * @deprecated As of version 9.7, use {@link #createDataProvider(ITmfTrace)} instead
     */
    @Deprecated(since = "9.7", forRemoval = true)
    default @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace, @NonNull String secondaryId) {
        return createProvider(trace);
    }

    /**
     * Create a {@link ITmfDataProvider} for the given trace. If this factory
     * does not know how to handle the given trace it will return null. The
     * resulting provider should have an ID that is an aggregate of the provider's
     * own ID and the secondaryId as such: <provider ID>:<secondaryId>
     *
     * @param trace
     *            A trace
     * @param secondaryId
     *            Additional ID to identify different instances of the same
     *            provider, for instance, when the same provider can be used for
     *            different analysis modules
     * @return {@link ITmfDataProvider} that can be use for the given trace with
     *         ID <provider ID>:<secondaryId>, or <code>null</code> if no provider
     *         is available for this trace and ID
     * @since 9.7
     */
    default @Nullable ITmfDataProvider createDataProvider(@NonNull ITmfTrace trace, @NonNull String secondaryId) {
        return createProvider(trace, secondaryId);
    }

    /**
     * Gets the collection of data provider descriptors for this trace that this
     * data provider factory can create, if the trace supports it, else
     * it returns an empty list
     *
     * @param trace
     *            trace whose support for this data provider we want to determine
     * @return a collection of {@link IDataProviderDescriptor} if this trace supports this
     *         provider, else empty list
     * @since 5.0
     */
    default @NonNull Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        return Collections.emptyList();
    }

    /**
     *  Disposes the data provider factory
     *  @since 9.5
     */
    default void dispose() {
        // do nothing
    }

    /**
     * Supported classes that a factory can adapt:
     * - {@link ITmfDataProviderConfigurator}
     *
     * {@inheritDoc}
     */
    @Override
    default <T> T getAdapter(Class<T> adapter) {
        if (adapter.isInstance(this)) {
            return adapter.cast(this);
        }
        return null;
    }
}
