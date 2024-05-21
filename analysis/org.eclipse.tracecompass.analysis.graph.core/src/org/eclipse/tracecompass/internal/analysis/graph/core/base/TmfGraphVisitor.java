/*******************************************************************************
 * Copyright (c) 2015, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Francis Giraldeau - Initial implementation and API
 *   Geneviève Bastien - Initial implementation and API
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.base;

/**
 * An empty implementation of the graph visitor
 *
 * @author Geneviève Bastien
 * @author Francis Giraldeau
 */
public class TmfGraphVisitor implements ITmfGraphVisitor {

    @Override
    public void visitHead(TmfVertex node) {
        // Do nothing
    }

    @Override
    public void visit(TmfVertex node) {
        // Do nothing
    }

    @Override
    public void visit(TmfEdge edge, boolean horizontal) {
        // Do nothing
    }

}
