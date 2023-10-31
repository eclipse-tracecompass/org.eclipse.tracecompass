/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.profiling.ui.weightedtree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.tracecompass.internal.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree.ITree;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree.IWeightedTreeProvider.DataType;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.tree.IWeightedTreeProvider.MetricType;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfStartAnalysisSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractTmfTreeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData.ITmfColumnPercentageProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;

/**
 * An abstract tree viewer implementation for displaying a weighted tree
 *
 * @author Geneviève Bastien
 */
public class WeightedTreeViewer extends AbstractTmfTreeViewer {

    // Order CCT children by decreasing length
    private static final Comparator<TreeNodeEntry> COMPARATOR = (o1, o2) -> Long.compare(o2.getTreeNode().getWeight(), o1.getTreeNode().getWeight());

    private MenuManager fTablePopupMenuManager;
    private MetricType fWeightType = new MetricType(String.valueOf(Messages.WeightedTreeViewer_Weight), DataType.NUMBER, null);
    private boolean fInitialized = false;
    private final WeightedTreeView fView;

    private static final String[] DEFAULT_COLUMN_NAMES = new String[] {
            Objects.requireNonNull(Messages.WeightedTreeViewer_Element),
            Objects.requireNonNull(Messages.WeightedTreeViewer_Weight)
    };

    /**
     * Constructor
     *
     * @param parent
     *            the parent composite
     * @param view
     *            The parent view
     */
    public WeightedTreeViewer(@Nullable Composite parent, WeightedTreeView view) {
        super(parent, false);
        setLabelProvider(new WeightedTreeLabelProvider(Collections.emptyList()));
        fView = view;
        fTablePopupMenuManager = new MenuManager();
        fTablePopupMenuManager.setRemoveAllWhenShown(true);
        fTablePopupMenuManager.addMenuListener(manager -> {
            TreeViewer viewer = getTreeViewer();
            ISelection selection = viewer.getSelection();
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection sel = (IStructuredSelection) selection;
                if (manager != null) {
                    appendToTablePopupMenu(manager, sel);
                }
            }
        });
        Menu tablePopup = fTablePopupMenuManager.createContextMenu(getTreeViewer().getTree());
        Tree tree = getTreeViewer().getTree();
        tree.setMenu(tablePopup);
        addSelectionChangeListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(@Nullable SelectionChangedEvent event) {
                if (event == null) {
                    return;
                }
                if (event.getSelection() instanceof IStructuredSelection) {
                    Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
                    if (selection instanceof TmfTreeViewerEntry) {
                        Set<WeightedTree<?>> trees = new HashSet<>();
                        IWeightedTreeProvider<?, ?, WeightedTree<?>> treeProvider = null;
                        if (selection instanceof TreeNodeEntry) {
                            treeProvider = ((TreeNodeEntry) selection).fTreeProvider;
                        }
                        for (ITmfTreeViewerEntry entry : ((TmfTreeViewerEntry) selection).getChildren()) {
                            // FIXME: Should we aggregate all children trees of
                            // all children elements?
                            if (!(entry instanceof TreeNodeEntry)) {
                                continue;
                            }
                            trees.add(((TreeNodeEntry) entry).getTreeNode());
                            treeProvider = ((TreeNodeEntry) entry).fTreeProvider;
                        }
                        // If there are no children, don't update current
                        // selection
                        if (treeProvider != null) {
                            fView.elementSelected(trees, treeProvider);
                        }
                    }
                }
            }
        });
    }

    /** Provides label for the Segment Store tree viewer cells */
    private class WeightedTreeLabelProvider extends TreeLabelProvider {
        private final Map<Integer, MetricType> fFormatMap;

        public WeightedTreeLabelProvider(List<MetricType> list) {
            // The additional metrics start after the default columns
            int metricIndex = DEFAULT_COLUMN_NAMES.length;
            fFormatMap = new HashMap<>();
            for (MetricType metric : list) {
                fFormatMap.put(metricIndex, metric);
                metricIndex++;
            }
        }

        @Override
        public String getColumnText(@Nullable Object element, int columnIndex) {
            String value = ""; //$NON-NLS-1$
            if (element instanceof HiddenTreeViewerEntry) {
                if (columnIndex == 0) {
                    value = ((HiddenTreeViewerEntry) element).getName();
                }
            } else if (element instanceof ElementEntry) {
                ElementEntry<?> entry = (ElementEntry<?>) element;
                if (columnIndex == 0) {
                    return String.valueOf(entry.getName());
                }
                value = StringUtils.EMPTY;
            } else if (element instanceof TreeNodeEntry) {
                TreeNodeEntry entry = (TreeNodeEntry) element;
                if (columnIndex == 0) {
                    return String.valueOf(entry.getName());
                }
                WeightedTree<?> callSite = entry.getTreeNode();
                if (columnIndex == 1) {
                    return String.valueOf(fWeightType.format(callSite.getWeight()));
                }
                MetricType metricType = fFormatMap.get(columnIndex);
                if (metricType != null) {
                    return metricType.format(entry.getMetric(columnIndex - DEFAULT_COLUMN_NAMES.length));
                }
            }
            return Objects.requireNonNull(value);
        }
    }

    private static class WeightedPercentageProvider implements ITmfColumnPercentageProvider {
        @Override
        public double getPercentage(@Nullable Object data) {
            double value = 0;
            if (data instanceof TreeNodeEntry) {
                TreeNodeEntry entry = (TreeNodeEntry) data;
                WeightedTree<?> callSite = entry.getTreeNode();

                // Find the total length from the parent
                ITmfTreeViewerEntry parentEntry = entry;
                while (parentEntry != null && !(parentEntry instanceof ElementEntry)) {
                    parentEntry = parentEntry.getParent();
                }
                if (parentEntry != null) {
                    value = (double) callSite.getWeight() / ((ElementEntry<?>) parentEntry).getTotalLength();
                }
            }
            return value;
        }
    }

    @Override
    protected ITmfTreeColumnDataProvider getColumnDataProvider() {
        return new ITmfTreeColumnDataProvider() {
            @Override
            public List<TmfTreeColumnData> getColumnData() {
                // TODO: Ideally, since we have the analysisID, we could get an
                // empty analysis class. The metric methods should be static and
                // we could build it right away with an empty instance
                return Collections.emptyList();
            }
        };
    }

    /**
     * Get the additional columns from the treeProvider. This method returns
     * null if there are no additional columns, so nothing to change from
     * default
     */
    private static ITmfTreeColumnDataProvider getColumnDataProvider(IWeightedTreeProvider<?, ?, WeightedTree<?>> treeProvider) {
        List<MetricType> additionalMetrics = treeProvider.getAdditionalMetrics();
        return new ITmfTreeColumnDataProvider() {
            @Override
            public List<@Nullable TmfTreeColumnData> getColumnData() {
                /* All columns are sortable */
                List<@Nullable TmfTreeColumnData> columns = new ArrayList<>(2 + additionalMetrics.size());
                // Add the default columns
                TmfTreeColumnData column = new TmfTreeColumnData(DEFAULT_COLUMN_NAMES[0]);
                column.setAlignment(SWT.LEFT);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((!(e1 instanceof TreeNodeEntry)) || (!(e2 instanceof TreeNodeEntry))) {
                            return 0;
                        }
                        TreeNodeEntry n1 = (TreeNodeEntry) e1;
                        TreeNodeEntry n2 = (TreeNodeEntry) e2;

                        return n1.getName().compareTo(n2.getName());
                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(treeProvider.getWeightType().getTitle());
                column.setPercentageProvider(new WeightedPercentageProvider());
                column.setAlignment(SWT.RIGHT);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((!(e1 instanceof TreeNodeEntry)) || (!(e2 instanceof TreeNodeEntry))) {
                            return 0;
                        }
                        TreeNodeEntry n1 = (TreeNodeEntry) e1;
                        TreeNodeEntry n2 = (TreeNodeEntry) e2;

                        WeightedTree<?> callsite1 = n1.getTreeNode();
                        WeightedTree<?> callsite2 = n2.getTreeNode();

                        return Long.compare(callsite1.getWeight(), callsite2.getWeight());
                    }
                });
                columns.add(column);
                // Create a column for each additional metric
                int metricIndex = 0;
                for (MetricType metric : additionalMetrics) {
                    column = new TmfTreeColumnData(metric.getTitle());
                    column.setAlignment(SWT.RIGHT);
                    int index = metricIndex;
                    switch (metric.getDataType()) {
                    case BINARY_SPEED:
                    case BYTES:
                    case NANOSECONDS:
                    case NUMBER:
                        // Add a number comparator
                        column.setComparator(new ViewerComparator() {
                            @Override
                            public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                                if ((!(e1 instanceof TreeNodeEntry)) || (!(e2 instanceof TreeNodeEntry))) {
                                    return 0;
                                }
                                Object metricValue1 = ((TreeNodeEntry) e1).getMetric(index);
                                Object metricValue2 = ((TreeNodeEntry) e2).getMetric(index);

                                if (metricValue1 instanceof Long && metricValue2 instanceof Long) {
                                    return Long.compare((Long) metricValue1, (Long) metricValue2);
                                }
                                if (metricValue1 instanceof Double && metricValue2 instanceof Double) {
                                    return Double.compare((Double) metricValue1, (Double) metricValue2);
                                }
                                if (metricValue1 instanceof Number && metricValue2 instanceof Number) {
                                    return Double.compare(((Number) metricValue1).doubleValue(), ((Number) metricValue2).doubleValue());
                                }
                                return (String.valueOf(metricValue1)).compareTo(String.valueOf(metricValue2));
                            }
                        });
                        break;
                    case OTHER:
                        // Add a string comparator
                        column.setComparator(new ViewerComparator() {
                            @Override
                            public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                                if ((!(e1 instanceof TreeNodeEntry)) || (!(e2 instanceof TreeNodeEntry))) {
                                    return 0;
                                }
                                Object metricValue1 = ((TreeNodeEntry) e1).getMetric(index);
                                Object metricValue2 = ((TreeNodeEntry) e2).getMetric(index);

                                return (String.valueOf(metricValue1)).compareTo(String.valueOf(metricValue2));
                            }
                        });
                        break;
                    default:
                        // No comparator
                    }
                    columns.add(column);
                    metricIndex++;
                }
                // Add a column for filler at the end
                column = new TmfTreeColumnData(""); //$NON-NLS-1$
                columns.add(column);
                return columns;
            }
        };
    }

    @Override
    public void initializeDataSource(ITmfTrace trace) {
        Set<IWeightedTreeProvider<?, ?, WeightedTree<?>>> modules = fView.getWeightedTrees(trace);

        modules.forEach(m -> {
            if (m instanceof IAnalysisModule) {
                ((IAnalysisModule) m).schedule();
            }
        });
        if (!modules.isEmpty() && !fInitialized) {
            initializeViewer(modules.iterator().next());
            fInitialized = true;
        }
    }

    /**
     * Listen to see if one of the view's analysis is restarted
     *
     * @param signal
     *            The analysis started signal
     */
    @SuppressWarnings("unlikely-arg-type")
    @TmfSignalHandler
    public void analysisStart(TmfStartAnalysisSignal signal) {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }
        Set<IWeightedTreeProvider<?, ?, WeightedTree<?>>> modules = fView.getWeightedTrees(trace);
        if (modules.contains(signal.getAnalysisModule())) {
            updateContent(trace.getStartTime().toNanos(), trace.getEndTime().toNanos(), false);
        }
    }

    /**
     * From a tree provider, initialize the viewer data/columns/label providers,
     * etc
     */
    private void initializeViewer(IWeightedTreeProvider<?, ?, WeightedTree<?>> treeProvider) {
        fWeightType = treeProvider.getWeightType();
        ITmfTreeColumnDataProvider columns = getColumnDataProvider(treeProvider);
        Display.getDefault().asyncExec(() -> {
            setTreeColumns(columns.getColumnData());
            setLabelProvider(new WeightedTreeLabelProvider(treeProvider.getAdditionalMetrics()));
        });
    }

    /**
     * Method to add commands to the context sensitive menu.
     *
     * @param manager
     *            the menu manager
     * @param sel
     *            the current selection
     */
    protected void appendToTablePopupMenu(IMenuManager manager, IStructuredSelection sel) {
        // Empty
    }

    /**
     * Class for defining an entry in the statistics tree.
     *
     * @param <E>
     *            The type of entry element
     */
    protected class ElementEntry<@NonNull E> extends TmfTreeViewerEntry {

        private final E fThisElement;
        private final IWeightedTreeProvider<?, E, WeightedTree<?>> fTreeProvider;
        private @Nullable List<ITmfTreeViewerEntry> fChildren;

        /**
         * Constructor
         *
         * @param element
         *            The tree to display under this element
         * @param provider
         *            The tree provider for this entry
         */
        public ElementEntry(E element, IWeightedTreeProvider<?, E, WeightedTree<?>> provider) {
            super(String.valueOf(element));
            fThisElement = element;
            fTreeProvider = provider;
        }

        /**
         * Gets the statistics object
         *
         * @return statistics object
         */
        public E getElement() {
            return fThisElement;
        }

        @Override
        public boolean hasChildren() {
            return true;
        }

        @Override
        public List<ITmfTreeViewerEntry> getChildren() {
            List<ITmfTreeViewerEntry> children = fChildren;
            if (children == null) {
                children = new ArrayList<>();
                Object thisNode = fThisElement;
                // Does this element have children?
                if (thisNode instanceof ITree) {
                    children.addAll(getChildrenElements((ITree) thisNode));
                }
                children.addAll(getChildrenTreeNodes());
                fChildren = children;
            }
            return children;
        }

        /**
         * Get the total length for the callsites children of this element. This
         * is used for percentages
         *
         * @return The total length of the children callsites
         */
        public long getTotalLength() {
            List<ITmfTreeViewerEntry> childrenCallSites = getChildren();
            long length = 0L;
            for (ITmfTreeViewerEntry callsiteEntry : childrenCallSites) {
                length += ((TreeNodeEntry) callsiteEntry).getTreeNode().getWeight();
            }
            return length;
        }

        private List<ITmfTreeViewerEntry> getChildrenTreeNodes() {
            List<ITmfTreeViewerEntry> list = new ArrayList<>();
            for (WeightedTree<?> callsite : fTreeProvider.getTreeSet().getTreesFor(fThisElement)) {
                list.add(new TreeNodeEntry(callsite, this, fTreeProvider));
            }
            return list;
        }

        @SuppressWarnings("unchecked")
        private List<ITmfTreeViewerEntry> getChildrenElements(ITree thisNode) {
            List<ITmfTreeViewerEntry> list = new ArrayList<>();
            for (ITree elChild : thisNode.getChildren()) {
                list.add(new ElementEntry<>((E) elChild, fTreeProvider));
            }
            return list;
        }
    }

    /**
     * Class for defining an entry in the statistics tree.
     */
    protected static class TreeNodeEntry extends TmfTreeViewerEntry {

        private final WeightedTree<?> fTreeNode;
        private final IWeightedTreeProvider<?, ?, WeightedTree<?>> fTreeProvider;
        private @Nullable List<ITmfTreeViewerEntry> fChildren = null;

        /**
         * Constructor
         *
         * @param callsite
         *            The callsite corresponding to this entry
         * @param parent
         *            The parent element
         * @param treeProvider
         *            The tree provider
         */
        public TreeNodeEntry(WeightedTree<?> callsite, TmfTreeViewerEntry parent, IWeightedTreeProvider<?, ?, WeightedTree<?>> treeProvider) {
            super(treeProvider.toDisplayString(callsite));
            fTreeNode = callsite;
            this.setParent(parent);
            fTreeProvider = treeProvider;
        }

        /**
         * Gets the statistics object
         *
         * @return statistics object
         */
        public WeightedTree<?> getTreeNode() {
            return fTreeNode;
        }

        @Override
        public boolean hasChildren() {
            return !fTreeNode.getChildren().isEmpty();
        }

        /**
         * Get the corresponding metric for this node
         *
         * @param metricIndex
         *            The index of the metric to get
         * @return The metric for this tree node
         */
        public Object getMetric(int metricIndex) {
            return fTreeProvider.getAdditionalMetric(fTreeNode, metricIndex);
        }

        @Override
        public List<ITmfTreeViewerEntry> getChildren() {
            List<ITmfTreeViewerEntry> children = fChildren;
            if (children == null) {
                List<TreeNodeEntry> cctChildren = new ArrayList<>();
                for (WeightedTree<?> callsite : fTreeNode.getChildren()) {
                    TreeNodeEntry entry = new TreeNodeEntry(callsite, this, fTreeProvider);
                    int index = Collections.binarySearch(cctChildren, entry, COMPARATOR);
                    cctChildren.add((index < 0 ? -index - 1 : index), entry);
                }
                children = new ArrayList<>(cctChildren);
                fChildren = children;
            }
            return children;
        }
    }

    @Override
    protected @Nullable ITmfTreeViewerEntry updateElements(ITmfTrace trace, long start, long end, boolean isSelection) {
        Set<IWeightedTreeProvider<?, ?, WeightedTree<?>>> modules = fView.getWeightedTrees(trace);

        if (isSelection || modules.isEmpty()) {
            return null;
        }
        modules.forEach(m -> {
            if (m instanceof IAnalysisModule) {
                ((IAnalysisModule) m).waitForCompletion();
            }
        });

        TmfTreeViewerEntry root = new TmfTreeViewerEntry(""); //$NON-NLS-1$
        List<ITmfTreeViewerEntry> entryList = root.getChildren();

        for (IWeightedTreeProvider<?, ?, WeightedTree<?>> module : modules) {
            setGlobalData(entryList, module);
        }
        return root;
    }

    private <@NonNull E> void setGlobalData(List<ITmfTreeViewerEntry> entryList, IWeightedTreeProvider<?, E, WeightedTree<?>> module) {
        Collection<E> elements = module.getTreeSet().getElements();

        for (E element : elements) {
            ElementEntry<E> entry = new ElementEntry<>(element, module);
            entryList.add(entry);
        }
    }

    @Override
    @TmfSignalHandler
    public void windowRangeUpdated(@Nullable TmfWindowRangeUpdatedSignal signal) {
        // Do nothing. We do not want to update the view and lose the selection
        // if the window range is updated with current selection outside of this
        // new range.
    }

    /**
     * Get the total column label
     *
     * @return the totals column label
     */
    protected String getTotalLabel() {
        return Objects.requireNonNull(Messages.WeightedTreeViewer_LabelTotal);
    }

    /**
     * Get the selection column label
     *
     * @return The selection column label
     */
    protected String getSelectionLabel() {
        return Objects.requireNonNull(Messages.WeightedTreeViewer_LabelSelection);
    }

    /**
     * Class to define a level in the tree that doesn't have any values.
     */
    protected static class HiddenTreeViewerEntry extends TmfTreeViewerEntry {
        /**
         * Constructor
         *
         * @param name
         *            the name of the level
         */
        public HiddenTreeViewerEntry(String name) {
            super(name);
        }
    }

    @Override
    protected void setSelectionRange(long selectionBeginTime, long selectionEndTime) {
        super.setSelectionRange(selectionBeginTime, selectionEndTime);
        updateContent(selectionBeginTime, selectionEndTime, true);
    }
}
