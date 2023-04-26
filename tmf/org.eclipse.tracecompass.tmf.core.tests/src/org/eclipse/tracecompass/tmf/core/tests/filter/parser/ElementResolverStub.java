package org.eclipse.tracecompass.tmf.core.tests.filter.parser;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.model.ICoreElementResolver;

import com.google.common.collect.Multimap;

/**
 * Implements an {@link ICoreElementResolver} for test
 *
 * @author Jean-Christian
 *
 */
public class ElementResolverStub implements ICoreElementResolver {
    private final @NonNull Multimap<@NonNull String, @NonNull Object> fData;

    /**
     * Constructor
     *
     * @param data
     *            The data to filter on
     */
    public ElementResolverStub(@NonNull Multimap<@NonNull String, @NonNull Object> data) {
        fData = data;
    }

    @Override
    public @NonNull Multimap<@NonNull String, @NonNull Object> getMetadata() {
        return fData;
    }

}
