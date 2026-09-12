/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.data.Envelope;
import io.debezium.schema.DataCollectionSchema;
import io.debezium.schema.DatabaseSchema;
import org.apache.kafka.connect.data.Date;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;

import java.util.List;

/**
 * Holds the active Kafka Connect schema (key, value, envelope) for the CSV table.
 *
 * <p>Schema is derived from the CSV header line and rebuilt whenever a new header is encountered (schema evolution).
 * Callers invoke {@link #register} to update it.
 * Schema names follow the pattern {@code <logicalName>.<tableName>.Key/.Value/.Envelope}.
 */
class CsvSchema implements DatabaseSchema<CsvId> {

    private CsvCollectionSchema cachedSchema;

    /**
     * Builds and caches key/value/envelope schemas from the given column definitions.
     * Called on the initial schema header and again on each schema-evolution header.
     *
     * @param id   table identifier used for schema naming
     * @param cols ordered column definitions; first column is treated as the primary key
     * @param cfg  connector config; provides logical name and the source-info struct maker
     */
    void register(CsvId id, List<ColumnDef> cols, CommonConnectorConfig cfg) {
        String prefix = cfg.getLogicalName() + "." + id.identifier();

        // Key schema: single-field struct containing only the primary key (first column)
        ColumnDef keyCol = cols.get(0);
        Schema keySchema = SchemaBuilder.struct()
                .name(prefix + ".Key")
                .field(keyCol.name(), toKafkaSchema(keyCol.type()))
                .build();

        // Value schema must be optional: Envelope uses it directly as the "before"/"after" field
        // schema, and "before" is null for READ/CREATE events — non-optional would cause serialisation failure.
        SchemaBuilder valueBuilder = SchemaBuilder.struct().name(prefix + ".Value");
        for (ColumnDef col : cols) {
            valueBuilder.field(col.name(), toKafkaSchema(col.type()));
        }
        Schema valueSchema = valueBuilder.optional().build();

        Schema sourceSchema = cfg.getSourceInfoStructMaker().schema();

        // Envelope adds op / ts_ms / source / before / after fields around the value schema
        Envelope envelope = Envelope.defineSchema()
                .withName(prefix + ".Envelope")
                .withRecord(valueSchema)
                .withSource(sourceSchema)
                .build();

        cachedSchema = new CsvCollectionSchema(id, keySchema, envelope);
    }

    /** Returns the most recently registered schema, or {@code null} if none registered yet. */
    @Override
    public DataCollectionSchema schemaFor(CsvId csvId) {
        return cachedSchema;
    }

    /** Typed variant of {@link #schemaFor}. */
    CsvCollectionSchema getCurrentSchema() {
        return cachedSchema;
    }

    /** {@code true} once the first schema header has been processed. */
    @Override
    public boolean tableInformationComplete() {
        return cachedSchema != null;
    }

    /** No schema history topic needed for CSV. */
    @Override
    public boolean isHistorized() {
        return false;
    }

    @Override
    public void close() {
        // nothing to close
    }

    /**
     * Maps a {@link ColumnType} to the corresponding Kafka Connect {@link Schema}.
     * All schemas are optional to support schema evolution (adding columns without breaking consumers).
     */
    static Schema toKafkaSchema(ColumnType type) {
        return switch (type) {
            case INT -> Schema.OPTIONAL_INT32_SCHEMA;
            case LONG -> Schema.OPTIONAL_INT64_SCHEMA;
            case FLOAT -> Schema.OPTIONAL_FLOAT32_SCHEMA;
            case DOUBLE -> Schema.OPTIONAL_FLOAT64_SCHEMA;
            case BOOLEAN -> Schema.OPTIONAL_BOOLEAN_SCHEMA;
            case DATE -> Date.builder().optional().build();
            default -> Schema.OPTIONAL_STRING_SCHEMA;
        };
    }
}
