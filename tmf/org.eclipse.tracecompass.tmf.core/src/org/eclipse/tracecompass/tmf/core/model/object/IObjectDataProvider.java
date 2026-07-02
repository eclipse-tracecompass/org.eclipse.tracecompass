package org.eclipse.tracecompass.tmf.core.model.object;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;

/**
 * Interface for a data provider that returns a generic object as model
 *
 * @since 10.2
 */
public interface IObjectDataProvider extends ITmfDataProvider {

    /**
     * This method computes a generic object model. Then, it returns a
     * {@link TmfModelResponse} that contains the model.
     *
     * @param fetchParameters
     *            Map of parameters that can be used to compute result object
     * @param monitor
     *            A ProgressMonitor to cancel task
     * @return A {@link TmfModelResponse} instance
     */
    public TmfModelResponse<ObjectModel> fetchData(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor);

}
