/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.ws;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Windowed;

import io.debezium.examples.kstreams.liveupdate.aggregator.StreamsPipeline;
import io.debezium.examples.kstreams.liveupdate.aggregator.serdes.StringWindowedSerde;

@ServerEndpoint("/example")
@ApplicationScoped
public class ChangeEventsWebsocketEndpoint {

    private static final Logger LOG = Logger.getLogger( ChangeEventsWebsocketEndpoint.class.getName() );

    private final Set<Session> sessions = Collections.newSetFromMap( new ConcurrentHashMap<>() );

    private KafkaStreams streams;

    @PostConstruct
    public void startKStreams() {
        final String bootstrapServers = "kafka:9092";

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-aggregator-ws");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10*1024);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder builder = new StreamsBuilder();

        final KStream<Windowed<String>, String> salesPerCategory = StreamsPipeline.salesPerCategory(builder)
                .toStream()
                .peek((k, v) -> sessions.forEach(s -> {
                    try {
                        s.getBasicRemote().sendText("{ \"category\" : \"" + k.key() + "\", \"accumulated-sales\" : " + v + " }");
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));

        salesPerCategory.to(
                "sales_per_category",
                Produced.with(new StringWindowedSerde(), Serdes.String())
         );

        streams = new KafkaStreams(builder.build(), props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        streams.start();
    }

    @PreDestroy
    public void closeKStreams() {
        streams.close();
    }

    @OnOpen
    public void open(Session session) {
        LOG.info( "Opening session: " + session.getId() );
        sessions.add(session);
    }

    @OnClose
    public void close(Session session, CloseReason c) {
        sessions.remove( session );
        LOG.info( "Closing: " + session.getId() );
    }
}
