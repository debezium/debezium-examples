/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator;

import static org.apache.kafka.common.serialization.Serdes.Long;
import static org.apache.kafka.common.serialization.Serdes.String;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.kstreams.liveupdate.aggregator.model.Category;
import io.debezium.examples.kstreams.liveupdate.aggregator.model.Order;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.StringWindowedSerde;
import io.debezium.examples.kstreams.liveupdate.aggregator.ws.ChangeEventsWebsocketEndpoint;
import io.debezium.serde.Serdes;
/**
 * Starts up the KStreams pipeline once the source topics have been created.
 *
 * @author Gunnar Morling
 */
@ApplicationScoped
public class StreamsPipelineManager {

    private static final Logger LOG = LoggerFactory.getLogger( StreamsPipelineManager.class );

    @Inject
    @ConfigProperty(name="orders.topic")
    String ordersTopic;

    @Inject
    @ConfigProperty(name="categories.topic")
    String categoriesTopic;

    @Inject
    ChangeEventsWebsocketEndpoint websocketsEndPoint;

    @Produces
    Topology createStreamTopology() {
        LOG.info("Creating stream topology");

        StreamsBuilder builder = new StreamsBuilder();

        Serde<Long> longKeySerde = Serdes.payloadJson(Long.class);
        longKeySerde.configure(Collections.emptyMap(), true);

        Serde<Order> orderSerde = Serdes.payloadJson(Order.class);
        orderSerde.configure(Collections.singletonMap("from.field", "after"), false);

        Serde<Category> categorySerde = Serdes.payloadJson(Category.class);
        categorySerde.configure(Collections.singletonMap("from.field", "after"), false);

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
                        Joined.with(Long(), orderSerde, null)
                )

                // Group by category name, windowed by 5 sec
                .selectKey((k, v) -> v.categoryName)
                .groupByKey(Grouped.with(String(), orderSerde))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(5)))

                // Accumulate category sales per time window
                .aggregate(
                        () -> 0L, /* initializer */
                        (aggKey, newValue, aggValue) -> {
                            aggValue += newValue.salesPrice;
                            return aggValue;
                        },
                        Materialized.with(String(), Long())
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
                Produced.with(new StringWindowedSerde(), String())
         );

        return builder.build();
    }
}
