/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.apache.pulsar;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.config.Configuration;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.examples.apache.pulsar.config.PropertyLoader;
import io.debezium.util.Clock;

public class PulsarProducer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PulsarProducer.class);

    private final Configuration config;
    private final JsonConverter valueConverter;
    private final JsonConverter keyConverter;
    private PulsarClient client;
    private final HashMap<String, Producer<String>> producerHashMap = new HashMap<>();
    private final Properties propConfig;

    private PulsarProducer() {

        InputStream propsInputStream = PulsarProducer.class.getClassLoader().getResourceAsStream("config.properties");
        propConfig = new Properties();
        try {
            propConfig.load(propsInputStream);
        }
        catch (IOException e) {
            logger.error("Couldn't load properties", e);
        }

        PropertyLoader.loadEnvironmentValues(propConfig);
        config = Configuration.from(propConfig);

        keyConverter = new JsonConverter();
        keyConverter.configure(config.asMap(), true);
        valueConverter = new JsonConverter();
        valueConverter.configure(config.asMap(), false);
    }

    private Producer<String> getProducer(String topic) {
        String topicFormat = propConfig.getProperty("pulsar.topic");
        String topicURI = MessageFormat.format(topicFormat, topic);
        Producer<String> producer = producerHashMap.get(topic);

        if (producer == null) {
            try {
                producer = client.newProducer(Schema.STRING)
                        .topic(topicURI)
                        .create();
            }
            catch (PulsarClientException e) {
                throw new RuntimeException(e);
            }

            producerHashMap.put(topic, producer);
        }

        return producer;
    }

    @Override
    public void run() {
        final EmbeddedEngine engine = EmbeddedEngine.create()
                .using(config)
                .using(this.getClass().getClassLoader())
                .using(Clock.SYSTEM)
                .notifying(this::sendRecord)
                .build();

        try {
            client = PulsarClient.builder()
                    .serviceUrl(propConfig.getProperty("pulsar.broker.address"))
                    .build();
        }
        catch (PulsarClientException e) {
            throw new RuntimeException(e);
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Requesting embedded engine to shut down");
            try {
                engine.stop();

                producerHashMap.forEach((topic, producer) -> {
                    logger.info(String.format("Closing producer for topic %s", topic));
                    try {
                        producerHashMap.get(topic).close();
                    }
                    catch (PulsarClientException e) {
                        logger.error("Couldn't close producer", e);
                    }
                    logger.info("Producer Closed");

                });
                client.close();
            }
            catch (PulsarClientException e) {
                throw new RuntimeException(e);
            }
        }));

        awaitTermination(executor);
    }

    /**
     * For Every record this method will be invoked* @param record
     */
    private void sendRecord(SourceRecord record) {
        final byte[] payload = valueConverter.fromConnectData("dummy", record.valueSchema(), record.value());
        final byte[] key = keyConverter.fromConnectData("dummy", record.keySchema(), record.key());
        logger.debug("Publishing Topic --> " + record.topic());
        logger.debug("Key -->" + new String(key));
        logger.debug("Payload --> " + new String(payload));
        Producer<String> producer;

        producer = getProducer(record.topic());

        while (true) {

            try {
                MessageId msgID = producer.newMessage().key(new String(key)).value(new String(payload)).send();
                logger.debug(String.format("Published message with Id %s", msgID));
                break;
            }
            catch (PulsarClientException e) {
                throw new RuntimeException("Couldn't send message", e);
            }
        }
    }

    private void awaitTermination(ExecutorService executor) {
        try {
            while (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.info("Waiting another 10 seconds for the embedded engine to shut down");
            }
        }
        catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    public static void main(String[] args) {
        new PulsarProducer().run();
    }
}
