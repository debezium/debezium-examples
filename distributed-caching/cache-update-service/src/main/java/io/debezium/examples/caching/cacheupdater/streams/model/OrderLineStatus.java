/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.cacheupdater.streams.model;

/**
 * Various statuses in which a {@link OrderLine} may be within.
 */
public enum OrderLineStatus {
    ENTERED,
    CANCELLED,
    SHIPPED
}
