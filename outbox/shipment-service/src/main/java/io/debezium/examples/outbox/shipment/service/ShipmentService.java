/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.outbox.shipment.service;

import java.time.LocalDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.eclipse.microprofile.opentracing.Traced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.debezium.examples.outbox.shipment.model.Shipment;

@ApplicationScoped
@Traced
public class ShipmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentService.class);

    @PersistenceContext
    EntityManager entityManager;

    @Transactional(value=TxType.MANDATORY)
    public void orderCreated(JsonNode event) {
        LOGGER.info("Processing 'OrderCreated' event: {}", event);

        final long orderId = event.get("id").asLong();
        final long customerId = event.get("customerId").asLong();
        final LocalDateTime orderDate = LocalDateTime.parse(event.get("orderDate").asText());

        entityManager.persist(new Shipment(customerId, orderId, orderDate));
    }

    @Transactional(value=TxType.MANDATORY)
    public void orderLineUpdated(JsonNode event) {
        LOGGER.info("Processing 'OrderLineUpdated' event: {}", event);
    }
}
