/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.rest;

import io.debezium.examples.outbox.order.model.OrderLineStatus;

/**
 * A value object that represents the response for a {@link UpdateOrderLineRequest}.
 */
public class UpdateOrderLineResponse {

    private final OrderLineStatus oldStatus;
    private final OrderLineStatus newStatus;

    public UpdateOrderLineResponse(OrderLineStatus oldStatus, OrderLineStatus newStatus) {
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public OrderLineStatus getOldStatus() {
        return oldStatus;
    }

    public OrderLineStatus getNewStatus() {
        return newStatus;
    }
}
