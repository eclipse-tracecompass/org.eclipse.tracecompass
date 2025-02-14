/*******************************************************************************
 * Copyright (c) 2014, 2025 École Polytechnique de Montréal and others.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *   Mikael Ferland - Enable usage of different tree viewer types
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.viewers.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.internal.tmf.ui.viewers.tree.TreeUtil;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfTimeViewer;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.ScopeLog;

/**
 * Abstract class for viewers who will display data using a TreeViewer. It
 * automatically synchronizes with time information of the UI. It also
 * implements some common functionalities for all tree viewer, such as managing
 * the column data, content initialization and update. The viewer implementing
 * this does not have to worry about whether some code runs in the UI thread or
 * not.
 *
 * @author Geneviève Bastien
 */
public abstract class AbstractTmfTreeViewer extends TmfTimeViewer {

    /**
     * Selective expansion is O(n2) in jface. So have a limit set. If it's too big,
     * we don't care anyway, people will filter
     */
    private static final int THRESHOLD_TO_PRESERVE_TREE = 500;
    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(AbstractTmfTreeViewer.class);
    private final TreeViewer fTreeViewer;
    private final Map<ITmfTrace, TmfTreeViewerEntry> fRoots = Collections.synchronizedMap(new HashMap<>());

    // ------------------------------------------------------------------------
    // Internal classes
    // ------------------------------------------------------------------------

    /* The elements of the tree viewer are of type ITmfTreeViewerEntry */
    private class TreeContentProvider implements ITreeContentProvider {

        @Override
        public void dispose() {
            // Do nothing
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Do nothing
        }

        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof ITmfTreeViewerEntry) {
                return ((ITmfTreeViewerEntry) inputElement).getChildren().toArray(new ITmfTreeViewerEntry[0]);
            }
            return new ITmfTreeViewerEntry[0];
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            ITmfTreeViewerEntry entry = (ITmfTreeViewerEntry) parentElement;
            List<? extends ITmfTreeViewerEntry> children = entry.getChildren();
            return children.toArray(new ITmfTreeViewerEntry[children.size()]);
        }

        @Override
        public Object getParent(Object element) {
            ITmfTreeViewerEntry entry = (ITmfTreeViewerEntry) element;
            return entry.getParent();
        }

        @Override
        public boolean hasChildren(Object element) {
            ITmfTreeViewerEntry entry = (ITmfTreeViewerEntry) element;
            return entry.hasChildren();
        }

    }

    /**
     * Base class to provide the labels for the tree viewer. Views extending
     * this class typically need to override the getColumnText method if they
     * have more than one column to display. It also allows to change the font
     * and colors of the cells.
     */
    protected static class TreeLabelProvider implements ITableLabelProvider, ITableFontProvider, ITableColorProvider {

        @Override
        public void addListener(ILabelProviderListener listener) {
            // Do nothing
        }

        @Override
        public void dispose() {
            // Do nothing
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        @Override
        public void removeListener(ILabelProviderListener listener) {
            // Do nothing
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if ((element instanceof ITmfTreeViewerEntry) && (columnIndex == 0)) {
                ITmfTreeViewerEntry entry = (ITmfTreeViewerEntry) element;
                return entry.getName();
            }
            return new String();
        }

        @Override
        public Color getForeground(Object element, int columnIndex) {
            return null;
        }

        @Override
        public Color getBackground(Object element, int columnIndex) {
            return null;
        }

        @Override
        public Font getFont(Object element, int columnIndex) {
            return null;
        }

    }

    /**
     * Element comparer that considers elements of class
     * {@link TmfGenericTreeEntry} with the same model id to be equal.
     *
     * @since 6.2
     */
    protected static class ElementComparer implements IElementComparer {
        @Override
        public boolean equals(@Nullable Object a, @Nullable Object b) {
            if (a instanceof TmfGenericTreeEntry && b instanceof TmfGenericTreeEntry) {
                ITmfTreeDataModel aModel = ((TmfGenericTreeEntry<?>) a).getModel();
                ITmfTreeDataModel bModel = ((TmfGenericTreeEntry<?>) b).getModel();
                if (aModel != null && bModel != null) {
                    return aModel.getId() == bModel.getId();
                }
            }
            return Objects.equals(a, b);
        }

        @Override
        public int hashCode(@Nullable Object element) {
            if (element instanceof TmfGenericTreeEntry) {
                ITmfTreeDataModel model = ((TmfGenericTreeEntry<?>) element).getModel();
                if (model != null) {
                    return Long.hashCode(model.getId());
                }
            }
            return Objects.hashCode(element);
        }
    }

    // ------------------------------------------------------------------------
    // Constructors and initialization methods
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param parent
     *            The parent composite that holds this viewer
     * @param allowMultiSelect
     *            Whether multiple selections are allowed
     */
    public AbstractTmfTreeViewer(Composite parent, boolean allowMultiSelect) {
        this(parent, new TreeViewer(parent, SWT.FULL_SELECTION | SWT.H_SCROLL | (allowMultiSelect ? SWT.MULTI : 0)));
    }

    /**
     * Constructor
     *
     * @param parent
     *            The parent composite that holds this viewer
     * @param treeViewer
     *            The tree viewer to use
     * @since 3.1
     */
    public AbstractTmfTreeViewer(Composite parent, TreeViewer treeViewer) {
        super(parent);
        /* Build the tree viewer part of the view */
        fTreeViewer = treeViewer;
        final Tree tree = fTreeViewer.getTree();
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);
        fTreeViewer.setContentProvider(new TreeContentProvider());
        fTreeViewer.setLabelProvider(new TreeLabelProvider());
        fTreeViewer.setComparer(new ElementComparer());
        List<TmfTreeColumnData> columns = getColumnDataProvider().getColumnData();
        this.setTreeColumns(columns);
    }

    /**
     * Get the column data provider that will contain the list of columns to be
     * part of this viewer. It is called once during the constructor.
     *
     * @return The tree column data provider for this viewer.
     */
    protected abstract ITmfTreeColumnDataProvider getColumnDataProvider();

    /**
     * Sets the tree columns for this tree viewer
     *
     * @param columns
     *            The tree column data
     */
    public void setTreeColumns(final List<TmfTreeColumnData> columns) {
        boolean hasPercentProvider = false;
        for (final TmfTreeColumnData columnData : columns) {
            columnData.createColumn(fTreeViewer);
            hasPercentProvider |= (columnData.getPercentageProvider() != null);
        }

        if (hasPercentProvider) {
            /*
             * Handler that will draw bar charts in the cell using a percentage
             * value.
             */
            fTreeViewer.getTree().addListener(SWT.EraseItem, event -> {
                if (columns.get(event.index).getPercentageProvider() != null) {

                    double percentage = columns.get(event.index).getPercentageProvider().getPercentage(event.item.getData());
                    if (percentage == 0) { // No bar to draw
                        return;
                    }

                    if ((event.detail & SWT.SELECTED) > 0) {
                        /*
                         * The item is selected. Draw our own background to
                         * avoid overwriting the bar.
                         */
                        event.gc.fillRectangle(event.x, event.y, event.width, event.height);
                        event.detail &= ~SWT.SELECTED;
                    }

                    int barWidth = (int) ((fTreeViewer.getTree().getColumn(event.index).getWidth() - 8) * percentage);
                    int oldAlpha = event.gc.getAlpha();
                    Color oldForeground = event.gc.getForeground();
                    Color oldBackground = event.gc.getBackground();
                    /*
                     * Draws a transparent gradient rectangle from the color of
                     * foreground and background.
                     */
                    event.gc.setAlpha(64);
                    event.gc.setForeground(event.item.getDisplay().getSystemColor(SWT.COLOR_BLUE));
                    event.gc.setBackground(event.item.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
                    event.gc.fillGradientRectangle(event.x, event.y, barWidth, event.height, true);
                    event.gc.drawRectangle(event.x, event.y, barWidth, event.height);
                    /* Restores old values */
                    event.gc.setForeground(oldForeground);
                    event.gc.setBackground(oldBackground);
                    event.gc.setAlpha(oldAlpha);
                    event.detail &= ~SWT.BACKGROUND;
                }
            });
        }
    }

    /**
     * Set the label provider that will fill the columns of the tree viewer
     *
     * @param labelProvider
     *            The label provider to fill the columns
     */
    protected void setLabelProvider(IBaseLabelProvider labelProvider) {
        fTreeViewer.setLabelProvider(labelProvider);
    }

    /**
     * Get the tree viewer object
     *
     * @return The tree viewer object displayed by this viewer
     * @since 2.2
     */
    public TreeViewer getTreeViewer() {
        return fTreeViewer;
    }

    // ------------------------------------------------------------------------
    // ITmfViewer
    // ------------------------------------------------------------------------

    @Override
    public Control getControl() {
        return fTreeViewer.getControl();
    }

    @Override
    public void refresh() {
        try (ScopeLog refreshTree = new ScopeLog(LOGGER, Level.FINE, getClass().getCanonicalName() + "#refresh()")) {
            Tree tree = fTreeViewer.getTree();
            tree.setRedraw(false);
            fTreeViewer.refresh();
            tree.setRedraw(true);
        }
    }

    @Override
    public void loadTrace(ITmfTrace trace) {
        super.loadTrace(trace);
        fRoots.computeIfAbsent(trace, t -> new TmfTreeViewerEntry(t.getName()));
        if (trace == null) {
            return;
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                initializeDataSource(trace);
                Display.getDefault().asyncExec(() -> {
                    if (!trace.equals(getTrace())) {
                        return;
                    }
                    clearContent();
                    updateContent(getWindowStartTime(), getWindowEndTime(), false);
                });
            }
        };
        thread.start();
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Gets the root element for the specified trace. Using a fixed root element
     * helps the tree maintain its state when the model is updated or sorted.
     *
     * @param trace
     *            the trace
     * @return the root element, or null if the trace is closed
     * @since 6.2
     */
    public @Nullable TmfTreeViewerEntry getRoot(ITmfTrace trace) {
        return fRoots.get(trace);
    }

    /**
     * Set the currently selected items in the treeviewer
     *
     * @param selection
     *            The list of selected items
     */
    public void setSelection(@NonNull List<ITmfTreeViewerEntry> selection) {
        IStructuredSelection sel = new StructuredSelection(selection);
        fTreeViewer.setSelection(sel, true);
    }

    /**
     * Add a selection listener to the tree viewer. This will be called when the
     * selection changes and contain all the selected items.
     *
     * The selection change listener can be used like this:
     *
     * <pre>
     * getTreeViewer().addSelectionChangeListener(new ISelectionChangedListener() {
     *     &#064;Override
     *     public void selectionChanged(SelectionChangedEvent event) {
     *         if (event.getSelection() instanceof IStructuredSelection) {
     *             Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
     *             if (selection instanceof ITmfTreeViewerEntry) {
     *                 // Do something
     *             }
     *         }
     *     }
     * });
     * </pre>
     *
     * @param listener
     *            The {@link ISelectionChangedListener}
     */
    public void addSelectionChangeListener(ISelectionChangedListener listener) {
        fTreeViewer.addSelectionChangedListener(listener);
    }

    /**
     * Method called when the trace is loaded, to initialize any data once the
     * trace has been set, but before the first call to update the content of
     * the viewer.
     *
     * @param trace
     *            the trace being loaded
     * @since 3.3
     */
    protected void initializeDataSource(@NonNull ITmfTrace trace) {
        /* Override to initialize the data source */
    }

    /**
     * Clears the current content of the viewer.
     */
    protected void clearContent() {
        fTreeViewer.setInput(null);
    }

    /**
     * Method called after the content has been updated and the new input has
     * been set on the tree.
     *
     * @param rootEntry
     *            The new input of this viewer, or null if none
     */
    protected void contentChanged(ITmfTreeViewerEntry rootEntry) {

    }

    /**
     * Requests an update of the viewer's content in a given time range or
     * selection time range. An extra parameter defines whether these times
     * correspond to the selection or the visible range, as the viewer may
     * update differently in those cases.
     *
     * @param start
     *            The start time of the requested content
     * @param end
     *            The end time of the requested content
     * @param isSelection
     *            <code>true</code> if this time range is for a selection,
     *            <code>false</code> for the visible time range
     */
    protected void updateContent(final long start, final long end, final boolean isSelection) {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }
        Job thread = new Job("") { //$NON-NLS-1$
            @Override
            public IStatus run(IProgressMonitor monitor) {
                final ITmfTreeViewerEntry newRootEntry = updateElements(trace, start, end, isSelection);
                /* Set the input in main thread only if it changed */
                if (newRootEntry != null) {
                    Display.getDefault().asyncExec(() -> {
                        if (fTreeViewer.getControl().isDisposed()) {
                            return;
                        }

                        Object currentRootEntry = fTreeViewer.getInput();
                        updateTreeUI(fTreeViewer, newRootEntry);
                        if (newRootEntry != currentRootEntry) {
                            // FIXME should add a bit of padding
                            for (TreeColumn column : fTreeViewer.getTree().getColumns()) {
                                column.pack();
                            }
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        };
        thread.setSystem(true);
        thread.schedule();
    }

    /**
     * Update tree UI, this preserves selection and expansion levels, needs to
     * be called in UI thread
     *
     * @param treeViewer
     *            the tree to update
     * @param newInput
     *            the entries to display
     *
     * @since 6.0
     */
    protected final void updateTreeUI(TreeViewer treeViewer, @NonNull ITmfTreeViewerEntry newInput) {
        try (ScopeLog updateTreeUi = new ScopeLog(LOGGER, Level.FINE, getClass().getSimpleName() + "#updateTreeUI")) { //$NON-NLS-1$
            Set<String> expandedPaths = new HashSet<>();
            Object input = fTreeViewer.getInput();

            /*
             * Find in the new entries the equivalent of the selected one
             */
            ISelection selection = null;
            try (ScopeLog selection1 = new ScopeLog(LOGGER, Level.FINE, getClass().getSimpleName() + "#getSelection")) { //$NON-NLS-1$
                selection = TreeUtil.getNewSelection(fTreeViewer.getSelection(), newInput);
            }
            try (ScopeLog updatePaths = new ScopeLog(LOGGER, Level.FINE, getClass().getSimpleName() + "#updatePaths")) { //$NON-NLS-1$
                /*
                 * Get currently expanded nodes
                 */
                Object[] expandedElements = fTreeViewer.getExpandedElements();
                for (Object element : expandedElements) {
                    if (element instanceof ITmfTreeViewerEntry) {
                        expandedPaths.add(TreeUtil.getPath((ITmfTreeViewerEntry) element).toString());
                    }
                }
            }
            // It's a salad
            Set<@NonNull ITmfTreeViewerEntry> allLeafEntries = new HashSet<>();
            Set<String> allPaths = new HashSet<>();
            Set<String> allChildren = Collections.emptySet();
            try (ScopeLog updatePaths = new ScopeLog(LOGGER, Level.FINE, getClass().getSimpleName() + "#fillAllLeaves")) { //$NON-NLS-1$
                if (input instanceof ITmfTreeViewerEntry) {
                    TreeUtil.addIf(allLeafEntries, (ITmfTreeViewerEntry) input, entry -> !entry.hasChildren());
                    for (ITmfTreeViewerEntry child : ((ITmfTreeViewerEntry) input).getChildren()) {
                        TreeUtil.add(allPaths, child);
                    }
                }
                allChildren = allLeafEntries.stream().map(entry -> TreeUtil.getPath(entry)).map(Collection<String>::toString).collect(Collectors.toSet());
            }
            Set<String> allLeaves = allChildren;
            Set<@NonNull ITmfTreeViewerEntry> newExpanded = new HashSet<>();
            try (ScopeLog updatePaths = new ScopeLog(LOGGER, Level.FINE, getClass().getSimpleName() + "#addAllNonCollapsed")) { //$NON-NLS-1$
                /*
                 * All the current nodes minus currently expanded are collapsed,
                 * so if an entry is included in the expanded list or it is NOT
                 * present in in the previous list, expand it. Basically
                 * collapse all that was previously collapsed.
                 *
                 * Also, make sure all new children are auto-expanded.
                 */
                TreeUtil.addIf(newExpanded, newInput, (ITmfTreeViewerEntry entry) -> {
                    String key = TreeUtil.getPath(entry).toString();
                    return (expandedPaths.contains(key) || !allPaths.contains(key)) || allLeaves.contains(key);
                });
            }
            try (ScopeLog updatePaths = new ScopeLog(LOGGER, Level.FINE, getClass().getSimpleName() + "#updateInput")) { //$NON-NLS-1$
                if (newInput != input) {
                    fTreeViewer.setInput(newInput);
                } else {
                    fTreeViewer.refresh();
                }
            }
            try (ScopeLog updatePaths = new ScopeLog(LOGGER, Level.FINE, getClass().getSimpleName() + "#contentChanged")) { //$NON-NLS-1$
                contentChanged(newInput);
            }
            try (ScopeLog updatePaths = new ScopeLog(LOGGER, Level.FINE, getClass().getSimpleName() + "#resetSelection")) { //$NON-NLS-1$
                /*
                 * Reset Selection
                 */
                if (!selection.isEmpty()) {
                    fTreeViewer.setSelection(selection, true);
                }
            }
            try (ScopeLog updatePaths = new ScopeLog(LOGGER, Level.FINE, getClass().getSimpleName() + "#expand")) { //$NON-NLS-1$
                /*
                 * Reset Expanded. This may be a slow operation, so only do it for smaller trees
                 */
                if (newExpanded.size() > THRESHOLD_TO_PRESERVE_TREE) {
                    fTreeViewer.expandAll();
                } else {
                    fTreeViewer.setExpandedElements(newExpanded.toArray());
                }

            }
        }
    }

    /**
     * Update the entries to the given start/end time. An extra parameter
     * defines whether these times correspond to the selection or the visible
     * range, as the viewer may update differently in those cases. This methods
     * returns a root node that is not meant to be visible. The children of this
     * 'fake' root node are the first level of entries that will appear in the
     * tree. If no update is necessary, the method should return
     * <code>null</code>. To empty the tree, a root node containing an empty
     * list of children should be returned.
     *
     * This method is not called in the UI thread when using the default viewer
     * content update. Resource-intensive calculations here should not block the
     * UI.
     *
     * @param trace
     *            The trace
     * @param start
     *            The start time of the requested content
     * @param end
     *            The end time of the requested content
     * @param isSelection
     *            <code>true</code> if this time range is for a selection,
     *            <code>false</code> for the visible time range
     * @return The root entry of the list of entries to display or
     *         <code>null</code> if no update necessary
     * @since 3.3
     */
    protected abstract ITmfTreeViewerEntry updateElements(@NonNull ITmfTrace trace, long start, long end, boolean isSelection);

    /**
     * Get the current input displayed by the viewer
     *
     * @return The input of the tree viewer, the root entry
     */
    protected ITmfTreeViewerEntry getInput() {
        return (ITmfTreeViewerEntry) fTreeViewer.getInput();
    }

    /**
     * Sets the auto-expand level to be used for the input of the viewer. The
     * value 0 means that there is no auto-expand; 1 means that top-level
     * elements are expanded, but not their children; 2 means that top-level
     * elements are expanded, and their children, but not grand-children; and so
     * on.
     * <p>
     * The value {@link AbstractTreeViewer#ALL_LEVELS} means that all subtrees
     * should be expanded.
     * </p>
     *
     * @param level
     *            non-negative level, or {@link AbstractTreeViewer#ALL_LEVELS}
     *            to expand all levels of the tree
     * @since 4.0
     */
    public void setAutoExpandLevel(int level) {
        if (fTreeViewer != null) {
            fTreeViewer.setAutoExpandLevel(level);
        }
    }

    // ------------------------------------------------------------------------
    // Signal Handler
    // ------------------------------------------------------------------------

    /**
     * Signal handler for handling of the window range signal. This time range
     * is the visible zone of the view.
     *
     * @param signal
     *            The {@link TmfWindowRangeUpdatedSignal}
     */
    @Override
    @TmfSignalHandler
    public void windowRangeUpdated(TmfWindowRangeUpdatedSignal signal) {
        super.windowRangeUpdated(signal);
        updateContent(this.getWindowStartTime(), this.getWindowEndTime(), false);
    }

    @Override
    @TmfSignalHandler
    public void traceClosed(@Nullable TmfTraceClosedSignal signal) {
        if (signal != null) {
            synchronized (fRoots) {
                fRoots.remove(signal.getTrace());
            }
        }
        super.traceClosed(signal);
    }

    @Override
    public void reset() {
        super.reset();
        clearContent();
    }

}
