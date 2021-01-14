/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.commons;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An entity mapping that represents a purchase order.
 */
public class ProtoPurchaseOrder {

    private Long id;

    private Long customerId;

    private LocalDateTime orderDate;

    private List<ProtoOrderLine> lineItems;

    @ProtoFactory
    public ProtoPurchaseOrder(Long id, Long customerId, LocalDateTime orderDate, List<ProtoOrderLine> lineItems) {
        this.id = id;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.lineItems = new ArrayList<>( lineItems );
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

    @ProtoField(number = 4, collectionImplementation = ArrayList.class)
    public List<ProtoOrderLine> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<ProtoOrderLine> lineItems) {
        this.lineItems = lineItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProtoPurchaseOrder that = (ProtoPurchaseOrder) o;
        return Objects.equals(id, that.id) && Objects.equals(customerId, that.customerId) && Objects
              .equals(orderDate, that.orderDate) && Objects.equals(lineItems, that.lineItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, orderDate, lineItems);
    }

    @Override
    public String toString() {
        return "ProtoPurchaseOrder{" + "id=" + id + ", customerId=" + customerId + ", orderDate='" + orderDate + '\''
              + ", lineItems=" + lineItems + '}';
    }
}
