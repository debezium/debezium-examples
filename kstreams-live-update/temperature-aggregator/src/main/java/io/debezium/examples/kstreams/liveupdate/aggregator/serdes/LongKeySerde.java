/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.serdes;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LongKeySerde implements Serde<Long> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<Long> serializer() {
        return new Serializer<Long>() {

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {
            }

            @Override
            public byte[] serialize(String topic, Long data) {
                return String.valueOf(data).getBytes(Charset.forName("UTF-8"));
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public Deserializer<Long> deserializer() {
        return new Deserializer<Long>() {

            private final ObjectMapper mapper = new ObjectMapper();
            private final JsonFactory factory = mapper.getFactory();

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {
            }

            @Override
            public Long deserialize(String topic, byte[] data) {
                try {
                    JsonParser parser = factory.createParser(data);
                    JsonNode root = mapper.readTree(parser);

                    if(root.isNumber()) {
                        return root.asLong();
                    }
                    else {
                        return root.get("payload").get("id").asLong();
                    }
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void close() {
            }
        };
    }
}
