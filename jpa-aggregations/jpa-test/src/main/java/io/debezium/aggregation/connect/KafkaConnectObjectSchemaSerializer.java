/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.aggregation.connect;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.debezium.aggregation.connect.KafkaConnectSchemaFactoryWrapper.KafkaConnectArraySchemaAdapter;
import io.debezium.aggregation.connect.KafkaConnectSchemaFactoryWrapper.KafkaConnectObjectSchemaAdapter;
import io.debezium.aggregation.connect.KafkaConnectSchemaFactoryWrapper.KafkaConnectSchemaAdapter;

public class KafkaConnectObjectSchemaSerializer extends StdSerializer<KafkaConnectObjectSchemaAdapter> {

        private boolean isRoot;

        public KafkaConnectObjectSchemaSerializer() {
            this(true);
        }

        public KafkaConnectObjectSchemaSerializer(boolean isRoot) {
            this(null);
            this.isRoot = isRoot;
        }

        public KafkaConnectObjectSchemaSerializer(Class<KafkaConnectObjectSchemaAdapter> t) {
            super(t);
        }

        @Override
        public void serialize(KafkaConnectObjectSchemaAdapter value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();

            if (isRoot) {
                gen.writeObjectFieldStart("schema");
            }

            gen.writeStringField("type", value.getConnectType());
            gen.writeArrayFieldStart("fields");

            for (KafkaConnectSchemaAdapter propertySchema : value.getFields()) {
                if (propertySchema instanceof KafkaConnectObjectSchemaAdapter) {
                    KafkaConnectObjectSchemaSerializer itemSerializer = new KafkaConnectObjectSchemaSerializer(false);
                    itemSerializer.serialize((KafkaConnectObjectSchemaAdapter) propertySchema, gen, provider);
                }
                else if (propertySchema instanceof KafkaConnectArraySchemaAdapter) {
                    KafkaConnectArraySchemaSerializer itemSerializer = new KafkaConnectArraySchemaSerializer();
                    itemSerializer.serialize((KafkaConnectArraySchemaAdapter) propertySchema, gen, provider);
                }
                else {
                    KafkaConnectSchemaSerializer itemSerializer = new KafkaConnectSchemaSerializer();
                    itemSerializer.serialize(propertySchema, gen, provider);
                }

            }

            gen.writeEndArray();
            gen.writeBooleanField("optional", value.isOptional());
            gen.writeStringField("name", value.getName());

            if (isRoot) {
                gen.writeEndObject();
            }
            else if (value.getField() != null) {
                gen.writeStringField("field", value.getField());
            }

            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(KafkaConnectObjectSchemaAdapter value, JsonGenerator gen, SerializerProvider serializers,
                TypeSerializer typeSer) throws IOException {
            serialize(value, gen, serializers);
        }
    }

