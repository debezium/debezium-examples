/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import io.debezium.examples.outbox.order.event.ExportedEvent;
import io.debezium.examples.outbox.order.event.OrderCreatedEvent;
import io.debezium.examples.outbox.order.event.OrderLineUpdatedEvent;
import io.debezium.examples.outbox.order.model.EntityNotFoundException;
import io.debezium.examples.outbox.order.model.OrderLineStatus;
import io.debezium.examples.outbox.order.model.PurchaseOrder;

@ApplicationScoped
public class OrderService {

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private Event<ExportedEvent> event;

    @Transactional
    public PurchaseOrder addOrder(PurchaseOrder order) {
        order = entityManager.merge(order);
        event.fire(OrderCreatedEvent.of(order));
        return order;
    }

    @Transactional
    public PurchaseOrder updateOrderLine(long orderId, long orderLineId, OrderLineStatus newStatus) {
        PurchaseOrder order = entityManager.find(PurchaseOrder.class, orderId);

        if (order == null) {
            throw new EntityNotFoundException("Order with id " + orderId + " couldn't be found");
        }

        OrderLineStatus oldStatus = order.updateOrderLine(orderLineId, newStatus);
        event.fire(OrderLineUpdatedEvent.of(orderId, orderLineId, newStatus, oldStatus));

        return order;
    }
}
