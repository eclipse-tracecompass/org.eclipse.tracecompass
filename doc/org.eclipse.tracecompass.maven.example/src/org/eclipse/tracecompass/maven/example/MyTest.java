/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.maven.example;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.core.trace.CTFTraceReader;

public class MyTest {
    public static void main(String[] args) {
        try {
            // TODO Fix path
            CTFTrace trace = new CTFTrace("/home/user/git/tracecompass-test-traces/ctf/src/main/resources/trace2/");
            try (CTFTraceReader traceReader = new CTFTraceReader(trace);) {

                while (traceReader.hasMoreEvents()) {
                    IEventDefinition ed = traceReader.getCurrentEventDef();
                    /* Do something with the event */
                    System.out.println(ed.toString());
                    traceReader.advance();
                }
            }
        } catch (CTFException e) {
            e.printStackTrace();
        }
    }
}
