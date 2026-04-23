/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.data.Envelope;
import io.debezium.schema.DataCollectionSchema;
import org.apache.kafka.connect.data.Schema;

/**
 * Kafka Connect schema for the single CSV "table".
 *
 * <p>The key schema is a struct containing only the primary-key column
 * (the first column defined in the CSV schema header).
 *
 * The value schema is wrapped in a Debezium {@link Envelope} that adds
 * the standard {@code op}, {@code ts_ms}, {@code before}, {@code after} and {@code source} fields.
 *
 * <p>Instances are created and cached by {@link CsvSchema#register}.
 */
class CsvCollectionSchema implements DataCollectionSchema {

    private final CsvId id;
    private final Schema keySchema;
    private final Envelope envelope;

    CsvCollectionSchema(CsvId id, Schema keySchema, Envelope envelope) {
        this.id = id;
        this.keySchema = keySchema;
        this.envelope = envelope;
    }

    @Override
    public CsvId id() {
        return id;
    }

    @Override
    public Schema keySchema() {
        return keySchema;
    }

    @Override
    public Envelope getEnvelopeSchema() {
        return envelope;
    }
}
