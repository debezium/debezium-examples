/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.kstreams.liveupdate.aggregator.model;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Order {

    public long id;

    @JsonProperty("ts")
    public ZonedDateTime timestamp;

    @JsonProperty("purchaser_id")
    public long purchaserId;

    @JsonProperty("product_id")
    public long productId;

    @JsonProperty("category_id")
    public long categoryId;

    @JsonProperty("category_name")
    public String categoryName;

    public int quantity;

    @JsonProperty("sales_price")
    public long salesPrice;

    Order() {
    }

    public Order(long id, ZonedDateTime timestamp, long purchaserId, long productId, long categoryId,
            String categoryName, int quantity, long salesPrice) {
        this.id = id;
        this.timestamp = timestamp;
        this.purchaserId = purchaserId;
        this.productId = productId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.quantity = quantity;
        this.salesPrice = salesPrice;
    }
}
