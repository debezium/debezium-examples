/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.caching.commons;

import org.infinispan.protostream.annotations.ProtoEnumValue;

/**
 * Various statuses in which a {@link OrderLine} may be within.
 */
public enum OrderLineStatus {
    @ProtoEnumValue(number = 1)
    ENTERED,
    @ProtoEnumValue(number = 2)
    CANCELLED,
    @ProtoEnumValue(number = 3)
    SHIPPED
}
