/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.routingsmt;

import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;

/**
 * Takes Kafka Connect schema and JSON payload from a message and expands it into a properly typed record.
 *
 * @author Gunnar Morling
 *
 * @param <R>
 */
public class EventRouter<R extends ConnectRecord<R>> implements Transformation<R> {

    public EventRouter() {

    }
    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public R apply(R record) {
        Struct struct = (Struct) record.value();
        String op = struct.getString("op");

        // ignoring deletions in the events table
        if (op.equals("d")) {
            return null;
        }
        else if (op.equals("c")) {
            Long timestamp = struct.getInt64("ts_ms");
            Struct after = struct.getStruct("after");

            String key = after.getString("aggregateid");
            String topic = after.getString("aggregatetype");

            String eventId = after.getString("id");
            String eventType = after.getString("type");
            String payload = after.getString("payload");

            Schema valueSchema = SchemaBuilder.struct()
                .field("eventId", after.schema().field("id").schema())
                .field("eventType", after.schema().field("type").schema())
                .field("ts_ms", struct.schema().field("ts_ms").schema())
                .field("payload", after.schema().field("payload").schema())
                .build();

            Struct value = new Struct(valueSchema)
                .put("eventId", eventId)
                .put("eventType", eventType)
                .put("ts_ms", timestamp)
                .put("payload", payload);

            return record.newRecord(topic, null, Schema.STRING_SCHEMA, key, valueSchema, value,
                    record.timestamp());
        }
        else {
            throw new IllegalArgumentException("Record of unexpected op type: " + record);
        }
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef();
    }

    @Override
    public void close() {
    }
}
