/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.facade;

import java.io.StringReader;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.outbox.shipment.log.MessageLog;
import io.debezium.examples.outbox.shipment.service.ShipmentService;

@ApplicationScoped
public class OrderEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventHandler.class);

    @Inject
    private MessageLog log;

    @Inject
    private ShipmentService shipmentService;

    @Transactional
    public void onOrderEvent(UUID eventId, String key, String event, Long ts) {
        if (log.alreadyProcessed(eventId)) {
            LOGGER.info("Event with UUID {} was already retrieved, ignoring it", eventId);
            return;
        }

        JsonObject json = Json.createReader(new StringReader(event)).readObject();
        JsonObject payload = json.containsKey("schema") ? json.getJsonObject("payload") :json;

        String eventType = payload.getString("eventType");
        String eventPayload = payload.getString("payload");

        JsonReader payloadReader = Json.createReader(new StringReader(eventPayload));
        JsonObject payloadObject = payloadReader.readObject();

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
