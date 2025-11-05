/*******************************************************************************
 * Copyright (c) 2018, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.internal.jsontrace.core.job;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.internal.jsontrace.core.Activator;
import org.eclipse.tracecompass.internal.jsontrace.core.Messages;
import org.eclipse.tracecompass.internal.provisional.jsontrace.core.trace.JsonTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.traceeventlogger.LogUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * On-disk sorting job. It splits a trace into tracelets. Each tracelet is
 * sorted in ram and written to disk, then the tracelets are merged into a big
 * trace.
 *
 * @author Matthew Khouzam
 */
public abstract class SortingJob extends Job {

    private static final char CLOSE_BRACKET = ']';
    private static final char OPEN_BRACKET = '[';
    private static final int CHARS_PER_LINE_ESTIMATE = 50;
    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(SortingJob.class);
    private static final int CHUNK_SIZE = 65535;
    private static final int METADATA_MAX_SIZE = 10000000;

    private static final Comparator<PartiallyParsedEvent> EVENT_COMPARATOR = Comparator
            .comparing(PartiallyParsedEvent::getTs);

    private static final class PartiallyParsedEvent {
        private static final @NonNull BigDecimal MINUS_ONE = BigDecimal.valueOf(-1);

        private final BigDecimal fTs;
        private String fLine;
        private final int fPos;

        public PartiallyParsedEvent(String key, String string, int i) {
            fLine = string;
            int indexOf = string.indexOf(key);
            fPos = i;
            if (indexOf < 0) {
                fTs = MINUS_ONE;
            } else {
                int index = indexOf + key.length();
                int end = string.indexOf(',', index);
                if (end == -1) {
                    end = string.indexOf('}', index);
                }
                BigDecimal ts;
                String number = string.substring(index, end).trim().replace("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
                if (!number.isEmpty()) {
                    try {
                        // This may be a bit slow, it can be optimized if need
                        // be.
                        ts = new BigDecimal(number);
                    } catch (NumberFormatException e) {
                        // Cannot be parsed as a number, set to -1
                        ts = MINUS_ONE;
                    }
                } else {
                    ts = MINUS_ONE;
                }
                fTs = ts;
            }
        }

        public BigDecimal getTs() {
            return fTs;
        }
    }

    private final Integer fBracketsToSkip;
    private final String fTsKey;
    private final String fPath;
    private final List<String> fPathToEvents;
    private String fMetadata;
    private final ITmfTrace fTrace;

    /**
     * Constructor
     *
     * @param trace
     *            Trace to sort
     * @param path
     *            Trace path
     * @param tsKey
     *            Timestamp key, represent the json object key. The value associated
     *            to this key is the timestamp that will be use to sort
     * @param bracketsToSkip
     *            Number of bracket to skip
     */
    protected SortingJob(ITmfTrace trace, String path, String tsKey, int bracketsToSkip) {
        super(Messages.SortingJob_description);
        fTrace = trace;
        fPath = path;
        fTsKey = tsKey;
        fBracketsToSkip = bracketsToSkip;
        fPathToEvents = Collections.emptyList();
    }

    /**
     * Constructor
     *
     * @param trace
     *            Trace to sort
     * @param path
     *            Trace path
     * @param tsKey
     *            Timestamp key, represent the json object key. The value
     *            associated to this key is the timestamp that will be use to
     *            sort
     * @param pathToEvents
     *            Json key path indicating the events
     */
    protected SortingJob(ITmfTrace trace, String path, String tsKey, List<String> pathToEvents) {
        super(Messages.SortingJob_description);
        fTrace = trace;
        fPath = path;
        fTsKey = tsKey;
        fBracketsToSkip = -1;
        fPathToEvents = pathToEvents;
    }

    /**
     * Getter for the trace path
     *
     * @return the path
     */
    public String getPath() {
        return fPath;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        ITmfTrace trace = fTrace;

        IProgressMonitor subMonitor = SubMonitor.convert(monitor, 3);
        if (trace == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Trace cannot be null"); //$NON-NLS-1$
        }
        String dir = TmfTraceManager.getSupplementaryFileDir(trace);

        subMonitor.beginTask(Messages.SortingJob_sorting, (int) (new File(fPath).length() / CHARS_PER_LINE_ESTIMATE));
        subMonitor.subTask(Messages.SortingJob_splitting);
        File tempDir = new File(dir + ".tmp"); //$NON-NLS-1$
        tempDir.mkdirs();
        List<File> tracelings = new ArrayList<>();
        try (BufferedInputStream parser = new BufferedInputStream(new FileInputStream(fPath))) {
            IStatus status = goToEventArray(parser, subMonitor);
            if (status != null) {
                return status;
            }
            // Split events into chunk and sort them
            status = splitTrace(parser, subMonitor, tempDir, tracelings);
            if (status != null) {
                return status;
            }
            // Merge all the chunks and write all the events into another file
            File resultTrace = new File(dir + File.separator + new File(trace.getPath()).getName());
            boolean success = resultTrace.createNewFile();
            if (!success) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Could not create file " + resultTrace.getAbsolutePath()); //$NON-NLS-1$
            }
            if (!fPathToEvents.isEmpty()) {
                fMetadata = fMetadata + new String(parser.readNBytes(METADATA_MAX_SIZE), java.nio.charset.StandardCharsets.UTF_8);
            }
            processMetadata(trace, dir, parser);
            return mergeChunks(subMonitor, tracelings, resultTrace);
        } catch (IOException e) {
            LogUtils.traceInstant(LOGGER, Level.WARNING, "IOException in sorting job", "trace", fPath, //$NON-NLS-1$ //$NON-NLS-2$
                    "exception", e); //$NON-NLS-1$
            return new Status(IStatus.WARNING, Activator.PLUGIN_ID, "IOException in sorting job for " + fPath, e); //$NON-NLS-1$
        } finally {
            try {
                for (File tl : tracelings) {
                    Files.delete(tl.toPath());
                }
                Files.delete(tempDir.toPath());
            } catch (IOException e) {
                Activator.getInstance().logError(e.getMessage(), e);
            }
            subMonitor.done();
        }
    }

    private IStatus goToEventArray(BufferedInputStream parser, IProgressMonitor monitor) throws IOException {
        // TODO: When the constructor is removed, remove this condition
        if (fBracketsToSkip >= 0) {
            return goToEventArrayBracket(parser);
        }
        if (!fPathToEvents.isEmpty()) {
            return goToEventArrayPath(parser, monitor);
        }
        return null;
    }

    private IStatus goToEventArrayBracket(BufferedInputStream parser) throws IOException {
        int data = 0;
        for (int nbBracket = 0; nbBracket < fBracketsToSkip; nbBracket++) {
            data = parser.read();
            while (data != '[') {
                data = parser.read();
                if (data == -1) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            "Missing symbol \'[\' or \']\' in " + fPath); //$NON-NLS-1$
                }
            }
        }
        return null;
    }

    private IStatus goToEventArrayPath(BufferedInputStream parser, IProgressMonitor monitor) throws IOException {
        JsonFactory factory = new JsonFactory();
        long byteOffset = 0;
        try(JsonParser jsonParser = factory.createParser(new File(fPath))) {
            int depth = 0;
            while (!jsonParser.isClosed() && jsonParser.currentLocation().getByteOffset() < METADATA_MAX_SIZE) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                JsonToken token = jsonParser.nextToken();
                if (token == JsonToken.FIELD_NAME) {
                    String fieldName = jsonParser.currentName();
                    if (fieldName.equals(fPathToEvents.get(depth))) {
                        depth += 1;
                        if (depth == fPathToEvents.size()) {
                            break;
                        }
                    }
                    jsonParser.nextToken();
                }
            }
            byteOffset = jsonParser.currentLocation().getByteOffset();
        }
        fMetadata = new String(parser.readNBytes((int) byteOffset), java.nio.charset.StandardCharsets.UTF_8) + "]"; //$NON-NLS-1$
        return null;
    }

    private IStatus splitTrace(BufferedInputStream parser, IProgressMonitor monitor, File tempDirectory, List<File> tracelings) throws IOException {
        int data = 0;
        List<PartiallyParsedEvent> events = new ArrayList<>(CHUNK_SIZE);
        String eventString = JsonTrace.readNextEventString(parser::read);
        if (eventString == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Empty event in " + fPath); //$NON-NLS-1$
        }
        PartiallyParsedEvent line = new PartiallyParsedEvent(fTsKey, eventString, 0);
        line.fLine = data + '"' + line.fLine;
        int cnt = 0;
        int filen = 0;
        while (eventString != null) {
            while (cnt < CHUNK_SIZE) {
                events.add(line);
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                eventString = JsonTrace.readNextEventString(parser::read);
                if (eventString == null) {
                    break;
                }
                line = new PartiallyParsedEvent(fTsKey, eventString, 0);
                cnt++;
            }
            events.sort(EVENT_COMPARATOR);
            cnt = 0;
            File traceling = new File(tempDirectory + File.separator + "test" + filen + ".json"); //$NON-NLS-1$ //$NON-NLS-2$
            tracelings.add(traceling);
            boolean success = traceling.createNewFile();
            if (!success) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "Could not create partial file " + traceling.getAbsolutePath()); //$NON-NLS-1$
            }
            try (PrintWriter fs = new PrintWriter(traceling)) {
                fs.println(OPEN_BRACKET);
                for (PartiallyParsedEvent sortedEvent : events) {
                    fs.println(sortedEvent.fLine + ',');
                }
                fs.println(CLOSE_BRACKET);
            }
            events.clear();
            filen++;
            monitor.worked(1);
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
        }
        return null;
    }

    @SuppressWarnings("resource")
    private IStatus mergeChunks(IProgressMonitor monitor, List<File> tracelings, File resultTrace) throws IOException {
        monitor.subTask(Messages.SortingJob_merging);
        PriorityQueue<PartiallyParsedEvent> evs = new PriorityQueue<>(EVENT_COMPARATOR);
        List<BufferedInputStream> parsers = new ArrayList<>();
        int i = 0;
        int data = 0;
        try {
            // Initialize parsers
            for (File traceling : tracelings) {
                /*
                 * This resource is added to a priority queue and then removed
                 * in the finally clause.
                 */
                BufferedInputStream createParser = new BufferedInputStream(new FileInputStream(traceling));
                parsers.add(createParser);
                while (data != '[') {
                    data = createParser.read();
                    if (data == -1) {
                        break;
                    }
                }
                String eventString = JsonTrace.readNextEventString(createParser::read);
                PartiallyParsedEvent parse = new PartiallyParsedEvent(fTsKey, eventString, i);
                evs.add(parse);
                i++;
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    break;
                }
            }
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            // Write the resulting trace
            try (PrintWriter tempWriter = new PrintWriter(resultTrace)) {
                tempWriter.println('[');
                while (!evs.isEmpty()) {
                    PartiallyParsedEvent sortedEvent = evs.poll();
                    if (sortedEvent == null) {
                        break;
                    }
                    PartiallyParsedEvent parse = readNextEvent(parsers.get(sortedEvent.fPos), fTsKey, sortedEvent.fPos);
                    if (parse != null) {
                        tempWriter.println(sortedEvent.fLine.trim() + ',');
                        evs.add(parse);
                    } else {
                        tempWriter.println(sortedEvent.fLine.trim() + (evs.isEmpty() ? "" : ',')); //$NON-NLS-1$
                    }
                    monitor.worked(1);
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                }
                tempWriter.println(']');
            }
        } finally {
            for (BufferedInputStream tmpParser : parsers) {
                tmpParser.close();
            }
        }
        return Status.OK_STATUS;
    }

    /**
     * Process whatever metadata that can be found after the event list in the
     * trace file file
     *
     * @param trace
     *            the trace to be sort
     * @param dir
     *            the path to the trace file
     * @param parser
     *            The file parser, position at the end of the events
     * @throws IOException
     *             Exceptions thrown by reading file
     */
    protected void processMetadata(ITmfTrace trace, String dir, BufferedInputStream parser) throws IOException {
        this.processMetadata(trace, dir);
        this.processMetadataJson(trace, fMetadata);
    }

    /**
     * Process whatever metadata that can be found after the event list in the
     * trace file file
     *
     * @param trace
     *            the trace to be sort
     * @param metadata
     *            the string containing everything except all the events
     */
    protected void processMetadataJson(ITmfTrace trace, String metadata) {
        // do nothing
    }

    /**
     * Process whatever metadata that can be found after the event list in the
     * trace file file
     *
     * @param trace
     *            the trace to be sort
     * @param dir
     *            the path to the trace file
     * @throws IOException
     *             Exceptions thrown by reading file
     */
    protected abstract void processMetadata(ITmfTrace trace, String dir) throws IOException;

    private static @Nullable PartiallyParsedEvent readNextEvent(BufferedInputStream parser, String key, int i)
            throws IOException {
        String event = JsonTrace.readNextEventString(parser::read);
        return event == null ? null : new PartiallyParsedEvent(key, event, i);
    }
}