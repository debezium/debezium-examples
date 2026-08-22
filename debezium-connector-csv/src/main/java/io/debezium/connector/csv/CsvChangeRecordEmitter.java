/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.data.Envelope;
import io.debezium.pipeline.spi.ChangeRecordEmitter;
import io.debezium.pipeline.spi.OffsetContext;
import io.debezium.schema.DataCollectionSchema;
import org.apache.kafka.connect.data.Struct;

import java.time.Instant;

/**
 * Wraps a pre-parsed CSV row and emits it as a Debezium change record.
 *
 * <p>Called by {@link io.debezium.pipeline.EventDispatcher} after schema lookup.
 * Builds the full Kafka Connect envelope struct (op, before, after, source, ts_ms)
 * and hands it to the dispatcher's internal receiver.
 *
 * <p>UPDATE events carry no before-image; {@code before} is always {@code null}.
 */
class CsvChangeRecordEmitter implements ChangeRecordEmitter<CsvPartition> {

    private final CsvPartition partition;
    private final CsvOffsetContext offsetContext;
    private final Envelope.Operation operation;
    private final Struct keyStruct;
    private final Struct valueStruct;

    CsvChangeRecordEmitter(CsvPartition partition,
                           CsvOffsetContext offsetContext,
                           Envelope.Operation operation,
                           Struct keyStruct,
                           Struct valueStruct) {
        this.partition = partition;
        this.offsetContext = offsetContext;
        this.operation = operation;
        this.keyStruct = keyStruct;
        this.valueStruct = valueStruct;
    }

    /**
     * Constructs the envelope struct for the current operation and dispatches it.
     *
     * @param schema   resolved collection schema; provides the {@link Envelope} builder
     * @param receiver dispatcher-provided receiver that enqueues the final SourceRecord
     * @throws InterruptedException if the receiver's queue is interrupted
     *
     * <p>NOTE: the value passed to {@code receiver.changeRecord()} must be the complete
     * envelope struct, not the raw value struct — a mismatch causes
     * {@code DataException: Mismatching schema} in the JSON converter.
     */
    @Override
    public void emitChangeRecords(DataCollectionSchema schema, Receiver<CsvPartition> receiver)
            throws InterruptedException {
        Envelope envelope = schema.getEnvelopeSchema();
        Struct source = offsetContext.getSourceInfoStruct();
        Instant now = Instant.now();

        // Envelope API: read/create(after, source, ts) · update(before, after, source, ts) · delete(before, source, ts)
        Struct envelopeStruct = switch (operation) {
            case READ    -> envelope.read(valueStruct, source, now);
            case CREATE  -> envelope.create(valueStruct, source, now);
            case UPDATE  -> envelope.update(null, valueStruct, source, now);
            case DELETE  -> envelope.delete(valueStruct, source, now);
            default      -> throw new IllegalStateException("Unexpected operation: " + operation);
        };

        receiver.changeRecord(partition, schema, operation, keyStruct, envelopeStruct, offsetContext, null);
    }

    @Override
    public CsvPartition getPartition() {
        return partition;
    }

    @Override
    public OffsetContext getOffset() {
        return offsetContext;
    }

    @Override
    public Envelope.Operation getOperation() {
        return operation;
    }
}
