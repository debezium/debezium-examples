/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.credit.facade;

import io.debezium.examples.saga.credit.model.Credit;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class CreditDeserializer extends ObjectMapperDeserializer<Credit> {

    public CreditDeserializer() {
        super(Credit.class);
    }
}
