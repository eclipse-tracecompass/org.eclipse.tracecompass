# Trace Compass Server Developer Guide
<!-- TOC START -->
- [Trace Compass Server Developer Guide](#trace-compass-server-developer-guide)
  - [Introduction](#introduction)
  - [Key Concepts](#key-concepts)
    - [Analysis](#analysis)
    - [Configuration](#configuration)
    - [Configuration source](#configuration-source)
    - [Configuration source type](#configuration-source-type)
    - [Data Provider (Output)](#data-provider-output)
    - [Data Provider Configurator](#data-provider-configurator)
    - [Data Provider Factory](#data-provider-factory)
    - [Data Provider Descriptor (Output Descriptor)](#data-provider-descriptor-output-descriptor)
    - [Data Provider Manager](#data-provider-manager)
    - [Data Provider Type](#data-provider-type)
    - [Experiment](#experiment)
    - [Generic State system](#generic-state-system)
    - [Segment Store](#segment-store)
    - [Trace](#trace)
    - [Workspace](#workspace)
  - [Trace Compass Server Workspace](#trace-compass-server-workspace)
    - [Workspace Root](#workspace-root)
    - [`.metadata` Folder](#metadata-folder)
    - [`Tracing` Project folder](#tracing-project-folder)
      - [`Traces` Folder](#traces-folder)
      - [`Experiments` Folder](#experiments-folder)
      - [Supplementary `.tracing` Folder](#supplementary-tracing-folder)
      - [Finding the supplementary folder for a trace or experiment](#finding-the-supplementary-folder-for-a-trace-or-experiment)
  - [Implementing a custom trace server](#implementing-a-custom-trace-server)
  - [Implementing a new trace type](#implementing-a-new-trace-type)
  - [Component interactions](#component-interactions)
  - [Implementing a global configuration](#implementing-a-global-configuration)
    - [Implementing a configuration source](#implementing-a-configuration-source)
  - [Implementing an analysis module](#implementing-an-analysis-module)
    - [Taking advantage of the Analysis Framework](#taking-advantage-of-the-analysis-framework)
  - [Creating a data provider](#creating-a-data-provider)
    - [Creating an XY Data Provider](#creating-an-xy-data-provider)
      - [Supported query parameters](#supported-query-parameters)
    - [Creating a Time Graph Data Provider](#creating-a-time-graph-data-provider)
      - [Supported query parameters](#supported-query-parameters-1)
    - [Creating a Data Tree Data Provider](#creating-a-data-tree-data-provider)
      - [Supported query parameters](#supported-query-parameters-2)
    - [Virtual Table](#virtual-table)
      - [Supported query parameters](#supported-query-parameters-3)
  - [Using the data provider manager to access data providers](#using-the-data-provider-manager-to-access-data-providers)
    - [Implementing a Data Provider Factory](#implementing-a-data-provider-factory)
    - [Extension point](#extension-point)
    - [Using data provider factories with experiments](#using-data-provider-factories-with-experiments)
    - [Utilities](#utilities)
    - [Registering a data provider factory programmatically](#registering-a-data-provider-factory-programmatically)
      - [Implementing a facotry for single-instance data providers](#implementing-a-facotry-for-single-instance-data-providers)
      - [Implementing a facotry for multi-instance data providers](#implementing-a-facotry-for-multi-instance-data-providers)
      - [Grouping of data providers](#grouping-of-data-providers)
      - [Implementing a data provider without analysis module](#implementing-a-data-provider-without-analysis-module)
  - [Implementing a configurable data provider](#implementing-a-configurable-data-provider)
    - [Implementing a configuration source type](#implementing-a-configuration-source-type)
- [The configuration source type describes the input parameters to provide when to pass when creating a new data provider or global configuration.](#the-configuration-source-type-describes-the-input-parameters-to-provide-when-to-pass-when-creating-a-new-data-provider-or-global-configuration)
    - [Implementing a configurable data provider without analysis module](#implementing-a-configurable-data-provider-without-analysis-module)
      - [Implementing an `ITmfDataProviderConfigurator` without analysis module](#implementing-an-itmfdataproviderconfigurator-without-analysis-module)
      - [Updating data provider factory for configurable data provider (without analysis module)](#updating-data-provider-factory-for-configurable-data-provider-without-analysis-module)
      - [Updating data provider class for configurable data provider (without analysis module)](#updating-data-provider-class-for-configurable-data-provider-without-analysis-module)
    - [Implementing a configurable data provider with an analysis module](#implementing-a-configurable-data-provider-with-an-analysis-module)
      - [Implementing an `ITmfDataProviderConfigurator` with analysis module](#implementing-an-itmfdataproviderconfigurator-with-analysis-module)
      - [Creating a configurable analysis module](#creating-a-configurable-analysis-module)
      - [Updating data provider factory for configurable data provider (with analysis module)](#updating-data-provider-factory-for-configurable-data-provider-with-analysis-module)
      - [Updating data provider class for configurable data provider (with analysis module)](#updating-data-provider-class-for-configurable-data-provider-with-analysis-module)
    - [Future considerations](#future-considerations)
  - [To Do](#to-do)
<!-- TOC END -->

## Introduction

The purpose of Trace Compass trace server is to facilitate the integration of trace analysis and visualization using client applications that implement the `Trace Server Protocol`, for example, the `vscode-trace-extension` or `tsp-python-client`.

This guide goes over the internal components of the Trace Compass framework and how to add custom, domain specific features to it. It should help developers trying to add new capabilities (support for new trace type, new analysis or views, etc.) to the framework. End-users, using the RCP for example, should not have to worry about the concepts explained here.

## Key Concepts

### Analysis
An `analysis` module in Trace Compass is an entity that consumes trace events of a trace and/or experiment, performs some computation, for example CPU or memory usage, use state machines to track analysis states of a system, follows key characteristics stored trace events and much more. `analysis` modules may persist (e.g. using a state system) the results on disk so that those computations don't need to be re-done every time the data is requested.

### Configuration

A `configuration` is the data structure to configure and customize the trace server back-end globally (e.g. load XML analysis) or to configure data providers with parameters.

### Configuration source

A configuration source is a source of configurations. It is responsible to manage global configurations (e.g. create, delete, update etc).

### Configuration source type

A configuration source type describes the input parameters to provide when creating a new data provider or to configure the trace server globally.

### Data Provider (Output)

A `data provider` or `output` is the part that provides the data for visualizations. Each `experiment` will have to advertise what data providers are available for a given experiment. A data provider may or may not use an `analysis` module.

### Data Provider Configurator

A `data provider configurator` provides methods to create and delete derived data providers using a configuration. This can be used to parameterize an existing data provider, for example, cpu usage for given processes only.

### Data Provider Factory

A data provider factory's purpose is to create data provider instances that can be queried for the actual data structures. The factory has a method to return `data provider descriptors` that describe the data providers that the factory can create. A `data provider factory` can also be a `data provider configurator` or provide a separate data provider configurator class to be used for data provider configurations.

### Data Provider Descriptor (Output Descriptor)

This descriptor describes the data provider. This is information that clients will use to determine which endpoints (methods) to call, what data structures to expect, what capabilities the data provider has (e.g. can it be deleted or can it create derived data providers). It also has a name and description. Each data provider might have children data providers, which are either derived or just grouped together.

### Data Provider Manager

The `data provider manager` is a core entity in Trace Compass that can be used to manage data providers and its factories. It provides utilities to get all available data providers (descriptors), get or create data provider instances, register and deregister factories programmatically, remove data provider instance and more.

### Data Provider Type

The `provider type` determines what is the data structures the `data provider` returns and which methods are available to query the data provider. Such methods will have corresponding trace server endpoints.

### Experiment

An experiment consists of one or more [traces](#trace). Trace events from each trace are sorted in chronological order. The trace server works with experiments for providing data using data providers.

### Generic State system

A generic `state system` is a utility available in Trace Compass to track different states over the duration of a trace. A state system may be persisted to disk as `state history` as interval tree data structure.

### Segment Store

A segment store is similar to a state system, but it allows for overlapping intervals. This is useful to store any kind of intervals (e.g. latencies) over the duration of a trace.

### Trace

A trace is the integral part for the trace visualization. A trace consists of trace events. Each trace event has a timestamp and payload. The format of the trace is trace type specific. A parsers needs to be available in the trace server for each trace type.

### Workspace

The workspace (e.g. for trace server) is the place where the application stores application-specific data.

## Trace Compass Server Workspace

The Trace Compass workspace structure is based on the workspace of Eclipse platform. The workspace structure is an organized system that the Eclipse platform uses to manage `projects`, `settings`, and `metadata`. 

Trace Compass defines so-called tracing projects where trace files, experiments and other analysis persistent information is stored. By default, there is one tracing project called 'Tracing'. While it's possible to have multiple tracing projects the Trace Compass server has no API to create other tracing projects. The workspace structure of the `Trace Compass server` is the same as the workspace structure of the classic Trace Compass RCP application.

The following chapters describe the workspace of the Trace Compass trace server application. This folder structure is created when starting the server and should not be updated manually!

```python
<Workspace Root>                                       # Workspace root
  ├── .log/                                            # The error log
  ├── .lock/                                           # Lock file (when server is running)
  ├── .metadata                                        # Stores internal configuration and metadata.
      ├── .plugins/                                    # Plugin-specific data
      │       ├── org.eclipse.tracecompass.tmf.analysis.xml.core # xml.core plugin specific data
      │       │   └── xml_files                        # root folder for xml files
      │       │       ├── my-custom-analysis.xml       # example xml analysis   
      |       |       ├── ...
      │       ├── org.eclipse.tracecompass.tmf.core    # tmf.core plugin specific data
      │       │   ├── dataprovider.ini                 # file to hide/show data providers
      │       │   ├── markers.xml                      # definition of overlay markers
      │       │   └── markers.xsd                      # xsd for markers.xml 
      |       ├── org.eclipse.core.runtime             # Platform runtime configuraiton files
      |       │   └── .settings
      |       │       ├── org.eclipse.tracecompass.tmf.analysis.xml.core.prefs # xml.core preferences
      |       |       ├── org.eclipse.tracecompass.tmf.core.prefs              # tmf.core preferences 
      |       |       ├── ...
      │       ├── org.eclipse.core.resources           # Eclipse platform file system resources related files
      │       │   ├── .projects
      │       │   │   └── Tracing                      # Platform files for Tracing project
      │       │   │       ├── .indexes                 # Resources indexes
      |       |   |       ├── ... 
      |       |   ├── ...
      |       ├── ...   

```

### Workspace Root
This is the main folder where all Trace Compass projects, settings, and metadata are stored. By default, the `Trace Compass server` creates the workspace in the user's home folder under the name `.tracecompass-webapp` (e.g. `/home/<username>/.tracecompass-webapp`). 

One can change the workspace location when starting the Trace Compass server, by adding command-line parameter `-data <new path>` or updating the `tracecompass.ini` file of the server download package.

### `.metadata` Folder

This folder stores internal configuration and metadata. It will contain the following important files and directories (amongst others):

- `.log`: Contains the error logs
- `.lock`: Lock file indicating that workspace is in-use. Should only be there if server application is running. 
- `.plugins`: Each plug-in of the application may store configuration files under a folder with its plug-in name. For example, `org.eclipse.tracecompass.core` will store marker.xml, dataprovider.ini and other files. Those are internal and should not be updated manually.

### `Tracing` Project folder

There is only one project folder for the `Trace Compass server` with the name `Tracing`. It has the following structure:

```python
Tracing
  ├── .project                                                # Project metadata
  ├── Traces                                                  # Traces root folder
  |  ├── /<path-to-trace1>/<sym-link-name-of-trace1>          # Trace link for <name-of-trace1>
  |  ├── /<path-to-trace2>/<sym-link-name-of-trace2>          # Trace link for <name-of-trace2>
  ├── Experiments                                             # Experiments root folder
  |  ├── <experiment-name>                                    # Folder for experiment with name <experiment-name>
  |      ├── /<path-to-trace1>/<name-of-trace1>               # Trace path, matching path in Traces
  |      ├── /<path-to-trace2>/<name-of-trace2>               # Trace path, matching path in Traces
  ├── .tracing                                                # Supplementary folder
      ├── <experiment-name>-exp                               # Experiment specific supplementary folder
      |   ├── checkpoint_btree.idx                            # Experiment Btree checkpoint index
      |   ├── checkpoint_flatarray.idx                        # Experiment Flat array checkpoint index
      |   ├── <exp-analysis-state-system1.ht>                 # A state system
      |   ├── <...>                                           # config files and directories
      ├──  /<path-to-trace1>/<name-of-trace1>                 # Trace specific supplementary folder, matching path in Traces
      |                    ├── checkpoint_btree.idx           # Experiment Btree checkpoint index
      |                    ├── checkpoint_flatarray.idx       # Experiment Flat Array checkpoint index
      |                    ├── <analysis-state-system1.ht>    # A state system
      |                    ├── <analysis-state-system2.ht>    # Another state system
      |                    ├── <...>                          # More supplementary files and folders
      ├── /<path-to-trace2>/<name-of-trace2>                  # Trace supplementary folder, matching path in Traces
                           ├── checkpoint_btree.idx           # Experiment Btree checkpoint index
                           ├── checkpoint_flatarray.idx       # Experiment Flat Array checkpoint index
                           ├── <analysis-state-system1.ht>    # A state system
                           ├── <analysis-state-system2.ht>    # Another state system
                           ├── <...>                          # More supplementary files and folders
```

This folder structure is created and updated when opening a trace, experiment or configuring data providers. It's cleaned-up by the application when deleting a trace, experiment or configuration. This should not be updated manually!

#### `Traces` Folder

The `Traces` folder structure determines which traces are available on the server. To be unique, the path matches the file system path of the trace. The trace name (e.g. `name-of-trace1`) is a symlink to the trace file (or trace folder) in the file system. 

#### `Experiments` Folder
An experiment is identified by the experiment name in the `Experiment` folder (e.g. `experiment-name`). The contained traces are identified by the path to the trace matching the path in the `Traces` folder.

#### Supplementary `.tracing` Folder
Each experiment has a supplementary folder under the `.tracing` folder for experiment related data (e.g. experiment index, state systems, configuration files etc.). The name of the experiment supplementary folder is `<experiment-name>` with suffix `-exp`. 

Each trace has also a supplementary folder under the `tracing` folder. The path of the trace supplementary folder matches the of trace under the `Traces` folder. 

#### Finding the supplementary folder for a trace or experiment
When a trace is openend or a experiment is created the supplementary folder is created by the server application. It stores the path into persistent property of the `org.eclipse.core.resources.IFolder` representing the trace in the workspace. See below for the code snipped that sets the supplementary folder persistent property.

```java
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

    //...
    private static synchronized boolean createResource(String path, IResource resource) throws CoreException {

        // create the resource hierarchy.
        IPath targetLocation = new org.eclipse.core.runtime.Path(path);
        createFolder((IFolder) resource.getParent(), null);
        if (!ResourceUtil.createSymbolicLink(resource, targetLocation, true, null)) {
            return false;
        }

        // create supplementary folder on file system:
        IFolder supplRootFolder = resource.getProject().getFolder(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER_NAME);
        IFolder supplFolder = supplRootFolder.getFolder(resource.getProjectRelativePath().removeFirstSegments(1));
        createFolder(supplFolder, null);
        resource.setPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, supplFolder.getLocation().toOSString());

        return true;
    }
    //...
```

To retrieve the supplementary one can read the persistent property directy from the resource, or use the TmfTraceManager class to get it from the `ITmfTrace` object representing a trace or experiment.

```java
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

    //...
        // Trace
        ITmfTrace trace = getTrace();
        String supplPath = TmfTraceManager.getSupplementaryFileDir(trace);

        // Experiment
        TmfExperiment experiment = getExperiment();
        String expSupplPath = TmfTraceManager.getSupplementaryFileDir(experiment);
   //...
```

Note: 
- If the `trace.getResource()` returns null, `TmfTraceManager.getSupplementaryFileDir(trace)` will create a temporary folder under the OS's temp folder (for Linux /tmp/) and returns this directory. This folder will be temporary and every call with a null resource it will create a new one. Hence, it's not suitable for persistence.
- Always, use the Eclipse platform API to create resources and to set the persistence property.


## Implementing a custom trace server

To create a custom trace server based on Trace Compass, a custom Eclipse RCP application has to be defined that includes all relevant Trace Compass core plug-ins, core extensions (e.g. lttng.core, profiling.core) and all custom plug-ins for custom trace parsing and analysis. Don't include any plug-ins that requires UI. 

The best way to start is create a Trace Server RCP product application by copying the following [Trace Compass server product definition](https://github.com/eclipse-tracecompass-incubator/org.eclipse.tracecompass.incubator/tree/master/trace-server/org.eclipse.tracecompass.incubator.trace.server.product). 
 
Then you can modify the content of the `traceserver.product` file, e.g. rename server application, add and/or remove plug-ins.

## Implementing a new trace type

A trace type defines a parser for the raw trace file(s). It will parse the raw trace events and convert it to an internal data structure (`ITmfEvent`) which has a timestamp, event type and payload (content) with fields. Those events then can be further analysed in the trace server application. To add a new trace type follow the instructions in the [Trace Compass Developer Guide for Eclipse](https://archive.eclipse.org/tracecompass/doc/nightly/org.eclipse.tracecompass.doc.dev/Implementing-a-New-Trace-Type.html#Implementing_a_New_Trace_Type).

## Component interactions

Trace compass provides a mechanism for different components to interact with each other using signals. The signals can carry information that is specific to each signal. In the Trace Server only uses a subset of the available signals, where `TmfTraceOpenedSignal` and `TmfTraceClosedSignal` are the most important signals, some other signals are not applicable when running in the trace server context. See the [Trace Compass Developer Guide for Eclipse](https://archive.eclipse.org/tracecompass/doc/nightly/org.eclipse.tracecompass.doc.dev/Component-Interaction.html#Component_Interaction) for more information about signals and how to send and receive them.

## Implementing a global configuration

Global configurations are used to load configuration parameters that configures the trace server application. For example, one can load XML analysis defintions using the global configuartion interface.

### Implementing a configuration source

To implement a configuration source use the extension point for configuration source with id `org.eclipse.tracecompass.tmf.core.config`. The following example explains for the XML anlaysis available in the Trace Compass core code base.

```xml
<extension
         point="org.eclipse.tracecompass.tmf.core.config">
      <source
            id="org.eclipse.tracecompass.tmf.core.config.xmlsourcetype"
            class="org.eclipse.tracecompass.internal.tmf.analysis.xml.core.config.XmlConfigurationSource">
      </source>
   </extension>
</extension>
```

After that implement the `ITmfConfigurationSource` interface. Implement the following methods:

- `ITmfConfigurationSourceType getConfigurationSourceType()`
Return the configuration source type that this configuration source can handle. The configuration source type descibes the input parameter to pass when creating a global configuration. See chapter [Implementing a configuration source type](#implementing-a-configuration-source-type) about the class to return.
- `ITmfConfiguration create(ITmfConfiguration configuration)`
    This method is called to create a global configuration base on the input configuration. It returns a `ITmfConfiguration` instance. It is repsonsible to persist the configuration in memory and on disk so that it is available after a server restart. To persist to disk the plug-in state location of workspace can be used which is accessible using Eclipse platform APIs.
- `ITmfConfiguration update(String id, ITmfConfiguration configuration)`
    This method is called to update an existing configuration with new parameters.
- `ITmfConfiguration remove(String id)`
    This method is called to remove an existing configuration by ID. If the configuration exisits, this method is responsible to clean-up the peristed data. If, for example, analysis modules with state systems or other persisted data were created as result of an configuration, this method needs to make sure that those are clean-up.
- `List<ITmfConfiguration> getConfigurations()`
    Gets all configuration instances.
- `boolean contains(String id)`
    Method to check if a configuration with given ID exists 
- `ITmfConfiguration get(String id)` 
    Returns a configuration with given ID if it exists 
- `void dispose()`
    Disposes the configuration source

The below the code for the `XmlConfigurationSource`. Note it reuses utilities that already existed for the integration with Eclipse Trace Compass.

```java
/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.analysis.xml.core.config;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlAnalysisModuleSource;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSource;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;

import com.google.common.collect.ImmutableList;

/**
 * Implementation of {@link ITmfConfigurationSource} for managing data
 * driven-analysis in XML files.
 *
 * @author Bernd Hufmann
 */
public class XmlConfigurationSource implements ITmfConfigurationSource {

    private static final ITmfConfigurationSourceType fType;

    private static final String XML_ANALYSIS_TYPE_ID = "org.eclipse.tracecompass.tmf.core.config.xmlsourcetype"; //$NON-NLS-1$
    private static final String NAME = nullToEmptyString(Messages.XmlConfigurationSource_Name);
    private static final String DESCRIPTION = nullToEmptyString(Messages.XmlConfigurationSource_Description);
    private static final String PATH_KEY = "path"; //$NON-NLS-1$
    private static final String PATH_DESCRIPTION = nullToEmptyString(Messages.XmlConfigurationSource_PathDescription);
    private Map<String, ITmfConfiguration> fConfigurations = new ConcurrentHashMap<>();

    static {
        TmfConfigParamDescriptor.Builder descriptorBuilder = new TmfConfigParamDescriptor.Builder()
                .setKeyName(PATH_KEY)
                .setDescription(PATH_DESCRIPTION);

        fType = new TmfConfigurationSourceType.Builder()
                .setId(XML_ANALYSIS_TYPE_ID)
                .setDescription(DESCRIPTION)
                .setName(NAME)
                .setConfigParamDescriptors(ImmutableList.of(descriptorBuilder.build())).build();
    }

    /**
     * Default Constructor
     */
    @SuppressWarnings("null")
    public XmlConfigurationSource() {
        for (Entry<@NonNull String, @NonNull File> entry : XmlUtils.listFiles().entrySet()) {
            ITmfConfiguration config = createConfiguration(entry.getValue());
            fConfigurations.put(config.getId(), config);
        }
    }

    @Override
    public ITmfConfigurationSourceType getConfigurationSourceType() {
        return fType;
    }

    @Override
    public ITmfConfiguration create(Map<String, Object> parameters) throws TmfConfigurationException {
        return createOrUpdateXml(null, parameters);
    }

    @Override
    public @Nullable ITmfConfiguration get(String id) {
        return fConfigurations.get(id);
    }

    @Override
    public ITmfConfiguration update(String id, Map<String, Object> parameters) throws TmfConfigurationException {
        ITmfConfiguration config = fConfigurations.get(id);
        if (config == null) {
            throw new TmfConfigurationException("No such configuration with ID: " + id); //$NON-NLS-1$
        }
        return createOrUpdateXml(config, parameters);
    }

    @Override
    public @Nullable ITmfConfiguration remove(String id) {
        if (fConfigurations.get(id) == null) {
            return null;
        }

        if (!XmlUtils.listFiles().containsKey(id)) {
            return null;
        }

        XmlUtils.deleteFiles(ImmutableList.of(id));
        XmlUtils.saveFilesStatus();
        XmlAnalysisModuleSource.notifyModuleChange();
        return fConfigurations.remove(id);
    }

    @Override
    public List<ITmfConfiguration> getConfigurations() {
        return ImmutableList.copyOf(fConfigurations.values());
    }

    @Override
    public boolean contains(String id) {
        return fConfigurations.containsKey(id);
    }

    @Override
    public void dispose() {
        fConfigurations.clear();
    }

    private static @Nullable File getFile(Map<String, Object> parameters) {
        String path = (String) parameters.get(PATH_KEY);
        if (path == null) {
            return null;
        }
        return new File(path);
    }

    private ITmfConfiguration createOrUpdateXml(@Nullable ITmfConfiguration existingConfig, Map<String, Object> parameters) throws TmfConfigurationException {
        File file = getFile(parameters);
        if (file == null) {
            throw new TmfConfigurationException("Missing path"); //$NON-NLS-1$
        }

        ITmfConfiguration config = createConfiguration(file);

        IStatus status = XmlUtils.xmlValidate(file);
        if (status.isOK()) {
            if (existingConfig == null) {
                status = XmlUtils.addXmlFile(file);
            } else {
                if (!existingConfig.getId().equals(config.getId())) {
                    throw new TmfConfigurationException("File mismatch"); //$NON-NLS-1$
                }
                XmlUtils.updateXmlFile(file);
            }
            if (status.isOK()) {
                XmlAnalysisModuleSource.notifyModuleChange();
                XmlUtils.saveFilesStatus();
                fConfigurations.put(config.getId(), config);
                return config;
            }
        }
        String statusMessage = status.getMessage();
        String message = statusMessage != null? statusMessage : "Failed to update xml analysis configuration"; //$NON-NLS-1$
        if (status.getException() != null) {
            throw new TmfConfigurationException(message, status.getException());
        }
        throw new TmfConfigurationException(message);
    }

    @SuppressWarnings("null")
    private static String getName(String file) {
        return new Path(file).removeFileExtension().toString();
    }

    private static ITmfConfiguration createConfiguration(File file) {
        String id = file.getName();
        String name = getName(file.getName());
        String description = NLS.bind(Messages.XmlConfigurationSource_ConfigDescription, name);
        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(name)
                .setId(id)
                .setDescription(description.toString())
                .setSourceTypeId(XML_ANALYSIS_TYPE_ID);
       return builder.build();
    }
}
```

## Implementing an analysis module

To create analysis modules using the `Analysis Framework` follow the instructions in the Trace Compass developer guide. Those analysis modules are applicable to all traces of given trace type or all trace types. The can be executed automatically when open a trace or can be executed manually when opening a output. When a trace or experiment is opened (when handling the `TmfTraceOpenedSignal`) the `TmfTrace` instance will get all registered analysis modules from the `TmfAnalysisManager` that are applicable for the trace or trace type. It is also possible to add analysis modules programmatically using the `ITmfTrace.addAnalysisModule(ITmfAnalysisModule)`, which can be used, for example, for configurable analysis modules that created outside the analysis frame work (see [here](#implementing-an-itmfdataproviderconfigurator-with-analysis-module)). All analysis module instances stored in the `TmfTrace` will be disposed automatically when a trace is closed (upon reception of the `TmfTraceClosedSignal`).

### Taking advantage of the Analysis Framework

There are multiple utilities and classes that help to access the analysis modules for a trace. Use `TmfTraceUtils` or `TmfAnalysisManager` for that.

## Creating a data provider

The data provider interface aims to provide a standard data model for different types of views. 

Data providers are queried with a query parameters, which usually contains a time range as well as other parameters required to correctly filter and sample the returned data. They also take an optional progress monitor to cancel the task. The returned models are encapsulated in a `TmfModelResponse` object, which is generic (to the response's type) and also encapsulates the Status of the reponse:

* `CANCELLED` if the query was cancelled by the progress monitor
* `FAILED` if an error occurred inside the data provider
* `RUNNING` if the response was returned before the underlying analysis was completed, and querying the provider again with the same parameters can return a different model.
* `COMPLETED` if the underlying analysis is finished and we do not expect a different response for the query parameters.

`Note` that a complete example of analysis and data provider can be found in the [org.eclipse.tracecompass.examples.core plugin sources](https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/blob/master/doc/org.eclipse.tracecompass.examples.core) and [org.eclipse.tracecompass.examples.ui plugin sources](https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/blob/master/doc/org.eclipse.tracecompass.examples.ui) respectively.''

The Trace Compass server and the Trace Server Protocol is not aware of the concept `Analysis` in contrary to the Eclipse Trace Compass RCP application. The Trace Compass server however, uses data providers to visualize analysis results. The following chapters will go over how to generate different types of data providers and how to instantiate them in the trace server context. 

Each data provider factory created through those instructions has to implement the `IDataProviderFactory.getDescriptor(ITmfTrace)` method which returns a list of data provider descriptors that this factory can instantiate. This will make the data providers visible over the Trace Server Protocol (TSP).

### Creating an XY Data Provider
The XY data provider type is used to associate an XY series to an entry from the tree. The data provider is queried with a filter that also contains a Collection of the IDs of the entries for which we want XY series. The response contains a map of the series for the desired IDs.

Each XY series can have its own x axis (`ISeriesModel` / `SeriesModel` - encapsulated in an `ITmfXyModel` / `TmfXyModel`) or they can be shared by all models (`IYModel` / `YModel` encapsulated in an `ITmfCommonXAxisModel` / `TmfCommonXAxisModel`). The X axis is an array of longs, which makes it useful for a time axis or time buckets, but it can be used for any XY content.

The interface to implement is `ITmfTreeXYDataProvider`. Abstract base classes exist for common use case, e.g. `AbstractTreeCommonXDataProvider` or `AbstractTreeCommonXDataProvider` for tree data providers that are using a state system. Extend those classes if applicable.

Here is a simple example of XY data provider, retrieving data from a simple state system displaying the child attributes of the root attributes.

```java
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.examples.core.analysis.ExampleStateSystemAnalysisModule;
import org.eclipse.tracecompass.internal.tmf.core.model.tree.AbstractTreeDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.TmfCommonXAxisModel;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * An example of an XY data provider.
 *
 * @author Genevi\E8ve Bastien
 */
@SuppressWarnings("restriction")
@NonNullByDefault
public class ExampleXYDataProvider extends AbstractTreeDataProvider<ExampleStateSystemAnalysisModule, TmfTreeDataModel> implements ITmfTreeXYDataProvider<TmfTreeDataModel> {

    /**
     * Provider unique ID.
     */
    public static final String ID = "org.eclipse.tracecompass.examples.xy.dataprovider"; //$NON-NLS-1$
    private static final AtomicLong sfAtomicId = new AtomicLong();

    private final BiMap<Long, Integer> fIDToDisplayQuark = HashBiMap.create();

    /**
     * Constructor
     *
     * @param trace
     *            The trace this data provider is for
     * @param analysisModule
     *            The analysis module
     */
    public ExampleXYDataProvider(ITmfTrace trace, ExampleStateSystemAnalysisModule analysisModule) {
        super(trace, analysisModule);
    }

    /**
     * Create the time graph data provider
     *
     * @param trace
     *            The trace for which is the data provider
     * @return The data provider
     */
    public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(ITmfTrace trace) {
        ExampleStateSystemAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, ExampleStateSystemAnalysisModule.class, ExampleStateSystemAnalysisModule.ID);
        return module != null ? new ExampleXYDataProvider(trace, module) : null;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        // Make an entry for each base quark
        List<TmfTreeDataModel> entryList = new ArrayList<>();
        for (Integer quark : ss.getQuarks("CPUs", "*")) { //$NON-NLS-1$ //$NON-NLS-2$
            int statusQuark = ss.optQuarkRelative(quark, "Status"); //$NON-NLS-1$
            if (statusQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                Long id = fIDToDisplayQuark.inverse().computeIfAbsent(statusQuark, q -> sfAtomicId.getAndIncrement());
                entryList.add(new TmfTreeDataModel(id, -1, ss.getAttributeName(quark)));
            }
        }
        return new TmfTreeModel<>(Collections.emptyList(), entryList);
    }

    @Override
    public TmfModelResponse<ITmfXyModel> fetchXY(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = getAnalysisModule().getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        Map<Integer, double[]> quarkToValues = new HashMap<>();
        // Prepare the quarks to display
        Collection<Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (selectedItems == null) {
            // No selected items, take them all
            selectedItems = fIDToDisplayQuark.keySet();
        }
        List<Long> times = getTimes(ss, DataProviderParameterUtils.extractTimeRequested(fetchParameters));
        for (Long id : selectedItems) {
            Integer quark = fIDToDisplayQuark.get(id);
            if (quark != null) {
                quarkToValues.put(quark, new double[times.size()]);
            }
        }
        long[] nativeTimes = new long[times.size()];
        for (int i = 0; i < times.size(); i++) {
            nativeTimes[i] = times.get(i);
        }

        // Query the state system to fill the array of values
        try {
            for (ITmfStateInterval interval : ss.query2D(quarkToValues.keySet(), times)) {
                if (monitor != null && monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                double[] row = quarkToValues.get(interval.getAttribute());
                Object value = interval.getValue();
                if (row != null && (value instanceof Number)) {
                    Double dblValue = ((Number) value).doubleValue();
                    for (int i = 0; i < times.size(); i++) {
                        Long time = times.get(i);
                        if (interval.getStartTime() <= time && interval.getEndTime() >= time) {
                            row[i] = dblValue;
                        }
                    }
                }
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }
        List<IYModel> models = new ArrayList<>();
        for (Entry<Integer, double[]> values : quarkToValues.entrySet()) {
            models.add(new YModel(fIDToDisplayQuark.inverse().getOrDefault(values.getKey(), -1L), values.getValue()));
        }

        return new TmfModelResponse<>(new TmfCommonXAxisModel("Example XY data provider", nativeTimes, models), Status.COMPLETED, CommonStatusMessage.COMPLETED); //$NON-NLS-1$
    }

    private static List<Long> getTimes(ITmfStateSystem key, @Nullable List<Long> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        List<@NonNull Long> times = new ArrayList<>();
        for (long t : list) {
            if (key.getStartTime() <= t && t <= key.getCurrentEndTime()) {
                times.add(t);
            }
        }
        Collections.sort(times);
        return times;
    }

}
```

Note that it possible to indicate to the client if this series should be shown by default. To achieve this use the class `TmfXyTreeDataModel` instead of `TmfTreeDataModel` for the tree model which has a default flag and set it to true.

#### Supported query parameters

This chapter will describe the query parameters supported by all the `TREE_TIME_XY` data providers. It will only describe the parameters that have corresponding parameters in the [Trace Server Protocol](https://eclipse-cdt-cloud.github.io/trace-server-protocol) which are passed by a client to the trace server. Other parameters that certain data provider implementation support and the classic Eclipse UI is using won't be mentioned here, because they are not exposed as part of the `Trace Server Protocol`. Note that the parameter names below are the parameters internal to Trace Compass and not the names always the names that are defined in the `Trace Server Protocol` 

Parameters are passed as a `Map<String, Object>` where the key is the parameter name in the tables below.

**Parameters to `fetchTree(Map<String, Object> parameters, IProgressMonitor monitor)`:**
| Parameter name | Description|
|:---------------| :----------|
| `requested_times`| Array of longs for timestamps, 2 values, for start and end of range |


**Parameters to `fetchXY(Map<String, Object> parameters, IProgressMonitor monitor)`:**
| Parameter name | Description|
|:---------------| :----------|
| `requested_times`| Array of long for timestamps to be sampled|
| `requested_items`| Array of integer, IDs of xy series returned by fetchTree() above |


### Creating a Time Graph Data Provider

The Time Graph data provider is used to associate states to tree entries, i.e. a sampled list of states, with a start time, duration, integer value and optional label. The time graph states (`ITimeGraphState` / `TimeGraphState`) are encapsulated in an `ITimeGraphRowModel` which also provides the ID of the entry they map to.

The time graph data provider can also supply arrows to link entries one to another with a start time and start ID as well as a duration and target ID. The interface to implement is `ITimeGraphArrow`, else `TimeGraphArrow` can be extended.

Additional information can be added to the states with tooltips, which are maps of tooltip entry names to tooltip entry values. The data provider may also suggest styles for the states by implementing the `IOutputStyleProvider` interface.

The interface to implement is `ITimeGraphDataProvider`.

Also, if the data provider wants to provide some styling information, for example, colors, height and opacity, etc, it can implement the `IOutputStyleProvider` interface who will add a method to fetch styles. The `TimeGraphState` objects can then be constructed with a style and the view will automatically use this style information.

If the data provider wants to provide some annotations, for example, symbols on rows or view-wide background duration layers, it can implement the `IOutputAnnotationProvider` interface who will add methods to fetch annotation categories and fetch annotations (per category).

Here is a simple example of a time graph data provider, retrieving data from a simple state system where each root attribute is to be displayed. It also provides simple styling. No annotations are implemented here, see [ThreadStatusDataProvider](https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/blob/master/analysis/org.eclipse.tracecompass.analysis.os.linux.core/src/org/eclipse/tracecompass/internal/analysis/os/linux/core/threadstatus/ThreadStatusDataProvider.java) for an example implementation of the `IOutputAnnotationProvider` interface.

```java
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.examples.core.analysis.ExampleStateSystemAnalysisModule;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * An example of a time graph data provider.
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
@NonNullByDefault
public class ExampleTimeGraphDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel>, IOutputStyleProvider {

    /**
     * Provider unique ID.
     */
    public static final String ID = "org.eclipse.tracecompass.examples.timegraph.dataprovider"; //$NON-NLS-1$
    private static final AtomicLong sfAtomicId = new AtomicLong();
    private static final String STYLE0_NAME = "style0"; //$NON-NLS-1$
    private static final String STYLE1_NAME = "style1"; //$NON-NLS-1$
    private static final String STYLE2_NAME = "style2"; //$NON-NLS-1$

    /* The map of basic styles */
    private static final Map<String, OutputElementStyle> STATE_MAP;
    /*
     * A map of styles names to a style that has the basic style as parent, to
     * avoid returning complete styles for each state
     */
    private static final Map<String, OutputElementStyle> STYLE_MAP;

    static {
        /* Build three different styles to use as examples */
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();

        builder.put(STYLE0_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE0_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("blue")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 0.5f,
                StyleProperties.OPACITY, 0.75f)));
        builder.put(STYLE1_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE1_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("yellow")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 1.0f,
                StyleProperties.OPACITY, 1.0f)));
        builder.put(STYLE2_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE2_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("green")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 0.75f,
                StyleProperties.OPACITY, 0.5f)));
        STATE_MAP = builder.build();

        /* build the style map too */
        builder = new ImmutableMap.Builder<>();
        builder.put(STYLE0_NAME, new OutputElementStyle(STYLE0_NAME));
        builder.put(STYLE1_NAME, new OutputElementStyle(STYLE1_NAME));
        builder.put(STYLE2_NAME, new OutputElementStyle(STYLE2_NAME));
        STYLE_MAP = builder.build();
    }

    private final BiMap<Long, Integer> fIDToDisplayQuark = HashBiMap.create();
    private ExampleStateSystemAnalysisModule fModule;

    /**
     * Constructor
     *
     * @param trace
     *            The trace this analysis is for
     * @param module
     *            The scripted analysis for this data provider
     */
    public ExampleTimeGraphDataProvider(ITmfTrace trace, ExampleStateSystemAnalysisModule module) {
        super(trace);
        fModule = module;
    }

    /**
     * Create the time graph data provider
     *
     * @param trace
     *            The trace for which is the data provider
     * @return The data provider
     */
    public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(ITmfTrace trace) {
        ExampleStateSystemAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, ExampleStateSystemAnalysisModule.class, ExampleStateSystemAnalysisModule.ID);
        return module != null ? new ExampleTimeGraphDataProvider(trace, module) : null;
    }

    @Override
    public TmfModelResponse<TmfTreeModel<@NonNull ITimeGraphEntryModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        fModule.waitForInitialization();
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        boolean isComplete = ss.waitUntilBuilt(0);
        long endTime = ss.getCurrentEndTime();

        // Make an entry for each base quark
        List<ITimeGraphEntryModel> entryList = new ArrayList<>();
        for (Integer quark : ss.getQuarks("CPUs", "*")) { //$NON-NLS-1$ //$NON-NLS-2$
            Long id = fIDToDisplayQuark.inverse().computeIfAbsent(quark, q -> sfAtomicId.getAndIncrement());
            entryList.add(new TimeGraphEntryModel(id, -1, ss.getAttributeName(quark), ss.getStartTime(), endTime));
        }

        Status status = isComplete ? Status.COMPLETED : Status.RUNNING;
        String msg = isComplete ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING;
        return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), entryList), status, msg);
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    public @NonNull TmfModelResponse<TimeGraphModel> fetchRowModel(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        try {
            List<@NonNull ITimeGraphRowModel> rowModels = getDefaultRowModels(fetchParameters, ss, monitor);
            if (rowModels == null) {
                rowModels = Collections.emptyList();
            }
            return new TmfModelResponse<>(new TimeGraphModel(rowModels), Status.COMPLETED, CommonStatusMessage.COMPLETED);
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }
    }

    private @Nullable List<ITimeGraphRowModel> getDefaultRowModels(Map<String, Object> fetchParameters, ITmfStateSystem ss, @Nullable IProgressMonitor monitor) throws IndexOutOfBoundsException, TimeRangeException, StateSystemDisposedException {
        Map<Integer, ITimeGraphRowModel> quarkToRow = new HashMap<>();
        // Prepare the quarks to display
        Collection<Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (selectedItems == null) {
            // No selected items, take them all
            selectedItems = fIDToDisplayQuark.keySet();
        }
        for (Long id : selectedItems) {
            Integer quark = fIDToDisplayQuark.get(id);
            if (quark != null) {
                quarkToRow.put(quark, new TimeGraphRowModel(id, new ArrayList<>()));
            }
        }

        // This regex map automatically filters or highlights the entry
        // according to the global filter entered by the user
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        // Query the state system to fill the states
        long currentEndTime = ss.getCurrentEndTime();
        for (ITmfStateInterval interval : ss.query2D(quarkToRow.keySet(), getTimes(ss, DataProviderParameterUtils.extractTimeRequested(fetchParameters)))) {
            if (monitor != null && monitor.isCanceled()) {
                return Collections.emptyList();
            }
            ITimeGraphRowModel row = quarkToRow.get(interval.getAttribute());
            if (row != null) {
                List<@NonNull ITimeGraphState> states = row.getStates();
                ITimeGraphState timeGraphState = getStateFromInterval(interval, currentEndTime);
                // This call will compare the state with the filter predicate
                applyFilterAndAddState(states, timeGraphState, row.getEntryID(), predicates, monitor);
            }
        }
        for (ITimeGraphRowModel model : quarkToRow.values()) {
            model.getStates().sort(Comparator.comparingLong(ITimeGraphState::getStartTime));
        }

        return new ArrayList<>(quarkToRow.values());
    }

    private static TimeGraphState getStateFromInterval(ITmfStateInterval statusInterval, long currentEndTime) {
        long time = statusInterval.getStartTime();
        long duration = Math.min(currentEndTime, statusInterval.getEndTime() + 1) - time;
        Object o = statusInterval.getValue();
        if (!(o instanceof Long)) {
            // Add a null state
            return new TimeGraphState(time, duration, Integer.MIN_VALUE);
        }
        String styleName = "style" + ((Long) o) % 3; //$NON-NLS-1$
        return new TimeGraphState(time, duration, String.valueOf(o), STYLE_MAP.get(styleName));
    }

    private static Set<Long> getTimes(ITmfStateSystem key, @Nullable List<Long> list) {
        if (list == null) {
            return Collections.emptySet();
        }
        Set<@NonNull Long> times = new HashSet<>();
        for (long t : list) {
            if (key.getStartTime() <= t && t <= key.getCurrentEndTime()) {
                times.add(t);
            }
        }
        return times;
    }

    @Override
    public @NonNull TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        /**
         * If there were arrows to be drawn, this is where they would be defined
         */
        return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        /**
         * If there were tooltips to be drawn, this is where they would be
         * defined
         */
        return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STATE_MAP), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

}
```

#### Supported query parameters

This chapter will describe the query parameters supported by all the `TimeGraph` data providers. It will only describe the parameters that have corresponding parameters in the [Trace Server Protocol](https://eclipse-cdt-cloud.github.io/trace-server-protocol) which are passed by a client to the trace server. Other parameters that certain data provider implementation support and the classic Eclipse UI is using won't be mentioned here, because they are not exposed as part of the `Trace Server Protocol`. Note that the parameter names below are the parameters internal to Trace Compass and not the names always the names that are defined in the `Trace Server Protocol`.

Parameters are passed as a `Map<String, Object>` where the key is the parameter name in the tables below.

**Parameter to `fetchTree(Map<String, Object> parameters, IProgressMonitor monitor)`:**

| Parameter name | Description|
|:---------------| :----------|
| `requested_times`| Array of longs for timestamps, 2 values, for start and end of range |

**Parameters to `fetchRowModel(Map<String, Object> parameters, IProgressMonitor monitor)`:**

| Parameter name    | Description|
|:----------------- | :----------|
| `requested_times`   | Array of long for timestamps to be sampled|
| `requested_items`   | Array of integer, IDs of time graph rows returned by fetchTree() above |
| `regex_map_filters` | Optional map key to list of regular expressions. Where key "1" are for dimmed states and "4" for excluded states. States returned by the fetchRowModel() will have the `tag` bit mask set accordingly. |
| `full_search`       | `true` for deep search where the full time range between the first and last requested timestamp should be searched for filter matches. `false` for sampled search where only the `requested_times` will be searched. Return only one matching state per gap in requested timestamps needs to be returned in the response. |

**Parameters to `fetchArrows(Map<String, Object> parameters, IProgressMonitor monitor)`:**

| Parameter name    | Description|
|:----------------- | :----------|
| `requested_times`   | Array of long for timestamps to be sampled|

**Parameters to `fetchTooltip(Map<String, Object> parameters, IProgressMonitor monitor)`:**

| Parameter name    | Description|
|:----------------- | :----------|
| `requested_times`   | Array of long. Should contain only one value for the timestamp the tooltip to be calculated |
| `requested_items`   | Array of int. Should contain only one value for row ID the tooltip to be calculated |
| `requested_element` | The Object, the tooltip to be calculated. Supported objects: `TimeGraphState`, `TimeGraphArrow` or `Annotation` |

If the time graph data provider implements `IOutputStyleProvider` the following parameters need to be added to the fetch methods:

**Parameters to `fetchStyle(Map<String, Object> parameters, IProgressMonitor monitor)`:**

No parameters need to be added. Just pass an empty map.

If the time graph data provider implements `IOutputAnnotationProvider` the following parameters need to be added to the fetch methods:

**Parameters to `fetchAnnotationCategories(Map<String, Object> parameters, IProgressMonitor monitor)`:**
No parameters need to be added. Just pass an empty map.

**Parameters to `fetchAnnotations(Map<String, Object> parameters, IProgressMonitor monitor)`:**
| Parameter name    | Description|
|:----------------- | :----------|
| `requested_times`   | Array of long for timestamps to be sampled|
| `requested_items`   | Array of integer, IDs of time graph rows returned by fetchTree() above |
| `requested_marker_categories` | Array of category strings returned by `fetchAnnotationCategories`() above. Only annotations of the category shall be present in the returned annotation model |


### Creating a Data Tree Data Provider

A `DATA_TREE` data provider is used to display trees with columns, which can be used for statistics. The data provider returns a `TmfTreeDataModel` the same way as the XY or Time Graph tree.

Here is an example of such data provider. This time, the example just implements the `ITmfTreeDataProvider`.

```java
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
package org.eclipse.tracecompass.examples.core.data.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfLostEvent;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Simple events statistics data provider
 * 
 * @author Bernd Hufmann
 */
@SuppressWarnings("null")
@NonNullByDefault
public class ExampleEventsStatisticsDataProvider implements ITmfTreeDataProvider<TmfTreeDataModel> {
    private static long fCount = 0;

    private @Nullable ITmfTrace fTrace;
    private @Nullable StatsPerTypeRequest fRequest;
    private @Nullable List<TmfTreeDataModel> fCachedResult = null;

    /**
     * Constructor
     * @param trace
     *          the trace (not experiment)
     */
    public ExampleEventsStatisticsDataProvider(ITmfTrace trace) {
        fTrace = trace;
    }

    @Override
    public @NonNull String getId() {
        return "org.eclipse.tracecompass.examples.nomodulestats"; //$NON-NLS-1$
    }

    @Override
    public @NonNull TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {

        ITmfTrace trace = fTrace;
        if (trace == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        StatsPerTypeRequest request = fRequest;
        if (request == null) {
            // Start new request
            request = new StatsPerTypeRequest(trace, TmfTimeRange.ETERNITY);
            trace.sendRequest(request);
            TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), Collections.emptyList()); //$NON-NLS-1$ //$NON-NLS-2$
            fRequest = request;
            return new TmfModelResponse<>(model, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        if (request.isCancelled()) {
            TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), Collections.emptyList()); //$NON-NLS-1$ //$NON-NLS-2$
            return new TmfModelResponse<>(model, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
        }

        if (!request.isCompleted()) {
            TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), Collections.emptyList()); //$NON-NLS-1$ //$NON-NLS-2$
            return new TmfModelResponse<>(model, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        List<TmfTreeDataModel> values = fCachedResult;
        if (values == null) {
            long traceId = fCount++;
            values = new ArrayList<>();
            long total = 0;
            for (Entry<String, Long> entry : request.getResults().entrySet()) {
                values.add(new TmfTreeDataModel(fCount++, traceId, List.of(entry.getKey(), String.valueOf(entry.getValue()))));
                total += entry.getValue();
            }
            TmfTreeDataModel traceEntry = new TmfTreeDataModel(traceId, -1, List.of(trace.getName(), String.valueOf(total)));
            values.add(0, traceEntry);
            fCachedResult = values;
        }
        TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), values); //$NON-NLS-1$ //$NON-NLS-2$
        return new TmfModelResponse<>(model, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private class StatsPerTypeRequest extends TmfEventRequest {

        /* Map in which the results are saved */
        private final Map<@NonNull String, @NonNull Long> stats;

        public StatsPerTypeRequest(ITmfTrace trace, TmfTimeRange range) {
            super(trace.getEventType(), range, 0, ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.BACKGROUND);
            this.stats = new HashMap<>();
        }

        public Map<@NonNull String, @NonNull Long> getResults() {
            return stats;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            if (event.getTrace() == fTrace) {
                String eventType = event.getName();
                /*
                 * Special handling for lost events: instead of counting just
                 * one, we will count how many actual events it represents.
                 */
                if (event instanceof ITmfLostEvent) {
                    ITmfLostEvent le = (ITmfLostEvent) event;
                    incrementStats(eventType, le.getNbLostEvents());
                    return;
                }

                /* For standard event types, just increment by one */
                incrementStats(eventType, 1L);
            }
        }

        private void incrementStats(@NonNull String key, long count) {
            stats.merge(key, count, Long::sum);
        }
    }

    @Override
    public void dispose() {
        fRequest = null;
        fCachedResult = null;
    }
}
```

#### Supported query parameters

This chapter will describe the query parameters supported by all the `DATA_TREE` data providers. It will only describe the parameters that have corresponding parameters in the [Trace Server Protocol](https://eclipse-cdt-cloud.github.io/trace-server-protocol) which are passed by a client to the trace server. Other parameters that certain data provider implementation support and the classic Eclipse UI is using won't be mentioned here, because they are not exposed as part of the `Trace Server Protocol`. Note that the parameter names below are the parameters internal to Trace Compass and not the names always the names that are defined in the `Trace Server Protocol`.

Parameters are passed as a `Map<String, Object>` where the key is the parameter name in the tables below.

**Parameters to `fetchTree(Map<String, Object> parameters, IProgressMonitor monitor)`:**

| Parameter name   | Description|
|:-----------------| :----------|
| `isFiltered`     | Optional flag. `true` if data for `requested_times` should return. If `false` or ommitted return data for the whole trace| 
| `requested_times`| Array of longs for timestamps, 2 values, for start and end of range |


### Virtual Table

Another data provider type is the virtual table data provider for large data sets. See the [TmfEventTableDataProvider](https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/blob/master/tmf/org.eclipse.tracecompass.tmf.core/src/org/eclipse/tracecompass/internal/provisional/tmf/core/model/events/TmfEventTableDataProvider.java) for an example of such data provider.

#### Supported query parameters

This chapter will describe the query parameters supported by all the `VIRTUAL_TABLE` data providers. It will only describe the parameters that have corresponding parameters in the [Trace Server Protocol](https://eclipse-cdt-cloud.github.io/trace-server-protocol) which are passed by a client to the trace server. Other parameters that certain data provider implementation support and the classic Eclipse UI is using won't be mentioned here, because they are not exposed as part of the `Trace Server Protocol`. Note that the parameter names below are the parameters internal to Trace Compass and not the names always the names that are defined in the `Trace Server Protocol`.

Note `fetchTree()` is used to get the columns model. The returned model `TmfTreeModel` will contain desription of each column. The ID will represent the column ids.

Parameters are passed as a `Map<String, Object>` where the key is the parameter name in the tables below.

**Parameters to `fetchTree(Map<String, Object> parameters, IProgressMonitor monitor)`:**

No parameters need to be added. Just pass an empty map.

**Parameters to `fetchLines(Map<String, Object> parameters, IProgressMonitor monitor)`:**

| Parameter name | Description|
|:---------------| :----------|
| `requested_table_index` | The start index of event in the table to query from (if requested_times is omitted) | 
| `requested_times`       | Array of longs for timestamps, only on value of start timestamp to query from. The event with that time or nearest following shall be returned(if rquested_table_index is omitted) |
| `requested_table_count` | is number lines (rows) that shall be returned |
| `requested_table_column_ids` | Optional list of column IDs (return by fetchTree() above) to return. If omitted the all columns are returned.
| `table_search_expressions` | For searching providing a map <columnId, regular expression> Returned lines that match the search expression will be taggged by setting the highlight bit (8) in the properties bit mask of the return line model |
| `table_search_direction` | Optional, the search direction string NEXT or PREVIOUS. If omitted and `table_search_expressions` exists then NEXT will be used|

## Using the data provider manager to access data providers

Data providers can be managed by the `DataProviderManager` class, which uses an [extension point](#extension-point) and factories for data providers. Factories can also programatically be registered (and deregistered) to (from) the `DataProviderManager`, see [here](#registering-a-data-provider-factory-programmatically for more details.

This manager associates a unique data provider per trace and extension point ID, ensuring that data providers can be reused and that each entry for a trace reuses the same unique entry ID.

The manager uses the registered data provider factories to get all available data providers for a trace and to get or create data provider instances based on the ID provided by the descriptors.

```java
    //...
        ITmfTrace experiment = getExperiment();
        List<IDataProviderDescriptors> descriptors = DataProviderManager.getAvailableProviders(experiment);

        // fetch create the thread status data provider
        String outputId = ThreadStatusDataProvider.ID;
        ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = 
            manager.fetchOrCreateDataProvider(experiment,
                                              outputId, 
                                              ITimeGraphDataProvider.class);
            String outputId = ThreadStatusDataProvider.ID;

        // fetch the existing data provider instance (created above)
        ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider2 = 
            manager.fetchExistingCreateDataProvider(experiment,
                                                    outputId, 
                                                    ITimeGraphDataProvider.class);
    //...

```
The Trace Compass server will use this method for the corresponding endpoint to get all available data providers.

### Implementing a Data Provider Factory

The data provider manager requires a factory for the various data providers, to create the data provider instances for a trace. Here is an example of factory class to create the time graph data provider of the previous section.

```java
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.examples.core.analysis.ExampleStateSystemAnalysisModule;
import org.eclipse.tracecompass.internal.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfTreeXYCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * An example of a time graph data provider factory.
 *
 * This factory is also in the developer documentation of Trace Compass. If it is
 * modified here, the doc should also be updated.
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class ExampleTimeGraphProviderFactory implements IDataProviderFactory {
    //...
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ExampleTimeGraphDataProvider.ID)
            .setName("Example time graph data provider") //$NON-NLS-1$
            .setDescription("This is an example of a time graph data provider using a state system analysis as a source of data") //$NON-NLS-1$
            .setProviderType(ProviderType.TIME_GRAPH)
            .build();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        Collection<@NonNull ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        if (traces.size() == 1) {
            return ExampleTimeGraphDataProvider.create(trace);
        }
        return TmfTimeGraphCompositeDataProvider.create(traces, ExampleTimeGraphDataProvider.ID); //$NON-NLS-1$
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        ExampleStateSystemAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, ExampleStateSystemAnalysisModule.class, ExampleStateSystemAnalysisModule.ID);
        return module != null ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }
}
```
### Extension point
This extension needs to be added to the plugin's plugin.xml file:

```xml
<extension point="org.eclipse.tracecompass.tmf.core.dataprovider">
    <dataProviderFactory
         class="org.eclipse.tracecompass.examples.core.data.provider.ExampleTimeGraphProviderFactory"
         id="org.eclipse.tracecompass.examples.timegraph.dataprovider">
    </dataProviderFactory>
    <dataProviderFactory
         class="org.eclipse.tracecompass.examples.core.data.provider.ExampleXYDataProviderFactory"
         id="org.eclipse.tracecompass.examples.xy.dataprovider">
    </dataProviderFactory>
</extension>
```

### Using data provider factories with experiments

The Trace Compass framework allows to use the `DataProviderFactory` with single traces or experiments. The trace server will always create experiments even if a trace is a single trace.

In the data provider manager, experiments also get a unique instance of a data provider, which can be specific or encapsulate the data providers from the child traces. For example, an experiment can have its own concrete data provider when required (an analysis that runs only on experiments), or the factory would create a `CompositeDataProvider` (using `TmfTreeXYCompositeDataProvider` or `TmfTimeGraphCompositeDataProvider`) encapsulating the providers from its traces. The benefit of encapsulating the providers from child traces is that their entries/IDs can be reused, limiting the number of created objects and ensuring consistency in views. These composite data providers dispatch the request to all the encapsulated providers and aggregates the results into the expected data structure.

 The Data Provider Factories will have make sure that:
- the getDescriptor(ITmfTrace) returns only a single data provider descriptor for the same type
- it creates composite data provider instances (e.g. `TmfTimeGraphCompositeDataProvider`) with an array of data providers for each applicable sub-trace

### Utilities

Abstract base classes are provided for TreeXY and time graph data providers based on `TmfStateSystemAnalysisModule`s (`AbstractTreeCommonXDataProvider` and `AbstractTimeGraphDataProvider`, respectively). They handle concurrency, mapping of state system attributes to unique IDs, exceptions, caching and encapsulating the model in a response with the correct status.

### Registering a data provider factory programmatically

The most common way to register data provider factories is using the extension point as described above. However, the `DataProviderManager` has APIs to register and deregister factories programmatically. This allows to manage the lifecycle of custom data provider factories from extension code.

```java
    //...

    // Register a custom factory
    IDataProviderFactory factory = createCustomFactory();
    DataProviderManager.addDataProviderFactory("my.custom.data.providater.factory");

    // Deregister a custom factory
    DataProviderManager.removeDataProviderFactory("my.custom.data.providater.factory");
    //..
```

#### Implementing a facotry for single-instance data providers
If you would like to create a `DataProviderFactory` for a data provider that is using one single analysis module, you can get the analysis module from the trace as shown in the example below.

```java
    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        KernelAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
        IDataProviderDescriptor descriptor = new DataProviderDescriptor.Builder()
                .setId(ThreadStatusDataProvider.ID)
                .setName(Objects.requireNonNull(Messages.ThreadStatusDataProviderFactory_title))
                .setDescription(Objects.requireNonNull(Messages.ThreadStatusDataProviderFactory_descriptionText))
                .setProviderType(ProviderType.TIME_GRAPH)
                .build();
        return module != null ? Collections.singletonList(descriptor) : Collections.emptyList();
    }
```

Note that the passed trace can be an experiment that contains one or more traces. For that make sure that a given data provider descriptor is only returned once. For example, if the experiment has 2 kernel traces, then return the `ThreadStatusDataprovider` descriptor only once. The code above will take care of it because method `KernelAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID);` returns the first kernel analysis module found. If you where to use `Iterator<KernelAnalysisModule> iter = TmfTraceUtils.getAnalysisModulesOfClass(trace, KernelAnalysisModule.class)` it would return multiple modules if there are multiple kernel traces in the experiment.

```java
    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        Iterable<KernelAnalysisModule> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, KernelAnalysisModule.class);
        List<IDataProviderDescriptor> descriptors = new ArrayList<>();
        Set<String> existingModules = new HashSet<>();
        for (ISegmentStoreProvider module : modules) {
            IAnalysisModule analysis = (IAnalysisModule) module;
            // Only add analysis once per trace (which could be an experiment)
            if (!existingModules.contains(analysis.getId())) {
                IDataProviderDescriptor descriptor = getDataProviderDescriptor(analysis);
                if (descriptor != null) {
                    descriptors.add(descriptor);
                    existingModules.add(analysis.getId());
                }
            }
        }
        return descriptors;
    }
```

With this data provider descriptor the factory needs to implement only the `IDataProviderFactory.createDataProvider(ITmfTrace)`.

```java
    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createDataProvider(@NonNull ITmfTrace trace) {
        if (trace instanceof TmfExperiment) {
            return TmfTimeGraphCompositeDataProvider.create(TmfTraceManager.getTraceSet(trace), ThreadStatusDataProvider.ID);
        }
        KernelAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
        if (module != null) {
            module.schedule();
            return new ThreadStatusDataProvider(trace, module);
        }

        return null;
    }
```

#### Implementing a facotry for multi-instance data providers

For use cases that the `IDataProviderFactory` instance can create multiple instances of a data provider. One example is, a data provider that can be used with different analysis modules or configurations. For this case the ID of the `IDataProviderDescriptor` has to be created differently. It is required to concatenate the data provider factory ID and the analysis module ID separated by `:`.

```java
    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        Iterable<IFlameChartProvider> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, IFlameChartProvider.class);
        List<IDataProviderDescriptor> descriptors = new ArrayList<>();
        Set<String> existingModules = new HashSet<>();
        for (IFlameChartProvider module : modules) {
            IAnalysisModule analysis = module;
            // Only add analysis once per trace (which could be an experiment)
            if (!existingModules.contains(analysis.getId())) {
                DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
                builder.setId(FlameChartDataProvider.ID + DataProviderConstants.ID_SEPARATOR + analysis.getId())
                        .setParentId(analysis.getConfiguration() != null ? analysis.getId() : null)
                        .setName(Objects.requireNonNull(analysis.getName() + " - " +Messages.FlameChartDataProvider_Title)) //$NON-NLS-1$
                        .setDescription(Objects.requireNonNull(NLS.bind(Messages.FlameChartDataProvider_Description, analysis.getHelpText())))
                        .setProviderType(ProviderType.TIME_GRAPH)
                        .setConfiguration(analysis.getConfiguration());
                descriptors.add(builder.build());
                existingModules.add(analysis.getId());
            }
        }
        return descriptors;
    }
```

With this data provider descriptor the factory needs to implement the `IDataProviderFactory.createDataProvider(ITmfTrace, String secondaryId)`.

The secondary ID in the example above is the analysis ID.

```java
    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        return null;
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace, String secondaryId) {
        if (trace instanceof TmfExperiment) {
            return TmfTimeGraphCompositeDataProvider.create(TmfTraceManager.getTraceSet(trace), FlameChartDataProvider.ID, secondaryId);
        }
        IFlameChartProvider module = TmfTraceUtils.getAnalysisModuleOfClass(trace, IFlameChartProvider.class, secondaryId);
        if (module != null) {
            module.schedule();
            return new FlameChartDataProvider(trace, module, secondaryId);
        }

        return null;
    }
```

#### Grouping of data providers

Data providers can be grouped under a common parent. A common parent is indicated by the `parentId` in the data provider descriptor. The data provider parent can be any of existing data providers or container data providers whose data provider descriptor has the `ProviderType.NONE`. They don't have any outputs associated with them.

```java
    //...
    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        KernelAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
        descriptor = new DataProviderDescriptor.Builder()
                .setId(ThreadStatusDataProvider.ID)
                .setParentId("org.eclipse.tracecompass.linux.kernel");
                .setName(Objects.requireNonNull(Messages.ThreadStatusDataProviderFactory_title))
                .setDescription(Objects.requireNonNull(Messages.ThreadStatusDataProviderFactory_descriptionText))
                .setProviderType(ProviderType.TIME_GRAPH)
                .build();
        return module != null ? Collections.singletonList(descriptor) : Collections.emptyList();
    }
    //...
```

#### Implementing a data provider without analysis module

Data providers can use analysis modules, but they don't have to. For example, you can implement a data provider that gets the event statistics for the whole trace by reading all the events of a trace. See [Creating a Data Tree Data Provider](#creating-a-data-tree-data-provider) for an example.

## Implementing a configurable data provider

Defining data providers statically as described in the previous chapters is not always flexible enough for user's needs. It's often required to create derived data providers from an existing data provider or data providers based on some configuration parameters. For example, it might be interesting to derive a CPU usage data provider from the original CPU usage data provider, that shows the CPU usage for a given CPU only. Another use case is to derive a virtual table data provider from the events table data provider, that shows only trace events with a certain event type.

Using data provider configurators this is possible. Note that this interface is hooked-up to the Trace Compass trace server so that end-users can configure it using client APIs. For Eclipse Trace Compass this is not done and extenders will have to manage such configurations using their custom UI implementation.

A data provider configurator needs to implement the `ITmfDataProviderConfigurator` interface. Implement the following methods

- `List<ITmfConfigurationSourceType> getConfigurationSourceTypes()`
   Return one or more configuration source type that this configurator can handle. The configuration source type describes the input parameter to pass when creating a data provider
- `IDataProviderDescriptor createDataProviderDescriptors(ITmfTrace trace, ITmfConfiguration configuration)`
  This method is called to create derived data providers base on the input configuration. It returns a data provider descriptor of the derived data provider. The descriptor has to have the configuration set, has to have the capability of `canDelete` (so that it can be deleted) as well as it has to have an ID that has the configuration ID appended, which will be used by the corresponding data provider factory to create an instance of the data provider. It is responsible to create and manage analysis modules (e.g. add to ITmfTrace object) and persist the configuration in memory and disk so that it available after a server restart.
- `removeDataProviderDescriptor(ITmfTrace trace, IDataProviderDescriptor descriptor) throws TmfConfigurationException`
  Method is called to remove the derived data provider identified by the passed descriptor. It is responsible to clean-up analysis modules, delete peristent data (e.g. state systems) and delete persisted configuration (memory cache and disk).
- Add helper methods if it fits your configurator, for example, `getConfiguration(ITmfTrace, configId)`, `getDescriptorFromConfig(ITmfConfiguration)` or `generateID(configID)`
- Implement signal handler to handle `TmfTraceOpenedSignal` to read persisted configuration from disk (if implemented, not in example below)
- Implement signal handler to handle `TmfTraceClosedSignal` to remove trace or experiment from cached configurations.

The corresponding data provider factory now has to adapt to this configurator. For that override add the configurator as class member to the factory and overide the `getAdapter()` method.

Alternatively, the data provider factory can implement the `ITmfDataProviderConfigurator` itself on the contrary of containing the configurator class.

The data provider descriptors generated by the factory (`List<IDataProviderDescriptor> getDescriptors(ITmfTrace)`) have to include all the descriptors created from all configuration instances. Let the configurator return the list of such descriptors. Note that the descriptor created by a configuration has to return the configuration when method `IDataProviderDescriptor.getConfiguration()` is called, it has to have the capability `canDelete` set (so that it can be deleted) as well as it has to have an ID that can identify the configuration (e.g. by appending the configuration).

The factory also has to use the configurator to apply the configuration when the `createDataProvider(ITmfTrace, String)` method is called, note that the secondaryId (String) will determine which configuration is applied.

The actual data provider needs to apply the configuration. The easiest way is to pass it to the constructor and then inside the class use it. Make sure that the `getId()` method returns the concatenated ID and configuration ID.

### Implementing a configuration source type

<<<<<<< HEAD
The configuration source type describes the input parameters to provide when to pass when creating a new data provider or global configuration.
=======
The configuration source type descibes the input parameters to provide when to pass when creating a new data provider or global configuration.
>>>>>>> 222504c42f (doc: Add global configuration to trace-server developer guide)

The interface to implement is `ITmfConfigurationSourceType`. You can use the `TmfConfigurationSourceType.Builder` class to build such type. It has name, description, unique ID, an optional JSON schema file or a list of simple `TmfConfigurationParameter` instances. Use schema as much as you can. The schema will describe the JSON parameters, that `ITmfConfiguration.getParameters()` will return when passed to the configurator.


```java
    @Override
    public @NonNull List<@NonNull ITmfConfigurationSourceType> getConfigurationSourceTypes() {
        return List.of(new TmfConfigurationSourceType.Builder()
                .setId("example.data.provider.config.source.type")
                .setDescription("Example configuration source to demonstrate a configurator")
                .setName("Example configuration")
                .setSchemaFile(schemaFile)
                .build());
    }
```

### Implementing a configurable data provider without analysis module

To demonstrate how to implement a configurable data provider that doesn't use an analyis module, we will modify the data provider of chapter [Implementing data provider without analysis module](#implementing-a-data-provider-without-analysis-module). Please note that the class name and package names are different to be able to have independent examples in the example plug-in. 

#### Implementing an `ITmfDataProviderConfigurator` without analysis module

Here is the example implementation of the configurator.

```java
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

package org.eclipse.tracecompass.examples.core.data.provider.config;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.examples.core.Activator;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.component.TmfComponent;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.osgi.framework.Bundle;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Example data provider configurator
 * 
 * @author Bernd Hufmann
 */
@SuppressWarnings({"nls", "null"})
public class ExampleDataTreeDataProviderConfigurator extends TmfComponent implements ITmfDataProviderConfigurator {
    
    private static final ITmfConfigurationSourceType CONFIG_SOURCE_TYPE;
    private static final String SCHEMA = "schema/example-schema.json";
    
    private Table<String, ITmfTrace, ITmfConfiguration> fTmfConfigurationTable = HashBasedTable.create();
    
    static {
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
        IPath defaultPath = new Path(SCHEMA);
        URL url = FileLocator.find(bundle, defaultPath, null);
        File schemaFile = null;
        try {
            schemaFile = new File(FileLocator.toFileURL(url).toURI());
        } catch (URISyntaxException | IOException e) {
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ("Failed to read schema file: " + SCHEMA + e)));
        }
        CONFIG_SOURCE_TYPE = new TmfConfigurationSourceType.Builder()
                .setId("example.data.provider.config.source.type")
                .setDescription("Example configuration source to demostrate a configurator")
                .setName("Example configuration")
                .setSchemaFile(schemaFile)
                .build();
    }

    /**
     * Constructor
     */
    public ExampleDataTreeDataProviderConfigurator() {
        super("ExampleDataProviderConfigurator");
    }

    @Override
    public @NonNull List<@NonNull ITmfConfigurationSourceType> getConfigurationSourceTypes() {
        // Return one or more configuration source type that this configurator can handle
        return List.of(CONFIG_SOURCE_TYPE);
    }

    @Override
    public @NonNull IDataProviderDescriptor createDataProviderDescriptors(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        // Check if configuration exists
        if (fTmfConfigurationTable.contains(configuration.getId(), trace)) {
            throw new TmfConfigurationException("Configuration already exists with label: " + configuration.getName()); //$NON-NLS-1$
        }
        /* 
         * - Apply configuration
         *   - if needed, create analysis module with configuration and add it to the trace ITmfTrace.addAnalysisModule()
         *   - parse parameters (e.g. JSON parse) in configuration and store data
         * - Write configuration to disk (if it supposed to survive a restart)
         *   - E.g. write it in supplementary directory of the trace (or experiment) or propagate it to supplementary directory of
         *    of each trace in experiment.
         *    - Use TmfConfiguration.writeConfiguration(configuration, null);
         * - Store configuration for this trace in class storage
         */
        fTmfConfigurationTable.put(configuration.getId(), trace, configuration);

        return getDescriptorFromConfig(configuration);
    }

    @Override
    public void removeDataProviderDescriptor(@NonNull ITmfTrace trace, @NonNull IDataProviderDescriptor descriptor) throws TmfConfigurationException {
        // Check if configuration exists
        ITmfConfiguration creationConfiguration = descriptor.getConfiguration();
        if (creationConfiguration == null) {
            throw new TmfConfigurationException("Data provider was not created by a configuration"); //$NON-NLS-1$
        }
        
        String configId = creationConfiguration.getId();
        // Remove configuration from class storage
        ITmfConfiguration config = fTmfConfigurationTable.get(configId, trace);
        if (config == null) {
            return;
        }
        config = fTmfConfigurationTable.remove(configId, trace);

        /*
         * - Remove configuration
         *   - if needed, remove analysis from trace: ITmfAnalysisModule module =(ITmfTrace.removeAnalysisModule())
         *   - Call module.dispose() analysis module
         *   - Call module.clearPeristentData() (if analysis module has persistent data like a state system)
         * - Delete configuration from disk (if it was persisted)
         */
        
    }

    /**
     * Get list of configured descriptors
     * @param trace
     *            the trace
     * @return list of configured descriptors
     */
    public List<IDataProviderDescriptor> getDataProviderDescriptors(ITmfTrace trace) {
        return fTmfConfigurationTable.column(trace).values()
                .stream()
                .map(config -> getDescriptorFromConfig(config))
                .toList();
    }

    // Create descriptors per configuration
    private @NonNull static IDataProviderDescriptor getDescriptorFromConfig(ITmfConfiguration configuration) {
        DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
            return builder.setId(generateID(configuration.getId()))
                   .setConfiguration(configuration)
                   .setParentId(ExampleConfigurableDataTreeDataProviderFactory.ID)
                   .setCapabilities(new DataProviderCapabilities.Builder().setCanDelete(true).build())
                   .setDescription(configuration.getDescription())
                   .setName(configuration.getName())
                   .setProviderType(ProviderType.DATA_TREE)
                   .build();
    }

    /**
     * Gets the configuration for a trace and configId
     * @param trace
     *          the trace
     * @param configId
     *          the configId
     * @return the configuration for a trace and configId
     */
    public @Nullable ITmfConfiguration getConfiguration(ITmfTrace trace, String configId) {
        return fTmfConfigurationTable.get(configId, trace);
    }

    /**
     * Generate data provider ID using a config ID.
     * 
     * @param configId
     *          the config id
     * @return data provider ID using a config ID.
     */
    public static String generateID(String configId) {
        return ExampleConfigurableDataTreeDataProviderFactory.ID + DataProviderConstants.ID_SEPARATOR + configId;
        
    }

    /**
     * Handles trace closed signal to clean configuration table for this trace
     *
     * @param signal
     *            the close signal to handle
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        fTmfConfigurationTable.column(trace).clear();
    }
}
```
#### Updating data provider factory for configurable data provider (without analysis module)

After implementing the configurator, the data provider factory of chapter ["implementing-a-data-provider-without analysis module"](#implementing-a-data-provider-without-analysis-module) needs to be updated to use the configurator. 

- Add a member configurator member variable
- Implemented method `ITmfDataProvider createDataProvider(@NonNull ITmfTrace trace, String secondaryId)` where the seondaryId will include the configuraiton ID. Create a configurable data provider instance using the confguration identified by the configuration ID.
- Update method `Collection<IDataroviderDescriptor> getDescriptors(ITmfTrace)` to include data provider descriptors created by configurations
- Override the `getAdapter(Class)` method to return the configurator instance when requested

Here is an example of a data provider factory implementation that uses the configurator above.

```java
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

package org.eclipse.tracecompass.examples.core.data.provider.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * Example data provider factory
 * 
 * @author Bernd Hufmann
 */
public class ExampleConfigurableDataTreeDataProviderFactory implements IDataProviderFactory {
    /** The factory ID */
    public static final String ID = "org.eclipse.tracecompass.examples.nomodulestats.config"; //$NON-NLS-1$

    private ExampleDataTreeDataProviderConfigurator fConfigurator = new ExampleDataTreeDataProviderConfigurator();
    
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName("Simple Event Statistics (all)") //$NON-NLS-1$
            .setDescription("Simple Event statistics all event") //$NON-NLS-1$
            .setProviderType(ProviderType.DATA_TREE)
             // Only for configurators, indicate that this data provider can create derived data providers
            .setCapabilities(new DataProviderCapabilities.Builder().setCanCreate(true).build()) 
            .build();

    @Override
    public @Nullable ITmfDataProvider createDataProvider(@NonNull ITmfTrace trace) {
        if (trace instanceof TmfExperiment) {
            return TmfTreeComis crpositeDataProvider.create(TmfTraceManager.getTraceSet(trace), ID);
        }
        return new ExampleConfigurableDataTreeDataProvider(trace);
    }

    @Override
    @SuppressWarnings("null")
    public @Nullable ITmfDataProvider createDataProvider(@NonNull ITmfTrace trace, String secondaryId) {
        ITmfConfiguration config = fConfigurator.getConfiguration(trace, secondaryId);
        if (trace instanceof TmfExperiment) {
            List<ExampleConfigurableDataTreeDataProvider> list = TmfTraceManager.getTraceSet(trace)
                .stream()
                .map(tr -> new ExampleConfigurableDataTreeDataProvider(tr, config))
               .toList();
            return new TmfTreeCompositeDataProvider<>(list, ExampleDataTreeDataProviderConfigurator.generateID(secondaryId));
        }
        return new ExampleConfigurableDataTreeDataProvider(trace, config);
    }

    @Override
    public @NonNull Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        List<IDataProviderDescriptor> descriptors = new ArrayList<>();
        descriptors.add(DESCRIPTOR);
        descriptors.addAll(fConfigurator.getDataProviderDescriptors(trace));
        return descriptors;
    }

    @Override
    public <T> @Nullable T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(ExampleDataTreeDataProviderConfigurator.class)) {
            return adapter.cast(fConfigurator);
        }
        return IDataProviderFactory.super.getAdapter(adapter);
    }

    @Override
    public void dispose() {
        fConfigurator.dispose();
    }
}
```
#### Updating data provider class for configurable data provider (without analysis module)

Next, update the data provider implementation of chapter ["implementing-a-data-provider without analysis module"](#implementing-a-data-provider-without-analysis-module) needs to be updated to use the configurator. 

- Add constructor to pass the configuration instance. Store the configuration and/or parse the configuration to extract the relevant parameters returned by `configuration.getParameters()`.
- Make sure that the ID returned by `getId()` contains the configuration ID, for example, concatenate it the base data provider ID.
- Apply the configuration. In the example below the `TmfEventRequest` is using the configuration parameters where the `configuration.getParameters()` are parsed (from JSON) in the constructor of the event request.

Here is an example implementation of the configurable data provider.

```java
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
package org.eclipse.tracecompass.examples.core.data.provider.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfLostEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.filter.ITmfFilter;
import org.eclipse.tracecompass.tmf.core.filter.model.TmfFilterMatchesNode;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Simple events statistics data provider
 * 
 * @author Bernd Hufmann
 */
@SuppressWarnings("null")
@NonNullByDefault
public class ExampleConfigurableDataTreeDataProvider implements ITmfTreeDataProvider<TmfTreeDataModel> {
    private static final String BASE_ID = "org.eclipse.tracecompass.examples.nomodulestats.config"; //$NON-NLS-1$;
    
    private static long fCount = 0;
    
    private @Nullable ITmfTrace fTrace;
    private @Nullable StatsPerTypeRequest fRequest;
    private @Nullable List<TmfTreeDataModel> fCachedResult = null;
    private @Nullable ITmfConfiguration fConfiguration;
    private String fId = BASE_ID;

    /**
     * Constructor
     * @param trace
     *          the trace (not experiment)
     */
    public ExampleConfigurableDataTreeDataProvider(ITmfTrace trace) {
        this(trace, null);
    }

    /**
     * Constructor with configuration
     * @param trace
     *      the trace (not experiment)
     * @param configuration
     *      the configuration
     */
    public ExampleConfigurableDataTreeDataProvider(ITmfTrace trace, @Nullable ITmfConfiguration configuration) {
        fTrace = trace;
        fConfiguration = configuration;
        if (configuration != null) {
            fId = BASE_ID + DataProviderConstants.ID_SEPARATOR + configuration.getId();
        }
    }

    @Override
    public @NonNull String getId() {
        return fId;
    }

    @Override
    public @NonNull TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {

        ITmfTrace trace = fTrace;
        if (trace == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        StatsPerTypeRequest request = fRequest;
        if (request == null) {
            // Start new request
            request = new StatsPerTypeRequest(trace, TmfTimeRange.ETERNITY);
            trace.sendRequest(request);
            TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), Collections.emptyList()); //$NON-NLS-1$ //$NON-NLS-2$
            fRequest = request;
            return new TmfModelResponse<>(model, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        if (request.isCancelled()) {
            TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), Collections.emptyList()); //$NON-NLS-1$ //$NON-NLS-2$
            return new TmfModelResponse<>(model, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
        }

        if (!request.isCompleted()) {
            TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), Collections.emptyList()); //$NON-NLS-1$ //$NON-NLS-2$
            return new TmfModelResponse<>(model, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        List<TmfTreeDataModel> values = fCachedResult;
        if (values == null) {
            long traceId = fCount++;
            values = new ArrayList<>();
            long total = 0;
            for (Entry<String, Long> entry : request.getResults().entrySet()) {
                values.add(new TmfTreeDataModel(fCount++, traceId, List.of(entry.getKey(), String.valueOf(entry.getValue()))));
                total += entry.getValue();
            }
            TmfTreeDataModel traceEntry = new TmfTreeDataModel(traceId, -1, List.of(trace.getName(), String.valueOf(total)));
            values.add(0, traceEntry);
            fCachedResult = values;
        }
        TmfTreeModel<TmfTreeDataModel> model = new TmfTreeModel<>(List.of("Name", "Value"), values); //$NON-NLS-1$ //$NON-NLS-2$
        return new TmfModelResponse<>(model, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private class StatsPerTypeRequest extends TmfEventRequest {

        /* Map in which the results are saved */
        private final Map<@NonNull String, @NonNull Long> stats;
        private @Nullable ITmfFilter fFilter = null;

        public StatsPerTypeRequest(ITmfTrace trace, TmfTimeRange range) {
            super(trace.getEventType(), range, 0, ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.BACKGROUND);
            this.stats = new HashMap<>();
            
            if (fConfiguration != null) {
                try {
                    String jsonParameters = new Gson().toJson(fConfiguration.getParameters(), Map.class);
                    String regEx = new Gson().fromJson(jsonParameters, InternalConfiguration.class).getFilter();
                    if (regEx != null) {
                        TmfFilterMatchesNode filter = new TmfFilterMatchesNode(null);
                        filter.setEventAspect(TmfBaseAspects.getEventTypeAspect());
                        filter.setRegex(regEx);
                        fFilter = filter;
                    }
                } catch (JsonSyntaxException e) {
                    fFilter = null;
                }
            }
        }

        public Map<@NonNull String, @NonNull Long> getResults() {
            return stats;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            if (event.getTrace() == fTrace) {
                if (fFilter == null || (fFilter != null && fFilter.matches(event))) {
                    String eventType = event.getName();
                    /*
                     * Special handling for lost events: instead of counting just
                     * one, we will count how many actual events it represents.
                     */
                    if (event instanceof ITmfLostEvent) {
                        ITmfLostEvent le = (ITmfLostEvent) event;
                        incrementStats(eventType, le.getNbLostEvents());
                        return;
                    }

                    /* For standard event types, just increment by one */
                    incrementStats(eventType, 1L);
                }
            }
        }

        private void incrementStats(@NonNull String key, long count) {
            stats.merge(key, count, Long::sum);
        }
    }

    @Override
    public void dispose() {
        fRequest = null;
        fCachedResult = null;
    }

    private static class InternalConfiguration {
        @Expose
        @SerializedName(value = "filter")
        private @Nullable String fFilter = null;

        public @Nullable String getFilter() {
            return fFilter;
        }
    }

}

```

### Implementing a configurable data provider with an analysis module

Let's implement a configurable data provider where an analysis module with state sytsem is used. In the example, the configuration will be applied to the state provider, hence a new state system file will be created. The configuration instance will have to propagated from the client down to the analysis module and its state provider. 

To demonstrate how to implement a configurable data provider that doesn't use an analyis module, we will modify the data provider of chapter [Creating a timegraph data provider](#creating-a-time-graph-data-provider).


#### Implementing an `ITmfDataProviderConfigurator` with analysis module

The example below shows configurator for a data provider that uses an analysis module with state system, but it creates the data to returned by reading the state system created through the analysis module.. 

The steps to implement as described in [Implementing a configurable data provider](#implementing-a-configurable-data-provider). However, there are the following additional considerations:

- `IDataProviderDescriptor createDataProviderDescriptors(ITmfTrace trace, ITmfConfiguration configuration)`
    When called, the configuration needs to be applied, i.e. create analysis module with the configuration, add the analysis module to the trace and/or experiment persist the configuration on disk and cache the configuration inside the class. 
- `void removeDataProviderDescriptor(ITmfTrace trace, IDataProviderDescriptor descriptor)`
    This method will remove the configuration, all stored information and persisted data corresponding the derived data provider whose descriptor is passed to the method. For that remove the analysis module(s) from traces or experiment and delete persisted data (e.g. state system) 

In the example below, the configurator instantiates the analysis module and passes the configuration object to the constructor. Then the trace is set and the analysis module is stored in the trace object. Dispose and clear persistent data if needed.

```java

     //...
        ExampleConfigurableStateSystemAnalysisModule module = new ExampleConfigurableStateSystemAnalysisModule(configuration);
        try {
            // 
            if (module.setTrace(trace)) {
                IAnalysisModule oldModule = trace.addAnalysisModule(module);
                // Sanity check
                if (oldModule != null) {
                    oldModule.dispose();
                    oldModule.clearPersistentData();
                }
            } else {
                // in case analysis module doesn't apply to the trace
                module.dispose();
            }
        } catch (TmfAnalysisException | TmfTraceException e) {
            // Should not happen
            module.dispose();
        }
        //...
```

When removing the configuration, the configuration will dispose the analysis module and will clear the pesistent data.

```java
        //...
        IAnalysisModule module = ExampleConfigurableDataTreeDataProviderFactory.removeAnalysisModule(analysisId);
        if (module != null) {
            module.dispose();
            module.clearPersistentData();
        }
        //...
```

Here is the full example implementation.

```java

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

package org.eclipse.tracecompass.examples.core.data.provider.config;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.examples.core.Activator;
import org.eclipse.tracecompass.examples.core.analysis.ExampleStateSystemAnalysisModule;
import org.eclipse.tracecompass.examples.core.analysis.config.ExampleConfigurableStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.component.TmfComponent;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.osgi.framework.Bundle;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Example data provider configurator
 * 
 * @author Bernd Hufmann
 */
@SuppressWarnings({"nls", "null"})
public class ExampleTimeGraphProviderWithAnalysisConfigurator extends TmfComponent implements ITmfDataProviderConfigurator {
    
    private static final ITmfConfigurationSourceType CONFIG_SOURCE_TYPE;
    private static final String SCHEMA = "schema/example-schema-cpu.json";
    
    private Table<String, ITmfTrace, ITmfConfiguration> fTmfConfigurationTable = HashBasedTable.create();
    
    static {
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
        IPath defaultPath = new Path(SCHEMA);
        URL url = FileLocator.find(bundle, defaultPath, null);
        File schemaFile = null;
        try {
            schemaFile = new File(FileLocator.toFileURL(url).toURI());
        } catch (URISyntaxException | IOException e) {
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ("Failed to read schema file: " + SCHEMA + e)));
        }
        CONFIG_SOURCE_TYPE = new TmfConfigurationSourceType.Builder()
                .setId("example.time.graph.data.provider.config.source.type")
                .setDescription("Example configuration source to demostrate a configurator")
                .setName("Example configuration")
                .setSchemaFile(schemaFile)
                .build();
    }

    /**
     * Constructor
     */
    public ExampleTimeGraphProviderWithAnalysisConfigurator() {
        super("ExampleDataProviderConfigurator");
    }
    
    @Override
    public @NonNull List<@NonNull ITmfConfigurationSourceType> getConfigurationSourceTypes() {
        // Return one or more configuration source type that this configurator can handle
        return List.of(CONFIG_SOURCE_TYPE);
    }

    @Override
    public @NonNull IDataProviderDescriptor createDataProviderDescriptors(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        // Check if configuration exists
        if (fTmfConfigurationTable.contains(configuration.getId(), trace)) {
            throw new TmfConfigurationException("Configuration already exists with label: " + configuration.getName()); //$NON-NLS-1$
        }
        applyConfiguration(configuration, trace);
        writeConfiguration(configuration, trace);
        fTmfConfigurationTable.put(configuration.getId(), trace, configuration);
        return getDescriptorFromConfig(configuration);
    }

    @Override
    public void removeDataProviderDescriptor(@NonNull ITmfTrace trace, @NonNull IDataProviderDescriptor descriptor) throws TmfConfigurationException {
        ITmfConfiguration creationConfiguration = descriptor.getConfiguration();
        if (creationConfiguration == null) {
            throw new TmfConfigurationException("Data provider was not created by a configuration"); //$NON-NLS-1$
        }
        
        String configId = creationConfiguration.getId();
        ITmfConfiguration config = fTmfConfigurationTable.get(configId, trace);
        if (config == null) {
            return;
        }
        config = fTmfConfigurationTable.remove(configId, trace);
        deleteConfiguration(creationConfiguration, trace);
        removeConfiguration(creationConfiguration, trace);
    }

    private static void applyConfiguration(ITmfConfiguration configuration, ITmfTrace trace) throws TmfConfigurationException {
        // Apply only to each trace in experiment
        if (trace instanceof TmfExperiment) {
            for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                applyConfiguration(configuration, tr);
            }
            return;
        }
        ExampleConfigurableStateSystemAnalysisModule module = new ExampleConfigurableStateSystemAnalysisModule(configuration);
        try {
            // 
            if (module.setTrace(trace)) {
                IAnalysisModule oldModule = trace.addAnalysisModule(module);
                // Sanity check
                if (oldModule != null) {
                    oldModule.dispose();
                    oldModule.clearPersistentData();
                }
            } else {
                // in case analysis module doesn't apply to the trace
                module.dispose();
            }
        } catch (TmfAnalysisException | TmfTraceException e) {
            // Should not happen
            module.dispose();
        }
    }

    private static void writeConfiguration(ITmfConfiguration configuration, ITmfTrace trace) throws TmfConfigurationException {
        IPath path = getConfigurationRootFolder(trace);
        TmfConfiguration.writeConfiguration(configuration, path);
    }

    private static void deleteConfiguration(ITmfConfiguration configuration, ITmfTrace trace) throws TmfConfigurationException {
        IPath traceConfig = getConfigurationRootFolder(trace);
        traceConfig = traceConfig.append(File.separator).append(configuration.getId()).addFileExtension(TmfConfiguration.JSON_EXTENSION);
        File configFile = traceConfig.toFile();
        if ((!configFile.exists()) || !configFile.delete()) {
            throw new TmfConfigurationException("InAndOut configuration file can't be deleted from trace: configId=" + configuration.getId()); //$NON-NLS-1$
        }
    }

    private static void removeConfiguration(ITmfConfiguration configuration, ITmfTrace trace) throws TmfConfigurationException {
        String analysisId = generateID(ExampleStateSystemAnalysisModule.ID, configuration.getId());
        for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
            // Remove and clear persistent data
            try {
                IAnalysisModule module = tr.removeAnalysisModule(analysisId);
                if (module != null) {
                    module.dispose();
                    module.clearPersistentData();
                }
            } catch (TmfTraceException e) {
                throw new TmfConfigurationException("Error removing analysis module from trace: analysis ID=" + analysisId, e); //$NON-NLS-1$
            }
        }
    }

    private static List<ITmfConfiguration> readConfigurations(ITmfTrace trace) throws TmfConfigurationException {
        IPath rootPath = getConfigurationRootFolder(trace);
        File folder = rootPath.toFile();
        List<ITmfConfiguration> list = new ArrayList<>();
        if (folder.exists()) {
            File[] listOfFiles = folder.listFiles();
            for (File file : listOfFiles) {
                IPath path = new Path(file.getName());
                if (path.getFileExtension().equals(TmfConfiguration.JSON_EXTENSION)) {
                    ITmfConfiguration config = TmfConfiguration.fromJsonFile(file);
                    list.add(config);
                }
            }
        }
        return list;
    }
    
    private static IPath getConfigurationRootFolder(ITmfTrace trace) {
        String supplFolder = TmfTraceManager.getSupplementaryFileDir(trace);
        IPath supplPath = new Path(supplFolder);
        return supplPath
                .addTrailingSeparator()
                .append(ExampleConfigurableTimeGraphProviderFactory.ID);
    }
    
    /**
     * Get list of configured descriptors
     * @param trace
     *            the trace
     * @return list of configured descriptors
     */
    public List<IDataProviderDescriptor> getDataProviderDescriptors(ITmfTrace trace) {
        return fTmfConfigurationTable.column(trace).values()
                .stream()
                .map(config -> getDescriptorFromConfig(config))
                .toList();
    }

    // Create descriptors per configuration
    private static IDataProviderDescriptor getDescriptorFromConfig(ITmfConfiguration configuration) {
        DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
            return builder.setId(generateID(generateID(ExampleConfigurableStateSystemAnalysisModule.ID, configuration.getId())))
                   .setConfiguration(configuration)
                   .setParentId(ExampleConfigurableTimeGraphProviderFactory.ID)
                   .setCapabilities(new DataProviderCapabilities.Builder().setCanDelete(true).build())
                   .setDescription(configuration.getDescription())
                   .setName(configuration.getName())
                   .setProviderType(ProviderType.TIME_GRAPH)
                   .build();
    }

    /**
     * Gets the configuration for a trace and configId
     * @param trace
     *          the trace
     * @param configId
     *          the configId
     * @return the configuration for a trace and configId
     */
    public @Nullable ITmfConfiguration getConfiguration(ITmfTrace trace, String configId) {
        return fTmfConfigurationTable.get(configId, trace);
    }

    /**
     * Generate data provider ID using a config ID.
     * 
     * @param configId
     *          the config id
     * @return data provider ID using a config ID.
     */
    public static String generateID(String configId) {
        return generateID(ExampleConfigurableTimeGraphProviderFactory.ID, configId);
        
    }

    /**
     * Generate data provider ID using a base ID and config ID.
     * 
     * @param baseId
     *          the base id
     * @param configId
     *          the config id
     * @return generated ID
     */
    public static String generateID(String baseId, String configId) {
        if (configId == null) {
            return baseId;
        }
        return baseId + DataProviderConstants.ID_SEPARATOR + configId;
    }

    
    /**
     * Signal handler for opened trace signal. Will populate trace
     * configurations
     *
     * @param signal
     *            the signal to handle
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        if (trace == null) {
            return;
        }
        try {
            if (trace instanceof TmfExperiment) {
                List<ITmfConfiguration> configs = readConfigurations(trace);
                for (ITmfConfiguration config : configs) {
                    if (!fTmfConfigurationTable.contains(config.getId(), trace)) {
                        fTmfConfigurationTable.put(config.getId(), trace, config);
                        applyConfiguration(config, trace);
                    }
                }
            }
        } catch (TmfConfigurationException e) {
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ("Error applying configurations for trace " + trace.getName() + ", exception" + e))); //$NON-NLS-1$
        }
    }
    
    /**
     * Handles trace closed signal to clean configuration table for this trace
     *
     * @param signal
     *            the close signal to handle
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        fTmfConfigurationTable.column(trace).clear();
    }
}
```

#### Creating a configurable analysis module 

The analysis module itself, needs to apply the configuration which is implementation specific. Make sure that the `getId()` method uses the configuration Id as part of the analysis module ID. The analysis module then has to pass the configuration to the state provider class.


Here is the complete example for the analysis module.

```java
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
```

If you look closer into the implementation, you will notice that the configuration is passed to the state provider class:
```
    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new ExampleConfigurableStateProvider(Objects.requireNonNull(getTrace()), fConfiguration);
    }
```

For the full implementation of the state provider class see below. Note that the state provider ID needs to include the configuration ID.:

```java
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

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * An example of a simple state provider for a simple state system analysis
 *
 * This module is also in the developer documentation of Trace Compass. If it is
 * modified here, the doc should also be updated.
 *
 * @author Alexandre Montplaisir
 * @author Geneviève Bastien
 * @author Bernd Hufmann
 */
public class ExampleConfigurableStateProvider extends AbstractTmfStateProvider {

    private static final @NonNull String PROVIDER_ID = "org.eclipse.tracecompass.examples.state.system.module.config"; //$NON-NLS-1$
    private static final int VERSION = 0;
    private int fCpu = -1;

    /**
     * Constructor
     * 
     * @param trace
     *                     the trace
     * @param configuration
     *                     the configuration
     */
    public ExampleConfigurableStateProvider(@NonNull ITmfTrace trace, @Nullable ITmfConfiguration configuration) {
        super(trace, PROVIDER_ID + (configuration == null ? "" : ":" + configuration.getId())); //$NON-NLS-1$ //$NON-NLS-2$
        if (configuration != null) {
            try {
                String jsonParameters = new Gson().toJson(configuration.getParameters(), Map.class);
                @SuppressWarnings("null")
                Integer cpu = new Gson().fromJson(jsonParameters, InternalConfiguration.class).getCpu();
                if (cpu != null) {
                    fCpu = cpu.intValue();
                }
            } catch (JsonSyntaxException e) {
                fCpu = -1;
            }
        }
    }
    
    /**
     * Constructor
     *
     * @param trace
     *            The trace for this state provider
     */
    public ExampleConfigurableStateProvider(@NonNull ITmfTrace trace) {
        super(trace, PROVIDER_ID);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new ExampleConfigurableStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {

        /**
         * Do what needs to be done with this event, here is an example that
         * updates the CPU state and TID after a sched_switch
         */
        if (event.getName().equals("sched_switch")) { //$NON-NLS-1$

            final long ts = event.getTimestamp().getValue();
            Long nextTid = event.getContent().getFieldValue(Long.class, "next_tid"); //$NON-NLS-1$
            Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
            if (cpu == null || nextTid == null || (fCpu >= 0 && cpu != fCpu)) {
                return;
            }

            ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());
            int quark = ss.getQuarkAbsoluteAndAdd("CPUs", String.valueOf(cpu)); //$NON-NLS-1$
            // The main quark contains the tid of the running thread
            ss.modifyAttribute(ts, nextTid, quark);

            // The status attribute has an integer value
            int statusQuark = ss.getQuarkRelativeAndAdd(quark, "Status"); //$NON-NLS-1$
            Integer value = (nextTid > 0 ? 1 : 0);
            ss.modifyAttribute(ts, value, statusQuark);
        }
    }

    
    private static class InternalConfiguration {
        @Expose
        @SerializedName(value = "cpu")
        private @Nullable Integer fCpuValue = null;

        public @Nullable Integer getCpu() {
            return fCpuValue;
        }
    }
}


```

#### Updating data provider factory for configurable data provider (with analysis module)

The implementation of the factory follows the same principle than the without analysis modules (see [here](#updating-data-provider-factory-for-configurable-data-provider-without-analysis-module)). However, the analysis modules have to be considered.


```java
/**********************************************************************
 * Copyright (c) 2020, 2025 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.examples.core.data.provider.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.examples.core.analysis.config.ExampleConfigurableStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TmfTimeGraphCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * An example of a time graph data provider factory with configurator and analysis module.
 *
 * This factory is also in the developer documentation of Trace Compass. If it is
 * modified here, the doc should also be updated.
 *
 * @author Geneviève Bastien
 * @author Bernd Hufmann
 */
public class ExampleConfigurableTimeGraphProviderFactory implements IDataProviderFactory {

    /** The ID */
    public static final String ID = ExampleConfigurableTimeGraphDataProvider.ID;

    private static final ExampleTimeGraphProviderWithAnalysisConfigurator fConfigurator = new ExampleTimeGraphProviderWithAnalysisConfigurator(); 
    
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName("Example configuarable time graph") //$NON-NLS-1$
            .setDescription("This is an example of a configurable time graph data provider using a state system analysis as a source of data") //$NON-NLS-1$
            .setProviderType(ProviderType.TIME_GRAPH)
            .setCapabilities(new DataProviderCapabilities.Builder().setCanCreate(true).build())
            .build();

    @Override
    public @Nullable ITmfDataProvider createDataProvider(ITmfTrace trace) {
        Collection<@NonNull ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        if (traces.size() == 1) {
            return ExampleConfigurableTimeGraphDataProvider.create(trace);
        }
        return TmfTimeGraphCompositeDataProvider.create(traces, ID);
    }

    @SuppressWarnings("null")
    @Override
    public @Nullable ITmfDataProvider createDataProvider(@NonNull ITmfTrace trace, String secondaryId) {
        if (trace instanceof TmfExperiment) {
            List<ExampleConfigurableTimeGraphDataProvider> list = TmfTraceManager.getTraceSet(trace)
                .stream()
                .map(tr -> ExampleConfigurableTimeGraphDataProvider.create(tr, secondaryId))
               .toList();
            return new TmfTimeGraphCompositeDataProvider<>(list, ExampleTimeGraphProviderWithAnalysisConfigurator.generateID(secondaryId));
        }
        return ExampleConfigurableTimeGraphDataProvider.create(trace, secondaryId);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        ExampleConfigurableStateSystemAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, ExampleConfigurableStateSystemAnalysisModule.class, ExampleConfigurableStateSystemAnalysisModule.ID);
        List<IDataProviderDescriptor> descriptors = new ArrayList<>();
        if (module != null) {
            descriptors.add(DESCRIPTOR);
        }
        descriptors.addAll(fConfigurator.getDataProviderDescriptors(trace));
        return descriptors;
    }

    @Override
    public <T> @Nullable T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(ExampleTimeGraphProviderWithAnalysisConfigurator.class)) {
            return adapter.cast(fConfigurator);
        }
        return IDataProviderFactory.super.getAdapter(adapter);
    }
}
```

#### Updating data provider class for configurable data provider (with analysis module)

The Data provider itself will have to have the configuration passed. Make sure that the data provider's ID contains the configuration ID to distinquish different instances.

```java
/**********************************************************************
 * Copyright (c) 2020, 2025 École Polytechnique de Montréal and other
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.examples.core.data.provider.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.examples.core.analysis.config.ExampleConfigurableStateSystemAnalysisModule;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * An example of a configurable time graph data provider.
 *
 * This class is also in the developer documentation of Trace Compass. If it is
 * modified here, the doc should also be updated.
 *
 * @author Geneviève Bastien
 * @author Bernd Hufmann
 */
@NonNullByDefault
public class ExampleConfigurableTimeGraphDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<ITimeGraphEntryModel>, IOutputStyleProvider {

    /**
     * Provider unique ID.
     */
    public static final String ID = "org.eclipse.tracecompass.examples.timegraph.dataprovider.config"; //$NON-NLS-1$

    private static final AtomicLong sfAtomicId = new AtomicLong();
    private static final String STYLE0_NAME = "style0"; //$NON-NLS-1$
    private static final String STYLE1_NAME = "style1"; //$NON-NLS-1$
    private static final String STYLE2_NAME = "style2"; //$NON-NLS-1$

    /* The map of basic styles */
    private static final Map<String, OutputElementStyle> STATE_MAP;
    /*
     * A map of styles names to a style that has the basic style as parent, to
     * avoid returning complete styles for each state
     */
    private static final Map<String, OutputElementStyle> STYLE_MAP;

    static {
        /* Build three different styles to use as examples */
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();

        builder.put(STYLE0_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE0_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("blue")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 0.5f,
                StyleProperties.OPACITY, 0.75f)));
        builder.put(STYLE1_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE1_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("yellow")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 1.0f,
                StyleProperties.OPACITY, 1.0f)));
        builder.put(STYLE2_NAME, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, STYLE2_NAME,
                StyleProperties.BACKGROUND_COLOR, String.valueOf(X11ColorUtils.toHexColor("green")), //$NON-NLS-1$
                StyleProperties.HEIGHT, 0.75f,
                StyleProperties.OPACITY, 0.5f)));
        STATE_MAP = builder.build();

        /* build the style map too */
        builder = new ImmutableMap.Builder<>();
        builder.put(STYLE0_NAME, new OutputElementStyle(STYLE0_NAME));
        builder.put(STYLE1_NAME, new OutputElementStyle(STYLE1_NAME));
        builder.put(STYLE2_NAME, new OutputElementStyle(STYLE2_NAME));
        STYLE_MAP = builder.build();
    }

    @SuppressWarnings("null")
    private final BiMap<Long, Integer> fIDToDisplayQuark = HashBiMap.create();
    private ExampleConfigurableStateSystemAnalysisModule fModule;
    private String fId = ID;

    /**
     * Constructor
     *
     * @param trace
     *            The trace this analysis is for
     * @param module
     *            The scripted analysis for this data provider
     */
    public ExampleConfigurableTimeGraphDataProvider(ITmfTrace trace, ExampleConfigurableStateSystemAnalysisModule module) {
        super(trace);
        fModule = module;
        ITmfConfiguration config = module.getConfiguration(); 
        if (config != null) {
            fId = ID + DataProviderConstants.ID_SEPARATOR + config.getId();
        }
    }

    /**
     * Create the time graph data provider
     *
     * @param trace
     *            The trace for which is the data provider
     * @param analysisId
     *            The analysisId of the analysis
     * @return The data provider
     */
    public static @Nullable ExampleConfigurableTimeGraphDataProvider create(ITmfTrace trace, String analysisId) {
        ExampleConfigurableStateSystemAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, ExampleConfigurableStateSystemAnalysisModule.class, analysisId);
        if (module != null) {
            module.schedule();
            return new ExampleConfigurableTimeGraphDataProvider(trace, module);
        }
        return null;
    }

    /**
     * Create the time graph data provider
     *
     * @param trace
     *            The trace for which is the data provider
     * @return The data provider
     */
    public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(ITmfTrace trace) {
        ExampleConfigurableStateSystemAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, ExampleConfigurableStateSystemAnalysisModule.class, ExampleConfigurableStateSystemAnalysisModule.ID);
        if (module != null) {
            module.schedule();
            return new ExampleConfigurableTimeGraphDataProvider(trace, module);
        }
        return null;
    }

    @Override
    public TmfModelResponse<TmfTreeModel<ITimeGraphEntryModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        fModule.waitForInitialization();
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        boolean isComplete = ss.waitUntilBuilt(0);
        long endTime = ss.getCurrentEndTime();

        // Make an entry for each base quark
        List<ITimeGraphEntryModel> entryList = new ArrayList<>();
        for (Integer quark : ss.getQuarks("CPUs", "*")) { //$NON-NLS-1$ //$NON-NLS-2$
            Long id = fIDToDisplayQuark.inverse().computeIfAbsent(quark, q -> sfAtomicId.getAndIncrement());
            entryList.add(new TimeGraphEntryModel(id, -1, ss.getAttributeName(quark), ss.getStartTime(), endTime));
        }

        Status status = isComplete ? Status.COMPLETED : Status.RUNNING;
        String msg = isComplete ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING;
        return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), entryList), status, msg);
    }

    @Override
    public String getId() {
        return fId;
    }

    @Override
    public TmfModelResponse<TimeGraphModel> fetchRowModel(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        try {
            List<ITimeGraphRowModel> rowModels = getDefaultRowModels(fetchParameters, ss, monitor);
            if (rowModels == null) {
                rowModels = Collections.emptyList();
            }
            return new TmfModelResponse<>(new TimeGraphModel(rowModels), Status.COMPLETED, CommonStatusMessage.COMPLETED);
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }
    }

    private @Nullable List<ITimeGraphRowModel> getDefaultRowModels(Map<String, Object> fetchParameters, ITmfStateSystem ss, @Nullable IProgressMonitor monitor) throws IndexOutOfBoundsException, TimeRangeException, StateSystemDisposedException {
        Map<Integer, ITimeGraphRowModel> quarkToRow = new HashMap<>();
        // Prepare the quarks to display
        Collection<Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (selectedItems == null) {
            // No selected items, take them all
            selectedItems = fIDToDisplayQuark.keySet();
        }
        for (Long id : selectedItems) {
            Integer quark = fIDToDisplayQuark.get(id);
            if (quark != null) {
                quarkToRow.put(quark, new TimeGraphRowModel(id, new ArrayList<>()));
            }
        }

        // This regex map automatically filters or highlights the entry
        // according to the global filter entered by the user
        Map<Integer, Predicate<Multimap<String, Object>>> predicates = new HashMap<>();
        Multimap<Integer, String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        // Query the state system to fill the states
        long currentEndTime = ss.getCurrentEndTime();
        for (ITmfStateInterval interval : ss.query2D(quarkToRow.keySet(), getTimes(ss, DataProviderParameterUtils.extractTimeRequested(fetchParameters)))) {
            if (monitor != null && monitor.isCanceled()) {
                return Collections.emptyList();
            }
            ITimeGraphRowModel row = quarkToRow.get(interval.getAttribute());
            if (row != null) {
                List<ITimeGraphState> states = row.getStates();
                ITimeGraphState timeGraphState = getStateFromInterval(interval, currentEndTime);
                // This call will compare the state with the filter predicate
                applyFilterAndAddState(states, timeGraphState, row.getEntryID(), predicates, monitor);
            }
        }
        for (ITimeGraphRowModel model : quarkToRow.values()) {
            model.getStates().sort(Comparator.comparingLong(ITimeGraphState::getStartTime));
        }

        return new ArrayList<>(quarkToRow.values());
    }

    private static TimeGraphState getStateFromInterval(ITmfStateInterval statusInterval, long currentEndTime) {
        long time = statusInterval.getStartTime();
        long duration = Math.min(currentEndTime, statusInterval.getEndTime() + 1) - time;
        Object o = statusInterval.getValue();
        if (!(o instanceof Long)) {
            // Add a null state
            return new TimeGraphState(time, duration, Integer.MIN_VALUE);
        }
        String styleName = "style" + ((Long) o) % 3; //$NON-NLS-1$
        return new TimeGraphState(time, duration, String.valueOf(o), STYLE_MAP.get(styleName));
    }

    private static Set<Long> getTimes(ITmfStateSystem key, @Nullable List<Long> list) {
        if (list == null) {
            return Collections.emptySet();
        }
        Set<Long> times = new HashSet<>();
        for (long t : list) {
            if (key.getStartTime() <= t && t <= key.getCurrentEndTime()) {
                times.add(t);
            }
        }
        return times;
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        /**
         * If there were arrows to be drawn, this is where they would be defined
         */
        return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        /**
         * If there were tooltips to be drawn, this is where they would be
         * defined
         */
        return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STATE_MAP), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }
}

```

### Future considerations

The interfaces for configurable data providers gives the developer the freedom to write their domain specific configurable data providers. They can choose how to implement it. However, many times the different implementations will do similar things. Hence, it would be good to provide abstract base classes for configurable data providers, e.g. an abstract class for the `ITmfDataProviderConfigurator`. Other candidates are analysis modules, state providers, factory etc.

## To Do
Provide guides for other features relevant for developing a custom trace server using Trace Compass core APIs, e.g.
- Trace annotation provider (Marker sets, requested_marker_set etc)
- Time Range columns (e.g. for data tree)
- Data Types in general (e.g. in data tree, events table)
- Time graph filtering
- Details on creating a virtual table 
- How to add a new data provider type
