/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.event;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.debezium.examples.outbox.order.model.PurchaseOrder;
import io.debezium.quarkus.outbox.ExportedEvent;

/**
 * A 'Customer' event that indicates an invoice has been created.
 */
public class InvoiceCreatedEvent implements ExportedEvent {

    private static ObjectMapper mapper = new ObjectMapper();

    private final long customerId;
    private final JsonNode order;
    private final Instant timestamp;

    private InvoiceCreatedEvent(long customerId, JsonNode order) {
        this.customerId = customerId;
        this.order = order;
        this.timestamp = Instant.now();
    }

    public static InvoiceCreatedEvent of(PurchaseOrder order) {
        ObjectNode asJson = mapper.createObjectNode()
                .put("orderId", order.getId())
                .put("invoiceDate", order.getOrderDate().toString())
                .put("invoiceValue", order.getTotalValue());

        return new InvoiceCreatedEvent(order.getCustomerId(), asJson);
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(customerId);
    }

    @Override
    public String getAggregateType() {
        return "Customer";
    }

    @Override
    public JsonNode getPayload() {
        return order;
    }

    @Override
    public String getType() {
        return "InvoiceCreated";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
}
