/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.examples.saga.framework.internal;

public class SagaStepState {

    public String type;
    public SagaStepStatus status;

    public SagaStepState(String type, SagaStepStatus status) {
        this.type = type;
        this.status = status;
    }
}
