/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.model;

public enum OrderLineStatus {
    ENTERED,
    CANCELLED,
    SHIPPED;
}
