/*******************************************************************************
 * Copyright (c) 2010, 2025 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Bernd Hufmann - Added supplementary files handling (in class TmfTraceElement)
 *   Geneviève Bastien - Copied supplementary files handling from TmfTracElement
 *                 Moved to this class code to copy a model element
 *                 Renamed from TmfWithFolderElement to TmfCommonProjectElement
 *   Patrick Tasse - Add support for folder elements
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.project.model;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.editors.ITmfEventsEditorConstants;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.io.ResourceUtil;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType.TraceElementType;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.traceeventlogger.LogUtils;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.osgi.framework.Bundle;

/**
 * Base class for tracing project elements: it implements the common behavior of
 * all project elements: supplementary files, analysis, types, etc.
 *
 * @author Geneviève Bastien
 */
public abstract class TmfCommonProjectElement extends TmfProjectModelElement {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private static final String BOOKMARKS_HIDDEN_FILE = ".bookmarks"; //$NON-NLS-1$

    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(TmfCommonProjectElement.class);

    /* Direct child elements */
    private TmfViewsElement fViewsElement = null;
    private TmfOnDemandAnalysesElement fOnDemandAnalysesElement = null;
    private TmfReportsElement fReportsElement = null;

    /** This trace type ID as defined in plugin.xml */
    private String fTraceTypeId = null;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor. Creates model element.
     *
     * @param name
     *            The name of the element
     * @param resource
     *            The resource.
     * @param parent
     *            The parent element
     */
    public TmfCommonProjectElement(String name, IResource resource, TmfProjectModelElement parent) {
        super(name, resource, parent);
        refreshTraceType();
    }

    // ------------------------------------------------------------------------
    // ITmfProjectModelElement
    // ------------------------------------------------------------------------

    /**
     * @since 2.3
     */
    // TODO: Remove this method when major version changes
    @Override
    public void dispose() {
        super.dispose();
    }

    /**
     * @since 2.0
     */
    @Override
    protected synchronized void refreshChildren() {
        /* Get the base path to put the resource to */
        IPath tracePath = getResource().getFullPath();
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        if (this.getParent() instanceof TmfExperimentElement) {
            return;
        }

        if (TmfTraceType.getTraceType(getTraceType()) == null) {
            if (fViewsElement != null) {
                removeChild(fViewsElement);
                fViewsElement.dispose();
                fViewsElement = null;
            }
            if (fOnDemandAnalysesElement != null) {
                removeChild(fOnDemandAnalysesElement);
                fOnDemandAnalysesElement.dispose();
                fOnDemandAnalysesElement = null;
            }
            if (fReportsElement != null) {
                removeChild(fReportsElement);
                fReportsElement.dispose();
                fReportsElement = null;
            }
            return;
        }
        if (fViewsElement == null) {
            /* Add the "Views" node */
            IFolder viewsNodeRes = root.getFolder(tracePath.append(TmfViewsElement.PATH_ELEMENT));
            fViewsElement = new TmfViewsElement(viewsNodeRes, this);
            addChild(fViewsElement);
        }
        fViewsElement.refreshChildren();

        if (fOnDemandAnalysesElement == null) {
            /* Add the "On-demand Analyses" node */
            IFolder analysesNodeRes = root.getFolder(tracePath.append(TmfOnDemandAnalysesElement.PATH_ELEMENT));
            fOnDemandAnalysesElement = new TmfOnDemandAnalysesElement(analysesNodeRes, this);
            addChild(fOnDemandAnalysesElement);
        }
        fOnDemandAnalysesElement.refreshChildren();

        if (fReportsElement == null) {
            /* Add the "Reports" node */
            IFolder reportsNodeRes = root.getFolder(tracePath.append(TmfReportsElement.PATH_ELEMENT));
            fReportsElement = new TmfReportsElement(reportsNodeRes, this);
            addChild(fReportsElement);
        }
        fReportsElement.refreshChildren();
    }

    /**
     * @since 2.0
     */
    @Override
    public Image getIcon() {
        String traceType = getTraceType();
        if (traceType == null || TmfTraceType.getTraceType(traceType) == null) {
            // request the label to the Eclipse platform
            Image icon = TmfProjectModelIcons.WORKSPACE_LABEL_PROVIDER.getImage(getResource());
            return (icon == null ? TmfProjectModelIcons.DEFAULT_TRACE_ICON : icon);
        }

        IConfigurationElement traceUIAttributes = TmfTraceTypeUIUtils.getTraceUIAttributes(traceType,
                (this instanceof TmfTraceElement) ? TraceElementType.TRACE : TraceElementType.EXPERIMENT);
        if (traceUIAttributes != null) {
            String iconAttr = traceUIAttributes.getAttribute(TmfTraceTypeUIUtils.ICON_ATTR);
            if (iconAttr != null) {
                String name = traceUIAttributes.getContributor().getName();
                if (name != null) {
                    Bundle bundle = Platform.getBundle(name);
                    if (bundle != null) {
                        Image image = TmfProjectModelIcons.loadIcon(bundle, iconAttr);
                        if (image != null) {
                            return image;
                        }
                    }
                }
            }
        }
        /* Let subclasses specify an icon */
        return null;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Get the child element "Views". There should always be one.
     *
     * @return The child element
     * @since 2.0
     */
    protected TmfViewsElement getChildElementViews() {
        return fViewsElement;
    }

    /**
     * Get the child element "Reports".
     *
     * @return The Reports child element
     * @since 2.0
     */
    public TmfReportsElement getChildElementReports() {
        return fReportsElement;
    }

    /**
     * Returns the trace type ID.
     *
     * @return trace type ID.
     */
    public String getTraceType() {
        return fTraceTypeId;
    }

    /**
     * Refreshes the trace type field by reading the trace type persistent
     * property of the resource.
     */
    public void refreshTraceType() {
        try {
            fTraceTypeId = TmfTraceType.getTraceTypeId(getResource());
        } catch (CoreException e) {
            Activator.getDefault().logError(NLS.bind(Messages.TmfCommonProjectElement_ErrorRefreshingProperty, getName()), e);
        }
    }

    /**
     * Instantiate a <code>ITmfTrace</code> object based on the trace type and
     * the corresponding extension.
     *
     * @return the <code>ITmfTrace</code> or <code>null</code> for an error
     */
    public abstract ITmfTrace instantiateTrace();

    /**
     * Return the supplementary folder path for this element. The returned path
     * is relative to the project's supplementary folder.
     *
     * @return The supplementary folder path for this element
     */
    protected String getSupplementaryFolderPath() {
        return getElementPath() + getSuffix();
    }

    /**
     * Return the element path relative to its common element (traces folder,
     * experiments folder or experiment element).
     *
     * @return The element path
     */
    public @NonNull String getElementPath() {
        ITmfProjectModelElement parent = getParent();
        while (!(parent instanceof TmfTracesFolder || parent instanceof TmfExperimentElement || parent instanceof TmfExperimentFolder)) {
            parent = parent.getParent();
        }
        IPath path = getResource().getFullPath().makeRelativeTo(parent.getPath());
        return checkNotNull(path.toString());
    }

    /**
     * Return the element destination path relative to its common element (traces
     * folder, experiments folder or experiment element).
     *
     * @param destinationPath
     *            Full destination path
     *
     * @return The element destination path
     * @since 3.3
     */
    public @NonNull String getDestinationPathRelativeToParent(IPath destinationPath) {
        ITmfProjectModelElement parent = getParent();
        while (!(parent instanceof TmfTracesFolder || parent instanceof TmfExperimentElement || parent instanceof TmfExperimentFolder)) {
            parent = parent.getParent();
        }
        IPath path = destinationPath.makeRelativeTo(parent.getPath());
        return checkNotNull(path.toString());
    }

    /**
     * @return The suffix for the supplementary folder
     */
    protected String getSuffix() {
        return ""; //$NON-NLS-1$
    }

    /**
     * Returns a list of TmfTraceElements contained in project element.
     *
     * @return a list of TmfTraceElements, empty list if none
     */
    public List<TmfTraceElement> getTraces() {
        return new ArrayList<>();
    }

    /**
     * Get the instantiated trace associated with this element.
     *
     * @return The instantiated trace or null if trace is not (yet) available
     */
    public ITmfTrace getTrace() {
        for (ITmfTrace trace : TmfTraceManager.getInstance().getOpenedTraces()) {
            if (getResource().equals(trace.getResource())) {
                return trace;
            }
        }
        return null;
    }

    /**
     * Returns the file resource used to store bookmarks after creating it if
     * necessary. If the trace resource is a file, it is returned directly. If the
     * trace resource is a folder, a linked file is returned. The file will be
     * created if it does not exist.
     *
     * @param monitor
     *            the progress monitor
     * @return the bookmarks file
     * @throws CoreException
     *             if the bookmarks file cannot be created
     * @throws OperationCanceledException
     *             if the operation was canceled
     * @since 4.0
     */
    public IFile createBookmarksFile(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
        SubMonitor subMonitor = SubMonitor.convert(monitor);
        IFile file = getBookmarksFile();
        if (getResource() instanceof IFolder) {
            TmfTraceFolder tracesFolder = getProject().getTracesFolder();
            if (tracesFolder == null) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.TmfProject_TracesFolderNotExists));
            }
            return createBookmarksFile(tracesFolder.getResource(), ITmfEventsEditorConstants.TRACE_EDITOR_INPUT_TYPE, subMonitor);
        }
        return file;
    }

    /**
     * Actually returns the bookmark file or creates it in the project element's
     * folder
     *
     * @param bookmarksFolder
     *            Folder where to put the bookmark file
     * @param editorInputType
     *            The editor input type to set (trace or experiment)
     * @param monitor
     *            The progress monitor
     * @return The bookmark file
     * @throws CoreException
     *             if the bookmarks file cannot be created
     * @throws OperationCanceledException
     *             if the operation was canceled
     * @since 4.0
     */
    protected IFile createBookmarksFile(IFolder bookmarksFolder, String editorInputType, IProgressMonitor monitor) throws CoreException, OperationCanceledException {
        SubMonitor subMonitor = SubMonitor.convert(monitor, 2);
        IFile file = getBookmarksFile();
        if (!file.exists()) {
            final IFile bookmarksFile = bookmarksFolder.getFile(BOOKMARKS_HIDDEN_FILE);
            if (!bookmarksFile.exists()) {
                final InputStream source = new ByteArrayInputStream(new byte[0]);
                bookmarksFile.create(source, IResource.FORCE | IResource.HIDDEN, subMonitor.split(1));
            }
            file.createLink(bookmarksFile.getLocation(), IResource.REPLACE | IResource.HIDDEN, subMonitor.split(1));
        }
        file.setPersistentProperty(TmfCommonConstants.TRACETYPE, editorInputType);
        return file;
    }

    /**
     * Returns the optional editor ID from the trace type extension.
     *
     * @return the editor ID or <code>null</code> if not defined.
     */
    public abstract String getEditorId();

    /**
     * Returns the file resource used to store bookmarks. The file may not
     * exist.
     *
     * @return the bookmarks file
     */
    public IFile getBookmarksFile() {
        final IFolder folder = (IFolder) getResource();
        IFile file = folder.getFile(getName() + '_');
        return file;
    }

    /**
     * Close open editors associated with this experiment.
     */
    public void closeEditors() {
        IFile file = getBookmarksFile();
        FileEditorInput input = new FileEditorInput(file);
        IWorkbench wb = PlatformUI.getWorkbench();
        for (IWorkbenchWindow wbWindow : wb.getWorkbenchWindows()) {
            for (IWorkbenchPage wbPage : wbWindow.getPages()) {
                for (IEditorReference editorReference : wbPage.getEditorReferences()) {
                    try {
                        if (editorReference.getEditorInput().equals(input)) {
                            wbPage.closeEditor(editorReference.getEditor(false), false);
                        }
                    } catch (PartInitException e) {
                        Activator.getDefault().logError(NLS.bind(Messages.TmfCommonProjectElement_ErrorClosingEditor, getName()), e);
                    }
                }
            }
        }
    }

    /**
     * Get a friendly name for the type of element this common project element
     * is, to be displayed in UI messages.
     *
     * @return A string for the type of project element this object is, for
     *         example "trace" or "experiment"
     */
    public abstract String getTypeName();

    /**
     * Copy this model element
     *
     * @param newName
     *            The name of the new element
     * @param copySuppFiles
     *            Whether to copy supplementary files or not
     * @return the new Resource object
     */
    public IResource copy(final String newName, final boolean copySuppFiles) {
        return copy(newName, copySuppFiles, true);
    }

    /**
     * Copy this model element at the same place as this element
     * (ex./Traces/thisElementPath).
     *
     * @param newName
     *            The name of the new element
     * @param copySuppFiles
     *            Whether to copy supplementary files or not
     * @param copyAsLink
     *            Whether to copy as a link or not
     * @return the new Resource object
     * @since 3.3
     */
    public IResource copy(final String newName, final boolean copySuppFiles, final boolean copyAsLink) {
        final IPath newPath = getParent().getResource().getFullPath().addTrailingSeparator().append(newName);
        return copy(copySuppFiles, copyAsLink, newPath);
    }

    /**
     * Copy this model element to the destinationPath
     *
     * @param copySuppFiles
     *            Whether to copy supplementary files or not
     * @param copyAsLink
     *            Whether to copy as a link or not
     * @param destinationPath
     *            The path where the element will be copied
     * @return the new Resource object
     * @since 3.3
     */
    public IResource copy(final boolean copySuppFiles, final boolean copyAsLink, IPath destinationPath) {
        /* Copy supplementary files first, only if needed */
        if (copySuppFiles) {
            String newElementPath = getDestinationPathRelativeToParent(destinationPath);
            copySupplementaryFolder(newElementPath);
        }
        /* Copy the trace */
        try {
            int flags = IResource.FORCE;
            if (copyAsLink) {
                flags |= IResource.SHALLOW;
            }

            IResource trace = ResourceUtil.copyResource(getResource(), destinationPath, flags, null);

            /* Delete any bookmarks file found in copied trace folder */
            if (trace instanceof IFolder) {
                IFolder folderTrace = (IFolder) trace;
                for (IResource member : folderTrace.members()) {
                    String traceTypeId = TmfTraceType.getTraceTypeId(member);
                    if (ITmfEventsEditorConstants.TRACE_INPUT_TYPE_CONSTANTS.contains(traceTypeId)
                            || ITmfEventsEditorConstants.EXPERIMENT_INPUT_TYPE_CONSTANTS.contains(traceTypeId)) {
                        member.delete(true, null);
                    }
                }
            }
            return trace;
        } catch (CoreException e) {
            Activator.getDefault().logError("Error copying " + getName(), e); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Get the list of analysis elements
     *
     * @return Array of analysis elements
     */
    public List<@NonNull TmfAnalysisElement> getAvailableAnalysis() {
        TmfViewsElement viewsElement = getChildElementViews();
        if (viewsElement != null) {
            return viewsElement.getChildren().stream()
                    .filter(TmfAnalysisElement.class::isInstance)
                    .map(TmfAnalysisElement.class::cast)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * @since 3.0
     * @return list of children analysis elements
     */
    public List<TmfAnalysisElement> getAvailableChildrenAnalyses() {
        return Collections.emptyList();
    }

    // ------------------------------------------------------------------------
    // Supplementary files operations
    // ------------------------------------------------------------------------

    /**
     * Deletes this element specific supplementary folder.
     */
    public void deleteSupplementaryFolder() {
        IFolder supplFolder = getTraceSupplementaryFolder(getSupplementaryFolderPath());
        try {
            deleteFolder(supplFolder);
        } catch (CoreException e) {
            Activator.getDefault().logError("Error deleting supplementary folder " + supplFolder, e); //$NON-NLS-1$
        }
    }

    private static void deleteFolder(IFolder folder) throws CoreException {
        if (folder.exists()) {
            folder.delete(true, new NullProgressMonitor());
        }
        IContainer parent = folder.getParent();
        // delete empty folders up to the parent project
        if (parent instanceof IFolder && (!parent.exists() || parent.members().length == 0)) {
            deleteFolder((IFolder) parent);
        }
    }

    /**
     * Renames the element specific supplementary folder according to the new
     * element name or path.
     *
     * @param newElementPath
     *            The new element name or path
     */
    public void renameSupplementaryFolder(String newElementPath) {
        IFolder oldSupplFolder = getTraceSupplementaryFolder(getSupplementaryFolderPath());

        // Rename supplementary folder
        try {
            if (oldSupplFolder.exists()) {
                IFolder newSupplFolder = prepareTraceSupplementaryFolder(newElementPath + getSuffix(), false);
                oldSupplFolder.move(newSupplFolder.getFullPath(), true, new NullProgressMonitor());
            }
            deleteFolder(oldSupplFolder);
        } catch (CoreException e) {
            Activator.getDefault().logError("Error renaming supplementary folder " + oldSupplFolder, e); //$NON-NLS-1$
        }
    }

    /**
     * Copies the element specific supplementary folder to the new element name
     * or path.
     *
     * @param newElementPath
     *            The new element name or path
     */
    public void copySupplementaryFolder(String newElementPath) {
        IFolder oldSupplFolder = getTraceSupplementaryFolder(getSupplementaryFolderPath());

        // copy supplementary folder
        if (oldSupplFolder.exists()) {
            try {
                IFolder newSupplFolder = prepareTraceSupplementaryFolder(newElementPath + getSuffix(), false);
                oldSupplFolder.copy(newSupplFolder.getFullPath(), true, new NullProgressMonitor());
                // Temporary fix for Bug 532677: IResource.copy() does not copy the hidden flag
                hidePropertiesFolder(newSupplFolder);
            } catch (CoreException e) {
                Activator.getDefault().logError("Error copying supplementary folder " + oldSupplFolder, e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Copies the element specific supplementary folder a new folder.
     *
     * @param destination
     *            The destination folder to copy to.
     */
    public void copySupplementaryFolder(IFolder destination) {
        IFolder oldSupplFolder = getTraceSupplementaryFolder(getSupplementaryFolderPath());

        // copy supplementary folder
        if (oldSupplFolder.exists()) {
            try {
                TraceUtils.createFolder((IFolder) destination.getParent(), new NullProgressMonitor());
                oldSupplFolder.copy(destination.getFullPath(), true, new NullProgressMonitor());
                // Temporary fix for Bug 532677: IResource.copy() does not copy the hidden flag
                hidePropertiesFolder(destination);
            } catch (CoreException e) {
                Activator.getDefault().logError("Error copying supplementary folder " + oldSupplFolder, e); //$NON-NLS-1$
            }
        }
    }

    private static void hidePropertiesFolder(IFolder supplFolder) throws CoreException {
        IFolder propertiesFolder = supplFolder.getFolder(TmfCommonConstants.TRACE_PROPERTIES_FOLDER);
        if (propertiesFolder.exists()) {
            propertiesFolder.setHidden(true);
        }
    }

    /**
     * Refreshes the element specific supplementary folder information. It creates
     * the folder if not exists. It sets the persistence property of the trace
     * resource
     *
     * @param monitor
     *            the progress monitor
     * @since 4.0
     */
    public void refreshSupplementaryFolder(IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, 2);
        IFolder supplFolder = createSupplementaryFolder(subMonitor.split(1));
        try {
            supplFolder.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.split(1));
        } catch (CoreException e) {
            Activator.getDefault().logError("Error refreshing supplementary folder " + supplFolder, e); //$NON-NLS-1$
        }
    }

    /**
     * Refreshes the element specific supplementary folder information. It
     * creates the folder if not exists. It sets the persistence property of the
     * trace resource
     */
    public void refreshSupplementaryFolder() {
        refreshSupplementaryFolder(new NullProgressMonitor());
    }

    /**
     * Checks if supplementary resource exist or not.
     *
     * @return <code>true</code> if one or more files are under the element
     *         supplementary folder
     */
    public boolean hasSupplementaryResources() {
        IResource[] resources = getSupplementaryResources();
        return (resources.length > 0);
    }

    /**
     * Returns the supplementary resources under the trace supplementary folder.
     *
     * @return array of resources under the trace supplementary folder.
     */
    public IResource[] getSupplementaryResources() {
        IFolder supplFolder = getTraceSupplementaryFolder(getSupplementaryFolderPath());
        if (supplFolder.exists()) {
            try {
                return supplFolder.members();
            } catch (CoreException e) {
                Activator.getDefault().logError("Error deleting supplementary folder " + supplFolder, e); //$NON-NLS-1$
            }
        }
        return new IResource[0];
    }

    /**
     * Deletes the given resources.
     *
     * @param resources
     *            array of resources to delete.
     */
    public void deleteSupplementaryResources(IResource[] resources) {

        for (int i = 0; i < resources.length; i++) {
            try {
                resources[i].delete(true, new NullProgressMonitor());
                // Needed to audit for privacy concerns
                LogUtils.traceInstant(LOGGER, Level.CONFIG, "deleteSupplementaryResources", resources[i].getFullPath().toOSString() ); //$NON-NLS-1$
            } catch (CoreException e) {
                Activator.getDefault().logError("Error deleting supplementary resource " + resources[i], e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Deletes all supplementary resources in the supplementary directory
     */
    public void deleteSupplementaryResources() {
        deleteSupplementaryResources(getSupplementaryResources());
    }

    /**
     * Returns the trace specific supplementary folder under the project's
     * supplementary folder. The folder and its parent folders will be created if
     * they don't exist.
     *
     * @param monitor
     *            the progress monitor
     * @return the trace specific supplementary folder
     * @since 4.0
     */
    public IFolder prepareSupplementaryFolder(IProgressMonitor monitor) {
        return prepareTraceSupplementaryFolder(getSupplementaryFolderPath(), true, monitor);
    }

    private IFolder createSupplementaryFolder(IProgressMonitor monitor) {
        IFolder supplFolder = prepareTraceSupplementaryFolder(getSupplementaryFolderPath(), true, monitor);

        try {
            getResource().setPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, supplFolder.getLocation().toOSString());
        } catch (CoreException e) {
            Activator.getDefault().logError("Error setting persistant property " + TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, e); //$NON-NLS-1$
        }
        return supplFolder;
    }

}
