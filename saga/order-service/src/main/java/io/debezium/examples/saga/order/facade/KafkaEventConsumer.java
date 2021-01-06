/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.facade;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.saga.order.event.PaymentEvent;
import io.debezium.examples.saga.order.saga.OrderPlacementEventHandler;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
public class KafkaEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @Inject
    private OrderPlacementEventHandler eventHandler;

    @Incoming("paymentresponse")
    public CompletionStage<Void> onPaymentMessage(KafkaRecord<String, PaymentEvent> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
            LOG.debug("Kafka message with key = {} arrived", message.getKey());

            UUID messageId = UUID.fromString(getHeaderAsString(message, "id"));
            UUID sagaId = UUID.fromString(message.getKey());

            eventHandler.onPaymentEvent(sagaId, messageId, message.getPayload());

        }).thenRun(() -> message.ack());
    }

    private String getHeaderAsString(KafkaRecord<?, ?> record, String name) {
        Header header = record.getHeaders().lastHeader(name);
        if (header == null) {
            throw new IllegalArgumentException("Expected record header '" + name + "' not present");
        }

        return new String(header.value(), Charset.forName("UTF-8"));
    }
}
