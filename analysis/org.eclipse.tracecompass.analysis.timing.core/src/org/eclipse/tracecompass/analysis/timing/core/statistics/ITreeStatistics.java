package org.eclipse.tracecompass.analysis.timing.core.statistics;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Interface for classes implementing a tree of statistics. Extends the
 * {@link IStatistics} to add the childs of one statistic as well as the name of
 * one tree node.
 *
 *
 * @param <E>
 *            The type of object to calculate statistics on
 * @author ezhasiw
 */
public interface ITreeStatistics<@NonNull E> extends IStatistics<@NonNull E> {

    /**
     * @return The name of this statistic
     */
    String getName();

    /**
     * @param treeStatistic
     *            the child statistic to add.
     * @return Whether the add was successful.
     */
    boolean addChild(ITreeStatistics<@NonNull E> treeStatistic);

    /**
     * @return The list of child statistics.
     */
    List<ITreeStatistics<@NonNull E>> getChilds();
}
