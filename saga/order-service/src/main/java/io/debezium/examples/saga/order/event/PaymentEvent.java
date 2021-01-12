/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.order.event;

import java.util.UUID;

import org.apache.kafka.common.header.Headers;

public class PaymentEvent {

    public UUID sagaId;
    public UUID messageId;
    public PaymentStatus status;
    public Headers headers;

    public PaymentEvent(UUID sagaId, UUID messageId, PaymentStatus status, Headers headers) {
        this.sagaId = sagaId;
        this.messageId = messageId;
        this.status = status;
        this.headers = headers;
    }

    @Override
    public String toString() {
        return "PaymentEvent [sagaId=" + sagaId + ", messageId=" + messageId + ", status=" + status + "]";
    }
}
