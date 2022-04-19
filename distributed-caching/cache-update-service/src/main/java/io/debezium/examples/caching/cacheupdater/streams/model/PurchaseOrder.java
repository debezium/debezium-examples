/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.cacheupdater.streams.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PurchaseOrder {

    public Long id;

    @JsonProperty("customer_id")
    public long customerId;

    /**
     * Coming in as *micros* since epoch.
     */
    @JsonProperty("order_date")
    public long orderDate;

    public int version;
}
