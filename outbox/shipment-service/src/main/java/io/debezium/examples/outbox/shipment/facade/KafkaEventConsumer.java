/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.facade;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.context.Scope;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
public class KafkaEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @Inject
    OrderEventHandler orderEventHandler;

    @Inject
    Tracer tracer;

    @Inject
    Span span;

    @Incoming("orders")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<Void> onMessage(KafkaRecord<String, String> message) throws IOException {
        return CompletableFuture.runAsync(() -> {
                span = tracer.spanBuilder("onMessage").startSpan();
                try (final Scope scope = span.makeCurrent()){
                    LOG.debug("Kafka message with key = {} arrived", message.getKey());
                    span.addEvent("Kafka message with key = "+message.getKey()+" arrived");

                    String eventId = getHeaderAsString(message, "id");
                    String eventType = getHeaderAsString(message, "eventType");

                    orderEventHandler.onOrderEvent(
                            UUID.fromString(eventId),
                            eventType,
                            message.getKey(),
                            message.getPayload(),
                            message.getTimestamp()
                    );

                    message.ack();
                    span.addEvent("ack");
                }
                catch (Exception e) {
                    LOG.error("Error while preparing shipment");
                    span.addEvent("Error while preparing shipment");
                    throw e;
                }
                finally {
                    span.end();
                }
        });
    }

    private String getHeaderAsString(KafkaRecord<?, ?> record, String name) {
        Header header = record.getHeaders().lastHeader(name);
        if (header == null) {
            throw new IllegalArgumentException("Expected record header '" + name + "' not present");
        }

        return new String(header.value(), Charset.forName("UTF-8"));
    }
}
