/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.order.rest;

import io.debezium.examples.caching.commons.OrderLine;
import io.debezium.examples.caching.commons.PurchaseOrder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A value object that represents a request to create a {@link PurchaseOrder}.
 */
public class CreateOrderRequest {

    private long customerId;
    private LocalDateTime orderDate;
    private List<OrderLineDto> lineItems;

    public CreateOrderRequest() {
        this.lineItems = new ArrayList<>();
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

    public List<OrderLineDto> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<OrderLineDto> lineItems) {
        this.lineItems = lineItems;
    }

    public PurchaseOrder toOrder() {
        List<OrderLine> lines = lineItems.stream()
                .map(l -> new OrderLine(l.getItem(), l.getQuantity(), l.getTotalPrice()))
                .collect(Collectors.toList());

        return new PurchaseOrder(customerId, orderDate, lines);
    }
}
