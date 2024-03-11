/*******************************************************************************
 * Copyright (c) 2015, 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.stream;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TsdlUtils.isAnyUnaryString;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.CTFStrings;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IEventHeaderDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.CtfCoreLoggerUtil;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.CTFJsonMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonDataStreamMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.JsonStructureFieldMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.MetadataStrings;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeSpecifierListParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.trace.CTFStream;

/**
 *
 * An <em>event stream</em> can be divided into contiguous event packets of
 * variable size. An event packet can contain a certain amount of padding at the
 * end. The stream header is repeated at the beginning of each event packet.
 * <br>
 * The event stream header will therefore be referred to as the <em>event packet
 * header</em> throughout the rest of this document. <br>
 *
 * An event stream is divided in contiguous event packets of variable size.
 * These subdivisions allow the trace analyzer to perform a fast binary search
 * by time within the stream (typically requiring to index only the event packet
 * headers) without reading the whole stream. These subdivisions have a variable
 * size to eliminate the need to transfer the event packet padding when
 * partially filled event packets must be sent when streaming a trace for live
 * viewing/analysis. An event packet can contain a certain amount of padding at
 * the end. Dividing streams into event packets is also useful for network
 * streaming over UDP and flight recorder mode tracing (a whole event packet can
 * be swapped out of the buffer atomically for reading). <br>
 * The stream header is repeated at the beginning of each event packet to allow
 * flexibility in terms of:
 * <ul>
 * <li>streaming support</li>
 * <li>allowing arbitrary buffers to be discarded without making the trace
 * unreadable</li>
 * <li>allow UDP packet loss handling by either dealing with missing event
 * packet or asking for re-transmission</li>
 * <li>transparently support flight recorder mode</li>
 * <li>transparently support crash dump</li>
 * </ul>
 *
 * @author Matthew Khouzam
 * @author Efficios - Description
 *
 */
public final class StreamDeclarationParser extends AbstractScopedCommonTreeParser {

    private static final String IDENTIFIER_MUST_BE_A_STRING = "Left side of CTF assignment must be a string"; //$NON-NLS-1$
    private static final String PACKET_CONTEXT = "packet.context "; //$NON-NLS-1$
    private static final String EVENT_CONTEXT = "event.context "; //$NON-NLS-1$
    private static final String EVENT_HEADER = "event.header "; //$NON-NLS-1$
    private static final String STREAM_ID = "stream id "; //$NON-NLS-1$
    private static final String EXPECTS_A_STRUCT = "expects a struct"; //$NON-NLS-1$
    private static final String SCOPE_NOT_FOUND = "scope not found"; //$NON-NLS-1$
    private static final String EXPECTS_A_TYPE_SPECIFIER = "expects a type specifier"; //$NON-NLS-1$
    private static final String ALREADY_DEFINED = "already defined"; //$NON-NLS-1$

    /**
     * A parameter object, contains a trace, a stream and a scope
     *
     * @author Matthew Khouzam
     *
     */
    @NonNullByDefault
    public static final class Param implements ICommonTreeParserParameter {
        private final CTFStream fStream;
        private final CTFTrace fTrace;
        private final DeclarationScope fDeclarationScope;

        /**
         * The parameter object
         *
         * @param trace
         *            the trace
         * @param stream
         *            the stream
         * @param scope
         *            the scope
         */
        public Param(CTFTrace trace, CTFStream stream, DeclarationScope scope) {
            fTrace = trace;
            fStream = stream;
            fDeclarationScope = scope;
        }

    }

    /**
     * Instance
     */
    public static final StreamDeclarationParser INSTANCE = new StreamDeclarationParser();

    private StreamDeclarationParser() {
    }

    /**
     * Parse a stream declaration.
     *
     * @param streamDecl
     *            the AST node containing the STREAM type
     * @param param
     *            The stream to fill, the trace and the current scope
     * @return the stream declaration
     * @throws ParseException
     *             if the stream AST node is malformed
     */
    @Override
    public CTFStream parse(ICTFMetadataNode streamDecl, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        DeclarationScope scope = (((Param) param).fDeclarationScope);
        CTFStream stream = ((Param) param).fStream;
        CTFTrace fTrace = ((Param) param).fTrace;

        if (streamDecl instanceof CTFJsonMetadataNode) {
            JsonDataStreamMetadataNode decl = (JsonDataStreamMetadataNode) streamDecl;

            if (decl.getId() >= 0) {
                long streamId = decl.getId();
                stream.setId(streamId);
            }

            JsonStructureFieldMetadataNode eventHeader = decl.getEventRecordHeaderClass();
            if (eventHeader != null) {
                IDeclaration eventHeaderDecl = TypeSpecifierListParser.INSTANCE.parse(eventHeader, new TypeSpecifierListParser.Param(fTrace, null, null, scope));
                DeclarationScope eventHeaderScope = lookupStructName(eventHeader, scope);
                verifyEventHeaderScope(eventHeaderScope);
                setEventHeader(stream, eventHeaderDecl);
            }

            JsonStructureFieldMetadataNode eventContext = decl.getEventRecordCommonContextClass();
            if (eventContext != null) {
                IDeclaration eventContextDecl = TypeSpecifierListParser.INSTANCE.parse(eventContext, new TypeSpecifierListParser.Param(fTrace, null, null, scope));
                verifyEventContext(eventContextDecl);
                stream.setEventContext((StructDeclaration) eventContextDecl);
            }

            JsonStructureFieldMetadataNode packetContext = decl.getPacketContextFieldClass();
            if (packetContext != null) {
                IDeclaration packetContextDecl = TypeSpecifierListParser.INSTANCE.parse(packetContext, new TypeSpecifierListParser.Param(fTrace, null, null, scope));
                verifyPacketContext(packetContextDecl);
                stream.setPacketContext((StructDeclaration) packetContextDecl);
            }
        } else {
            /* There should be a left and right */

            ICTFMetadataNode leftNode = streamDecl.getChild(0);
            ICTFMetadataNode rightNode = streamDecl.getChild(1);

            List<ICTFMetadataNode> leftStrings = leftNode.getChildren();

            if (!isAnyUnaryString(leftStrings.get(0))) {
                throw new ParseException(IDENTIFIER_MUST_BE_A_STRING);
            }

            String left = concatenateUnaryStrings(leftStrings);

            if (left.equals(MetadataStrings.ID)) {
                if (stream.isIdSet()) {
                    throw new ParseException(STREAM_ID + ALREADY_DEFINED);
                }

                long streamID = StreamIdParser.INSTANCE.parse(rightNode, null);

                stream.setId(streamID);
            } else if (left.equals(MetadataStrings.EVENT_HEADER)) {
                if (stream.isEventHeaderSet()) {
                    throw new ParseException(EVENT_HEADER + ALREADY_DEFINED);
                }

                ICTFMetadataNode typeSpecifier = rightNode.getChild(0);

                if (!(CTFParser.tokenNames[CTFParser.TYPE_SPECIFIER_LIST].equals(typeSpecifier.getType()))) {
                    throw new ParseException(EVENT_HEADER + EXPECTS_A_TYPE_SPECIFIER);
                }

                IDeclaration eventHeaderDecl = TypeSpecifierListParser.INSTANCE.parse(typeSpecifier, new TypeSpecifierListParser.Param(fTrace, null, null, scope));
                DeclarationScope eventHeaderScope = lookupStructName(typeSpecifier, scope);
                DeclarationScope eventScope = new DeclarationScope(scope, MetadataStrings.EVENT);
                verifyEventHeaderScope(eventHeaderScope);
                eventHeaderScope.setName(CTFStrings.HEADER);
                eventScope.addChild(eventHeaderScope);
                setEventHeader(stream, eventHeaderDecl);
            } else if (left.equals(MetadataStrings.EVENT_CONTEXT)) {
                if (stream.isEventContextSet()) {
                    throw new ParseException(EVENT_CONTEXT + ALREADY_DEFINED);
                }

                ICTFMetadataNode typeSpecifier = rightNode.getChild(0);

                if (!(CTFParser.tokenNames[CTFParser.TYPE_SPECIFIER_LIST].equals(typeSpecifier.getType()))) {
                    throw new ParseException(EVENT_CONTEXT + EXPECTS_A_TYPE_SPECIFIER);
                }

                IDeclaration eventContextDecl = TypeSpecifierListParser.INSTANCE.parse(typeSpecifier, new TypeSpecifierListParser.Param(fTrace, null, null, scope));

                verifyEventContext(eventContextDecl);

                stream.setEventContext((StructDeclaration) eventContextDecl);
            } else if (left.equals(MetadataStrings.PACKET_CONTEXT)) {
                if (stream.isPacketContextSet()) {
                    throw new ParseException(PACKET_CONTEXT + ALREADY_DEFINED);
                }

                ICTFMetadataNode typeSpecifier = rightNode.getChild(0);

                if (!(CTFParser.tokenNames[CTFParser.TYPE_SPECIFIER_LIST].equals(typeSpecifier.getType()))) {
                    throw new ParseException(PACKET_CONTEXT + EXPECTS_A_TYPE_SPECIFIER);
                }

                IDeclaration packetContextDecl = TypeSpecifierListParser.INSTANCE.parse(typeSpecifier, new TypeSpecifierListParser.Param(fTrace, null, null, scope));

                verifyPacketContext(packetContextDecl);

                stream.setPacketContext((StructDeclaration) packetContextDecl);
            } else {
                CtfCoreLoggerUtil.logWarning("Unknown stream attribute: " + left); //$NON-NLS-1$
            }
        }
        return stream;
    }

    private static void verifyPacketContext(IDeclaration packetContextDecl) throws ParseException {
        if (!(packetContextDecl instanceof StructDeclaration)) {
            throw new ParseException(PACKET_CONTEXT + EXPECTS_A_STRUCT);
        }
    }

    private static void verifyEventContext(IDeclaration eventContextDecl) throws ParseException {
        if (!(eventContextDecl instanceof StructDeclaration)) {
            throw new ParseException(EVENT_CONTEXT + EXPECTS_A_STRUCT);
        }
    }

    private static void verifyEventHeaderScope(DeclarationScope eventHeaderScope) throws ParseException {
        if (eventHeaderScope == null) {
            throw new ParseException(EVENT_HEADER + SCOPE_NOT_FOUND);
        }
    }

    private static void setEventHeader(CTFStream stream, IDeclaration eventHeaderDecl) throws ParseException {
        if (eventHeaderDecl instanceof StructDeclaration) {
            stream.setEventHeader((StructDeclaration) eventHeaderDecl);
        } else if (eventHeaderDecl instanceof IEventHeaderDeclaration) {
            stream.setEventHeader((IEventHeaderDeclaration) eventHeaderDecl);
        } else {
            throw new ParseException(EVENT_HEADER + EXPECTS_A_STRUCT);
        }
    }

    private static DeclarationScope lookupStructName(ICTFMetadataNode typeSpecifier, DeclarationScope scope) {
        /*
         * This needs a struct.struct_name.name to work, luckily, that is 99.99%
         * of traces we receive.
         */
        final ICTFMetadataNode potentialStruct = typeSpecifier.getChild(0);
        DeclarationScope eventHeaderScope = null;
        if (CTFParser.tokenNames[CTFParser.STRUCT].equals(potentialStruct.getType())) {
            final ICTFMetadataNode potentialStructName = potentialStruct.getChild(0);
            if (CTFParser.tokenNames[CTFParser.STRUCT_NAME].equals(potentialStructName.getType())) {
                final String name = potentialStructName.getChild(0).getText();
                eventHeaderScope = scope.lookupChildRecursive(name);
                if (eventHeaderScope == null) {
                    eventHeaderScope = lookupScopeRecursiveStruct(name, scope);
                }
            }
        }
        /*
         * If that fails, maybe the struct is anonymous
         */
        if (eventHeaderScope == null) {
            eventHeaderScope = scope.lookupChildRecursive(MetadataStrings.STRUCT);
        }

        /*
         * This can still be null
         */
        return eventHeaderScope;
    }

    private static DeclarationScope lookupScopeRecursiveStruct(String name, DeclarationScope scope) {
        if (scope == null) {
            return null;
        }
        if (scope.lookupStruct(name) != null) {
            return scope;
        }
        return lookupScopeRecursiveStruct(name, scope.getParentScope());
    }

}
