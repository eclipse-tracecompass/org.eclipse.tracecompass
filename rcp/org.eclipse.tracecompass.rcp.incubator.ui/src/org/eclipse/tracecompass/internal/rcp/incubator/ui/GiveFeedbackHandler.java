/*******************************************************************************
 * Copyright (c) 2017, 2024 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.rcp.incubator.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.program.Program;

/**
 * @author Geneviève Bastien
 */
public class GiveFeedbackHandler extends AbstractHandler {

    private static final String URL = "https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/issues"; //$NON-NLS-1$

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Program.launch(URL);
        return null;
    }

}
