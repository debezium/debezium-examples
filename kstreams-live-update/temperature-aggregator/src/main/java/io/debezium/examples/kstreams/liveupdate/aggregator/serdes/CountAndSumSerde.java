package io.debezium.examples.kstreams.liveupdate.aggregator.serdes;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import io.debezium.examples.kstreams.liveupdate.aggregator.model.CountAndSum;

public class CountAndSumSerde implements Serde<CountAndSum> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<CountAndSum> serializer() {
        return new Serializer<CountAndSum>() {

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {
            }

            @Override
            public byte[] serialize(String topic, CountAndSum data) {
                return String.valueOf(data.count + ";" + data.sum).getBytes(Charset.forName("UTF-8"));
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public Deserializer<CountAndSum> deserializer() {
        return new Deserializer<CountAndSum>() {

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {
            }

            @Override
            public CountAndSum deserialize(String topic, byte[] data) {
                if (data == null) {
                    return null;
                }

                String[] parts = new String(data, Charset.forName("UTF-8")).split(";");
                return new CountAndSum(Long.valueOf(parts[0]), Long.valueOf(parts[1]));
            }

            @Override
            public void close() {
            }
        };
    }
}
