/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.payment.event;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.debezium.examples.saga.payment.model.PaymentStatus;
import io.debezium.outbox.quarkus.ExportedEvent;

public class PaymentEvent implements ExportedEvent<String, JsonNode> {

    private static ObjectMapper mapper = new ObjectMapper();

    private final UUID sagaId;
    private final JsonNode payload;
    private final Instant timestamp;

    private PaymentEvent(UUID sagaId, JsonNode payload) {
        this.sagaId = sagaId;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    public static PaymentEvent of(UUID sagaId, PaymentStatus status) {
        ObjectNode asJson = mapper.createObjectNode()
                .put("status", status.name());

        return new PaymentEvent(sagaId, asJson);
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(sagaId);
    }

    @Override
    public String getAggregateType() {
        return "payment";
    }

    @Override
    public JsonNode getPayload() {
        return payload;
    }

    @Override
    public String getType() {
        return "PaymentUpdated";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
}
