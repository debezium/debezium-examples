/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.serdes;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.kstream.Windowed;

public class StringWindowedSerde implements Serde<Windowed<String>> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<Windowed<String>> serializer() {
        return new StringWindowedSerializer();
    }

    @Override
    public Deserializer<Windowed<String>> deserializer() {
        throw new UnsupportedOperationException();
    }

    private final class StringWindowedSerializer implements Serializer<Windowed<String>> {
        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public byte[] serialize(String topic, Windowed<String> data) {
            return String.format(
                    "%s (%s - %s)",
                    data.key(),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(data.window().start()), ZoneId.of("UTC")),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(data.window().end()), ZoneId.of("UTC"))
            ).getBytes(Charset.forName("UTF-8"));
        }

        @Override
        public void close() {
        }
    }
}
