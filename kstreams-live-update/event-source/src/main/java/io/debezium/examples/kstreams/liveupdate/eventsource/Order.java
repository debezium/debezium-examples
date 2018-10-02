/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.eventsource;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    public long id;

    @Column(name = "ts")
    public ZonedDateTime timestamp;

    @Column(name = "purchaser_id")
    public long purchaserId;

    @Column(name = "product_id")
    public long productId;

    @ManyToOne
    @JoinColumn(name = "category_id")
    public Category category;

    public int quantity;

    @Column(name = "sales_price")
    public long salesPrice;

    Order() {
    }

    public Order(ZonedDateTime timestamp, long purchaserId, long productId, Category category, int quantity,
            long salesPrice) {
        this.timestamp = timestamp;
        this.purchaserId = purchaserId;
        this.productId = productId;
        this.category = category;
        this.quantity = quantity;
        this.salesPrice = salesPrice;
    }

    @Override
    public String toString() {
        return "Order [id=" + id + ", timestamp=" + timestamp + ", purchaserId=" + purchaserId + ", productId="
                + productId + ", category=" + category + ", quantity=" + quantity + ", salesPrice=" + salesPrice + "]";
    }
}
