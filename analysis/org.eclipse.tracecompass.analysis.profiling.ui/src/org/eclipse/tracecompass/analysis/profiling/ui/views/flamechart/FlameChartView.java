/*******************************************************************************
 * Copyright (c) 2013, 2017 Ericsson and others.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *   Bernd Hufmann - Updated signal handling
 *   Marc-Andre Laperle - Map from binary file
 *   Mikael Ferland - Support multiple symbol providers for a trace
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.profiling.ui.views.flamechart;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callstack.provider.CallStackDataProvider;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callstack.provider.CallStackEntryModel;
import org.eclipse.tracecompass.internal.analysis.profiling.ui.Activator;
import org.eclipse.tracecompass.internal.analysis.profiling.ui.views.flamechart.Messages;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProviderPreferencePage;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderConfigDialog;
import org.eclipse.tracecompass.tmf.ui.symbols.TmfSymbolProviderUpdatedSignal;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLog;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLogBuilder;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


/**
 * Main implementation for the Call Stack view
 *
 * @author Patrick Tasse
 */
public class FlameChartView extends BaseDataProviderTimeGraphView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /** View ID. */
    public static final String ID = "org.eclipse.linuxtools.tmf.ui.views.callstack"; //$NON-NLS-1$
    private static final Logger LOGGER = Logger.getLogger(FlameChartView.class.getName());

    private static final String[] COLUMN_NAMES = new String[] {
            Messages.CallStackView_FunctionColumn,
            Messages.CallStackView_PidTidColumn,
            Messages.CallStackView_DepthColumn,
            Messages.CallStackView_EntryTimeColumn,
            Messages.CallStackView_ExitTimeColumn,
            Messages.CallStackView_DurationColumn
    };

    private static final Comparator<ITimeGraphEntry> NAME_COMPARATOR = Comparator.comparing(ITimeGraphEntry::getName);

    private static final Comparator<ITimeGraphEntry> THREAD_ID_COMPARATOR = (o1, o2) -> {
        if (o1 instanceof TimeGraphEntry && o2 instanceof TimeGraphEntry) {
            TimeGraphEntry t1 = (TimeGraphEntry) o1;
            TimeGraphEntry t2 = (TimeGraphEntry) o2;
            ITmfTreeDataModel model1 = t1.getEntryModel();
            ITmfTreeDataModel model2 = t2.getEntryModel();
            if (model1 instanceof CallStackEntryModel && model2 instanceof CallStackEntryModel) {
                CallStackEntryModel m1 = (CallStackEntryModel) model1;
                CallStackEntryModel m2 = (CallStackEntryModel) model2;
                if (m1.getStackLevel() == 0 && m2.getStackLevel() == 0) {
                    return Long.compare(m1.getPid(), m2.getPid());
                }
            }
        }
        return 0;
    };

    private static final Comparator<ITimeGraphEntry> DEPTH_COMPARATOR = (o1, o2) -> {
        if (o1 instanceof TimeGraphEntry && o2 instanceof TimeGraphEntry) {
            TimeGraphEntry t1 = (TimeGraphEntry) o1;
            TimeGraphEntry t2 = (TimeGraphEntry) o2;
            ITmfTreeDataModel model1 = t1.getEntryModel();
            ITmfTreeDataModel model2 = t2.getEntryModel();
            if (model1 instanceof CallStackEntryModel && model2 instanceof CallStackEntryModel) {
                return Integer.compare(((CallStackEntryModel) model1).getStackLevel(), ((CallStackEntryModel) model2).getStackLevel());
            }
        }
        return 0;
    };

    @SuppressWarnings("unchecked")
    private static final Comparator<ITimeGraphEntry>[] COMPARATORS = new Comparator[] {
            Comparator.comparing(ITimeGraphEntry::getName),
            THREAD_ID_COMPARATOR,
            DEPTH_COMPARATOR,
            Comparator.comparingLong(ITimeGraphEntry::getStartTime),
            Comparator.comparingLong(ITimeGraphEntry::getEndTime)
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.CallStackView_ThreadColumn
    };

    private static final Image PROCESS_IMAGE = Objects.requireNonNull(Activator.getDefault()).getImageFromPath("icons/obj16/process_obj.gif"); //$NON-NLS-1$
    private static final Image THREAD_IMAGE = Objects.requireNonNull(Activator.getDefault()).getImageFromPath("icons/obj16/thread_obj.gif"); //$NON-NLS-1$
    private static final Image STACKFRAME_IMAGE = Objects.requireNonNull(Activator.getDefault()).getImageFromPath("icons/obj16/stckframe_obj.gif"); //$NON-NLS-1$

    private static final String IMPORT_BINARY_ICON_PATH = "icons/obj16/binaries_obj.gif"; //$NON-NLS-1$

    private static final Function<@Nullable ITimeGraphEntry, @Nullable String> RESOLVER = (entry) -> {
        if (entry instanceof TimeGraphEntry) {
            ITmfTreeDataModel model = ((TimeGraphEntry) entry).getEntryModel();
            if (model instanceof CallStackEntryModel) {
                int type = ((CallStackEntryModel) model).getStackLevel();
                if (type >= 0) {
                    return Messages.CallStackPresentationProvider_Thread;
                } else if (type == -1) {
                    return Messages.CallStackPresentationProvider_Process;
                }
            }
        }
        return null;
    };

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    // The next event action
    private @Nullable Action fNextEventAction;

    // The previous event action
    private @Nullable Action fPrevEventAction;

    // The next item action
    private @Nullable Action fNextItemAction;

    // The previous item action
    private @Nullable Action fPreviousItemAction;

    // The action to import a binary file mapping */
    private @Nullable Action fConfigureSymbolsAction;

    // When set to true, syncToTime() will select the first call stack entry
    // whose current state start time exactly matches the sync time.
    private boolean fSyncSelection = false;

    private final Map<Long, ITimeGraphState> fFunctions = new HashMap<>();

    // ------------------------------------------------------------------------
    // Classes
    // ------------------------------------------------------------------------

    private class CallStackComparator implements Comparator<ITimeGraphEntry> {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            if (o1 instanceof TimeGraphEntry && o2 instanceof TimeGraphEntry) {
                TimeGraphEntry t1 = (TimeGraphEntry) o1;
                TimeGraphEntry t2 = (TimeGraphEntry) o2;
                ITmfTreeDataModel model1 = t1.getEntryModel();
                ITmfTreeDataModel model2 = t2.getEntryModel();
                CallStackEntryModel m1 = (CallStackEntryModel) model1;
                CallStackEntryModel m2 = (CallStackEntryModel) model2;
                if (m1.getStackLevel() == 0 && m2.getStackLevel() == 0) {
                    return NAME_COMPARATOR.compare(t1, t2);
                } else if (m1.getStackLevel() == -1 && m2.getStackLevel() == -1) {
                    return Integer.compare(m1.getPid(), m2.getPid());
                }
            }
            return 0;
        }
    }

    private class CallStackTreeLabelProvider extends TreeLabelProvider {

        @Override
        public @Nullable Image getColumnImage(@Nullable Object element, int columnIndex) {
            if (columnIndex == 0 && element instanceof TimeGraphEntry) {
                TimeGraphEntry entry = (TimeGraphEntry) element;
                ITmfTreeDataModel entryModel = entry.getEntryModel();
                if (entryModel instanceof CallStackEntryModel) {
                    CallStackEntryModel callStackEntryModel = (CallStackEntryModel) entryModel;
                    if (callStackEntryModel.getStackLevel() == CallStackEntryModel.PROCESS) {
                        return PROCESS_IMAGE;
                    } else if (callStackEntryModel.getStackLevel() == CallStackEntryModel.THREAD) {
                        return THREAD_IMAGE;
                    } else if (fFunctions.containsKey(entryModel.getId())) {
                        return STACKFRAME_IMAGE;
                    }
                }
            }
            return null;
        }

        @Override
        public String getColumnText(@Nullable Object element, int columnIndex) {
            if (element instanceof TraceEntry && columnIndex == 0) {
                return String.valueOf(((TraceEntry) element).getName());
            } else if (element instanceof TimeGraphEntry) {
                TimeGraphEntry entry = (TimeGraphEntry) element;
                ITmfTreeDataModel model = entry.getEntryModel();
                ITimeGraphState function = fFunctions.get(model.getId());
                if (columnIndex == 0 && (!(model instanceof CallStackEntryModel) ||
                        (model instanceof CallStackEntryModel && ((CallStackEntryModel) model).getStackLevel() <= 0))) {
                    // trace, process, threads
                    return String.valueOf(entry.getName());
                }

                if (function != null) {
                    if (columnIndex == 0) {
                        // functions
                        return String.valueOf(function.getLabel());
                    } else if (columnIndex == 2 && model instanceof CallStackEntryModel) {
                        return Integer.toString(((CallStackEntryModel) model).getStackLevel());
                    } else if (columnIndex == 3) {
                        return String.valueOf(TmfTimestampFormat.getDefaulTimeFormat().format(function.getStartTime()));
                    } else if (columnIndex == 4) {
                        return String.valueOf(TmfTimestampFormat.getDefaulTimeFormat().format(function.getStartTime() + function.getDuration()));
                    } else if (columnIndex == 5) {
                        return String.valueOf(TmfTimestampFormat.getDefaulIntervalFormat().format(function.getDuration()));
                    }
                } else if (model instanceof CallStackEntryModel) {
                    CallStackEntryModel callStackEntryModel = (CallStackEntryModel) model;
                    if (columnIndex == 1 && callStackEntryModel.getStackLevel() <= 0 && callStackEntryModel.getPid() >= 0) {
                        return Integer.toString(callStackEntryModel.getPid());
                    } else if (columnIndex == 3 && callStackEntryModel.getStackLevel() <= 0) {
                        return String.valueOf(TmfTimestampFormat.getDefaulTimeFormat().format(callStackEntryModel.getStartTime()));
                    } else if (columnIndex == 4 && callStackEntryModel.getStackLevel() <= 0) {
                        return String.valueOf(TmfTimestampFormat.getDefaulTimeFormat().format(callStackEntryModel.getEndTime()));
                    }

                }
            }
            return ""; //$NON-NLS-1$
        }
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public FlameChartView() {
        this(ID, new BaseDataProviderTimeGraphPresentationProvider(), CallStackDataProvider.ID);
    }

    /**
     * Custom constructor, used for extending the callstack view with a custom
     * presentation provider or data provider.
     *
     * @param id
     *            The ID of the view
     * @param presentationProvider
     *            the presentation provider
     * @param dataProviderID
     *            the data provider id
     * @since 3.3
     */
    public FlameChartView(String id, TimeGraphPresentationProvider presentationProvider, String dataProviderID) {
        super(id, presentationProvider, dataProviderID);
        setTreeColumns(COLUMN_NAMES, COMPARATORS, 0);
        setTreeLabelProvider(new CallStackTreeLabelProvider());
        setEntryComparator(new CallStackComparator());
        setFilterColumns(FILTER_COLUMN_NAMES);
        setFilterLabelProvider(new CallStackTreeLabelProvider());
        setStateTypeNameSupplier(RESOLVER);
    }

    // ------------------------------------------------------------------------
    // ViewPart
    // ------------------------------------------------------------------------

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);

        getTimeGraphViewer().addTimeListener(event -> synchingToTime(event.getBeginTime()));

        getTimeGraphViewer().getTimeGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(@Nullable MouseEvent event) {
                ITimeGraphEntry selection = getTimeGraphViewer().getSelection();
                if (!(selection instanceof TimeGraphEntry)) {
                    // also null checks
                    return;
                }
                ITimeGraphState function = fFunctions.get(((TimeGraphEntry) selection).getEntryModel().getId());
                if (function != null) {
                    long entryTime = function.getStartTime();
                    long exitTime = entryTime + function.getDuration();
                    TmfTimeRange range = new TmfTimeRange(TmfTimestamp.fromNanos(entryTime), TmfTimestamp.fromNanos(exitTime));
                    broadcast(new TmfWindowRangeUpdatedSignal(FlameChartView.this, range, getTrace()));
                    getTimeGraphViewer().setStartFinishTime(entryTime, exitTime);
                    startZoomThread(entryTime, exitTime);
                }
            }
        });

        getTimeGraphViewer().getTimeGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(@Nullable MouseEvent e) {
                TimeGraphControl timeGraphControl = getTimeGraphViewer().getTimeGraphControl();
                ISelection selection = timeGraphControl.getSelection();
                if (selection instanceof IStructuredSelection) {
                    for (Object object : ((IStructuredSelection) selection).toList()) {
                        if (object instanceof NamedTimeEvent) {
                            NamedTimeEvent event = (NamedTimeEvent) object;
                            long startTime = event.getTime();
                            long endTime = startTime + event.getDuration();
                            TmfTimeRange range = new TmfTimeRange(TmfTimestamp.fromNanos(startTime), TmfTimestamp.fromNanos(endTime));
                            broadcast(new TmfWindowRangeUpdatedSignal(FlameChartView.this, range, getTrace()));
                            getTimeGraphViewer().setStartFinishTime(startTime, endTime);
                            startZoomThread(startTime, endTime);
                            break;
                        }
                    }
                }
            }
        });

        IEditorPart editor = getSite().getPage().getActiveEditor();
        if (editor instanceof ITmfTraceEditor) {
            ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
            if (trace != null) {
                traceSelected(new TmfTraceSelectedSignal(this, trace));
            }
        }
    }

    /**
     * Handler for the selection range signal.
     *
     * @param signal
     *            The incoming signal
     * @since 1.0
     */
    @Override
    @TmfSignalHandler
    public void selectionRangeUpdated(final @Nullable TmfSelectionRangeUpdatedSignal signal) {
        fSyncSelection = true;
        super.selectionRangeUpdated(signal);
    }

    /**
     * @since 2.0
     */
    @Override
    @TmfSignalHandler
    public void windowRangeUpdated(final @Nullable TmfWindowRangeUpdatedSignal signal) {
        if (signal == null || signal.getSource() == this) {
            return;
        }
        super.windowRangeUpdated(signal);
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    /**
     * @since 2.0
     */
    @Override
    protected void rebuild() {
        super.rebuild();
        updateConfigureSymbolsAction();
    }

    @Override
    protected void buildEntryList(final ITmfTrace trace, final ITmfTrace parentTrace, final IProgressMonitor monitor) {
        CallStackDataProvider provider = DataProviderManager.getInstance().getOrCreateDataProvider(trace,
                getProviderId(), CallStackDataProvider.class);
        if (provider == null) {
            addUnavailableEntry(trace, parentTrace);
            return;
        }

        provider.resetFunctionNames(monitor);
        super.buildEntryList(trace, parentTrace, monitor);
    }

    private void addUnavailableEntry(ITmfTrace trace, ITmfTrace parentTrace) {
        String name = Messages.CallStackView_StackInfoNotAvailable + ' ' + '(' + trace.getName() + ')';
        TimeGraphEntry unavailableEntry = new TimeGraphEntry(name, 0, 0) {
            @Override
            public boolean hasTimeEvents() {
                return false;
            }
        };
        addToEntryList(parentTrace, Collections.singletonList(unavailableEntry));
        if (parentTrace == getTrace()) {
            refresh();
        }
    }

    @Override
    protected TimeEvent createTimeEvent(@Nullable TimeGraphEntry entry, @Nullable ITimeGraphState state) {
        requireNonNull(state, "state should not be null when creating time event"); //$NON-NLS-1$

        String label = state.getLabel();
        if (state.getValue() == Integer.MIN_VALUE && label == null && state.getStyle() == null) {
            return new NullTimeEvent(entry, state.getStartTime(), state.getDuration());
        }
        if (label != null) {
            return new NamedTimeEvent(entry, label, state);
        }
        return new TimeEvent(entry, state);
    }

    /**
     * @since 1.2
     */
    @Override
    protected void synchingToTime(final long time) {
        List<TimeGraphEntry> traceEntries = getEntryList(getTrace());
        if (traceEntries != null) {
            for (TraceEntry traceEntry : Iterables.filter(traceEntries, TraceEntry.class)) {
                Iterable<TimeGraphEntry> unfiltered = Utils.flatten(traceEntry);
                Map<Long, TimeGraphEntry> map = Maps.uniqueIndex(unfiltered, e -> e.getEntryModel().getId());
                // use time -1 as a lower bound for the end of Time events to be included.
                SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(time - 1, time, 2, map.keySet());
                TmfModelResponse<@NonNull TimeGraphModel> response = traceEntry.getProvider().fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(filter), null);
                TimeGraphModel model = response.getModel();
                if (model != null) {
                    for (ITimeGraphRowModel row : model.getRows()) {
                        syncToRow(row, time, map);
                    }
                }
            }
        }
        fSyncSelection = false;
        if (Display.getCurrent() != null) {
            getTimeGraphViewer().refresh();
        }
    }

    private void syncToRow(ITimeGraphRowModel rowModel, long time, Map<Long, TimeGraphEntry> entryMap) {
        long id = rowModel.getEntryID();
        List<@NonNull ITimeGraphState> list = rowModel.getStates();
        if (!list.isEmpty()) {
            ITimeGraphState event = list.get(0);
            if (event.getStartTime() + event.getDuration() <= time && list.size() > 1) {
                /*
                 * get the second time graph state as passing time - 1 as a first argument to
                 * the filter will get the previous state, if time is the beginning of an event
                 */
                event = list.get(1);
            }
            if (event.getLabel() != null) {
                fFunctions.put(id, event);
            } else {
                fFunctions.remove(id);
            }

            if (fSyncSelection && time == event.getStartTime()) {
                TimeGraphEntry entry = entryMap.get(id);
                if (entry != null) {
                    fSyncSelection = false;
                    Display.getDefault().asyncExec(() -> {
                        getTimeGraphViewer().setSelection(entry, true);
                        getTimeGraphViewer().getTimeGraphControl().fireSelectionChanged();
                    });
                }
            }
        } else {
            fFunctions.remove(id);
        }
    }

    private void makeActions() {
        Action previousItemAction = getTimeGraphViewer().getPreviousItemAction();
        fPreviousItemAction = previousItemAction;
        previousItemAction.setText(Messages.FlameChartView_PreviousItemActionNameText);
        previousItemAction.setToolTipText(Messages.FlameChartView_PreviousItemActionToolTipText);
        Action nextItemAction = getTimeGraphViewer().getNextItemAction();
        fNextItemAction = nextItemAction;
        nextItemAction.setText(Messages.FlameChartView_NextItemActionNameText);
        nextItemAction.setToolTipText(Messages.FlameChartView_NextItemActionToolTipText);
    }

    /**
     * @since 1.2
     */
    @Override
    protected void fillLocalToolBar(@Nullable IToolBarManager manager) {
        if (manager == null) {
            return;
        }
        makeActions();
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getConfigureSymbolsAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getShowFilterDialogAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getResetScaleAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getPreviousEventAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getNextEventAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getToggleBookmarkAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getPreviousMarkerAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getNextMarkerAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fPreviousItemAction);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fNextItemAction);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getZoomInAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getZoomOutAction());
    }

    /**
     * Get the the next event action.
     *
     * @return The action object
     */
    private Action getNextEventAction() {
        Action nextAction = fNextEventAction;
        if (nextAction == null) {
            Action superNextAction = getTimeGraphViewer().getNextEventAction();
            nextAction = new Action() {
                @Override
                public void run() {
                    TimeGraphViewer viewer = getTimeGraphViewer();
                    ITimeGraphEntry entry = viewer.getSelection();
                    if (entry instanceof TimeGraphEntry) {
                        TimeGraphEntry callStackEntry = (TimeGraphEntry) entry;
                        ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = getProvider(callStackEntry);
                        long selectionBegin = viewer.getSelectionBegin();
                        SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(selectionBegin, Long.MAX_VALUE, 2, Collections.singleton(callStackEntry.getEntryModel().getId()));
                        TmfModelResponse<@NonNull TimeGraphModel> response = provider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(filter), null);
                        TimeGraphModel model = response.getModel();
                        if (model == null || model.getRows().size() != 1) {
                            return;
                        }
                        List<@NonNull ITimeGraphState> row = model.getRows().get(0).getStates();
                        if (row.size() != 1) {
                            return;
                        }
                        ITimeGraphState stackInterval = row.get(0);
                        if (stackInterval.getStartTime() <= selectionBegin && selectionBegin <= stackInterval.getStartTime() + stackInterval.getDuration()) {
                            viewer.setSelectedTimeNotify(stackInterval.getStartTime() + stackInterval.getDuration() + 1, true);
                        } else {
                            viewer.setSelectedTimeNotify(stackInterval.getStartTime(), true);
                        }
                        int stackLevel = stackInterval.getValue();
                        ITimeGraphEntry selectedEntry = callStackEntry.getParent().getChildren().get(Integer.max(0, stackLevel - 1));
                        viewer.setSelection(selectedEntry, true);
                        viewer.getTimeGraphControl().fireSelectionChanged();
                        startZoomThread(viewer.getTime0(), viewer.getTime1());
                    }
                }
            };

            nextAction.setText(superNextAction.getText());
            nextAction.setToolTipText(superNextAction.getToolTipText());
            nextAction.setImageDescriptor(superNextAction.getImageDescriptor());
            fNextEventAction = nextAction;
        }

        return nextAction;
    }

    /**
     * Get the previous event action.
     *
     * @return The Action object
     */
    private Action getPreviousEventAction() {
        Action prevAction = fPrevEventAction;
        if (prevAction == null) {
            Action superPrevAction = getTimeGraphViewer().getPreviousEventAction();
            prevAction = new Action() {
                @Override
                public void run() {
                    TimeGraphViewer viewer = getTimeGraphViewer();
                    ITimeGraphEntry entry = viewer.getSelection();
                    if (entry instanceof TimeGraphEntry) {
                        TimeGraphEntry callStackEntry = (TimeGraphEntry) entry;
                        ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = getProvider(callStackEntry);
                        long selectionBegin = viewer.getSelectionBegin();
                        SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(Lists.newArrayList(Long.MIN_VALUE, selectionBegin), Collections.singleton(callStackEntry.getEntryModel().getId()));
                        TmfModelResponse<@NonNull TimeGraphModel> response = provider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(filter), null);
                        TimeGraphModel model = response.getModel();
                        if (model == null || model.getRows().size() != 1) {
                            return;
                        }
                        List<@NonNull ITimeGraphState> row = model.getRows().get(0).getStates();
                        if (row.size() != 1) {
                            return;
                        }
                        ITimeGraphState stackInterval = row.get(0);
                        viewer.setSelectedTimeNotify(stackInterval.getStartTime(), true);
                        int stackLevel = stackInterval.getValue();
                        ITimeGraphEntry selectedEntry = callStackEntry.getParent().getChildren().get(Integer.max(0, stackLevel - 1));
                        viewer.setSelection(selectedEntry, true);
                        viewer.getTimeGraphControl().fireSelectionChanged();
                        startZoomThread(viewer.getTime0(), viewer.getTime1());
                    }
                }
            };

            prevAction.setText(superPrevAction.getText());
            prevAction.setToolTipText(superPrevAction.getToolTipText());
            prevAction.setImageDescriptor(superPrevAction.getImageDescriptor());
            fPrevEventAction = prevAction;
        }

        return prevAction;
    }

    /**
     * Sets the state type name supplier, useful for customized flamecharts.
     *
     * @since 3.0
     */
    protected void setStateTypeNameSupplier(Function<@Nullable ITimeGraphEntry, @Nullable String> resolver) {
        ((BaseDataProviderTimeGraphPresentationProvider) getPresentationProvider()).setStateTypeNameResolver(resolver);
    }

    // ------------------------------------------------------------------------
    // Methods related to function name mapping
    // ------------------------------------------------------------------------

    private Action getConfigureSymbolsAction() {
        if (fConfigureSymbolsAction != null) {
            return fConfigureSymbolsAction;
        }

        Action configureSymbolsAction = new Action(Messages.CallStackView_ConfigureSymbolProvidersText) {
            @Override
            public void run() {
                SymbolProviderConfigDialog dialog = new SymbolProviderConfigDialog(getSite().getShell(), getProviderPages());
                if (dialog.open() == IDialogConstants.OK_ID) {
                    /*
                     * Nothing to do. SymbolProviderConfigDialog will send a
                     * TmfSymbolProviderUpdatedSignal to notify registered components.
                     */
                }
            }
        };

        configureSymbolsAction.setToolTipText(Messages.CallStackView_ConfigureSymbolProvidersTooltip);
        configureSymbolsAction.setImageDescriptor(Objects.requireNonNull(Activator.getDefault()).getImageDescripterFromPath(IMPORT_BINARY_ICON_PATH));

        /*
         * The updateConfigureSymbolsAction() method (called by refresh()) will set the
         * action to true if applicable after the symbol provider has been properly
         * loaded.
         */
        configureSymbolsAction.setEnabled(false);

        fConfigureSymbolsAction = configureSymbolsAction;
        return configureSymbolsAction;
    }

    /**
     * @return an array of {@link ISymbolProviderPreferencePage} that will configure
     *         the current traces
     */
    private ISymbolProviderPreferencePage[] getProviderPages() {
        List<ISymbolProviderPreferencePage> pages = new ArrayList<>();
        ITmfTrace trace = getTrace();
        if (trace != null) {
            for (ITmfTrace subTrace : getTracesToBuild(trace)) {
                Collection<@NonNull ISymbolProvider> symbolProviders = SymbolProviderManager.getInstance().getSymbolProviders(subTrace);
                for (org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider provider : Iterables.filter(symbolProviders, org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider.class)) {
                    ISymbolProviderPreferencePage page = provider.createPreferencePage();
                    if (page != null) {
                        pages.add(page);
                    }
                }
            }
        }
        return pages.toArray(new ISymbolProviderPreferencePage[pages.size()]);
    }

    /**
     * Update the enable status of the configure symbols action
     */
    private void updateConfigureSymbolsAction() {
        ISymbolProviderPreferencePage[] providerPages = getProviderPages();
        getConfigureSymbolsAction().setEnabled(providerPages.length > 0);
    }

    private IStatus resetSymbolsSync() {
        List<TimeGraphEntry> traceEntries = getEntryList(getTrace());
        if (traceEntries != null) {
            for (TraceEntry traceEntry : Iterables.filter(traceEntries, TraceEntry.class)) {
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = traceEntry.getProvider();
                if (provider instanceof CallStackDataProvider callstackProvider) {
                    callstackProvider.resetFunctionNames(new NullProgressMonitor());
                }

                // reset full and zoomed events here
                Iterable<TimeGraphEntry> flatten = Utils.flatten(traceEntry);
                flatten.forEach(e -> e.setSampling(null));

                // recompute full events
                long start = traceEntry.getStartTime();
                long end = traceEntry.getEndTime();
                final long resolution = Long.max(1, (end - start) / getDisplayWidth());
                zoomEntries(flatten, start, end, resolution, new NullProgressMonitor());
            }
            // zoomed events will be retriggered by refreshing
            refresh();
        }
        synchingToTime(getTimeGraphViewer().getSelectionBegin());
        return Status.OK_STATUS;
    }

    private void resetSymbols() {
        try (FlowScopeLog flowParent = new FlowScopeLogBuilder(LOGGER, Level.FINE, "Resetting Symbols - Job launch").setCategory("Flame Chart").build();) { //$NON-NLS-1$ //$NON-NLS-2$
            new Job("Resetting Symbols") { //$NON-NLS-1$
                @Override
                protected IStatus run(@Nullable IProgressMonitor monitor) {
                    try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "Resetting Symbols").setParentScope(flowParent).build()) { //$NON-NLS-1$
                        return resetSymbolsSync();
                    }
                }
            }.schedule();
        }
    }

    @TmfSignalHandler
    @Override
    public void traceClosed(@Nullable TmfTraceClosedSignal signal) {
        List<@NonNull TimeGraphEntry> traceEntries = getEntryList(Objects.requireNonNull(signal).getTrace());
        if (traceEntries != null) {
            /*
             * remove functions associated to the trace's entries.
             */
            Iterable<TimeGraphEntry> all = Iterables.concat(Iterables.transform(traceEntries, Utils::flatten));
            all.forEach(entry -> fFunctions.remove(entry.getEntryModel().getId()));
        }
        super.traceClosed(signal);
    }

    /**
     * Symbol map provider updated
     *
     * @param signal
     *            the signal
     * @since 4.1
     */
    @TmfSignalHandler
    public void symbolMapUpdated(TmfSymbolProviderUpdatedSignal signal) {
        if (signal.getSource() != this) {
            resetSymbols();
        }
    }
}
