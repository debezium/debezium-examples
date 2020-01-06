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

import io.debezium.examples.outbox.order.event.InvoiceCreatedEvent;
import io.debezium.examples.outbox.order.event.OrderCreatedEvent;
import io.debezium.examples.outbox.order.event.OrderLineUpdatedEvent;
import io.debezium.examples.outbox.order.model.EntityNotFoundException;
import io.debezium.examples.outbox.order.model.OrderLineStatus;
import io.debezium.examples.outbox.order.model.PurchaseOrder;
import io.debezium.quarkus.outbox.ExportedEvent;

/**
 * An application-scoped bean that facilitates {@link PurchaseOrder} business functionality.
 *
 * @author Chris Cranford
 */
@ApplicationScoped
public class OrderService {
    @PersistenceContext
    EntityManager entityManager;

    @Inject
    Event<ExportedEvent> event;

    /**
     * Add a new {@link PurchaseOrder}.
     *
     * @param order the purchase order
     * @return the persisted purchase order
     */
    @Transactional
    public PurchaseOrder addOrder(PurchaseOrder order) {
        order = entityManager.merge(order);

        // Fire events for newly created PurchaseOrder
        event.fire(OrderCreatedEvent.of(order));
        event.fire(InvoiceCreatedEvent.of(order));

        return order;
    }

    /**
     * Update the a {@link PurchaseOrder} line's status.
     *
     * @param orderId the purchase order id
     * @param orderLineId the purchase order line id
     * @param newStatus the new order line status
     * @return the updated purchase order
     */
    @Transactional
    public PurchaseOrder updateOrderLine(long orderId, long orderLineId, OrderLineStatus newStatus) {
        PurchaseOrder order = entityManager.find(PurchaseOrder.class, orderId);
        if (order == null) {
            throw new EntityNotFoundException("Order with id " + orderId + " could not be found");
        }

        OrderLineStatus oldStatus = order.updateOrderLine(orderLineId, newStatus);
        event.fire(OrderLineUpdatedEvent.of(orderId, orderLineId, newStatus, oldStatus));

        return order;
    }
}
