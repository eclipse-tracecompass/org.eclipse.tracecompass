/**********************************************************************
 * Copyright (c) 2020, 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataTypeUtils;

import com.google.common.collect.ImmutableList;

/**
 * Helper class to define table headers and format statistics values
 *
 * @author Bernd Hufmann
 */
public final class SegmentStoreStatisticsAspects {

    private Function<Number, String> fMapper;
    private Function<String, String> fLabelMapper;
    private List<IDataAspect<NamedStatistics>> fAspects;

    /**
     * Default constructor. An object created with this constructor provides the
     * default aspects for statistics generated from a segment store.
     */
    public SegmentStoreStatisticsAspects() {
        this(Collections.emptyList());
    }

    /**
     * An object created with this constructor provides the default aspects for
     * statistics generated from a segment store and the user defined aspects
     * provided as parameters.
     *
     * @param userDefinedAspects
     *            a list of user defined aspects that will be added to the
     *            default ones
     */
    public SegmentStoreStatisticsAspects(List<IDataAspect<NamedStatistics>> userDefinedAspects) {
        fMapper = e -> String.format("%s", DataTypeUtils.getFormat(DataType.DURATION, "").format(e)); //$NON-NLS-1$ //$NON-NLS-2$
        fLabelMapper = e -> e;
        fAspects = createAspects(userDefinedAspects);
    }

    /**
     * Set a mapper function to convert a statistics Number to String.
     * Used for minimum, maximum, average, standard deviation and total.
     *
     * @param mapper
     *              function to convert a Number to String
     */
    protected synchronized void setMapper(Function<Number, String> mapper) {
        fMapper = mapper;
    }

    /**
     * Sets mapper function to format label string to an output string.
     *
     * @param mapper
     *                function to map input string to output string. This can
     *                be used, for example, to change a symbol address to a
     *                symbol name.
     */
    protected synchronized void setLabelMapper(UnaryOperator<String> mapper) {
        fLabelMapper = mapper;
    }

    /**
    *
    * @return immutable list of data aspects.
    */
    public synchronized List<IDataAspect<NamedStatistics>> getAspects() {
        return fAspects;
    }

    private List<IDataAspect<NamedStatistics>> createAspects(List<IDataAspect<NamedStatistics>> userDefinedAspects) {
        ImmutableList.Builder<IDataAspect<NamedStatistics>> aspectsBuilder = new ImmutableList.Builder<>();
        aspectsBuilder.add(new IDataAspect<NamedStatistics>() {
            @Override
            public String getName() {
                return Objects.requireNonNull(Messages.SegmentStoreStatistics_Label);
            }
            @Override
            public @Nullable Object apply(NamedStatistics input) {
                return fLabelMapper.apply(input.getName());
            }
        });
        aspectsBuilder.add(new IDataAspect<NamedStatistics>() {
            @Override
            public String getName() {
                return Objects.requireNonNull(Objects.requireNonNull(Messages.SegmentStoreStatistics_MinLabel));
            }
            @Override
            public @Nullable Object apply(NamedStatistics input) {
                IStatistics<ISegment> statistics = input.getStatistics();
                return statistics.getNbElements() != 0 ? fMapper.apply(statistics.getMinNumber()) : null;
            }
        });

        aspectsBuilder.add(new IDataAspect<NamedStatistics>() {
            @Override
            public String getName() {
                return Objects.requireNonNull(Objects.requireNonNull(Messages.SegmentStoreStatistics_MaxLabel));
            }
            @Override
            public @Nullable Object apply(NamedStatistics input) {
                IStatistics<ISegment> statistics = input.getStatistics();
                return statistics.getNbElements() != 0 ? fMapper.apply(statistics.getMaxNumber()) : null;
            }
        });

        aspectsBuilder.add(new IDataAspect<NamedStatistics>() {
            @Override
            public String getName() {
                return Objects.requireNonNull(Objects.requireNonNull(Messages.SegmentStoreStatistics_AverageLabel));
            }
            @Override
            public @Nullable Object apply(NamedStatistics input) {
                IStatistics<ISegment> statistics = input.getStatistics();
                return statistics.getNbElements() != 0 ? fMapper.apply(statistics.getMean()) : null;
            }
        });

        aspectsBuilder.add(new IDataAspect<NamedStatistics>() {
            @Override
            public String getName() {
                return Objects.requireNonNull(Objects.requireNonNull(Messages.SegmentStoreStatistics_StandardDeviationLabel));
            }
            @Override
            public @Nullable Object apply(NamedStatistics input) {
                IStatistics<ISegment> statistics = input.getStatistics();
                return statistics.getNbElements() != 0 ? fMapper.apply(statistics.getStdDev()) : null;
            }
        });

        aspectsBuilder.add(new IDataAspect<NamedStatistics>() {
            @Override
            public String getName() {
                return Objects.requireNonNull(Objects.requireNonNull(Messages.SegmentStoreStatistics_CountLabel));
            }
            @Override
            public @Nullable Object apply(NamedStatistics input) {
                return String.valueOf(input.getStatistics().getNbElements());
            }
        });

        aspectsBuilder.add(new IDataAspect<NamedStatistics>() {
            @Override
            public String getName() {
                return Objects.requireNonNull(Objects.requireNonNull(Messages.SegmentStoreStatistics_TotalLabel));
            }
            @Override
            public @Nullable Object apply(NamedStatistics input) {
                IStatistics<ISegment> statistics = input.getStatistics();
                return statistics.getNbElements() != 0 ? fMapper.apply(statistics.getTotal()) : null;
            }
        });

        aspectsBuilder.add(new ITypedDataAspect<NamedStatistics>() {
            @Override
            public String getName() {
                return Objects.requireNonNull(Messages.SegmentStoreStatistics_MinTimeRangeLabel);
            }
            @Override
            public @Nullable String apply(NamedStatistics input) {
                ISegment min = input.getStatistics().getMinObject();
                if (min != null) {
                    return DataTypeUtils.toRangeString(min.getStart(), min.getEnd());
                }
                return null;
            }
            @Override
            public DataType getDataType() {
                return DataType.TIME_RANGE;
            }
        });

        aspectsBuilder.add(new ITypedDataAspect<NamedStatistics>() {
            @Override
            public String getName() {
                return Objects.requireNonNull(Messages.SegmentStoreStatistics_MaxTimeRangeLabel);
            }
            @Override
            public @Nullable String apply(NamedStatistics input) {
                ISegment max = input.getStatistics().getMaxObject();
                if (max != null) {
                    return DataTypeUtils.toRangeString(max.getStart(), max.getEnd());
                }
                return null;
            }
            @Override
            public DataType getDataType() {
                return DataType.TIME_RANGE;
            }
        });

        aspectsBuilder.addAll(userDefinedAspects);

        return aspectsBuilder.build();
    }

   /**
    * Wrapper of statistics name and actual statistics implementation
    */
   public static class NamedStatistics {
       private String fName;
       private IStatistics<ISegment> fStatistics;
       /**
        * @param name
        *           The label the statistics are for
        * @param statistics
        *           The statistics object
        */
       public NamedStatistics(String name, IStatistics<ISegment> statistics) {
           fName = name;
           fStatistics = statistics;
       }
       /**
        * Get the label that the statistics is for.
        * @return the label
        */
       public String getName() {
           return fName;
       }
       /**
        * Get the corresponding statistics object.
        * @return statistics object
        */
       public IStatistics<ISegment> getStatistics() {
           return fStatistics;
      }
   }
}