/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.customer.facade;

import io.debezium.examples.saga.customer.model.CreditLimitEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class CreditDeserializer extends ObjectMapperDeserializer<CreditLimitEvent> {

    public CreditDeserializer() {
        super(CreditLimitEvent.class);
    }
}
