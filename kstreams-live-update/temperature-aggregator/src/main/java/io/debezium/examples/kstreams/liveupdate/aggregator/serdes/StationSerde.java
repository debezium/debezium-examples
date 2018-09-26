package io.debezium.examples.kstreams.liveupdate.aggregator.serdes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.examples.kstreams.liveupdate.aggregator.model.Station;

public class StationSerde implements Serde<Station> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<Station> serializer() {
        return new StationSerializer();
    }

    @Override
    public Deserializer<Station> deserializer() {
        return new StationDeserializer();
    }

    private final class StationDeserializer implements Deserializer<Station> {

        private final ObjectMapper mapper = new ObjectMapper();
        private final JsonFactory factory = mapper.getFactory();

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public Station deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }

            try {
                JsonParser parser = factory.createParser(data);
                JsonNode root = mapper.readTree(parser);
                JsonNode payload = root.get("payload");
                JsonNode station = payload != null ? payload : root;

                return new Station(
                        station.get("id").asLong(),
                        station.get("name").asText()
                );
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
        }
    }

    private final class StationSerializer implements Serializer<Station> {
        private final ObjectMapper mapper = new ObjectMapper();
        private final JsonFactory factory = mapper.getFactory();

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public byte[] serialize(String topic, Station data) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                JsonGenerator generator = factory.createGenerator(baos);

                generator.writeStartObject();
                generator.writeNumberField("id", data.id);
                generator.writeStringField("name", data.name);
                generator.writeEndObject();

                generator.flush();
                return baos.toByteArray();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
        }
    }
}
