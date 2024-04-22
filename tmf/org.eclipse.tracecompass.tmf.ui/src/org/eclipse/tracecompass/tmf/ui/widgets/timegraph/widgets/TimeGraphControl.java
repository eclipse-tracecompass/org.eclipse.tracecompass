/*****************************************************************************
 * Copyright (c) 2007, 2022 Intel Corporation and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Intel Corporation - Initial API and implementation
 *   Ruslan A. Scherbakov, Intel - Initial API and implementation
 *   Alvaro Sanchez-Leon, Ericsson - Updated for TMF
 *   Patrick Tasse, Ericsson - Refactoring
 *   Geneviève Bastien, École Polytechnique de Montréal - Move code to
 *                            provide base classes for time graph view
 *                            Add display of links between items
 *   Xavier Raynaud, Kalray - Code optimization
 *   Generoso Pagano, Inria - Support for drag selection listeners
 *****************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
import org.eclipse.tracecompass.common.core.math.SaturatedArithmetic;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.model.IStylePresentationProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.ITimeGraphStylePresentationProvider;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfUIPreferences;
import org.eclipse.tracecompass.internal.tmf.ui.util.LineClipper;
import org.eclipse.tracecompass.internal.tmf.ui.util.SymbolHelper;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.TimeGraphRender;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.TimeGraphRender.DeferredEntry;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.TimeGraphRender.DeferredItem;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.TimeGraphRender.DeferredLine;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.TimeGraphRender.DeferredSegment;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.TimeGraphRender.DeferredState;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.TimeGraphRender.DeferredTinyState;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.TimeGraphRender.DeferredTransparentState;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.TimeGraphRender.LongPoint;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.TimeGraphRender.PostDrawEvent;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.model.TimeGraphLineEntry;
import org.eclipse.tracecompass.internal.tmf.ui.widgets.timegraph.model.TimeLineEvent;
import org.eclipse.tracecompass.tmf.core.model.CoreFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties.BorderStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties.SymbolType;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties.VerticalAlign;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.ui.colors.RGBAUtil;
import org.eclipse.tracecompass.tmf.ui.model.StyleManager;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentSignal;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils.Resolution;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils.TimeFormat;
import org.eclipse.tracecompass.tmf.ui.views.ITmfTimeAligned;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphColorListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphTimeListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphTreeListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphViewerFilterListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphTreeExpansionEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

import com.google.common.collect.Iterables;

/**
 * Time graph control implementation
 *
 * @author Alvaro Sanchez-Leon
 * @author Patrick Tasse
 */
public class TimeGraphControl extends TimeGraphBaseControl
        implements FocusListener, KeyListener, MouseMoveListener, MouseListener,
        MouseWheelListener, MouseTrackListener, TraverseListener, ISelectionProvider,
        MenuDetectListener, ITmfTimeGraphDrawingHelper, ITimeGraphColorListener, Listener {

    /**
     * Default state width ratio
     *
     * @since 4.1
     */
    public static final float DEFAULT_STATE_WIDTH = 1.0f;

    /**
     * Default link width ratio
     *
     * @since 4.1
     */
    public static final float DEFAULT_LINK_WIDTH = 0.1f;

    /**
     * Constant indicating that all levels of the time graph should be expanded
     */
    public static final int ALL_LEVELS = AbstractTreeViewer.ALL_LEVELS;

    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(TimeGraphControl.class);

    private static final @NonNull String HIGHLIGHTED_BOUND_COLOR = "#ff3300"; //$NON-NLS-1$
    private static final @NonNull RGBAColor BLACK = new RGBAColor(0, 0, 0, 255);
    private static final int OPAQUE = 255;
    private static final int HIGHLIGHTED_BOUND_WIDTH = 4;
    private static final int DRAG_MARGIN = 5;

    private static final int DRAG_NONE = 0;
    private static final int DRAG_TRACE_ITEM = 1;
    private static final int DRAG_SPLIT_LINE = 2;
    private static final int DRAG_ZOOM = 3;
    private static final int DRAG_SELECTION = 4;

    /**
     * Get item height from provider
     */
    private static final int CUSTOM_ITEM_HEIGHT = -1;
    private static final int MIN_MIDLINE_HEIGHT = 3;

    private static final double ZOOM_FACTOR = 1.5;
    private static final double ZOOM_IN_FACTOR = 0.8;
    private static final double ZOOM_OUT_FACTOR = 1.25;

    private static final int SNAP_WIDTH = 3;
    private static final int ARROW_HOVER_MAX_DIST = 5;

    /**
     * base to height ratio
     */
    private static final double ARROW_RATIO = Math.sqrt(3) / 2;
    private static final int NO_STATUS = -1;
    private static final int STATUS_WITHOUT_CURSOR_TIME = -2;

    private static final int MAX_LABEL_LENGTH = 256;

    private static final int VERTICAL_ZOOM_DELAY = 400;

    private static final String PREFERRED_WIDTH = "width"; //$NON-NLS-1$

    /**
     * The alpha color component value for dimmed events
     *
     * @since 4.0
     */
    private static final int DIMMED_ALPHA_COEFFICIENT = 4;

    /** Resource manager */
    private LocalResourceManager fResourceManager = new LocalResourceManager(JFaceResources.getResources());

    /** Color map for event types */
    private Color[] fEventColorMap = null;

    private ITimeDataProvider fTimeProvider;
    private ITableLabelProvider fLabelProvider;
    private IStatusLineManager fStatusLineManager = null;
    private Tree fTree = null;
    private TimeGraphScale fTimeGraphScale = null;

    private boolean fIsInFocus = false;
    private boolean fMouseOverSplitLine = false;
    private int fGlobalItemHeight = CUSTOM_ITEM_HEIGHT;
    private int fHeightAdjustment = 0;
    private int fMaxItemHeight = 0;
    private boolean fBlendSubPixelEvents = false;
    private int fMinimumItemWidth = 0;
    private int fTopIndex = 0;
    private int fDragState = DRAG_NONE;
    private boolean fDragBeginMarker = false;
    private int fDragButton;
    private int fDragX0 = 0;
    private int fDragX = 0;
    private ITimeGraphEntry fDragEntry = null;
    private boolean fHasNamespaceFocus = false;
    /**
     * Used to preserve accuracy of modified selection
     */
    private long fDragTime0 = 0;
    private int fIdealNameSpace = 0;
    private boolean fAutoResizeColumns = true;
    private long fTime0bak;
    private long fTime1bak;
    private ITimeGraphPresentationProvider fTimeGraphProvider = null;
    private ItemData fItemData = null;
    private List<IMarkerEvent> fMarkers = null;
    private boolean fMarkersVisible = true;
    private List<SelectionListener> fSelectionListeners;
    private List<ITimeGraphTimeListener> fDragSelectionListeners;
    private final List<ISelectionChangedListener> fSelectionChangedListeners = new ArrayList<>();
    private final List<ITimeGraphTreeListener> fTreeListeners = new ArrayList<>();
    private final List<ITimeGraphViewerFilterListener> fViewerFilterListeners = new ArrayList<>();
    private final List<MenuDetectListener> fTimeGraphEntryMenuListeners = new ArrayList<>();
    private final List<MenuDetectListener> fTimeEventMenuListeners = new ArrayList<>();
    private final Cursor fDragCursor = Display.getDefault().getSystemCursor(SWT.CURSOR_HAND);
    private final Cursor fResizeCursor = Display.getDefault().getSystemCursor(SWT.CURSOR_CROSS);
    private final Cursor fWaitCursor = Display.getDefault().getSystemCursor(SWT.CURSOR_WAIT);
    private final Cursor fZoomCursor = Display.getDefault().getSystemCursor(SWT.CURSOR_SIZEWE);
    private final Set<@NonNull ViewerFilter> fFilters = new LinkedHashSet<>();
    private MenuDetectEvent fPendingMenuDetectEvent = null;
    private boolean fGridLinesVisible = true;
    private final Color fGray = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
    private final @NonNull RGBAColor fGrayColor = new RGBAColor((RGBAUtil.fromRGBA(fGray.getRGBA())));
    private final @NonNull RGBAColor fTransparentGrayColor = new RGBAColor(fGrayColor.getRed(), fGrayColor.getGreen(), fGrayColor.getBlue(), fGrayColor.getAlpha() / 4);
    private Color fGridLineColor = fGray;
    private boolean fLabelsVisible = true;
    private boolean fMidLinesVisible = true;
    private boolean fHideArrows = false;
    private int fAutoExpandLevel = ALL_LEVELS;
    private Entry<ITimeGraphEntry, Integer> fVerticalZoomAlignEntry = null;
    private long fVerticalZoomAlignTime = 0;
    private int fBorderWidth = 0;
    private int fHeaderHeight = 0;
    private int fLastTransparentX = -1;

    private boolean fFilterActive;
    private boolean fHasSavedFilters;
    private boolean fHideEmptyRowsFilterActive;

    private List<DeferredEntry> fPostDrawEntries = new ArrayList<>();

    private List<PostDrawEvent> fPostDrawArrows = new ArrayList<>();

    private List<DeferredSegment> fPoints = new ArrayList<>();

    private List<DeferredLine> fLines = new ArrayList<>();

    private List<Rectangle> fSelectedRectangles = new ArrayList<>();

    private @Nullable Point fSize = null;
    private @Nullable Rectangle fBounds = null;

    private final @NonNull String fPaintScopeLabel = getClass().getCanonicalName() + "#paint"; //$NON-NLS-1$
    private final @NonNull String fBackgroundScopeLabel = getClass().getCanonicalName() + "#drawBackground"; //$NON-NLS-1$
    private final @NonNull String fGridLinesScopeLabel = getClass().getCanonicalName() + "#drawGridlines"; //$NON-NLS-1$
    private final @NonNull String fBgmScopeLabel = getClass().getCanonicalName() + "#drawBgMarkers"; //$NON-NLS-1$
    private final @NonNull String fItemsScopeLabel = getClass().getCanonicalName() + "#drawItems"; //$NON-NLS-1$
    private final @NonNull String fLinksScopeLabel = getClass().getCanonicalName() + "#drawLinks"; //$NON-NLS-1$
    private final @NonNull String fMarkersScopeLabel = getClass().getCanonicalName() + "#drawMarkers"; //$NON-NLS-1$
    private final @NonNull String fDrawItemsCountLabel = fItemsScopeLabel + "#count"; //$NON-NLS-1$
    private final @NonNull String fDrawMarkersCountLabel = fMarkersScopeLabel + "#count"; //$NON-NLS-1$
    private final @NonNull String fDrawLinksCountLabel = fLinksScopeLabel + "#count"; //$NON-NLS-1$

    private DeferredEntry fCurrentDeferredEntry;

    /**
     * Standard constructor
     *
     * @param parent
     *            The parent composite object
     * @param colors
     *            The color scheme to use
     */
    public TimeGraphControl(Composite parent, TimeGraphColorScheme colors) {

        super(parent, colors, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);

        fItemData = new ItemData();

        addControlListener(new TimeGraphControlListener());
        addFocusListener(this);
        addMouseListener(this);
        addMouseMoveListener(this);
        addMouseTrackListener(this);
        addMouseWheelListener(this);
        addTraverseListener(this);
        addKeyListener(this);
        addMenuDetectListener(this);
        addListener(SWT.MouseWheel, this);
        addDisposeListener(e -> fResourceManager.dispose());
    }

    /**
     * Sets the timegraph provider used by this timegraph viewer.
     *
     * @param timeGraphProvider
     *            the timegraph provider
     */
    public void setTimeGraphProvider(ITimeGraphPresentationProvider timeGraphProvider) {
        fTimeGraphProvider = timeGraphProvider;

        timeGraphProvider.setDrawingHelper(this);
        timeGraphProvider.addColorListener(this);

        StateItem[] stateItems = fTimeGraphProvider.getStateTable();
        colorSettingsChanged(stateItems);
    }

    /**
     * Gets the timegraph provider used by this timegraph viewer.
     *
     * @return the timegraph provider, or <code>null</code> if not set.
     */
    public ITimeGraphPresentationProvider getTimeGraphProvider() {
        return fTimeGraphProvider;
    }

    /**
     * Gets the time data provider used by this viewer.
     *
     * @return The time data provider, or <code>null</code> if not set
     * @since 2.1
     */
    public ITimeDataProvider getTimeDataProvider() {
        return fTimeProvider;
    }

    /**
     * Gets the color map used by this timegraph viewer.
     *
     * @return a color map, or <code>null</code> if not set.
     */
    public Color[] getEventColorMap() {
        return fEventColorMap;
    }

    /**
     * Assign the given time provider
     *
     * @param timeProvider
     *            The time provider
     */
    public void setTimeProvider(ITimeDataProvider timeProvider) {
        fTimeProvider = timeProvider;
        redraw();
    }

    /**
     * Set the label provider for the name space
     *
     * @param labelProvider
     *            The label provider
     * @since 2.3
     */
    public void setLabelProvider(ITableLabelProvider labelProvider) {
        fLabelProvider = labelProvider;
        redraw();
    }

    /**
     * Get the label provider for the name space
     *
     * @return The label provider
     * @since 2.3
     */
    public ITableLabelProvider getLabelProvider() {
        return fLabelProvider;
    }

    /**
     * Assign the status line manager
     *
     * @param statusLineManager
     *            The status line manager, or null to disable status line
     *            messages
     */
    public void setStatusLineManager(IStatusLineManager statusLineManager) {
        if (fStatusLineManager != null && statusLineManager == null) {
            fStatusLineManager.setMessage(""); //$NON-NLS-1$
        }
        fStatusLineManager = statusLineManager;
    }

    /**
     * Assign the tree that represents the name space header
     *
     * @param tree
     *            The tree
     * @since 2.3
     */
    public void setTree(Tree tree) {
        fTree = tree;
    }

    /**
     * Returns the tree control associated with this time graph control. The
     * tree is only used for column handling of the name space and contains no
     * tree items.
     *
     * @return the tree control
     * @since 2.3
     */
    public Tree getTree() {
        return fTree;
    }

    /**
     * Sets the columns for this time graph control's name space.
     *
     * @param columnNames
     *            the column names
     * @since 2.3
     */
    public void setColumns(String[] columnNames) {
        Tree tree = getTree();
        for (TreeColumn column : tree.getColumns()) {
            column.dispose();
        }
        ControlListener controlListener = new ControlListener() {
            @Override
            public void controlResized(ControlEvent e) {
                if (fAutoResizeColumns && ((TreeColumn) e.widget).getWidth() < (Integer) e.widget.getData(PREFERRED_WIDTH)) {
                    fAutoResizeColumns = false;
                }
                redraw();
            }

            @Override
            public void controlMoved(ControlEvent e) {
                redraw();
            }
        };
        for (String columnName : columnNames) {
            TreeColumn column = new TreeColumn(tree, SWT.LEFT);
            column.setMoveable(true);
            column.setText(columnName);
            column.pack();
            column.setData(PREFERRED_WIDTH, column.getWidth());
            column.addControlListener(controlListener);
        }
    }

    /**
     * Assign the time graph scale
     *
     * @param timeGraphScale
     *            The time graph scale
     */
    public void setTimeGraphScale(TimeGraphScale timeGraphScale) {
        fTimeGraphScale = timeGraphScale;
    }

    /**
     * Add a selection listener
     *
     * @param listener
     *            The listener to add
     */
    public void addSelectionListener(SelectionListener listener) {
        if (listener == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        if (null == fSelectionListeners) {
            fSelectionListeners = new ArrayList<>();
        }
        fSelectionListeners.add(listener);
    }

    /**
     * Remove a selection listener
     *
     * @param listener
     *            The listener to remove
     */
    public void removeSelectionListener(SelectionListener listener) {
        if (null != fSelectionListeners) {
            fSelectionListeners.remove(listener);
        }
    }

    /**
     * Selection changed callback
     */
    public void fireSelectionChanged() {
        if (null != fSelectionListeners) {
            Iterator<SelectionListener> it = fSelectionListeners.iterator();
            while (it.hasNext()) {
                SelectionListener listener = it.next();
                listener.widgetSelected(null);
            }
        }

        for (ISelectionChangedListener listener : fSelectionChangedListeners) {
            listener.selectionChanged(new SelectionChangedEvent(this, getSelection()));
        }
    }

    /**
     * Default selection callback
     */
    public void fireDefaultSelection() {
        if (null != fSelectionListeners) {
            Iterator<SelectionListener> it = fSelectionListeners.iterator();
            while (it.hasNext()) {
                SelectionListener listener = it.next();
                listener.widgetDefaultSelected(null);
            }
        }
    }

    /**
     * Add a drag selection listener
     *
     * @param listener
     *            The listener to add
     */
    public void addDragSelectionListener(ITimeGraphTimeListener listener) {
        if (listener == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        if (null == fDragSelectionListeners) {
            fDragSelectionListeners = new ArrayList<>();
        }
        fDragSelectionListeners.add(listener);
    }

    /**
     * Remove a drag selection listener
     *
     * @param listener
     *            The listener to remove
     */
    public void removeDragSelectionListener(ITimeGraphTimeListener listener) {
        if (null != fDragSelectionListeners) {
            fDragSelectionListeners.remove(listener);
        }
    }

    /**
     * Drag Selection changed callback
     *
     * @param start
     *            Time interval start
     * @param end
     *            Time interval end
     */
    public void fireDragSelectionChanged(long start, long end) {
        // check for backward intervals
        long beginTime;
        long endTime;
        if (start > end) {
            beginTime = end;
            endTime = start;
        } else {
            beginTime = start;
            endTime = end;
        }
        // call the listeners
        if (null != fDragSelectionListeners) {
            Iterator<ITimeGraphTimeListener> it = fDragSelectionListeners.iterator();
            while (it.hasNext()) {
                ITimeGraphTimeListener listener = it.next();
                listener.timeSelected(new TimeGraphTimeEvent(this, beginTime, endTime));
            }
        }
    }

    /**
     * Get the traces in the model
     *
     * @return The array of traces
     */
    public ITimeGraphEntry[] getTraces() {
        return fItemData.getEntries();
    }

    /**
     * Refresh the data for the thing
     */
    public void refreshData() {
        fItemData.refreshData();
        redraw();
    }

    /**
     * Refresh data for the given traces
     *
     * @param traces
     *            The traces to refresh
     */
    public void refreshData(ITimeGraphEntry[] traces) {
        fItemData.refreshData(traces);
        redraw();
    }

    /**
     * Refresh the links (arrows) of this widget
     *
     * @param events
     *            The link event list
     */
    public void refreshArrows(List<ILinkEvent> events) {
        fItemData.refreshArrows(events);
    }

    /**
     * Get the links (arrows) of this widget
     *
     * @return The unmodifiable link event list
     *
     * @since 1.1
     */
    public List<ILinkEvent> getArrows() {
        return Collections.unmodifiableList(fItemData.fLinks);
    }

    boolean ensureVisibleItem(int idx, boolean redraw) {
        boolean changed = false;
        int index = idx;
        if (index < 0) {
            for (index = 0; index < fItemData.fExpandedItems.length; index++) {
                if (fItemData.fExpandedItems[index].fSelected) {
                    break;
                }
            }
        }
        if (index >= fItemData.fExpandedItems.length) {
            return changed;
        }
        if (index < fTopIndex) {
            setTopIndex(index);
            if (redraw) {
                redraw();
            }
            changed = true;
        } else {
            int page = countPerPage();
            if (index >= fTopIndex + page) {
                setTopIndex(index - page + 1);
                if (redraw) {
                    redraw();
                }
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Assign the given index as the top one
     *
     * @param idx
     *            The index
     */
    public void setTopIndex(int idx) {
        int index = Math.min(idx, fItemData.fExpandedItems.length - countPerPage());
        index = Math.max(0, index);
        fTopIndex = index;
        redraw();
    }

    /**
     * Set the top index so that the requested element is at the specified
     * position.
     *
     * @param entry
     *            the time graph entry to be positioned
     * @param y
     *            the requested y-coordinate
     * @since 2.0
     */
    public void setElementPosition(ITimeGraphEntry entry, int y) {
        Item item = fItemData.fItemMap.get(entry);
        if (item == null || item.fExpandedIndex == -1) {
            return;
        }
        int index = item.fExpandedIndex;
        Rectangle itemRect = getItemRect(getClientArea(), index);
        int delta = itemRect.y + itemRect.height - y;
        int topIndex = getItemIndexAtY(delta);
        if (topIndex != -1) {
            setTopIndex(topIndex);
        } else {
            if (delta < 0) {
                setTopIndex(0);
            } else {
                setTopIndex(getExpandedElementCount());
            }
        }
    }

    /**
     * Sets the auto-expand level to be used for new entries discovered when
     * calling {@link #refreshData()} or
     * {@link #refreshData(ITimeGraphEntry[])}. The value 0 means that there is
     * no auto-expand; 1 means that top-level entries are expanded, but not
     * their children; 2 means that top-level entries are expanded, and their
     * children, but not grand-children; and so on.
     * <p>
     * The value {@link #ALL_LEVELS} means that all subtrees should be expanded.
     * </p>
     *
     * @param level
     *            non-negative level, or <code>ALL_LEVELS</code> to expand all
     *            levels of the tree
     */
    public void setAutoExpandLevel(int level) {
        fAutoExpandLevel = level;
    }

    /**
     * Returns the auto-expand level.
     *
     * @return non-negative level, or <code>ALL_LEVELS</code> if all levels of
     *         the tree are expanded automatically
     * @see #setAutoExpandLevel
     */
    public int getAutoExpandLevel() {
        return fAutoExpandLevel;
    }

    /**
     * Get the expanded state of a given entry.
     *
     * @param entry
     *            The entry
     * @return true if the entry is expanded, false if collapsed
     * @since 1.1
     */
    public boolean getExpandedState(ITimeGraphEntry entry) {
        Item item = fItemData.fItemMap.get(entry);
        return (item != null ? item.fExpanded : false);
    }

    /**
     * Set the expanded state of a given entry
     *
     * @param entry
     *            The entry
     * @param expanded
     *            True if expanded, false if collapsed
     */
    public void setExpandedState(ITimeGraphEntry entry, boolean expanded) {
        Item item = fItemData.findItem(entry);
        if (item != null && item.fExpanded != expanded) {
            item.fExpanded = expanded;
            fItemData.updateExpandedItems();
            redraw();
        }
    }

    /**
     * Set the expanded state of a given list of entries
     *
     * @param entries
     *            The list of entries
     * @param expanded
     *            True if expanded, false if collapsed
     * @since 3.1
     */
    public void setExpandedState(Iterable<ITimeGraphEntry> entries, boolean expanded) {
        for (ITimeGraphEntry entry : entries) {
            Item item = fItemData.findItem(entry);
            if (item != null) {
                item.fExpanded = expanded;
            }
        }
        fItemData.updateExpandedItems();
        redraw();
    }

    /**
     * Set the expanded state of a given entry to certain relative level. It
     * will call fireTreeEvent() for each changed entry. At the end it will call
     * redraw().
     *
     * @param entry
     *            The entry
     * @param level
     *            level to expand to or negative for all levels
     * @param expanded
     *            True if expanded, false if collapsed
     */
    private void setExpandedState(ITimeGraphEntry entry, int level, boolean expanded) {
        setExpandedStateInt(entry, level, expanded);
        redraw();
    }

    /**
     * Set the expanded state of a given entry and its children to the first
     * level that has one collapsed entry.
     *
     * @param entry
     *            The entry
     */
    private void setExpandedStateLevel(ITimeGraphEntry entry) {
        int level = findExpandedLevel(entry);
        if (level >= 0) {
            setExpandedStateInt(entry, level, true);
            redraw();
        }
    }

    /*
     * Inner class for finding relative level with at least one collapsed entry.
     */
    private class SearchNode {
        SearchNode(ITimeGraphEntry e, int l) {
            entry = e;
            level = l;
        }

        ITimeGraphEntry entry;
        int level;
    }

    /**
     * Finds the relative level with at least one collapsed entry.
     *
     * @param entry
     *            the start entry
     * @return the found level or -1 if all levels are already expanded.
     */
    private int findExpandedLevel(ITimeGraphEntry entry) {
        Queue<SearchNode> queue = new LinkedList<>();
        SearchNode root = new SearchNode(entry, 0);
        queue.add(root);

        while (!queue.isEmpty()) {
            SearchNode node = queue.remove();
            if (node.entry.hasChildren() && !getExpandedState(node.entry)) {
                return node.level;
            }
            for (ITimeGraphEntry e : node.entry.getChildren()) {
                if (e.hasChildren()) {
                    SearchNode n = new SearchNode(e, node.level + 1);
                    queue.add(n);
                }
            }
        }
        return -1;
    }

    /**
     * Set the expanded state of a given entry to certain relative level. It
     * will call fireTreeEvent() for each changed entry. No redraw is done.
     *
     * @param entry
     *            The entry
     * @param level
     *            level to expand to or negative for all levels
     * @param expanded
     *            True if expanded, false if collapsed
     */
    private void setExpandedStateInt(ITimeGraphEntry entry, int aLevel, boolean expanded) {
        int level = aLevel;
        if ((level > 0) || (level < 0)) {
            level--;
            if (entry.hasChildren()) {
                for (ITimeGraphEntry e : entry.getChildren()) {
                    setExpandedStateInt(e, level, expanded);
                }
            }
        }
        Item item = fItemData.findItem(entry);
        if (item != null && item.fExpanded != expanded) {
            item.fExpanded = expanded;
            fItemData.updateExpandedItems();
            fireTreeEvent(item.fEntry, item.fExpanded);
        }
    }

    /**
     * Collapses all nodes of the viewer's tree, starting with the root.
     */
    public void collapseAll() {
        for (Item item : fItemData.fItems) {
            item.fExpanded = false;
        }
        fItemData.updateExpandedItems();
        redraw();
    }

    /**
     * Expands all nodes of the viewer's tree, starting with the root.
     */
    public void expandAll() {
        for (Item item : fItemData.fItems) {
            item.fExpanded = true;
        }
        fItemData.updateExpandedItems();
        redraw();
    }

    /**
     * Add a tree listener
     *
     * @param listener
     *            The listener to add
     */
    public void addTreeListener(ITimeGraphTreeListener listener) {
        if (!fTreeListeners.contains(listener)) {
            fTreeListeners.add(listener);
        }
    }

    /**
     * Remove a tree listener
     *
     * @param listener
     *            The listener to remove
     */
    public void removeTreeListener(ITimeGraphTreeListener listener) {
        if (fTreeListeners.contains(listener)) {
            fTreeListeners.remove(listener);
        }
    }

    /**
     * Tree event callback
     *
     * @param entry
     *            The affected entry
     * @param expanded
     *            The expanded state (true for expanded, false for collapsed)
     */
    public void fireTreeEvent(ITimeGraphEntry entry, boolean expanded) {
        TimeGraphTreeExpansionEvent event = new TimeGraphTreeExpansionEvent(this, entry);
        for (ITimeGraphTreeListener listener : fTreeListeners) {
            if (expanded) {
                listener.treeExpanded(event);
            } else {
                listener.treeCollapsed(event);
            }
        }
    }

    /**
     * Add a viewer filter listener
     *
     * @param listener
     *            The listener to add
     * @since 3.2
     */
    public void addViewerFilterListener(ITimeGraphViewerFilterListener listener) {
        if (!fViewerFilterListeners.contains(listener)) {
            fViewerFilterListeners.add(listener);
        }
    }

    /**
     * Remove a viewer filter listener
     *
     * @param listener
     *            The listener to remove
     * @since 3.2
     */
    public void removeViewerFilterListener(ITimeGraphViewerFilterListener listener) {
        if (fViewerFilterListeners.contains(listener)) {
            fViewerFilterListeners.remove(listener);
        }
    }

    /**
     * Viewer filter added callback
     *
     * @param filters
     *            The added filters
     *
     * @since 3.1
     */
    private void fireFiltersAdded(@NonNull Iterable<ViewerFilter> filters) {
        for (ITimeGraphViewerFilterListener listener : fViewerFilterListeners) {
            listener.filtersAdded(filters);
        }
    }

    /**
     * Viewer filter changed callback
     *
     * @param filters
     *            The changed filters
     *
     * @since 3.1
     */
    private void fireFiltersChanged(@NonNull Iterable<ViewerFilter> filters) {
        for (ITimeGraphViewerFilterListener listener : fViewerFilterListeners) {
            listener.filtersChanged(filters);
        }
    }

    /**
     * Viewer filter removed callback
     *
     * @param filters
     *            The removed filters
     *
     * @since 3.1
     */
    private void fireFiltersRemoved(@NonNull Iterable<ViewerFilter> filters) {
        for (ITimeGraphViewerFilterListener listener : fViewerFilterListeners) {
            listener.filtersRemoved(filters);
        }
    }

    /**
     * Add a menu listener on {@link ITimeGraphEntry}s
     *
     * @param listener
     *            The listener to add
     */
    public void addTimeGraphEntryMenuListener(MenuDetectListener listener) {
        if (!fTimeGraphEntryMenuListeners.contains(listener)) {
            fTimeGraphEntryMenuListeners.add(listener);
        }
    }

    /**
     * Remove a menu listener on {@link ITimeGraphEntry}s
     *
     * @param listener
     *            The listener to remove
     */
    public void removeTimeGraphEntryMenuListener(MenuDetectListener listener) {
        if (fTimeGraphEntryMenuListeners.contains(listener)) {
            fTimeGraphEntryMenuListeners.remove(listener);
        }
    }

    /**
     * Menu event callback on {@link ITimeGraphEntry}s
     *
     * @param event
     *            The MenuDetectEvent, with field {@link TypedEvent#data} set to
     *            the selected {@link ITimeGraphEntry}
     */
    private void fireMenuEventOnTimeGraphEntry(MenuDetectEvent event) {
        for (MenuDetectListener listener : fTimeGraphEntryMenuListeners) {
            listener.menuDetected(event);
        }
    }

    /**
     * Add a menu listener on {@link ITimeEvent}s
     *
     * @param listener
     *            The listener to add
     */
    public void addTimeEventMenuListener(MenuDetectListener listener) {
        if (!fTimeEventMenuListeners.contains(listener)) {
            fTimeEventMenuListeners.add(listener);
        }
    }

    /**
     * Remove a menu listener on {@link ITimeEvent}s
     *
     * @param listener
     *            The listener to remove
     */
    public void removeTimeEventMenuListener(MenuDetectListener listener) {
        if (fTimeEventMenuListeners.contains(listener)) {
            fTimeEventMenuListeners.remove(listener);
        }
    }

    /**
     * Menu event callback on {@link ITimeEvent}s
     *
     * @param event
     *            The MenuDetectEvent, with field {@link TypedEvent#data} set to
     *            the selected {@link ITimeEvent}
     */
    private void fireMenuEventOnTimeEvent(MenuDetectEvent event) {
        for (MenuDetectListener listener : fTimeEventMenuListeners) {
            listener.menuDetected(event);
        }
    }

    @Override
    public boolean setFocus() {
        if ((fTimeProvider != null) && fTimeProvider.getNameSpace() > 0) {
            fHasNamespaceFocus = true;
        }
        return super.setFocus();
    }

    /**
     * Returns the current selection for this time graph. If a time graph entry
     * is selected, it will be the first element in the selection. If a time
     * event is selected, it will be the second element in the selection.
     *
     * @return the current selection
     */
    @Override
    public ISelection getSelection() {
        ITimeGraphEntry entry = getSelectedTrace();
        if (null != entry && null != fTimeProvider) {
            long selectedTime = fTimeProvider.getSelectionBegin();
            ITimeEvent event = Utils.findEvent(entry, selectedTime, 0);
            if (event == null) {
                return new StructuredSelection(entry);
            }
            return new StructuredSelection(new Object[] { entry, event });
        }
        return StructuredSelection.EMPTY;
    }

    /**
     * Get the selection object
     *
     * @return The selection
     */
    public ISelection getSelectionTrace() {
        ITimeGraphEntry entry = getSelectedTrace();
        if (null != entry) {
            return new StructuredSelection(entry);
        }
        return StructuredSelection.EMPTY;
    }

    /**
     * Enable/disable one of the traces in the model
     *
     * @param n
     *            1 to enable it, -1 to disable. The method returns immediately
     *            if another value is used.
     */
    public void selectTrace(int n) {
        if ((n != 1) && (n != -1)) {
            return;
        }

        boolean changed = false;
        int lastSelection = -1;
        for (int i = 0; i < fItemData.fExpandedItems.length; i++) {
            Item item = fItemData.fExpandedItems[i];
            if (item.fSelected) {
                lastSelection = i;
                if ((1 == n) && (i < fItemData.fExpandedItems.length - 1)) {
                    item.fSelected = false;
                    item = fItemData.fExpandedItems[i + 1];
                    item.fSelected = true;
                    changed = true;
                } else if ((-1 == n) && (i > 0)) {
                    item.fSelected = false;
                    item = fItemData.fExpandedItems[i - 1];
                    item.fSelected = true;
                    changed = true;
                }
                break;
            }
        }

        if (lastSelection < 0 && fItemData.fExpandedItems.length > 0) {
            Item item = fItemData.fExpandedItems[0];
            item.fSelected = true;
            changed = true;
        }

        if (changed) {
            ensureVisibleItem(-1, false);
            redraw();
            fireSelectionChanged();
        }
    }

    /**
     * Select an event
     *
     * @param n
     *            1 for next event, -1 for previous event
     * @param extend
     *            true to extend selection range, false for single selection
     * @since 1.0
     */
    public void selectEvent(int n, boolean extend) {
        if (null == fTimeProvider) {
            return;
        }
        ITimeGraphEntry entry = getSelectedTrace();
        if (entry == null) {
            return;
        }
        long time = fTimeProvider.getSelectionEnd();
        if (n > 0) {
            time = Math.min(Utils.nextChange(entry, time), fTimeProvider.getMaxTime());
        } else if (n < 0) {
            time = Math.max(Utils.prevChange(entry, time), fTimeProvider.getMinTime());
        }
        if (extend) {
            fTimeProvider.setSelectionRangeNotify(fTimeProvider.getSelectionBegin(), time, true);
        } else {
            fTimeProvider.setSelectedTimeNotify(time, true);
        }
        fireSelectionChanged();
        updateStatusLine(STATUS_WITHOUT_CURSOR_TIME);
    }

    /**
     * Select the next event
     *
     * @param extend
     *            true to extend selection range, false for single selection
     * @since 1.0
     */
    public void selectNextEvent(boolean extend) {
        selectEvent(1, extend);
        // Notify if visible time window has been adjusted
        fTimeProvider.setStartFinishTimeNotify(fTimeProvider.getTime0(), fTimeProvider.getTime1());
    }

    /**
     * Select the previous event
     *
     * @param extend
     *            true to extend selection range, false for single selection
     * @since 1.0
     */
    public void selectPrevEvent(boolean extend) {
        selectEvent(-1, extend);
        // Notify if visible time window has been adjusted
        fTimeProvider.setStartFinishTimeNotify(fTimeProvider.getTime0(), fTimeProvider.getTime1());
    }

    /**
     * Select the next trace
     */
    public void selectNextTrace() {
        selectTrace(1);
    }

    /**
     * Select the previous trace
     */
    public void selectPrevTrace() {
        selectTrace(-1);
    }

    /**
     * Scroll left or right by one quarter window size
     *
     * @param left
     *            true to scroll left, false to scroll right
     */
    public void horizontalScroll(boolean left) {
        long time0 = fTimeProvider.getTime0();
        long time1 = fTimeProvider.getTime1();
        long timeMin = fTimeProvider.getMinTime();
        long timeMax = fTimeProvider.getMaxTime();
        long range = time1 - time0;
        if (range <= 0) {
            return;
        }
        long increment = Math.max(1, range / 4);
        if (left) {
            time0 = Math.max(time0 - increment, timeMin);
            time1 = time0 + range;
        } else {
            time1 = Math.min(time1 + increment, timeMax);
            time0 = time1 - range;
        }
        fTimeProvider.setStartFinishTimeNotify(time0, time1);
    }

    /**
     * Zoom based on mouse cursor location with mouse scrolling
     *
     * @param zoomIn
     *            true to zoom in, false to zoom out
     */
    public void zoom(boolean zoomIn) {
        Point cursorDisplayLocation = getDisplay().getCursorLocation();
        Point cursorControlLocation = toControl(cursorDisplayLocation);
        Point cursorParentLocation = getParent().toControl(cursorDisplayLocation);
        Rectangle controlBounds = getBounds();
        // check the X axis only
        if (!controlBounds.contains(cursorParentLocation.x, controlBounds.y)) {
            return;
        }
        int nameSpace = fTimeProvider.getNameSpace();
        int timeSpace = fTimeProvider.getTimeSpace();
        int xPos = Math.max(nameSpace, Math.min(nameSpace + timeSpace, cursorControlLocation.x));
        long time0 = fTimeProvider.getTime0();
        long time1 = fTimeProvider.getTime1();
        long interval = time1 - time0;
        if (interval == 0) {
            interval = 1;
        } // to allow getting out of single point interval
        long newInterval;
        if (zoomIn) {
            newInterval = Math.max(Math.round(interval * ZOOM_IN_FACTOR), fTimeProvider.getMinTimeInterval());
        } else {
            newInterval = (long) Math.ceil(interval * ZOOM_OUT_FACTOR);
        }
        long center = time0 + Math.round(((double) (xPos - nameSpace) / timeSpace * interval));
        long newTime0 = center - Math.round((double) newInterval * (center - time0) / interval);
        /* snap to bounds if zooming out of range */
        newTime0 = Math.max(fTimeProvider.getMinTime(), Math.min(newTime0, fTimeProvider.getMaxTime() - newInterval));
        long newTime1 = newTime0 + newInterval;
        fTimeProvider.setStartFinishTimeNotify(newTime0, newTime1);
    }

    /**
     * zoom in using single click
     */
    public void zoomIn() {
        long prevTime0 = fTimeProvider.getTime0();
        long prevTime1 = fTimeProvider.getTime1();
        long prevRange = prevTime1 - prevTime0;
        if (prevRange == 0) {
            return;
        }
        ITimeDataProvider provider = fTimeProvider;
        long selTime = (provider.getSelectionEnd() + provider.getSelectionBegin()) / 2;
        if (selTime < prevTime0 || selTime > prevTime1) {
            selTime = (prevTime0 + prevTime1) / 2;
        }
        long time0 = selTime - (long) ((selTime - prevTime0) / ZOOM_FACTOR);
        long time1 = selTime + (long) ((prevTime1 - selTime) / ZOOM_FACTOR);

        long min = fTimeProvider.getMinTimeInterval();
        if ((time1 - time0) < min) {
            time0 = selTime - (selTime - prevTime0) * min / prevRange;
            time1 = time0 + min;
        }

        fTimeProvider.setStartFinishTimeNotify(time0, time1);
    }

    /**
     * zoom out using single click
     */
    public void zoomOut() {
        long prevTime0 = fTimeProvider.getTime0();
        long prevTime1 = fTimeProvider.getTime1();
        ITimeDataProvider provider = fTimeProvider;
        long selTime = (provider.getSelectionEnd() + provider.getSelectionBegin()) / 2;
        if (selTime < prevTime0 || selTime > prevTime1) {
            selTime = (prevTime0 + prevTime1) / 2;
        }
        long newInterval;
        long time0;
        if (prevTime1 - prevTime0 <= 1) {
            newInterval = 2;
            time0 = selTime - 1;
        } else {
            newInterval = (long) Math.ceil((prevTime1 - prevTime0) * ZOOM_FACTOR);
            time0 = selTime - (long) Math.ceil((selTime - prevTime0) * ZOOM_FACTOR);
        }
        /* snap to bounds if zooming out of range */
        time0 = Math.max(fTimeProvider.getMinTime(), Math.min(time0, fTimeProvider.getMaxTime() - newInterval));
        long time1 = time0 + newInterval;

        fTimeProvider.setStartFinishTimeNotify(time0, time1);
    }

    /**
     * Zoom vertically.
     *
     * @param zoomIn
     *            true to zoom in, false to zoom out
     * @since 2.0
     */
    public void verticalZoom(boolean zoomIn) {
        if (zoomIn) {
            fHeightAdjustment++;
        } else {
            fHeightAdjustment--;
            fHeightAdjustment = Math.max(fHeightAdjustment, 1 - fMaxItemHeight);
        }
        fItemData.refreshData();
        redraw();
    }

    /**
     * Reset the vertical zoom to default.
     *
     * @since 2.0
     */
    public void resetVerticalZoom() {
        fHeightAdjustment = 0;
        fItemData.refreshData();
        redraw();
    }

    /**
     * Set the vertical grid lines visibility. The default is true.
     *
     * @param visible
     *            true to show the grid lines, false otherwise
     * @since 2.0
     */
    public void setGridLinesVisible(boolean visible) {
        fGridLinesVisible = visible;
        redraw();
    }

    /**
     * Get the vertical grid lines visibility.
     *
     * @return true if the grid lines are visible, false otherwise
     * @since 2.0
     */
    public boolean getGridLinesVisible() {
        return fGridLinesVisible;
    }

    /**
     * Set the grid line color. The default is SWT.COLOR_GRAY.
     *
     * @param color
     *            the grid line color
     * @since 2.0
     */
    public void setGridLineColor(Color color) {
        fGridLineColor = color;
        redraw();
    }

    /**
     * Get the grid line color.
     *
     * @return the grid line color
     * @since 2.0
     */
    public Color getGridLineColor() {
        return fGridLineColor;
    }

    /**
     * Set the label visibility. The default is true.
     *
     * @param visible
     *            true to show the labels, false otherwise
     * @since 5.2
     */
    public void setLabelsVisible(boolean visible) {
        fLabelsVisible = visible;
        redraw();
    }

    /**
     * Set the horizontal middle lines visibility. The default is true.
     *
     * @param visible
     *            true to show the middle lines, false otherwise
     * @since 4.0
     */
    public void setMidLinesVisible(boolean visible) {
        fMidLinesVisible = visible;
        redraw();
    }

    /**
     * Get the horizontal middle lines visibility.
     *
     * @return true if the middle lines are visible, false otherwise
     * @since 4.0
     */
    public boolean getMidLinesVisible() {
        return fMidLinesVisible;
    }

    /**
     * Set the markers list.
     *
     * @param markers
     *            The markers list, or null
     * @since 2.0
     */
    public void setMarkers(List<IMarkerEvent> markers) {
        fMarkers = markers;
    }

    /**
     * Get the markers list.
     *
     * @return The markers list, or null
     * @since 2.0
     */
    public List<IMarkerEvent> getMarkers() {
        return fMarkers;
    }

    /**
     * Set the markers visibility. The default is true.
     *
     * @param visible
     *            true to show the markers, false otherwise
     * @since 2.0
     */
    public void setMarkersVisible(boolean visible) {
        fMarkersVisible = visible;
    }

    /**
     * Get the markers visibility.
     *
     * @return true if the markers are visible, false otherwise
     * @since 2.0
     */
    public boolean getMarkersVisible() {
        return fMarkersVisible;
    }

    /**
     * Hide arrows
     *
     * @param hideArrows
     *            true to hide arrows
     */
    public void hideArrows(boolean hideArrows) {
        fHideArrows = hideArrows;
    }

    /**
     * Follow the arrow forward
     *
     * @param extend
     *            true to extend selection range, false for single selection
     * @since 1.0
     */
    public void followArrowFwd(boolean extend) {
        ITimeGraphEntry trace = getSelectedTrace();
        if (trace == null) {
            return;
        }
        long selectedTime = fTimeProvider.getSelectionEnd();
        for (ILinkEvent link : fItemData.fLinks) {
            if (link.getEntry() == trace && link.getTime() == selectedTime) {
                selectItem(link.getDestinationEntry(), false);
                if (link.getDuration() != 0) {
                    if (extend) {
                        fTimeProvider.setSelectionRangeNotify(fTimeProvider.getSelectionBegin(), link.getTime() + link.getDuration(), true);
                    } else {
                        fTimeProvider.setSelectedTimeNotify(link.getTime() + link.getDuration(), true);
                    }
                    // Notify if visible time window has been adjusted
                    fTimeProvider.setStartFinishTimeNotify(fTimeProvider.getTime0(), fTimeProvider.getTime1());
                }
                fireSelectionChanged();
                updateStatusLine(STATUS_WITHOUT_CURSOR_TIME);
                return;
            }
        }
        selectNextEvent(extend);
    }

    /**
     * Follow the arrow backward
     *
     * @param extend
     *            true to extend selection range, false for single selection
     * @since 1.0
     */
    public void followArrowBwd(boolean extend) {
        ITimeGraphEntry trace = getSelectedTrace();
        if (trace == null) {
            return;
        }
        long selectedTime = fTimeProvider.getSelectionEnd();
        for (ILinkEvent link : fItemData.fLinks) {
            if (link.getDestinationEntry() == trace && link.getTime() + link.getDuration() == selectedTime) {
                selectItem(link.getEntry(), false);
                if (link.getDuration() != 0) {
                    if (extend) {
                        fTimeProvider.setSelectionRangeNotify(fTimeProvider.getSelectionBegin(), link.getTime(), true);
                    } else {
                        fTimeProvider.setSelectedTimeNotify(link.getTime(), true);
                    }
                    // Notify if visible time window has been adjusted
                    fTimeProvider.setStartFinishTimeNotify(fTimeProvider.getTime0(), fTimeProvider.getTime1());
                }
                fireSelectionChanged();
                updateStatusLine(STATUS_WITHOUT_CURSOR_TIME);
                return;
            }
        }
        selectPrevEvent(extend);
    }

    /**
     * Return the currently selected trace
     *
     * @return The entry matching the trace
     */
    public ITimeGraphEntry getSelectedTrace() {
        ITimeGraphEntry trace = null;
        int idx = getSelectedIndex();
        if (idx >= 0) {
            trace = fItemData.fExpandedItems[idx].fEntry;
        }
        return trace;
    }

    /**
     * Retrieve the index of the currently selected item
     *
     * @return The index
     */
    public int getSelectedIndex() {
        int idx = -1;
        for (int i = 0; i < fItemData.fExpandedItems.length; i++) {
            Item item = fItemData.fExpandedItems[i];
            if (item.fSelected) {
                idx = i;
                break;
            }
        }
        return idx;
    }

    boolean toggle(int idx) {
        boolean toggled = false;
        if (idx >= 0 && idx < fItemData.fExpandedItems.length) {
            Item item = fItemData.fExpandedItems[idx];
            if (item.fHasChildren) {
                item.fExpanded = !item.fExpanded;
                fItemData.updateExpandedItems();
                redraw();
                toggled = true;
                fireTreeEvent(item.fEntry, item.fExpanded);
            }
        }
        return toggled;
    }

    /**
     * Gets the index of the item at the given location.
     *
     * @param y
     *            the y coordinate
     * @return the index of the item at the given location, of -1 if none.
     */
    protected int getItemIndexAtY(int y) {
        int ySum = 0;
        if (y < 0) {
            for (int idx = fTopIndex - 1; idx >= 0; idx--) {
                ySum -= fItemData.fExpandedItems[idx].fItemHeight;
                if (y >= ySum) {
                    return idx;
                }
            }
        } else {
            for (int idx = fTopIndex; idx < fItemData.fExpandedItems.length; idx++) {
                ySum += fItemData.fExpandedItems[idx].fItemHeight;
                if (y < ySum) {
                    return idx;
                }
            }
        }
        return -1;
    }

    boolean isOverSplitLine(int x) {
        if (x < 0 || null == fTimeProvider) {
            return false;
        }
        int nameWidth = fTimeProvider.getNameSpace();
        return Math.abs(x - nameWidth) <= SNAP_WIDTH;
    }

    boolean isOverTimeSpace(int x, int y) {
        Point size = getSize();
        return x >= fTimeProvider.getNameSpace() && x < size.x && y >= 0 && y < size.y;
    }

    /**
     * Gets the {@link ITimeGraphEntry} at the given location.
     *
     * @param pt
     *            a point in the widget
     * @return the {@link ITimeGraphEntry} at this point, or <code>null</code>
     *         if none.
     * @since 2.0
     */
    public ITimeGraphEntry getEntry(Point pt) {
        int idx = getItemIndexAtY(pt.y);
        return idx >= 0 ? fItemData.fExpandedItems[idx].fEntry : null;
    }

    /**
     * Return the arrow event closest to the given point that is no further than
     * a maximum distance.
     *
     * @param pt
     *            a point in the widget
     * @return The closest arrow event, or null if there is none close enough.
     */
    protected ILinkEvent getArrow(Point pt) {
        if (fHideArrows) {
            return null;
        }
        ILinkEvent linkEvent = null;
        double minDistance = Double.MAX_VALUE;
        Rectangle clientArea = getClientArea();
        for (ILinkEvent event : fItemData.fLinks) {
            Rectangle rect = getArrowRectangle(clientArea, event);
            if (rect != null) {
                int x1 = rect.x;
                int y1 = rect.y;
                int x2 = x1 + rect.width;
                int y2 = y1 + rect.height;
                double d = Utils.distance(pt.x, pt.y, x1, y1, x2, y2);
                if (minDistance > d) {
                    minDistance = d;
                    linkEvent = event;
                }
            }
        }
        if (minDistance <= ARROW_HOVER_MAX_DIST) {
            return linkEvent;
        }
        return null;
    }

    @Override
    public int getXForTime(long time) {
        if (null == fTimeProvider) {
            return -1;
        }
        long time0 = fTimeProvider.getTime0();
        long time1 = fTimeProvider.getTime1();
        int width = getWidth();
        int nameSpace = fTimeProvider.getNameSpace();
        double pixelsPerNanoSec = (width - nameSpace <= RIGHT_MARGIN) ? 0 : (double) (width - nameSpace - RIGHT_MARGIN) / (time1 - time0);
        return SaturatedArithmetic.add(getBounds().x + nameSpace, (int) ((time - time0) * pixelsPerNanoSec));
    }

    @Override
    public Point getSize() {
        Point size = fSize;
        if (size == null) {
            size = Objects.requireNonNull(super.getSize());
            fSize = size;
        }
        return new Point(size.x, size.y);
    }

    @Override
    public Rectangle getBounds() {
        Rectangle bounds = fBounds;
        if (bounds == null) {
            bounds = Objects.requireNonNull(super.getBounds());
            fBounds = bounds;
        }
        return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Returns a integer describing the receiver's width in points.
     *
     * @return the receiver's width
     *
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the receiver</li>
     *                </ul>
     */
    private int getWidth() {
        return getSize().x;
    }

    @Override
    public long getTimeAtX(int coord) {
        if (null == fTimeProvider) {
            return -1;
        }
        long hitTime = -1;
        Point size = getSize();
        long time0 = fTimeProvider.getTime0();
        long time1 = fTimeProvider.getTime1();
        int nameWidth = fTimeProvider.getNameSpace();
        final int x = coord - nameWidth;
        int timeWidth = size.x - nameWidth - RIGHT_MARGIN;
        if (x >= 0 && size.x >= nameWidth) {
            if (time1 - time0 > timeWidth) {
                // nanosecond smaller than one pixel: use the first integer
                // nanosecond of this pixel's time range
                hitTime = time0 + (long) Math.ceil((time1 - time0) * ((double) x / timeWidth));
            } else {
                // nanosecond greater than one pixel: use closest nanosecond to
                // this pixel's start time
                hitTime = time0 + Math.round((time1 - time0) * ((double) x / timeWidth));
            }
        }
        return hitTime;
    }

    void selectItem(int idx, boolean addSelection) {
        selectItem(idx, addSelection, true);
    }

    void selectItem(int idx, boolean addSelection, boolean reveal) {
        boolean changed = false;
        if (addSelection) {
            if (idx >= 0 && idx < fItemData.fExpandedItems.length) {
                Item item = fItemData.fExpandedItems[idx];
                changed = !item.fSelected;
                item.fSelected = true;
            }
        } else {
            for (int i = 0; i < fItemData.fExpandedItems.length; i++) {
                Item item = fItemData.fExpandedItems[i];
                if ((i == idx && !item.fSelected) || (idx == -1 && item.fSelected)) {
                    changed = true;
                }
                item.fSelected = i == idx;
            }
        }
        if (reveal) {
            changed |= ensureVisibleItem(idx, true);
        }
        if (changed) {
            redraw();
        }
    }

    /**
     * Select an entry and make it visible
     *
     * @param entry
     *            The entry to select
     * @param addSelection
     *            <code>true</code> to add the entry to the current selection,
     *            or <code>false</code> to set a new selection
     */
    public void selectItem(ITimeGraphEntry entry, boolean addSelection) {
        int idx = fItemData.findItemIndex(entry);
        selectItem(idx, addSelection);
    }

    /**
     * Select an entry
     *
     * @param entry
     *            The entry to select
     * @param addSelection
     *            <code>true</code> to add the entry to the current selection,
     *            or <code>false</code> to set a new selection
     * @param reveal
     *            <code>true</code> if the selection is to be made visible, and
     *            <code>false</code> otherwise
     * @since 2.3
     */
    public void selectItem(ITimeGraphEntry entry, boolean addSelection, boolean reveal) {
        int idx = fItemData.findItemIndex(entry);
        selectItem(idx, addSelection, reveal);
    }

    /**
     * Retrieve the number of entries shown per page.
     *
     * @return The count
     */
    public int countPerPage() {
        int height = getSize().y;
        int count = 0;
        int ySum = 0;
        for (int idx = fTopIndex; idx < fItemData.fExpandedItems.length; idx++) {
            ySum += fItemData.fExpandedItems[idx].fItemHeight;
            if (ySum >= height) {
                return count;
            }
            count++;
        }
        for (int idx = fTopIndex - 1; idx >= 0; idx--) {
            ySum += fItemData.fExpandedItems[idx].fItemHeight;
            if (ySum >= height) {
                return count;
            }
            count++;
        }
        return count;
    }

    /**
     * Get the index of the top element
     *
     * @return The index
     */
    public int getTopIndex() {
        return fTopIndex;
    }

    /**
     * Get the number of expanded (visible) items
     *
     * @return The count of expanded (visible) items
     */
    public int getExpandedElementCount() {
        return fItemData.fExpandedItems.length;
    }

    /**
     * Get an array of all expanded (visible) elements
     *
     * @return The expanded (visible) elements
     */
    public ITimeGraphEntry[] getExpandedElements() {
        ArrayList<ITimeGraphEntry> elements = new ArrayList<>();
        for (Item item : fItemData.fExpandedItems) {
            elements.add(item.fEntry);
        }
        return elements.toArray(new ITimeGraphEntry[0]);
    }

    /**
     * Get the expanded (visible) element at the specified index.
     *
     * @param index
     *            the element index
     * @return The expanded (visible) element or null if out of range
     * @since 2.0
     */
    public ITimeGraphEntry getExpandedElement(int index) {
        if (index < 0 || index >= fItemData.fExpandedItems.length) {
            return null;
        }
        return fItemData.fExpandedItems[index].fEntry;
    }

    /**
     * Get the bounds of the specified entry relative to its parent time graph.
     *
     * @param entry
     *            the time graph entry
     * @return the bounds of the entry, or null if the entry is not visible
     * @since 2.3
     */
    public Rectangle getItemBounds(ITimeGraphEntry entry) {
        int idx = fItemData.findItemIndex(entry);
        if (idx >= 0) {
            return getItemRect(getClientArea(), idx);
        }
        return null;
    }

    Rectangle getNameRect(Rectangle bounds, int idx, int nameWidth) {
        Rectangle rect = getItemRect(bounds, idx);
        rect.width = nameWidth;
        return rect;
    }

    Rectangle getStatesRect(Rectangle bounds, int idx, int nameWidth) {
        Rectangle rect = getItemRect(bounds, idx);
        rect.x += nameWidth;
        rect.width -= nameWidth;
        return rect;
    }

    Rectangle getItemRect(Rectangle bounds, int idx) {
        int[] ySums = fItemData.fYSums;
        ySums[0] = 0;
        if (ySums[idx] == ItemData.UNSET_SUM) {
            /* find closest previous item that is set */
            int i = idx - 1;
            while (ySums[i] == ItemData.UNSET_SUM) {
                i--;
            }
            /* calculate and set all sums up to requested index */
            while (i < idx) {
                ySums[i + 1] = ySums[i] + fItemData.fExpandedItems[i].fItemHeight;
                i++;
            }
        }
        /* subtract top index position from calculated position */
        int y = bounds.y + ySums[idx] - ySums[fTopIndex];
        int height = fItemData.fExpandedItems[idx].fItemHeight;
        return new Rectangle(bounds.x, y, bounds.width, height);
    }

    @Override
    void paint(Rectangle bounds, PaintEvent e) {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, fPaintScopeLabel)) {
            GC gc = e.gc;
            fPostDrawEntries.clear();
            fPostDrawArrows.clear();
            fLines.clear();
            fPoints.clear();
            fSelectedRectangles.clear();

            if (bounds.width < 2 || bounds.height < 2 || null == fTimeProvider) {
                return;
            }

            fIdealNameSpace = 0;
            int nameSpace = fTimeProvider.getNameSpace();
            try (ScopeLog bgScope = new ScopeLog(LOGGER, Level.FINEST, fBackgroundScopeLabel)) {
                // draw the background layer
                drawBackground(bounds, nameSpace, gc);
            }
            try (ScopeLog glScope = new ScopeLog(LOGGER, Level.FINEST, fGridLinesScopeLabel)) {
                // draw the grid lines
                drawGridLines(bounds, gc);
            }
            try (ScopeLog bgmScope = new ScopeLog(LOGGER, Level.FINEST, fBgmScopeLabel)) {
                // draw the background markers
                drawMarkers(bounds, fTimeProvider, fMarkers, false, nameSpace, gc);
            }
            try (ScopeLog itemsScope = new ScopeLog(LOGGER, Level.FINEST, fItemsScopeLabel)) {
                // draw the items
                drawItems(bounds, fTimeProvider, fItemData.fExpandedItems, fTopIndex, nameSpace, gc);
            }
            try (ScopeLog markerScope = new ScopeLog(LOGGER, Level.FINEST, fMarkersScopeLabel)) {
                // draw the foreground markers
                drawMarkers(bounds, fTimeProvider, fMarkers, true, nameSpace, gc);
            }

            try (ScopeLog linksScope = new ScopeLog(LOGGER, Level.FINEST, fLinksScopeLabel)) {
                // draw the links (arrows)
                drawLinks(bounds, fTimeProvider, fItemData.fLinks, nameSpace, gc);
            }

            gc.setAlpha(OPAQUE * 2 / 5);

            long time0 = fTimeProvider.getTime0();
            long time1 = fTimeProvider.getTime1();
            long selectionBegin = fTimeProvider.getSelectionBegin();
            long selectionEnd = fTimeProvider.getSelectionEnd();
            double pixelsPerNanoSec = (bounds.width - nameSpace <= RIGHT_MARGIN) ? 0 : (double) (bounds.width - nameSpace - RIGHT_MARGIN) / (time1 - time0);
            int x0 = SaturatedArithmetic.add(bounds.x + nameSpace, (int) ((selectionBegin - time0) * pixelsPerNanoSec));
            int x1 = SaturatedArithmetic.add(bounds.x + nameSpace, (int) ((selectionEnd - time0) * pixelsPerNanoSec));

            // draw selection lines
            if (fDragState != DRAG_SELECTION) {
                gc.setForeground(getColorScheme().getColor(TimeGraphColorScheme.SELECTED_TIME));
                if (x0 >= nameSpace && x0 < bounds.x + bounds.width) {
                    gc.drawLine(x0, bounds.y, x0, bounds.y + bounds.height);
                }
                if (x1 != x0) {
                    if (x1 >= nameSpace && x1 < bounds.x + bounds.width) {
                        gc.drawLine(x1, bounds.y, x1, bounds.y + bounds.height);
                    }
                }
            }

            // draw selection background
            if (selectionBegin != 0 && selectionEnd != 0 && fDragState != DRAG_SELECTION) {
                x0 = Math.max(nameSpace, Math.min(bounds.x + bounds.width, x0));
                x1 = Math.max(nameSpace, Math.min(bounds.x + bounds.width, x1));
                gc.setBackground(getColorScheme().getBkColor(false, false, true));
                if (x1 - x0 > 1) {
                    gc.fillRectangle(new Rectangle(x0 + 1, bounds.y, x1 - x0 - 1, bounds.height));
                } else if (x0 - x1 > 1) {
                    gc.fillRectangle(new Rectangle(x1 + 1, bounds.y, x0 - x1 - 1, bounds.height));
                }
            }

            // draw drag selection background
            if (fDragState == DRAG_ZOOM || fDragState == DRAG_SELECTION) {
                gc.setBackground(getColorScheme().getBkColor(false, false, true));
                if (fDragX0 < fDragX) {
                    gc.fillRectangle(new Rectangle(fDragX0, bounds.y, fDragX - fDragX0, bounds.height));
                } else if (fDragX0 > fDragX) {
                    gc.fillRectangle(new Rectangle(fDragX, bounds.y, fDragX0 - fDragX, bounds.height));
                }
            }

            // draw split line
            if (DRAG_SPLIT_LINE == fDragState ||
                    (DRAG_NONE == fDragState && fMouseOverSplitLine && fTimeProvider.getNameSpace() > 0)) {
                gc.setBackground(getColorScheme().getColor(TimeGraphColorScheme.DARK_GRAY));
            } else {
                gc.setBackground(getColorScheme().getColor(TimeGraphColorScheme.GRAY));
            }
            gc.fillRectangle(bounds.x + nameSpace - SNAP_WIDTH, bounds.y, SNAP_WIDTH, bounds.height);

            if (DRAG_ZOOM == fDragState && Math.max(fDragX, fDragX0) > nameSpace) {
                gc.setForeground(getColorScheme().getColor(TimeGraphColorScheme.TOOL_FOREGROUND));
                gc.drawLine(fDragX0, bounds.y, fDragX0, bounds.y + bounds.height - 1);
                if (fDragX != fDragX0) {
                    gc.drawLine(fDragX, bounds.y, fDragX, bounds.y + bounds.height - 1);
                }
            } else if (DRAG_SELECTION == fDragState && Math.max(fDragX, fDragX0) > nameSpace) {
                gc.setForeground(getColorScheme().getColor(TimeGraphColorScheme.SELECTED_TIME));
                gc.drawLine(fDragX0, bounds.y, fDragX0, bounds.y + bounds.height - 1);
                if (fDragX != fDragX0) {
                    gc.drawLine(fDragX, bounds.y, fDragX, bounds.y + bounds.height - 1);
                }
            }

            gc.setAlpha(OPAQUE);
            for (PostDrawEvent postDrawEvent : fPostDrawArrows) {
                postDrawEvent.draw(fTimeGraphProvider, gc);
            }
            fPostDrawEntries.clear();
            fPostDrawArrows.clear();

            fTimeGraphProvider.postDrawControl(bounds, gc);
        }
    }

    /**
     * Draw the background layer. Fills the background of the control's name
     * space and states space, updates the background of items if necessary, and
     * draws the item's name text and middle line.
     *
     * @param bounds
     *            The bounds of the control
     * @param nameSpace
     *            The name space width
     * @param gc
     *            Graphics context
     * @since 2.0
     */
    protected void drawBackground(Rectangle bounds, int nameSpace, GC gc) {
        // draw empty name space background
        gc.setBackground(getColorScheme().getBkColor(false, false, true));
        drawBackground(gc, bounds.x, bounds.y, nameSpace, bounds.height);

        // draw empty states space background
        gc.setBackground(getColorScheme().getColor(TimeGraphColorScheme.BACKGROUND));
        drawBackground(gc, bounds.x + nameSpace, bounds.y, bounds.width - nameSpace, bounds.height);

        for (int i = fTopIndex; i < fItemData.fExpandedItems.length; i++) {
            if (fItemData.fExpandedItems[i].fItemHeight == 0) {
                continue;
            }
            Rectangle itemRect = getItemRect(bounds, i);
            if (itemRect.y >= bounds.y + bounds.height) {
                break;
            }
            Item item = fItemData.fExpandedItems[i];
            // draw the background of selected item and items with no time
            // events
            if (!item.fEntry.hasTimeEvents()) {
                gc.setBackground(getColorScheme().getBkColorGroup(item.fSelected, fIsInFocus));
                gc.fillRectangle(itemRect);
            } else if (item.fSelected) {
                gc.setBackground(getColorScheme().getBkColor(true, fIsInFocus, true));
                gc.fillRectangle(itemRect.x, itemRect.y, nameSpace, itemRect.height);
                gc.setBackground(getColorScheme().getBkColor(true, fIsInFocus, false));
                gc.fillRectangle(nameSpace, itemRect.y, itemRect.width - nameSpace, itemRect.height);
            }
            // draw the name space
            Rectangle nameRect = new Rectangle(itemRect.x, itemRect.y, nameSpace, itemRect.height);
            drawName(item, nameRect, gc);
            if (fMidLinesVisible && item.fEntry.hasTimeEvents() && item.fItemHeight > MIN_MIDLINE_HEIGHT && !(item.fEntry instanceof TimeGraphLineEntry)) {
                Rectangle rect = new Rectangle(nameSpace, itemRect.y, itemRect.width - nameSpace, itemRect.height);
                drawMidLine(rect, gc);
            }
        }
    }

    /**
     * Draw the grid lines
     *
     * @param bounds
     *            The bounds of the control
     * @param gc
     *            Graphics context
     * @since 2.0
     */
    public void drawGridLines(Rectangle bounds, GC gc) {
        if (!fGridLinesVisible) {
            return;
        }
        gc.setForeground(fGridLineColor);
        gc.setAlpha(fGridLineColor.getAlpha());
        for (int x : fTimeGraphScale.getTickList()) {
            gc.drawLine(x, bounds.y, x, bounds.y + bounds.height);
        }
        gc.setAlpha(OPAQUE);
    }

    /**
     * Draw the markers
     *
     * @param bounds
     *            The rectangle of the area
     * @param timeProvider
     *            The time provider
     * @param markers
     *            The list of markers
     * @param foreground
     *            true to draw the foreground markers, false otherwise
     * @param nameSpace
     *            The width reserved for the names
     * @param gc
     *            Reference to the SWT GC object
     * @since 2.0
     */
    protected void drawMarkers(Rectangle bounds, ITimeDataProvider timeProvider, List<IMarkerEvent> markers, boolean foreground, int nameSpace, GC gc) {
        if (!fMarkersVisible || markers == null || markers.isEmpty()) {
            return;
        }
        gc.setClipping(new Rectangle(nameSpace, 0, bounds.width - nameSpace, bounds.height));
        /* the list can grow concurrently but cannot shrink */
        int size = markers.size();
        for (int i = 0; i < size; i++) {
            IMarkerEvent marker = markers.get(i);
            if (marker.isForeground() == foreground) {
                drawMarker(marker, bounds, timeProvider, nameSpace, gc);
            }
        }
        gc.setClipping((Rectangle) null);
        TraceCompassLogUtils.traceCounter(LOGGER, Level.FINER, fDrawMarkersCountLabel, size);
    }

    /**
     * Draw a single marker
     *
     * @param marker
     *            The marker event
     * @param bounds
     *            The bounds of the control
     * @param timeProvider
     *            The time provider
     * @param nameSpace
     *            The width reserved for the name
     * @param gc
     *            Reference to the SWT GC object
     * @since 2.0
     */
    protected void drawMarker(IMarkerEvent marker, Rectangle bounds, ITimeDataProvider timeProvider, int nameSpace, GC gc) {
        Rectangle rect = Utils.clone(bounds);
        if (marker.getEntry() != null) {
            int index = fItemData.findItemIndex(marker.getEntry());
            if (index == -1) {
                return;
            }
            rect = getStatesRect(bounds, index, nameSpace);
            if (rect.y < 0 || rect.y > bounds.height) {
                return;
            }
        }
        int x0 = getXForTime(marker.getTime());
        int x1 = getXForTime(marker.getTime() + marker.getDuration());
        if (x0 > bounds.width || x1 < nameSpace) {
            return;
        }
        rect.x = Math.max(nameSpace, Math.min(bounds.width, x0));
        rect.width = Math.max(1, Math.min(bounds.width, x1) - rect.x);

        StyleManager styleManager = getStyleManager();
        OutputElementStyle elementStyle = getElementStyle(marker);
        if (elementStyle == null) {
            return;
        }

        RGBAColor rgba = styleManager.getColorStyle(elementStyle, StyleProperties.COLOR);
        int colorInt = (rgba != null) ? rgba.toInt() : RGBAUtil.fromRGBA(marker.getColor());
        Color color = TimeGraphRender.getColor(colorInt);
        Object symbolType = styleManager.getStyle(elementStyle, StyleProperties.SYMBOL_TYPE);
        if (symbolType != null && symbolType != SymbolType.NONE) {
            gc.setAlpha(colorInt & 0xff);
            Float heightFactor = styleManager.getFactorStyle(elementStyle, StyleProperties.HEIGHT);
            heightFactor = (heightFactor != null) ? Math.max(0.0f, Math.min(1.0f, heightFactor)) : 1.0f;
            int symbolSize = (int) Math.ceil(rect.height * heightFactor / 2);
            Object verticalAlign = styleManager.getStyle(elementStyle, StyleProperties.VERTICAL_ALIGN);
            int y = rect.y + rect.height / 2;
            if (VerticalAlign.BOTTOM.equals(verticalAlign)) {
                y += Math.max(0, rect.height / 2 - symbolSize);
            } else if (VerticalAlign.TOP.equals(verticalAlign)) {
                y -= Math.max(0, rect.height / 2 - symbolSize);
            }
            String symbol = String.valueOf(symbolType);
            switch (symbol) {
            case SymbolType.CROSS:
                SymbolHelper.drawCross(gc, color, symbolSize, rect.x, y);
                break;
            case SymbolType.PLUS:
                SymbolHelper.drawPlus(gc, color, symbolSize, rect.x, y);
                break;
            case SymbolType.SQUARE:
                SymbolHelper.drawSquare(gc, color, symbolSize, rect.x, y);
                break;
            case SymbolType.TRIANGLE:
                SymbolHelper.drawTriangle(gc, color, symbolSize, rect.x, y);
                break;
            case SymbolType.INVERTED_TRIANGLE:
                SymbolHelper.drawInvertedTriangle(gc, color, symbolSize, rect.x, y);
                break;
            case SymbolType.CIRCLE:
                SymbolHelper.drawCircle(gc, color, symbolSize, rect.x, y);
                break;
            case SymbolType.DIAMOND:
                SymbolHelper.drawDiamond(gc, color, symbolSize, rect.x, y);
                break;
            default:
                Color oldColor = gc.getForeground();
                gc.setForeground(color);
                int height = (int) (rect.height * heightFactor);
                TimeGraphRender.setFontForHeight(height, gc);
                int textSize = (gc.textExtent(symbol).y + 1) / 2;
                if (VerticalAlign.BOTTOM.equals(verticalAlign)) {
                    y = rect.y + rect.height / 2 + Math.max(0, rect.height / 2 - textSize);
                } else if (VerticalAlign.TOP.equals(verticalAlign)) {
                    y = rect.y + rect.height / 2 - Math.max(0, rect.height / 2 - textSize);
                }
                gc.drawText(symbol, rect.x - symbolSize, y - textSize, true);
                gc.setForeground(oldColor);
            }
            gc.setAlpha(OPAQUE);
            if (marker.getDuration() == 0) {
                return;
            }
        }
        oldDrawMarker(marker, gc, rect, colorInt);
    }

    private void oldDrawMarker(IMarkerEvent marker, GC gc, Rectangle rect, int colorInt) {
        Color color = TimeGraphRender.getColor(colorInt);
        gc.setBackground(color);
        gc.setAlpha(colorInt & 0xff);
        gc.fillRectangle(rect);
        gc.setAlpha(OPAQUE);
        String label = marker.getLabel();
        if (fLabelsVisible && label != null && marker.getEntry() != null) {
            label = label.substring(0, Math.min(label.indexOf('\n') != -1 ? label.indexOf('\n') : label.length(), MAX_LABEL_LENGTH));
            gc.setForeground(color);
            Utils.drawText(gc, label, rect.x - gc.textExtent(label).x, rect.y, true);
        }
    }

    /**
     * Draw many items at once
     *
     * @param bounds
     *            The bounds of the control
     * @param timeProvider
     *            The time provider
     * @param items
     *            The array items to draw
     * @param topIndex
     *            The index of the first element to draw
     * @param nameSpace
     *            The name space width
     * @param gc
     *            Graphics context
     */
    public void drawItems(Rectangle bounds, ITimeDataProvider timeProvider,
            Item[] items, int topIndex, int nameSpace, GC gc) {
        int bottomIndex = Integer.min(topIndex + countPerPage() + 1, items.length);
        for (int i = topIndex; i < bottomIndex; i++) {
            Item item = items[i];
            drawItem(item, bounds, timeProvider, i, nameSpace, gc);
        }
        TraceCompassLogUtils.traceCounter(LOGGER, Level.FINER, fDrawItemsCountLabel, bottomIndex - topIndex);

        if (gc == null) {
            return;
        }

        /*
         * Draw entries, entries contain events
         */
        for (DeferredEntry entry : fPostDrawEntries) {
            entry.draw(fTimeGraphProvider, gc);
        }

        // Defer line drawing
        for (DeferredLine line : fLines) {
            line.draw(fTimeGraphProvider, gc);
        }

        Color prev = gc.getForeground();
        Color black = TimeGraphRender.getColor(BLACK.toInt());
        gc.setForeground(black);
        int prevAA = gc.getAntialias();
        /*
         * BUG: Doesn't work in certain distros of Linux the end result is
         * anti-aliased points. They may actually look better but are not as
         * accurate.
         */
        gc.setAntialias(SWT.OFF);
        int prevLineWidth = gc.getLineWidth();
        gc.setLineWidth(1);
        // Deferred point drawing, they are aggregated into segments
        for (DeferredSegment seg : fPoints) {
            seg.draw(fTimeGraphProvider, gc);
        }
        gc.setLineWidth(prevLineWidth);
        gc.setAntialias(prevAA);
        // Draw selection at very end
        for (Rectangle rectangle : fSelectedRectangles) {
            int arc = Math.min(rectangle.height + 1, rectangle.width) / 2;
            gc.drawRoundRectangle(rectangle.x - 1, rectangle.y - 1, rectangle.width, rectangle.height + 1, arc, arc);
        }
        gc.setForeground(prev);
    }

    /**
     * Draws the item
     *
     * @param item
     *            The item to draw
     * @param bounds
     *            The bounds of the control
     * @param timeProvider
     *            The time provider
     * @param i
     *            The expanded item index
     * @param nameSpace
     *            The name space width
     * @param gc
     *            Graphics context
     */
    protected void drawItem(Item item, Rectangle bounds, ITimeDataProvider timeProvider, int i, int nameSpace, GC gc) {
        if (fItemData.fExpandedItems[i].fItemHeight == 0) {
            return;
        }
        Rectangle itemRect = getItemRect(bounds, i);
        if (itemRect.y >= bounds.y + bounds.height || item.fEntry == null) {
            return;
        }
        ITimeGraphEntry entry = Objects.requireNonNull(item.fEntry);
        long time0 = timeProvider.getTime0();
        long time1 = timeProvider.getTime1();
        long selectedTime = fTimeProvider.getSelectionEnd();
        Rectangle rect = new Rectangle(nameSpace, itemRect.y, itemRect.width - nameSpace, itemRect.height);
        DeferredEntry deferredEntry = new DeferredEntry(entry, rect);
        fCurrentDeferredEntry = deferredEntry;
        if (rect.isEmpty() || (time1 <= time0)) {
            fPostDrawEntries.add(deferredEntry);
            return;
        }

        boolean selected = item.fSelected;
        // K pixels per second
        double pixelsPerNanoSec = (rect.width <= RIGHT_MARGIN) ? 0 : (double) (rect.width - RIGHT_MARGIN) / (time1 - time0);

        if (entry.hasTimeEvents() && !(isFilterActive() && hasSavedFilters() && !((TimeGraphEntry) item.fEntry).hasZoomedEvents())) {
            gc.setClipping(new Rectangle(nameSpace, 0, bounds.width - nameSpace, bounds.height));
            fillSpace(rect, gc, selected);

            long maxDuration = (timeProvider.getTimeSpace() == 0) ? Long.MAX_VALUE : 1 * (time1 - time0) / timeProvider.getTimeSpace();
            Iterator<@NonNull ITimeEvent> iterator = entry.getTimeEventsIterator(time0, time1, maxDuration);
            switch (entry.getStyle()) {
            case LINE:
                drawLineGraphEntry(time0, rect, pixelsPerNanoSec, iterator);
                break;
            case STATE:
                drawTimeGraphEntry(gc, time0, selectedTime, rect, selected, pixelsPerNanoSec, iterator);
                break;
            default:
                break;
            }
            gc.setClipping((Rectangle) null);
        }
        fPostDrawEntries.add(deferredEntry);
    }

    private void drawLineGraphEntry(long time0, @NonNull Rectangle rect, double pixelsPerNanoSec, Iterator<ITimeEvent> iterator) {
        // clamp 0 - max positive
        long max = Long.MIN_VALUE;
        long min = 0;
        List<@NonNull List<@NonNull LongPoint>> seriesModel = new ArrayList<>();
        TimeLineEvent lastValid = null;
        while (iterator.hasNext()) {
            ITimeEvent event = iterator.next();
            if (!(event instanceof TimeLineEvent)) {
                continue;
            }
            int x = SaturatedArithmetic.add(rect.x, (int) ((event.getTime() - time0) * pixelsPerNanoSec));
            if (x >= rect.x + rect.width) {
                // event is out of bounds
                continue;
            }
            TimeLineEvent timeEvent = (TimeLineEvent) event;
            List<Long> values = timeEvent.getValues();
            for (int i = 0; i < values.size(); i++) {
                if (seriesModel.size() <= i) {
                    seriesModel.add(new ArrayList<>());
                }
                Long val = values.get(i);
                if (val != null) {
                    // get max and min, this is a relative scale.
                    max = Math.max(Math.abs(val), max);
                    min = 0;
                    lastValid = timeEvent;
                    seriesModel.get(i).add(new LongPoint(x, val));
                }
            }
        }
        if (lastValid == null) {
            return;
        }
        double scale = (max - min) == 0 ? 1.0 : (double) rect.height / (max - min);

        StyleManager styleManager = getStyleManager();
        OutputElementStyle elementStyle = getElementStyle(lastValid);
        if (elementStyle == null) {
            return;
        }

        RGBAColor rgba = styleManager.getColorStyle(elementStyle, StyleProperties.COLOR);
        fLines.add(new DeferredLine(rect, min, seriesModel, rgba == null ? BLACK : rgba, scale));
    }

    private void drawTimeGraphEntry(GC gc, long time0, long selectedTime, Rectangle rect, boolean selected, double pixelsPerNanoSec, Iterator<ITimeEvent> iterator) {
        int lastX = -1;
        fLastTransparentX = -1;
        int margins = TimeGraphRender.getMarginForHeight(rect.height);
        int height = rect.height - margins;
        int topMargin = (margins + 1) / 2;
        Rectangle stateRect = new Rectangle(rect.x, rect.y + topMargin, rect.width, height);
        while (iterator.hasNext()) {
            ITimeEvent event = iterator.next();
            int x = SaturatedArithmetic.add(rect.x, (int) ((event.getTime() - time0) * pixelsPerNanoSec));
            int xEnd = SaturatedArithmetic.add(rect.x, (int) ((event.getTime() + event.getDuration() - time0) * pixelsPerNanoSec));
            if (x >= rect.x + rect.width || xEnd < rect.x) {
                // event is out of bounds
                continue;
            }
            xEnd = Math.min(rect.x + rect.width, xEnd);
            stateRect.x = Math.max(rect.x, x);
            stateRect.width = Math.max(1, xEnd - stateRect.x + 1);
            if (stateRect.x < lastX) {
                stateRect.width -= (lastX - stateRect.x);
                if (stateRect.width > 0) {
                    stateRect.x = lastX;
                } else {
                    stateRect.width = 0;
                }
            }
            boolean timeSelected = selectedTime >= event.getTime() && selectedTime < event.getTime() + event.getDuration();
            if (drawState(getColorScheme(), event, stateRect, gc, selected, timeSelected)) {
                lastX = stateRect.x + stateRect.width;
            }
        }
    }

    /**
     * Draw the links
     *
     * @param bounds
     *            The bounds of the control
     * @param timeProvider
     *            The time provider
     * @param links
     *            The list of link events
     * @param nameSpace
     *            The name space width
     * @param gc
     *            Graphics context
     */
    public void drawLinks(Rectangle bounds, ITimeDataProvider timeProvider,
            List<ILinkEvent> links, int nameSpace, GC gc) {
        if (fHideArrows) {
            return;
        }
        int topIndex = fTopIndex;
        int bottomIndex = topIndex + countPerPage() + 1;
        gc.setClipping(new Rectangle(nameSpace, 0, bounds.width - nameSpace, bounds.height));
        /* the list can grow concurrently but cannot shrink */
        int size = links.size();
        for (int i = 0; i < size; i++) {
            ILinkEvent event = links.get(i);
            int srcIndex = fItemData.findItemIndex(event.getEntry());
            int destIndex = fItemData.findItemIndex(event.getDestinationEntry());

            if ((srcIndex == -1) || (destIndex == -1) || (srcIndex < topIndex && destIndex < topIndex) || (srcIndex > bottomIndex && destIndex > bottomIndex)) {
                continue;
            }
            drawLink(event, bounds, timeProvider, nameSpace, gc);
        }
        gc.setClipping((Rectangle) null);
        TraceCompassLogUtils.traceCounter(LOGGER, Level.FINER, fDrawLinksCountLabel, size);
    }

    /**
     * Draws a link type event
     *
     * @param event
     *            The link event to draw
     * @param bounds
     *            The bounds of the control
     * @param timeProvider
     *            The time provider
     * @param nameSpace
     *            The name space width
     * @param gc
     *            Graphics context
     */
    protected void drawLink(ILinkEvent event, Rectangle bounds, ITimeDataProvider timeProvider, int nameSpace, GC gc) {
        drawArrow(getColorScheme(), Objects.requireNonNull(event), getArrowRectangle(bounds, event), gc);
    }

    private @Nullable Rectangle getArrowRectangle(Rectangle bounds, ILinkEvent event) {
        int srcIndex = fItemData.findItemIndex(event.getEntry());
        int destIndex = fItemData.findItemIndex(event.getDestinationEntry());

        if ((srcIndex == -1) || (destIndex == -1)) {
            return null;
        }

        Rectangle src = getStatesRect(bounds, srcIndex, fTimeProvider.getNameSpace());
        Rectangle dst = getStatesRect(bounds, destIndex, fTimeProvider.getNameSpace());

        int x0 = getXForTime(event.getTime());
        int x1 = getXForTime(event.getTime() + event.getDuration());

        // limit the x-coordinates to prevent integer overflow in calculations
        // and also GC.drawLine doesn't draw properly with large coordinates
        final int limit = Integer.MAX_VALUE / 1024;
        x0 = Math.max(-limit, Math.min(x0, limit));
        x1 = Math.max(-limit, Math.min(x1, limit));

        int y0 = src.y + src.height / 2;
        int y1 = dst.y + dst.height / 2;
        return LineClipper.clip(bounds, x0, y0, x1, y1);
    }

    /**
     * Draw an arrow
     *
     * @param colors
     *            Color scheme
     * @param event
     *            Time event for which we're drawing the arrow
     * @param rect
     *            The arrow rectangle
     * @param gc
     *            Graphics context
     * @return true if the arrow was drawn
     */
    protected boolean drawArrow(TimeGraphColorScheme colors, @NonNull ITimeEvent event,
            Rectangle rect, GC gc) {

        if (rect == null || ((rect.height == 0) && (rect.width == 0))) {
            return false;
        }
        StyleManager styleManager = getStyleManager();
        OutputElementStyle elementStyle = getElementStyle(event);
        if (elementStyle == null) {
            return false;
        }

        RGBAColor rgba = styleManager.getColorStyle(elementStyle, StyleProperties.COLOR);
        rgba = (rgba != null) ? rgba : BLACK;
        int colorInt = rgba.toInt();
        Color color = TimeGraphRender.getColor(colorInt);
        int alpha = rgba.getAlpha();
        int prevAlpha = gc.getAlpha();
        gc.setAlpha(alpha);

        gc.setForeground(color);
        gc.setBackground(color);
        int old = gc.getLineWidth();
        Float widthFactor = styleManager.getFactorStyle(elementStyle, StyleProperties.WIDTH);
        if (widthFactor == null) {
            Float heightFactor = styleManager.getFactorStyle(elementStyle, StyleProperties.HEIGHT);
            widthFactor = (heightFactor != null) ? heightFactor * 10.0f : 1.0f;
        }
        widthFactor = Math.max(1.0f, Math.min(10.0f, widthFactor));
        gc.setLineWidth(widthFactor.intValue());
        /* Draw the arrow */
        Point newEndpoint = drawArrowHead(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, widthFactor, gc);
        gc.drawLine(rect.x, rect.y, newEndpoint.x, newEndpoint.y);
        gc.setLineWidth(old);
        gc.setAlpha(prevAlpha);
        if (!Boolean.TRUE.equals(styleManager.getStyle(elementStyle, StyleProperties.annotated()))) {
            fPostDrawArrows.add(new PostDrawEvent(event, rect));
        }
        return true;
    }

    /*
     * @author Francis Giraldeau
     *
     * Inspiration:
     * http://stackoverflow.com/questions/3010803/draw-arrow-on-line-algorithm
     *
     * The algorithm was taken from this site, not the code itself
     */
    private static Point drawArrowHead(int x0, int y0, int x1, int y1, float scale, GC gc) {
        double factor = 10 + 2.0 * scale;
        double cos = 0.9510;
        double sin = 0.3090;
        long lenx = x1 - x0;
        long leny = y1 - y0;
        double len = Math.sqrt(lenx * lenx + leny * leny);

        double dx = factor * lenx / len;
        double dy = factor * leny / len;
        int end1X = (int) Math.round((x1 - (dx * cos + dy * -sin)));
        int end1Y = (int) Math.round((y1 - (dx * sin + dy * cos)));
        int end2X = (int) Math.round((x1 - (dx * cos + dy * sin)));
        int end2Y = (int) Math.round((y1 - (dx * -sin + dy * cos)));
        int[] arrow = new int[] { x1, y1, end1X, end1Y, end2X, end2Y, x1, y1 };
        gc.fillPolygon(arrow);
        /*
         * The returned point corresponds to the point at 1/4 from the head
         * basis basis to the tip. it will be used as end point the arrow line
         */
        return new Point((3 * ((end1X + end2X) / 2) + x1) / 4, (3 * ((end1Y + end2Y) / 2) + y1) / 4);
    }

    /**
     * Draw the name space of an item.
     *
     * @param item
     *            Item object
     * @param bounds
     *            The bounds of the item's name space
     * @param gc
     *            Graphics context
     */
    protected void drawName(Item item, Rectangle bounds, GC gc) {
        // No name space to be drawn
        if (fTimeProvider.getNameSpace() == 0) {
            return;
        }

        boolean hasTimeEvents = item.fEntry.hasTimeEvents();
        if (hasTimeEvents) {
            gc.setClipping(bounds);
        }

        int height = bounds.height - TimeGraphRender.getMarginForHeight(bounds.height);
        TimeGraphRender.setFontForHeight(height, Objects.requireNonNull(gc));

        String name = fLabelProvider == null ? item.fName : fLabelProvider.getColumnText(item.fEntry, 0);
        Rectangle rect = Utils.clone(bounds);
        rect.y += (bounds.height - gc.stringExtent(name).y) / 2;
        Tree tree = getTree();
        TreeColumn[] columns = tree.getColumns();
        int idealNameSpace = 0;
        for (int i = 0; i < columns.length; i++) {
            int columnIndex = tree.getColumnOrder()[i];
            TreeColumn column = columns[columnIndex];
            rect.width = column.getWidth();
            gc.setClipping(rect.x, bounds.y, Math.min(rect.width, bounds.x + bounds.width - rect.x - SNAP_WIDTH), bounds.height);
            int width = MARGIN;
            if (i == 0) {
                // first visible column
                width += item.fLevel * EXPAND_SIZE;
                if (item.fHasChildren) {
                    // draw expand/collapse arrow
                    gc.setBackground(getColorScheme().getColor(TimeGraphColorScheme.DARK_GRAY));
                    int arrowHeightHint = (height < 4) ? height : (height < 6) ? height - 1 : height - 2;
                    int arrowHalfHeight = Math.max(1, Math.min(arrowHeightHint, (int) Math.round((EXPAND_SIZE - 2) / ARROW_RATIO))) / 2;
                    int arrowHalfWidth = (Math.max(1, Math.min(EXPAND_SIZE - 2, (int) Math.round(arrowHeightHint * ARROW_RATIO))) + 1) / 2;
                    int x1 = bounds.x + width + 1;
                    int x2 = x1 + 2 * arrowHalfWidth;
                    int midy = bounds.y + bounds.height / 2;
                    int y1 = midy - arrowHalfHeight;
                    int y2 = midy + arrowHalfHeight;
                    if (!item.fExpanded) { // >
                        gc.fillPolygon(new int[] { x1, y1, x2, midy, x1, y2 });
                    } else { // v
                        int midx = x1 + arrowHalfWidth;
                        gc.fillPolygon(new int[] { x1, y1, x2, y1, midx, y2 });
                    }
                }
                width += EXPAND_SIZE + MARGIN;

                Image img = fLabelProvider != null ? fLabelProvider.getColumnImage(item.fEntry, columnIndex)
                        : columnIndex == 0 ? fTimeGraphProvider.getItemImage(item.fEntry) : null;
                if (img != null) {
                    // draw icon
                    int imgHeight = img.getImageData().height;
                    int imgWidth = img.getImageData().width;
                    int dstHeight = Math.min(bounds.height, imgHeight);
                    int dstWidth = dstHeight * imgWidth / imgHeight;
                    int x = width;
                    int y = bounds.y + (bounds.height - dstHeight) / 2;
                    gc.drawImage(img, 0, 0, imgWidth, imgHeight, x, y, dstWidth, dstHeight);
                    width += imgWidth + MARGIN;
                }
            } else {
                if (fLabelProvider == null) {
                    break;
                }
            }
            String label = fLabelProvider != null ? fLabelProvider.getColumnText(item.fEntry, columnIndex)
                    : columnIndex == 0 ? item.fName : ""; //$NON-NLS-1$
            gc.setForeground(getColorScheme().getFgColor(item.fSelected, fIsInFocus));
            Rectangle textRect = new Rectangle(rect.x + width, rect.y, rect.width - width, rect.height);
            int textWidth = Utils.drawText(gc, label, textRect, true);
            width += textWidth + MARGIN;
            if (textWidth > 0) {
                idealNameSpace = rect.x + width;
            }
            if (fMidLinesVisible && item.fEntry.hasTimeEvents() && columns.length == 1 && item.fItemHeight > MIN_MIDLINE_HEIGHT) {
                drawMidLine(new Rectangle(bounds.x + width, bounds.y, bounds.x + bounds.width, bounds.height), gc);
            }
            if (fAutoResizeColumns && width > column.getWidth()) {
                column.setData(PREFERRED_WIDTH, width);
                column.setWidth(width);
            }
            gc.setForeground(getColorScheme().getColor(TimeGraphColorScheme.MID_LINE));
            if (i < columns.length - 1) {
                // not the last visible column: draw the vertical cell border
                int x = rect.x + rect.width - 1;
                gc.drawLine(x, bounds.y, x, bounds.y + bounds.height);
            }
            rect.x += rect.width;
        }
        fIdealNameSpace = Math.max(fIdealNameSpace, idealNameSpace);

        gc.setClipping((Rectangle) null);
    }

    /**
     * Draw the state (color fill)
     *
     * @param colors
     *            Color scheme
     * @param event
     *            Time event for which we're drawing the state
     * @param rect
     *            The state rectangle
     * @param gc
     *            Graphics context
     * @param selected
     *            Is this time event currently selected (so it appears
     *            highlighted)
     * @param timeSelected
     *            Is the timestamp currently selected
     * @return true if the state was drawn
     */
    protected boolean drawState(TimeGraphColorScheme colors, @NonNull ITimeEvent event,
            Rectangle rect, GC gc, boolean selected, boolean timeSelected) {

        StyleManager styleManager = getStyleManager();
        OutputElementStyle elementStyle = getElementStyle(event);
        if (elementStyle == null) {
            return false;
        }
        boolean transparent = elementStyle.getParentKey() == null && elementStyle.getStyleValues().isEmpty();
        boolean visible = rect.width <= 0 ? false : true;
        rect.width = Math.max(1, rect.width);
        Float heightFactor = styleManager.getFactorStyle(elementStyle, StyleProperties.HEIGHT);
        heightFactor = (heightFactor != null) ? Math.max(0.0f, Math.min(1.0f, heightFactor)) : DEFAULT_STATE_WIDTH;
        int height = 0;
        if (heightFactor != 0.0f && rect.height != 0) {
            height = Math.max(1, (int) (rect.height * heightFactor));
        }
        Rectangle drawRect = new Rectangle(rect.x, rect.y + ((rect.height - height) / 2), rect.width, height);
        Color black = TimeGraphRender.getColor(BLACK.toInt());
        gc.setForeground(black);

        List<DeferredItem> states = fCurrentDeferredEntry.getItems();
        if (transparent) {
            if (visible) {
                // Avoid overlapping transparent states
                int x = Math.max(fLastTransparentX, drawRect.x);
                drawRect.width = drawRect.x + drawRect.width - x;
                if (drawRect.width > 0) {
                    // Draw transparent background
                    RGBAColor bgColor = fTransparentGrayColor;
                    DeferredItem deferredItem = new DeferredTransparentState(drawRect, bgColor);
                    if (states.isEmpty() || !states.get(states.size() - 1).getBounds().intersects(drawRect)) {
                        states.add(deferredItem);
                        deferredItem.add(new PostDrawEvent(event, drawRect));
                    }
                    fLastTransparentX = Math.max(fLastTransparentX, drawRect.x + drawRect.width);
                } else {
                    // clamp it to 0, just in case
                    drawRect.width = 0;
                }
                if (drawRect.width <= 2) {
                    // Draw point over state
                    addPoint(fPoints, rect.x, rect.y - 2);
                    if (drawRect.width == 2) {
                        addPoint(fPoints, rect.x + 1, rect.y - 2);
                    }
                }
            } else {
                addPoint(fPoints, rect.x, rect.y - 2);
            }

            return false;
        }

        int arc = Math.min(drawRect.height + 1, drawRect.width) / 2;
        RGBAColor rgba = styleManager.getColorStyle(elementStyle, StyleProperties.BACKGROUND_COLOR);
        @NonNull RGBAColor bgColor = (rgba != null) ? rgba : BLACK;
        boolean reallySelected = timeSelected && selected;
        // fill all rect area
        boolean draw = visible || fBlendSubPixelEvents;
        DeferredItem last = null;
        if (draw) {
            if (!states.isEmpty()) {
                DeferredItem state = states.get(states.size() - 1);
                while ((state instanceof DeferredTransparentState) && (state.getBounds().x == drawRect.x)) {
                    states.remove(states.size() - 1);
                    state = states.isEmpty() ? null : states.get(states.size() - 1);
                }
            }

            RGBAColor borderColor = BLACK;
            int lineWidth = DeferredItem.NO_BORDER;
            Object borderStyle = styleManager.getStyle(elementStyle, StyleProperties.BORDER_STYLE);
            boolean hasBorders = borderStyle != null && !BorderStyle.NONE.equals(borderStyle);
            if (hasBorders) {
                Object borderWidth = styleManager.getStyle(elementStyle, StyleProperties.BORDER_WIDTH);
                lineWidth = 1;
                if (borderWidth instanceof Integer) {
                    lineWidth = (int) borderWidth;
                }
                borderColor = styleManager.getColorStyle(elementStyle, StyleProperties.BORDER_COLOR);
                if (borderColor == null) {
                    borderColor = BLACK;
                }
            }

            /*
             * This has been tested in Linux and Windows, results may vary. The
             * rounded rectangle is not noticeable for adjacent states of the
             * same color until width=6 (arc=3) with antialiasing, or width=8
             * (arc=4) without antialiasing.
             *
             * In other words, only draw rounded rectangles if the arc is
             * noticeable.
             */
            if (arc >= 2) {
                last = new DeferredState(drawRect, bgColor, Objects.requireNonNull(borderColor), arc, lineWidth, fLabelsVisible ? event.getLabel() : null);
                states.add(last);
            } else {
                DeferredTinyState tinyCandidate = new DeferredTinyState(drawRect, bgColor, Objects.requireNonNull(borderColor), lineWidth);
                boolean skipState = false;
                if (!states.isEmpty()) {
                    DeferredItem prev = states.get(states.size() - 1);
                    if (prev instanceof DeferredTinyState) {
                        DeferredTinyState tinyState = (DeferredTinyState) prev;
                        if (fBlendSubPixelEvents) {
                            skipState = tinyState.squash(tinyCandidate);
                        } else if (tinyState.extend(tinyCandidate)) {
                            skipState = true;
                        }
                    }
                }
                if (!skipState) {
                    states.add(tinyCandidate);
                }
                last = tinyCandidate;
            }
        }

        if (reallySelected) {
            fSelectedRectangles.add(drawRect);
        }
        if (!visible) {
            addPoint(fPoints, rect.x, rect.y - 2);
        }
        if (visible && !Boolean.TRUE.equals(styleManager.getStyle(elementStyle, StyleProperties.annotated())) && last != null) {
            last.add(new PostDrawEvent(event, drawRect));
        }
        return visible && !event.isPropertyActive(CoreFilterProperty.DIMMED);
    }

    private static void addPoint(List<DeferredSegment> points, int x, int y) {
        if (!points.isEmpty()) {
            DeferredSegment point = points.get(points.size() - 1);

            if (point.contains(x, y)) {
                point.extend(x);
                return;
            }
        }
        points.add(new DeferredSegment(x, y));
    }

    private StyleManager getStyleManager() {
        return (fTimeGraphProvider instanceof IStylePresentationProvider) ? ((IStylePresentationProvider) fTimeGraphProvider).getStyleManager() : StyleManager.empty();
    }

    private @Nullable OutputElementStyle getElementStyle(@NonNull ITimeEvent event) {
        OutputElementStyle elementStyle;
        if (fTimeGraphProvider instanceof ITimeGraphStylePresentationProvider) {
            ITimeGraphStylePresentationProvider provider = (ITimeGraphStylePresentationProvider) fTimeGraphProvider;
            elementStyle = provider.getElementStyle(event);
            if (elementStyle == null) {
                return null;
            }
            if (elementStyle.getParentKey() == null && elementStyle.getStyleValues().isEmpty()) {
                return elementStyle;
            }
            return new OutputElementStyle(elementStyle.getParentKey(), applyEventStyleProperties(new HashMap<>(elementStyle.getStyleValues()), event));
        }
        if (!(event instanceof MarkerEvent)) {
            int colorIdx = fTimeGraphProvider.getStateTableIndex(event);
            if (colorIdx == ITimeGraphPresentationProvider.INVISIBLE) {
                return null;
            }
            if (colorIdx == ITimeGraphPresentationProvider.TRANSPARENT) {
                return new OutputElementStyle(null, new HashMap<>());
            }
        }
        Map<@NonNull String, @NonNull Object> styleMap = new HashMap<>(fTimeGraphProvider.getEventStyle(event));
        return new OutputElementStyle(null, applyEventStyleProperties(styleMap, event));
    }

    private static @NonNull Map<@NonNull String, @NonNull Object> applyEventStyleProperties(@NonNull Map<@NonNull String, @NonNull Object> styleMap, ITimeEvent event) {
        if (event.isPropertyActive(CoreFilterProperty.DIMMED)) {
            float opacity = (float) styleMap.getOrDefault(StyleProperties.OPACITY, 1.0f);
            styleMap.put(StyleProperties.OPACITY, opacity / DIMMED_ALPHA_COEFFICIENT);
            styleMap.put(StyleProperties.annotated(), Boolean.TRUE);
        }
        if (event.isPropertyActive(CoreFilterProperty.BOUND)) {
            styleMap.put(StyleProperties.BORDER_COLOR, HIGHLIGHTED_BOUND_COLOR);
            styleMap.put(StyleProperties.BORDER_WIDTH, HIGHLIGHTED_BOUND_WIDTH);
            styleMap.put(StyleProperties.BORDER_STYLE, BorderStyle.SOLID);
            styleMap.put(StyleProperties.annotated(), Boolean.FALSE);
        }
        return styleMap;
    }

    /**
     * Fill an item's states rectangle
     *
     * @param rect
     *            The states rectangle
     * @param gc
     *            Graphics context
     * @param selected
     *            true if the item is selected
     */
    protected void fillSpace(Rectangle rect, GC gc, boolean selected) {
        /* Nothing to draw */
    }

    /**
     * Draw a line at the middle height of a rectangle
     *
     * @param rect
     *            The rectangle
     * @param gc
     *            Graphics context
     */
    private void drawMidLine(Rectangle rect, GC gc) {
        gc.setForeground(getColorScheme().getColor(TimeGraphColorScheme.MID_LINE));
        int midy = rect.y + rect.height / 2;
        gc.drawLine(rect.x, midy, rect.x + rect.width, midy);
    }

    @Override
    public void keyTraversed(TraverseEvent e) {
        if ((e.detail == SWT.TRAVERSE_TAB_NEXT) || (e.detail == SWT.TRAVERSE_TAB_PREVIOUS)) {
            e.doit = true;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int idx = -1;
        if (fItemData.fExpandedItems.length == 0) {
            return;
        }
        if (SWT.HOME == e.keyCode) {
            idx = 0;
        } else if (SWT.END == e.keyCode) {
            idx = fItemData.fExpandedItems.length - 1;
        } else if (SWT.ARROW_DOWN == e.keyCode) {
            idx = getSelectedIndex();
            if (idx < 0) {
                idx = 0;
            } else {
                // Skip invisible items
                while (idx < fItemData.fExpandedItems.length - 1) {
                    idx++;
                    if (fItemData.fExpandedItems[idx].fItemHeight > 0) {
                        break;
                    }
                }
            }
        } else if (SWT.ARROW_UP == e.keyCode) {
            idx = getSelectedIndex();
            if (idx < 0) {
                idx = 0;
            } else {
                // Skip invisible items
                while (idx > 0) {
                    idx--;
                    if (fItemData.fExpandedItems[idx].fItemHeight > 0) {
                        break;
                    }
                }
            }
        } else if (SWT.ARROW_LEFT == e.keyCode && fDragState == DRAG_NONE) {
            boolean extend = (e.stateMask & SWT.SHIFT) != 0;
            selectPrevEvent(extend);
        } else if (SWT.ARROW_RIGHT == e.keyCode && fDragState == DRAG_NONE) {
            boolean extend = (e.stateMask & SWT.SHIFT) != 0;
            selectNextEvent(extend);
        } else if (SWT.PAGE_DOWN == e.keyCode) {
            int page = countPerPage();
            idx = getSelectedIndex();
            if (idx < 0) {
                idx = 0;
            }
            idx += page;
            if (idx >= fItemData.fExpandedItems.length) {
                idx = fItemData.fExpandedItems.length - 1;
            }
        } else if (SWT.PAGE_UP == e.keyCode) {
            int page = countPerPage();
            idx = getSelectedIndex();
            if (idx < 0) {
                idx = 0;
            }
            idx -= page;
            if (idx < 0) {
                idx = 0;
            }
        } else if (SWT.CR == e.keyCode) {
            idx = getSelectedIndex();
            if (idx >= 0) {
                if (fItemData.fExpandedItems[idx].fHasChildren) {
                    toggle(idx);
                } else {
                    fireDefaultSelection();
                }
            }
            idx = -1;
        } else if ((e.character == '+' || e.character == '=') && ((e.stateMask & SWT.CTRL) != 0)) {
            fVerticalZoomAlignEntry = getVerticalZoomAlignSelection();
            verticalZoom(true);
            if (fVerticalZoomAlignEntry != null) {
                setElementPosition(fVerticalZoomAlignEntry.getKey(), fVerticalZoomAlignEntry.getValue());
            }
        } else if (e.character == '-' && ((e.stateMask & SWT.CTRL) != 0)) {
            fVerticalZoomAlignEntry = getVerticalZoomAlignSelection();
            verticalZoom(false);
            if (fVerticalZoomAlignEntry != null) {
                setElementPosition(fVerticalZoomAlignEntry.getKey(), fVerticalZoomAlignEntry.getValue());
            }
        } else if (e.character == '0' && ((e.stateMask & SWT.CTRL) != 0)) {
            fVerticalZoomAlignEntry = getVerticalZoomAlignSelection();
            resetVerticalZoom();
            if (fVerticalZoomAlignEntry != null) {
                setElementPosition(fVerticalZoomAlignEntry.getKey(), fVerticalZoomAlignEntry.getValue());
            }
        } else if ((e.character == '+' || e.character == '=') && ((e.stateMask & SWT.CTRL) == 0)) {
            if (fHasNamespaceFocus) {
                ITimeGraphEntry entry = getSelectedTrace();
                setExpandedState(entry, 0, true);
            }
        } else if (e.character == '-' && ((e.stateMask & SWT.CTRL) == 0)) {
            if (fHasNamespaceFocus) {
                ITimeGraphEntry entry = getSelectedTrace();
                if ((entry != null) && entry.hasChildren()) {
                    setExpandedState(entry, -1, false);
                }
            }
        } else if ((e.character == '*') && ((e.stateMask & SWT.CTRL) == 0)) {
            if (fHasNamespaceFocus) {
                ITimeGraphEntry entry = getSelectedTrace();
                if ((entry != null) && entry.hasChildren()) {
                    setExpandedStateLevel(entry);
                }
            }
        }
        if (idx >= 0) {
            selectItem(idx, false);
            fireSelectionChanged();
        }
        int x = toControl(e.display.getCursorLocation()).x;
        updateCursor(x, e.stateMask | e.keyCode);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int x = toControl(e.display.getCursorLocation()).x;
        updateCursor(x, e.stateMask & ~e.keyCode);
    }

    @Override
    public void focusGained(FocusEvent e) {
        fIsInFocus = true;
        redraw();
        updateStatusLine(STATUS_WITHOUT_CURSOR_TIME);
    }

    @Override
    public void focusLost(FocusEvent e) {
        fIsInFocus = false;
        if (DRAG_NONE != fDragState) {
            setCapture(false);
            fDragState = DRAG_NONE;
        }
        redraw();
        updateStatusLine(NO_STATUS);
    }

    /**
     * @return If the current view is focused
     */
    public boolean isInFocus() {
        return fIsInFocus;
    }

    /**
     * Provide the possibility to control the wait cursor externally e.g. data
     * requests in progress
     *
     * @param waitInd
     *            Should we wait indefinitely?
     */
    public void waitCursor(boolean waitInd) {
        // Update cursor as indicated
        if (waitInd) {
            setCursor(fWaitCursor);
        } else {
            setCursor(null);
        }
    }

    private void updateCursor(int x, int stateMask) {
        // if Wait cursor not active, check for the need to change the cursor
        if (getCursor() == fWaitCursor) {
            return;
        }
        Cursor cursor = null;
        if (fDragState == DRAG_SPLIT_LINE) {
            // Nothing done.
        } else if (fDragState == DRAG_SELECTION) {
            cursor = fResizeCursor;
        } else if (fDragState == DRAG_TRACE_ITEM) {
            cursor = fDragCursor;
        } else if (fDragState == DRAG_ZOOM) {
            cursor = fZoomCursor;
        } else if ((stateMask & SWT.MODIFIER_MASK) == SWT.CTRL) {
            cursor = fDragCursor;
        } else if ((stateMask & SWT.MODIFIER_MASK) == SWT.SHIFT) {
            cursor = fResizeCursor;
        } else if (!isOverSplitLine(x)) {
            long selectionBegin = fTimeProvider.getSelectionBegin();
            long selectionEnd = fTimeProvider.getSelectionEnd();
            int xBegin = getXForTime(selectionBegin);
            int xEnd = getXForTime(selectionEnd);
            if (Math.abs(x - xBegin) < SNAP_WIDTH || Math.abs(x - xEnd) < SNAP_WIDTH) {
                cursor = fResizeCursor;
            }
        }
        if (getCursor() != cursor) {
            setCursor(cursor);
        }
    }

    /**
     * Update the status line following a change of selection.
     *
     * @since 2.0
     */
    public void updateStatusLine() {
        updateStatusLine(STATUS_WITHOUT_CURSOR_TIME);
    }

    private void updateStatusLine(int x) {
        // use the time provider of the time graph scale for the status line
        ITimeDataProvider tdp = fTimeGraphScale.getTimeProvider();
        if (fStatusLineManager == null || null == tdp ||
                tdp.getTime0() == tdp.getTime1()) {
            return;
        }

        long cursorTime = -1;
        long selectionBeginTime = 0;
        long selectionEndTime = 0;
        if ((x >= 0 || x == STATUS_WITHOUT_CURSOR_TIME) && fDragState == DRAG_NONE) {
            if (x != STATUS_WITHOUT_CURSOR_TIME) {
                long time = getTimeAtX(x);
                if (time >= 0) {
                    if (tdp instanceof ITimeDataProviderConverter) {
                        time = ((ITimeDataProviderConverter) tdp).convertTime(time);
                    }
                    cursorTime = time;
                }
            }
            selectionBeginTime = tdp.getSelectionBegin();
            selectionEndTime = tdp.getSelectionEnd();
        } else if (fDragState == DRAG_SELECTION || fDragState == DRAG_ZOOM) {
            long time0 = fDragBeginMarker ? getTimeAtX(fDragX0) : fDragTime0;
            long time = fDragBeginMarker ? fDragTime0 : getTimeAtX(fDragX);
            if (tdp instanceof ITimeDataProviderConverter) {
                time0 = ((ITimeDataProviderConverter) tdp).convertTime(time0);
                time = ((ITimeDataProviderConverter) tdp).convertTime(time);
            }
            // Use the time of T2 to update the cursor time
            cursorTime = time;
            selectionBeginTime = time0;
            selectionEndTime = time;
        }
        String message = buildStatusMessage(cursorTime, selectionBeginTime, selectionEndTime, tdp.getTimeFormat().convert(), Resolution.NANOSEC);
        fStatusLineManager.setMessage(message);
    }

    private static String buildStatusMessage(long cursorTime, long selectionBeginTime, long selectionEndTime, TimeFormat tf, Resolution res) {
        StringBuilder message = new StringBuilder();

        if (cursorTime >= 0) {
            message.append(NLS.bind("T: {0}{1}     ", //$NON-NLS-1$
                    new Object[] {
                            tf == TimeFormat.CALENDAR ? FormatTimeUtils.formatDate(cursorTime) + ' ' : "", //$NON-NLS-1$
                            FormatTimeUtils.formatTime(cursorTime, tf, res)
                    }));
        }

        message.append(NLS.bind("T1: {0}{1}", //$NON-NLS-1$
                new Object[] {
                        tf == TimeFormat.CALENDAR ? FormatTimeUtils.formatDate(selectionBeginTime) + ' ' : "", //$NON-NLS-1$
                        FormatTimeUtils.formatTime(selectionBeginTime, tf, res)
                }));

        if (selectionBeginTime != selectionEndTime) {
            message.append(NLS.bind("     T2: {0}{1}     \u0394: {2}", //$NON-NLS-1$
                    new Object[] {
                            tf == TimeFormat.CALENDAR ? FormatTimeUtils.formatDate(selectionEndTime) + ' ' : "", //$NON-NLS-1$
                            FormatTimeUtils.formatTime(selectionEndTime, tf, res),
                            FormatTimeUtils.formatDelta(selectionEndTime - selectionBeginTime, tf, res)
                    }));
        }
        return message.toString();
    }

    @Override
    public void mouseMove(MouseEvent e) {
        if (null == fTimeProvider) {
            return;
        }
        Point size = getSize();
        if (DRAG_TRACE_ITEM == fDragState) {
            int nameWidth = fTimeProvider.getNameSpace();
            if (e.x > nameWidth && size.x > nameWidth && fDragX != e.x) {
                fDragX = e.x;
                long timeDelta = getTimeAtX(fDragX) - getTimeAtX(fDragX0);
                long time1 = fTime1bak - timeDelta;
                long maxTime = fTimeProvider.getMaxTime();
                if (time1 > maxTime) {
                    time1 = maxTime;
                }
                long time0 = time1 - (fTime1bak - fTime0bak);
                if (time0 < fTimeProvider.getMinTime()) {
                    time0 = fTimeProvider.getMinTime();
                    time1 = time0 + (fTime1bak - fTime0bak);
                }
                fTimeProvider.setStartFinishTimeNotify(time0, time1);
                setElementPosition(fDragEntry, e.y);
            }
        } else if (DRAG_SPLIT_LINE == fDragState) {
            fDragX = e.x;
            fTimeProvider.setNameSpace(e.x);
            TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(this, getTimeViewAlignmentInfo()));
        } else if (DRAG_SELECTION == fDragState) {
            if (fDragBeginMarker) {
                fDragX0 = Math.min(Math.max(e.x, fTimeProvider.getNameSpace()), size.x - RIGHT_MARGIN);
            } else {
                fDragX = Math.min(Math.max(e.x, fTimeProvider.getNameSpace()), size.x - RIGHT_MARGIN);
            }
            redraw();
            fTimeGraphScale.setDragRange(fDragX0, fDragX);
            fireDragSelectionChanged(getTimeAtX(fDragX0), getTimeAtX(fDragX));
        } else if (DRAG_ZOOM == fDragState) {
            fDragX = Math.min(Math.max(e.x, fTimeProvider.getNameSpace()), size.x - RIGHT_MARGIN);
            redraw();
            fTimeGraphScale.setDragRange(fDragX0, fDragX);
        } else if (DRAG_NONE == fDragState) {
            boolean mouseOverSplitLine = isOverSplitLine(e.x);
            if (fMouseOverSplitLine != mouseOverSplitLine) {
                redraw();
            }
            fMouseOverSplitLine = mouseOverSplitLine;
        }

        if (e.x >= fTimeProvider.getNameSpace()) {
            fHasNamespaceFocus = false;
        } else {
            fHasNamespaceFocus = true;
        }
        updateCursor(e.x, e.stateMask);
        updateStatusLine(e.x);
    }

    @Override
    public void mouseDoubleClick(MouseEvent e) {
        if (null == fTimeProvider) {
            return;
        }
        if (1 == e.button && (e.stateMask & SWT.BUTTON_MASK) == 0) {
            if (isOverSplitLine(e.x) && fTimeProvider.getNameSpace() != 0) {
                fTimeProvider.setNameSpace(fIdealNameSpace);
                boolean mouseOverSplitLine = isOverSplitLine(e.x);
                if (fMouseOverSplitLine != mouseOverSplitLine) {
                    redraw();
                }
                fMouseOverSplitLine = mouseOverSplitLine;
                TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(this, getTimeViewAlignmentInfo()));
                return;
            }
            int idx = getItemIndexAtY(e.y);
            if (idx >= 0) {
                selectItem(idx, false);
                fireDefaultSelection();
            }
        }
    }

    @Override
    public void mouseDown(MouseEvent e) {
        if (fDragState != DRAG_NONE) {
            return;
        }
        if (1 == e.button && (e.stateMask & SWT.MODIFIER_MASK) == 0) {
            int nameSpace = fTimeProvider.getNameSpace();
            if (nameSpace != 0 && isOverSplitLine(e.x)) {
                fDragState = DRAG_SPLIT_LINE;
                fDragButton = e.button;
                fDragX = e.x;
                fDragX0 = fDragX;
                redraw();
                updateCursor(e.x, e.stateMask);
                return;
            }
        }
        if (1 == e.button && ((e.stateMask & SWT.MODIFIER_MASK) == 0 || (e.stateMask & SWT.MODIFIER_MASK) == SWT.SHIFT)) {
            int nameSpace = fTimeProvider.getNameSpace();
            int idx = getItemIndexAtY(e.y);
            if (idx >= 0) {
                Item item = fItemData.fExpandedItems[idx];
                if (item.fHasChildren && e.x < nameSpace && e.x < MARGIN + (item.fLevel + 1) * EXPAND_SIZE) {
                    toggle(idx);
                    return;
                }
                selectItem(idx, false);
                fireSelectionChanged();
            } else {
                selectItem(idx, false); // clear selection
                fireSelectionChanged();
            }
        } else if (3 == e.button && e.x < fTimeProvider.getNameSpace()) {
            int idx = getItemIndexAtY(e.y);
            selectItem(idx, false);
            fireSelectionChanged();
        }
        if (fTimeProvider == null ||
                fTimeProvider.getTime0() == fTimeProvider.getTime1() ||
                getWidth() - fTimeProvider.getNameSpace() <= 0) {
            return;
        }
        if (1 == e.button && ((e.stateMask & SWT.MODIFIER_MASK) == 0 || (e.stateMask & SWT.MODIFIER_MASK) == SWT.SHIFT)) {
            long hitTime = getTimeAtX(e.x);
            if (hitTime >= 0) {
                setCapture(true);

                fDragState = DRAG_SELECTION;
                fDragBeginMarker = false;
                fDragButton = e.button;
                fDragX = e.x;
                fDragX0 = fDragX;
                fDragTime0 = getTimeAtX(fDragX0);
                long selectionBegin = fTimeProvider.getSelectionBegin();
                long selectionEnd = fTimeProvider.getSelectionEnd();
                int xBegin = getXForTime(selectionBegin);
                int xEnd = getXForTime(selectionEnd);
                if ((e.stateMask & SWT.MODIFIER_MASK) == SWT.SHIFT) {
                    long time = getTimeAtX(e.x);
                    if (Math.abs(time - selectionBegin) < Math.abs(time - selectionEnd)) {
                        fDragBeginMarker = true;
                        fDragX = xEnd;
                        fDragX0 = e.x;
                        fDragTime0 = selectionEnd;
                    } else {
                        fDragX0 = xBegin;
                        fDragTime0 = selectionBegin;
                    }
                } else {
                    long time = getTimeAtX(e.x);
                    if (Math.abs(e.x - xBegin) < SNAP_WIDTH && Math.abs(time - selectionBegin) <= Math.abs(time - selectionEnd)) {
                        fDragBeginMarker = true;
                        fDragX = xEnd;
                        fDragX0 = e.x;
                        fDragTime0 = selectionEnd;
                    } else if (Math.abs(e.x - xEnd) < SNAP_WIDTH && Math.abs(time - selectionEnd) <= Math.abs(time - selectionBegin)) {
                        fDragX0 = xBegin;
                        fDragTime0 = selectionBegin;
                    }
                }
                fTime0bak = fTimeProvider.getTime0();
                fTime1bak = fTimeProvider.getTime1();
                redraw();
                updateCursor(e.x, e.stateMask);
                fTimeGraphScale.setDragRange(fDragX0, fDragX);
            }
        } else if (2 == e.button || (1 == e.button && (e.stateMask & SWT.MODIFIER_MASK) == SWT.CTRL)) {
            long hitTime = getTimeAtX(e.x);
            if (hitTime > 0) {
                setCapture(true);
                fDragState = DRAG_TRACE_ITEM;
                fDragButton = e.button;
                fDragX = e.x;
                fDragX0 = fDragX;
                fDragEntry = getExpandedElement(getItemIndexAtY(e.y));
                fTime0bak = fTimeProvider.getTime0();
                fTime1bak = fTimeProvider.getTime1();
                updateCursor(e.x, e.stateMask);
            }
        } else if (3 == e.button) {
            if (e.x >= fTimeProvider.getNameSpace()) {
                setCapture(true);
                fDragX = Math.min(Math.max(e.x, fTimeProvider.getNameSpace()), getWidth() - RIGHT_MARGIN);
                fDragX0 = fDragX;
                fDragTime0 = getTimeAtX(fDragX0);
                fDragState = DRAG_ZOOM;
                fDragButton = e.button;
                redraw();
                updateCursor(e.x, e.stateMask);
                fTimeGraphScale.setDragRange(fDragX0, fDragX);
            }
        }
    }

    @Override
    public void mouseUp(MouseEvent e) {
        if (fPendingMenuDetectEvent != null && e.button == 3) {
            if ((fDragState == DRAG_ZOOM) && isInDragZoomMargin()) {
                // Select entry and time event for single click
                long time = getTimeAtX(fDragX0);
                fTimeProvider.setSelectionRangeNotify(time, time, false);
                int idx = getItemIndexAtY(e.y);
                selectItem(idx, false);
                fireSelectionChanged();
            }
            menuDetected(fPendingMenuDetectEvent);
        }
        if (DRAG_NONE != fDragState) {
            setCapture(false);
            if (e.button == fDragButton && DRAG_TRACE_ITEM == fDragState) {
                fDragState = DRAG_NONE;
                fDragEntry = null;
                if (fDragX != fDragX0) {
                    fTimeProvider.notifyStartFinishTime();
                }
            } else if (e.button == fDragButton && DRAG_SPLIT_LINE == fDragState) {
                fDragState = DRAG_NONE;
                redraw();
            } else if (e.button == fDragButton && DRAG_SELECTION == fDragState) {
                fDragState = DRAG_NONE;
                if (fDragX == fDragX0) { // click without selecting anything
                    long time = getTimeAtX(e.x);
                    fTimeProvider.setSelectedTimeNotify(time, false);
                } else {
                    long time0 = fDragBeginMarker ? getTimeAtX(fDragX0) : fDragTime0;
                    long time1 = fDragBeginMarker ? fDragTime0 : getTimeAtX(fDragX);
                    fTimeProvider.setSelectionRangeNotify(time0, time1, false);
                }
                redraw();
                fTimeGraphScale.setDragRange(-1, -1);
            } else if (e.button == fDragButton && DRAG_ZOOM == fDragState) {
                fDragState = DRAG_NONE;
                int nameWidth = fTimeProvider.getNameSpace();
                if ((Math.max(fDragX, fDragX0) > nameWidth) && !isInDragZoomMargin()) {
                    long time0 = getTimeAtX(fDragX0);
                    long time1 = getTimeAtX(fDragX);
                    if (time0 < time1) {
                        fTimeProvider.setStartFinishTimeNotify(time0, time1);
                    } else {
                        fTimeProvider.setStartFinishTimeNotify(time1, time0);
                    }
                } else {
                    redraw();
                }
                fTimeGraphScale.setDragRange(-1, -1);
            }
        }
        updateCursor(e.x, e.stateMask);
        updateStatusLine(e.x);
    }

    @Override
    public void mouseEnter(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseExit(MouseEvent e) {
        if (fMouseOverSplitLine) {
            fMouseOverSplitLine = false;
            redraw();
        }
        updateStatusLine(STATUS_WITHOUT_CURSOR_TIME);
    }

    @Override
    public void mouseHover(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseScrolled(MouseEvent e) {
        if (fDragState != DRAG_NONE || e.count == 0) {
            return;
        }

        /*
         * On some platforms the mouse scroll event is sent to the control that
         * has focus even if it is not under the cursor. Handle the event only
         * if over the time graph control.
         */
        Point size = getSize();
        Rectangle bounds = new Rectangle(0, 0, size.x, size.y);
        if (!bounds.contains(e.x, e.y)) {
            return;
        }

        boolean horizontalZoom = false;
        boolean horizontalScroll = false;
        boolean verticalZoom = false;
        boolean verticalScroll = false;

        // over the time graph control
        if ((e.stateMask & SWT.MODIFIER_MASK) == (SWT.SHIFT | SWT.CTRL)) {
            verticalZoom = true;
        } else if (e.x < fTimeProvider.getNameSpace()) {
            // over the name space
            verticalScroll = true;
        } else {
            // over the state area
            if ((e.stateMask & SWT.MODIFIER_MASK) == SWT.CTRL) {
                // over the state area, CTRL pressed
                horizontalZoom = true;
            } else if ((e.stateMask & SWT.MODIFIER_MASK) == SWT.SHIFT) {
                // over the state area, SHIFT pressed
                horizontalScroll = true;
            } else {
                // over the state area, no modifier pressed
                verticalScroll = true;
            }
        }
        if (verticalZoom) {
            fVerticalZoomAlignEntry = getVerticalZoomAlignCursor(e.y);
            verticalZoom(e.count > 0);
            if (fVerticalZoomAlignEntry != null) {
                setElementPosition(fVerticalZoomAlignEntry.getKey(), fVerticalZoomAlignEntry.getValue());
            }
        } else if (horizontalZoom && fTimeProvider.getTime0() != fTimeProvider.getTime1()) {
            zoom(e.count > 0);
        } else if (horizontalScroll) {
            horizontalScroll(e.count > 0);
        } else if (verticalScroll) {
            setTopIndex(getTopIndex() - e.count);
        }
    }

    /**
     * Get the vertical zoom alignment entry and position based on the current
     * selection. If there is no selection or if the selection is not visible,
     * return an alignment entry with a null time graph entry.
     *
     * @return a map entry where the key is the selection's time graph entry and
     *         the value is the center y-coordinate of that entry, or null
     */
    private Entry<ITimeGraphEntry, Integer> getVerticalZoomAlignSelection() {
        Entry<ITimeGraphEntry, Integer> alignEntry = getVerticalZoomAlignOngoing();
        if (alignEntry != null) {
            return alignEntry;
        }
        int index = getSelectedIndex();
        if (index == -1 || index >= getExpandedElementCount()) {
            return new SimpleEntry<>(null, 0);
        }
        Rectangle bounds = getClientArea();
        Rectangle itemRect = getItemRect(bounds, index);
        if (itemRect.y < bounds.y || itemRect.y > bounds.y + bounds.height) {
            /* selection is not visible */
            return new SimpleEntry<>(null, 0);
        }
        ITimeGraphEntry entry = getExpandedElement(index);
        int y = itemRect.y + itemRect.height / 2;
        return new SimpleEntry<>(entry, y);
    }

    /**
     * Get the vertical zoom alignment entry and position at the specified
     * cursor position.
     *
     * @param y
     *            the cursor y-coordinate
     * @return a map entry where the key is the time graph entry under the
     *         cursor and the value is the cursor y-coordinate
     */
    private Entry<ITimeGraphEntry, Integer> getVerticalZoomAlignCursor(int y) {
        Entry<ITimeGraphEntry, Integer> alignEntry = getVerticalZoomAlignOngoing();
        if (alignEntry != null) {
            return alignEntry;
        }
        int index = getItemIndexAtY(y);
        if (index == -1) {
            index = getExpandedElementCount() - 1;
        }
        ITimeGraphEntry entry = getExpandedElement(index);
        return new SimpleEntry<>(entry, y);
    }

    /**
     * Get the vertical zoom alignment entry and position if there is an ongoing
     * one and we are within the vertical zoom delay, or otherwise return null.
     *
     * @return a map entry where the key is a time graph entry and the value is
     *         a y-coordinate, or null
     */
    private Entry<ITimeGraphEntry, Integer> getVerticalZoomAlignOngoing() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis < fVerticalZoomAlignTime + VERTICAL_ZOOM_DELAY) {
            /*
             * If the vertical zoom is triggered repeatedly in a short amount of
             * time, use the initial event's entry and position.
             */
            fVerticalZoomAlignTime = currentTimeMillis;
            return fVerticalZoomAlignEntry;
        }
        fVerticalZoomAlignTime = currentTimeMillis;
        return null;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.type == SWT.MouseWheel) {
            // prevent horizontal scrolling when the mouse wheel is used to
            // scroll vertically or zoom
            event.doit = false;
        }
    }

    @Override
    public int getBorderWidth() {
        return fBorderWidth;
    }

    /**
     * Set the border width
     *
     * @param borderWidth
     *            The width
     */
    public void setBorderWidth(int borderWidth) {
        this.fBorderWidth = borderWidth;
    }

    /**
     * @return The current height of the header row
     */
    public int getHeaderHeight() {
        return fHeaderHeight;
    }

    /**
     * Set the height of the header row
     *
     * @param headerHeight
     *            The height
     */
    public void setHeaderHeight(int headerHeight) {
        this.fHeaderHeight = headerHeight;
    }

    /**
     * @return The default height of regular item rows
     */
    public int getItemHeight() {
        return fGlobalItemHeight;
    }

    /**
     * Set the default height of regular item rows.
     *
     * @param rowHeight
     *            The height
     */
    public void setItemHeight(int rowHeight) {
        this.fGlobalItemHeight = rowHeight;
        for (Item item : fItemData.fItems) {
            item.fItemHeight = rowHeight;
        }
    }

    /**
     * Set the height of a specific item. Overrides the default item height.
     *
     * @param entry
     *            A time graph entry
     * @param rowHeight
     *            The height
     * @return true if the height is successfully stored, false otherwise
     */
    public boolean setItemHeight(ITimeGraphEntry entry, int rowHeight) {
        Item item = fItemData.findItem(entry);
        if (item != null) {
            item.fItemHeight = rowHeight;
            return true;
        }
        return false;
    }

    /**
     * Set the minimum item width
     *
     * @param width
     *            The minimum width
     */
    public void setMinimumItemWidth(int width) {
        this.fMinimumItemWidth = width;
    }

    /**
     * @return The minimum item width
     */
    public int getMinimumItemWidth() {
        return fMinimumItemWidth;
    }

    /**
     * Set whether all time events with a duration shorter than one pixel should
     * be blended in. If false, only the first such time event will be drawn and
     * the subsequent time events in the same pixel will be discarded. The
     * default value is false.
     *
     * @param blend
     *            true if sub-pixel events should be blended, false otherwise.
     * @since 1.1
     */
    public void setBlendSubPixelEvents(boolean blend) {
        fBlendSubPixelEvents = blend;
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        if (listener != null && !fSelectionChangedListeners.contains(listener)) {
            fSelectionChangedListeners.add(listener);
        }
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        if (listener != null) {
            fSelectionChangedListeners.remove(listener);
        }
    }

    @Override
    public void setSelection(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object ob = ((IStructuredSelection) selection).getFirstElement();
            if (ob instanceof ITimeGraphEntry) {
                selectItem((ITimeGraphEntry) ob, false);
            }
        }

    }

    /**
     * Add a new viewer filter object
     *
     * @param filter
     *            The filter object to be attached to the view
     * @since 3.1
     */
    public void addFilter(@NonNull ViewerFilter filter) {
        fFilters.add(filter);
        fireFiltersAdded(Collections.singleton(filter));
    }

    /**
     * Change the viewer filter object
     *
     * The filter elements are already updated, we only let the listener know
     * that a change happened at this point
     *
     * @param filter
     *            The filter object to be attached to the view
     * @since 3.2
     */
    public void changeFilter(@NonNull ViewerFilter filter) {
        fireFiltersChanged(Collections.singleton(filter));
    }

    /**
     * Remove a viewer filter object
     *
     * @param filter
     *            The filter object to be attached to the view
     */
    public void removeFilter(@NonNull ViewerFilter filter) {
        fFilters.remove(filter);
        fireFiltersRemoved(Collections.singleton(filter));
    }

    /**
     * Returns this control's filters.
     *
     * @return an array of viewer filters
     * @since 1.2
     */
    public @NonNull ViewerFilter[] getFilters() {
        return Iterables.toArray(fFilters, ViewerFilter.class);
    }

    /**
     * Sets the filters, replacing any previous filters.
     *
     * @param filters
     *            an array of viewer filters, or null
     * @since 1.2
     */
    public void setFilters(@NonNull ViewerFilter[] filters) {
        fFilters.clear();
        if (filters != null) {
            List<@NonNull ViewerFilter> filtersList = Arrays.asList(filters);
            fFilters.addAll(filtersList);
            fireFiltersChanged(filtersList);
        }
    }

    @Override
    public void colorSettingsChanged(StateItem[] stateItems) {
        /* Destroy previous colors from the resource manager */
        if (fEventColorMap != null) {
            for (Color color : fEventColorMap) {
                fResourceManager.destroyColor(color.getRGB());
            }
        }
        if (stateItems != null) {
            fEventColorMap = new Color[stateItems.length];
            for (int i = 0; i < stateItems.length; i++) {
                fEventColorMap[i] = fResourceManager.createColor(stateItems[i].getStateColor());
            }
        } else {
            fEventColorMap = new Color[] {};
        }
        redraw();
    }

    private class ItemData {
        private Map<ITimeGraphEntry, Item> fItemMap = new LinkedHashMap<>();
        private Item[] fExpandedItems = new Item[0];
        private Item[] fItems = new Item[0];
        private ITimeGraphEntry[] fRootEntries = new ITimeGraphEntry[0];
        private List<ILinkEvent> fLinks = new ArrayList<>();
        private int[] fYSums = new int[0];
        public static final int UNSET_SUM = -1;

        public ItemData() {
            // Do nothing
        }

        public Item findItem(ITimeGraphEntry entry) {
            return fItemMap.get(entry);
        }

        public void resetYSums() {
            int[] ySums = new int[fExpandedItems.length];
            Arrays.fill(ySums, UNSET_SUM);
            fYSums = ySums;
        }

        public int findItemIndex(ITimeGraphEntry entry) {
            Item item = fItemMap.get(entry);
            if (item == null) {
                return -1;
            }
            return item.fExpandedIndex;
        }

        public void refreshData() {
            ITimeGraphEntry selection = getSelectedTrace();
            Map<ITimeGraphEntry, Item> itemMap = new LinkedHashMap<>();
            fMaxItemHeight = 0;
            for (int i = 0; i < fRootEntries.length; i++) {
                ITimeGraphEntry entry = fRootEntries[i];
                refreshData(itemMap, null, 0, entry);
            }
            fItemMap = itemMap;
            fItems = fItemMap.values().toArray(new Item[0]);
            updateExpandedItems();
            if (selection != null) {
                for (Item item : fExpandedItems) {
                    if (item.fEntry == selection) {
                        item.fSelected = true;
                        break;
                    }
                }
            }
        }

        private void refreshData(Map<ITimeGraphEntry, Item> itemMap, Item parent, int level, ITimeGraphEntry entry) {
            Item item = new Item(entry, entry.getName(), level);
            if (parent != null) {
                parent.fChildren.add(item);
            }
            if (fGlobalItemHeight == CUSTOM_ITEM_HEIGHT) {
                item.fItemHeight = fTimeGraphProvider.getItemHeight(entry);
            } else {
                item.fItemHeight = fGlobalItemHeight;
            }
            fMaxItemHeight = Math.max(fMaxItemHeight, item.fItemHeight);
            item.fItemHeight = Math.max(1, item.fItemHeight + fHeightAdjustment);
            itemMap.put(entry, item);
            if (entry.hasChildren()) {
                Item oldItem = fItemMap.get(entry);
                if (oldItem != null && oldItem.fHasChildren && level == oldItem.fLevel && entry.getParent() == oldItem.fEntry.getParent()) {
                    /* existing items keep their old expanded state */
                    item.fExpanded = oldItem.fExpanded;
                } else {
                    /*
                     * new items set the expanded state according to auto-expand
                     * level
                     */
                    item.fExpanded = fAutoExpandLevel == ALL_LEVELS || level < fAutoExpandLevel;
                }
                item.fHasChildren = true;
                for (ITimeGraphEntry child : entry.getChildren()) {
                    refreshData(itemMap, item, level + 1, child);
                }
            }
        }

        public void updateExpandedItems() {
            for (Item item : fItems) {
                item.fExpandedIndex = -1;
            }
            List<Item> expandedItemList = new ArrayList<>();
            for (int i = 0; i < fRootEntries.length; i++) {
                ITimeGraphEntry entry = fRootEntries[i];
                Item item = findItem(entry);
                refreshExpanded(expandedItemList, item);
            }

            if (Activator.getDefault().getPreferenceStore().getBoolean(ITmfUIPreferences.FILTER_EMPTY_ROWS) ? hasSavedFilters() : isHideEmptyRowsFilterActive()) {
                filterData(expandedItemList);
            }

            fExpandedItems = expandedItemList.toArray(new Item[0]);
            resetYSums();
            fTopIndex = Math.min(fTopIndex, Math.max(0, fExpandedItems.length - 1));
        }

        private boolean hasEvents(Item item) {
            ITimeGraphEntry entry = item.fEntry;
            return (!entry.hasTimeEvents() || (entry instanceof TimeGraphEntry && ((TimeGraphEntry) entry).hasZoomedEvents()));
        }

        private boolean hasMarkers(Item item) {
            ITimeGraphEntry entry = item.fEntry;
            List<IMarkerEvent> markers = fMarkers;

            for (IMarkerEvent marker : markers) {
                if (entry == marker.getEntry()) {
                    long t0 = marker.getTime();
                    long t1 = t0 + marker.getDuration();
                    if (t0 < fTimeProvider.getTime1() && t1 > fTimeProvider.getTime0()) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void filterData(List<Item> expandedItemList) {
            for (Item item : expandedItemList) {
                if (!hasEvents(item) && !hasMarkers(item)) {
                    item.fItemHeight = 0;
                }
            }
        }

        private void refreshExpanded(Collection<Item> expandedItemList, Item item) {
            // Check for filters
            boolean display = true;
            for (ViewerFilter filter : fFilters) {
                if (!filter.select(null, item.fEntry.getParent(), item.fEntry)) {
                    display = false;
                    break;
                }
            }
            if (display) {
                item.fExpandedIndex = expandedItemList.size();
                expandedItemList.add(item);
                if (item.fHasChildren && item.fExpanded) {
                    for (Item child : item.fChildren) {
                        refreshExpanded(expandedItemList, child);
                    }
                }
            }
        }

        public void refreshData(ITimeGraphEntry[] entries) {
            if (entries == null) {
                fRootEntries = null;
            } else {
                fRootEntries = Arrays.copyOf(entries, entries.length);
            }

            refreshData();
        }

        public void refreshArrows(List<ILinkEvent> events) {
            /* If links are null, reset the list */
            if (events != null) {
                fLinks = events;
            } else {
                fLinks = new ArrayList<>();
            }
        }

        public ITimeGraphEntry[] getEntries() {
            return fRootEntries;
        }
    }

    private class Item {
        private boolean fExpanded;
        private int fExpandedIndex;
        private boolean fSelected;
        private boolean fHasChildren;
        private int fItemHeight;
        private final int fLevel;
        private final List<Item> fChildren;
        private final String fName;
        private final ITimeGraphEntry fEntry;

        public Item(ITimeGraphEntry entry, String name, int level) {
            this.fEntry = entry;
            this.fName = name;
            this.fLevel = level;
            this.fChildren = new ArrayList<>();
        }

        @Override
        public String toString() {
            return fName;
        }
    }

    @Override
    public void menuDetected(MenuDetectEvent e) {
        if (null == fTimeProvider) {
            return;
        }
        /*
         * This flag indicates if menu was prevented from being shown below and
         * therefore must be made visible on callback from mouseUp().
         */
        boolean pendingEventCallback = fPendingMenuDetectEvent != null;
        Point p = toControl(e.x, e.y);
        if (e.detail == SWT.MENU_MOUSE && isOverTimeSpace(p.x, p.y)) {
            if (fPendingMenuDetectEvent == null) {
                /*
                 * Feature in Linux. The MenuDetectEvent is received before
                 * mouseDown. Store the event and trigger it later just before
                 * handling mouseUp. This allows for the method to detect if
                 * mouse is used to drag zoom.
                 */
                fPendingMenuDetectEvent = e;
                /*
                 * Prevent the platform to show the menu when returning. The
                 * menu will be shown (see below) when this method is called
                 * again during mouseUp().
                 */
                e.doit = false;
                return;
            }
            fPendingMenuDetectEvent = null;
            if (fDragState != DRAG_ZOOM || !isInDragZoomMargin()) {
                /*
                 * Don't show the menu on mouseUp() if a drag zoom is in
                 * progress with a drag range outside of the drag zoom margin,
                 * or if any other drag operation, or none, is in progress.
                 */
                e.doit = false;
                return;
            }
        } else {
            if (fDragState != DRAG_NONE) {
                /*
                 * Don't show the menu on keyboard menu or mouse menu outside of
                 * the time space if any drag operation is in progress.
                 */
                e.doit = false;
                return;
            }
        }
        int idx = getItemIndexAtY(p.y);
        if (idx >= 0 && idx < fItemData.fExpandedItems.length) {
            Item item = fItemData.fExpandedItems[idx];
            ITimeGraphEntry entry = item.fEntry;

            /* Send menu event for the time graph entry */
            e.doit = true;
            e.data = entry;
            fireMenuEventOnTimeGraphEntry(e);
            Menu menu = getMenu();
            if (pendingEventCallback && e.doit && (menu != null)) {
                menu.setVisible(true);
            }

            /* Send menu event for time event */
            if (entry.hasTimeEvents()) {
                ITimeEvent event = Utils.findEvent(entry, getTimeAtX(p.x), 2);
                if (event != null) {
                    e.doit = true;
                    e.data = event;
                    fireMenuEventOnTimeEvent(e);
                    menu = getMenu();
                    if (pendingEventCallback && e.doit && (menu != null)) {
                        menu.setVisible(true);
                    }
                }
            }
        }
    }

    /**
     * Perform the alignment operation.
     *
     * @param offset
     *            the alignment offset
     *
     * @see ITmfTimeAligned
     *
     * @since 1.0
     */
    public void performAlign(int offset) {
        fTimeProvider.setNameSpace(offset);
    }

    /**
     * Return the time alignment information
     *
     * @return the time alignment information
     *
     * @see ITmfTimeAligned
     *
     * @since 1.0
     */
    public TmfTimeViewAlignmentInfo getTimeViewAlignmentInfo() {
        return new TmfTimeViewAlignmentInfo(getShell(), toDisplay(0, 0), fTimeProvider.getNameSpace());
    }

    private boolean isInDragZoomMargin() {
        return (Math.abs(fDragX - fDragX0) < DRAG_MARGIN);
    }

    /**
     * Set the filtering status of the timegraph
     *
     * @param isActive
     *            True whether there is active filters, false otherwise
     * @since 4.0
     */
    public void setFilterActive(boolean isActive) {
        fFilterActive = isActive;
    }

    /**
     * Test if a time event filter is applied, basically it tells if the view is
     * in filter mode
     *
     * @return True if the view is in filter mode, false otherwise
     * @since 4.0
     *
     */
    public boolean isFilterActive() {
        return fFilterActive;
    }

    /**
     * Set whether filters have been saved or not.
     *
     * @param hasSavedFilter
     *            The saved filter status
     * @since 4.0
     */
    public void setSavedFilterStatus(boolean hasSavedFilter) {
        fHasSavedFilters = hasSavedFilter;
    }

    /**
     * Tells whether the timegraph has saved filters or not
     *
     * @return True whether there is saved filters, false otherwise
     * @since 4.0
     */
    public boolean hasSavedFilters() {
        return fHasSavedFilters;
    }

    /**
     * Tells whether the HideEmptyRows action is active or not
     *
     * @return True when the HideEmptyRows action is active, false otherwise
     * @since 6.2
     */
    public boolean isHideEmptyRowsFilterActive() {
        return fHideEmptyRowsFilterActive;
    }

    /**
     * Set whether the HideEmptyRows action is active or not
     *
     * @param hideEmptyRowsFilterActive
     *            true to make the HideEmptyRows action active
     * @since 6.2
     */
    public void setHideEmptyRowsFilterActive(boolean hideEmptyRowsFilterActive) {
        fHideEmptyRowsFilterActive = hideEmptyRowsFilterActive;
    }

    private final class TimeGraphControlListener implements ControlListener {
        @Override
        public void controlMoved(ControlEvent e) {
            resetCache();
        }

        @Override
        public void controlResized(ControlEvent e) {
            resetCache();
        }
    }

    private void resetCache() {
        fBounds = null;
        fSize = null;
    }
}
