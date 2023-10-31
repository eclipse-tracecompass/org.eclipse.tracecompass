/*******************************************************************************
 * Copyright (c) 2013, 2018 Ericsson and others.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.model.OsStrings;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.actions.FollowThreadAction;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FlameChartDataProvider;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FlameChartEntryModel;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FlameChartEntryModel.EntryType;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.ICoreElementResolver;
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
import org.eclipse.tracecompass.tmf.ui.views.TmfViewFactory;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Main implementation for the Flame Chart view
 *
 * @author Patrick Tasse
 * @author Geneviève Bastien
 */
public class FlameChartView extends BaseDataProviderTimeGraphView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /** View ID. */
    public static final String ID = "org.eclipse.tracecompass.analysis.profiling.ui.flamechart"; //$NON-NLS-1$

    private static final String[] COLUMN_NAMES = new String[] {
            Messages.CallStackView_FunctionColumn,
            Messages.CallStackView_DepthColumn,
            Messages.CallStackView_EntryTimeColumn,
            Messages.CallStackView_ExitTimeColumn,
            Messages.CallStackView_DurationColumn
    };

    private static final Comparator<ITimeGraphEntry> DEPTH_COMPARATOR = (o1, o2) -> {
        if (o1 instanceof TimeGraphEntry && o2 instanceof TimeGraphEntry) {
            TimeGraphEntry t1 = (TimeGraphEntry) o1;
            TimeGraphEntry t2 = (TimeGraphEntry) o2;
            ITmfTreeDataModel model1 = t1.getEntryModel();
            ITmfTreeDataModel model2 = t2.getEntryModel();
            if (model1 instanceof FlameChartEntryModel && model2 instanceof FlameChartEntryModel) {
                FlameChartEntryModel m1 = (FlameChartEntryModel) model1;
                FlameChartEntryModel m2 = (FlameChartEntryModel) model2;
                if (m1.getEntryType() == EntryType.FUNCTION && m2.getEntryType() == EntryType.FUNCTION) {
                    return Integer.compare(Integer.valueOf(m1.getName()), Integer.valueOf(m2.getName()));
                }
                return Integer.compare(m1.getEntryType().ordinal(), m2.getEntryType().ordinal());
            }
        }
        return 0;
    };

    @SuppressWarnings("unchecked")
    private static final Comparator<ITimeGraphEntry>[] COMPARATORS = new Comparator[] {
            Comparator.comparing(ITimeGraphEntry::getName),
            DEPTH_COMPARATOR,
            Comparator.comparingLong(ITimeGraphEntry::getStartTime),
            Comparator.comparingLong(ITimeGraphEntry::getEndTime)
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.CallStackView_ThreadColumn
    };

    private static final Image GROUP_IMAGE = Activator.getDefault().getImageFromPath("icons/obj16/thread_obj.gif"); //$NON-NLS-1$
    private static final Image STACKFRAME_IMAGE = Activator.getDefault().getImageFromPath("icons/obj16/stckframe_obj.gif"); //$NON-NLS-1$

    private static final String IMPORT_BINARY_ICON_PATH = "icons/obj16/binaries_obj.gif"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    // The next event action
    private @Nullable Action fNextEventAction = null;

    // The previous event action
    private @Nullable Action fPrevEventAction = null;

    // The action to import a binary file mapping
    private @Nullable Action fConfigureSymbolsAction = null;

    // When set to true, syncToTime() will select the first call stack entry
    // whose current state start time exactly matches the sync time.
    private boolean fSyncSelection = false;

    private final Map<Long, ITimeGraphState> fFunctions = new HashMap<>();

    // ------------------------------------------------------------------------
    // Classes
    // ------------------------------------------------------------------------

    private static class CallStackComparator implements Comparator<ITimeGraphEntry> {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            if (o1 instanceof TimeGraphEntry && o2 instanceof TimeGraphEntry) {
                TimeGraphEntry t1 = (TimeGraphEntry) o1;
                TimeGraphEntry t2 = (TimeGraphEntry) o2;
                return DEPTH_COMPARATOR.compare(t1, t2);
            }
            return 0;
        }
    }

    private class CallStackTreeLabelProvider extends TreeLabelProvider {
        @Override
        public @Nullable Image getColumnImage(@Nullable Object element, int columnIndex) {
            if (columnIndex == 0 && (element instanceof TimeGraphEntry)) {
                TimeGraphEntry entry = (TimeGraphEntry) element;
                ITmfTreeDataModel entryModel = entry.getEntryModel();
                if (entryModel instanceof FlameChartEntryModel) {
                    FlameChartEntryModel model = (FlameChartEntryModel) entryModel;
                    if (model.getEntryType() == EntryType.LEVEL) {
                        // Is this the symbol key image? then show the symbol
                        // key image, otherwise, just the group image
                        return GROUP_IMAGE;
                    }
                    if (model.getEntryType() == EntryType.FUNCTION && fFunctions.containsKey(entryModel.getId())) {
                        return STACKFRAME_IMAGE;
                    }
                }
            }
            return null;
        }

        @Override
        public @Nullable String getColumnText(@Nullable Object element, int columnIndex) {
            if (element instanceof TraceEntry && columnIndex == 0) {
                return ((TraceEntry) element).getName();
            } else if (element instanceof TimeGraphEntry) {
                TimeGraphEntry entry = (TimeGraphEntry) element;
                ITmfTreeDataModel model = entry.getEntryModel();
                ITimeGraphState function = fFunctions.get(model.getId());
                if (columnIndex == 0 && (!(model instanceof FlameChartEntryModel) ||
                        (model instanceof FlameChartEntryModel && ((FlameChartEntryModel) model).getEntryType() != EntryType.FUNCTION))) {
                    // It is not a function entry
                    return entry.getName();
                }

                if (function != null) {
                    if (columnIndex == 0) {
                        // functions
                        return function.getLabel();
                    } else if (columnIndex == 1 && model instanceof FlameChartEntryModel) {
                        return entry.getName();
                    } else if (columnIndex == 2) {
                        return TmfTimestampFormat.getDefaulTimeFormat().format(function.getStartTime());
                    } else if (columnIndex == 3) {
                        return TmfTimestampFormat.getDefaulTimeFormat().format(function.getStartTime() + function.getDuration());
                    } else if (columnIndex == 4) {
                        return TmfTimestampFormat.getDefaulIntervalFormat().format(function.getDuration());
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
        this(ID, new BaseDataProviderTimeGraphPresentationProvider(), FlameChartDataProvider.ID);
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
     */
    @SuppressWarnings("null")
    public FlameChartView(String id, TimeGraphPresentationProvider presentationProvider, String dataProviderID) {
        super(id, presentationProvider, dataProviderID);
        setTreeColumns(COLUMN_NAMES, COMPARATORS, 0);
        setTreeLabelProvider(new CallStackTreeLabelProvider());
        setEntryComparator(new CallStackComparator());
        setFilterColumns(FILTER_COLUMN_NAMES);
        setFilterLabelProvider(new CallStackTreeLabelProvider());
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
     */
    @Override
    @TmfSignalHandler
    public void selectionRangeUpdated(final @Nullable TmfSelectionRangeUpdatedSignal signal) {
        fSyncSelection = true;
        super.selectionRangeUpdated(signal);
    }

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

    @Override
    protected void rebuild() {
        super.rebuild();
        updateConfigureSymbolsAction();
    }

    @Override
    protected String getProviderId() {
        String secondaryId = this.getViewSite().getSecondaryId();
        return (secondaryId == null) ? FlameChartDataProvider.ID : FlameChartDataProvider.ID + ':' + secondaryId.split(TmfViewFactory.INTERNAL_SECONDARY_ID_SEPARATOR)[0]; // NOSONAR
    }

    @Override
    protected void buildEntryList(final ITmfTrace trace, final ITmfTrace parentTrace, final IProgressMonitor monitor) {
        FlameChartDataProvider provider = DataProviderManager
                .getInstance().getOrCreateDataProvider(trace, getProviderId(), FlameChartDataProvider.class);
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
    protected void synchingToTime(final long time) {
        List<TimeGraphEntry> traceEntries = getEntryList(getTrace());
        if (traceEntries != null) {
            for (TraceEntry traceEntry : Iterables.filter(traceEntries, TraceEntry.class)) {
                if (traceEntry.getStartTime() >= time) {
                    continue;
                }
                // Do not query for kernel entries
                Iterable<TimeGraphEntry> unfiltered = Utils.flatten(traceEntry);
                Iterable<TimeGraphEntry> filtered = Iterables.filter(unfiltered, e -> {
                    ITmfTreeDataModel model = e.getEntryModel();
                    if (model instanceof FlameChartEntryModel) {
                        return ((FlameChartEntryModel) model).getEntryType() != EntryType.KERNEL;
                    }
                    return true;
                });
                Map<Long, TimeGraphEntry> map = Maps.uniqueIndex(filtered, e -> e.getEntryModel().getId());
                // use time -1 as a lower bound for the end of Time events to be
                // included.
                SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(Math.max(traceEntry.getStartTime(), time - 1), time, 2, map.keySet());
                TmfModelResponse<TimeGraphModel> response = traceEntry.getProvider().fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(filter), null);
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
        List<ITimeGraphState> list = rowModel.getStates();
        if (!list.isEmpty()) {
            ITimeGraphState event = list.get(0);
            if (event.getStartTime() + event.getDuration() <= time && list.size() > 1) {
                /*
                 * get the second time graph state as passing time - 1 as a
                 * first argument to the filter will get the previous state, if
                 * time is the beginning of an event
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

    @Override
    protected void fillLocalToolBar(@Nullable IToolBarManager manager) {
        if (manager == null) {
            return;
        }
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
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getPreviousItemAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getNextItemAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getZoomInAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getTimeGraphViewer().getZoomOutAction());
    }

    /**
     * Get the the next event action.
     *
     * @return The action object
     */
    @SuppressWarnings("null")
    private Action getNextEventAction() {
        Action nextEventAction = fNextEventAction;
        if (fNextEventAction == null) {
            Action nextAction = getTimeGraphViewer().getNextEventAction();
            fNextEventAction = new Action() {
                @Override
                public void run() {
                    TimeGraphViewer viewer = getTimeGraphViewer();
                    ITimeGraphEntry entry = viewer.getSelection();
                    if (entry instanceof TimeGraphEntry) {
                        TimeGraphEntry callStackEntry = (TimeGraphEntry) entry;
                        ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = getProvider(callStackEntry);
                        long selectionBegin = viewer.getSelectionBegin();
                        Map<@NonNull String, @NonNull Object> parameters = FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(selectionBegin, Long.MAX_VALUE, 2, Collections.singleton(callStackEntry.getEntryModel().getId())));
                        TmfModelResponse<@NonNull TimeGraphModel> response = provider.fetchRowModel(parameters, null);
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
            nextEventAction = fNextEventAction;
            nextEventAction.setText(nextAction.getText());
            nextEventAction.setToolTipText(nextAction.getToolTipText());
            nextEventAction.setImageDescriptor(nextAction.getImageDescriptor());
        }
        return nextEventAction;
    }

    /**
     * Get the previous event action.
     *
     * @return The Action object
     */
    @SuppressWarnings("null")
    private Action getPreviousEventAction() {
        Action prevEventAction = fPrevEventAction;
        if (fPrevEventAction == null) {
            Action prevAction = getTimeGraphViewer().getPreviousEventAction();
            fPrevEventAction = new Action() {
                @Override
                public void run() {
                    TimeGraphViewer viewer = getTimeGraphViewer();
                    ITimeGraphEntry entry = viewer.getSelection();
                    if (entry instanceof TimeGraphEntry) {
                        TimeGraphEntry callStackEntry = (TimeGraphEntry) entry;
                        ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = getProvider(callStackEntry);
                        long selectionBegin = viewer.getSelectionBegin();
                        Map<@NonNull String, @NonNull Object> parameters = FetchParametersUtils
                                .selectionTimeQueryToMap(new SelectionTimeQueryFilter(Lists.newArrayList(Long.MIN_VALUE, selectionBegin), Collections.singleton(callStackEntry.getEntryModel().getId())));
                        TmfModelResponse<@NonNull TimeGraphModel> response = provider.fetchRowModel(parameters, null);
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
            prevEventAction = fPrevEventAction;
            prevEventAction.setText(prevAction.getText());
            prevEventAction.setToolTipText(prevAction.getToolTipText());
            prevEventAction.setImageDescriptor(prevAction.getImageDescriptor());
        }
        return prevEventAction;
    }

    @Override
    protected void fillTimeGraphEntryContextMenu(IMenuManager menuManager) {
        ISelection selection = getSite().getSelectionProvider().getSelection();
        if (selection instanceof StructuredSelection) {
            StructuredSelection sSel = (StructuredSelection) selection;
            if (sSel.getFirstElement() instanceof TimeGraphEntry) {
                TimeGraphEntry entry = (TimeGraphEntry) sSel.getFirstElement();
                ITmfTreeDataModel entryModel = entry.getEntryModel();
                if (entryModel instanceof ICoreElementResolver) {
                    Multimap<String, Object> metadata = ((ICoreElementResolver) entryModel).getMetadata();
                    Collection<Object> tids = metadata.get(OsStrings.tid());
                    if (tids.size() == 1) {
                        HostThread hostThread = new HostThread(getTrace(entry).getHostId(), (Integer) tids.iterator().next());
                        menuManager.add(new FollowThreadAction(FlameChartView.this, String.valueOf(hostThread.getTid()), hostThread));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Methods related to function name mapping
    // ------------------------------------------------------------------------

    private Action getConfigureSymbolsAction() {
        if (fConfigureSymbolsAction != null) {
            return fConfigureSymbolsAction;
        }
        fConfigureSymbolsAction = new Action(Messages.CallStackView_ConfigureSymbolProvidersText) {
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
        Action configureSymbolsAction = fConfigureSymbolsAction;
        configureSymbolsAction.setToolTipText(Messages.CallStackView_ConfigureSymbolProvidersTooltip);
        configureSymbolsAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(IMPORT_BINARY_ICON_PATH));

        /*
         * The updateConfigureSymbolsAction() method (called by refresh()) will
         * set the action to true if applicable after the symbol provider has
         * been properly loaded.
         */
        configureSymbolsAction.setEnabled(false);

        return configureSymbolsAction;
    }

    /**
     * @return an array of {@link ISymbolProviderPreferencePage} that will
     *         configure the current traces
     */
    private ISymbolProviderPreferencePage[] getProviderPages() {
        List<ISymbolProviderPreferencePage> pages = new ArrayList<>();
        ITmfTrace trace = getTrace();
        if (trace != null) {
            for (ITmfTrace subTrace : getTracesToBuild(trace)) {
                @SuppressWarnings("null")
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

    private void resetSymbols() {
        List<TimeGraphEntry> traceEntries = getEntryList(getTrace());
        if (traceEntries != null) {
            for (TraceEntry traceEntry : Iterables.filter(traceEntries, TraceEntry.class)) {
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = traceEntry.getProvider();
                if (provider instanceof FlameChartDataProvider) {
                    ((FlameChartDataProvider) provider).resetFunctionNames(new NullProgressMonitor());
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
    }

    @TmfSignalHandler
    @Override
    public void traceClosed(@Nullable TmfTraceClosedSignal signal) {
        if (signal == null) {
            return;
        }
        List<TimeGraphEntry> traceEntries = getEntryList(signal.getTrace());
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
     */
    @TmfSignalHandler
    public void symbolMapUpdated(TmfSymbolProviderUpdatedSignal signal) {
        if (signal.getSource() != this) {
            resetSymbols();
        }
    }
}
