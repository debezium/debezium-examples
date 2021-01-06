/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.framework;

/**
 * A message representing one logical step of execution in a saga.
 *
 * @author Gunnar Morling
 */
public class SagaStepMessage {

    public String type;
    public String payload;

    public SagaStepMessage(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }
}
