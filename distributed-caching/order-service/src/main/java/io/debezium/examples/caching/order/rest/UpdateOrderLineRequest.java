/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.order.rest;

import io.debezium.examples.caching.model.OrderLineStatus;

/**
 * A value object that represents updating a {@link io.debezium.examples.caching.model.OrderLine} status.
 */
public class UpdateOrderLineRequest {

    private int version;
    private OrderLineStatus newStatus;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public OrderLineStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderLineStatus newStatus) {
        this.newStatus = newStatus;
    }
}
