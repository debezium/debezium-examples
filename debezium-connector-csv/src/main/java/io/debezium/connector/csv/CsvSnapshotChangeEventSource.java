/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.data.Envelope;
import io.debezium.pipeline.EventDispatcher;
import io.debezium.pipeline.source.SnapshottingTask;
import io.debezium.pipeline.source.spi.ChangeEventSource;
import io.debezium.pipeline.source.spi.SnapshotChangeEventSource;
import io.debezium.pipeline.source.spi.SnapshotProgressListener;
import io.debezium.pipeline.spi.SnapshotResult;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Reads snapshot rows ({@code S|…}) from the CSV file and emits READ events.
 *
 * <p>Processes lines from the beginning: registers schema on each header line, emits a READ event per S-row,
 * and stops at the first non-S data line (handing off to streaming) or at end of file.
 *
 * Updates {@link CsvOffsetContext} to the handover line so streaming resumes at the correct
 * position. Skipped entirely when an offset already exists.
 */
class CsvSnapshotChangeEventSource implements SnapshotChangeEventSource<CsvPartition, CsvOffsetContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvSnapshotChangeEventSource.class);

    /** Separator used between columns in data rows. */
    static final String DATA_SEPARATOR = "\\|";

    /** Prefix that identifies a schema-header line. */
    static final String HEADER_PREFIX = "EventType";

    /** Separator used between column specs in a schema header line. */
    static final String HEADER_SEPARATOR = ";";

    private final CsvConnectorConfig config;
    private final CsvId csvId;
    private final CsvSchema schema;
    private final EventDispatcher<CsvPartition, CsvId> dispatcher;
    private final SnapshotProgressListener<CsvPartition> progressListener;

    CsvSnapshotChangeEventSource(CsvConnectorConfig config,
                                  CsvId csvId,
                                  CsvSchema schema,
                                  EventDispatcher<CsvPartition, CsvId> dispatcher,
                                  SnapshotProgressListener<CsvPartition> progressListener) {
        this.config = config;
        this.csvId = csvId;
        this.schema = schema;
        this.dispatcher = dispatcher;
        this.progressListener = progressListener;
    }

    @Override
    public SnapshottingTask getSnapshottingTask(CsvPartition partition, CsvOffsetContext offsetContext) {
        boolean shouldSnapshot = offsetContext.getLineNumber() == 0;
        LOGGER.info("Snapshot decision for partition {}: shouldSnapshot={}, currentLine={}",
                partition, shouldSnapshot, offsetContext.getLineNumber());
        return new SnapshottingTask(shouldSnapshot, false, List.of(), Map.of(), false);
    }

    @Override
    public SnapshottingTask getBlockingSnapshottingTask(CsvPartition partition, CsvOffsetContext offsetContext,
            io.debezium.pipeline.signal.actions.snapshotting.SnapshotConfiguration config) {
        // CSV snapshot is sequential — blocking and non-blocking modes are equivalent.
        return getSnapshottingTask(partition, offsetContext);
    }

    /**
     * Reads all lines from the start of the file, emitting READ events for S-rows.
     * Stops and returns {@link SnapshotResult#completed} at the first non-S line or end of file.
     *
     * @param task carries {@code shouldSkipSnapshot}; skipped when a prior offset exists
     * @throws InterruptedException if the change-event queue is interrupted during dispatch
     */
    @Override
    public SnapshotResult<CsvOffsetContext> execute(ChangeEventSource.ChangeEventSourceContext context,
                                                     CsvPartition partition,
                                                     CsvOffsetContext offsetContext,
                                                     SnapshottingTask task)
            throws InterruptedException {

        if (task.shouldSkipSnapshot()) {
            LOGGER.info("Skipping snapshot – resuming streaming from line {}", offsetContext.getLineNumber());
            return SnapshotResult.skipped(offsetContext);
        }

        LOGGER.info("Starting snapshot of {}", config.getFilePath());

        List<String> lines;
        try {
            lines = Files.readAllLines(config.getFilePath());
        }
        catch (IOException e) {
            throw new UncheckedIOException("Cannot read CSV file " + config.getFilePath(), e);
        }

        List<ColumnDef> currentSchema = null;
        int snapshotCount = 0;

        for (int i = 0; i < lines.size(); i++) {
            if (!context.isRunning()) {
                return SnapshotResult.aborted();
            }

            String line = lines.get(i);

            if (isHeader(line)) {
                // Schema header line: register the column definitions
                currentSchema = parseHeader(line);
                schema.register(csvId, currentSchema, config);
                LOGGER.debug("Registered schema at line {}: {}", i, currentSchema);
            }
            else if (line.startsWith("S|")) {
                if (currentSchema == null) {
                    throw new IllegalStateException(
                            "Data row at line " + i + " precedes any schema header in " + config.getFilePath());
                }
                dispatch(Envelope.Operation.READ, line, i, currentSchema, partition, offsetContext);
                offsetContext.setLineNumber(i + 1);
                snapshotCount++;
            }
            else {
                // first non-S, non-header line: hand off to streaming
                offsetContext.setLineNumber(i);
                LOGGER.info("Snapshot complete: {} records read, handing over to streaming at line {}", snapshotCount, i);
                return SnapshotResult.completed(offsetContext);
            }
        }

        // end of file — all lines were S-rows or headers
        offsetContext.setLineNumber(lines.size());
        LOGGER.info("Snapshot complete: {} records read (end of file at line {})", snapshotCount, lines.size());
        return SnapshotResult.completed(offsetContext);
    }

    /** Returns {@code true} if the line is a schema header. */
    static boolean isHeader(String line) {
        return line.startsWith(HEADER_PREFIX);
    }

    /**
     * Parses a schema-header line into an ordered list of {@link ColumnDef}s.
     * The leading {@code EventType} field is skipped; remaining fields are {@code name} or {@code name:TYPE}.
     *
     * @param line a header line starting with {@code EventType;…}
     * @return ordered column definitions; first entry is the primary key
     */
    static List<ColumnDef> parseHeader(String line) {
        String[] parts = line.split(HEADER_SEPARATOR);
        // skip parts[0] ("EventType")
        List<ColumnDef> cols = new java.util.ArrayList<>(parts.length - 1);
        for (int i = 1; i < parts.length; i++) {
            cols.add(ColumnDef.parse(parts[i]));
        }
        return List.copyOf(cols);
    }

    /**
     * Parses a pipe-delimited data row and dispatches it as a change event.
     *
     * @param op            CDC operation (READ, CREATE, UPDATE, DELETE)
     * @param line          raw CSV line; {@code parts[0]} is the event-type char, {@code parts[1..n]} are values
     * @param lineNumber    zero-based line index in the file
     * @param cols          active column definitions; {@code cols[0]} is the primary key
     * @param partition     source partition
     * @param offsetContext current offset; updated via {@code event()} before dispatch
     */
    void dispatch(Envelope.Operation op, String line, int lineNumber,
                  List<ColumnDef> cols, CsvPartition partition,
                  CsvOffsetContext offsetContext) throws InterruptedException {

        String[] parts = line.split(DATA_SEPARATOR, -1); // parts[0]=event-type, parts[1..n]=values

        CsvCollectionSchema collectionSchema = schema.getCurrentSchema();
        Schema keySchema = collectionSchema.keySchema();
        Schema valueSchema = collectionSchema.getEnvelopeSchema().schema()
                .field("after").schema();

        ColumnDef keyCol = cols.get(0);
        Struct keyStruct = new Struct(keySchema)
                .put(keyCol.name(), convertValue(parts[1], keyCol.type()));

        Struct valueStruct = new Struct(valueSchema);
        for (int i = 0; i < cols.size(); i++) {
            ColumnDef col = cols.get(i);
            String rawValue = (i + 1 < parts.length) ? parts[i + 1] : null;
            valueStruct.put(col.name(), rawValue == null ? null : convertValue(rawValue, col.type()));
        }

        offsetContext.event(csvId, null);

        dispatcher.dispatchDataChangeEvent(partition, csvId,
                new CsvChangeRecordEmitter(partition, offsetContext, op, keyStruct, valueStruct));
    }

    /**
     * Converts a raw CSV string to the Java type required by the Kafka Connect schema.
     *
     * @param raw  raw string from the CSV; {@code null} or empty returns {@code null}
     * @param type target column type
     * @return typed value, or {@code null} if raw is empty/null
     *
     * <p>NOTE: DATE must be {@link java.util.Date} (logical representation), not the INT32
     * wire value — the converter (e.g. JsonConverter) handles the encoding.
     */
    static Object convertValue(String raw, ColumnType type) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return switch (type) {
            case INT -> Integer.parseInt(raw);
            case LONG -> Long.parseLong(raw);
            case FLOAT -> Float.parseFloat(raw);
            case DOUBLE -> Double.parseDouble(raw);
            case BOOLEAN -> Boolean.parseBoolean(raw);
            case DATE -> {
                LocalDate date = LocalDate.parse(raw);
                yield Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
            }
            default -> raw;
        };
    }
}
