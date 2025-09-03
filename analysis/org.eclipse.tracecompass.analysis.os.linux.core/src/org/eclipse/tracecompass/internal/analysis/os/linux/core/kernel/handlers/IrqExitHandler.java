/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers;

import java.util.List;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Irq Exit handler
 */
public class IrqExitHandler extends KernelEventHandler {

    /**
     * Constructor
     *
     * @param layout
     *            event layout
     */
    public IrqExitHandler(IKernelAnalysisEventLayout layout) {
        super(layout);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) throws AttributeNotFoundException {
        Integer cpu = KernelEventHandlerUtils.getCpu(event);
        if (cpu == null) {
            return;
        }
        int currentThreadNode = KernelEventHandlerUtils.getCurrentThreadNode(cpu, ss);
        Integer irqId = ((Long) event.getContent().getField(getLayout().fieldIrq()).getValue()).intValue();
        List<Integer> subAttrs = ss.getSubAttributes(KernelEventHandlerUtils.getNodeIRQs(cpu, ss), false);
        String irqPrefix = irqId.toString() + "/"; //$NON-NLS-1$
        List<Integer> quarks = subAttrs.stream().filter(iquark -> ss.getAttributeName(iquark).startsWith(irqPrefix)).toList();
        int quark = ITmfStateSystem.INVALID_ATTRIBUTE;
        for (int quarkCandidate : quarks) {
            if (ss.queryOngoing(quarkCandidate) != null) {
                quark = quarkCandidate;
                break;
            }
        }
        String name = ss.getAttributeName(quark);
        /* Put this IRQ back to inactive in the resource tree */
        long timestamp = KernelEventHandlerUtils.getTimestamp(event);
        ss.modifyAttribute(timestamp, (Object) null, quark);

        /* Set the previous process back to running */
        KernelEventHandlerUtils.setProcessToRunning(timestamp, currentThreadNode, ss);

        /* Set the CPU status back to running or "idle" */
        KernelEventHandlerUtils.updateCpuStatus(timestamp, cpu, ss);

        /* Update the aggregate IRQ entry to set it to this CPU */
        int aggregateQuark = ss.getQuarkAbsoluteAndAdd(Attributes.IRQS, name);
        /* Update the aggregate IRQ entry to set it to a running CPU */
        Integer prevCpu = KernelEventHandlerUtils.getCpuForIrq(ss, irqId);
        ss.modifyAttribute(timestamp, prevCpu, aggregateQuark);
    }
}
