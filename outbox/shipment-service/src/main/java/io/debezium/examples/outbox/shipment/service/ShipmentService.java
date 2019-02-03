/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.service;

import java.time.LocalDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.outbox.shipment.model.Shipment;

@ApplicationScoped
public class ShipmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(value=TxType.MANDATORY)
    public void orderCreated(JsonObject event) {
        LOGGER.info("Processing 'OrderCreated' event: {}", event);

        JsonNumber orderId = event.getJsonNumber("id");
        String customer = event.getString("customer");
        LocalDateTime orderDate = LocalDateTime.parse(event.getString("orderDate"));

        entityManager.persist(new Shipment(customer, orderId.longValue(), orderDate));
    }

    @Transactional(value=TxType.MANDATORY)
    public void orderLineUpdated(JsonObject event) {
        LOGGER.info("Processing 'OrderLineUpdated' event: {}", event);
    }
}
