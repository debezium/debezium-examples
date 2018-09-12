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

public class KafkaConnectArraySchemaSerializer extends StdSerializer<KafkaConnectArraySchemaAdapter> {

	        public KafkaConnectArraySchemaSerializer() {
	            this(null);
	        }

	        public KafkaConnectArraySchemaSerializer(Class<KafkaConnectArraySchemaAdapter> t) {
	            super(t);
	        }

	        @Override
	        public void serialize(KafkaConnectArraySchemaAdapter value, JsonGenerator gen, SerializerProvider provider) throws IOException {
	            gen.writeStartObject();
	            gen.writeStringField("type", value.getConnectType());

	            if (!value.isByteArray()) {
	                gen.writeFieldName("items");
	                if (value.getConnectItems() instanceof KafkaConnectObjectSchemaAdapter) {
	                    KafkaConnectObjectSchemaSerializer itemSerializer = new KafkaConnectObjectSchemaSerializer(false);
	                    itemSerializer.serialize((KafkaConnectObjectSchemaAdapter) value.getConnectItems(), gen, provider);
	                }
	                else {
	                    KafkaConnectSchemaSerializer itemSerializer = new KafkaConnectSchemaSerializer();
	                    itemSerializer.serialize(value.getConnectItems(), gen, provider);
	                }
	            }


	            gen.writeBooleanField("optional", value.isOptional());
	            gen.writeStringField("field", value.getField());

	            gen.writeEndObject();
	        }

	        @Override
	        public void serializeWithType(KafkaConnectArraySchemaAdapter value, JsonGenerator gen, SerializerProvider serializers,
	                TypeSerializer typeSer) throws IOException {
	            serialize(value, gen, serializers);
	        }
	    }