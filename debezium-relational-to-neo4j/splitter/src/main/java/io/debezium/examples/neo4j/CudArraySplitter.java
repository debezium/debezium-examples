/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.neo4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Split step for the Neo4jCudConverter SMT running in {@code output.mode=array}.
 * <p>
 * The SMT writes one JSON array of CUD events per source change event to keep node
 * events ordered before their relationship events. The Neo4j sink connector expects
 * one CUD event per Kafka record, so this Kafka Streams application reads each array
 * topic, explodes it into individual records (preserving order), and writes them to
 * the matching {@code neo4j.*} topic the sink consumes.
 */
public class CudArraySplitter {

    private static final Logger LOG = LoggerFactory.getLogger(CudArraySplitter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> TABLES = List.of("customers", "products", "orders", "order_items");
    private static final String SOURCE_PREFIX = "dbserver1.public.";
    private static final String DEST_PREFIX = "neo4j.";

    public static void main(String[] args) {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, env("APPLICATION_ID", "neo4j-cud-splitter"));
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env("BOOTSTRAP_SERVERS", "kafka:9092"));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.consumerPrefix("auto.offset.reset"), "earliest");

        final StreamsBuilder builder = new StreamsBuilder();
        for (final String table : TABLES) {
            builder.<String, String> stream(SOURCE_PREFIX + table, Consumed.with(Serdes.String(), Serdes.String()))
                    .flatMapValues(CudArraySplitter::splitArray)
                    .to(DEST_PREFIX + table, Produced.with(Serdes.String(), Serdes.String()));
        }

        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // The Debezium source connector is registered after the stack is up, so the
        // dbserver1.public.* source topics may not exist when this app first starts. Kafka Streams
        // treats missing source topics as fatal (ERROR state). Release the latch so the process
        // exits non-zero and the container's restart policy retries until the topics appear.
        streams.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.ERROR) {
                LOG.error("Kafka Streams entered ERROR state (source topics not created yet?); "
                        + "exiting so the container restarts and retries");
                latch.countDown();
            }
        });

        // Kafka Streams runs on daemon threads, so keep the main thread alive until the JVM is
        // asked to stop; the shutdown hook closes the topology and releases the latch.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            streams.close();
            latch.countDown();
        }));

        try {
            streams.start();
            LOG.info("Neo4j CUD splitter started for topics {}", TABLES);
            latch.await();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Exit non-zero on a fatal Streams error so `restart: on-failure` brings us back once the
        // source topics exist. A normal shutdown (SIGTERM) leaves the state non-ERROR and exits 0.
        if (streams.state() == KafkaStreams.State.ERROR) {
            System.exit(1);
        }
    }

    /**
     * Explode a JSON array value into its individual elements as JSON strings,
     * preserving order. Tombstones (null) and blanks yield no output; a value that
     * is already a single JSON object is passed through unchanged.
     */
    static List<String> splitArray(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            final JsonNode root = MAPPER.readTree(value);
            if (!root.isArray()) {
                return List.of(value);
            }
            final List<String> events = new ArrayList<>(root.size());
            for (final JsonNode element : root) {
                events.add(element.toString());
            }
            return events;
        }
        catch (Exception e) {
            LOG.warn("Skipping unparseable CUD record: {}", value, e);
            return List.of();
        }
    }

    private static String env(String name, String defaultValue) {
        final String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
