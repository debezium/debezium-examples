/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.kstreams.liveupdate.aggregator.model.Category;
import io.debezium.examples.kstreams.liveupdate.aggregator.model.Order;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.ChangeEventAwareJsonSerde;

public class StreamsPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(StreamsPipeline.class);

    public static KTable<Windowed<String>, String> salesPerCategory(StreamsBuilder builder) {
        Serde<Long> longKeySerde = new ChangeEventAwareJsonSerde<>(Long.class);
        longKeySerde.configure(Collections.emptyMap(), true);

        Serde<Order> orderSerde = new ChangeEventAwareJsonSerde<>(Order.class);
        orderSerde.configure(Collections.emptyMap(), false);

        Serde<Category> categorySerde = new ChangeEventAwareJsonSerde<>(Category.class);
        categorySerde.configure(Collections.emptyMap(), false);

        KTable<Long, Category> category = builder.table("dbserver1.inventory.categories", Consumed.with(longKeySerde, categorySerde));

        return builder.stream(
                "dbserver1.inventory.orders",
                Consumed.with(longKeySerde, orderSerde)
                )
                .selectKey((k, v) -> v.categoryId)
                .join(
                        category,
                        (value1, value2) -> {
                            value1.categoryName = value2.name;
                            return value1;
                        },
                        Joined.with(Serdes.Long(), orderSerde, null)
                )
                .selectKey((k, v) -> v.categoryName)
                .groupByKey(Serialized.with(Serdes.String(), orderSerde))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(5).toMillis()))
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
                .mapValues(v -> String.valueOf(v));
    }

    public static void waitForTopicsToBeCreated(String bootstrapServers) {
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
                    else if (t.contains("dbserver1.inventory.categories") && t.contains("dbserver1.inventory.orders")) {
                        LOG.info("Found topics 'dbserver1.inventory.categories' and 'dbserver1.inventory.orders'");
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
