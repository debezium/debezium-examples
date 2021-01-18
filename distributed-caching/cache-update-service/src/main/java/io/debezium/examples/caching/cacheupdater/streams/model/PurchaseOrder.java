/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.cacheupdater.streams.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PurchaseOrder {

    public Long id;

    @JsonProperty("customer_id")
    public long customerId;

    @JsonProperty("order_date")
    public Instant orderDate;
}
