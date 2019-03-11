/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.facade;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.examples.outbox.shipment.log.MessageLog;
import io.debezium.examples.outbox.shipment.service.ShipmentService;

@ApplicationScoped
public class OrderEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventHandler.class);

    @Inject
    MessageLog log;

    @Inject
    ShipmentService shipmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void onOrderEvent(UUID eventId, String key, JsonNode event) throws IOException {
        if (log.alreadyProcessed(eventId)) {
            LOGGER.info("Event with UUID {} was already retrieved, ignoring it", eventId);
            return;
        }

        final JsonNode payload = event.has("schema") ? event.get("payload") : event;

        final String eventType = payload.get("eventType").asText();
        final Long ts = payload.get("ts_ms").asLong();
        final String eventPayload = payload.get("payload").asText();

        final JsonNode payloadObject = objectMapper.readTree(eventPayload);

        LOGGER.info("Received 'Order' event -- key: {}, event id: '{}', event type: '{}', ts: '{}'", key, eventId, eventType, ts);

        if (eventType.equals("OrderCreated")) {
            shipmentService.orderCreated(payloadObject);
        }
        else if (eventType.equals("OrderLineUpdated")) {
            shipmentService.orderLineUpdated(payloadObject);
        }
        else {
            LOGGER.warn("Unkown event type");
        }

        log.processed(eventId);
    }
}
