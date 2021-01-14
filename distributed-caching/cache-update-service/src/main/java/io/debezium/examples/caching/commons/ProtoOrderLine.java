/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.commons;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Objects;

public class ProtoOrderLine {

    private Long id;

    private String item;

    private Integer quantity = 0;

    private Double totalPrice;

    private ProtoOrderLineStatus status;

    private Long orderId;

    @ProtoFactory
    public ProtoOrderLine(Long id, String item, Integer quantity, Double totalPrice, ProtoOrderLineStatus status, Long orderId) {
        this.id = id;
        this.item = item;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
        this.orderId = orderId;
    }

    @ProtoField(number = 1)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ProtoField(number = 2)
    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    @ProtoField(number = 3)
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @ProtoField(number = 4)
    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    @ProtoField(number = 5)
    public ProtoOrderLineStatus getStatus() {
        return status;
    }

    public void setStatus(ProtoOrderLineStatus status) {
        this.status = status;
    }

    @ProtoField(number = 6)
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProtoOrderLine that = (ProtoOrderLine) o;
        return quantity == that.quantity && Objects.equals(id, that.id) && Objects.equals(item, that.item) && Objects
              .equals(totalPrice, that.totalPrice) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, item, quantity, totalPrice, status);
    }

    @Override
    public String toString() {
        return "ProtoOrderLine{" + "id=" + id + ", item='" + item + '\'' + ", quantity=" + quantity + ", totalPrice="
              + totalPrice + ", status=" + status + '}';
    }
}
