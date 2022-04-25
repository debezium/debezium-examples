/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import io.debezium.examples.caching.commons.EntityNotFoundException;

/**
 * An entity mapping that represents a purchase order.
 */
@Entity
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_order_ids")
    @SequenceGenerator(name = "purchase_order_ids", sequenceName = "seq_purchase_order", allocationSize = 50)
    private Long id;

    @Column(name="customer_id")
    private Long customerId;

    @Column(name="order_date")
    private LocalDateTime orderDate;

    @Version
    private Integer version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "purchaseOrder")
    private List<OrderLine> lineItems;

    public PurchaseOrder() {
    }

    @ProtoFactory
    public PurchaseOrder(Long id, Long customerId, LocalDateTime orderDate, Integer version, List<OrderLine> lineItems) {
        this.id = id;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.version = version;
        this.lineItems = lineItems;
    }

    public PurchaseOrder(long customerId, LocalDateTime orderDate, List<OrderLine> lineItems) {
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.lineItems = new ArrayList<>( lineItems );
        lineItems.forEach( line -> line.setPurchaseOrder( this ) );
    }

    @ProtoField(number = 1)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ProtoField(number = 2)
    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    @ProtoField(number = 3)
    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    @ProtoField(number = 4)
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @ProtoField(number = 5, collectionImplementation = ArrayList.class)
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

    @Override
    public String toString() {
        return "PurchaseOrder [id=" + id + ", customerId=" + customerId + ", orderDate=" + orderDate + ", version="
                + version + ", lineItems=" + lineItems + "]";
    }
}
