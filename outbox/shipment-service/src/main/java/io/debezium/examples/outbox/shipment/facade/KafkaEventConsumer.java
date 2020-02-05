/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.facade;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.reactive.messaging.kafka.KafkaMessage;

@ApplicationScoped
public class KafkaEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @Inject
    OrderEventHandler orderEventHandler;

    @Incoming("orders")
    public CompletionStage<Void> onMessage(KafkaMessage<String, String> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
                LOG.debug("Kafka message with key = {} arrived", message.getKey());

                String eventId = message.getHeaders().getOneAsString("id").orElseThrow(() -> new IllegalArgumentException("Expected record header 'id' not present"));
                String eventType = message.getHeaders().getOneAsString("eventType").orElseThrow(() -> new IllegalArgumentException("Expected record header 'eventType' not present"));

                orderEventHandler.onOrderEvent(
                        UUID.fromString(eventId),
                        eventType,
                        message.getKey(),
                        message.getPayload(),
                        message.getTimestamp()
                );
        });
    }
}
