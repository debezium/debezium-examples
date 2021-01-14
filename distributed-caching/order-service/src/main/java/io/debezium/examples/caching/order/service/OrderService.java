/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.order.service;

import io.debezium.examples.caching.commons.ProtoOrderLine;
import io.debezium.examples.caching.commons.ProtoPurchaseOrder;
import io.debezium.examples.caching.order.model.EntityNotFoundException;
import io.debezium.examples.caching.order.model.OrderLine;
import io.debezium.examples.caching.order.model.OrderLineStatus;
import io.debezium.examples.caching.order.model.PurchaseOrder;
import io.quarkus.infinispan.client.Remote;
import org.infinispan.client.hotrod.RemoteCache;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

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
    @Remote("orders")
    RemoteCache<String, ProtoPurchaseOrder> orders;

    /**
     * Add a new {@link PurchaseOrder}.
     *
     * @param order the purchase order
     * @return the persisted purchase order
     */
    @Transactional
    public PurchaseOrder addOrder(PurchaseOrder order) {
        order = entityManager.merge(order);
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

        order.updateOrderLine(orderLineId, newStatus);

        return order;
    }

    public Optional<PurchaseOrder> getById(String id) {
        ProtoPurchaseOrder order = orders.get(id);
        if(order == null) {
            return Optional.empty();
        }
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(order.getId());
        purchaseOrder.setLineItems(new ArrayList<>());
        purchaseOrder.setCustomerId(order.getCustomerId());
        // TODO handle date
        purchaseOrder.setOrderDate(order.getOrderDate());

        for(ProtoOrderLine ol: order.getLineItems()) {
            OrderLine orderLine = new OrderLine(ol.getItem(), ol.getQuantity(), new BigDecimal(ol.getTotalPrice()));
            orderLine.setId(ol.getId());
            orderLine.setStatus(OrderLineStatus.valueOf(orderLine.getStatus().name()));
            purchaseOrder.getLineItems().add(orderLine);
        }

        return Optional.of(purchaseOrder);
    }
}
