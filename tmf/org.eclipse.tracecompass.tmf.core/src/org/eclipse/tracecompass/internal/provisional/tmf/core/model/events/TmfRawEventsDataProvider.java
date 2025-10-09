/**********************************************************************
 * Copyright (c) 2018, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.core.model.events;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.object.IObjectDataProvider;
import org.eclipse.tracecompass.tmf.core.model.object.ObjectModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * This data provider will return an object representing an array of raw event
 * data.
 *
 * @author Patrick Tasse
 */
public class TmfRawEventsDataProvider extends AbstractTmfTraceDataProvider implements IObjectDataProvider {

    /**
     * Extension point ID.
     */
    public static final String ID = "org.eclipse.tracecompass.tmf.core.model.events.data"; //$NON-NLS-1$

    private static final String SELECTION_RANGE = "selection_range"; //$NON-NLS-1$
    private static final String START = "start"; //$NON-NLS-1$
    private static final String NEXT = "next"; //$NON-NLS-1$
    private static final String PREVIOUS = "previous"; //$NON-NLS-1$
    private static final String TYPE = "type"; //$NON-NLS-1$
    private static final String FIELD = "field"; //$NON-NLS-1$
    private static final String COUNT = "count"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace
     *            A trace on which we are interested to fetch a raw event model
     */
    public TmfRawEventsDataProvider(ITmfTrace trace) {
        super(trace);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public TmfModelResponse<ObjectModel> fetchData(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfTrace trace = getTrace();
        long rank = 0L;
        Long prev = null;
        Long next = null;
        Object selectionRangeObj = fetchParameters.get(SELECTION_RANGE);
        Object nextObj = fetchParameters.get(NEXT);
        Object previousObj = fetchParameters.get(PREVIOUS);
        Object typeObj = fetchParameters.get(TYPE);
        Object fieldObj = fetchParameters.get(FIELD);
        Object countObj = fetchParameters.get(COUNT);
        if (nextObj instanceof Number nextNum) {
            rank = nextNum.longValue();
            prev = rank;
            previousObj = null;
        } else if (previousObj instanceof Number previousNum) {
            rank = previousNum.longValue();
            next = rank;
            nextObj = null;
        } else if (selectionRangeObj instanceof Map<?,?> selectionRange) {
            if (selectionRange.get(START) instanceof Number start) {
                rank = trace.seekEvent(TmfTimestamp.fromNanos(start.longValue())).getRank();
            }
            nextObj = null;
            previousObj = null;
        }
        Pattern eventTypePattern = (typeObj instanceof String type) ? Pattern.compile(type) : null;
        Pattern fieldPattern = (fieldObj instanceof String field) ? Pattern.compile(field) : null;
        Integer max = null;
        if (countObj instanceof Number count) {
            max = count.intValue();
        }
        if (eventTypePattern == null && fieldPattern == null && previousObj instanceof Number) {
            // optimize backward request when no filters specified
            if (max == null) {
                max = (int) rank;
                rank = 0L;
            } else if (rank >= max) {
                rank = rank - max;
            } else {
                max = (int) rank;
                rank = 0L;
            }
            // clear previousObj to read forwards
            previousObj = null;
        }
        List<Object> object = new ArrayList<>();
        ObjectModel model;
        if (previousObj instanceof Number) {
            // read backward
            while (rank > 0 && (max == null || object.size() < max)) {
                rank--;
                ITmfContext context = trace.seekEvent(rank);
                long eventRank = context.getRank();
                ITmfEvent event = trace.getNext(context);
                if (event == null) {
                    break;
                }
                if (eventTypePattern == null || eventTypePattern.matcher(event.getName()).find()) {
                    Object eventObj = convertEvent(event, eventRank, fieldPattern);
                    if (eventObj != null) {
                        object.add(0, eventObj);
                    }
                }
            }
            model = new ObjectModel(object);
            if (rank > 0L) {
                model.setPrevious(rank);
            }
            if (next != null) {
                model.setNext(next);
            }
        } else {
            // read forward
            ITmfContext context = trace.seekEvent(rank);
            while (max == null || object.size() < max) {
                long eventRank = context.getRank();
                ITmfEvent event = trace.getNext(context);
                if (event == null) {
                    break;
                }
                if (eventTypePattern == null || eventTypePattern.matcher(event.getName()).find()) {
                    Object eventObj = convertEvent(event, eventRank, fieldPattern);
                    if (eventObj != null) {
                        object.add(eventObj);
                    }
                }
            }
            model = new ObjectModel(object);
            next = context.getRank();
            if (trace.getNext(context) != null) {
                model.setNext(next);
            }
            if (prev != null && prev > 0L) {
                model.setPrevious(prev);
            }
        }
        return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private static @Nullable Object convertEvent(ITmfEvent event, long rank, @Nullable Pattern fieldPattern) {
        Map<String, @Nullable Object> data = new LinkedHashMap<>();

        // Add basic event information
        data.put("rank", rank); //$NON-NLS-1$
        data.put(TmfBaseAspects.getTimestampAspect().getName(), event.getTimestamp().toNanos());
        data.put(TmfBaseAspects.getEventTypeAspect().getName(), event.getType().getName());
        Object contents = convertEventField(event.getContent(), fieldPattern);
        if (fieldPattern != null && contents instanceof Map map) {
            if (map.isEmpty()) {
                return null;
            }
        }
        data.put(TmfBaseAspects.getContentsAspect().getName(), contents);
        return data;
    }

    private static @Nullable Object convertEventField(@Nullable ITmfEventField field, @Nullable Pattern fieldPattern) {
        if (field == null) {
            return null;
        }
        Builder<String, @Nullable Object> builder = ImmutableMap.builder();
        for (ITmfEventField subField : field.getFields()) {
            if (subField == null) {
                continue;
            }
            String name = subField.getName();
            if (fieldPattern == null || fieldPattern.matcher(name).find()) {
                Object value = subField.getValue();
                if (value instanceof ITmfEventField) {
                    builder.put(name, convertEventField((ITmfEventField) value, null));
                } else {
                    builder.put(name, value);
                }
            }
        }
        Object value = field.getValue();
        if (value instanceof ITmfEventField) {
            value = convertEventField((ITmfEventField) value, null);
        }
        ImmutableMap<String, @Nullable Object> fields = builder.build();
        if (value == null) {
            return fields;
        } else if (fields.isEmpty()) {
            return value;
        } else {
            return Map.of("value", value, "fields", fields); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
