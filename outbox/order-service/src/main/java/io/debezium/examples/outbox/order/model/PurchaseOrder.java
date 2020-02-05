/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

/**
 * An entity mapping that represents a purchase order.
 */
@Entity
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_order_ids")
    @SequenceGenerator(name = "purchase_order_ids", sequenceName = "seq_purchase_order", allocationSize = 50)
    private Long id;

    private long customerId;

    private LocalDateTime orderDate;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "purchaseOrder")
    private List<OrderLine> lineItems;

    PurchaseOrder() {
    }

    public PurchaseOrder(long customerId, LocalDateTime orderDate, List<OrderLine> lineItems) {
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.lineItems = new ArrayList<>( lineItems );
        lineItems.forEach( line -> line.setPurchaseOrder( this ) );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public List<OrderLine> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<OrderLine> lineItems) {
        this.lineItems = lineItems;
    }

    public OrderLineStatus updateOrderLine(long orderLineId, OrderLineStatus newStatus) {
        for (OrderLine orderLine : lineItems) {
            if (orderLine.getId() == orderLineId) {
                OrderLineStatus oldStatus = orderLine.getStatus();
                orderLine.setStatus(newStatus);
                return oldStatus;
            }
        }

        throw new EntityNotFoundException("Order does not contain line with id " + orderLineId);
    }

    public BigDecimal getTotalValue() {
        return lineItems.stream().map(OrderLine::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
