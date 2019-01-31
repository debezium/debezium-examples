/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.event;

import com.fasterxml.jackson.databind.JsonNode;

public interface ExportedEvent {

    String getAggregateId();

    String getAggregateType();

    JsonNode getPayload();

    String getType();
}
