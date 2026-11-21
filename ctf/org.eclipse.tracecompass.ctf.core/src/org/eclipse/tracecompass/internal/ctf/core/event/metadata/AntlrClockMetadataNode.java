/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.CtfCoreLoggerUtil;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ICTFMetadataNode;

/**
 * Adapter that wraps a CTF1 (TSDL/ANTLR) clock node and implements
 * {@link IClockMetadataNode}. This allows the ClockParser to work uniformly
 * with both ANTLR and JSON metadata nodes without instanceof checks.
 *
 * The ANTLR clock node contains children that are key-value pairs. This adapter
 * parses them into typed fields upon construction.
 *
 * @author Eclipse Trace Compass
 */
public class AntlrClockMetadataNode implements IClockMetadataNode {

    private final ICTFMetadataNode fDelegate;

    private @Nullable String fClockName;
    private long fFrequency = 1000000000L;
    private long fPrecision;
    private @Nullable String fDescription;
    private long fOffsetSeconds;
    private long fOffsetCycles;
    private boolean fAbsolute;
    private @Nullable String fUuid;

    private static final String NAME = "name"; //$NON-NLS-1$
    private static final String FREQ = "freq"; //$NON-NLS-1$
    private static final String PRECISION = "precision"; //$NON-NLS-1$
    private static final String DESCRIPTION = "description"; //$NON-NLS-1$
    private static final String OFFSET_S = "offset_s"; //$NON-NLS-1$
    private static final String OFFSET = "offset"; //$NON-NLS-1$
    private static final String ABSOLUTE = "absolute"; //$NON-NLS-1$
    private static final String UUID_KEY = "uuid"; //$NON-NLS-1$

    /**
     * Constructor. Wraps the given ANTLR clock node and parses its children
     * into typed fields.
     *
     * @param delegate
     *            the original ANTLR clock metadata node to wrap
     */
    public AntlrClockMetadataNode(ICTFMetadataNode delegate) {
        fDelegate = delegate;
        parseChildren();
    }

    private void parseChildren() {
        List<ICTFMetadataNode> children = fDelegate.getChildren();
        if (children == null) {
            return;
        }
        for (ICTFMetadataNode child : children) {
            String key = child.getChild(0).getChild(0).getChild(0).getText();
            ICTFMetadataNode value = child.getChild(1).getChild(0).getChild(0);
            String childType = value.getType();
            String text = value.getText();

            if (CTFParser.tokenNames[CTFParser.INTEGER].equals(childType) || CTFParser.tokenNames[CTFParser.DECIMAL_LITERAL].equals(childType)) {
                Long numValue;
                try {
                    numValue = Long.parseLong(text);
                } catch (NumberFormatException e) {
                    CtfCoreLoggerUtil.logWarning("Number conversion issue with " + text + ". Assigning " + key + " = 0."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    numValue = Long.valueOf(0L);
                }
                assignNumericField(key, numValue);
            } else {
                assignStringField(key, text);
            }
        }
    }

    private void assignNumericField(String key, long value) {
        switch (key) {
        case FREQ:
            fFrequency = value;
            break;
        case PRECISION:
            fPrecision = value;
            break;
        case OFFSET_S:
            fOffsetSeconds = value;
            break;
        case OFFSET:
            fOffsetCycles = value;
            break;
        default:
            break;
        }
    }

    private void assignStringField(String key, String value) {
        switch (key) {
        case NAME:
            fClockName = value;
            break;
        case DESCRIPTION:
            fDescription = value;
            break;
        case ABSOLUTE:
            fAbsolute = "TRUE".equalsIgnoreCase(value) || "1".equals(value); //$NON-NLS-1$ //$NON-NLS-2$
            break;
        case UUID_KEY:
            fUuid = value;
            break;
        default:
            break;
        }
    }

    // --- IClockMetadataNode methods ---

    @Override
    public @Nullable String getClockName() {
        return fClockName;
    }

    @Override
    public long getClockFrequency() {
        return fFrequency;
    }

    @Override
    public long getClockPrecision() {
        return fPrecision;
    }

    @Override
    public @Nullable String getClockDescription() {
        return fDescription;
    }

    @Override
    public long getClockOffsetSeconds() {
        return fOffsetSeconds;
    }

    @Override
    public long getClockOffsetCycles() {
        return fOffsetCycles;
    }

    @Override
    public @Nullable String getClockOrigin() {
        return null;
    }

    @Override
    public boolean isClockAbsolute() {
        return fAbsolute;
    }

    @Override
    public @Nullable String getClockUUID() {
        return fUuid;
    }

    // --- ICTFMetadataNode delegation ---

    @Override
    public ICTFMetadataNode getParent() {
        return fDelegate.getParent();
    }

    @Override
    public ICTFMetadataNode getChild(String id) {
        return fDelegate.getChild(id);
    }

    @Override
    public ICTFMetadataNode getChild(int index) {
        return fDelegate.getChild(index);
    }

    @Override
    public String getText() {
        return fDelegate.getText();
    }

    @Override
    public String getType() {
        return fDelegate.getType();
    }

    @Override
    public void setParent(ICTFMetadataNode node) {
        fDelegate.setParent(node);
    }

    @Override
    public void addChild(ICTFMetadataNode node) {
        fDelegate.addChild(node);
    }

    @Override
    public List<ICTFMetadataNode> getChildren() {
        return fDelegate.getChildren();
    }

    @Override
    public int getChildCount() {
        return fDelegate.getChildCount();
    }

    @Override
    public ICTFMetadataNode getFirstChildWithType(String typeDeclaratorList) {
        return fDelegate.getFirstChildWithType(typeDeclaratorList);
    }
}
