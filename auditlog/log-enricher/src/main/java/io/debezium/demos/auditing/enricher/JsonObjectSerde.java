package io.debezium.demos.auditing.enricher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * A {@link Serde} that (de-)serializes JSON.
 */
@RegisterForReflection
public class JsonObjectSerde implements Serde<JsonObject> {

    public JsonObjectSerde() {
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<JsonObject> serializer() {
        return new JsonSerializer();
    }

    @Override
    public Deserializer<JsonObject> deserializer() {
        return new JsonDeserializer();
    }

    private final class JsonDeserializer implements Deserializer<JsonObject> {

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public JsonObject deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }

            try (JsonReader reader = Json.createReader(new ByteArrayInputStream(data))) {
                return reader.readObject();
            }
        }

        @Override
        public void close() {
        }
    }

    private final class JsonSerializer implements Serializer<JsonObject> {

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public byte[] serialize(String topic, JsonObject data) {
            if (data == null) {
                return null;
            }

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                Json.createWriter(output).writeObject(data);
                return output.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
        }
    }
}
