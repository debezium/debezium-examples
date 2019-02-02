/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.log;

import java.time.Instant;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@ApplicationScoped
public class MessageLog {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(value=TxType.MANDATORY)
    public void processed(UUID eventId) {
        entityManager.persist(new ConsumedMessage(eventId, Instant.now()));
    }

    @Transactional(value=TxType.MANDATORY)
    public boolean alreadyProcessed(UUID eventId) {
        return entityManager.find(ConsumedMessage.class, eventId) != null;
    }
}
