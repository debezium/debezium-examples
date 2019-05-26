/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.kstreams.liveupdate.aggregator.model.Category;
import io.debezium.examples.kstreams.liveupdate.aggregator.model.Order;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.ChangeEventAwareJsonSerde;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.StringWindowedSerde;
import io.debezium.examples.kstreams.liveupdate.aggregator.ws.ChangeEventsWebsocketEndpoint;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 * Starts up the KStreams pipeline once the source topics have been created.
 *
 * @author Gunnar Morling
 */
@ApplicationScoped
public class StreamsPipelineManager {

    private static final Logger LOG = LoggerFactory.getLogger( StreamsPipelineManager.class );

    @Inject
    @ConfigProperty(name="kafka.bootstrap.servers", defaultValue="kafka:9092")
    private String kafkaBootstrapServers;

    @Inject
    @ConfigProperty(name="orders.topic", defaultValue="dbserver1.inventory.orders")
    private String ordersTopic;

    @Inject
    @ConfigProperty(name="categories.topic", defaultValue="dbserver1.inventory.categories")
    private String categoriesTopic;

    @Inject
    private ChangeEventsWebsocketEndpoint websocketsEndPoint;

    private KafkaStreams streams;
    private ExecutorService executor;

    private boolean started = false;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("#### KStreamsPipeline#startKStreams()");

        Properties props = getProperties();
        StreamsBuilder builder = new StreamsBuilder();

        Serde<Long> longKeySerde = new ChangeEventAwareJsonSerde<>(Long.class);
        longKeySerde.configure(Collections.emptyMap(), true);

        Serde<Order> orderSerde = new ChangeEventAwareJsonSerde<>(Order.class);
        orderSerde.configure(Collections.emptyMap(), false);

        Serde<Category> categorySerde = new ChangeEventAwareJsonSerde<>(Category.class);
        categorySerde.configure(Collections.emptyMap(), false);

        KTable<Long, Category> category = builder.table(categoriesTopic, Consumed.with(longKeySerde, categorySerde));

        KStream<Windowed<String>, String> salesPerCategory = builder.stream(
                ordersTopic,
                Consumed.with(longKeySerde, orderSerde)
                )

                // Join with categories on category id
                .selectKey((k, v) -> v.categoryId)
                .join(
                        category,
                        (value1, value2) -> {
                            value1.categoryName = value2.name;
                            return value1;
                        },
                        Joined.with(Serdes.Long(), orderSerde, null)
                )

                // Group by category name, windowed by 5 sec
                .selectKey((k, v) -> v.categoryName)
                .groupByKey(Serialized.with(Serdes.String(), orderSerde))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(5).toMillis()))

                // Accumulate category sales per time window
                .aggregate(
                        () -> 0L, /* initializer */
                        (aggKey, newValue, aggValue) -> {
                            aggValue += newValue.salesPrice;
                            return aggValue;
                        },
                        Materialized.with(Serdes.String(), Serdes.Long())
                )
                .mapValues(v -> BigDecimal.valueOf(v)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .mapValues(v -> String.valueOf(v))

                // Push to WebSockets
                .toStream()
                .peek((k, v) -> {
                    websocketsEndPoint.getSessions().forEach(s -> {
                        try {
                            s.getBasicRemote().sendText("{ \"category\" : \"" + k.key() + "\", \"accumulated-sales\" : " + v + " }");
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });

        salesPerCategory.to(
                "sales_per_category",
                Produced.with(new StringWindowedSerde(), Serdes.String())
         );

        streams = new KafkaStreams(builder.build(), props);

        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            waitForTopicsToBeCreated(kafkaBootstrapServers);
            streams.start();
        });

        started = true;
    }

    public boolean isStarted() {
        return started;
    }

    private Properties getProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-aggregator-ws");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10*1024);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    void onStop(@Observes ShutdownEvent ev) {
        streams.close();
        executor.shutdown();
    }

    private void waitForTopicsToBeCreated(String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(config)) {
            AtomicBoolean topicsCreated = new AtomicBoolean(false);

            while (topicsCreated.get() == false) {
                LOG.info("Waiting for topics to be created");

                ListTopicsResult topics = adminClient.listTopics();
                topics.names().whenComplete((t, e) -> {
                    if (e != null) {
                        throw new RuntimeException(e);
                    }
                    else if (t.contains(categoriesTopic) && t.contains(ordersTopic)) {
                        LOG.info("Found topics '{}' and '{}'", categoriesTopic, ordersTopic);
                        topicsCreated.set(true);
                    }
                });

                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
