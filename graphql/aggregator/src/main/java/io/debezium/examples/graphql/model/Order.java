/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.graphql.model;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Order {

    public long id;

    @JsonProperty("ts")
    public ZonedDateTime timestamp;

    @JsonProperty("product_id")
    public long productId;

    @JsonProperty("customer_id")
    public long customerId;

    public int quantity;

    Order() {
    }

    @Override
    public String toString() {
        return "Order [id=" + id + ", timestamp=" + timestamp + ", productId=" + productId + ", customerId=" + customerId + ", quantity=" + quantity + "]";
    }

}
