package io.debezium.examples.kstreams.liveupdate.aggregator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Serialized;

import io.debezium.examples.kstreams.liveupdate.aggregator.model.CountAndSum;
import io.debezium.examples.kstreams.liveupdate.aggregator.model.Station;
import io.debezium.examples.kstreams.liveupdate.aggregator.model.TemperatureMeasurement;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.CountAndSumSerde;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.LongKeySerde;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.StationSerde;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.TemperatureMeasurementSerde;

public class Main {

    public static void main(String[] args) {

        if(args.length != 3) {
            System.err.println("usage: java -jar <package> "
                    + Main.class.getName() + " <parent_topic> <children_topic> <bootstrap_servers>");
            System.exit(-1);
        }

        final String parentTopic = args[0];
        final String childrenTopic = args[1];
        final String bootstrapServers = args[2];

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streaming-aggregates-ddd");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10*1024);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder builder = new StreamsBuilder();

        Serde<Long> longKeySerde = new LongKeySerde();

        Serde<TemperatureMeasurement> temperatureMeasurementsSerde = new TemperatureMeasurementSerde();
        Serde<Station> stationSerde = new StationSerde();
        Serde<CountAndSum> countAndSumSerde = new CountAndSumSerde();

        KTable<Long, Station> stations = builder.table("dbserver1.inventory.stations", Consumed.with(longKeySerde, stationSerde));

        KTable<String, String> maxTemperaturesByStation = builder.stream(
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


        maxTemperaturesByStation.toStream().to("average_temperatures_by_station", Produced.with(Serdes.String(), Serdes.String()));
        maxTemperaturesByStation.toStream().print(Printed.toSysOut());

        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        streams.start();
    }
}
