/*******************************************************************************
 * Copyright (c) 2011, 2023 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Matthew Khouzam - Initial Design and Grammar
 *     Francis Giraldeau - Initial API and implementation
 *     Simon Marchi - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.ctf.core.event.CTFCallsite;
import org.eclipse.tracecompass.ctf.core.event.CTFClock;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.EventDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.ClockParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeAliasParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeSpecifierListParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypedefParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.callsite.CallSiteParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.environment.EnvironmentParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.event.EventParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.stream.StreamParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.trace.TraceDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;
import org.eclipse.tracecompass.internal.ctf.core.trace.CTFStream;
import org.eclipse.tracecompass.internal.ctf.core.utils.JsonMetadataStrings;

import com.google.common.collect.Iterables;

/**
 * IOStructGen
 */
public class IOStructGen {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * The trace
     */
    private final @NonNull CTFTrace fTrace;
    private ICTFMetadataNode fTree;

    /**
     * The current declaration scope.
     */
    private final @NonNull DeclarationScope fRoot;

    /**
     * Data helpers needed for streaming
     */

    private boolean fHasBeenParsed = false;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param tree
     *            the tree (ANTLR generated) with the parsed TSDL data.
     * @param trace
     *            the trace containing the places to put all the read metadata
     */
    public IOStructGen(ICTFMetadataNode tree, @NonNull CTFTrace trace) {
        fTrace = trace;
        fTree = tree;
        fRoot = NonNullUtils.checkNotNull(trace.getScope());
    }

    /**
     * Parse the tree and populate the trace defined in the constructor.
     *
     * @throws ParseException
     *             If there was a problem parsing the metadata
     */
    public void generate() throws ParseException {
        parseRoot(fTree);
    }

    /**
     * Parse a partial tree and populate the trace defined in the constructor.
     * Does not check for a "trace" block as there is only one in the trace and
     * thus
     *
     * @throws ParseException
     *             If there was a problem parsing the metadata
     */
    public void generateFragment() throws ParseException {
        parseIncompleteRoot(fTree);
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Sets a new tree to parse
     *
     * @param newTree
     *            the new tree to parse
     */
    public void setTree(ICTFMetadataNode newTree) {
        fTree = newTree;
    }

    /**
     * Parse the root node.
     *
     * @param root
     *            A ROOT node.
     * @throws ParseException
     */
    private void parseRoot(ICTFMetadataNode root) throws ParseException {

        List<ICTFMetadataNode> children = root.getChildren();

        ICTFMetadataNode traceNode = null;
        boolean hasStreams = false;
        List<ICTFMetadataNode> events = new ArrayList<>();
        Collection<CTFCallsite> callsites = new ArrayList<>();
        for (ICTFMetadataNode child : children) {
            final String type = child.getType();
            if (CTFParser.tokenNames[CTFParser.DECLARATION].equals(type)) {
                parseRootDeclaration(child);
            } else if (CTFParser.tokenNames[CTFParser.TRACE].equals(type) || JsonMetadataStrings.FRAGMENT_TRACE.equals(type)) {
                if (traceNode != null) {
                    throw new ParseException("Only one trace block is allowed"); //$NON-NLS-1$
                }
                traceNode = child;
                parseTrace(traceNode);
            } else if (CTFParser.tokenNames[CTFParser.STREAM].equals(type)) {
                StreamParser.INSTANCE.parse(child, new StreamParser.Param(fTrace, fRoot));
                hasStreams = true;
            } else if (CTFParser.tokenNames[CTFParser.EVENT].equals(type)) {
                events.add(child);
            } else if (CTFParser.tokenNames[CTFParser.CLOCK].equals(type)) {
                CTFClock ctfClock = ClockParser.INSTANCE.parse(child, null);
                String nameValue = ctfClock.getName();
                fTrace.addClock(nameValue, ctfClock);
            } else if (CTFParser.tokenNames[CTFParser.ENV].equals(type)) {
                fTrace.setEnvironment(EnvironmentParser.INSTANCE.parse(child, null));
            } else if (CTFParser.tokenNames[CTFParser.CALLSITE].equals(type)) {
                callsites.add(CallSiteParser.INSTANCE.parse(child, null));
            } else if (JsonMetadataStrings.FRAGMENT_PREAMBLE.equals(type)) {
                // Do nothing for now
            } else {
                throw childTypeError(child);
            }
        }
        if (traceNode == null) {
            throw new ParseException("Missing trace block"); //$NON-NLS-1$
        }
        parseEvents(events, callsites, hasStreams);
        fHasBeenParsed = true;
    }

    private void parseEvents(List<ICTFMetadataNode> events, Collection<CTFCallsite> staticCallsites, boolean hasStreams) throws ParseException {
        if (!hasStreams && !events.isEmpty()) {
            /* Add an empty stream that will have a null id */
            fTrace.addStream(new CTFStream(fTrace));
        }
        for (ICTFMetadataNode event : events) {
            EventDeclaration ev = EventParser.INSTANCE.parse(event, new EventParser.Param(fTrace, fRoot));
            List<CTFCallsite> callsites = staticCallsites.stream().filter(cs -> ev.getName().equals(cs.getEventName())).collect(Collectors.toList());
            ev.addCallsites(callsites);

        }
    }

    private void parseIncompleteRoot(ICTFMetadataNode root) throws ParseException {
        if (!fHasBeenParsed) {
            throw new ParseException("You need to run generate first"); //$NON-NLS-1$
        }
        List<ICTFMetadataNode> children = root.getChildren();
        List<ICTFMetadataNode> events = new ArrayList<>();
        Collection<CTFCallsite> callsites = new ArrayList<>();
        for (ICTFMetadataNode child : children) {
            final String type = child.getType();
            if (CTFParser.tokenNames[CTFParser.DECLARATION].equals(type)) {
                parseRootDeclaration(child);
                break;
            } else if (CTFParser.tokenNames[CTFParser.TRACE].equals(type)) {
                throw new ParseException("Trace block defined here, please use generate and not generateFragment to parse this fragment"); //$NON-NLS-1$
            } else if (CTFParser.tokenNames[CTFParser.STREAM].equals(type)) {
                StreamParser.INSTANCE.parse(child, new StreamParser.Param(fTrace, fRoot));
                break;
            } else if (CTFParser.tokenNames[CTFParser.EVENT].equals(type)) {
                events.add(child);
                break;
            } else if (CTFParser.tokenNames[CTFParser.CLOCK].equals(type)) {
                CTFClock ctfClock = ClockParser.INSTANCE.parse(child, null);
                String nameValue = ctfClock.getName();
                fTrace.addClock(nameValue, ctfClock);
                break;
            } else if (CTFParser.tokenNames[CTFParser.ENV].equals(type)) {
                fTrace.setEnvironment(EnvironmentParser.INSTANCE.parse(child, null));
                break;
            } else if (CTFParser.tokenNames[CTFParser.CALLSITE].equals(type)) {
                callsites.add(CallSiteParser.INSTANCE.parse(child, null));
                break;
            } else {
                throw childTypeError(child);
            }
        }
        parseEvents(events, callsites, !Iterables.isEmpty(fTrace.getStreams()));
    }

    private void parseTrace(ICTFMetadataNode traceNode) throws ParseException {

        CTFTrace trace = fTrace;
        List<ICTFMetadataNode> children = traceNode.getChildren();
        if (children == null) {
            throw new ParseException("Trace block is empty"); //$NON-NLS-1$
        }

        for (ICTFMetadataNode child : children) {
            String type = child.getType();
            if (CTFParser.tokenNames[CTFParser.TYPEALIAS].equals(type)) {
                TypeAliasParser.INSTANCE.parse(child, new TypeAliasParser.Param(trace, fRoot));
            } else if (child instanceof JsonStructureFieldMetadataNode) {
                IDeclaration packetHeaderDecl = TypeSpecifierListParser.INSTANCE.parse(child, new TypeSpecifierListParser.Param(fTrace, null, null, fRoot));
                trace.setPacketHeader((StructDeclaration) packetHeaderDecl);
            } else if (CTFParser.tokenNames[CTFParser.TYPEDEF].equals(type)) {
                TypedefParser.INSTANCE.parse(child, new TypedefParser.Param(trace, fRoot));
            } else if (CTFParser.tokenNames[CTFParser.CTF_EXPRESSION_TYPE].equals(type) || CTFParser.tokenNames[CTFParser.CTF_EXPRESSION_VAL].equals(type)) {
                TraceDeclarationParser.INSTANCE.parse(child, new TraceDeclarationParser.Param(fTrace, fRoot));
            } else {
                throw childTypeError(child);
            }
        }

        /*
         * If trace byte order was not specified and not using packet based
         * metadata
         */
        if (fTrace.getByteOrder() == null) {
            throw new ParseException("Trace byte order not set"); //$NON-NLS-1$
        }
    }

    /**
     * Parses a declaration at the root level.
     *
     * @param declaration
     *            The declaration subtree.
     * @throws ParseException
     */
    private void parseRootDeclaration(ICTFMetadataNode declaration)
            throws ParseException {

        List<ICTFMetadataNode> children = declaration.getChildren();

        for (ICTFMetadataNode child : children) {
            String type = child.getType();
            if (CTFParser.tokenNames[CTFParser.TYPEDEF].equals(type)) {
                TypedefParser.INSTANCE.parse(child, new TypedefParser.Param(fTrace, fRoot));
            } else if (CTFParser.tokenNames[CTFParser.TYPEALIAS].equals(type)) {
                TypeAliasParser.INSTANCE.parse(child, new TypeAliasParser.Param(fTrace, fRoot));
            } else if (CTFParser.tokenNames[CTFParser.TYPE_SPECIFIER_LIST].equals(type)) {
                TypeSpecifierListParser.INSTANCE.parse(child, new TypeSpecifierListParser.Param(fTrace, null, null, fRoot));
            } else {
                throw childTypeError(child);
            }
        }
    }

    /**
     * Throws a ParseException stating that the parent-child relation between
     * the given node and its parent is not valid. It means that the shape of
     * the AST is unexpected.
     *
     * @param child
     *            The invalid child node.
     * @return ParseException with details
     */
    private static ParseException childTypeError(ICTFMetadataNode child) {
        ICTFMetadataNode parent = child.getParent();
        String error = "Parent " + parent.getType() //$NON-NLS-1$
                + " can't have a child of type " //$NON-NLS-1$
                + child.getType() + "."; //$NON-NLS-1$

        return new ParseException(error);
    }

}
