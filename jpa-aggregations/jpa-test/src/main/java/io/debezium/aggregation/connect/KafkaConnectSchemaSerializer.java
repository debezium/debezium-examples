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

import io.debezium.aggregation.connect.KafkaConnectSchemaFactoryWrapper.KafkaConnectSchemaAdapter;

public class KafkaConnectSchemaSerializer extends StdSerializer<KafkaConnectSchemaAdapter> {

	    public KafkaConnectSchemaSerializer() {
	        this(null);
	    }

	    public KafkaConnectSchemaSerializer(Class<KafkaConnectSchemaAdapter> t) {
	        super(t);
	    }

        @Override
        public void serialize(KafkaConnectSchemaAdapter value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("type", value.getConnectType());
            gen.writeBooleanField("optional", value.isOptional());
            if (value.getField() != null) {
                gen.writeStringField("field", value.getField());
            }
            if (value.getName() != null) {
                gen.writeStringField("name", value.getName());
                gen.writeNumberField("version", 1);
            }
            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(KafkaConnectSchemaAdapter value, JsonGenerator gen, SerializerProvider serializers,
                TypeSerializer typeSer) throws IOException {
            serialize(value, gen, serializers);
        }
	}

