/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.service;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShipmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentService.class);

    public void orderCreated(JsonObject event) {
        LOGGER.info("Processing 'OrderCreated' event: {}", event);
    }

    public void orderLineUpdated(JsonObject event) {
        LOGGER.info("Processing 'OrderLineUpdated' event: {}", event);
    }
}
