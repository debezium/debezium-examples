/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.serdes;

import java.io.IOException;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * A {@link Serde} that (de-)serializes JSON. The {@link Deserializer} supports Debezium's CDC message format, i.e. for
 * such messages the values to be deserialized will be unwrapped from the {@code id} field (for keys) or from the
 * {@code after} field.
 *
 * @author Gunnar Morling
 *
 * @param <T> The object type
 */
public class ChangeEventAwareJsonSerde<T> implements Serde<T> {

    private final ObjectMapper mapper;
    private final ObjectReader reader;
    private boolean isKey;

    public ChangeEventAwareJsonSerde(Class<T> objectType) {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        this.reader = mapper.readerFor(objectType);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        this.isKey = isKey;
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<T> serializer() {
        return new JsonSerializer();
    }

    @Override
    public Deserializer<T> deserializer() {
        return new JsonDeserializer();
    }

    private final class JsonDeserializer implements Deserializer<T> {

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public T deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }

            try {
                JsonNode node = mapper.readTree(data);

                if (isKey) {
                    if (node.isObject()) {
                        if (node.has("payload")) {
                            return reader.readValue(node.get("payload").get("id"));
                        }
                        else {
                            return reader.readValue(node.get("id"));
                        }
                    }
                    else {
                        return reader.readValue(node);
                    }
                }
                else {
                    JsonNode payload = node.get("payload");
                    if (payload != null) {
                        return reader.readValue(payload.get("after"));
                    }
                    else {
                        if (node.has("before") && node.has("after") && node.has("source")) {
                            return reader.readValue(node.get("after"));
                        }
                        else {
                            return reader.readValue(node);
                        }
                    }
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
        }
    }

    private final class JsonSerializer implements Serializer<T> {

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public byte[] serialize(String topic, T data) {
            try {
                return mapper.writeValueAsBytes(data);
            }
            catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
        }
    }
}
