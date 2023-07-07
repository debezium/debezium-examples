/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.messagelog;

import java.time.Instant;
import java.util.UUID;

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

@ApplicationScoped
public class MessageLog {
    private static final Logger LOG = LoggerFactory.getLogger(MessageLog.class);

    @Inject
    Tracer tracer;

    @Inject
    Span span;

    @PersistenceContext
    EntityManager entityManager;

    @Transactional(value=TxType.MANDATORY)
    public void processed(UUID eventId) {
        span = tracer.spanBuilder("processed").startSpan();
        try (final Scope scope = span.makeCurrent()){
            entityManager.persist(new ConsumedMessage(eventId, Instant.now()));
        } finally {
            span.end();
        }
    }

    @Transactional(value=TxType.MANDATORY)
    public boolean alreadyProcessed(UUID eventId) {
        LOG.debug("Looking for event with id {} in message log", eventId);
        span = tracer.spanBuilder("alreadyProcessed").startSpan();
        try (final Scope scope = span.makeCurrent()){
            return entityManager.find(ConsumedMessage.class, eventId) != null;
        } finally {
            span.end();
        }
    }
}
