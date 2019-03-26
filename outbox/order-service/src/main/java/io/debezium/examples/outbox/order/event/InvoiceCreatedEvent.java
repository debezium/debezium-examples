/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.debezium.examples.outbox.order.model.PurchaseOrder;
import io.debezium.examples.outbox.order.outbox.ExportedEvent;

import java.util.Date;

public class InvoiceCreatedEvent implements ExportedEvent {

    private static ObjectMapper mapper = new ObjectMapper();

    private final long customerId;
    private final JsonNode order;
    private final Long timestamp;

    private InvoiceCreatedEvent(long customerId, JsonNode order) {
        this.customerId = customerId;
        this.order = order;
        this.timestamp = (new Date()).getTime();
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
    public String getType() {
        return "InvoiceCreated";
    }

    @Override
    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public JsonNode getPayload() {
        return order;
    }
}
