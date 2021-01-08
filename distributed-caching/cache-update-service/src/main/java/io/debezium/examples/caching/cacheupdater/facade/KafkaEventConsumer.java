/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.cacheupdater.facade;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
public class KafkaEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @Incoming("orders")
    public CompletionStage<Void> onPurchaseOrder(KafkaRecord<String, String> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
            System.out.println("Received PO: " + message.getKey() + " - " + message.getPayload());
        }).thenRun(() -> message.ack());
    }

    @Incoming("orderlines")
    public CompletionStage<Void> onOrderLine(KafkaRecord<String, String> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
            System.out.println("Received OL: " + message.getKey() + " - " + message.getPayload());
        }).thenRun(() -> message.ack());
    }
}
