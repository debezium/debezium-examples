/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphql.eventsource;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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

    @Column(name = "product_id")
    public long productId;

//    @ManyToOne
    @Column(name = "customer_id")
    public long customerId;

    public int quantity;

    Order() {
    }

    public Order(ZonedDateTime timestamp, long customerId, long productId, int quantity) {
        this.timestamp = timestamp;
        this.productId = productId;
        this.customerId = customerId;
        this.quantity = quantity;
//        this.salesPrice = salesPrice;
    }

    @Override
    public String toString() {
        return "Order [id=" + id + ", timestamp=" + timestamp + ", productId=" + productId + ", customerId=" + customerId + ", quantity=" + quantity + "]";
    }

 
}
