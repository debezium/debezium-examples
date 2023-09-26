/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.model;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

import java.math.BigDecimal;

/**
 * An entity mapping that represents a line item on a {@link PurchaseOrder} entity.
 */
@Entity
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_line_ids")
    @SequenceGenerator(name = "order_line_ids", sequenceName = "seq_order_line", allocationSize=50)
    private Long id;

    private String item;

    private Integer quantity;

    @Column(name="total_price")
    private BigDecimal totalPrice;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private PurchaseOrder purchaseOrder;

    @Enumerated(EnumType.STRING)
    private OrderLineStatus status;

    public OrderLine() {
    }

    @ProtoFactory
    public OrderLine(Long id, String item, Integer quantity, BigDecimal totalPrice, OrderLineStatus status) {
        this.id = id;
        this.item = item;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public OrderLine(String item, Integer quantity, BigDecimal totalPrice) {
        this.item = item;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = OrderLineStatus.ENTERED;
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
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    @ProtoField(number = 5)
    public OrderLineStatus getStatus() {
        return status;
    }

    public void setStatus(OrderLineStatus status) {
        this.status = status;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }
}
