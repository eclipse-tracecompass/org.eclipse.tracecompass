package org.eclipse.tracecompass.analysis.timing.core.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Extend {@link Statistics} to support a tree of statistics.
 *
 * @param <E>
 *            The type of object to calculate statistics on
 * @author ezhasiw
 */
public class TreeStatistics<@NonNull E> extends Statistics<@NonNull E> implements ITreeStatistics<@NonNull E> {

    private String fName = ""; //$NON-NLS-1$
    private List<ITreeStatistics<@NonNull E>> fChildStatistics = new ArrayList<> ();

    /**
     * Constructor
     *
     * @param name
     *            The name of this statistic
     */
    public TreeStatistics(String name) {
        super();
        fName = name;
    }

    /**
     * Constructor
     *
     * @param mapper
     *            A mapper function that takes an object to computes statistics
     *            for and returns the value to use for the statistics
     * @param name
     *            The name of this statistic
     */
    public TreeStatistics(Function<E, @Nullable ? extends @Nullable Number> mapper, String name) {
        super(mapper);
        fName = name;
    }

    @Override
    public List<ITreeStatistics<@NonNull E>> getChilds() {
        return fChildStatistics;
    }

    @Override
    public boolean addChild(@NonNull  ITreeStatistics<@NonNull E> treeStatistic) {
        return fChildStatistics.add(treeStatistic);
    }

    @Override
    public String getName() {
        return fName;
    }
}
