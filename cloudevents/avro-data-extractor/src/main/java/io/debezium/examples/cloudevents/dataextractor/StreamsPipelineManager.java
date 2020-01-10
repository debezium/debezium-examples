/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.cloudevents.dataextractor;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.debezium.examples.cloudevents.dataextractor.model.CloudEvent;
import io.debezium.serde.DebeziumSerdes;
/**
 * Starts up the KStreams pipeline once the source topics have been created.
 *
 * @author Gunnar Morling
 */
@ApplicationScoped
public class StreamsPipelineManager {

    private static final Logger LOG = LoggerFactory.getLogger( StreamsPipelineManager.class );

    @Inject
    @ConfigProperty(name="json.avro.customers.topic")
    String jsonAvroCustomersTopic;

    @Inject
    @ConfigProperty(name="json.avro.extracted.topic")
    String jsonAvroExtractedTopic;

    @Inject
    @ConfigProperty(name="avro.avro.customers.topic")
    String avroAvroCustomersTopic;

    @Inject
    @ConfigProperty(name="avro.avro.extracted.topic")
    String avroAvroExtractedTopic;

    @Inject
    @ConfigProperty(name="schema.registry.url")
    String schemaRegistryUrl;

    @Produces
    Topology createStreamTopology() {
        LOG.info("Creating stream topology");

        StreamsBuilder builder = new StreamsBuilder();

        Serde<Long> longKeySerde = DebeziumSerdes.payloadJson(Long.class);
        longKeySerde.configure(Collections.emptyMap(), true);
        Serde<CloudEvent> orderSerde = DebeziumSerdes.payloadJson(CloudEvent.class);
        orderSerde.configure(Collections.emptyMap(), false);

        builder.stream(jsonAvroCustomersTopic, Consumed.with(longKeySerde, orderSerde))
            .mapValues(ce -> ce.data)
            .to(jsonAvroExtractedTopic, Produced.with(longKeySerde, Serdes.ByteArray()));

        Serde<GenericRecord> genericAvroSerde = new GenericAvroSerde();
        Map<String, String> config = Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        genericAvroSerde.configure(config, false);

        builder.stream(avroAvroCustomersTopic, Consumed.with(longKeySerde, genericAvroSerde))
            .mapValues(ce -> ((ByteBuffer) ce.get("data")).array())
            .to(avroAvroExtractedTopic, Produced.with(longKeySerde, Serdes.ByteArray()));

        return builder.build();
    }
}
