/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.payment.facade;

import io.debezium.examples.saga.payment.model.Payment;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class PaymentDeserializer extends ObjectMapperDeserializer<Payment> {

    public PaymentDeserializer() {
        super(Payment.class);
    }
}
