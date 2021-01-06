/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.customer.event;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.debezium.examples.saga.customer.model.CreditStatus;
import io.debezium.outbox.quarkus.ExportedEvent;

public class CreditEvent implements ExportedEvent<String, JsonNode> {

    private static ObjectMapper mapper = new ObjectMapper();

    private final UUID sagaId;
    private final JsonNode payload;
    private final Instant timestamp;

    private CreditEvent(UUID sagaId, JsonNode payload) {
        this.sagaId = sagaId;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    public static CreditEvent of(UUID sagaId, CreditStatus status) {
        ObjectNode asJson = mapper.createObjectNode()
                .put("status", status.name());

        return new CreditEvent(sagaId, asJson);
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(sagaId);
    }

    @Override
    public String getAggregateType() {
        return "credit-approval";
    }

    @Override
    public JsonNode getPayload() {
        return payload;
    }

    @Override
    public String getType() {
        return "CreditUpdated";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
}
