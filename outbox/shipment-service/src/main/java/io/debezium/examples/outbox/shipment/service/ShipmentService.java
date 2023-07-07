/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.service;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.inject.Inject;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.debezium.examples.outbox.shipment.model.Shipment;

@ApplicationScoped
public class ShipmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentService.class);

    @Inject
    Tracer tracer;

    @Inject
    Span span;

    @PersistenceContext
    EntityManager entityManager;

    @Transactional(value=TxType.MANDATORY)
    public void orderCreated(JsonNode event) {
        LOGGER.info("Processing 'OrderCreated' event: {}", event);

        span = tracer.spanBuilder("orderCreated").startSpan();
        try (final Scope scope = span.makeCurrent()){
            final long orderId = event.get("id").asLong();
            final long customerId = event.get("customerId").asLong();
            final LocalDateTime orderDate = LocalDateTime.parse(event.get("orderDate").asText());

            entityManager.persist(new Shipment(customerId, orderId, orderDate));
        } finally {
            span.end();
        }
    }

    @Transactional(value=TxType.MANDATORY)
    public void orderLineUpdated(JsonNode event) {
        LOGGER.info("Processing 'OrderLineUpdated' event: {}", event);
        span = tracer.spanBuilder("orderLineUpdated").startSpan();
        try (final Scope scope = span.makeCurrent()){
            span.addEvent("Processing 'OrderLineUpdated' event: "+event);
        } finally {
            span.end();
        }
    }
}
