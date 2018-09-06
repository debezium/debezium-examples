/*
 *
 * Copyright (c) 2018 Nutanix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Yuvaraj L
 */
package io.debezium.examples.apache.pulsar;

import io.debezium.config.Configuration;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.examples.apache.pulsar.config.PropertyLoader;
import io.debezium.util.Clock;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.pulsar.client.api.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class PulsarProducer implements Runnable {
    private final Configuration config;
    private final JsonConverter valueConverter;
    private final JsonConverter keyConverter;
    private PulsarClient client;
    private HashMap<String, Producer<String>> producerHashMap = new HashMap<>();
    private Properties propConfig;
    private Logger logger = Logger.getLogger(PulsarProducer.class);

    private PulsarProducer() {

        InputStream propsInputStream = PulsarProducer.class.getClassLoader().getResourceAsStream("config.properties");
        propConfig = new Properties();
        try {
            propConfig.load(propsInputStream);
        } catch (IOException e) {
            logger.error(e);
        }
        PropertyLoader.loadEnvironmentValues(propConfig);
        config = Configuration.from(propConfig);

//                /* begin engine properties */
//                .with("connector.class",
//                        "io.debezium.connector.postgresql.PostgresConnector")
//                .with("offset.storage",
//                        "org.apache.kafka.connect.storage.FileOffsetBackingStore")
//                .with("offset.storage.file.filename",
//                        "offset.dat")
//                .with("offset.flush.interval.ms", 5000)
//                /* begin connector properties */
//                .with("name", "my-sql-connector")
//                .with("database.hostname", "192.168.33.10")
//                .with("database.port", 5432)
//                .with("database.user", "postgres")
//                .with("database.password", "password")
//                .with("database.dbname", "test")
//                .with("plugin.name", "wal2json")
//                .with("database.server.name", "test")
//                .with("database.history",
//                        "io.debezium.relational.history.FileDatabaseHistory")
//                .with("database.history.file.filename",
//                        "dbhistory.dat")
//                .build();
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
            } catch (PulsarClientException e) {
                logger.error(e);
                return null;
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

        } catch (PulsarClientException e) {
            logger.error(e);
        }

        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Going to Halt");
            try {
                engine.stop();
                producerHashMap.forEach((topic, producer) -> {
                    System.out.println(String.format("Closing producer for topic %s", topic));
                    try {
                        producerHashMap.get(topic).close();
                    } catch (PulsarClientException e) {
                        logger.error(e);
                    }
                    System.out.println("Producer Closed");

                });
                client.close();
            } catch (PulsarClientException e) {
                logger.error(e);
            }

            try {
                engine.await(30, TimeUnit.SECONDS);
                mainThread.join();
            } catch (InterruptedException e) {
                logger.error(e);
            }
        }));


        engine.run();
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
        while (producer == null) {
            try {
                logger.error("Unable to create Producer . Retrying after 10 seconds");
                Thread.sleep(10000);
                producer = getProducer(record.topic());

            } catch (InterruptedException e) {
                logger.error(e);
            }

        }

        while (true) {

            try {

                MessageId msgID = producer.newMessage().key(new String(key)).value(new String(payload)).send();
                logger.debug(String.format("Published message with Id %s", msgID));
                break;
            } catch (PulsarClientException e) {
                logger.error(e);
                try {
                    logger.error("Unable to publish the message.Retrying after 10 seconds.");
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    logger.error(e1);
                }
            }
        }

    }

    public static void main(String[] args) {
        new PulsarProducer().run();
    }

}
