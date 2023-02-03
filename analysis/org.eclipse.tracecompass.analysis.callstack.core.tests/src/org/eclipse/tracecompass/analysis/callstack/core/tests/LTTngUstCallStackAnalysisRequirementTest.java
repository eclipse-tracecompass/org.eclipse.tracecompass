/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.callstack.core.tests;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.analysis.callstack.core.LttngUstCallStackAnalysisRequirement;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.lttng2.ust.core.trace.layout.ILttngUstEventLayout;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventType;
import org.junit.AfterClass;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Test the {@link LttngUstCallStackAnalysisRequirement} class
 *
 * @author Bernd Hufmann
 */
public class LTTngUstCallStackAnalysisRequirementTest {

    private static final @NonNull String FUNC_EXIT_FAST = "lttng_ust_cyg_profile_fast:func_exit";
    private static final @NonNull String FUNC_EXIT = "lttng_ust_cyg_profile:func_exit";
    private static final @NonNull String FUNC_ENTRY_FAST = "lttng_ust_cyg_profile_fast:func_entry";
    private static final @NonNull String FUNC_ENTRY = "lttng_ust_cyg_profile:func_entry";
    private static final @NonNull String OTHER_EVENT = "OTHER";

    enum EventType {

        EVT_EXIT_FAST(FUNC_EXIT_FAST),
        EVT_EXIT(FUNC_EXIT),
        EVT_ENTRY_FAST(FUNC_ENTRY_FAST),
        EVT_ENTRY(FUNC_ENTRY),
        EVT_OTHER(OTHER_EVENT);

        private final @NonNull CtfTmfEventType fType;

        EventType(@NonNull String name) {
            fType = new CtfTmfEventType(name, null) {
                @Override
                public String getName() {
                    return name;
                }
            };
        }

        @NonNull CtfTmfEventType getEventType() {
            return fType;
        }
    }

    enum TestData {

        TRACE_WITH_VALID_EVENTS(EventType.EVT_ENTRY, EventType.EVT_EXIT, true),
        TRACE_WITH_VALID_EVENTS_FAST(EventType.EVT_ENTRY_FAST, EventType.EVT_EXIT_FAST, true),
        TRACE_WITH_MISSING_EVENTS(EventType.EVT_OTHER, EventType.EVT_EXIT_FAST, false),
        TRACE_MISMATCH_EVENTS(EventType.EVT_ENTRY_FAST, EventType.EVT_EXIT, false);

        private final @NonNull LttngUstTrace fTrace;
        private final boolean fIsValid;

        TestData(EventType first, EventType second, boolean isValid) {
            fTrace = new LttngUstTrace() {
                @Override
                public Set<CtfTmfEventType> getContainedEventTypes() {
                    return ImmutableSet.of(first.getEventType(), second.getEventType());
                }
            };
            fIsValid = isValid;
        }

        @NonNull LttngUstTrace getTrace() {
            return fTrace;
        }

        boolean isValid() {
            return fIsValid;
        }
    }

    /**
     * Clean up
     */
    @AfterClass
    public static void cleanup() {
        for (TestData testData : TestData.values()) {
            testData.getTrace().dispose();
        }
    }

    /**
     * Test Call Stack Analysis requirements
     */
    @Test
    public void testCallStackRequirements() {
        LttngUstCallStackAnalysisRequirement req = new LttngUstCallStackAnalysisRequirement(ILttngUstEventLayout.DEFAULT_LAYOUT);
        for (TestData item : TestData.values()) {
            assertEquals(item.name(), item.isValid(), req.test(item.getTrace()));
        }
    }
}
