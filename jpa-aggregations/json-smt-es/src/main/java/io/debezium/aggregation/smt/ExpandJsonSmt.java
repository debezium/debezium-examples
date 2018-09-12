/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.aggregation.smt;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.transforms.Transformation;

/**
 * Takes Kafka Connect schema and JSON payload from a message and expands it into a properly typed record.
 *
 * @author Gunnar Morling
 *
 * @param <R>
 */
public class ExpandJsonSmt<R extends ConnectRecord<R>> implements Transformation<R> {

    private JsonConverter converter;

    @Override
    public void configure(Map<String, ?> configs) {
        Map<String, String> config = new HashMap<>();
        config.put("converter.type", "value");
        // config.put("schemas.enable", "false");

        converter = new JsonConverter();
        converter.configure(config);
    }

    @Override
    public R apply(R record) {
        // we're only interested in actual CDC records not schema change events
        if (record.valueSchema() == null || !record.valueSchema().name().endsWith(".Envelope")) {
            return record;
        }

        Struct struct = (Struct) record.value();
        String op = struct.getString("op");

        if (op.equals("r") || op.equals("c") || op.equals("u")) {
            Struct after = struct.getStruct("after");
            Object id = after.get("rootId");
            String rootType = after.getString("rootType");
            String keySchema = after.getString("keySchema");
            String valueSchema = after.getString("valueSchema");
            String materialization = after.getString("materialization");

            String valueEnvelope = "{ " + valueSchema.substring(2, valueSchema.length() - 2) + ", \"payload\" : " + materialization
                    + "}";

            SchemaAndValue value = converter.toConnectData("dummy", valueEnvelope.getBytes());

            String keyEnvelope = "{ " + keySchema.substring(2, keySchema.length() - 2) + ", \"payload\" : " + id
                    + "}";

            SchemaAndValue key = converter.toConnectData("dummy", keyEnvelope.getBytes());

            return record.newRecord(rootType, record.kafkaPartition(), key.schema(), key.value(), value.schema(),
                    value.value(), record.timestamp());
        }
        else if (op.equals("d")) {
            Struct before = struct.getStruct("before");
            Object id = before.get("rootId");
            String rootType = before.getString("rootType");
            String keySchema = before.getString("keySchema");

            String keyEnvelope = "{ " + keySchema.substring(2, keySchema.length() - 2) + ", \"payload\" : " + id
                    + "}";

            SchemaAndValue key = converter.toConnectData("dummy", keyEnvelope.getBytes());

            return record.newRecord(rootType, record.kafkaPartition(), key.schema(), key.value(), null, null,
                    record.timestamp());
        }
        else {
            throw new IllegalArgumentException("Unexpected record type: " + record);
        }
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef();
    }

    @Override
    public void close() {
        converter.close();
    }
}
