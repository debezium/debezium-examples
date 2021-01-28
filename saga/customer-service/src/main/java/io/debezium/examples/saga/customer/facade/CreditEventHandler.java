/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.customer.facade;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import org.eclipse.microprofile.opentracing.Traced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.saga.customer.event.CreditEvent;
import io.debezium.examples.saga.customer.messagelog.MessageLog;
import io.debezium.examples.saga.customer.model.CreditLimitEvent;
import io.debezium.examples.saga.customer.model.CreditRequestType;
import io.debezium.examples.saga.customer.model.CreditStatus;
import io.debezium.examples.saga.customer.model.Customer;
import io.debezium.outbox.quarkus.ExportedEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@Traced
public class CreditEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditEventHandler.class);

    @Inject
    MessageLog log;

    @Inject
    Event<ExportedEvent<?, ?>> outboxEvent;

    @Inject
    EntityManager entityManager;

    @Transactional
    public void onCreditEvent(UUID eventId, UUID sagaId, CreditLimitEvent event) {
        if (log.alreadyProcessed(eventId)) {
            LOGGER.info("Event with UUID {} was already retrieved, ignoring it", eventId);
            return;
        }

        Customer customer = Customer.findById(event.customerId);

        if (customer == null) {
            throw new EntityNotFoundException("Customer not found: " + event.customerId);
        }

        CreditStatus status;

        if (event.type == CreditRequestType.REQUEST) {
            if (customer.fitsCreditLimit(event.paymentDue)) {
                status = CreditStatus.APPROVED;
                customer.allocateCreditLimit(event.paymentDue);
            }
            else {
                status = CreditStatus.REJECTED;
            }
        }
        else {
            customer.releaseCreditLimit(event.paymentDue);
            status = CreditStatus.CANCELLED;
        }

        this.outboxEvent.fire(CreditEvent.of(sagaId, status));

        log.processed(eventId);
    }

    @Transactional
    public void createTestData(@Observes StartupEvent se) {
        entityManager.createNativeQuery("INSERT INTO customer.customer (id, openlimit, version) VALUES (456, 50000, 0)")
            .executeUpdate();
    }
 }
