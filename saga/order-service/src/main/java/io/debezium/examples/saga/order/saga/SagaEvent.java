/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.saga;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import io.debezium.outbox.quarkus.ExportedEvent;

public class SagaEvent implements ExportedEvent<String, JsonNode> {

    private final UUID sagaId;
    private final String aggregateType;
    private final String eventType;
    private final JsonNode payload;
    private final Instant timestamp;

    public SagaEvent(UUID sagaId, String aggregateType, String eventType, JsonNode payload) {
        this.sagaId = sagaId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(sagaId);
    }

    @Override
    public String getAggregateType() {
        return aggregateType;
    }

    @Override
    public JsonNode getPayload() {
        return payload;
    }

    @Override
    public String getType() {
        return eventType;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
}
