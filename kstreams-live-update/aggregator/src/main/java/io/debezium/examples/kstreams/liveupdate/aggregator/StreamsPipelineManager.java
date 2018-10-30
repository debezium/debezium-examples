/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Windowed;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.StringWindowedSerde;
import io.debezium.examples.kstreams.liveupdate.aggregator.ws.ChangeEventsWebsocketEndpoint;

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
    private ChangeEventsWebsocketEndpoint websocketsEndPoint;

    private KafkaStreams streams;
    private ExecutorService executor;

    public void startKStreams(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOG.info("#### KStreamsPipeline#startKStreams()");

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-aggregator-ws");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10*1024);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder builder = new StreamsBuilder();

        final KStream<Windowed<String>, String> salesPerCategory = StreamsPipeline.salesPerCategory(builder)
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
            StreamsPipeline.waitForTopicsToBeCreated(kafkaBootstrapServers);
            streams.start();
        });
    }

    @PreDestroy
    public void closeKStreams() {
        streams.close();
    }
}
