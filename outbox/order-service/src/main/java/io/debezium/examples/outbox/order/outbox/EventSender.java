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

@ApplicationScoped
public class EventSender {

    @PersistenceContext
    private EntityManager entityManager;

    public void onExportedEvent(@Observes ExportedEvent event) {
        OutboxEvent outboxEvent = new OutboxEvent(
                event.getAggregateType(),
                event.getAggregateId(),
                event.getType(),
                event.getPayload(),
                event.getTimestamp()
        );

        // This will produce an INSERT followed by a DELETE;
        // So the events table will always be empty, but still both events will be captured from
        // the log by Debezium (and the latter will be ignored)
        entityManager.persist(outboxEvent);
        entityManager.remove(outboxEvent);
    }
}
