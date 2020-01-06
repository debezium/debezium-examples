/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.facade;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.kafka.KafkaMessage;

@ApplicationScoped
public class KafkaEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @Inject
    OrderEventHandler orderEventHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Incoming("orders")
    public CompletionStage<Void> onMessage(KafkaMessage<String, String> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
            try {
                LOG.debug("Kafka message with key = {} arrived", message.getKey());
                final Optional<String> eventId = message.getHeaders().getOneAsString("id");
                if (eventId.isPresent()) {
                    orderEventHandler.onOrderEvent(
                            UUID.fromString(eventId.get()),
                            message.getKey(),
                            objectMapper.readTree(message.getPayload()),
                            message.getTimestamp()
                    );
                }
                else {
                    LOG.warn("Skipping Kafka message with key = {}, id header was missing");
                }
            }
            catch (Throwable t) {
                LOG.error("Error in message processing", t);
            }
        });
    }
}
