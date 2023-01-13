/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.tmf.core.tests.model.xy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXyTreeDataModel;
import org.junit.Test;

/**
 * JUnit Test class to test {@link TmfXyTreeDataModel}
 */
public class TmfXyTreeDataModelTest {
    // ------------------------------------------------------------------------
    // Test data
    // ------------------------------------------------------------------------
    private static final String TO_STRING = "toString";
    private static final String HASH_CODE = "hashCode";
    private static final String EQUALS = "equals";

    private static final @NonNull List<@NonNull String> LABELS0 = Arrays.asList("label1, label2, label3");
    private static final long ID0 = 0L;
    private static final long PARENT_ID0 = -1L;
    private static final OutputElementStyle STYLE0 = null;

    private static final TmfXyTreeDataModel fModel0 = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0, true, STYLE0, false);
    private static final TmfXyTreeDataModel fModel1 = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0, true, STYLE0, true);

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------

    /**
     * Test constructors and getter/setters.
     */
    @Test
    public void testConstructors() {
        TmfXyTreeDataModel model = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0);
        assertEquals(ID0, model.getId());
        assertEquals(PARENT_ID0, model.getParentId());
        assertEquals(LABELS0.get(0), model.getName());
        assertEquals(LABELS0, model.getLabels());
        assertTrue(model.hasRowModel());
        assertEquals(STYLE0, model.getStyle());
        assertFalse(model.isDefault());

        model = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0, false, STYLE0);
        assertEquals(ID0, model.getId());
        assertEquals(PARENT_ID0, model.getParentId());
        assertEquals(LABELS0.get(0), model.getName());
        assertEquals(LABELS0, model.getLabels());
        assertFalse(model.hasRowModel());
        assertEquals(STYLE0, model.getStyle());
        assertFalse(model.isDefault());

        model = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0.get(0));
        assertEquals(ID0, model.getId());
        assertEquals(PARENT_ID0, model.getParentId());
        assertEquals(LABELS0.get(0), model.getName());
        assertEquals(LABELS0, model.getLabels());
        assertTrue(model.hasRowModel());
        assertEquals(STYLE0, model.getStyle());
        assertFalse(model.isDefault());

        assertEquals(ID0, fModel0.getId());
        assertEquals(PARENT_ID0, fModel0.getParentId());
        assertEquals(LABELS0.get(0), fModel0.getName());
        assertEquals(LABELS0, fModel0.getLabels());
        assertTrue(fModel0.hasRowModel());
        assertEquals(STYLE0, fModel0.getStyle());
        assertFalse(fModel0.isDefault());

        assertEquals(ID0, fModel1.getId());
        assertEquals(PARENT_ID0, fModel1.getParentId());
        assertEquals(LABELS0.get(0), fModel1.getName());
        assertEquals(LABELS0, fModel1.getLabels());
        assertTrue(fModel1.hasRowModel());
        assertEquals(STYLE0, fModel1.getStyle());
        assertTrue(fModel1.isDefault());
    }

    // ------------------------------------------------------------------------
    // TmfXyTreeDataModel#equals()
    // ------------------------------------------------------------------------

    /**
     * Run the {@link TmfXyTreeDataModel#equals} method test.
     */
    @Test
    public void testEqualsReflexivity() {
        assertTrue(EQUALS, fModel0.equals(fModel0));
        assertTrue(EQUALS, fModel1.equals(fModel1));

        assertTrue(EQUALS, !fModel0.equals(fModel1));
        assertTrue(EQUALS, !fModel1.equals(fModel0));
    }

    /**
     * Run the {@link TmfXyTreeDataModel#equals} method test.
     */
    @Test
    public void testEqualsSymmetry() {
        TmfXyTreeDataModel model0 = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0, true, STYLE0, false);
        TmfXyTreeDataModel model1 = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0, true, STYLE0, true);

        assertTrue(EQUALS, model0.equals(fModel0));
        assertTrue(EQUALS, fModel0.equals(model0));

        assertTrue(EQUALS, model1.equals(fModel1));
        assertTrue(EQUALS, fModel1.equals(model1));
    }

    /**
     * Run the {@link TmfXyTreeDataModel#equals} method test.
     */
    @Test
    public void testEqualsTransivity() {
        TmfXyTreeDataModel model0 = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0, true, STYLE0, true);
        TmfXyTreeDataModel model1 = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0, true, STYLE0, true);
        TmfXyTreeDataModel model2 = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0, true, STYLE0, true);

        assertTrue(EQUALS, model0.equals(model1));
        assertTrue(EQUALS, model1.equals(model2));
        assertTrue(EQUALS, model0.equals(model2));
    }

    /**
     * Run the {@link TmfXyTreeDataModel#equals} method test.
     */
    @Test
    public void testEqualsNull() {
        TmfXyTreeDataModel model0 = null;
        assertFalse(EQUALS, fModel0.equals(model0));
        assertFalse(EQUALS, fModel1.equals(model0));
    }

    // ------------------------------------------------------------------------
    // TmfXyTreeDataModel#hashCode
    // ------------------------------------------------------------------------

    /**
     * Run the {@link TmfXyTreeDataModel#hashCode} method test.
     */
    @Test
    public void testHashCode() {
        TmfXyTreeDataModel model0 = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0, true, STYLE0, false);
        TmfXyTreeDataModel model1 = new TmfXyTreeDataModel(ID0, PARENT_ID0, LABELS0, true, STYLE0, true);

        assertTrue(HASH_CODE, fModel0.hashCode() == model0.hashCode());
        assertTrue(HASH_CODE, fModel1.hashCode() == model1.hashCode());

        assertTrue(HASH_CODE, fModel0.hashCode() != model1.hashCode());
        assertTrue(HASH_CODE, fModel1.hashCode() != model0.hashCode());
    }

    /**
     * Test {@link TmfXyTreeDataModel#toString()}
     */
    @Test
    public void testToString() {
        assertEquals(TO_STRING, "<<name=[label1, label2, label3] id=0 parentId=-1 style=null hasRowModel=true> isDefault=false>", fModel0.toString());
        assertEquals(TO_STRING, "<<name=[label1, label2, label3] id=0 parentId=-1 style=null hasRowModel=true> isDefault=true>", fModel1.toString());
    }
}
