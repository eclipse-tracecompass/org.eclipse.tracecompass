/*******************************************************************************
 * Copyright (c) 2010, 2018 Ericsson
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
 *   Patrick Tasse - Add support for folder elements
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.project.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tracecompass.tmf.ui.properties.ReadOnlyTextPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;

/**
 * Implementation of model element representing the unique "Experiments" folder
 * in the project.
 * <p>
 *
 * @version 1.0
 * @author Francois Chouinard
 *
 */
public class TmfExperimentFolder extends TmfProjectModelElement implements IPropertySource2 {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    static final String EXPER_RESOURCE_NAME = "Experiments"; //$NON-NLS-1$

    // Property View stuff
    private static final String INFO_CATEGORY = "Info"; //$NON-NLS-1$
    private static final String NAME = "name"; //$NON-NLS-1$
    private static final String PATH = "path"; //$NON-NLS-1$
    private static final String LOCATION = "location"; //$NON-NLS-1$

    private static final ReadOnlyTextPropertyDescriptor NAME_DESCRIPTOR = new ReadOnlyTextPropertyDescriptor(NAME, NAME);
    private static final ReadOnlyTextPropertyDescriptor PATH_DESCRIPTOR = new ReadOnlyTextPropertyDescriptor(PATH, PATH);
    private static final ReadOnlyTextPropertyDescriptor LOCATION_DESCRIPTOR = new ReadOnlyTextPropertyDescriptor(LOCATION, LOCATION);

    private static final IPropertyDescriptor[] DESCRIPTORS = { NAME_DESCRIPTOR, PATH_DESCRIPTOR, LOCATION_DESCRIPTOR };

    static {
        NAME_DESCRIPTOR.setCategory(INFO_CATEGORY);
        PATH_DESCRIPTOR.setCategory(INFO_CATEGORY);
        LOCATION_DESCRIPTOR.setCategory(INFO_CATEGORY);
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor.
     * Creates a TmfExperimentFolder model element.
     * @param name The name of the folder
     * @param folder The folder reference
     * @param parent The parent (project element)
     */
    public TmfExperimentFolder(String name, IFolder folder, TmfProjectElement parent) {
        super(name, folder, parent);
    }

    // ------------------------------------------------------------------------
    // TmfProjectModelElement
    // ------------------------------------------------------------------------

    @Override
    public IFolder getResource() {
        return (IFolder) super.getResource();
    }

    /**
     * @since 2.0
     */
    @Override
    protected synchronized void refreshChildren() {
        IFolder folder = getResource();

        // Get the children from the model
        Map<String, ITmfProjectModelElement> childrenMap = new HashMap<>();
        for (ITmfProjectModelElement element : getChildren()) {
            childrenMap.put(element.getResource().getName(), element);
        }

        try {
            IResource[] members = folder.members();
            for (IResource resource : members) {
                if (resource instanceof IFolder) {
                    IFolder expFolder = (IFolder) resource;
                    String name = resource.getName();
                    ITmfProjectModelElement element = childrenMap.get(name);
                    if (element instanceof TmfExperimentElement) {
                        childrenMap.remove(name);
                    } else {
                        element = new TmfExperimentElement(name, expFolder, this);
                        addChild(element);
                    }
                    ((TmfExperimentElement) element).refreshChildren();
                }
            }
        } catch (CoreException e) {
        }

        // Cleanup dangling children from the model
        for (ITmfProjectModelElement danglingChild : childrenMap.values()) {
            removeChild(danglingChild);
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public Image getIcon() {
        return TmfProjectModelIcons.FOLDER_ICON;
    }

    /**
     * @since 2.0
     */
    @Override
    public String getLabelText() {
        return getName() + " [" + getChildren().size() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Returns a list of experiment model elements under the experiments folder.
     * @return list of experiment model elements
     */
    public List<TmfExperimentElement> getExperiments() {
        List<ITmfProjectModelElement> children = getChildren();
        List<TmfExperimentElement> traces = new ArrayList<>();
        for (ITmfProjectModelElement child : children) {
            if (child instanceof TmfExperimentElement) {
                traces.add((TmfExperimentElement) child);
            }
        }
        return traces;
    }

    /**
     * Finds the experiment element for a given resource
     *
     * @param resource
     *            the resource to search for
     * @return the experiment element if found else null
     * @since 2.0
     */
    public @Nullable TmfExperimentElement getExperiment(@NonNull IResource resource) {
        String name = resource.getName();
        if (name != null) {
            return getExperiment(name);
        }
        return null;
    }

    /**
     * Finds the experiment element for a given name
     *
     * @param name
     *            the name of experiment to search for
     * @return the experiment element if found else null
     * @since 2.0
     */
    public @Nullable TmfExperimentElement getExperiment(@NonNull String name) {
        Optional<@NonNull TmfExperimentElement> exp = getExperiments().stream()
                .filter(Objects::nonNull)
                .filter(experiment -> experiment.getName().equals(name))
                .findFirst();
        return exp.isPresent() ? exp.get() : null;
    }


    /**
     * Add an experiment element for the specified experiment resource. If an
     * experiment already exists for the specified resource, it is returned.
     *
     * @param resource
     *            the experiment resource
     * @return the experiment element, or null if this experiment folder is not the
     *         parent of the specified resource
     * @since 3.3
     */
    public synchronized TmfExperimentElement addExperiment(@NonNull IFolder resource) {
        if (!resource.getParent().equals(getResource())) {
            return null;
        }
        /* If an experiment element already exists for this resource, return it */
        TmfExperimentElement experiment = getExperiment(resource);
        if (experiment != null) {
            return experiment;
        }
        /* Create a new experiment element and add it as a child, then return it */
        String name = resource.getName();
        experiment = new TmfExperimentElement(name, resource, this);
        addChild(experiment);
        return experiment;
    }

    // ------------------------------------------------------------------------
    // IPropertySource2
    // ------------------------------------------------------------------------

    @Override
    public Object getEditableValue() {
        return null;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        return Arrays.copyOf(DESCRIPTORS, DESCRIPTORS.length);
    }

    @Override
    public Object getPropertyValue(Object id) {

        if (NAME.equals(id)) {
            return getName();
        }

        if (PATH.equals(id)) {
            return getPath().toString();
        }

        if (LOCATION.equals(id)) {
            return getLocation().toString();
        }

        return null;
    }

    @Override
    public void resetPropertyValue(Object id) {
        // Do nothing
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        // Do nothing
    }

    @Override
    public boolean isPropertyResettable(Object id) {
        return false;
    }

    @Override
    public boolean isPropertySet(Object id) {
        return false;
    }

}
