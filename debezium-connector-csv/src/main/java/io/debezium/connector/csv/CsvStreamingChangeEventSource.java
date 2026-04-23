/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.data.Envelope;
import io.debezium.pipeline.EventDispatcher;
import io.debezium.pipeline.source.spi.ChangeEventSource;
import io.debezium.pipeline.source.spi.StreamingChangeEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.debezium.connector.csv.CsvSnapshotChangeEventSource.DATA_SEPARATOR;
import static io.debezium.connector.csv.CsvSnapshotChangeEventSource.isHeader;
import static io.debezium.connector.csv.CsvSnapshotChangeEventSource.parseHeader;

/**
 * Streams ongoing changes by tailing the CSV file from the snapshot handover line.
 *
 * <p>Uses Java NIO {@link WatchService} to detect file modifications, then reads all
 * newly appended lines. Event type mapping: {@code I}→CREATE, {@code U}→UPDATE
 * (no before-image), {@code D}→DELETE. A new schema-header line at any position
 * re-registers the schema via {@link CsvSchema#register} and applies it to subsequent rows.
 *
 * <p>The line-number offset is incremented after every processed line (header or data)
 * so that streaming resumes at the exact position on restart.
 */
class CsvStreamingChangeEventSource implements StreamingChangeEventSource<CsvPartition, CsvOffsetContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvStreamingChangeEventSource.class);

    private final CsvConnectorConfig config;
    private final CsvId csvId;
    private final CsvSchema schema;
    private final EventDispatcher<CsvPartition, CsvId> dispatcher;

    CsvStreamingChangeEventSource(CsvConnectorConfig config,
                                   CsvId csvId,
                                   CsvSchema schema,
                                   EventDispatcher<CsvPartition, CsvId> dispatcher) {
        this.config = config;
        this.csvId = csvId;
        this.schema = schema;
        this.dispatcher = dispatcher;
    }

    /**
     * Main streaming loop. Registers a {@link WatchService} on the file's parent directory,
     * reads newly appended lines on each modification event, and dispatches change events.
     * Polls every 1 s to avoid missing rapid consecutive writes.
     *
     * @throws InterruptedException if the change-event queue is interrupted
     */
    @Override
    public void execute(ChangeEventSource.ChangeEventSourceContext context,
                        CsvPartition partition,
                        CsvOffsetContext offsetContext) throws InterruptedException {

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            config.getFilePath().getParent()
                    .register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            // schema may already be populated by snapshot; initialise from cache if not (e.g. all S-rows)
            List<ColumnDef> currentSchema = deriveCurrentSchema();
            long lineNum = offsetContext.getLineNumber();

            LOGGER.info("Starting streaming from line {} in {}", lineNum, config.getFilePath());

            while (context.isRunning()) {
                // Read any lines that have been appended since last iteration
                List<String> lines;
                try {
                    lines = Files.readAllLines(config.getFilePath());
                }
                catch (IOException e) {
                    throw new UncheckedIOException("Cannot read CSV file " + config.getFilePath(), e);
                }

                while (lineNum < lines.size()) {
                    String line = lines.get((int) lineNum);

                    if (isHeader(line)) {
                        // Schema evolution: new column definitions
                        currentSchema = parseHeader(line);
                        schema.register(csvId, currentSchema, config);
                        LOGGER.info("Schema evolved at line {}: {}", lineNum, currentSchema);
                    }
                    else {
                        // Data row: I / U / D
                        Envelope.Operation op = parseOperation(line);
                        if (op != null && currentSchema != null) {
                            dispatchDataLine(op, line, (int) lineNum, currentSchema, partition, offsetContext);
                        }
                        else if (currentSchema == null) {
                            LOGGER.warn("Skipping line {} – no schema registered yet", lineNum);
                        }
                    }

                    lineNum++;
                    offsetContext.setLineNumber(lineNum);
                }

                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key != null) {
                    key.pollEvents(); // drain events to prevent WatchService queue overflow
                    key.reset();
                }
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException("WatchService error for " + config.getFilePath(), e);
        }
    }

    /**
     * Maps the leading character of a data line to a Debezium {@link Envelope.Operation}.
     * Returns {@code null} for unknown or {@code S} lines; callers should skip {@code null}.
     */
    private static Envelope.Operation parseOperation(String line) {
        if (line.isEmpty()) {
            return null;
        }
        return switch (line.charAt(0)) {
            case 'I' -> Envelope.Operation.CREATE;
            case 'U' -> Envelope.Operation.UPDATE;
            case 'D' -> Envelope.Operation.DELETE;
            default -> null; // includes 'S' which should not appear in streaming
        };
    }

    /**
     * Reconstructs the active {@link ColumnDef} list from the cached schema.
     * Used when streaming starts without having seen a header line (snapshot already registered it).
     * Returns {@code null} if no schema has been registered yet.
     */
    private List<ColumnDef> deriveCurrentSchema() {
        CsvCollectionSchema cached = schema.getCurrentSchema();
        if (cached == null) {
            return null;
        }
        // rebuild ColumnDef list from the value schema's "after" fields
        return cached.getEnvelopeSchema().schema()
                .field("after").schema().fields().stream()
                .map(f -> new ColumnDef(f.name(), inferColumnType(f.schema())))
                .toList();
    }

    /** Reverse-maps a Kafka Connect Schema back to a {@link ColumnType}. */
    private static ColumnType inferColumnType(org.apache.kafka.connect.data.Schema s) {
        if (org.apache.kafka.connect.data.Date.LOGICAL_NAME.equals(s.name())) {
            return ColumnType.DATE;
        }
        return switch (s.type()) {
            case INT32 -> ColumnType.INT;
            case INT64 -> ColumnType.LONG;
            case FLOAT32 -> ColumnType.FLOAT;
            case FLOAT64 -> ColumnType.DOUBLE;
            case BOOLEAN -> ColumnType.BOOLEAN;
            default -> ColumnType.STRING;
        };
    }

    /**
     * Parses a streaming data row and dispatches it as a change event.
     *
     * @param op            CDC operation (CREATE, UPDATE, DELETE)
     * @param line          raw CSV line; {@code parts[0]} is the event-type char
     * @param lineNumber    zero-based line index (for logging only)
     * @param cols          active column definitions; {@code cols[0]} is the primary key
     */
    private void dispatchDataLine(Envelope.Operation op, String line, int lineNumber,
                                   List<ColumnDef> cols, CsvPartition partition,
                                   CsvOffsetContext offsetContext) throws InterruptedException {

        String[] parts = line.split(DATA_SEPARATOR, -1); // parts[0]=event-type, parts[1..n]=values

        CsvCollectionSchema collectionSchema = schema.getCurrentSchema();
        org.apache.kafka.connect.data.Schema keySchema = collectionSchema.keySchema();
        org.apache.kafka.connect.data.Schema valueSchema = collectionSchema.getEnvelopeSchema().schema()
                .field("after").schema();

        ColumnDef keyCol = cols.get(0);
        org.apache.kafka.connect.data.Struct keyStruct =
                new org.apache.kafka.connect.data.Struct(keySchema)
                        .put(keyCol.name(), CsvSnapshotChangeEventSource.convertValue(parts[1], keyCol.type()));

        org.apache.kafka.connect.data.Struct valueStruct =
                new org.apache.kafka.connect.data.Struct(valueSchema);
        for (int i = 0; i < cols.size(); i++) {
            ColumnDef col = cols.get(i);
            String rawValue = (i + 1 < parts.length) ? parts[i + 1] : null;
            valueStruct.put(col.name(),
                    rawValue == null ? null : CsvSnapshotChangeEventSource.convertValue(rawValue, col.type()));
        }

        offsetContext.event(csvId, null);

        dispatcher.dispatchDataChangeEvent(partition, csvId,
                new CsvChangeRecordEmitter(partition, offsetContext, op, keyStruct, valueStruct));
    }
}
