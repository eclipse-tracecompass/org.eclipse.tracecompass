/*******************************************************************************
 * Copyright (c) 2011, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.ctf.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.tracecompass.internal.ctf.core.CtfCoreLoggerUtil;
import org.junit.Test;

/**
 * <b><u>CtfCorePluginTest</u></b>
 * <p>
 * Test the CTF core logger utility
 */
@SuppressWarnings("javadoc")
public class CtfCoreLoggerUtilTest {

    // ------------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------------

    @Test
    public void testCtfCorePluginId() {
        assertEquals(
                "Plugin ID", "org.eclipse.tracecompass.ctf.core", CtfCoreLoggerUtil.PLUGIN_ID);
    }

    @Test
    public void testLog() {
        try {
            CtfCoreLoggerUtil.logInfo("Some message");
        } catch (Exception e) {
            fail();
        }
    }

}
