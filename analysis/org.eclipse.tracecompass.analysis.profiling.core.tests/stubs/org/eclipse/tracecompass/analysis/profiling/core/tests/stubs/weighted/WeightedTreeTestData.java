/*******************************************************************************
 * Copyright (c) 2019, 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.core.tests.stubs.weighted;

import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTreeSet;

/**
 * A class that encapsulates stub data for multiple tests
 *
 * @author Geneviève Bastien
 */
public class WeightedTreeTestData {

    /** Level 1, first object */
    public static final SimpleTree OBJ_L11 = new SimpleTree("l11");
    /** Level 1, second object */
    public static final SimpleTree OBJ_L12 = new SimpleTree("l12");
    /** Level 2, first object */
    public static final SimpleTree OBJ_L21 = new SimpleTree("l21");
    /** Level 2, second object */
    public static final SimpleTree OBJ_L22 = new SimpleTree("l22");
    /** Level 2, third object */
    public static final SimpleTree OBJ_L23 = new SimpleTree("l23");
    /** Level 2, fourth object */
    public static final SimpleTree OBJ_L24 = new SimpleTree("l24");

    private static final String TREE_OBJ1 = "op1";
    private static final String TREE_OBJ2 = "op2";
    private static final String TREE_OBJ3 = "op3";
    private static final String TREE_OBJ4 = "op4";
    private static final String TREE_OBJ5 = "op5";

    static {
        OBJ_L11.addChild(OBJ_L21);
        OBJ_L11.addChild(OBJ_L22);
        OBJ_L12.addChild(OBJ_L23);
        OBJ_L12.addChild(OBJ_L24);
    }

    /**
     * This data structure was done to generalize further the original concept
     * of callgraph. The data to test is a simplification of the original
     * callgraph data, as follows:
     *
     * For the elements, there are 2 root elements with each 2 children. For
     * weighted trees for each element, the syntax is [objec]>-[weight]
     *
     * <pre>
     * l11 ___ l21     op1-9  op4-8
     *     |              |
     *     |            op2-5
     *     |              |
     *     |            op3-1
     *     |__ l22      op2-17
     *                  /    \
     *                op3-1  op2-6
     *
     * l21 ___ l23         op1-19
     *     |            /    |    \
     *     |         op3-5 op2-3  op4-8
     *     |            |    |
     *     |         op1-2 op3-1
     *     |__ l23         op5-15
     *                       |
     *                     op2-12
     *                       |
     *                     op3-1
     * </pre>
     *
     * @return The stub tree set
     */
    public static WeightedTreeSet<String, SimpleTree> getStubData() {
        WeightedTreeSet<String, SimpleTree> stub = new WeightedTreeSet<>();

        // Prepare weighted trees for l21
        WeightedTree<String> wt = new WeightedTree<>(TREE_OBJ1, 9);
        WeightedTree<String> child = new WeightedTree<>(TREE_OBJ2, 5);
        child.addChild(new WeightedTree<>(TREE_OBJ3, 1));
        wt.addChild(child);
        stub.addWeightedTree(OBJ_L21, wt);

        stub.addWeightedTree(OBJ_L21, new WeightedTree<>(TREE_OBJ4, 8));

        // Prepare weighted trees for l22
        wt = new WeightedTree<>(TREE_OBJ2, 17);
        wt.addChild(new WeightedTree<>(TREE_OBJ3, 1));
        wt.addChild(new WeightedTree<>(TREE_OBJ2, 6));
        stub.addWeightedTree(OBJ_L22, wt);

        // Prepare weighted trees for l23
        wt = new WeightedTree<>(TREE_OBJ1, 19);
        child = new WeightedTree<>(TREE_OBJ3, 5);
        child.addChild(new WeightedTree<>(TREE_OBJ1, 2));
        wt.addChild(child);
        child = new WeightedTree<>(TREE_OBJ2, 3);
        child.addChild(new WeightedTree<>(TREE_OBJ3, 1));
        wt.addChild(child);
        wt.addChild(new WeightedTree<>(TREE_OBJ4, 8));
        stub.addWeightedTree(OBJ_L23, wt);

        // Prepare weighted trees for l24
        wt = new WeightedTree<>(TREE_OBJ5, 15);
        child = new WeightedTree<>(TREE_OBJ2, 12);
        child.addChild(new WeightedTree<>(TREE_OBJ3, 1));
        wt.addChild(child);
        stub.addWeightedTree(OBJ_L24, wt);

        return stub;
    }

}
