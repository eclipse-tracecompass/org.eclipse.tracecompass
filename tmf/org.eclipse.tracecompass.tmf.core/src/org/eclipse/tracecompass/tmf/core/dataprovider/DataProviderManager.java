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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Manager for org.eclipse.tracecompass.tmf.core.dataprovider extension point.
 *
 * @author Simon Delisle
 * @since 3.2
 */
public class DataProviderManager {

    /**
     * The singleton instance of this manager
     */
    private static @Nullable DataProviderManager INSTANCE;

    private static final String EXTENSION_POINT_ID = "org.eclipse.tracecompass.tmf.core.dataprovider"; //$NON-NLS-1$
    private static final String ELEMENT_NAME_PROVIDER = "dataProviderFactory"; //$NON-NLS-1$
    private static final String ATTR_CLASS = "class"; //$NON-NLS-1$
    private static final String ATTR_ID = "id"; //$NON-NLS-1$
    private static final String ELEMENT_NAME_HIDE_DATA_PROVIDER = "hideDataProvider"; //$NON-NLS-1$
    private static final String ATTR_ID_REGEX = "idRegex"; //$NON-NLS-1$
    private static final String ATTR_TRACETYPE = "tracetype"; //$NON-NLS-1$
    private static final URL CONFIG_FILE_TEMPLATE = DataProviderManager.class.getResource("/templates/dataprovider.ini"); //$NON-NLS-1$
    private static final File CONFIG_FILE = Activator.getDefault().getStateLocation().addTrailingSeparator().append("dataprovider.ini").toFile(); //$NON-NLS-1$
    private static final Pattern CONFIG_LINE_PATTERN = Pattern.compile("(hide|show):(.+):(.*)"); //$NON-NLS-1$
    private static final String HIDE = "hide"; //$NON-NLS-1$
    private static final String SHOW = "show"; //$NON-NLS-1$
    private static final String WILDCARD = "*"; //$NON-NLS-1$

    private Map<String, IDataProviderFactory> fDataProviderFactories = new HashMap<>();
    private Multimap<String, Pattern> fHideDataProviders = HashMultimap.create();
    private Multimap<String, Pattern> fShowDataProviders = HashMultimap.create();

    private final Multimap<ITmfTrace, ITmfTreeDataProvider<? extends ITmfTreeDataModel>> fInstances = LinkedHashMultimap.create();

    /**
     * Get the instance of the manager
     *
     * @return the singleton instance
     */
    public synchronized static DataProviderManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DataProviderManager();
        }
        return INSTANCE;
    }

    /**
     * Dispose the singleton instance if it exists
     *
     * @since 3.3
     */
    public static synchronized void dispose() {
        DataProviderManager manager = INSTANCE;
        if (manager != null) {
            TmfSignalManager.deregister(manager);
            for (IDataProviderFactory factory : manager.fDataProviderFactories.values()) {
                TmfSignalManager.deregister(factory);
                factory.dispose();
            }
            manager.fDataProviderFactories.clear();
            manager.fInstances.clear();
        }
        INSTANCE = null;
    }

    /**
     * Private constructor.
     */
    private DataProviderManager() {
        loadDataProviders();
        loadHiddenDataProviders();
        TmfSignalManager.register(this);
    }

    /**
     * Load data provider factories from the registry
     */
    private void loadDataProviders() {
        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);
        for (IConfigurationElement cElement : configElements) {
            if (cElement != null && cElement.getName().equals(ELEMENT_NAME_PROVIDER)) {
                try {
                    Object extension = cElement.createExecutableExtension(ATTR_CLASS);
                    fDataProviderFactories.put(cElement.getAttribute(ATTR_ID), (IDataProviderFactory) extension);
                } catch (CoreException e) {
                    Activator.logError("Unable to load extensions", e); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Load hidden data providers from the registry and configuration file
     */
    private void loadHiddenDataProviders() {
        IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);
        for (IConfigurationElement cElement : configElements) {
            if (cElement != null && cElement.getName().equals(ELEMENT_NAME_HIDE_DATA_PROVIDER)) {
                String idRegex = cElement.getAttribute(ATTR_ID_REGEX);
                String tracetype = cElement.getAttribute(ATTR_TRACETYPE);
                tracetype = (tracetype == null || tracetype.isBlank()) ? WILDCARD : tracetype;
                try {
                    fHideDataProviders.put(tracetype, Pattern.compile(idRegex));
                    String pluginId = ((IExtension) cElement.getParent()).getNamespaceIdentifier();
                    Activator.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, String.format("plugin: %s is hiding data providers matching regex:\"%s\" for tracetype:%s", pluginId, idRegex, tracetype))); //$NON-NLS-1$
                } catch (PatternSyntaxException e) {
                    Activator.logError("Invalid hideDataProvider regex pattern", e); //$NON-NLS-1$
                }
            }
        }
        if (!CONFIG_FILE.exists()) {
            try {
                File defaultConfigFile = new File(FileLocator.toFileURL(CONFIG_FILE_TEMPLATE).toURI());
                Files.copy(defaultConfigFile.toPath(), CONFIG_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (URISyntaxException | IOException e) {
                Activator.logError("Error copying " + CONFIG_FILE_TEMPLATE + " to " + CONFIG_FILE.getAbsolutePath(), e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = CONFIG_LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String oper = matcher.group(1);
                    String idRegex = matcher.group(2);
                    String tracetype = matcher.group(3);
                    tracetype = (tracetype == null || tracetype.isBlank()) ? WILDCARD : tracetype;
                    try {
                        if (oper.equals(HIDE)) {
                            fHideDataProviders.put(tracetype, Pattern.compile(idRegex));
                            Activator.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, String.format("dataprovider.ini is hiding data providers matching regex:\"%s\" for tracetype:%s", idRegex, tracetype))); //$NON-NLS-1$
                        } else if (oper.equals(SHOW)) {
                            fShowDataProviders.put(tracetype, Pattern.compile(idRegex));
                            Activator.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, String.format("dataprovider.ini is showing data providers matching regex:\"%s\" for tracetype:%s", idRegex, tracetype))); //$NON-NLS-1$
                        }
                    } catch (PatternSyntaxException e) {
                        Activator.logError("Invalid dataprovider.ini regex pattern", e); //$NON-NLS-1$
                    }
                }
            }
        } catch (IOException e) {
            Activator.logError("Error reading " + CONFIG_FILE.getAbsolutePath(), e); //$NON-NLS-1$
        }
    }

    /**
     * Gets or creates the data provider for the given trace.
     * <p>
     * This method should never be called from within a
     * {@link TmfSignalHandler}.
     *
     * @param trace
     *            An instance of {@link ITmfTrace}. Note, that trace can be an
     *            instance of TmfExperiment, too.
     * @param id
     *            Id of the data provider. This ID can be the concatenation of a
     *            provider ID + ':' + a secondary ID used to differentiate
     *            multiple instances of a same provider.
     * @param dataProviderClass
     *            Returned data provider must extend this class
     * @return the data provider or null if no data provider is found for the
     *         input parameter.
     * @since 8.0
     */
    public synchronized @Nullable <T extends ITmfTreeDataProvider<? extends ITmfTreeDataModel>> T getOrCreateDataProvider(@NonNull ITmfTrace trace, String id, Class<T> dataProviderClass) {
        ITmfTreeDataProvider<? extends ITmfTreeDataModel> dataProvider = getExistingDataProvider(trace, id, dataProviderClass);
        if (dataProvider != null) {
            return dataProviderClass.cast(dataProvider);
        }
        if (isHidden(id, trace)) {
            return null;
        }
        String[] ids = id.split(DataProviderConstants.ID_SEPARATOR, 2);
        for (ITmfTrace opened : TmfTraceManager.getInstance().getOpenedTraces()) {
            if (TmfTraceManager.getTraceSetWithExperiment(opened).contains(trace)) {
                /*
                 * if this trace or an experiment containing it is opened
                 */
                IDataProviderFactory providerFactory = fDataProviderFactories.get(ids[0]);
                if (providerFactory != null) {
                    dataProvider = ids.length > 1 ? providerFactory.createProvider(trace, String.valueOf(ids[1])) : providerFactory.createProvider(trace);
                    if (dataProvider != null && id.equals(dataProvider.getId()) && dataProviderClass.isAssignableFrom(dataProvider.getClass())) {
                        fInstances.put(trace, dataProvider);
                        return dataProviderClass.cast(dataProvider);
                    }
                }
                return null;
            }
        }
        return null;
    }

    /**
     * Get a data provider for the given trace if it already exists due to
     * calling {@link #getOrCreateDataProvider(ITmfTrace, String, Class)}
     * before.
     *
     * <p>
     * This method should never be called from within a
     * {@link TmfSignalHandler}.
     *
     * @param trace
     *            An instance of {@link ITmfTrace}. Note, that trace can be an
     *            instance of TmfExperiment, too.
     * @param id
     *            Id of the data provider. This ID can be the concatenation of a
     *            provider ID + ':' + a secondary ID used to differentiate
     *            multiple instances of a same provider.
     * @param dataProviderClass
     *            Returned data provider must extend this class
     * @return the data provider or null
     * @since 8.0
     */
    public synchronized @Nullable <T extends ITmfTreeDataProvider<? extends ITmfTreeDataModel>> T getExistingDataProvider(@NonNull ITmfTrace trace, String id, Class<T> dataProviderClass) {
        for (ITmfTreeDataProvider<? extends ITmfTreeDataModel> dataProvider : fInstances.get(trace)) {
            if (id.equals(dataProvider.getId()) && dataProviderClass.isAssignableFrom(dataProvider.getClass()) && !isHidden(id, trace)) {
                return dataProviderClass.cast(dataProvider);
            }
        }
        return null;
    }

    /**
     * Signal handler for the traceClosed signal.
     *
     * @param signal
     *            The incoming signal
     * @since 3.3
     */
    @TmfSignalHandler
    public void traceClosed(final TmfTraceClosedSignal signal) {
        new Thread(() -> {
            synchronized (DataProviderManager.this) {
                for (ITmfTrace trace : TmfTraceManager.getTraceSetWithExperiment(signal.getTrace())) {
                    fInstances.removeAll(trace).forEach(ITmfTreeDataProvider::dispose);
                }
            }
        }).start();
    }

    /**
     * Get the list of available providers for this trace / experiment without
     * triggering the analysis or creating the provider
     *
     * @param trace
     *            queried trace
     * @return list of the available providers for this trace / experiment
     * @since 5.0
     */
    public List<IDataProviderDescriptor> getAvailableProviders(@Nullable ITmfTrace trace) {
        if (trace == null) {
            return Collections.emptyList();
        }
        List<IDataProviderDescriptor> list = new ArrayList<>();
        for (IDataProviderFactory factory : fDataProviderFactories.values()) {
            Collection<IDataProviderDescriptor> descriptors = factory.getDescriptors(trace);
            for (IDataProviderDescriptor descriptor : descriptors) {
                if (!isHidden(descriptor.getId(), trace)) {
                    list.add(descriptor);
                }
            }
        }
        list.sort(Comparator.comparing(IDataProviderDescriptor::getName));
        return list;
    }

    private boolean isHidden(String id, @Nullable ITmfTrace trace) {
        return isMatching(fHideDataProviders, id, trace) && !isMatching(fShowDataProviders, id, trace);
    }

    private static boolean isMatching(Multimap<String, Pattern> multimap, String id, @Nullable ITmfTrace trace) {
        for (Pattern pattern : multimap.get(WILDCARD)) {
            if (pattern.matcher(id).matches()) {
                return true;
            }
        }
        for (ITmfTrace expTrace : TmfTraceManager.getTraceSet(trace)) {
            String tracetype = expTrace.getTraceTypeId();
            for (Pattern pattern : multimap.get(tracetype)) {
                if (pattern.matcher(id).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove a data provider from the instances. This method will not dispose
     * of the data provider. It is the responsibility of the caller to dispose
     * of it if needed.
     *
     * @param <T>
     *            The type of data provider
     * @param trace
     *            The trace for which to remove the data provider
     * @param provider
     *            The data provider to remove
     * @return Whether the data provider was removed. The result would be
     *         <code>false</code> if the data provider was not present in the
     *         list.
     * @since 5.1
     */
    public <T extends ITmfTreeDataProvider<? extends ITmfTreeDataModel>> boolean removeDataProvider(ITmfTrace trace, T provider) {
        return fInstances.remove(trace, provider);
    }

    /**
     * Remove a data provider from the instances by Id. This method will also dispose
     * the data provider.
     *
     * @param trace
     *            The trace for which to remove the data provider
     * @param id
     *            The id of the data provider to remove
     * @since 9.7
     */
    public void removeDataProvider(ITmfTrace trace, String id) {
        Iterator<ITmfTreeDataProvider<? extends ITmfTreeDataModel>> iter = fInstances.get(trace).iterator();
        while (iter.hasNext()) {
            ITmfTreeDataProvider<? extends ITmfTreeDataModel> dp = iter.next();
            if (dp.getId().equals(id)) {
                dp.dispose();
                iter.remove();
                break;
            }
        }
    }

    /**
     * Get all registered data provider factories.
     *
     * @return a collection of existing data provider factories
     * @since 9.4
     */
    public synchronized Collection<IDataProviderFactory> getFactories() {
        return fDataProviderFactories.entrySet().stream().filter(e -> !isHidden(e.getKey(), null)).map(Map.Entry::getValue).collect(Collectors.toList());
    }

    /**
     * Get a registered data provider factory for a given ID if it exists.
     *
     * @param id
     *            The ID of the data provider factory or a data provider ID of
     *            form factoryId:secondaryId
     *
     * @return the data provider factory for a given Id.
     * @since 9.4
     */
    public synchronized @Nullable IDataProviderFactory getFactory(String id) {
        String factoryId = extractFactoryId(id);
        return isHidden(factoryId, null) ? null : fDataProviderFactories.get(factoryId);
    }

    /**
     * Add a new data provider factory.
     *
     * If a data provider factory associated with the ID already exists it will
     * replace it and will remove and dispose all data provider instances
     * created by the given factory for all open traces.
     *
     * @param id
     *            The data provider factory ID
     * @param factory
     *            The data provider factory implementation
     * @since 9.4
     */
    public synchronized void addDataProviderFactory(String id, IDataProviderFactory factory) {
        IDataProviderFactory existingFactory = fDataProviderFactories.put(id, factory);
        removeExistingDataProviders(existingFactory, id);
    }

    /**
     * Remove a data provider factory. It will remove and dispose all data
     * provider instances created by the factory associated with the ID for all
     * open traces.
     *
     * Call this method only if calling method is owner of factory and had added
     * factory before.
     *
     * @param id
     *            The ID of the data provider factory or a data provider ID of
     *            form factoryId:secondaryId
     * @since 9.4
     */
    public synchronized void removeDataProviderFactory(String id) {
        String passedFactoryId = extractFactoryId(id);
        IDataProviderFactory existingFactory = fDataProviderFactories.remove(passedFactoryId);
        removeExistingDataProviders(existingFactory, passedFactoryId);
    }

    /**
     * Removes and disposes all existing data providers created by an existing
     * factory with given factory ID.
     *
     * @param factory
     *            the existing factory
     * @param passedFactoryId
     *            The factory ID (not data provider ID)
     */
    private void removeExistingDataProviders(IDataProviderFactory factory, String passedFactoryId) {
        if (factory != null) {
            for (ITmfTrace trace : fInstances.keySet()) {
                Iterator<ITmfTreeDataProvider<? extends ITmfTreeDataModel>> iter = fInstances.get(trace).iterator();
                while (iter.hasNext()) {
                    ITmfTreeDataProvider<? extends ITmfTreeDataModel> dp = iter.next();
                    String factoryId = extractFactoryId(dp.getId());
                    if (passedFactoryId.equals(factoryId)) {
                        dp.dispose();
                        iter.remove();
                    }
                }
            }
        }

    }

    private static String extractFactoryId(String id) {
        String[] ids = id.split(DataProviderConstants.ID_SEPARATOR, 2);
        return ids.length > 1 ? ids[0] : id;
    }

}
