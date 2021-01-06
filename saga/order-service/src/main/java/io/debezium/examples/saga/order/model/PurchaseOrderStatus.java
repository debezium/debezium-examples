/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.model;

public enum PurchaseOrderStatus {
    CREATED, PROCESSING, CANCELLED, COMPLETED;
}
