package io.debezium.examples.kstreams.liveupdate.aggregator.serdes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.examples.kstreams.liveupdate.aggregator.model.TemperatureMeasurement;

public class TemperatureMeasurementSerde implements Serde<TemperatureMeasurement> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<TemperatureMeasurement> serializer() {
        return new TemperatureMeasurementSerializer();
    }

    @Override
    public Deserializer<TemperatureMeasurement> deserializer() {
        return new TemperatureMeasurementDeserializer();
    }

    private final class TemperatureMeasurementDeserializer implements Deserializer<TemperatureMeasurement> {

        private final ObjectMapper mapper = new ObjectMapper();
        private final JsonFactory factory = mapper.getFactory();

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public TemperatureMeasurement deserialize(String topic, byte[] data) {
            try {
                JsonParser parser = factory.createParser(data);
                JsonNode root = mapper.readTree(parser);
                JsonNode payload = root.get("payload");

                if (payload != null) {
                    return new TemperatureMeasurement(
                            payload.get("id").asLong(),
                            payload.get("station_id").asLong(),
                            Double.valueOf(payload.get("value").asText()),
                            ZonedDateTime.parse(payload.get("ts").asText())
                    );
                }
                else {
                    return new TemperatureMeasurement(
                            root.get("id").asLong(),
                            root.get("station_id").asLong(),
                            root.get("station_name").asText(),
                            Double.valueOf(root.get("value").asText()),
                            ZonedDateTime.parse(root.get("ts").asText())
                    );
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

    private final class TemperatureMeasurementSerializer implements Serializer<TemperatureMeasurement> {
        private final ObjectMapper mapper = new ObjectMapper();
        private final JsonFactory factory = mapper.getFactory();

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public byte[] serialize(String topic, TemperatureMeasurement data) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                JsonGenerator generator = factory.createGenerator(baos);

                generator.writeStartObject();
                generator.writeNumberField("id", data.id);
                generator.writeNumberField("station_id", data.stationId);
                generator.writeStringField("station_name", data.stationName);
                generator.writeNumberField("value", data.value);
                generator.writeStringField("ts", data.timestamp.toString());
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
