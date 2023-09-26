/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.cacheupdater.streams.model;

public class OrderLineAndPurchaseOrder {

    public OrderLine orderLine;
    public PurchaseOrder purchaseOrder;

    public OrderLineAndPurchaseOrder() {
    }

    public OrderLineAndPurchaseOrder(OrderLine orderLine, PurchaseOrder purchaseOrder) {
        this.orderLine = orderLine;
        this.purchaseOrder = purchaseOrder;
    }

    public static OrderLineAndPurchaseOrder create(OrderLine orderLine, PurchaseOrder purchaseOrder) {
        return new OrderLineAndPurchaseOrder(orderLine, purchaseOrder);
    }

    public OrderLine orderLine() {
        return orderLine;
    }

    @Override
    public String toString() {
        return "OrderLineAndPurchaseOrder [orderLine=" + orderLine + ", purchaseOrder=" + purchaseOrder + "]";
    }
}
