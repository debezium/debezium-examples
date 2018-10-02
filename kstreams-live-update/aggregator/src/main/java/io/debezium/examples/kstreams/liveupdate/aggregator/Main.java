/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator;

import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Windowed;

public class Main {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("usage: java -jar <package> " + Main.class.getName() + " <bootstrap_servers>");
            System.exit(-1);
        }

        final String bootstrapServers = args[0];

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "temperature-aggregator");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder builder = new StreamsBuilder();
        final KTable<Windowed<String>, String> avgSalesPricePerCategory = StreamsPipeline
                .avgSalesPricePerCategory(builder);

        avgSalesPricePerCategory.toStream()
            .to(
                    "average_sales_price_per_category",
                    Produced.with(StreamsPipeline.getWindowedStringSerde(), Serdes.String())
             );

        avgSalesPricePerCategory.toStream().print(Printed.toSysOut());

        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        streams.start();
    }
}
