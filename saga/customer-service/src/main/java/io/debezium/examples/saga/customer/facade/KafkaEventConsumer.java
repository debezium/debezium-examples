/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.customer.facade;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.saga.customer.model.CreditLimitEvent;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
public class KafkaEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @Inject
    CreditEventHandler creditEventHandler;

    @Inject
    Tracer tracer;

    @Incoming("credit")
    public CompletionStage<Void> onMessage(KafkaRecord<String, CreditLimitEvent> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
            try (final Scope span = tracer.scopeManager().activate(getOrdersSpanBuilder(message.getHeaders()).start())) {
                LOG.debug("Kafka message with key = {} arrived", message.getKey());

                String eventId = getHeaderAsString(message, "id");

                creditEventHandler.onCreditEvent(
                        UUID.fromString(eventId),
                        UUID.fromString(message.getKey()),
                        message.getPayload()
                );
            }
        }).thenRun(() -> message.ack());
    }

    private Tracer.SpanBuilder getOrdersSpanBuilder(Headers headers) {
        return tracer.buildSpan("orders").asChildOf(TracingKafkaUtils.extractSpanContext(headers, tracer));
    }

    private String getHeaderAsString(KafkaRecord<?, ?> record, String name) {
        Header header = record.getHeaders().lastHeader(name);
        if (header == null) {
            throw new IllegalArgumentException("Expected record header '" + name + "' not present");
        }

        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
