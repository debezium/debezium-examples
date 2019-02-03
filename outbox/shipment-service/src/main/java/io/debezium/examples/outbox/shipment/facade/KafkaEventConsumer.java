/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.facade;

import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class KafkaEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger( KafkaEventConsumer.class );

    private static final String GROUP_ID = "shipment-service";

    @Resource(name="java:comp/DefaultManagedExecutorService")
    private ExecutorService executorService;

    @Inject
    private OrderEventHandler orderEventHandler;

    @Inject
    @ConfigProperty(name="bootstrap.servers")
    private String bootstrapServers;

    @Inject
    @ConfigProperty(name="order.topic.name", defaultValue="orders")
    private String topicName;

    private final AtomicBoolean running = new AtomicBoolean(true);

    public void startConsumer(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOG.info("Launching Consumer");
        executorService.submit(new PollingLoop());
    }

    public void shutdownEngine(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        LOG.info("Closing consumer");
        running.set(false);
    }

    private class PollingLoop implements Runnable {


        @Override
        public void run() {
            Properties props = new Properties();
            props.put("bootstrap.servers", bootstrapServers);
            props.put("group.id", GROUP_ID);
            props.put("key.deserializer", StringDeserializer.class.getName());
            props.put("value.deserializer", StringDeserializer.class.getName());

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Arrays.asList(topicName));

            try {
                while (running.get()) {
                    ConsumerRecords<String, String> records = consumer.poll(1000);

                    for (ConsumerRecord<String, String> record : records) {
                        orderEventHandler.onOrderEvent(
                                UUID.fromString(new String(record.headers().lastHeader("eventId").value())),
                                record.key(),
                                record.value()
                        );
                    }
                }
            }
            catch(Exception e) {
                LOG.error("Polling loop failed", e);
            }
            finally {
                LOG.info("Polling loop finished");
                consumer.close();
            }
        }
    }
}
