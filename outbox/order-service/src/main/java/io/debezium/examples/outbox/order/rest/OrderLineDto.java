/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.rest;

import java.math.BigDecimal;

import io.debezium.examples.outbox.order.model.OrderLine;
import io.debezium.examples.outbox.order.model.OrderLineStatus;
import io.debezium.examples.outbox.order.model.PurchaseOrder;

/**
 * A value object that represents an {@link OrderLine} for a {@link PurchaseOrder}.
 */
public class OrderLineDto {

    private Long id;
    private String item;
    private int quantity;
    private BigDecimal totalPrice;
    private OrderLineStatus status;

    public OrderLineDto() {
    }

    public OrderLineDto(String item, int quantity, BigDecimal totalPrice) {
        this.item = item;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = OrderLineStatus.ENTERED;
    }

    public OrderLineDto(long id, String item, int quantity, BigDecimal totalPrice, OrderLineStatus status) {
        this.id = id;
        this.item = item;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public OrderLineStatus getStatus() {
        return status;
    }

    public void setStatus(OrderLineStatus status) {
        this.status = status;
    }
}
