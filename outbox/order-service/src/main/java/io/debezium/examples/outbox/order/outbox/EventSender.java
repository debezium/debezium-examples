/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.outbox;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import io.debezium.examples.outbox.order.event.ExportedEvent;

@ApplicationScoped
public class EventSender {

    @PersistenceContext
    private EntityManager entityManager;

    public void onExportedEvent(@Observes ExportedEvent event) {
        OutboxEvent outboxEvent = new OutboxEvent(
                event.getAggregateType(), event.getAggregateId(), event.getType(), event.getPayload()
        );

        entityManager.persist(outboxEvent);
        entityManager.remove(outboxEvent);
    }
}
