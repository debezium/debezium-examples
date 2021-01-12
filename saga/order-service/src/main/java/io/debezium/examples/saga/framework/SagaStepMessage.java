/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.framework;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A message representing one logical step of execution in a saga.
 *
 * @author Gunnar Morling
 */
public class SagaStepMessage {

    public String type;
    public String eventType;
    public JsonNode payload;

    public SagaStepMessage(String type, String eventType, JsonNode payload) {
        this.type = type;
        this.eventType = eventType;
        this.payload = payload;
    }
}
