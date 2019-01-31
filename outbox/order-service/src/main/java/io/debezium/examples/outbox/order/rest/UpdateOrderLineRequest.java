/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.rest;

import io.debezium.examples.outbox.order.model.OrderLineStatus;

public class UpdateOrderLineRequest {

    private OrderLineStatus newStatus;

    public OrderLineStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderLineStatus newStatus) {
        this.newStatus = newStatus;
    }
}
