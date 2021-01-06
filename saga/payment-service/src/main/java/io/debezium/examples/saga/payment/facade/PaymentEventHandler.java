/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.payment.facade;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.examples.saga.payment.event.PaymentEvent;
import io.debezium.examples.saga.payment.messagelog.MessageLog;
import io.debezium.examples.saga.payment.model.Payment;
import io.debezium.examples.saga.payment.model.PaymentStatus;
import io.debezium.outbox.quarkus.ExportedEvent;

@ApplicationScoped
public class PaymentEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentEventHandler.class);

    @Inject
    MessageLog log;

    @Inject
    Event<ExportedEvent<?, ?>> event;

    @Transactional
    public void onPaymentEvent(UUID eventId, UUID sagaId, Payment event) {
        if (log.alreadyProcessed(eventId)) {
            LOGGER.info("Event with UUID {} was already retrieved, ignoring it", eventId);
            return;
        }

        if (event.status == PaymentStatus.REQUESTED) {
            if (event.creditCardNo.endsWith("9999")) {
                event.status = PaymentStatus.FAILED;
            }

            event.persist();

            this.event.fire(PaymentEvent.of(sagaId, event.status));
        }

        log.processed(eventId);
    }
}
