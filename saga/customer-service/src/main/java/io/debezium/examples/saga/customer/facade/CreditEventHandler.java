/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.customer.facade;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.saga.customer.event.CreditEvent;
import io.debezium.examples.saga.customer.messagelog.MessageLog;
import io.debezium.examples.saga.customer.model.Credit;
import io.debezium.examples.saga.customer.model.CreditRequestType;
import io.debezium.examples.saga.customer.model.CreditStatus;
import io.debezium.outbox.quarkus.ExportedEvent;

@ApplicationScoped
public class CreditEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditEventHandler.class);

    @Inject
    MessageLog log;

    @Inject
    Event<ExportedEvent<?, ?>> event;

    @Transactional
    public void onCreditEvent(UUID eventId, UUID sagaId, Credit event) {
        if (log.alreadyProcessed(eventId)) {
            LOGGER.info("Event with UUID {} was already retrieved, ignoring it", eventId);
            return;
        }

        CreditStatus status;

        if (event.type == CreditRequestType.REQUEST) {
            if (event.paymentDue > 5000) {
                status = CreditStatus.REJECTED;
            }
            else {
                status = CreditStatus.APPROVED;
            }
        }
        else {
            status = CreditStatus.CANCELLED;
        }

        event.persist();

        this.event.fire(CreditEvent.of(sagaId, status));

        log.processed(eventId);
    }
}
