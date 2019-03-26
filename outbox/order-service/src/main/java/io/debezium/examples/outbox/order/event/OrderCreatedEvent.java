/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.order.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.debezium.examples.outbox.order.model.OrderLine;
import io.debezium.examples.outbox.order.model.PurchaseOrder;
import io.debezium.examples.outbox.order.outbox.ExportedEvent;

import java.util.Date;

public class OrderCreatedEvent implements ExportedEvent {

    private static ObjectMapper mapper = new ObjectMapper();

    private final long id;
    private final JsonNode order;
    private final Long timestamp;

    private OrderCreatedEvent(long id, JsonNode order) {
        this.id = id;
        this.order = order;
        this.timestamp = (new Date()).getTime();
    }

    public static OrderCreatedEvent of(PurchaseOrder order) {
        ObjectNode asJson = mapper.createObjectNode()
                .put("id", order.getId())
                .put("customerId", order.getCustomerId())
                .put("orderDate", order.getOrderDate().toString());

        ArrayNode items = asJson.putArray("lineItems");

        for (OrderLine orderLine : order.getLineItems()) {
        items.add(
                mapper.createObjectNode()
                .put("id", orderLine.getId())
                .put("item", orderLine.getItem())
                .put("quantity", orderLine.getQuantity())
                .put("totalPrice", orderLine.getTotalPrice())
                .put("status", orderLine.getStatus().name())
            );
        }

        return new OrderCreatedEvent(order.getId(), asJson);
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(id);
    }

    @Override
    public String getAggregateType() {
        return "Order";
    }

    @Override
    public String getType() {
        return "OrderCreated";
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
