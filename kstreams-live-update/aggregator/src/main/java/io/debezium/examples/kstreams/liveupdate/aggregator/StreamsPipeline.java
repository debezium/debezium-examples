/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.internals.WindowedDeserializer;
import org.apache.kafka.streams.kstream.internals.WindowedSerializer;

import io.debezium.examples.kstreams.liveupdate.aggregator.model.Category;
import io.debezium.examples.kstreams.liveupdate.aggregator.model.Order;
import io.debezium.examples.kstreams.liveupdate.aggregator.model.ValueAggregator;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.ChangeEventAwareJsonSerde;

public class StreamsPipeline {

    public static KTable<Windowed<String>, String> avgSalesPricePerCategory(StreamsBuilder builder) {
        Serde<Long> longKeySerde = new ChangeEventAwareJsonSerde<>(Long.class);
        longKeySerde.configure(Collections.emptyMap(), true);

        Serde<Order> temperatureMeasurementsSerde = new ChangeEventAwareJsonSerde<>(Order.class);
        temperatureMeasurementsSerde.configure(Collections.emptyMap(), false);

        Serde<Category> stationSerde = new ChangeEventAwareJsonSerde<>(Category.class);
        stationSerde.configure(Collections.emptyMap(), false);

        Serde<ValueAggregator> countAndSumSerde = new ChangeEventAwareJsonSerde<>(ValueAggregator.class);
        stationSerde.configure(Collections.emptyMap(), false);

        KTable<Long, Category> stations = builder.table("dbserver1.inventory.categories", Consumed.with(longKeySerde, stationSerde));

        return builder.stream(
                "dbserver1.inventory.orders",
                Consumed.with(longKeySerde, temperatureMeasurementsSerde)
                )
                .selectKey((k, v) -> v.categoryId)
                .join(
                        stations,
                        (value1, value2) -> {
                            value1.categoryName = value2.name;
                            return value1;
                        },
                        Joined.with(Serdes.Long(), temperatureMeasurementsSerde, null)
                )
                .selectKey((k, v) -> v.categoryName)
                .groupByKey(Serialized.with(Serdes.String(), temperatureMeasurementsSerde))
                .windowedBy(TimeWindows.of(1000))
                .aggregate(
                        () -> new ValueAggregator(), /* initializer */
                        (aggKey, newValue, aggValue) -> {
                            return aggValue.increment(newValue.salesPrice);
                        },
                        Materialized.with(Serdes.String(), countAndSumSerde)
                )
                .mapValues(v -> BigDecimal.valueOf(v.sum)
                        .divide(BigDecimal.valueOf(v.count * 100), 2, RoundingMode.HALF_UP))
                .mapValues(String::valueOf);
    }

    public static Serde<Windowed<String>> getWindowedStringSerde() {
        WindowedSerializer<String> windowedSerializer = new WindowedSerializer<>(Serdes.String().serializer());
        WindowedDeserializer<String> windowedDeserializer = new WindowedDeserializer<>(Serdes.String().deserializer());

        return Serdes.serdeFrom(windowedSerializer, windowedDeserializer);
    }
}
