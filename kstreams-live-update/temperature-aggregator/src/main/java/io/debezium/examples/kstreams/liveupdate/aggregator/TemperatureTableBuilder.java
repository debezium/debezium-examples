package io.debezium.examples.kstreams.liveupdate.aggregator;

import io.debezium.examples.kstreams.liveupdate.aggregator.model.CountAndSum;
import io.debezium.examples.kstreams.liveupdate.aggregator.model.Station;
import io.debezium.examples.kstreams.liveupdate.aggregator.model.TemperatureMeasurement;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.CountAndSumSerde;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.LongKeySerde;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.StationSerde;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.TemperatureMeasurementSerde;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Serialized;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TemperatureTableBuilder {
  public static KTable<String, String> avgTemperaturesByStation(StreamsBuilder builder) {

    Serde<Long> longKeySerde = new LongKeySerde();

    Serde<TemperatureMeasurement> temperatureMeasurementsSerde = new TemperatureMeasurementSerde();
    Serde<Station> stationSerde = new StationSerde();
    Serde<CountAndSum> countAndSumSerde = new CountAndSumSerde();

    KTable<Long, Station> stations = builder.table("dbserver1.inventory.stations", Consumed.with(longKeySerde, stationSerde));

    return builder.stream(
        "dbserver1.inventory.temperature_measurements",
        Consumed.with(longKeySerde, temperatureMeasurementsSerde)
    )
        .selectKey((k, v) -> v.stationId)
        .join(
            stations,
            (value1, value2) -> {
              value1.stationName = value2.name;
              return value1;
            },
            Joined.with(Serdes.Long(), temperatureMeasurementsSerde, null)
        )
        .selectKey((k, v) -> v.stationName)
        .groupByKey(Serialized.with(Serdes.String(), temperatureMeasurementsSerde))
        .aggregate(
            () -> new CountAndSum(), /* initializer */
            (aggKey, newValue, aggValue) -> {
              aggValue.count++;
              aggValue.sum += newValue.value;
              return aggValue;
            },
            Materialized.with(Serdes.String(), countAndSumSerde)
        )
        .mapValues(v -> {
          BigDecimal avg = BigDecimal.valueOf(v.sum / (double) v.count);
          avg = avg.setScale(1, RoundingMode.HALF_UP);
          return avg.doubleValue();
        })
        .mapValues(String::valueOf);
  }
}
