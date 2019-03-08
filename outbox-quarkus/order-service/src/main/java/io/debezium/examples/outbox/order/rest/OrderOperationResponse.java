/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.rest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import io.debezium.examples.outbox.order.model.PurchaseOrder;

public class OrderOperationResponse {

    private final long id;
    private final long customerId;
    private final LocalDateTime orderDate;
    private final List<OrderLineDto> lineItems;

    public OrderOperationResponse(long id, long customerId, LocalDateTime orderDate, List<OrderLineDto> lineItems) {
        this.id = id;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.lineItems = lineItems;
    }

    public static OrderOperationResponse from(PurchaseOrder order) {
        List<OrderLineDto> lines = order.getLineItems()
                .stream()
                .map(l -> new OrderLineDto(l.getId(), l.getItem(), l.getQuantity(), l.getTotalPrice(), l.getStatus()))
                .collect(Collectors.toList());

        return new OrderOperationResponse(order.getId(), order.getCustomerId(), order.getOrderDate(), lines);
    }

    public long getId() {
        return id;
    }

    public long getCustomerId() {
        return customerId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public List<OrderLineDto> getLineItems() {
        return lineItems;
    }
}
