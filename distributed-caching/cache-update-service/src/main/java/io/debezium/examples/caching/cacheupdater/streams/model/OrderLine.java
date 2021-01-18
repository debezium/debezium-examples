/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.cacheupdater.streams.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An entity mapping that represents a line item on a {@link PurchaseOrder} entity.
 */
public class OrderLine {

    public Long id;
    public String item;
    public int quantity;

    @JsonProperty("total_price")
    public BigDecimal totalPrice;
    public OrderLineStatus status;
    public Long order_id;
}
