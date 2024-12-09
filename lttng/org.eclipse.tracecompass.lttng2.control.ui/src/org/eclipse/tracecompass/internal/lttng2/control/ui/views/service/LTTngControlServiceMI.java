/**********************************************************************
 * Copyright (c) 2014, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Jonathan Rajotte - Initial support for machine interface lttng 2.6
 *   Bernd Hufmann - Fix check for live session
 **********************************************************************/

package org.eclipse.tracecompass.internal.lttng2.control.ui.views.service;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.SchemaFactory;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.common.core.xml.XmlUtils;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IBaseEventInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IChannelInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IDomainInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IEventInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IFieldInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.ILoggerInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IProbeEventInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.ISessionInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.ISnapshotInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IUstProviderInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.LogLevelType;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceDomainType;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceEnablement;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceEventType;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceJulLogLevel;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceLog4jLogLevel;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TraceLogLevel;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.TracePythonLogLevel;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.BaseEventInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.BufferType;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.ChannelInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.DomainInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.EventInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.FieldInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.LoggerInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.ProbeEventInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.SessionInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.SnapshotInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.UstProviderInfo;
import org.eclipse.tracecompass.internal.lttng2.control.ui.Activator;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.handlers.XmlMiValidationErrorHandler;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.messages.Messages;
import org.eclipse.tracecompass.tmf.remote.core.shell.ICommandInput;
import org.eclipse.tracecompass.tmf.remote.core.shell.ICommandResult;
import org.eclipse.tracecompass.tmf.remote.core.shell.ICommandShell;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Service for sending LTTng trace control commands to remote host via machine
 * interface mode.
 *
 * @author Jonathan Rajotte
 */
public class LTTngControlServiceMI extends LTTngControlService {

    /**
     * The tracing key (.options) and System property to control whether or not schema validation should be used.
     */
    public static final String MI_SCHEMA_VALIDATION_KEY = Activator.PLUGIN_ID + "/mi/schema-validation"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final DocumentBuilder fDocumentBuilder;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param shell
     *            the command shell implementation to use
     * @param version
     *            the lttng version
     * @throws ExecutionException
     *             if the creation of the Schema and DocumentBuilder objects
     *             fails
     */
    public LTTngControlServiceMI(@NonNull ICommandShell shell, @Nullable LttngVersion version) throws ExecutionException {
        super(shell);
        setVersion(version);

        DocumentBuilderFactory docBuilderFactory = XmlUtils.newSafeDocumentBuilderFactory();
        docBuilderFactory.setExpandEntityReferences(false);
        docBuilderFactory.setValidating(false);

        // TODO: remove check for 2.11 when new schema is available
        if (isSchemaValidationEnabled() && (version != null && (version.compareTo(new LttngVersion(2, 11, 0, null, null, null, null, null, null)) < 0))) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                URL xsdUrl = LTTngControlService.class.getResource(LTTngControlServiceConstants.MI_XSD_FILENAME);
                if (version.compareTo(new LttngVersion(2, 8, 0, null, null, null, null, null, null)) >= 0) {
                    xsdUrl = LTTngControlService.class.getResource(LTTngControlServiceConstants.MI3_XSD_FILENAME);
                    // MI 3.0 added name spaces. It will fail to validate if this is not set to true.
                    docBuilderFactory.setNamespaceAware(true);
                }
                docBuilderFactory.setSchema(schemaFactory.newSchema(xsdUrl));
            } catch (SAXException e) {
                throw new ExecutionException(Messages.TraceControl_InvalidSchemaError, e);
            }
        }

        try {
            fDocumentBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ExecutionException(Messages.TraceControl_XmlDocumentBuilderError, e);
        }

        fDocumentBuilder.setErrorHandler(new XmlMiValidationErrorHandler());

    }

    private static boolean isSchemaValidationEnabled() {
        String schemaValidationKey = Platform.getDebugOption(MI_SCHEMA_VALIDATION_KEY);
        String systemProperty = System.getProperty(MI_SCHEMA_VALIDATION_KEY);
        return schemaValidationKey != null && Boolean.parseBoolean(schemaValidationKey) || systemProperty != null && Boolean.parseBoolean(systemProperty);
    }

    /**
     * Generate a Document object from an list of Strings.
     *
     * @param xmlStrings
     *            list of strings representing an xml input
     * @param documentBuilder
     *            the builder used to get the document
     * @return Document generated from strings input
     * @throws ExecutionException
     *             when parsing has failed
     */
    private static Document getDocumentFromStrings(List<String> xmlStrings, DocumentBuilder documentBuilder) throws ExecutionException {
        StringBuilder concatenedString = new StringBuilder();
        for (String string : xmlStrings) {
            concatenedString.append(string);
        }
        InputSource stream = new InputSource(new StringReader(concatenedString.toString()));

        Document document;
        try {
            document = documentBuilder.parse(stream);
        } catch (SAXException | IOException e) {
            throw new ExecutionException(Messages.TraceControl_XmlParsingError + ':' + e.toString(), e);
        }
        return document;

    }

    /**
     * Parse LTTng version from a MI command result
     *
     * @param commandResult
     *            the result obtained from a MI command
     * @return the LTTng version
     * @throws ExecutionException
     *             when xml extraction fail
     */
    public static LttngVersion parseVersion(ICommandResult commandResult) throws ExecutionException {
        DocumentBuilderFactory docBuilderFactory = XmlUtils.newSafeDocumentBuilderFactory();
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ExecutionException(Messages.TraceControl_XmlDocumentBuilderError, e);
        }

        Document doc = getDocumentFromStrings(commandResult.getOutput(), documentBuilder);
        NodeList element = doc.getElementsByTagName(MIStrings.VERSION);
        if (element.getLength() != 1) {
            throw new ExecutionException(Messages.TraceControl_UnsupportedVersionError);
        }

        int major = 0;
        int minor = 0;
        int patchLevel = 0;
        String license = ""; //$NON-NLS-1$
        String commit = ""; //$NON-NLS-1$
        String name = ""; //$NON-NLS-1$
        String description = ""; //$NON-NLS-1$
        String url = ""; //$NON-NLS-1$
        String fullVersion = ""; //$NON-NLS-1$
        NodeList child = element.item(0).getChildNodes();
        // Get basic information
        for (int i = 0; i < child.getLength(); i++) {
            Node node = child.item(i);
            switch (node.getNodeName()) {
            case MIStrings.VERSION_MAJOR:
                major = Integer.parseInt(node.getTextContent());
                break;
            case MIStrings.VERSION_MINOR:
                minor = Integer.parseInt(node.getTextContent());
                break;
            case MIStrings.VERSION_PATCH_LEVEL:
                patchLevel = Integer.parseInt(node.getTextContent());
                break;
            case MIStrings.VERSION_COMMIT:
                commit = node.getTextContent();
                break;
            case MIStrings.VERSION_DESCRIPTION:
                description = node.getTextContent();
                break;
            case MIStrings.VERSION_LICENSE:
                license = node.getTextContent();
                break;
            case MIStrings.VERSION_NAME:
                name = node.getTextContent();
                break;
            case MIStrings.VERSION_STR:
                fullVersion = node.getTextContent();
                break;
            case MIStrings.VERSION_WEB:
                url = node.getTextContent();
                break;
            default:
                break;
            }
        }
        return new LttngVersion(major, minor, patchLevel, license, commit, name, description, url, fullVersion);
    }

    @Override
    public List<String> getSessionNames(IProgressMonitor monitor) throws ExecutionException {
        ICommandInput command = createCommand(LTTngControlServiceConstants.COMMAND_LIST);
        ICommandResult result = executeCommand(command, monitor);

        Document doc = getDocumentFromStrings(result.getOutput(), fDocumentBuilder);

        NodeList elements = doc.getElementsByTagName(MIStrings.NAME);

        ArrayList<String> retArray = new ArrayList<>();
        for (int i = 0; i < elements.getLength(); i++) {
            Node node = elements.item(i);
            if (node.getParentNode().getNodeName().equalsIgnoreCase(MIStrings.SESSION)) {
                retArray.add(node.getTextContent());
            }
        }
        return retArray;
    }

    @Override
    public ISessionInfo getSession(String sessionName, IProgressMonitor monitor) throws ExecutionException {
        ICommandInput command = createCommand(LTTngControlServiceConstants.COMMAND_LIST, sessionName);
        ICommandResult result = executeCommand(command, monitor);

        ISessionInfo sessionInfo = new SessionInfo(sessionName);
        Document document = getDocumentFromStrings(result.getOutput(), fDocumentBuilder);

        NodeList sessionsNode = document.getElementsByTagName(MIStrings.SESSION);
        // There should be only one session
        if (sessionsNode.getLength() != 1) {
            throw new ExecutionException(NLS.bind(Messages.TraceControl_MiInvalidNumberOfElementError, MIStrings.SESSION));
        }

        // Populate session information
        Node rawSession = sessionsNode.item(0);
        parseSession(sessionInfo, rawSession);

        // Fetch the snapshot info
        if (sessionInfo.isSnapshotSession()) {
            ISnapshotInfo snapshot = getSnapshotInfo(sessionName, monitor);
            sessionInfo.setSnapshotInfo(snapshot);
        }

        return sessionInfo;
    }

    /**
     * @param sessionInfo
     * @param rawSession
     * @throws ExecutionException
     */
    private void parseSession(ISessionInfo sessionInfo, Node rawSession) throws ExecutionException {
        if (!rawSession.getNodeName().equalsIgnoreCase(MIStrings.SESSION)) {
            throw new ExecutionException(Messages.TraceControl_MiInvalidElementError);
        }
        NodeList rawSessionInfos = rawSession.getChildNodes();
        for (int i = 0; i < rawSessionInfos.getLength(); i++) {
            Node rawInfo = rawSessionInfos.item(i);
            switch (rawInfo.getNodeName()) {
            case MIStrings.NAME:
                sessionInfo.setName(rawInfo.getTextContent());
                break;
            case MIStrings.PATH:
                sessionInfo.setSessionPath(rawInfo.getTextContent());
                break;
            case MIStrings.ENABLED:
                sessionInfo.setSessionState(rawInfo.getTextContent());
                break;
            case MIStrings.SNAPSHOT_MODE:
                if (rawInfo.getTextContent().equals(LTTngControlServiceConstants.TRUE_NUMERICAL)) {
                    // real name will be set later
                    ISnapshotInfo snapshotInfo = new SnapshotInfo(""); //$NON-NLS-1$
                    sessionInfo.setSnapshotInfo(snapshotInfo);
                }
                break;
            case MIStrings.LIVE_TIMER_INTERVAL:
                long liveDelay = Long.parseLong(rawInfo.getTextContent());
                if ((liveDelay > 0 && (liveDelay <= LTTngControlServiceConstants.MAX_LIVE_TIMER_INTERVAL))) {
                    sessionInfo.setLive(true);
                    sessionInfo.setLiveUrl(SessionInfo.DEFAULT_LIVE_NETWORK_URL);
                    sessionInfo.setLivePort(SessionInfo.DEFAULT_LIVE_PORT);
                    sessionInfo.setLiveDelay(liveDelay);
                }
                break;
            case MIStrings.DOMAINS:
                // Extract the domains node
                NodeList rawDomains = rawInfo.getChildNodes();
                IDomainInfo domain = null;
                for (int j = 0; j < rawDomains.getLength(); j++) {
                    if (rawDomains.item(j).getNodeName().equalsIgnoreCase(MIStrings.DOMAIN)) {
                        domain = parseDomain(rawDomains.item(j));
                        sessionInfo.addDomain(domain);
                    }
                }
                break;
            default:
                break;
            }
        }

        if (!sessionInfo.isSnapshotSession()) {
            Matcher matcher = LTTngControlServiceConstants.TRACE_NETWORK_PATTERN.matcher(sessionInfo.getSessionPath());
            if (matcher.matches()) {
                sessionInfo.setStreamedTrace(true);
            }
        }
    }

    /**
     * Parse a raw domain XML node to a IDomainInfo object
     *
     * @param rawDomain
     *            a domain xml node
     * @return a populated {@link DomainInfo} object
     * @throws ExecutionException
     *             when missing required xml element (type)
     */
    protected IDomainInfo parseDomain(Node rawDomain) throws ExecutionException {
        IDomainInfo domain = null;
        // Get the type
        Node rawType = getFirstOf(rawDomain.getChildNodes(), MIStrings.TYPE);
        if (rawType == null) {
            throw new ExecutionException(Messages.TraceControl_MiMissingRequiredError);
        }
        String rawTypeString = rawType.getTextContent().toLowerCase();
        TraceDomainType domainType = TraceDomainType.valueOfString(rawTypeString);
        switch (domainType) {
        case KERNEL:
            domain = new DomainInfo(Messages.TraceControl_KernelProviderDisplayName);
            domain.setDomain(TraceDomainType.KERNEL);
            break;
        case UST:
            domain = new DomainInfo(Messages.TraceControl_UstGlobalDomainDisplayName);
            domain.setDomain(TraceDomainType.UST);
            break;
        case JUL:
            domain = new DomainInfo(Messages.TraceControl_JULDomainDisplayName);
            domain.setDomain(TraceDomainType.JUL);
            break;
        case LOG4J:
            domain = new DomainInfo(Messages.TraceControl_LOG4JDomainDisplayName);
            domain.setDomain(TraceDomainType.LOG4J);
            break;
        case PYTHON:
            domain = new DomainInfo(Messages.TraceControl_PythonDomainDisplayName);
            domain.setDomain(TraceDomainType.PYTHON);
            break;
        case UNKNOWN:
            domain = new DomainInfo(Messages.TraceControl_UnknownDomainDisplayName);
            domain.setDomain(TraceDomainType.UNKNOWN);
            break;
            //$CASES-OMITTED$
        default:
            throw new ExecutionException(Messages.TraceControl_MiInvalidElementError);
        }

        NodeList rawInfos = rawDomain.getChildNodes();
        for (int i = 0; i < rawInfos.getLength(); i++) {
            Node rawInfo = rawInfos.item(i);
            switch (rawInfo.getNodeName()) {
            case MIStrings.BUFFER_TYPE:
                BufferType bufferType = BufferType.valueOfString(rawInfo.getTextContent());
                domain.setBufferType(bufferType);
                break;
            case MIStrings.CHANNELS:
                ArrayList<IChannelInfo> channels = new ArrayList<>();
                parseChannels(rawInfo.getChildNodes(), channels);
                if (!channels.isEmpty()) {
                    domain.setChannels(channels);
                }
                break;
            case MIStrings.EVENTS:
                ArrayList<ILoggerInfo> loggers = new ArrayList<>();
                getLoggerInfo(rawInfo.getChildNodes(), loggers, domain.getDomain());
                domain.setLoggers(loggers);
                break;
            default:
                break;
            }
        }

        return domain;
    }

    /**
     * Parse a list of raw channel XML node into an ArrayList of IChannelInfo
     *
     * @param rawChannes
     *            List of raw channel XML node
     * @param channels
     *            the parsed channels list
     * @throws ExecutionException
     *             when missing required xml element (type)
     */
    private static void parseChannels(NodeList rawChannels, ArrayList<IChannelInfo> channels) throws ExecutionException {
        IChannelInfo channel = null;
        for (int i = 0; i < rawChannels.getLength(); i++) {
            Node rawChannel = rawChannels.item(i);
            if (rawChannel.getNodeName().equalsIgnoreCase(MIStrings.CHANNEL)) {
                channel = new ChannelInfo(""); //$NON-NLS-1$

                // Populate the channel
                NodeList rawInfos = rawChannel.getChildNodes();
                Node rawInfo = null;
                for (int j = 0; j < rawInfos.getLength(); j++) {
                    rawInfo = rawInfos.item(j);
                    switch (rawInfo.getNodeName()) {
                    case MIStrings.NAME:
                        channel.setName(rawInfo.getTextContent());
                        break;
                    case MIStrings.ENABLED:
                        channel.setState(TraceEnablement.valueOfString(rawInfo.getTextContent()));
                        break;
                    case MIStrings.EVENTS:
                        List<IEventInfo> events = new ArrayList<>();
                        getEventInfo(rawInfo.getChildNodes(), events);
                        channel.setEvents(events);
                        break;
                    case MIStrings.ATTRIBUTES:
                        NodeList rawAttributes = rawInfo.getChildNodes();
                        for (int k = 0; k < rawAttributes.getLength(); k++) {
                            Node attribute = rawAttributes.item(k);
                            switch (attribute.getNodeName()) {
                            case MIStrings.OVERWRITE_MODE:
                                channel.setOverwriteMode(!LTTngControlServiceConstants.OVERWRITE_MODE_ATTRIBUTE_FALSE_MI.equalsIgnoreCase(attribute.getTextContent()));
                                break;
                            case MIStrings.SUBBUF_SIZE:
                                channel.setSubBufferSize(Long.valueOf(attribute.getTextContent()));
                                break;
                            case MIStrings.NUM_SUBBUF:
                                channel.setNumberOfSubBuffers(Integer.valueOf(attribute.getTextContent()));
                                break;
                            case MIStrings.SWITCH_TIMER_INTERVAL:
                                channel.setSwitchTimer(Long.valueOf(attribute.getTextContent()));
                                break;
                            case MIStrings.READ_TIMER_INTERVAL:
                                channel.setReadTimer(Long.valueOf(attribute.getTextContent()));
                                break;
                            case MIStrings.OUTPUT_TYPE:
                                channel.setOutputType(attribute.getTextContent());
                                break;
                            case MIStrings.TRACEFILE_SIZE:
                                channel.setMaxSizeTraceFiles(Long.parseLong(attribute.getTextContent()));
                                break;
                            case MIStrings.TRACEFILE_COUNT:
                                channel.setMaxNumberTraceFiles(Integer.parseInt(attribute.getTextContent()));
                                break;
                            case MIStrings.LIVE_TIMER_INTERVAL:
                                // TODO: currently not supported by tmf
                                break;
                            case MIStrings.DISCARDED_EVENTS:
                                channel.setNumberOfDiscardedEvents(Long.parseLong(attribute.getTextContent()));
                                break;
                            case MIStrings.LOST_PACKETS:
                                channel.setNumberOfLostPackets(Long.parseLong(attribute.getTextContent()));
                                break;
                            default:
                                break;
                            }
                        }
                        break;
                    default:
                        break;
                    }
                }
                channels.add(channel);
            }
        }

    }

    @Override
    public ISnapshotInfo getSnapshotInfo(String sessionName, IProgressMonitor monitor) throws ExecutionException {
        // TODO A session can have multiple snapshot output. This need to be
        // supported in the future.
        // Currently the SessionInfo object does not support multiple snashot
        // output.
        // For now only keep the last one.
        ICommandInput command = createCommand(LTTngControlServiceConstants.COMMAND_SNAPSHOT, LTTngControlServiceConstants.COMMAND_LIST_SNAPSHOT_OUTPUT, LTTngControlServiceConstants.OPTION_SESSION, sessionName);
        ICommandResult result = executeCommand(command, monitor);
        Document doc = getDocumentFromStrings(result.getOutput(), fDocumentBuilder);
        NodeList rawSnapshotsOutputs = doc.getElementsByTagName(MIStrings.SNAPSHOT_OUTPUTS);

        ISnapshotInfo snapshotInfo = new SnapshotInfo(""); //$NON-NLS-1$

        // TODO: tmf does not have a notion of a ctrl url.
        for (int i = 0; i < rawSnapshotsOutputs.getLength(); i++) {
            NodeList rawSnapshotOutput = rawSnapshotsOutputs.item(i).getChildNodes();
            for (int j = 0; j < rawSnapshotOutput.getLength(); j++) {
                Node rawInfo = rawSnapshotOutput.item(j);
                switch (rawInfo.getNodeName()) {
                case MIStrings.ID:
                    snapshotInfo.setId(Integer.parseInt(rawInfo.getTextContent()));
                    break;
                case MIStrings.NAME:
                    snapshotInfo.setName(rawInfo.getTextContent());
                    break;
                case MIStrings.SNAPSHOT_CTRL_URL:
                    // The use of the ctrl_url for the snapshot path is to assure
                    // basic support. Refactoring is necessary in lttng and
                    // tmf side.
                    // See http://bugs.lttng.org/issues/828 (+comment)
                    snapshotInfo.setSnapshotPath(rawInfo.getTextContent());
                    break;
                default:
                    break;
                }
            }
        }

        // Check if the snapshot output is Streamed
        Matcher matcher2 = LTTngControlServiceConstants.TRACE_NETWORK_PATTERN.matcher(snapshotInfo.getSnapshotPath());
        if (matcher2.matches()) {
            snapshotInfo.setStreamedSnapshot(true);
        }

        return snapshotInfo;
    }

    @Override
    public List<IBaseEventInfo> getKernelProvider(IProgressMonitor monitor) throws ExecutionException {
        // Tracepoint events
        ICommandInput tracepointCommand = createCommand(LTTngControlServiceConstants.COMMAND_LIST, LTTngControlServiceConstants.OPTION_KERNEL);
        ICommandResult tracepointResult = executeCommand(tracepointCommand, monitor, false);
        List<IBaseEventInfo> events = new ArrayList<>();

        if (isError(tracepointResult)) {
            // Ignore the following 2 cases:
            // Spawning a session daemon
            // Error: Unable to list kernel events
            // or:
            // Error: Unable to list kernel events
            if (ignoredPattern(tracepointResult.getErrorOutput(), LTTngControlServiceConstants.LIST_KERNEL_NO_KERNEL_PROVIDER_PATTERN)) {
                return events;
            }
            throw new ExecutionException(Messages.TraceControl_CommandError + tracepointCommand.toString());
        }

        Document tracepointDocument = getDocumentFromStrings(tracepointResult.getOutput(), fDocumentBuilder);
        NodeList rawTracepointEvents = tracepointDocument.getElementsByTagName(MIStrings.EVENT);
        getBaseEventInfo(rawTracepointEvents, events);

        // Syscall events
        ICommandInput syscallCommand = createCommand(LTTngControlServiceConstants.COMMAND_LIST, LTTngControlServiceConstants.OPTION_KERNEL, LTTngControlServiceConstants.OPTION_SYSCALL);
        ICommandResult syscallResult = executeCommand(syscallCommand, monitor, false);
        List<IBaseEventInfo> syscallEvents = new ArrayList<>();

        if (isError(syscallResult)) {
            throw new ExecutionException(Messages.TraceControl_CommandError + syscallCommand.toString());
        }

        Document syscallDocument = getDocumentFromStrings(syscallResult.getOutput(), fDocumentBuilder);
        NodeList rawSyscallEvents = syscallDocument.getElementsByTagName(MIStrings.EVENT);
        getBaseEventInfo(rawSyscallEvents, syscallEvents);

        // Merge the tracepoint events with the syscall events (all under the Kernel provider)
        events.addAll(syscallEvents);
        return events;
    }

    @Override
    public List<IUstProviderInfo> getUstProvider(IProgressMonitor monitor) throws ExecutionException {
        ICommandInput command = createCommand(LTTngControlServiceConstants.COMMAND_LIST, LTTngControlServiceConstants.OPTION_UST);
        // Get the field too
        command.add(LTTngControlServiceConstants.OPTION_FIELDS);
        // Execute UST listing
        ICommandResult result = executeCommand(command, monitor, false);
        List<IUstProviderInfo> allProviders = new ArrayList<>();

        if (isError(result)) {
            // Ignore the following 2 cases:
            // Spawning a session daemon
            // Error: Unable to list UST events: Listing UST events failed
            // or:
            // Error: Unable to list UST events: Listing UST events failed
            if (ignoredPattern(result.getErrorOutput(), LTTngControlServiceConstants.LIST_UST_NO_UST_PROVIDER_PATTERN)) {
                return allProviders;
            }
            throw new ExecutionException(Messages.TraceControl_CommandError + command.toString());
        }

        Document document = getDocumentFromStrings(result.getOutput(), fDocumentBuilder);
        NodeList rawProviders = document.getElementsByTagName(MIStrings.PID);

        IUstProviderInfo providerInfo = null;

        for (int i = 0; i < rawProviders.getLength(); i++) {
            Node provider = rawProviders.item(i);
            Node name = getFirstOf(provider.getChildNodes(), MIStrings.NAME);
            if (name == null) {
                throw new ExecutionException(Messages.TraceControl_MiInvalidProviderError);
            }
            providerInfo = new UstProviderInfo(name.getTextContent());

            // Populate provider
            NodeList infos = provider.getChildNodes();
            for (int j = 0; j < infos.getLength(); j++) {
                Node info = infos.item(j);
                switch (info.getNodeName()) {
                case MIStrings.PID_ID:
                    providerInfo.setPid(Integer.parseInt(info.getTextContent()));
                    break;
                case MIStrings.EVENTS:
                    List<IBaseEventInfo> events = new ArrayList<>();
                    NodeList rawEvents = info.getChildNodes();
                    getBaseEventInfo(rawEvents, events);
                    providerInfo.setEvents(events);
                    break;
                default:
                    break;
                }
            }
            allProviders.add(providerInfo);
        }

        if (isVersionSupported("2.6")) { //$NON-NLS-1$
            getUstProviderLoggers(allProviders, TraceDomainType.JUL, monitor);
            getUstProviderLoggers(allProviders, TraceDomainType.LOG4J, monitor);
            if (isVersionSupported("2.7")) { //$NON-NLS-1$
                getUstProviderLoggers(allProviders, TraceDomainType.PYTHON, monitor);
            }
        }

        return allProviders;
    }

    @Override
    public ISessionInfo createSession(ISessionInfo sessionInfo, IProgressMonitor monitor) throws ExecutionException {
        if (sessionInfo.isStreamedTrace()) {
            return createStreamedSession(sessionInfo, monitor);
        }

        ICommandInput command = prepareSessionCreationCommand(sessionInfo);
        ICommandResult result = executeCommand(command, monitor);

        Document document = getDocumentFromStrings(result.getOutput(), fDocumentBuilder);
        NodeList sessions = document.getElementsByTagName(MIStrings.SESSION);

        // Number of session should be equal to 1
        if (sessions.getLength() != 1) {
            throw new ExecutionException(Messages.TraceControl_CommandError + " " + command + "\n" //$NON-NLS-1$//$NON-NLS-2$
                    + NLS.bind(Messages.TraceControl_UnexpectedNumberOfElementError, MIStrings.SESSION) + " " + sessions.getLength()); //$NON-NLS-1$
        }

        // Fetch a session from output
        ISessionInfo outputSession = new SessionInfo(""); //$NON-NLS-1$
        parseSession(outputSession, sessions.item(0));

        // Verify session name
        if ((outputSession.getName().equals("")) || (!"".equals(sessionInfo.getName()) && !outputSession.getName().equals(sessionInfo.getName()))) { //$NON-NLS-1$ //$NON-NLS-2$
            // Unexpected name returned
            throw new ExecutionException(Messages.TraceControl_CommandError + " " + command + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                    Messages.TraceControl_UnexpectedNameError + ": " + outputSession.getName()); //$NON-NLS-1$
        }

        // Verify session path
        if (!sessionInfo.isSnapshotSession() &&
                ((outputSession.getSessionPath() == null) || ((sessionInfo.getSessionPath() != null) && (!outputSession.getSessionPath().contains(sessionInfo.getSessionPath()))))) {
            // Unexpected path
            throw new ExecutionException(Messages.TraceControl_CommandError + " " + command + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                    Messages.TraceControl_UnexpectedPathError + ": " + outputSession.getName()); //$NON-NLS-1$
        }

        if (sessionInfo.isSnapshotSession()) {
            // Make it a snapshot session - content of snapshot info need to
            // set afterwards using getSession() or getSnapshotInfo()
            outputSession.setSnapshotInfo(new SnapshotInfo("")); //$NON-NLS-1$
        }

        return outputSession;
    }

    private @NonNull ISessionInfo createStreamedSession(ISessionInfo sessionInfo, IProgressMonitor monitor) throws ExecutionException {

        ICommandInput command = prepareStreamedSessionCreationCommand(sessionInfo);

        ICommandResult result = executeCommand(command, monitor);

        Document document = getDocumentFromStrings(result.getOutput(), fDocumentBuilder);
        NodeList sessions = document.getElementsByTagName(MIStrings.SESSION);

        // Number of session should be equal to 1
        if (sessions.getLength() != 1) {
            throw new ExecutionException(Messages.TraceControl_CommandError + " " + command + "\n" //$NON-NLS-1$//$NON-NLS-2$
                    + NLS.bind(Messages.TraceControl_UnexpectedNumberOfElementError, MIStrings.SESSION) + " " + sessions.getLength()); //$NON-NLS-1$
        }

        // Fetch a session from output
        ISessionInfo outputSession = new SessionInfo(""); //$NON-NLS-1$
        parseSession(outputSession, sessions.item(0));

        // Verify session name
        if ((outputSession.getName().equals("")) || (!"".equals(sessionInfo.getName()) && !outputSession.getName().equals(sessionInfo.getName()))) { //$NON-NLS-1$ //$NON-NLS-2$
            // Unexpected name returned
            throw new ExecutionException(Messages.TraceControl_CommandError + " " + command + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                    Messages.TraceControl_UnexpectedNameError + ": " + outputSession.getName()); //$NON-NLS-1$
        }

        sessionInfo.setName(outputSession.getName());
        sessionInfo.setStreamedTrace(true);

        // Verify session path
        if (sessionInfo.getNetworkUrl() != null) {
            if (!sessionInfo.isSnapshotSession() && (outputSession.getSessionPath() == null)) {
                // Unexpected path
                throw new ExecutionException(Messages.TraceControl_CommandError + " " + command + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                        Messages.TraceControl_UnexpectedPathError + ": " + outputSession.getName()); //$NON-NLS-1$
            }

            if (sessionInfo.isSnapshotSession()) {
                sessionInfo.setStreamedTrace(false);
            } else {
                sessionInfo.setSessionPath(outputSession.getSessionPath());
                // Check file protocol
                Matcher matcher = LTTngControlServiceConstants.TRACE_FILE_PROTOCOL_PATTERN.matcher(outputSession.getSessionPath());
                if (matcher.matches()) {
                    sessionInfo.setStreamedTrace(false);
                }
            }
        }

        // When using controlUrl and dataUrl the full session path is not known
        // yet
        // and will be set later on when listing the session
        return sessionInfo;
    }

    @Override
    public void destroySession(String sessionName, IProgressMonitor monitor) throws ExecutionException {
        ICommandInput command = createCommand(LTTngControlServiceConstants.COMMAND_DESTROY_SESSION, sessionName);

        ICommandResult result = executeCommand(command, monitor, false);
        List<String> errorOutput = result.getErrorOutput();

        if (isError(result)) {
            // Don't treat this as an error
            if (ignoredPattern(errorOutput, LTTngControlServiceConstants.SESSION_NOT_FOUND_ERROR_PATTERN)) {
                return;

            }
            throw new ExecutionException(Messages.TraceControl_CommandError + " " + command.toString() + "\n" + result.toString()); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Check for action effect
        Document doc = getDocumentFromStrings(result.getOutput(), fDocumentBuilder);
        NodeList sessions = doc.getElementsByTagName(MIStrings.SESSION);
        if (sessions.getLength() != 1) {
            throw new ExecutionException(NLS.bind(Messages.TraceControl_MiInvalidNumberOfElementError, MIStrings.SESSION));
        }

        Node rawSessionName = getFirstOf(sessions.item(0).getChildNodes(), MIStrings.NAME);
        if (rawSessionName == null) {
            throw new ExecutionException(Messages.TraceControl_MiMissingRequiredError);
        }

        // Validity check
        if (!rawSessionName.getTextContent().equals(sessionName)) {
            throw new ExecutionException(NLS.bind(Messages.TraceControl_UnexpectedValueError, rawSessionName.getTextContent(), sessionName));
        }
    }

    @Override
    protected ICommandInput createCommand(String... strings) {
        ICommandInput command = getCommandShell().createCommand();
        command.add(LTTngControlServiceConstants.CONTROL_COMMAND);
        List<@NonNull String> groupOption = getTracingGroupOption();
        if (!groupOption.isEmpty()) {
            command.addAll(groupOption);
        }
        command.add(LTTngControlServiceConstants.CONTROL_COMMAND_MI_OPTION);
        command.add(LTTngControlServiceConstants.CONTROL_COMMAND_MI_XML);
        for (String string : strings) {
            command.add(checkNotNull(string));
        }
        return command;
    }

    /**
     * @param xmlBaseEvents
     *            a Node list of base xml event element
     * @param events
     *            list of event generated by the parsing of the xml event
     *            element
     * @throws ExecutionException
     *             when a raw event is not a complete/valid xml event
     */
    private static void getBaseEventInfo(NodeList xmlBaseEvents, List<IBaseEventInfo> events) throws ExecutionException {
        IBaseEventInfo eventInfo = null;
        for (int i = 0; i < xmlBaseEvents.getLength(); i++) {
            NodeList rawInfos = xmlBaseEvents.item(i).getChildNodes();
            // Search for name
            if (xmlBaseEvents.item(i).getNodeName().equalsIgnoreCase(MIStrings.EVENT)) {
                Node rawName = getFirstOf(rawInfos, MIStrings.NAME);
                if (rawName == null) {
                    throw new ExecutionException(Messages.TraceControl_MiMissingRequiredError);
                }
                eventInfo = new BaseEventInfo(rawName.getTextContent());

                // Populate the event
                for (int j = 0; j < rawInfos.getLength(); j++) {
                    Node infoNode = rawInfos.item(j);
                    switch (infoNode.getNodeName()) {
                    case MIStrings.TYPE:
                        eventInfo.setEventType(infoNode.getTextContent());
                        break;
                    case MIStrings.LOGLEVEL:
                        eventInfo.setLogLevel(infoNode.getTextContent());
                        break;
                    case MIStrings.EVENT_FIELDS:
                        List<IFieldInfo> fields = new ArrayList<>();
                        getFieldInfo(infoNode.getChildNodes(), fields);
                        eventInfo.setFields(fields);
                        break;
                    default:
                        break;
                    }
                }
                events.add(eventInfo);
            }
        }
    }

    /**
     * @param xmlBaseEvents
     *            a Node list of xml event element linked to a session
     * @param events
     *            list of event generated by the parsing of the xml event
     *            element
     * @throws ExecutionException
     *             when a raw event is not a complete/valid xml event
     */
    static void getEventInfo(NodeList xmlEvents, List<IEventInfo> events) throws ExecutionException {
        IEventInfo eventInfo = null;
        for (int i = 0; i < xmlEvents.getLength(); i++) {
            NodeList rawInfos = xmlEvents.item(i).getChildNodes();
            // Search for name
            if (xmlEvents.item(i).getNodeName().equalsIgnoreCase(MIStrings.EVENT)) {
                Node rawName = getFirstOf(rawInfos, MIStrings.NAME);
                if (rawName == null) {
                    throw new ExecutionException(Messages.TraceControl_MiMissingRequiredError);
                }

                eventInfo = new EventInfo(rawName.getTextContent());

                // Basic information
                for (int j = 0; j < rawInfos.getLength(); j++) {
                    Node infoNode = rawInfos.item(j);
                    switch (infoNode.getNodeName()) {
                    case MIStrings.TYPE:
                        eventInfo.setEventType(infoNode.getTextContent());
                        break;
                    case MIStrings.LOGLEVEL_TYPE:
                        eventInfo.setLogLevelType(LogLevelType.valueOfString(infoNode.getTextContent()));
                        break;
                    case MIStrings.LOGLEVEL:
                        eventInfo.setLogLevel(TraceLogLevel.valueOfString(infoNode.getTextContent()));
                        break;
                    case MIStrings.ENABLED:
                        eventInfo.setState(TraceEnablement.valueOfString(infoNode.getTextContent()));
                        break;
                    case MIStrings.FILTER:
                        // Before LTTng 2.8: We emulate the non-mi behavior and simply put
                        // "with filter"
                        if (Boolean.TRUE.toString().equals(infoNode.getTextContent())) {
                            eventInfo.setFilterExpression(Messages.TraceControl_DefaultEventFilterString);
                        }
                        break;
                    case MIStrings.FILTER_EXPRESSION:
                        eventInfo.setFilterExpression(infoNode.getTextContent());
                        break;
                    case MIStrings.EXCLUSION:
                        // Before LTTng 2.8: We emulate the non-mi behavior and simply put
                        // "with exclude"
                        if (Boolean.TRUE.toString().equals(infoNode.getTextContent())) {
                            eventInfo.setExcludedEvents(Messages.TraceControl_DefaultEventExcludeString);
                        }
                        break;
                    case MIStrings.EXCLUSIONS:
                        StringBuilder tmpString = new StringBuilder();
                        // If there is multiple events excluded.
                        for (int k = 0; k < infoNode.getChildNodes().getLength(); k++) {
                            if (k > 0) {
                                tmpString.append(", "); //$NON-NLS-1$
                            }
                            tmpString.append(infoNode.getChildNodes().item(k).getTextContent());
                        }
                        eventInfo.setExcludedEvents(tmpString.toString());
                        break;
                    default:
                        break;
                    }
                }

                boolean isProbeFunction = (eventInfo.getEventType().equals(TraceEventType.PROBE)) || (eventInfo.getEventType().equals(TraceEventType.FUNCTION));
                if (isProbeFunction) {
                    IProbeEventInfo probeEvent = new ProbeEventInfo(eventInfo);
                    eventInfo = probeEvent;

                    Node rawDataNode = null;
                    switch (probeEvent.getEventType()) {
                    case FUNCTION:
                    case PROBE: {
                        // get attributes
                        Node rawAttributes = getFirstOf(rawInfos, MIStrings.ATTRIBUTES);
                        if (rawAttributes == null) {
                            throw new ExecutionException(Messages.TraceControl_MiMissingRequiredError);
                        }
                        rawDataNode = getFirstOf(rawAttributes.getChildNodes(), MIStrings.PROBE_ATTRIBUTES);
                        break;
                    }
                    case SYSCALL:
                    case TRACEPOINT:
                    case UNKNOWN:
                    default:
                        throw new ExecutionException(Messages.TraceControl_MiInvalidElementError);
                    }

                    if (rawDataNode == null) {
                        throw new ExecutionException(Messages.TraceControl_MiInvalidElementError);
                    }

                    // Extract info
                    NodeList rawDatas = rawDataNode.getChildNodes();
                    for (int j = 0; j < rawDatas.getLength(); j++) {
                        Node rawData = rawDatas.item(j);
                        switch (rawData.getNodeName()) {
                        case MIStrings.SYMBOL_NAME:
                            probeEvent.setSymbol(rawData.getTextContent());
                            break;
                        case MIStrings.ADDRESS:
                            probeEvent.setAddress(String.format("%#016x", new BigInteger(rawData.getTextContent()))); //$NON-NLS-1$
                            break;
                        case MIStrings.OFFSET:
                            probeEvent.setOffset(String.format("%#016x", new BigInteger(rawData.getTextContent()))); //$NON-NLS-1$
                            break;
                        default:
                            break;
                        }
                    }
                }

                // Add the event
                events.add(eventInfo);
            }
        }
    }

    static void getLoggerInfo(NodeList xmlEvents, List<ILoggerInfo> loggers, TraceDomainType domain) throws ExecutionException {
        ILoggerInfo loggerInfo = null;
        for (int i = 0; i < xmlEvents.getLength(); i++) {
            NodeList rawInfos = xmlEvents.item(i).getChildNodes();
            // Search for name
            if (xmlEvents.item(i).getNodeName().equalsIgnoreCase(MIStrings.EVENT)) {
                Node rawName = getFirstOf(rawInfos, MIStrings.NAME);
                if (rawName == null) {
                    throw new ExecutionException(Messages.TraceControl_MiMissingRequiredError);
                }

                loggerInfo = new LoggerInfo(rawName.getTextContent());
                loggerInfo.setDomain(domain);

                // Basic information
                for (int j = 0; j < rawInfos.getLength(); j++) {
                    Node infoNode = rawInfos.item(j);
                    switch (infoNode.getNodeName()) {
                    case MIStrings.LOGLEVEL_TYPE:
                        loggerInfo.setLogLevelType(LogLevelType.valueOfString(infoNode.getTextContent()));
                        break;
                    case MIStrings.LOGLEVEL:
                        switch (domain) {
                        case JUL:
                            loggerInfo.setLogLevel(TraceJulLogLevel.valueOfString(infoNode.getTextContent()));
                            break;
                        case LOG4J:
                            loggerInfo.setLogLevel(TraceLog4jLogLevel.valueOfString(infoNode.getTextContent()));
                            break;
                        case PYTHON:
                            loggerInfo.setLogLevel(TracePythonLogLevel.valueOfString(infoNode.getTextContent()));
                            break;
                            //$CASES-OMITTED$
                        default:
                            break;
                        }
                        break;
                    case MIStrings.ENABLED:
                        loggerInfo.setState(TraceEnablement.valueOfString(infoNode.getTextContent()));
                        break;
                    default:
                        break;
                    }
                }
                // Add the event
                loggers.add(loggerInfo);
            }
        }
    }

    /**
     * @param fieldsList
     *            a list of xml event_field element
     * @param fields
     *            a list of field generated by xml parsing
     * @throws ExecutionException
     *             when parsing fail or required elements are missing
     */
    private static void getFieldInfo(NodeList fieldsList, List<IFieldInfo> fields) throws ExecutionException {
        IFieldInfo fieldInfo = null;
        for (int i = 0; i < fieldsList.getLength(); i++) {
            Node field = fieldsList.item(i);
            if (field.getNodeName().equalsIgnoreCase(MIStrings.EVENT_FIELD)) {
                // Get name
                Node name = getFirstOf(field.getChildNodes(), MIStrings.NAME);
                if (name == null) {
                    throw new ExecutionException(Messages.TraceControl_MiMissingRequiredError);
                }
                fieldInfo = new FieldInfo(name.getTextContent());

                // Populate the field information
                NodeList infos = field.getChildNodes();
                for (int j = 0; j < infos.getLength(); j++) {
                    Node info = infos.item(j);
                    switch (info.getNodeName()) {
                    case MIStrings.TYPE:
                        fieldInfo.setFieldType(info.getTextContent());
                        break;
                    default:
                        break;
                    }
                }
                fields.add(fieldInfo);
            }
        }
    }

    /**
     * Retrieve the fist instance of a given node with tag name equal to tagName
     * parameter
     *
     * @param nodeList
     *            the list of Node to search against
     * @param tagName
     *            the tag name of the desired node
     * @return the first occurrence of a node with a tag name equals to tagName
     */
    private static @Nullable Node getFirstOf(NodeList nodeList, String tagName) {
        Node node = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (Objects.equals(nodeList.item(i).getNodeName(), tagName)) {
                node = nodeList.item(i);
                break;
            }
        }
        return node;
    }

    /**
     * Retrieve the loggers of a certain domain type for the UST provider.
     *
     * @param allProviders
     *            the list of UST providers
     * @param domain
     *            the loggers domain
     * @param monitor
     *            progress monitor
     * @throws ExecutionException
     */
    private void getUstProviderLoggers(List<IUstProviderInfo> allProviders, TraceDomainType domain, IProgressMonitor monitor) throws ExecutionException {
        // Getting the loggers information since those are under the UST provider
        ICommandInput command = createCommand(LTTngControlServiceConstants.COMMAND_LIST);
        switch (domain) {
        case JUL:
            command.add(LTTngControlServiceConstants.OPTION_JUL);
            break;
        case LOG4J:
            command.add(LTTngControlServiceConstants.OPTION_LOG4J);
            break;
        case PYTHON:
            command.add(LTTngControlServiceConstants.OPTION_PYTHON);
            break;
            //$CASES-OMITTED$
        default:
            break;
        }
        // Execute listing
        ICommandResult result = executeCommand(command, monitor, false);

        if (isError(result)) {
            // Ignore the following case (example jul):
            // Error: Unable to list jul events: Session daemon agent tracing is disabled
            if (ignoredPattern(result.getErrorOutput(), LTTngControlServiceConstants.LIST_UST_NO_UST_PROVIDER_PATTERN)) {
                return;
            }
            throw new ExecutionException(Messages.TraceControl_CommandError + command.toString());
        }

        Document document = getDocumentFromStrings(result.getOutput(), fDocumentBuilder);
        NodeList rawProviders = document.getElementsByTagName(MIStrings.PID);

        for (int i = 0; i < rawProviders.getLength(); i++) {
            Node provider = rawProviders.item(i);
            Node name = getFirstOf(provider.getChildNodes(), MIStrings.NAME);
            if (name == null) {
                throw new ExecutionException(Messages.TraceControl_MiInvalidProviderError);
            }

            Node id = getFirstOf(provider.getChildNodes(), MIStrings.PID_ID);

            if (id != null) {
                for (int k = 0; k < allProviders.size(); k++) {
                    if (allProviders.get(k).getPid() == Integer.parseInt(id.getTextContent())) {
                        Node events = getFirstOf(provider.getChildNodes(), MIStrings.EVENTS);
                        if (events != null) {
                            List<ILoggerInfo> loggers = new ArrayList<>();
                            NodeList rawEvents = events.getChildNodes();
                            getLoggerInfo(rawEvents, loggers, domain);
                            for (ILoggerInfo logger : loggers) {
                                logger.setDomain(domain);
                            }
                            allProviders.get(k).addLoggers(loggers);
                        }
                    }
                }
            }
        }
    }

    @Override
    public @NonNull List<String> getContextList(IProgressMonitor monitor) throws ExecutionException {
        if (!isVersionSupported("2.8.0")) { //$NON-NLS-1$
            return super.getContextList(monitor);
        }

        ICommandInput command = createCommand(LTTngControlServiceConstants.COMMAND_ADD_CONTEXT, LTTngControlServiceConstants.OPTION_LIST);
        ICommandResult result = executeCommand(command, monitor);

        if (!isVersionSupported("2.11.0")) { //$NON-NLS-1$
            return result.getOutput();
        }

        if (isError(result)) {
            throw new ExecutionException(Messages.TraceControl_CommandError + command.toString());
        }
        Document document = getDocumentFromStrings(result.getOutput(), fDocumentBuilder);
        NodeList rawContexts = document.getElementsByTagName(MIStrings.CONTEXT);

        List<String> returnedContexts = new ArrayList<>();
        for (int i = 0; i < rawContexts.getLength(); i++) {
            Node contextNode = rawContexts.item(i);
            Node symbol = getFirstOf(contextNode.getChildNodes(), MIStrings.SYMBOL);
            if (symbol != null) {
                returnedContexts.add(symbol.getTextContent());
            }
        }
        return returnedContexts;
    }
}
