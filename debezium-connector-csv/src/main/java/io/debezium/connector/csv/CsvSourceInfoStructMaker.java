/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.csv;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.connector.AbstractSourceInfoStructMaker;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;

/**
 * Builds the Kafka Connect schema and struct for the {@code source} field that
 * appears in every change event envelope.
 *
 * <p>In addition to the common Debezium fields (version, connector, name, ts_ms,
 * snapshot), the CSV source block includes:
 * <ul>
 *   <li>{@code file} – the name of the CSV database file</li>
 *   <li>{@code line} – the zero-based line number of the record in that file</li>
 * </ul>
 */
class CsvSourceInfoStructMaker extends AbstractSourceInfoStructMaker<CsvSourceInfo> {

    private Schema schema;

    @Override
    public void init(String connector, String version, CommonConnectorConfig config) {
        super.init(connector, version, config);
        schema = commonSchemaBuilder()
                .name("io.debezium.connector.csv.Source")
                .field("file", Schema.STRING_SCHEMA)
                .field("line", Schema.INT64_SCHEMA)
                .build();
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public Struct struct(CsvSourceInfo info) {
        return commonStruct(info)
                .put("file", info.getFileName())
                .put("line", info.getLineNumber());
    }
}
