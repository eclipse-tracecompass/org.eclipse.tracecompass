/*******************************************************************************
 * Copyright (c) 2013, 2022 Christian Pontesegger and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License_Identifier: EPL-2.0
 *
 * Contributors:
 *   Christian Pontesegger - initial API and implementation
 *   Mathieu Velten - Bug correction
 *   Bernd Hufmann - Copied from Eclipse EASE to Trace Compass and modified
 *******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.analysis.xml.core.fsm.model.values;

import org.eclipse.jdt.annotation.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * A script engine to execute JavaScript code on a Rhino interpreter.
 */
public class RhinoScriptEngine {

    private static final int DEFAULT_OPTIMIZATION_LEVEL = 9;

    /**
     * Get a Javascript context
     *
     * @return Javascript context
     */
    public static @Nullable Context getContext() {
        Context context = Context.getCurrentContext();
        if (context == null) {
            synchronized (ContextFactory.getGlobal()) {
                context = Context.enter();
            }
        }
        return context;
    }

    /** Rhino Scope. Created when interpreter is initialized */
    private @Nullable ScriptableObject fScope;

    private @Nullable Context fContext;

    /**
     * Creates a new Rhino interpreter.
     */
    public RhinoScriptEngine() {
        // Nothing to do
    }

    /**
     * Setup the Rhino engine
     */
    public synchronized void setupEngine() {
        Context context = getContext();
        if (context != null) {
            context.setGeneratingDebug(false);
            context.setOptimizationLevel(DEFAULT_OPTIMIZATION_LEVEL);
            context.setDebugger(null, null);

            fScope = new ImporterTopLevel(context);

            // enable script termination support
            context.setGenerateObserverCount(true);
            context.setInstructionObserverThreshold(10);

            // enable JS v1.8 language constructs
            try {
                Context.class.getDeclaredField("VERSION_ES6"); //$NON-NLS-1$
                context.setLanguageVersion(Context.VERSION_ES6);
            } catch (final Exception e) {
                try {
                    Context.class.getDeclaredField("VERSION_1_8"); //$NON-NLS-1$
                    context.setLanguageVersion(Context.VERSION_1_8);
                } catch (final Exception e1) {
                    context.setLanguageVersion(Context.VERSION_1_7);
                }
            }
            fContext = context;
        }
    }

    /**
     * Tear down the Rhino engine when not needed anymore
     */
    public synchronized void teardownEngine() {
        // cleanup context
        Context.exit();
        fContext = null;
        fScope = null;
    }

    /**
     * Execute a Javascript expression
     *
     * @param javascriptExpression
     *            the Javascript expression to run
     * @return result of execution or null if failed
     * @throws RhinoException
     *             if script execution fails
     */
    public @Nullable Object execute(final String javascriptExpression) throws RhinoException{
        Context context = fContext;
        ScriptableObject scope = fScope;
        Object result = null;
        if (context != null && scope != null) {
            result = context.evaluateString(scope, javascriptExpression, "Data Driven Source", 1, null); //$NON-NLS-1$
        }

        // evaluate result
        if (result == null || result instanceof Undefined) {
            return null;
        } else if (result instanceof NativeJavaObject) {
            return ((NativeJavaObject) result).unwrap();
        } else if ("org.mozilla.javascript.InterpretedFunction".equals(result.getClass().getName())) { //$NON-NLS-1$ //NOSONAR
            return null;
        }
        return result;
    }

    /**
     * Update ScriptableObject scope
     *
     * @param name
     *            the name of the property
     * @param value
     *            value to set the property to
     */
    public void put(String name, Object value) {
        ScriptableObject scope = fScope;
        if (scope != null) {
            scope.put(name, scope, value);
        }
    }
}
