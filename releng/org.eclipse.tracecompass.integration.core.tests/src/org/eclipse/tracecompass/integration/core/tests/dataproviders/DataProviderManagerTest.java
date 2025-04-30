/******************************************************************************
 * Copyright (c) 2021, 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.integration.core.tests.dataproviders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadStatusDataProvider;
import org.eclipse.tracecompass.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.tracecompass.lttng2.lttng.kernel.core.tests.shared.LttngKernelTestTraceUtils;
import org.eclipse.tracecompass.lttng2.ust.core.tests.shared.LttngUstTestTraceUtils;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for testing the data provider manager.
 */
public class DataProviderManagerTest {

    private static LttngKernelTrace fKernelTrace;
    private static LttngUstTrace fUstTrace;
    private static TmfExperiment fExperiment;
    private static final Set<IDataProviderDescriptor> EXPECTED_KERNEL_DP_DESCRIPTORS = new HashSet<>();
    private static final Set<IDataProviderDescriptor> EXPECTED_UST_DP_DESCRIPTORS = new HashSet<>();
    private static final String SEGMENTSTORE_SCATTER_FUTEX_DP_ID = "org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.scatter.dataprovider:lttng.analysis.futex";

    private static final String PATH = "/tmp/my-test.xml";
    private static final String ID = "my-test.xml";
    private static final String DESC = "descriptor";
    private static final String SOURCE_ID = "my-source-id";

    private static ITmfConfiguration sfCconfig;
    private static ITmfConfiguration sfCconfig2;

    private static final String CPU_USAGE_DP_ID = "org.eclipse.tracecompass.analysis.os.linux.core.cpuusage.CpuUsageDataProvider";

    static {
        // Kernel Trace
        DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
        builder.setName("CPU Usage")
                .setDescription("Show the CPU usage of a Linux kernel trace, returns the CPU usage per process and can be filtered by CPU core")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId(CPU_USAGE_DP_ID);
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Disk I/O View")
                .setDescription("Show the input and output throughput for each drive on a machine")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.analysis.os.linux.core.inputoutput.DisksIODataProvider");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Events Table")
                .setDescription("Show the raw events in table form for a given trace")
                .setProviderType(ProviderType.TABLE)
                .setId("org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableDataProvider");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Futex Contention Analysis - Latency Statistics")
                .setDescription("Show latency statistics provided by Analysis module: Futex Contention Analysis")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider:lttng.analysis.futex");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Futex Contention Analysis - Latency Table")
                .setDescription("Show latency table provided by Analysis module: Futex Contention Analysis")
                .setProviderType(ProviderType.TABLE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreTableDataProvider:lttng.analysis.futex");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Futex Contention Analysis - Function Density")
                .setDescription("Show function density provided by Analysis module: Futex Contention Analysis")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreDensityDataProvider:lttng.analysis.futex");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Futex Contention Analysis - Latency vs Time")
                .setDescription("Show latencies provided by Analysis module: Futex Contention Analysis")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId(SEGMENTSTORE_SCATTER_FUTEX_DP_ID);
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Futex Contention Analysis - Priority/Thread name Statistics Table")
                .setDescription("Show Priority/Thread name Statistics Table provided by Analysis module: Futex Contention Analysis")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityThreadNameStatisticsDataProvider:lttng.analysis.futex");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Futex Contention Analysis - Priority Statistics Table")
                .setDescription("Show Priority Statistics Table provided by Analysis module: Futex Contention Analysis")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityStatisticsDataProvider:lttng.analysis.futex");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Histogram")
                .setDescription("Show a histogram of number of events to time for a trace")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.tmf.core.histogram.HistogramDataProvider");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("IRQ Analysis - Latency Statistics")
                .setDescription("Show latency statistics provided by Analysis module: IRQ Analysis")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider:lttng.analysis.irq");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("IRQ Analysis - Latency Table")
                .setDescription("Show latency table provided by Analysis module: IRQ Analysis")
                .setProviderType(ProviderType.TABLE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreTableDataProvider:lttng.analysis.irq");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("IRQ Analysis - Function Density")
                .setDescription("Show function density provided by Analysis module: IRQ Analysis")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreDensityDataProvider:lttng.analysis.irq");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("IRQ Analysis - Latency vs Time")
                .setDescription("Show latencies provided by Analysis module: IRQ Analysis")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.scatter.dataprovider:lttng.analysis.irq");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("IRQ Analysis - Priority/Thread name Statistics Table")
                .setDescription("Show Priority/Thread name Statistics Table provided by Analysis module: IRQ Analysis")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityThreadNameStatisticsDataProvider:lttng.analysis.irq");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("IRQ Analysis - Priority Statistics Table")
                .setDescription("Show Priority Statistics Table provided by Analysis module: IRQ Analysis")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityStatisticsDataProvider:lttng.analysis.irq");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Memory Usage")
                .setDescription("Show the relative memory usage in the Linux kernel by process, can be filtered to show only the processes which were active on a time range")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.analysis.os.linux.core.kernelmemoryusage");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Resources Status")
                .setDescription("Show the state of CPUs (SYSCALL, RUNNING, IRQ, SOFT_IRQ or IDLE) and its IRQs/SOFT_IRQs as well as its frequency.")
                .setProviderType(ProviderType.TIME_GRAPH)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ResourcesStatusDataProvider");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("System Call Latency - Latency Statistics")
                .setDescription("Show latency statistics provided by Analysis module: System Call Latency")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider:org.eclipse.tracecompass.analysis.os.linux.latency.syscall");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("System Call Latency - Latency Table")
                .setDescription("Show latency table provided by Analysis module: System Call Latency")
                .setProviderType(ProviderType.TABLE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreTableDataProvider:org.eclipse.tracecompass.analysis.os.linux.latency.syscall");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("System Call Latency - Function Density")
                .setDescription("Show function density provided by Analysis module: System Call Latency")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreDensityDataProvider:org.eclipse.tracecompass.analysis.os.linux.latency.syscall");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("System Call Latency - Latency vs Time")
                .setDescription("Show latencies provided by Analysis module: System Call Latency")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.scatter.dataprovider:org.eclipse.tracecompass.analysis.os.linux.latency.syscall");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("System Call Latency - Priority/Thread name Statistics Table")
                .setDescription("Show Priority/Thread name Statistics Table provided by Analysis module: System Call Latency")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityThreadNameStatisticsDataProvider:org.eclipse.tracecompass.analysis.os.linux.latency.syscall");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("System Call Latency - Priority Statistics Table")
                .setDescription("Show Priority Statistics Table provided by Analysis module: System Call Latency")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityStatisticsDataProvider:org.eclipse.tracecompass.analysis.os.linux.latency.syscall");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Thread Status")
                .setDescription("Show the hierarchy of Linux threads and their status (RUNNING, SYSCALL, IRQ, IDLE)")
                .setProviderType(ProviderType.TIME_GRAPH)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadStatusDataProvider");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Scheduler Wakeup to Scheduler Switch Latency - Latency Statistics")
                .setDescription("Show latency statistics provided by Analysis module: Scheduler Wakeup to Scheduler Switch Latency")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider:org.eclipse.tracecompass.analysis.os.linux.core.swslatency.sws");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Scheduler Wakeup to Scheduler Switch Latency - Latency Table")
                .setDescription("Show latency table provided by Analysis module: Scheduler Wakeup to Scheduler Switch Latency")
                .setProviderType(ProviderType.TABLE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreTableDataProvider:org.eclipse.tracecompass.analysis.os.linux.core.swslatency.sws");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Scheduler Wakeup to Scheduler Switch Latency - Latency vs Time")
                .setDescription("Show latencies provided by Analysis module: Scheduler Wakeup to Scheduler Switch Latency")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.scatter.dataprovider:org.eclipse.tracecompass.analysis.os.linux.core.swslatency.sws");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Scheduler Wakeup to Scheduler Switch Latency - Priority/Thread name Statistics Table")
                .setDescription("Show Priority/Thread name Statistics Table provided by Analysis module: Scheduler Wakeup to Scheduler Switch Latency")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityThreadNameStatisticsDataProvider:org.eclipse.tracecompass.analysis.os.linux.core.swslatency.sws");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Scheduler Wakeup to Scheduler Switch Latency - Priority Statistics Table")
                .setDescription("Show Priority Statistics Table provided by Analysis module: Scheduler Wakeup to Scheduler Switch Latency")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityStatisticsDataProvider:org.eclipse.tracecompass.analysis.os.linux.core.swslatency.sws");
        EXPECTED_KERNEL_DP_DESCRIPTORS.add(builder.build());

        // UST Trace
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Events Table")
                .setDescription("Show the raw events in table form for a given trace")
                .setProviderType(ProviderType.TABLE)
                .setId("org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableDataProvider");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Flame Chart")
                .setDescription("Show a call stack over time")
                .setProviderType(ProviderType.TIME_GRAPH)
                .setId("org.eclipse.tracecompass.internal.analysis.profiling.callstack.provider.CallStackDataProvider");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Function Duration Statistics")
                .setDescription("Show the function duration statistics")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph.callgraphanalysis.statistics");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("Histogram")
                .setDescription("Show a histogram of number of events to time for a trace")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.tmf.core.histogram.HistogramDataProvider");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("LTTng-UST CallStack - Latency Statistics")
                .setDescription("Show latency statistics provided by Analysis module: LTTng-UST CallStack")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider:org.eclipse.linuxtools.lttng2.ust.analysis.callstack");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("LTTng-UST CallStack - Latency Table")
                .setDescription("Show latency table provided by Analysis module: LTTng-UST CallStack")
                .setProviderType(ProviderType.TABLE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreTableDataProvider:org.eclipse.linuxtools.lttng2.ust.analysis.callstack");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("LTTng-UST CallStack - Function Density")
                .setDescription("Show function density provided by Analysis module: LTTng-UST CallStack")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreDensityDataProvider:org.eclipse.linuxtools.lttng2.ust.analysis.callstack");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("LTTng-UST CallStack - Latency vs Time")
                .setDescription("Show latencies provided by Analysis module: LTTng-UST CallStack")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.scatter.dataprovider:org.eclipse.linuxtools.lttng2.ust.analysis.callstack");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("LTTng-UST CallStack - Priority/Thread name Statistics Table")
                .setDescription("Show Priority/Thread name Statistics Table provided by Analysis module: LTTng-UST CallStack")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityThreadNameStatisticsDataProvider:org.eclipse.linuxtools.lttng2.ust.analysis.callstack");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("LTTng-UST CallStack - Priority Statistics Table")
                .setDescription("Show Priority Statistics Table provided by Analysis module: LTTng-UST CallStack")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.internal.analysis.os.linux.core.segmentstore.PriorityStatisticsDataProvider:org.eclipse.linuxtools.lttng2.ust.analysis.callstack");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder = new DataProviderDescriptor.Builder();
        builder.setName("LTTng-UST CallStack (new) - Flame Chart")
                .setDescription("Show Flame Chart provided by Analysis module: LTTng-UST CallStack (new)")
                .setProviderType(ProviderType.TIME_GRAPH)
                .setId("org.eclipse.tracecompass.analysis.profiling.core.flamechart:org.eclipse.tracecompass.lttng2.ust.core.analysis.callstack");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder.setName("LTTng-UST CallStack (new) - Latency Statistics")
                .setDescription("Show latency statistics provided by Analysis module: LTTng-UST CallStack (new)")
                .setProviderType(ProviderType.DATA_TREE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider:org.eclipse.tracecompass.lttng2.ust.core.analysis.callstack");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder.setName("LTTng-UST CallStack (new) - Latency Table")
                .setDescription("Show latency table provided by Analysis module: LTTng-UST CallStack (new)")
                .setProviderType(ProviderType.TABLE)
                .setId("org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreTableDataProvider:org.eclipse.tracecompass.lttng2.ust.core.analysis.callstack");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
        builder.setName("LTTng-UST CallStack (new) - Latency vs Time")
                .setDescription("Show latencies provided by Analysis module: LTTng-UST CallStack (new)")
                .setProviderType(ProviderType.TREE_TIME_XY)
                .setId("org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.scatter.dataprovider:org.eclipse.tracecompass.lttng2.ust.core.analysis.callstack");
        EXPECTED_UST_DP_DESCRIPTORS.add(builder.build());
    }

    /**
     * Test Class setup
     */
    @BeforeClass
    public static void init() {
        DataProviderManager.getInstance();
        // Open kernel trace
        fKernelTrace = LttngKernelTestTraceUtils.getTrace(CtfTestTrace.KERNEL);

        // Open UST trace
        fUstTrace = LttngUstTestTraceUtils.getTrace(CtfTestTrace.CYG_PROFILE);

        // Open experiment with kernel and UST trace
        ITmfTrace[] traces = { fKernelTrace, fUstTrace };
        fExperiment = new TmfExperiment(ITmfEvent.class, "TextExperiment", traces, TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, null);
        TmfTraceOpenedSignal openTraceSignal = new TmfTraceOpenedSignal(fExperiment, fExperiment, null);
        TmfSignalManager.dispatchSignal(openTraceSignal);
        fExperiment.indexTrace(true);

        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setName(PATH)
                .setId(ID)
                .setDescription(DESC)
                .setSourceTypeId(SOURCE_ID)
                .setParameters(Collections.emptyMap());
        sfCconfig = builder.build();
        builder.setSourceTypeId(SOURCE_ID + "-1");
        sfCconfig2 = builder.build();
    }

    /**
     * Test class tear down method.
     */
    @AfterClass
    public static void tearDown() {
        // Dispose experiment and traces
        if (fExperiment != null) {
            TmfSignalManager.dispatchSignal(new TmfTraceClosedSignal(fExperiment, fExperiment));
            fExperiment.dispose();
        }
        DataProviderManager.dispose();
    }

    /**
     * Main test case
     */
    @Test
    public void test() {
        List<IDataProviderDescriptor> kernelDescriptors = DataProviderManager.getInstance().getAvailableProviders(fKernelTrace);
        List<IDataProviderDescriptor> ustDescriptors = DataProviderManager.getInstance().getAvailableProviders(fUstTrace);
        List<IDataProviderDescriptor> expDescriptors = DataProviderManager.getInstance().getAvailableProviders(fExperiment);

        // Verify kernel data provider descriptors
        for (IDataProviderDescriptor descriptor : kernelDescriptors) {
            assertTrue(expDescriptors.contains(descriptor));
            assertTrue(descriptor.getName(), EXPECTED_KERNEL_DP_DESCRIPTORS.contains(descriptor));
        }
        // Verify UST data provider descriptors
        for (IDataProviderDescriptor descriptor : ustDescriptors) {
            assertTrue(expDescriptors.contains(descriptor));
            assertTrue(descriptor.getName(), EXPECTED_UST_DP_DESCRIPTORS.contains(descriptor));
        }
    }

    /**
     * Test different get methods
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetter() {
        ITmfTrace trace = fKernelTrace;
        assertNotNull(trace);
        try {
            ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> dp = DataProviderManager.getInstance().getExistingDataProvider(trace, CPU_USAGE_DP_ID, ITmfTreeXYDataProvider.class);
            assertNull(dp);
            dp = DataProviderManager.getInstance().getOrCreateDataProvider(trace, CPU_USAGE_DP_ID, ITmfTreeXYDataProvider.class);
            assertNotNull(dp);
            ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> dp2 = DataProviderManager.getInstance().getExistingDataProvider(trace, CPU_USAGE_DP_ID, ITmfTreeXYDataProvider.class);
            assertNotNull(dp2);
            assertTrue(dp == dp2);
            ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> dp3 = DataProviderManager.getInstance().getOrCreateDataProvider(trace, CPU_USAGE_DP_ID, ITmfTreeXYDataProvider.class);
            assertNotNull(dp3);
            assertTrue(dp == dp3);
            assertTrue(dp == dp2);
        } finally {
            DataProviderManager.getInstance().removeDataProvider(trace, CPU_USAGE_DP_ID);
        }
    }

    /**
     * Test different get methods
     */
    @Test
    public void testGetterNew() {
        ITmfTrace trace = fKernelTrace;
        assertNotNull(trace);
        try {
            ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> dp = DataProviderManager.getInstance().fetchExistingDataProvider(trace, CPU_USAGE_DP_ID, ITmfTreeXYDataProvider.class);
            assertNull(dp);
            dp = DataProviderManager.getInstance().fetchOrCreateDataProvider(trace, CPU_USAGE_DP_ID, ITmfTreeXYDataProvider.class);
            assertNotNull(dp);
            ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> dp2 = DataProviderManager.getInstance().fetchExistingDataProvider(trace, CPU_USAGE_DP_ID, ITmfTreeXYDataProvider.class);
            assertNotNull(dp2);
            assertTrue(dp == dp2);
            ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> dp3 = DataProviderManager.getInstance().fetchOrCreateDataProvider(trace, CPU_USAGE_DP_ID, ITmfTreeXYDataProvider.class);
            assertNotNull(dp3);
            assertTrue(dp == dp3);
            assertTrue(dp == dp2);
        } finally {
            DataProviderManager.getInstance().removeDataProvider(trace, CPU_USAGE_DP_ID);
        }
    }

    /**
     * Test different factory get methods
     */
   @Test
   public void testFactoryMethods() {
       ITmfTrace trace = fKernelTrace;
       assertNotNull(trace);
       Collection<IDataProviderFactory> factories = DataProviderManager.getInstance().getFactories();
       assertNotNull(factories);
       for (IDataProviderFactory factory : factories) {
           Collection<IDataProviderDescriptor> descs = factory.getDescriptors(trace);
           for (IDataProviderDescriptor descriptor : descs) {
               assertTrue(descriptor.getName(), EXPECTED_KERNEL_DP_DESCRIPTORS.contains(descriptor));
           }
       }
       IDataProviderFactory factory =  DataProviderManager.getInstance().getFactory(SEGMENTSTORE_SCATTER_FUTEX_DP_ID);
       assertNotNull(factory);
       Collection<IDataProviderDescriptor> descs = factory.getDescriptors(trace);
       long count = descs.stream().filter(desc -> desc.getId().equals(SEGMENTSTORE_SCATTER_FUTEX_DP_ID)).count();
       assertEquals(1, count);
   }

    /**
     * Test different factory add/remove methods
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddRemoveFactoryMethods() {
        String myId = "my-id";
        IDataProviderFactory testFactory = createCustomFactory(myId);
        ITmfTrace trace = fKernelTrace;
        assertNotNull(trace);
        DataProviderManager.getInstance().addDataProviderFactory(myId, testFactory);
        assertEquals(testFactory, DataProviderManager.getInstance().getFactory(myId));
        List<IDataProviderDescriptor> kernelDescriptors = DataProviderManager.getInstance().getAvailableProviders(trace);
        assertEquals(1, kernelDescriptors.stream().filter(desc -> desc.getId().equals(myId)).count());

        ITimeGraphDataProvider<?> dp = DataProviderManager.getInstance().getOrCreateDataProvider(trace, myId, ITimeGraphDataProvider.class);
        assertNotNull(dp);

        DataProviderManager.getInstance().removeDataProviderFactory(myId);
        assertNull(DataProviderManager.getInstance().getFactory(myId));
        assertNull(DataProviderManager.getInstance().getExistingDataProvider(trace, myId, ITimeGraphDataProvider.class));
    }

    /**
     * Test different data provider add/remove methods
     */
     @SuppressWarnings("unchecked")
     @Test
     public void testRemoveDataProviderMethods() {
         String myId = "my-id";
         IDataProviderFactory testFactory = createCustomFactory(myId);
         ITmfTrace trace = fKernelTrace;
         assertNotNull(trace);
         DataProviderManager.getInstance().addDataProviderFactory(myId, testFactory);

         ITimeGraphDataProvider<?> dp = DataProviderManager.getInstance().getOrCreateDataProvider(trace, myId, ITimeGraphDataProvider.class);
         assertNotNull(dp);

         List<IDataProviderDescriptor> configDescriptors = DataProviderManager.getInstance().getAvailableProviders(trace, sfCconfig);
         assertEquals(1, configDescriptors.size());
         assertEquals(myId, configDescriptors.get(0).getId());

         // test remove by ID
         DataProviderManager.getInstance().removeDataProvider(trace, myId);
         assertNull(DataProviderManager.getInstance().getExistingDataProvider(trace, myId, ITimeGraphDataProvider.class));

         // test remove by dp instance
         dp = DataProviderManager.getInstance().getOrCreateDataProvider(trace, myId, ITimeGraphDataProvider.class);
         assertNotNull(dp);

         assertTrue(DataProviderManager.getInstance().removeDataProvider(trace, dp));
         assertNull(DataProviderManager.getInstance().getExistingDataProvider(trace, myId, ITimeGraphDataProvider.class));
         DataProviderManager.getInstance().removeDataProviderFactory(myId);
     }

    private static IDataProviderFactory createCustomFactory(@NonNull String myId) {
        return new IDataProviderFactory() {
            @SuppressWarnings("restriction")
            @Override
            public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {

                KernelAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
                if (module != null) {
                    return new ThreadStatusDataProvider(trace, module) {
                        @Override
                        public @NonNull String getId() {
                            return myId;
                        }
                    };
                }
                return null;
            }

            @SuppressWarnings("null")
            @Override
            public @NonNull Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
                DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder()
                        .setId(myId)
                        .setName(Objects.requireNonNull(""))
                        .setDescription(Objects.requireNonNull(""))
                        .setProviderType(ProviderType.TIME_GRAPH)
                        .setConfiguration(sfCconfig);
                IDataProviderDescriptor desc1 = builder.build();
                builder.setId(myId + "-1");
                builder.setConfiguration(sfCconfig2);
                IDataProviderDescriptor desc2 = builder.build();
                return List.of(desc1, desc2);
            }
        };
    }
}
