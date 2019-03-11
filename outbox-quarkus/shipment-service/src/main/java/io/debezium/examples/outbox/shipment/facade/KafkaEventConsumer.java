/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.facade;

import java.util.Optional;
import java.util.UUID;
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
    public CompletionStage<Void> onMessage(KafkaMessage<String, String> message) {
        LOG.debug("Kafka message with key = {} arrived", message.getKey());
        final Optional<String> eventId = message.getHeaders().getOneAsString("eventId");
        if (eventId.isPresent()) {
            orderEventHandler.onOrderEvent(
                    UUID.fromString(eventId.get()),
                    message.getKey(),
                    message.getPayload()
            );
        }
        else {
            LOG.warn("Skipping Kafka message with key = {}, eventId header was missing");
        }
        return message.ack();
    }
}
